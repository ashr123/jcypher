/************************************************************************
 * Copyright (c) 2014-2016 IoT-Solutions e.U.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ************************************************************************/

package iot.jcypher.query.result.util;

import iot.jcypher.concurrency.Locking;
import iot.jcypher.database.IDBAccess;
import iot.jcypher.database.remote.BoltDBAccess;
import iot.jcypher.domain.internal.DomainAccess.DomainAccessHandler.DBAccessWrapper;
import iot.jcypher.graph.*;
import iot.jcypher.graph.internal.ChangeListener;
import iot.jcypher.graph.internal.GrId;
import iot.jcypher.graph.internal.LocalId;
import iot.jcypher.graph.internal.LockUtil;
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.api.pattern.Node;
import iot.jcypher.query.api.pattern.Relation;
import iot.jcypher.query.api.start.StartPoint;
import iot.jcypher.query.factories.clause.*;
import iot.jcypher.query.factories.xpression.C;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.result.util.ResultHandler.AContentHandler.PropEntry;
import iot.jcypher.query.result.util.ResultHandler.AContentHandler.RowOrRecord;
import iot.jcypher.query.values.*;
import iot.jcypher.query.writer.Format;
import iot.jcypher.query.writer.WriterContext;
import iot.jcypher.transaction.ITransaction;
import iot.jcypher.util.QueriesPrintObserver.QueryToObserve;
import iot.jcypher.util.ResultSettings;
import iot.jcypher.util.Util;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.value.ListValue;

import javax.json.JsonObject;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

public class ResultHandler
{

	public static final String lockVersionProperty = "_c_version_";

	public static ThreadLocal<Boolean> includeNullValues = new ThreadLocal<Boolean>()
	{
		@Override
		protected Boolean initialValue()
		{
			return Boolean.FALSE;
		}
	};
	// allow to switch off writing version property for testing purposes
	public static boolean writeVersion = true;
	private AContentHandler contentHandler;
	private IDBAccess dbAccess;
	private Graph graph;
	private Locking lockingStrategy;
	private LocalElements localElements;
	private NodeRelationListener nodeRelationListener;
	private Map<Long, GrNode> nodesById;
	// contains changed and removed (deleted) nodes
	private Map<Long, GrNode> changedNodesById;
	private Map<Long, GrRelation> relationsById;
	// contains changed and removed (deleted) relations
	private Map<Long, GrRelation> changedRelationsById;
	// contains element versions if Locking is activated
	private ElementVersions elementVersions;
	private Map<String, List<GrNode>> nodeColumns;
	private Map<String, List<GrRelation>> relationColumns;
	private Map<String, List<GrPath>> pathColumns;
	@SuppressWarnings("rawtypes")
	private Map<String, List> valueColumns;
	// only needed to lookup nodes and relations
	private List<String> unResolvedColumns;

	/**
	 * construct a ResultHandler initialized with a jsonResult
	 *
	 * @param jsonResult
	 * @param queryIndex
	 * @param dbAccess
	 */
	public ResultHandler(JsonObject jsonResult, int queryIndex, IDBAccess dbAccess)
	{
		super();
		init(dbAccess);
		this.contentHandler = new JSONContentHandler(jsonResult, queryIndex);

	}

	/**
	 * construct a ResultHandler initialized with a statementResult
	 *
	 * @param statementResult
	 * @param dbAccess
	 */
	public ResultHandler(Result statementResult, IDBAccess dbAccess)
	{
		super();
		init(dbAccess);
		this.contentHandler = new BoltContentHandler(statementResult, this);

	}

	/**
	 * construct a ResultHandler initialized with a statementResult
	 *
	 * @param dbAccess
	 */
	public ResultHandler(IDBAccess dbAccess)
	{
		super();
		init(dbAccess);
		this.contentHandler = this.calcContentHandler(dbAccess);
	}

	private AContentHandler calcContentHandler(IDBAccess dba)
	{
		if (dba instanceof BoltDBAccess)
			return new BoltContentHandler(null, this);
		else if (dba instanceof DBAccessWrapper)
			return this.calcContentHandler(((DBAccessWrapper) dba).getDelegate());
		else
			return new JSONContentHandler(null, -1);
	}

	private void init(IDBAccess dbAccess)
	{
		this.dbAccess = dbAccess;
		this.lockingStrategy = Locking.NONE;
		this.localElements = new LocalElements();
		this.graph = GrAccess.createGraph(this);
		GrAccess.setGraphState(this.graph, SyncState.SYNC);
	}

	IDBAccess getDbAccess()
	{
		return dbAccess;
	}

	public void setDbAccess(IDBAccess dbAccess)
	{
		this.dbAccess = dbAccess;
	}

	AContentHandler getContentHandler()
	{
		return contentHandler;
	}

	public void setLockingStrategy(Locking lockingStrategy)
	{
		this.lockingStrategy = lockingStrategy;
	}

	public ITransaction createLockingTxIfNeeded()
	{
		if (this.lockingStrategy == Locking.OPTIMISTIC)
		{
			ITransaction tx = this.dbAccess.getTX();
			if (tx == null)
				return this.dbAccess.beginTX();
		}
		return null;
	}

	public LocalElements getLocalElements()
	{
		return localElements;
	}

	public Graph getGraph()
	{
		return this.graph;
	}

	public List<GrNode> getNodes(JcNode node)
	{
		String colKey = ValueAccess.getName(node);
		List<GrNode> nds = getNodes(colKey);
		nds = filterRemovedAndNullItems(nds);
		return Collections.unmodifiableList(nds);
	}

	private <T extends PersistableItem> List<T> filterRemovedAndNullItems(List<T> items)
	{
		ArrayList<T> rItems = new ArrayList<T>();
		for (T item : items)
		{
			if (item == null)
			{
				if (includeNullValues.get() || ResultSettings.includeNullValuesAndDuplicates.get().booleanValue())
					rItems.add(item);
			} else if (GrAccess.getState(item) != SyncState.REMOVED)
				rItems.add(item);
		}
		return rItems;
	}

	private List<GrNode> getNodes(String colKey)
	{
		List<GrNode> rNodes = getNodeColumns().get(colKey);
		if (rNodes == null)
		{
			rNodes = new ArrayList<GrNode>();
			Iterator<RowOrRecord> it = this.contentHandler.getDataIterator();
			int rowIdx = -1;
			while (it.hasNext())
			{ // iterate over rows
				rowIdx++;
				RowOrRecord roc = it.next();
				ElementInfo ei = roc.getElementInfo(colKey);
				GrNode rNode = null;
				if (!ei.isNull)
				{
					rNode = getNodesById().get(ei.id);
					if (rNode == null)
					{
						rNode = GrAccess.createNode(this, new GrId(ei.id), rowIdx);
						GrAccess.setState(rNode, SyncState.SYNC);
						GrAccess.addChangeListener(getNodeRelationListener(), rNode);
						getNodesById().put(ei.id, rNode);
					}
				}
				if (ResultSettings.includeNullValuesAndDuplicates.get().booleanValue() || !rNodes.contains(rNode))
					rNodes.add(rNode);
			}
			getNodeColumns().put(colKey, rNodes);
			getUnresolvedColumns().remove(colKey);
		}
		return rNodes;
	}

