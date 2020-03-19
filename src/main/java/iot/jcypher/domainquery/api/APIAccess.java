/************************************************************************
 * Copyright (c) 2014-2015 IoT-Solutions e.U.
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

package iot.jcypher.domainquery.api;

import iot.jcypher.domainquery.AbstractDomainQuery.IntAccess;
import iot.jcypher.domainquery.ast.*;
import iot.jcypher.domainquery.internal.QueryExecutor.MappingInfo;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcValue;

import java.util.List;

public class APIAccess
{

	public static final String nodePrefix = "n_";
	public static final String pathPrefix = "p_";
	public static final String hintKey_validNodes = "vn";
	public static final String hintKey_dom = "dom";

	public static Order createOrder(OrderExpression orderExpression)
	{
		return new Order(orderExpression);
	}

	public static BooleanOperation createBooleanOperation(PredicateExpression pe)
	{
		return new BooleanOperation(pe);
	}

	public static TerminalResult createTerminalResult(IASTObject ao)
	{
		return new TerminalResult(ao);
	}

	public static <T> DomainObjectMatch<T> createDomainObjectMatch(Class<T> domainObjectType,
	                                                               int num, MappingInfo mappingInfo)
	{
		return new DomainObjectMatch<T>(domainObjectType, num, mappingInfo);
	}

	/**
	 * @param targetType must be DomainObject.class  as this represents a generic DomainObjectMatch
	 * @param delegate
	 * @return
	 */
	public static <T> DomainObjectMatch<T> createDomainObjectMatch(Class<T> targetType,
	                                                               DomainObjectMatch<?> delegate)
	{
		return new DomainObjectMatch<T>(targetType, delegate);
	}

	public static <T> DomainObjectMatch<T> createDomainObjectMatch(DomainObjectMatch<T> domainObjectMatch,
	                                                               int num, MappingInfo mappingInfo)
	{
		return domainObjectMatch.create(num, mappingInfo);
	}

	public static Traverse createTraverse(TraversalExpression te)
	{
		return new Traverse(te);
	}

	public static <T> Select<T> createSelect(SelectExpression<T> se, IntAccess ia)
	{
		return new Select<T>(se, ia);
	}

	public static Collect createCollect(CollectExpression ce)
	{
		return new Collect(ce);
	}

	public static Count createCount(DomainObjectMatch<?> dom)
	{
		return new Count(dom);
	}

	public static DomainObjectMatch<?> getDomainObjectMatch(Count count)
	{
		return count.getDomainObjectMatch();
	}

	public static List<JcNode> getNodes(DomainObjectMatch<?> dom)
	{
		return dom.getNodes();
	}

	public static List<Class<?>> getTypeList(DomainObjectMatch<?> dom)
	{
		return dom.getTypeList();
	}

	public static MappingInfo getMappingInfo(DomainObjectMatch<?> dom)
	{
		return dom.getMappingInfo();
	}

	public static String getBaseNodeName(DomainObjectMatch<?> dom)
	{
		return dom.getBaseNodeName();
	}

	public static <T> Class<T> getDomainObjectType(DomainObjectMatch<T> dom)
	{
		return dom.getDomainObjectType();
	}

	public static Class<?> getTypeForNodeName(DomainObjectMatch<?> dom, String nodeName)
	{
		return dom.getTypeForNodeName(nodeName);
	}

	public static JcNode getNodeForType(DomainObjectMatch<?> dom, Class<?> clazz)
	{
		List<Class<?>> tl = dom.getTypeList();
		int idx = -1;
		for (int i = 0; i < tl.size(); i++)
		{
			if (tl.get(i).equals(clazz))
			{
				idx = i;
				break;
			}
		}
		if (idx != -1)
			return dom.getNodes().get(idx);
		return null;
	}

	public static boolean isPageChanged(DomainObjectMatch<?> dom)
	{
		return dom.isPageChanged();
	}

	public static void setPageChanged(DomainObjectMatch<?> dom, boolean pageChanged)
	{
		dom.setPageChanged(pageChanged);
	}

	public static int getPageOffset(DomainObjectMatch<?> dom)
	{
		return dom.getPageOffset();
	}

	public static int getPageLength(DomainObjectMatch<?> dom)
	{
		return dom.getPageLength();
	}

	public static DomainObjectMatch<?> getTraversalSource(DomainObjectMatch<?> dom)
	{
		return dom.getTraversalSource();
	}

	public static void setTraversalSource(DomainObjectMatch<?> dom, DomainObjectMatch<?> traversalSource)
	{
		dom.setTraversalSource(traversalSource);
	}

	public static List<DomainObjectMatch<?>> getCollectExpressionOwner(DomainObjectMatch<?> dom)
	{
		return dom.getCollectExpressionOwner();
	}

	public static void addCollectExpressionOwner(DomainObjectMatch<?> dom,
	                                             DomainObjectMatch<?> collectXprOwner)
	{
		dom.addCollectExpressionOwner(collectXprOwner);
	}

	public static boolean isPartOfReturn(DomainObjectMatch<?> dom)
	{
		return dom.isPartOfReturn();
	}

	public static void setPartOfReturn(DomainObjectMatch<?> dom, boolean partOfReturn)
	{
		dom.setPartOfReturn(partOfReturn);
	}

	public static UnionExpression getUnionExpression(DomainObjectMatch<?> dom)
	{
		return dom.getUnionExpression();
	}

	public static boolean isPartOfUnionExpression(DomainObjectMatch<?> unionDom,
	                                              DomainObjectMatch<?> part)
	{
		UnionExpression ue = unionDom.getUnionExpression();
		if (ue != null)
			return ue.getSources().contains(part);
		return false;
	}

	public static void setUnionExpression(DomainObjectMatch<?> dom, UnionExpression unionExpression)
	{
		dom.setUnionExpression(unionExpression);
	}

	public static IASTObject getAstObject(APIObject apo)
	{
		return apo.getAstObject();
	}

	public static JcValue getCloneOf(DomainObjectMatch<?> dom, JcValue val)
	{
		return dom.getCloneOf(val);
	}

	public static DomainObjectMatch<?> getDelegate(DomainObjectMatch<?> receiver)
	{
		return receiver.getDelegate();
	}
}
