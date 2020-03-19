/************************************************************************
 * Copyright (c) 2015 IoT-Solutions e.U.
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

import iot.jcypher.domainquery.AbstractDomainQuery.IntAccess;
import iot.jcypher.domainquery.api.APIAccess;
import iot.jcypher.domainquery.api.DomainObjectMatch;
import iot.jcypher.domainquery.internal.QueryExecutor;
import iot.jcypher.query.values.JcProperty;
import iot.jcypher.query.values.ValueAccess;

public class CollectExpression implements IASTObject
{

	private IntAccess domainQueryIntAccess;
	private JcProperty attribute;
	private DomainObjectMatch<?> end;

	public CollectExpression(JcProperty attribute, IntAccess domainQueryIntAccess)
	{
		super();
		this.attribute = attribute;
		this.domainQueryIntAccess = domainQueryIntAccess;
	}

	public QueryExecutor getQueryExecutor()
	{
		return this.domainQueryIntAccess.getQueryExecutor();
	}

	public DomainObjectMatch<?> getStartDOM()
	{
		return (DomainObjectMatch<?>) ValueAccess.getAnyHint(this.attribute, APIAccess.hintKey_dom);
	}

	public DomainObjectMatch<?> getEnd()
	{
		return end;
	}

	public void setEnd(DomainObjectMatch<?> end)
	{
		this.end = end;
	}

	public JcProperty getAttribute()
	{
		return attribute;
	}
}