	public List<GrRelation> getRelations(JcRelation relation)
	{
		String colKey = ValueAccess.getName(relation);
		List<GrRelation> rels = getRelations(colKey);
		rels = filterRemovedAndNullItems(rels);
		return Collections.unmodifiableList(rels);
	}

	private List<GrRelation> getRelations(String colKey)
	{
		List<GrRelation> rRelations = getRelationColumns().get(colKey);
		if (rRelations == null)
		{
			rRelations = new ArrayList<GrRelation>();
			Iterator<RowOrRecord> it = this.contentHandler.getDataIterator();
			int rowIdx = -1;
			while (it.hasNext())
			{ // iterate over rows
				rowIdx++;
				RowOrRecord roc = it.next();
				GrRelation rRelation = null;
				ElementInfo ei = roc.getElementInfo(colKey);
				if (!ei.isNull)
				{
					RelationInfo ri = roc.getRelationInfo(colKey);
					rRelation = getRelationsById().get(ei.id);
					if (rRelation == null)
					{
						rRelation = GrAccess.createRelation(this, new GrId(ei.id),
								new GrId(ri.startNodeId), new GrId(ri.endNodeId), rowIdx);
						GrAccess.setState(rRelation, SyncState.SYNC);
						GrAccess.addChangeListener(getNodeRelationListener(), rRelation);
						getRelationsById().put(ei.id, rRelation);
					}
				}
				if (ResultSettings.includeNullValuesAndDuplicates.get().booleanValue() || !rRelations.contains(rRelation))
					rRelations.add(rRelation);
			}
			getRelationColumns().put(colKey, rRelations);
			getUnresolvedColumns().remove(colKey);
		}

		return rRelations;
	}

	public List<GrPath> getPaths(JcPath path)
	{
		String colKey = ValueAccess.getName(path);
		List<GrPath> rPaths = getPathColumns().get(colKey);
		if (rPaths == null)
		{
			rPaths = new ArrayList<GrPath>();
			Iterator<RowOrRecord> it = this.contentHandler.getDataIterator();
			int rowIdx = -1;
			while (it.hasNext())
			{ // iterate over rows
				rowIdx++;
				RowOrRecord roc = it.next();
				PathInfo pinfo = roc.getPathInfo(colKey);

				if (pinfo != null)
				{
					int sz = pinfo.relationIds.size();
					List<GrId> relIds = new ArrayList<GrId>(sz);
					long sid;
					long eid = pinfo.startNodeId;
					for (int i = 0; i < sz; i++)
					{
						long rid = pinfo.relationIds.get(i);
						GrRelation rRelation = getRelationsById().get(rid);
						if (rRelation == null)
						{
							sid = eid;
							eid = roc.gePathtNodeIdAt(pinfo, i + 1);
							rRelation = GrAccess.createRelation(this, new GrId(rid),
									new GrId(sid), new GrId(eid), rowIdx);
							GrAccess.setState(rRelation, SyncState.SYNC);
							GrAccess.addChangeListener(getNodeRelationListener(), rRelation);
							getRelationsById().put(rid, rRelation);
						}
						relIds.add(new GrId(rid));
					}
					GrPath rPath = GrAccess.createPath(this, new GrId(pinfo.startNodeId),
							new GrId(pinfo.endNodeId), relIds, rowIdx);
					rPaths.add(rPath);
				} else if (ResultSettings.includeNullValuesAndDuplicates.get().booleanValue())
				{
					rPaths.add(null);
				}
			}
			getPathColumns().put(colKey, rPaths);
		}
		return Collections.unmodifiableList(rPaths);
	}

	public List<BigDecimal> getNumbers(JcNumber number)
	{
		return this.getValues(number);
	}

	public List<String> getStrings(JcString string)
	{
		return this.getValues(string);
	}

	public List<Boolean> getBooleans(JcBoolean bool)
	{
		return this.getValues(bool);
	}

	public List<List<?>> getCollections(JcCollection collection)
	{
		return this.getValues(collection);
	}

	public List<?> getObjects(JcValue val)
	{
		return this.getValues(val);
	}

	public GrNode getNode(GrId id, int rowIdx)
	{
		if (id instanceof LocalId)
			return getLocalNode(id.getId());
		else
			return getNode(id.getId(), rowIdx);
	}

	private GrNode getLocalNode(long id)
	{
		return this.localElements.getNode(id);
	}

	private GrNode getNode(long id, int rowIdx)
	{
		GrNode rNode = getNodesById().get(id);
		if (rNode == null)
		{
			// first resolve unresolved columns
			if (getUnresolvedColumns().size() > 0)
			{
				List<String> ucols = new ArrayList<String>();
				ucols.addAll(getUnresolvedColumns());
				for (int i = 0; i < ucols.size(); i++)
				{
					String colKey = ucols.get(i);
					Iterator<RowOrRecord> it = this.contentHandler.getDataIterator();
					boolean isNodeColumn = false;
					while (it.hasNext())
					{ // iterate over just one row
						RowOrRecord roc = it.next();
						ElementInfo ei = roc.getElementInfo(colKey);
						if (ei != null && !ei.isNull)
						{ // relation or node
							if (ElemType.NODE == ei.type)
							{
								isNodeColumn = true;
							}
						} else
							getUnresolvedColumns().remove(colKey);
						break;
					}
					if (isNodeColumn)
					{
						// resolve nodes of column
						getNodes(colKey);
						// test if node has been resolved
						rNode = getNodesById().get(id);
						if (rNode != null)
							return rNode;
					}
				}
			}
		}
		if (rNode == null)
		{
			rNode = GrAccess.createNode(this, new GrId(id), rowIdx);
			GrAccess.setState(rNode, SyncState.SYNC);
			GrAccess.addChangeListener(getNodeRelationListener(), rNode);
			getNodesById().put(id, rNode);
		}
		return rNode;
	}

	public GrRelation getRelation(GrId id)
	{
		if (id instanceof LocalId)
		{
			return getLocalRelation(id.getId());
		} else
			return getRelationsById().get(id.getId());
	}

	private GrRelation getLocalRelation(long id)
	{
		return this.localElements.getRelation(id);
	}

	public String getRelationType(long relationId, int rowIndex)
	{
		return this.contentHandler.getRelationType(relationId, rowIndex);
	}

	public List<GrProperty> getNodeProperties(GrId nodeId, int rowIndex)
	{
		if (nodeId instanceof LocalId)
			return new ArrayList<GrProperty>();
		else
			return getNodeProperties(nodeId.getId(), rowIndex);
	}

	private List<GrProperty> getNodeProperties(long nodeId, int rowIndex)
	{
		List<GrProperty> props = new ArrayList<GrProperty>();
		Iterator<PropEntry> esIt = this.contentHandler.getPropertiesIterator(nodeId, rowIndex, ElemType.NODE);
		while (esIt.hasNext())
		{
			PropEntry entry = esIt.next();
			GrProperty prop = GrAccess.createProperty(entry.getPropName());
			prop.setValue(this.contentHandler.convertContentValue(entry.getPropValue()));
			GrAccess.setState(prop, SyncState.SYNC);
			props.add(prop);
		}
		return props;
	}

