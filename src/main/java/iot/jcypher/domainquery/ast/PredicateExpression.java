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

package iot.jcypher.domainquery.ast;

import iot.jcypher.domain.mapping.MappingUtil;
import iot.jcypher.domainquery.api.APIAccess;
import iot.jcypher.domainquery.api.Count;
import iot.jcypher.domainquery.api.DomainObjectMatch;
import iot.jcypher.domainquery.api.IPredicateOperand1;
import iot.jcypher.domainquery.internal.IASTObjectsContainer;
import iot.jcypher.query.values.ValueAccess;
import iot.jcypher.query.values.ValueElement;

import java.util.Date;

public class PredicateExpression implements IASTObject
{

	private IPredicateOperand1 value_1;
	private Operator operator;
	private Object value_2;
	private int negationCount;
	private boolean inCollectionExpression;
	private boolean partOfCount;

	public PredicateExpression(IPredicateOperand1 value_1,
	                           IASTObjectsContainer astObjectsContainer)
	{
		super();
		this.value_1 = value_1;
		this.negationCount = 0;
		this.inCollectionExpression = astObjectsContainer instanceof SelectExpression<?>;
	}

	public PredicateExpression(IPredicateOperand1 value_1, boolean inColl)
	{
		super();
		this.value_1 = value_1;
		this.negationCount = 0;
		this.inCollectionExpression = inColl;
	}

	public Operator getOperator()
	{
		return operator;
	}

	public void setOperator(Operator operator)
	{
		this.operator = operator;
	}

	public DomainObjectMatch<?> getStartDOM()
	{
		if (value_1 instanceof DomainObjectMatch<?>)
			return (DomainObjectMatch<?>) value_1;
		else if (value_1 instanceof Count)
			return APIAccess.getDomainObjectMatch((Count) value_1);
		return (DomainObjectMatch<?>) ValueAccess.getAnyHint((ValueElement) value_1, APIAccess.hintKey_dom);
	}

	public IPredicateOperand1 getValue_1()
	{
		return value_1;
	}

	public void setValue_1(IPredicateOperand1 value_1)
	{
		this.value_1 = value_1;
	}

	public Object getValue_2()
	{
		return value_2;
	}

	public void setValue_2(Object value_2)
	{
		if (value_2 instanceof Date)
			this.value_2 = MappingUtil.dateToLong((Date) value_2);
		else
			this.value_2 = value_2;
	}

	public void addNegation()
	{
		this.negationCount++;
	}

	public int getNegationCount()
	{
		return negationCount;
	}

	public boolean isInCollectionExpression()
	{
		return inCollectionExpression;
	}

	public void setInCollectionExpression(boolean inCollectionExpression)
	{
		this.inCollectionExpression = inCollectionExpression;
	}

	public boolean isPartOfCount()
	{
		return partOfCount;
	}

	public void setPartOfCount(boolean partOfCount)
	{
		this.partOfCount = partOfCount;
	}

	public PredicateExpression createCopy()
	{
		PredicateExpression ret = new PredicateExpression(this.value_1, this.inCollectionExpression);
		ret.negationCount = this.negationCount;
		ret.operator = this.operator;
		ret.value_2 = this.value_2;
		return ret;
	}

	@Override
	public String toString()
	{
		return "[" + operator + "]";
	}

	/*****************************************************************/
	public enum Operator
	{
		EQUALS, NOT, LT, GT, LTE, GTE, LIKE, IN, CONTAINS, IS_NULL,
		STARTS_WITH, ENDS_WITH, CONTAINS_STRING
	}
}
