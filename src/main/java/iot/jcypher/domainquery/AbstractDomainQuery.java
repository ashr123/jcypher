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

package iot.jcypher.domainquery;

import iot.jcypher.domain.IDomainAccess;
import iot.jcypher.domain.genericmodel.DomainObject;
import iot.jcypher.domain.genericmodel.InternalAccess;
import iot.jcypher.domain.internal.DomainAccess.InternalDomainAccess;
import iot.jcypher.domainquery.api.*;
import iot.jcypher.domainquery.ast.*;
import iot.jcypher.domainquery.ast.ConcatenateExpression.Concatenator;
import iot.jcypher.domainquery.internal.*;
import iot.jcypher.query.values.JcProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractDomainQuery
{

	protected QueryExecutor queryExecutor;
	private IASTObjectsContainer astObjectsContainer;
	private IntAccess intAccess;

	public AbstractDomainQuery(IDomainAccess domainAccess)
	{
		super();
		this.queryExecutor = new QueryExecutor(domainAccess);
		this.astObjectsContainer = this.queryExecutor;
	}

	void recordQuery(RecordedQuery rq)
	{
		this.queryExecutor.recordQuery(rq, this);
	}

	void replayQuery(ReplayedQueryContext rqc)
	{
		this.queryExecutor.replayQuery(rqc);
	}

	/**
	 * Create a match for a specific type of domain objects
	 *
	 * @param domainObjectType
	 * @return a DomainObjectMatch for a specific type of domain objects
	 */
	<T> DomainObjectMatch<T> createMatchInternal(Class<T> domainObjectType)
	{
		DomainObjectMatch<T> ret = APIAccess.createDomainObjectMatch(domainObjectType,
				this.queryExecutor.getDomainObjectMatches().size(),
				this.queryExecutor.getMappingInfo());
		this.queryExecutor.getDomainObjectMatches().add(ret);
		return ret;
	}

	/**
	 * Create a match from a DomainObjectMatch specified in the context of another query
	 *
	 * @param domainObjectMatch a match specified in the context of another query
	 * @return a DomainObjectMatch
	 */
	@SuppressWarnings("unchecked")
	public <T> DomainObjectMatch<T> createMatchFrom(DomainObjectMatch<T> domainObjectMatch)
	{
		DomainObjectMatch<T> ret;
		FromPreviousQueryExpression pqe;
		DomainObjectMatch<?> match;
		DomainObjectMatch<?> delegate = APIAccess.getDelegate(domainObjectMatch);
		if (delegate != null)
		{ // generic model
			DomainObjectMatch<?> newDelegate = APIAccess.createDomainObjectMatch(delegate,
					this.queryExecutor.getDomainObjectMatches().size(),
					this.queryExecutor.getMappingInfo());
			this.queryExecutor.getDomainObjectMatches().add(newDelegate);
			pqe = new FromPreviousQueryExpression(
					newDelegate, delegate);
			ret = (DomainObjectMatch<T>) APIAccess.createDomainObjectMatch(DomainObject.class, newDelegate);
			match = newDelegate;
		} else
		{
			ret = APIAccess.createDomainObjectMatch(domainObjectMatch,
					this.queryExecutor.getDomainObjectMatches().size(),
					this.queryExecutor.getMappingInfo());
			this.queryExecutor.getDomainObjectMatches().add(ret);
			pqe = new FromPreviousQueryExpression(
					ret, domainObjectMatch);
			match = ret;
		}
		this.queryExecutor.addAstObject(pqe);
		QueryRecorder.recordAssignment(this, "createMatchFrom", match,
				QueryRecorder.reference(domainObjectMatch));
		return ret;
	}

	/**
	 * Create a match for a domain object which was retrieved by another query
	 *
	 * @param domainObject a domain object which was retrieved by another query
	 * @return a DomainObjectMatch
	 */
	@SuppressWarnings("unchecked")
	public <T> DomainObjectMatch<T> createMatchFor(T domainObject)
	{
		DomainObjectMatch<T> ret;
		if (domainObject.getClass().equals(DomainObject.class))
		{ // generic model
			List<DomainObject> source = new ArrayList<DomainObject>();
			source.add((DomainObject) domainObject);
			String typeName = ((DomainObject) domainObject).getDomainObjectType().getName();
			ret = (DomainObjectMatch<T>) createGenMatchForInternal(source, typeName);
		} else
		{
			List<T> source = new ArrayList<T>();
			source.add(domainObject);
			ret = this.createMatchForInternal(source, (Class<T>) domainObject.getClass());
		}
		DomainObjectMatch<?> delegate = APIAccess.getDelegate(ret);
		DomainObjectMatch<?> match = delegate != null ? delegate : ret;
		QueryRecorder.recordAssignment(this, "createMatchFor", match,
				QueryRecorder.reference(domainObject));
		return ret;
	}

	/**
	 * Create a match for a list of domain objects which were retrieved by another query
	 *
	 * @param domainObjects    a list of domain objects which were retrieved by another query
	 * @param domainObjectType the type of those domain objects
	 * @return a DomainObjectMatch
	 */
	protected <T> DomainObjectMatch<T> createMatchForInternal(List<T> domainObjects,
	                                                          Class<T> domainObjectType)
	{
		DomainObjectMatch<T> ret = APIAccess.createDomainObjectMatch(domainObjectType,
				this.queryExecutor.getDomainObjectMatches().size(),
				this.queryExecutor.getMappingInfo());
		this.queryExecutor.getDomainObjectMatches().add(ret);
		FromPreviousQueryExpression pqe = new FromPreviousQueryExpression(
				ret, domainObjects);
		this.queryExecutor.addAstObject(pqe);
		return ret;
	}

	/**
	 * Create a match for a list of domain objects which were retrieved by another query.
	 * <br/>The match will be part of a query performed on a generic domain model.
	 *
	 * @param domainObjects        a list of domain objects which were retrieved by another query
	 * @param domainObjectTypeName the type name of those domain objects
	 * @return a DomainObjectMatch
	 */
	protected DomainObjectMatch<DomainObject> createGenMatchForInternal(List<DomainObject> domainObjects,
	                                                                    String domainObjectTypeName)
	{
		InternalDomainAccess iAccess = this.queryExecutor.getMappingInfo().getInternalDomainAccess();
		try
		{
			iAccess.loadDomainInfoIfNeeded();
			List<Object> dobjs = new ArrayList<Object>();
			for (DomainObject dobj : domainObjects)
			{
				dobjs.add(InternalAccess.getRawObject(dobj));
			}
			@SuppressWarnings("rawtypes")
			Class clazz = iAccess.getClassForName(domainObjectTypeName);
			@SuppressWarnings("unchecked")
			DomainObjectMatch<?> delegate = createMatchForInternal(dobjs, clazz);
			DomainObjectMatch<DomainObject> ret = APIAccess.createDomainObjectMatch(DomainObject.class, delegate);
			return ret;
		} catch (Throwable e)
		{
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}
	}

	/**
	 * Get or create, if not exists, a query parameter.
	 *
	 * @param name of the parameter
	 * @return a query parameter
	 */
	public Parameter parameter(String name)
	{
		return this.queryExecutor.parameter(name);
	}

	public List<String> getParameterNames()
	{
		return new ArrayList<String>(this.queryExecutor.getParameterNames());
	}

	/**
	 * Start formulating a predicate expression.
	 * A predicate expression yields a boolean value.
	 * <br/>Takes an expression like 'person.stringAttribute("name")', yielding an attribute,
	 * <br/>e.g. WHERE(person.stringAttribute("name")).EQUALS(...)
	 *
	 * @param value the value(expression) to formulate the predicate expression upon.
	 * @return
	 */
	public BooleanOperation WHERE(IPredicateOperand1 value)
	{
		IPredicateOperand1 pVal = value;
		if (value instanceof DomainObjectMatch<?>)
		{
			DomainObjectMatch<?> delegate = APIAccess.getDelegate((DomainObjectMatch<?>) value);
			if (delegate != null) // generic model
				pVal = delegate;
		}
		PredicateExpression pe = new PredicateExpression(pVal, this.astObjectsContainer);
		this.astObjectsContainer.addAstObject(pe);
		BooleanOperation ret = APIAccess.createBooleanOperation(pe);
		QueryRecorder.recordInvocation(this, "WHERE", ret, QueryRecorder.placeHolder(pVal));
		return ret;
	}

	/**
	 * Or two predicate expressions
	 */
	public TerminalResult OR()
	{
		ConcatenateExpression ce = new ConcatenateExpression(Concatenator.OR);
		this.astObjectsContainer.addAstObject(ce);
		TerminalResult ret = APIAccess.createTerminalResult(ce);
		QueryRecorder.recordInvocation(this, "OR", ret);
		return ret;
	}

	/**
	 * Open a block, encapsulating predicate expressions
	 */
	public TerminalResult BR_OPEN()
	{
		ConcatenateExpression ce = new ConcatenateExpression(Concatenator.BR_OPEN);
		this.astObjectsContainer.addAstObject(ce);
		TerminalResult ret = APIAccess.createTerminalResult(ce);
		QueryRecorder.recordInvocation(this, "BR_OPEN", ret);
		return ret;
	}

	/**
	 * Close a block, encapsulating predicate expressions
	 */
	public TerminalResult BR_CLOSE()
	{
		ConcatenateExpression ce = new ConcatenateExpression(Concatenator.BR_CLOSE);
		this.astObjectsContainer.addAstObject(ce);
		TerminalResult ret = APIAccess.createTerminalResult(ce);
		QueryRecorder.recordInvocation(this, "BR_CLOSE", ret);
		return ret;
	}

	/**
	 * Define an order on a set of domain objects which are specified by
	 * a DomainObjectMatch in the context of the domain query.
	 *
	 * @param toOrder the DomainObjectMatch
	 *                specifying the set of domain objects which should be ordered
	 * @return
	 */
	public Order ORDER(DomainObjectMatch<?> toOrder)
	{
		DomainObjectMatch<?> delegate = APIAccess.getDelegate(toOrder);
		DomainObjectMatch<?> match = delegate != null ? delegate : toOrder;
		OrderExpression oe = this.queryExecutor.getOrderFor(match);
		Order ret = APIAccess.createOrder(oe);
		QueryRecorder.recordInvocation(this, "ORDER", ret, QueryRecorder.placeHolder(match));
		return ret;
	}

	/**
	 * Start traversing the graph of domain objects.
	 *
	 * @param start a DomainObjectMatch form where to start the traversal.
	 * @return
	 */
	public Traverse TRAVERSE_FROM(DomainObjectMatch<?> start)
	{
		DomainObjectMatch<?> delegate = APIAccess.getDelegate(start);
		DomainObjectMatch<?> match = delegate != null ? delegate : start;
		TraversalExpression te = new TraversalExpression(match, this.queryExecutor);
		this.queryExecutor.addAstObject(te);
		Traverse ret = APIAccess.createTraverse(te);
		QueryRecorder.recordInvocation(this, "TRAVERSE_FROM", ret, QueryRecorder.placeHolder(match));
		return ret;
	}

	/**
	 * Select domain objects out of a set of other domain objects.
	 *
	 * @param start with a DomainObjectMatch representing the initial set.
	 * @return
	 */
	public <T> Select<T> SELECT_FROM(DomainObjectMatch<T> start)
	{
		DomainObjectMatch<?> delegate = APIAccess.getDelegate(start);
		DomainObjectMatch<?> match = delegate != null ? delegate : start;
		SelectExpression<T> se = new SelectExpression<T>(APIAccess.getDomainObjectType(start),
				match, this.getIntAccess());
		this.queryExecutor.addAstObject(se);
		this.astObjectsContainer = se;
		Select<T> ret = APIAccess.createSelect(se, getIntAccess());
		QueryRecorder.recordInvocation(this, "SELECT_FROM", ret, QueryRecorder.placeHolder(match));
		return ret;
	}

	/**
	 * Reject domain objects from a set of domain objects.
	 * Answer a set containing all objects of the source set except the rejected ones.
	 *
	 * @param start with a DomainObjectMatch representing the initial set.
	 * @return
	 */
	public <T> Select<T> REJECT_FROM(DomainObjectMatch<T> start)
	{
		DomainObjectMatch<?> delegate = APIAccess.getDelegate(start);
		DomainObjectMatch<?> match = delegate != null ? delegate : start;
		SelectExpression<T> se = new SelectExpression<T>(APIAccess.getDomainObjectType(start),
				match, this.getIntAccess(), true);
		this.queryExecutor.addAstObject(se);
		this.astObjectsContainer = se;
		Select<T> ret = APIAccess.createSelect(se, getIntAccess());
		QueryRecorder.recordInvocation(this, "REJECT_FROM", ret, QueryRecorder.placeHolder(match));
		return ret;
	}

	/**
	 * Collect the specified attribute from all objects in a DomainObjectMatch
	 *
	 * @param attribute
	 * @return
	 */
	public Collect COLLECT(JcProperty attribute)
	{
		CollectExpression ce = new CollectExpression(attribute, this.getIntAccess());
		Collect coll = APIAccess.createCollect(ce);
		this.queryExecutor.addAstObject(ce);
		QueryRecorder.recordInvocation(this, "COLLECT", coll, QueryRecorder.placeHolder(attribute));
		return coll;
	}

	/**
	 * Build the union of the specified sets
	 *
	 * @param set
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> DomainObjectMatch<T> UNION(DomainObjectMatch<T>... set)
	{
		DomainObjectMatch<T> ret = this.union_Intersection(true, set);
		Object[] placeHolders = new Object[set.length];
		DomainObjectMatch<?> delegate;
		DomainObjectMatch<?> match;
		for (int i = 0; i < set.length; i++)
		{
			delegate = APIAccess.getDelegate(set[i]);
			match = delegate != null ? delegate : set[i];
			placeHolders[i] = QueryRecorder.placeHolder(match);
		}
		delegate = APIAccess.getDelegate(ret);
		match = delegate != null ? delegate : ret;
		QueryRecorder.recordAssignment(this, "UNION", match, placeHolders);
		return ret;
	}

	/**
	 * Build the intersection of the specified sets
	 *
	 * @param set
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> DomainObjectMatch<T> INTERSECTION(DomainObjectMatch<T>... set)
	{
		DomainObjectMatch<T> ret = this.union_Intersection(false, set);
		Object[] placeHolders = new Object[set.length];
		DomainObjectMatch<?> delegate;
		DomainObjectMatch<?> match;
		for (int i = 0; i < set.length; i++)
		{
			delegate = APIAccess.getDelegate(set[i]);
			match = delegate != null ? delegate : set[i];
			placeHolders[i] = QueryRecorder.placeHolder(match);
		}
		delegate = APIAccess.getDelegate(ret);
		match = delegate != null ? delegate : ret;
		QueryRecorder.recordAssignment(this, "INTERSECTION", match, placeHolders);
		return ret;
	}

	/**
	 * Execute the domain query
	 *
	 * @return a DomainQueryResult
	 */
	public DomainQueryResult execute()
	{
		DomainQueryResult ret = new DomainQueryResult(this);
		Object so = this.queryExecutor.getMappingInfo().getInternalDomainAccess().getSyncObject();
		if (so != null)
		{
			synchronized (so)
			{
				this.queryExecutor.execute();
			}
		} else
			this.queryExecutor.execute();
		return ret;
	}

	/**
	 * Retrieve the count for every DomainObjectMatch of the query
	 * in order to support pagination
	 *
	 * @return a CountQueryResult
	 */
	public CountQueryResult executeCount()
	{
		CountQueryResult ret = new CountQueryResult(this);
		Object so = this.queryExecutor.getMappingInfo().getInternalDomainAccess().getSyncObject();
		if (so != null)
		{
			synchronized (so)
			{
				this.queryExecutor.executeCount();
			}
		} else
			this.queryExecutor.executeCount();
		return ret;
	}

	/**
	 * Answer the context containing DomainObjectMatch(es) of a replayed query.
	 * <br/>Answer null, if this is not a replayed query.
	 *
	 * @return
	 */
	public ReplayedQueryContext getReplayedQueryContext()
	{
		return this.queryExecutor.getReplayedQueryContext();
	}

	/**
	 * Answer the recorded query. May return null.
	 *
	 * @return
	 */
	public RecordedQuery getRecordedQuery()
	{
		return this.queryExecutor.getRecordedQuery();
	}

	/**
	 * answer a map containing DomainObjectMatch to id entries
	 *
	 * @return
	 */
	Map<Object, String> getRecordedQueryObjects()
	{
		return this.queryExecutor.getRecordedQueryObjects();
	}

	QueryExecutor getQueryExecutor()
	{
		return this.queryExecutor;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private <T> DomainObjectMatch<T> union_Intersection(boolean union, DomainObjectMatch<T>... set)
	{
		DomainObjectMatch<T> ret;
		Boolean br_old = QueryRecorder.blockRecording.get();
		try
		{
			QueryRecorder.blockRecording.set(Boolean.TRUE);
			DomainObjectMatch[] newSet = set;
			boolean isGeneric = set.length > 0 && APIAccess.getDelegate(set[0]) != null;
			if (isGeneric)
			{
				newSet = new DomainObjectMatch[set.length];
				for (int i = 0; i < set.length; i++)
				{
					DomainObjectMatch<?> delegate = APIAccess.getDelegate(set[i]);
					if (delegate != null) // generic model
						newSet[i] = delegate;
					else
						newSet[i] = set[i];
				}
			}
			DomainObjectMatch newMatch = build_union_Intersection(union, newSet);
			if (isGeneric)
				ret = (DomainObjectMatch<T>) APIAccess.createDomainObjectMatch(DomainObject.class, newMatch);
			else
				ret = newMatch;
		} finally
		{
			QueryRecorder.blockRecording.set(br_old);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	private <T> DomainObjectMatch<T> build_union_Intersection(boolean union, DomainObjectMatch<T>... set)
	{
		DomainObjectMatch<T> ret = null;
		if (set.length > 0)
		{
			IASTObject lastOne = null;
			UnionExpression ue = new UnionExpression(union);
			ret = APIAccess.createDomainObjectMatch(APIAccess.getDomainObjectType(set[0]),
					this.queryExecutor.getDomainObjectMatches().size(),
					this.queryExecutor.getMappingInfo());
			this.queryExecutor.getDomainObjectMatches().add(ret);
			ue.setResult(ret);
			APIAccess.setUnionExpression(ret, ue);
			int idx = 0;
			if (set.length > 1)
				this.BR_OPEN();
			for (DomainObjectMatch<T> dom : set)
			{
				ue.getSources().add(dom);
				if (idx > 0 && union)
					this.OR();
				lastOne = APIAccess.getAstObject(this.WHERE(ret).IN(dom));
				idx++;
			}
			if (set.length > 1)
				lastOne = APIAccess.getAstObject(this.BR_CLOSE());
			ue.setLastOfUnionBase(lastOne);
		}
		return ret;
	}

	private IntAccess getIntAccess()
	{
		if (this.intAccess == null)
			this.intAccess = new IntAccess();
		return this.intAccess;
	}

	/****************************************************/
	public class IntAccess
	{
		public QueryExecutor getQueryExecutor()
		{
			return queryExecutor;
		}

		public AbstractDomainQuery getDomainQuery()
		{
			return AbstractDomainQuery.this;
		}

		public void resetAstObjectsContainer()
		{
			astObjectsContainer = queryExecutor;
		}
	}
}