	public List<GrLabel> getNodeLabels(long nodeId, int rowIndex)
	{
		return this.contentHandler.getNodeLabels(nodeId, rowIndex);
	}

	public List<GrProperty> getRelationProperties(GrId relationId, int rowIndex)
	{
		if (relationId instanceof LocalId)
			return new ArrayList<GrProperty>();
		else
			return getRelationProperties(relationId.getId(), rowIndex);
	}

	private List<GrProperty> getRelationProperties(long relationId, int rowIndex)
	{
		List<GrProperty> props = new ArrayList<GrProperty>();
		Iterator<PropEntry> esIt = this.contentHandler.getPropertiesIterator(relationId, rowIndex, ElemType.RELATION);
		while (esIt.hasNext())
		{
			PropEntry entry = esIt.next();
			GrProperty prop = GrAccess.createProperty(entry.getPropName());
			prop.setValue(this.contentHandler.convertContentValue(entry.getPropValue()));
			GrAccess.setState(prop, SyncState.SYNC);
			props.add(prop);
		}
		return props;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getValues(JcValue jcValue)
	{
		String colKey;
		if (jcValue instanceof JcProperty)
		{
			WriterContext ctxt = new WriterContext();
			ValueWriter.toValueExpression(jcValue, ctxt, ctxt.buffer);
			colKey = ctxt.buffer.toString();
		} else
			colKey = ValueAccess.getName(jcValue);
		List<T> vals = getValueColumns().get(colKey);
		if (vals == null)
		{
			vals = new ArrayList<T>();
			Iterator<RowOrRecord> it = this.contentHandler.getDataIterator();
			while (it.hasNext())
			{ // iterate over rows
				RowOrRecord roc = it.next();
				roc.addValue(colKey, vals);
			}
			getValueColumns().put(colKey, vals);
			getUnresolvedColumns().remove(colKey);
		}
		return vals;
	}

	private Map<String, List<GrNode>> getNodeColumns()
	{
		if (this.nodeColumns == null)
			this.nodeColumns = new HashMap<String, List<GrNode>>();
		return this.nodeColumns;
	}

	private Map<String, List<GrRelation>> getRelationColumns()
	{
		if (this.relationColumns == null)
			this.relationColumns = new HashMap<String, List<GrRelation>>();
		return this.relationColumns;
	}

	private Map<String, List<GrPath>> getPathColumns()
	{
		if (this.pathColumns == null)
			this.pathColumns = new HashMap<String, List<GrPath>>();
		return this.pathColumns;
	}

	@SuppressWarnings("rawtypes")
	private Map<String, List> getValueColumns()
	{
		if (this.valueColumns == null)
			this.valueColumns = new HashMap<String, List>();
		return this.valueColumns;
	}

	private Map<Long, GrNode> getNodesById()
	{
		if (this.nodesById == null)
			this.nodesById = new HashMap<Long, GrNode>();
		return this.nodesById;
	}

	private Map<Long, GrRelation> getRelationsById()
	{
		if (this.relationsById == null)
			this.relationsById = new HashMap<Long, GrRelation>();
		return this.relationsById;
	}

	private List<String> getUnresolvedColumns()
	{
		if (this.unResolvedColumns == null)
		{
			this.unResolvedColumns = new ArrayList<String>();
			this.unResolvedColumns.addAll(this.contentHandler.getColumns());
		}
		return this.unResolvedColumns;
	}

	private NodeRelationListener getNodeRelationListener()
	{
		if (this.nodeRelationListener == null)
			this.nodeRelationListener = new NodeRelationListener();
		return this.nodeRelationListener;
	}

	/**
	 * Update the underlying database with changes made on the graph
	 *
	 * @return a list of errors, which is empty if no errors occurred
	 */
	public List<JcError> store(Map<Long, Integer> elementVersionsMap)
	{
		Map<GrNode, JcNumber> createdNodeToIdMap = new HashMap<GrNode, JcNumber>();
		Map<GrRelation, JcNumber> createdRelationToIdMap = new HashMap<GrRelation, JcNumber>();
		List<ElemId2Query> elemIds2Query = createUpdateQueries(createdNodeToIdMap, createdRelationToIdMap, elementVersionsMap);
		Map<Long, Integer> elemId2ResultIndex = new HashMap<Long, Integer>();
		List<JcQuery> queries = collectQueries(elemIds2Query, elemId2ResultIndex);
		Util.printQueries(queries, QueryToObserve.UPDATE_QUERY, Format.PRETTY_1);
		List<JcError> errors = new ArrayList<JcError>();
		if (queries.size() > 0)
		{
			List<JcQueryResult> results = dbAccess.execute(queries);
			errors.addAll(Util.collectErrors(results));
			if (errors.isEmpty())
			{ // success
				JcError error;
				if ((error = checkLockingError(results, !createdNodeToIdMap.isEmpty() || !createdRelationToIdMap.isEmpty(),
						elemIds2Query)) != null)
				{
					errors.add(error);
					ITransaction tx = this.dbAccess.getTX();
					if (tx != null)
						tx.failure();
				}
				this.handleSyncState(results, createdNodeToIdMap,
						createdRelationToIdMap, error != null, elemId2ResultIndex);
			}
		}
		return errors;
	}

	private List<JcQuery> collectQueries(List<ElemId2Query> elemIds2Query, Map<Long, Integer> elemId2ResultIndex)
	{
		List<JcQuery> ret = new ArrayList<JcQuery>(elemIds2Query.size());
		for (ElemId2Query e2q : elemIds2Query)
		{
			ret.add(e2q.query);
			if (e2q.elemId >= 0)
				elemId2ResultIndex.put(e2q.elemId, e2q.queryIndex);
		}
		return ret;
	}

	private List<ElemId2Query> createUpdateQueries(Map<GrNode, JcNumber> createdNodeToIdMap,
	                                               Map<GrRelation, JcNumber> createdRelationToIdMap, Map<Long, Integer> elementVersionsMap)
	{
		QueryBuilder queryBuilder = new QueryBuilder();
		List<ElemId2Query> elemIds2Query = queryBuilder.buildUpdateAndRemoveQueries(elementVersionsMap);
		JcQuery createQuery = queryBuilder.buildCreateQuery(createdNodeToIdMap,
				createdRelationToIdMap);
		if (createQuery != null)
			elemIds2Query.add(new ElemId2Query(-1, elemIds2Query.size(), createQuery));
		return elemIds2Query;
	}

	/**
	 * in case of error return the element's id, or if not available return -2 in case of change or -3 in case of delete,
	 * in case of ok return -1
	 *
	 * @param results
	 * @param hasCreateQuery
	 * @param elemIds2Query
	 * @return
	 */
	private JcError checkLockingError(List<JcQueryResult> results, boolean hasCreateQuery, List<ElemId2Query> elemIds2Query)
	{
		JcError error = null;
		if (this.lockingStrategy == Locking.OPTIMISTIC)
		{
			JcNumber lockV = new JcNumber("lockV");
			JcNumber nSum = new JcNumber("sum");
			int to = hasCreateQuery ? results.size() - 1 : results.size(); // don't check the result of the create query
			for (int i = 0; i < to; i++)
			{
				ElemId2Query elemId2Query = elemIds2Query.get(i);
				if (elemId2Query.versionSum >= 0 && elemId2Query.elemId < 0)
				{ // delete query
					List<BigDecimal> ires = results.get(i).resultOf(nSum);
					if (ires.size() > 0)
					{
						if (((Number) ires.get(0)).intValue() != elemId2Query.versionSum)
						{
							error = new JcError("JCypher.Locking", "Optimistic locking failed (an element was changed by another client)",
									null);
							break;
						}
					} else
					{ // an element has been deleted
						error = new JcError("JCypher.Locking", "Optimistic locking failed (an element was deleted by another client)",
								null);
						break;
					}
				} else
				{ // change query
					List<BigDecimal> ires = results.get(i).resultOf(lockV);
					if (ires.size() > 0)
					{
						int res = ires.get(0).intValue();
						if (res == -2)
						{
							error = new JcError("JCypher.Locking", "Optimistic locking failed (an element was changed by another client)",
									"element id: " + elemId2Query.elemId);
							break;
						}
					} else
					{ // an element has been deleted
						error = new JcError("JCypher.Locking", "Optimistic locking failed (an element was deleted by another client)",
								"element id: " + elemId2Query.elemId);
						break;
					}
				}
			}
		}
		return error;
	}

	private void handleSyncState(List<JcQueryResult> results,
	                             Map<GrNode, JcNumber> createdNodeToIdMap,
	                             Map<GrRelation, JcNumber> createdRelationToIdMap,
	                             boolean hasErrors, Map<Long, Integer> elemId2ResultIndex)
	{

		List<Long> toRemove = new ArrayList<Long>();
		Iterator<Entry<Long, GrNode>> nbyId = this.getNodesById().entrySet().iterator();
		while (nbyId.hasNext())
		{
			Entry<Long, GrNode> entry = nbyId.next();
			Integer idx = elemId2ResultIndex.get(entry.getKey());
			checkRemovedSetSynchronized(toRemove, entry.getValue(), entry.getKey(),
					hasErrors, idx != null ? results.get(idx) : null);
		}
		for (Long id : toRemove)
		{
			this.getNodesById().remove(id);
		}
		this.changedNodesById = null;

		toRemove.clear();
		Iterator<Entry<Long, GrRelation>> rbyId = this.getRelationsById().entrySet().iterator();
		while (rbyId.hasNext())
		{
			Entry<Long, GrRelation> entry = rbyId.next();
			Integer idx = elemId2ResultIndex.get(entry.getKey());
			checkRemovedSetSynchronized(toRemove, entry.getValue(), entry.getKey(),
					hasErrors, idx != null ? results.get(idx) : null);
		}
		for (Long id : toRemove)
		{
			this.getRelationsById().remove(id);
		}
		this.elementVersions = null;
		this.changedRelationsById = null;

		JcQueryResult createResult = results.get(results.size() - 1);
		Iterator<Entry<GrNode, JcNumber>> nit = createdNodeToIdMap.entrySet().iterator();
		while (nit.hasNext())
		{
			Entry<GrNode, JcNumber> entry = nit.next();
			long id = exchangeGrId(entry.getKey(), entry.getValue(), createResult);
			GrAccess.setToSynchronized(entry.getKey());
			this.getNodesById().put(Long.valueOf(id), entry.getKey());
		}

		Iterator<Entry<GrRelation, JcNumber>> rit = createdRelationToIdMap.entrySet().iterator();
		while (rit.hasNext())
		{
			Entry<GrRelation, JcNumber> entry = rit.next();
			long id = exchangeGrId(entry.getKey(), entry.getValue(), createResult);
			GrAccess.setToSynchronized(entry.getKey());
			this.getRelationsById().put(Long.valueOf(id), entry.getKey());
		}
		this.localElements.clear();

		GrAccess.setGraphState(getGraph(), SyncState.SYNC);

	}

	private void checkRemovedSetSynchronized(List<Long> toRemove,
	                                         PersistableItem item, Long id, boolean hasErrors,
	                                         JcQueryResult qResult)
	{
		if (hasErrors)
		{ // reset version property (can only be with optimistic locking)
			Integer v;
			if (this.elementVersions != null)
			{ // there is at least one changed element
				if (item instanceof GrNode)        // elements to be removed are not added to elementVersions,
					v = this.elementVersions.nodeVersions.get(item);        // their version property is not changed
				else
					v = this.elementVersions.relationVersions.get(item);
				if (v != null)
				{ // for changed elements only
					GrProperty vprop = ((GrPropertyContainer) item).getProperty(lockVersionProperty);
					if (v == -1) // remove version property
						vprop.remove();
					else
					{ // reset version property
						vprop.setValue(v);
						GrAccess.setToSynchronized(vprop);
					}
				}
			}
		} else
		{
			if (writeVersion && lockingStrategy != Locking.OPTIMISTIC)
			{
				// version in db may be different from element version, so sync element version
				if (qResult != null)
				{ // only for changed elements
					JcNumber lockV = new JcNumber("lockV");
					GrProperty vprop = ((GrPropertyContainer) item).getProperty(lockVersionProperty);
					List<BigDecimal> lvr = qResult.resultOf(lockV);
					if (lvr.size() > 0)
					{
						int v = ((Number) lvr.get(0)).intValue();
						int ov = ((Number) vprop.getValue()).intValue();
						if (v != ov)
						{
							vprop.setValue(v);
							GrAccess.setToSynchronized(vprop);
						}
					} else
					{ // has been deleted on db, reset to previous value
						vprop.setValue(((Number) vprop.getValue()).intValue() - 1);
						GrAccess.setToSynchronized(vprop);
					}
				}
			}
			if (GrAccess.getState(item) == SyncState.REMOVED)
				toRemove.add(id);
			else if (GrAccess.getState(item) != SyncState.SYNC)
				GrAccess.setToSynchronized(item);
		}
	}

	/**
	 * @param pc
	 * @param jcId
	 * @param createResult
	 * @return the id
	 */
	private long exchangeGrId(GrPropertyContainer pc, JcNumber jcId,
	                          JcQueryResult createResult)
	{
		List<BigDecimal> bdIds = createResult.resultOf(jcId);
		long id = bdIds.get(0).longValue();
		GrId grId = new GrId(id);
		GrAccess.setGrId(grId, pc);
		return id;
	}

	/**************************************/
	public enum ElemType
	{
		NODE, RELATION
	}

	/**************************************/
	public static class ElementInfo
	{
		private long id;
		private ElemType type;
		private boolean isNull;

		public static ElementInfo parse(String selfString)
		{
			ElementInfo ret = new ElementInfo();
			ret.isNull = false;
			int lidx = selfString.lastIndexOf('/');
			ret.id = Long.parseLong(selfString.substring(lidx + 1));
			String preString = selfString.substring(0, lidx);
			lidx = preString.lastIndexOf('/');
			String typeString;
			if (lidx != -1)
				typeString = preString.substring(lidx + 1);
			else
				typeString = preString;

			if ("node".equals(typeString))
				ret.type = ElemType.NODE;
			else if ("relationship".equals(typeString))
				ret.type = ElemType.RELATION;

			return ret;
		}

		public static ElementInfo fromRecordValue(Value val)
		{
			if (val instanceof ListValue)
				return ElementInfo.fromRecordValue(((ListValue) val).get(0));
			ElementInfo ret = null;
			if (val != null)
			{
				String typName = val.type().name(); // NODE, RELATIONSHIP, NULL
				if ("NODE".equals(typName))
				{
					ret = new ElementInfo();
					ret.isNull = false;
					ret.id = val.asNode().id();
					ret.type = ElemType.NODE;
				} else if ("RELATIONSHIP".equals(typName))
				{
					ret = new ElementInfo();
					ret.isNull = false;
					ret.id = val.asRelationship().id();
					ret.type = ElemType.RELATION;
				} else if ("NULL".equals(typName))
					ret = ElementInfo.nullElement();
			}
			return ret;
		}

		public static ElementInfo nullElement()
		{
			ElementInfo ret = new ElementInfo();
			ret.isNull = true;
			return ret;
		}
	}

	/**************************************/
	public static class RelationInfo
	{
		private long startNodeId;
		private long endNodeId;

		public static RelationInfo parse(String startString, String endString)
		{
			RelationInfo ret = new RelationInfo();
			ret.startNodeId = ret.parseId(startString);
			ret.endNodeId = ret.parseId(endString);
			return ret;
		}

		public static RelationInfo fromRecordValue(Value val)
		{
			if (val instanceof ListValue)
				return RelationInfo.fromRecordValue(((ListValue) val).get(0));
			RelationInfo ret = null;
			if (val != null)
			{
				String typName = val.type().name(); // must be: RELATIONSHIP
				if ("RELATIONSHIP".equals(typName))
				{
					ret = new RelationInfo();
					ret.startNodeId = val.asRelationship().startNodeId();
					ret.endNodeId = val.asRelationship().endNodeId();
				}
			}
			return ret;
		}

		private long parseId(String str)
		{
			int lidx = str.lastIndexOf('/');
			return Long.parseLong(str.substring(lidx + 1));
		}
	}

	/**************************************/
	public static class PathInfo
	{
		private long startNodeId;
		private long endNodeId;
		private List<Long> relationIds;
		private Object contentObject;

		public PathInfo(long startNodeId, long endNodeId, List<Long> relationIds,
		                Object userObject)
		{
			super();
			this.startNodeId = startNodeId;
			this.endNodeId = endNodeId;
			this.relationIds = relationIds;
			this.contentObject = userObject;
		}

		public Object getContentObject()
		{
			return contentObject;
		}

		public void setContentObject(Object userObject)
		{
			this.contentObject = userObject;
		}

	}

	/**************************************/
	public static abstract class AContentHandler
	{
		public abstract List<String> getColumns();

		public abstract int getColumnIndex(String colKey);

		public abstract Iterator<RowOrRecord> getDataIterator();

		public abstract Object convertContentValue(Object val);

		public abstract Iterator<PropEntry> getPropertiesIterator(long id, int rowIndex, ElemType typ);

		public abstract String getRelationType(long relationId, int rowIndex);

		public abstract List<GrLabel> getNodeLabels(long nodeId, int rowIndex);

		/*********************************************/
		public static class PropEntry
		{
			private String propName;
			private Object propValue;

			public PropEntry(String propName, Object propValue)
			{
				super();
				this.propName = propName;
				this.propValue = propValue;
			}

			public String getPropName()
			{
				return propName;
			}

			public Object getPropValue()
			{
				return propValue;
			}
		}

		/*********************************************/
		public abstract class RowOrRecord
		{
			public abstract ElementInfo getElementInfo(String colKey);

			public abstract RelationInfo getRelationInfo(String colKey);

			public abstract PathInfo getPathInfo(String colKey);

			public abstract long gePathtNodeIdAt(PathInfo pathInfo, int index);

			public abstract <T> void addValue(String colKey, List<T> vals);
		}
	}

	/**************************************/
	private class NodeRelationListener implements ChangeListener
	{

		@Override
		public void changed(Object theChanged, SyncState oldState,
		                    SyncState newState)
		{
			boolean possiblyReturnedToSync = false;

			if (newState == SyncState.CHANGED || newState == SyncState.REMOVED)
			{
				if (theChanged instanceof GrNode)
				{
					if (changedNodesById == null)
						changedNodesById = new HashMap<Long, GrNode>();
					changedNodesById.put(((GrNode) theChanged).getId(), (GrNode) theChanged);
				} else if (theChanged instanceof GrRelation)
				{
					if (changedRelationsById == null)
						changedRelationsById = new HashMap<Long, GrRelation>();
					changedRelationsById.put(((GrRelation) theChanged).getId(), (GrRelation) theChanged);
				}
				if (GrAccess.getGraphState(getGraph()) == SyncState.SYNC)
					GrAccess.setGraphState(getGraph(), SyncState.CHANGED);
			} else if (newState == SyncState.SYNC)
			{
				if (theChanged instanceof GrNode)
				{
					if (changedNodesById != null)
						changedNodesById.remove(((GrNode) theChanged).getId());
				} else if (theChanged instanceof GrRelation)
				{
					if (changedRelationsById != null)
						changedRelationsById.remove(((GrRelation) theChanged).getId());
				}
			} else if (newState == SyncState.NEW)
			{
				if (GrAccess.getGraphState(getGraph()) == SyncState.SYNC)
					GrAccess.setGraphState(getGraph(), SyncState.CHANGED);
			} else if (newState == SyncState.NEW_REMOVED)
			{
				if (theChanged instanceof GrNode)
				{
					localElements.removeNode(((GrNode) theChanged).getId());
				} else if (theChanged instanceof GrRelation)
				{
					localElements.removeRelation(((GrRelation) theChanged).getId());
				}
			}

			if (newState == SyncState.SYNC || newState == SyncState.NEW_REMOVED)
			{
				if ((changedNodesById == null || changedNodesById.size() == 0) &&
						(changedRelationsById == null || changedRelationsById.size() == 0) &&
						localElements.isEmpty())
					possiblyReturnedToSync = true;
			}

			if (possiblyReturnedToSync && GrAccess.getGraphState(getGraph()) == SyncState.CHANGED)
			{
				GrAccess.setGraphState(getGraph(), SyncState.SYNC);
			}
		}
	}

	/**************************************/
	private class QueryBuilder
	{

		/**
		 * @param createdNodeToIdMap
		 * @return a Query to create elements
		 */
		JcQuery buildCreateQuery(Map<GrNode, JcNumber> createdNodeToIdMap,
		                         Map<GrRelation, JcNumber> createdRelationToIdMap)
		{
			List<GrNode2JcNode> nodesToCreate = new ArrayList<GrNode2JcNode>();
			List<IClause> createNodeClauses = new ArrayList<IClause>();
			Map<GrNode, JcNode> localNodeMap = new HashMap<GrNode, JcNode>();
			for (GrNode node : localElements.getLocalNodes())
			{
				addCreateNodeClause(node, createNodeClauses,
						localNodeMap, nodesToCreate);
			}

			List<GrRelation2JcRelation> relationsToCreate = new ArrayList<GrRelation2JcRelation>();
			List<IClause> startNodeClauses = new ArrayList<IClause>();
			List<IClause> createRelationClauses = new ArrayList<IClause>();
			Map<GrNode, JcNode> dbNodeMap = new HashMap<GrNode, JcNode>();
			for (GrRelation relation : localElements.getLocalRelations())
			{
				addCreateRelationClause(relation, createRelationClauses,
						localNodeMap, dbNodeMap, startNodeClauses, relationsToCreate);
			}
			List<IClause> clauses = startNodeClauses;
			clauses.addAll(createNodeClauses);
			clauses.addAll(createRelationClauses);
			for (GrNode2JcNode grn2jcn : nodesToCreate)
			{
				JcNumber nid = new JcNumber("NID_".concat(ValueAccess.getName(grn2jcn.jcNode)));
				createdNodeToIdMap.put(grn2jcn.grNode, nid);
				clauses.add(RETURN.value(grn2jcn.jcNode.id()).AS(nid));
			}
			for (GrRelation2JcRelation grr2jcr : relationsToCreate)
			{
				JcNumber rid = new JcNumber("RID_".concat(ValueAccess.getName(grr2jcr.jcRelation)));
				createdRelationToIdMap.put(grr2jcr.grRelation, rid);
				clauses.add(RETURN.value(grr2jcr.jcRelation.id()).AS(rid));
			}

			JcQuery ret = null;
			if (clauses.size() > 0)
			{
				IClause[] clausesArray = clauses.toArray(new IClause[clauses.size()]);
				ret = new JcQuery();
				ret.setClauses(clausesArray);
			}
			return ret;
		}

		List<ElemId2Query> buildUpdateAndRemoveQueries(Map<Long, Integer> elementVersionsMap)
		{
			List<ElemId2Query> ret = new ArrayList<ElemId2Query>();
			List<GrPropertyContainer> removedNodes = new ArrayList<GrPropertyContainer>();
			if (changedNodesById != null)
			{
				Iterator<GrNode> nit = changedNodesById.values().iterator();
				while (nit.hasNext())
				{
					GrNode node = nit.next();
					SyncState state = GrAccess.getState(node);
					if (state == SyncState.CHANGED)
					{
						ret.add(
								new ElemId2Query(node.getId(), ret.size(),
										buildChangedNodeOrRelationQuery(node, elementVersionsMap)));
					} else if (state == SyncState.REMOVED)
					{
						removedNodes.add(node);
					}
				}
			}

			List<GrPropertyContainer> removedRelations = new ArrayList<GrPropertyContainer>();
			if (changedRelationsById != null)
			{
				Iterator<GrRelation> rit = changedRelationsById.values().iterator();
				while (rit.hasNext())
				{
					GrRelation relation = rit.next();
					SyncState state = GrAccess.getState(relation);
					if (state == SyncState.CHANGED)
					{
						ret.add(
								new ElemId2Query(relation.getId(), ret.size(),
										buildChangedNodeOrRelationQuery(relation, elementVersionsMap)));
					} else if (state == SyncState.REMOVED)
					{
						removedRelations.add(relation);
					}
				}
			}

			if (removedRelations.size() > 0)
			{
				ElemId2Query elemId2Query = new ElemId2Query(-1, ret.size(), null);
				buildRemovedNodeOrRelationQuery(removedRelations, elemId2Query);
				ret.add(elemId2Query);
			}
			if (removedNodes.size() > 0)
			{
				ElemId2Query elemId2Query = new ElemId2Query(-1, ret.size(), null);
				buildRemovedNodeOrRelationQuery(removedNodes, elemId2Query);
				ret.add(elemId2Query);
			}

			return ret;
		}

		private void addCreateNodeClause(GrNode node,
		                                 List<IClause> clauses, Map<GrNode, JcNode> localNodeMap,
		                                 List<GrNode2JcNode> nodesToCreate)
		{
			String nm = "ln_".concat(String.valueOf(clauses.size()));
			JcNode n = new JcNode(nm);
			nodesToCreate.add(new GrNode2JcNode(node, n));
			Node create = CREATE.node(n);
			for (GrLabel label : node.getLabels())
			{
				create = create.label(label.getName());
			}
			for (GrProperty prop : node.getProperties())
			{
				create = create.property(prop.getName()).value(prop.getValue());
			}
			if (writeVersion || lockingStrategy == Locking.OPTIMISTIC)
				create = create.property(ResultHandler.lockVersionProperty).value(0);
			clauses.add(create);
			localNodeMap.put(node, n);
		}

		private void addCreateRelationClause(GrRelation relation,
		                                     List<IClause> createRelationClauses,
		                                     Map<GrNode, JcNode> localNodeMap, Map<GrNode, JcNode> dbNodeMap,
		                                     List<IClause> startNodeClauses, List<GrRelation2JcRelation> relationsToCreate)
		{
			String nm = "lr_".concat(String.valueOf(createRelationClauses.size()));
			JcRelation r = new JcRelation(nm);
			relationsToCreate.add(new GrRelation2JcRelation(relation, r));
			GrNode sNode = relation.getStartNode();
			GrNode eNode = relation.getEndNode();
			JcNode sn = getNode(sNode, localNodeMap, dbNodeMap, startNodeClauses);
			JcNode en = getNode(eNode, localNodeMap, dbNodeMap, startNodeClauses);
			Relation create = CREATE.node(sn).relation(r).out();
			if (relation.getType() != null)
				create = create.type(relation.getType());
			for (GrProperty prop : relation.getProperties())
			{
				create = create.property(prop.getName()).value(prop.getValue());
			}
			if (writeVersion || lockingStrategy == Locking.OPTIMISTIC)
				create = create.property(ResultHandler.lockVersionProperty).value(0);
			createRelationClauses.add(create.node(en));
		}

		private JcNode getNode(GrNode grNode, Map<GrNode, JcNode> localNodeMap,
		                       Map<GrNode, JcNode> dbNodeMap, List<IClause> startNodeClauses)
		{
			GrId grId = GrAccess.getGrId(grNode);
			JcNode n;
			if (grId instanceof LocalId)
			{
				n = localNodeMap.get(grNode);
			} else
			{
				n = dbNodeMap.get(grNode);
				if (n == null)
				{
					String nm = "rn_".concat(String.valueOf(startNodeClauses.size()));
					n = new JcNode(nm);
					StartPoint start = START.node(n).byId(grId.getId());
					startNodeClauses.add(start);
					dbNodeMap.put(grNode, n);
				}
			}
			return n;
		}

		private void buildRemovedNodeOrRelationQuery(List<GrPropertyContainer> elements, ElemId2Query elemId2Query)
		{
			List<IClause> clauses = new ArrayList<IClause>();
			List<IClause> removeClauses = new ArrayList<IClause>();
			List<String> elemNames = new ArrayList<String>(elements.size());
			for (int i = 0; i < elements.size(); i++)
			{
				String nm = "elem_".concat(String.valueOf(i));
				elemNames.add(nm);
				if (elements.get(0) instanceof GrNode)
				{
					JcNode elem = new JcNode(nm);
					//clauses.add(START.node(elem).byId(elements.get(i).getId()));

					// use OPTIONAL_MATCH to be tolerant for removed elements
					clauses.add(OPTIONAL_MATCH.node(elem));
					clauses.add(WHERE.valueOf(elem.id()).EQUALS(elements.get(i).getId()));
				} else if (elements.get(0) instanceof GrRelation)
				{
					JcRelation elem = new JcRelation(nm);
					//clauses.add(START.relation(elem).byId(elements.get(i).getId()));

					// use OPTIONAL_MATCH to be tolerant for removed elements
					clauses.add(OPTIONAL_MATCH.node().relation(elem).out().node());
					clauses.add(WHERE.valueOf(elem.id()).EQUALS(elements.get(i).getId()));
				}
			}

			LockUtil.Removes removes = new LockUtil.Removes();
			int idx = 0;
			for (String nm : elemNames)
			{
				JcElement elem = null;
				if (elements.get(0) instanceof GrNode)
					elem = new JcNode(nm);
				else if (elements.get(0) instanceof GrRelation)
					elem = new JcRelation(nm);
				int nodeVersion = -1;
				GrProperty prop = elements.get(idx).getProperty(lockVersionProperty);
				if (prop != null)
					nodeVersion = ((Number) prop.getValue()).intValue();

				if (lockingStrategy == Locking.OPTIMISTIC)
				{
					LockUtil.calcRemoves(removes, elem, nodeVersion);
				}

				removeClauses.add(DO.DELETE(elem));
				idx++;
			}

			if (removes.getWithClauses() != null)
			{
				JcNumber nSum = new JcNumber("sum");
				removes.getWithClauses().add(WITH.value(removes.getSum()).AS(nSum));
				clauses.addAll(removes.getWithClauses());
				JcValue x = new JcValue("x");
				// conditional remove in case of Locking.OPTIONAL
				IClause clause = FOR_EACH.element(x).IN(C.CREATE(new IClause[]{
						CASE.result(),
						WHEN.valueOf(nSum).EQUALS(removes.getVersionSum()),
						NATIVE.cypher("[1]"),
						ELSE.perform(),
						NATIVE.cypher("[]"),
						END.caseXpr()
				})).DO(removeClauses.toArray(new IClause[removeClauses.size()]));
				clauses.add(clause);
				clauses.add(RETURN.value(nSum));
			} else
				clauses.addAll(removeClauses);

			IClause[] clausesArray = clauses.toArray(new IClause[clauses.size()]);
			JcQuery query = new JcQuery();
			//query.setExtractParams(false);
			query.setClauses(clausesArray);
			elemId2Query.versionSum = removes.getVersionSum();
			elemId2Query.query = query;
		}

		private JcQuery buildChangedNodeOrRelationQuery(GrPropertyContainer element,
		                                                Map<Long, Integer> elementVersionsMap)
		{
			int nodeVersion = -1;
			if (elementVersionsMap != null)
			{ // only when coming from DomainAccess
				// with Locking.OTIMISTIC
				Integer v = elementVersionsMap.get(element.getId());
				if (v != null)
					nodeVersion = v;
			} else
			{
				GrProperty prop = element.getProperty(lockVersionProperty);
				if (prop != null)
					nodeVersion = ((Number) prop.getValue()).intValue();
			}

			handleVersionProperty(element, nodeVersion);
			List<IClause> clauses = new ArrayList<IClause>();
			JcElement elem = null;
			if (element instanceof GrNode)
				elem = new JcNode("elem");
			else if (element instanceof GrRelation)
				elem = new JcRelation("elem");
			List<IClause> startClause = buildStartClause(element);
			clauses.addAll(buildChangedPropertiesClauses(element));
			if (writeVersion || lockingStrategy == Locking.OPTIMISTIC)
				clauses.add(
						DO.SET(elem.property(lockVersionProperty)).byExpression(elem.numberProperty(lockVersionProperty).plus(1)));
			clauses.addAll(buildChangedLabelsClauses(element));
			IClause[] clausesArray;
			JcNumber lockV = new JcNumber("lockV");
			if (lockingStrategy == Locking.OPTIMISTIC && nodeVersion >= 0)
			{
				clausesArray = clauses.toArray(new IClause[clauses.size()]);
				clauses.clear();
				clauses.addAll(startClause);
				JcValue x = new JcValue("x");
				IClause clause = FOR_EACH.element(x).IN(C.CREATE(new IClause[]{
						CASE.result(),
						WHEN.valueOf(elem.property(lockVersionProperty)).NOT_EQUALS(nodeVersion),
						NATIVE.cypher("[1]"),
						ELSE.perform(),
						NATIVE.cypher("[]"),
						END.caseXpr()
				})).DO().SET(elem.property(lockVersionProperty)).to(-2);
				clauses.add(clause);
				clause = FOR_EACH.element(x).IN(C.CREATE(new IClause[]{
						CASE.result(),
						WHEN.valueOf(elem.property(lockVersionProperty)).EQUALS(nodeVersion),
						NATIVE.cypher("[1]"),
						ELSE.perform(),
						NATIVE.cypher("[]"),
						END.caseXpr()
				})).DO(clausesArray);
				clauses.add(clause);
			} else
				clauses.addAll(0, startClause);
			if (writeVersion || lockingStrategy == Locking.OPTIMISTIC)
			{
				JcNumber elemId = new JcNumber("elemId");
				clauses.add(RETURN.value(elem.property(lockVersionProperty)).AS(lockV));
				clauses.add(RETURN.value(elem.id()).AS(elemId));
			}
			clausesArray = clauses.toArray(new IClause[clauses.size()]);
			JcQuery query = new JcQuery();
			query.setClauses(clausesArray);
			return query;
		}

		private Collection<? extends IClause> buildChangedLabelsClauses(
				GrPropertyContainer element)
		{
			List<IClause> ret = new ArrayList<IClause>();
			if (element instanceof GrNode)
			{
				GrNode node = (GrNode) element;
				List<GrLabel> modified = GrAccess.getModifiedLabels(node);
				Iterator<GrLabel> lit = modified.iterator();
				while (lit.hasNext())
				{
					GrLabel lab = lit.next();
					SyncState state = GrAccess.getState(lab);
					JcNode elem = new JcNode("elem");
					IClause c = null;
					// a label can only be created and added or it can be removed
					// but a label can never be changed
					if (state == SyncState.NEW)
					{
						c = DO.SET(elem.label(lab.getName()));
					} else if (state == SyncState.REMOVED)
					{
						c = DO.REMOVE(elem.label(lab.getName()));
					}
					ret.add(c);
				}
			}
			return ret;
		}

		private Collection<? extends IClause> buildChangedPropertiesClauses(
				GrPropertyContainer element)
		{
			List<IClause> ret = new ArrayList<IClause>();
			List<GrProperty> modified = GrAccess.getModifiedProperties(element);
			Iterator<GrProperty> pit = modified.iterator();
			while (pit.hasNext())
			{
				GrProperty prop = pit.next();
				if (!prop.getName().equals(lockVersionProperty))
				{ // version property is handled differently
					SyncState state = GrAccess.getState(prop);
					JcElement elem = null;
					if (element instanceof GrNode)
						elem = new JcNode("elem");
					else
						elem = new JcRelation("elem");

					IClause c = null;
					if (state == SyncState.CHANGED || state == SyncState.NEW)
					{
						Object propValue = prop.getValue();
						if (propValue != null)
							c = DO.SET(elem.property(prop.getName())).to(prop.getValue());
						else
							c = DO.SET(elem.property(prop.getName())).toNull();
					} else if (state == SyncState.REMOVED)
					{
						c = DO.REMOVE(elem.property(prop.getName()));
					}
					ret.add(c);
				}
			}
			return ret;
		}

		private List<IClause> buildStartClause(GrPropertyContainer element)
		{
			List<IClause> ret = new ArrayList<IClause>();
			JcElement elem;
			if (element instanceof GrNode)
			{
				elem = new JcNode("elem");
				//ret = START.node(elem).byId(id);
				ret.add(OPTIONAL_MATCH.node((JcNode) elem));
			} else
			{
				elem = new JcRelation("elem");
				//ret = START.relation(elem).byId(id);
				ret.add(OPTIONAL_MATCH.node().relation((JcRelation) elem).out().node());
			}
			ret.add(WHERE.valueOf(elem.id()).EQUALS(element.getId()));
			return ret;
		}

		private void handleVersionProperty(GrPropertyContainer element, int curVersion)
		{
			if (writeVersion || lockingStrategy == Locking.OPTIMISTIC)
			{
				if (elementVersions == null)
					elementVersions = new ElementVersions();
				int oldVersion = curVersion;
				GrProperty prop = element.getProperty(ResultHandler.lockVersionProperty);
				if (prop != null)
					prop.setValue(oldVersion + 1);
				else
					prop = element.addProperty(ResultHandler.lockVersionProperty, oldVersion + 1);

				if (element instanceof GrNode)
					elementVersions.nodeVersions.put(element, oldVersion);
				else if (element instanceof GrRelation)
					elementVersions.relationVersions.put(element, oldVersion);
			}
		}

		/*************************************/
		private class GrNode2JcNode
		{
			private GrNode grNode;
			private JcNode jcNode;

			private GrNode2JcNode(GrNode grNode, JcNode jcNode)
			{
				super();
				this.grNode = grNode;
				this.jcNode = jcNode;
			}
		}

		/*************************************/
		private class GrRelation2JcRelation
		{
			private GrRelation grRelation;
			private JcRelation jcRelation;

			private GrRelation2JcRelation(GrRelation grRelation, JcRelation jcRelation)
			{
				super();
				this.grRelation = grRelation;
				this.jcRelation = jcRelation;
			}
		}
	}

	/**************************************/
	private class ElementVersions
	{

		private Map<GrPropertyContainer, Integer> nodeVersions;
		private Map<GrPropertyContainer, Integer> relationVersions;
		private ElementVersions()
		{
			super();
			this.nodeVersions = new HashMap<GrPropertyContainer, Integer>();
			this.relationVersions = new HashMap<GrPropertyContainer, Integer>();
		}
	}

	/**************************************/
	private class ElemId2Query
	{
		private long elemId;
		private int queryIndex;
		private int versionSum;
		private JcQuery query;

		private ElemId2Query(long elemId, int queryIndex, JcQuery query)
		{
			super();
			this.elemId = elemId;
			this.queryIndex = queryIndex;
			this.query = query;
			this.versionSum = -1;
		}

	}

	/**************************************/
	public class LocalElements
	{
		private LocalIdBuilder nodeIdBuilder;
		private LocalIdBuilder relationIdBuilder;

		private Map<Long, GrNode> localNodesById;
		private Map<Long, GrRelation> localRelationsById;

		public GrNode createNode()
		{
			if (this.nodeIdBuilder == null)
				this.nodeIdBuilder = new LocalIdBuilder();
			LocalId lid = new LocalId(this.nodeIdBuilder.getId());
			GrNode node = GrAccess.createNode(ResultHandler.this, lid, -1);
			if (this.localNodesById == null)
				this.localNodesById = new HashMap<Long, GrNode>();
			GrAccess.addChangeListener(getNodeRelationListener(), node);
			this.localNodesById.put(lid.getId(), node);
			GrAccess.notifyState(node);
			return node;
		}

		public GrRelation createRelation(String type, GrNode startNode, GrNode endNode)
		{
			if (this.relationIdBuilder == null)
				this.relationIdBuilder = new LocalIdBuilder();
			LocalId lid = new LocalId(this.relationIdBuilder.getId());
			GrRelation relation = GrAccess.createRelation(ResultHandler.this, lid,
					GrAccess.getGrId(startNode), GrAccess.getGrId(endNode), type);
			if (this.localRelationsById == null)
				this.localRelationsById = new HashMap<Long, GrRelation>();
			GrAccess.addChangeListener(getNodeRelationListener(), relation);
			this.localRelationsById.put(lid.getId(), relation);
			GrAccess.notifyState(relation);
			return relation;
		}

		private GrNode getNode(long id)
		{
			if (this.localNodesById != null)
				return this.localNodesById.get(id);
			return null;
		}

		private GrRelation getRelation(long id)
		{
			if (this.localRelationsById != null)
				return this.localRelationsById.get(id);
			return null;
		}

		private void removeNode(long id)
		{
			if (this.localNodesById != null)
				this.localNodesById.remove(id);
		}

		private void removeRelation(long id)
		{
			if (this.localRelationsById != null)
				this.localRelationsById.remove(id);
		}

		private List<GrNode> getLocalNodes()
		{
			List<GrNode> ret = new ArrayList<GrNode>();
			if (this.localNodesById != null)
			{
				for (GrNode node : this.localNodesById.values())
				{
					ret.add(node);
				}
			}
			return ret;
		}

		private List<GrRelation> getLocalRelations()
		{
			List<GrRelation> ret = new ArrayList<GrRelation>();
			if (this.localRelationsById != null)
			{
				for (GrRelation relation : this.localRelationsById.values())
				{
					ret.add(relation);
				}
			}
			return ret;
		}

		private void clear()
		{
			this.nodeIdBuilder = null;
			this.localNodesById = null;
			this.relationIdBuilder = null;
			this.localRelationsById = null;
		}

		public boolean isEmpty()
		{
			return (this.localNodesById == null || this.localNodesById.size() == 0) &&
					(this.localRelationsById == null || this.localRelationsById.size() == 0);
		}
	}
}
