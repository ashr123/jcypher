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

package iot.jcypher.query.api.pattern;

import iot.jcypher.query.api.APIObject;
import iot.jcypher.query.ast.pattern.PatternExpression;
import iot.jcypher.query.ast.pattern.PatternProperty;
import iot.jcypher.query.values.ValueElement;

import java.util.ArrayList;
import java.util.List;


public class Property<T> extends APIObject
{

	private T element;

	Property(PatternExpression px, T element)
	{
		super();
		this.astNode = px;
		this.element = element;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>specify a property value (of a node or relation) to be matched or created in a pattern;</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>takes a primitive java value like a String or a Number</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>e.g. MATCH.node(n).property("name").<b>value("John")</b></i></div>
	 * <br/>
	 */
	public <E> T value(E value)
	{
		PatternExpression px = (PatternExpression) this.astNode;
		PatternProperty prop = px.getLastElement().getLastProperty();
		prop.setValue(value);
		return this.element;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>specify a property value (of a node or relation) representing a list to be matched or created in a pattern;</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>takes primitive java values like Strings or Numbers</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>e.g. CREATE.node(n).property("numbers").<b>values(1, 2, 3)</b></i></div>
	 * <br/>
	 */
	@SuppressWarnings("unchecked")
	public <E> T values(E... value)
	{
		List<E> list = new ArrayList<E>();
		for (int i = 0; i < value.length; i++)
			list.add(value[i]);
		return values(list);
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>specify a property value (of a node or relation) representing a list to be matched or created in a pattern;</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>takes a List of primitive java values like Strings or Numbers</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>e.g. CREATE.node(n).property("numbers").<b>values(list)</b></i></div>
	 * <br/>
	 */
	public <E> T values(List<E> values)
	{
		PatternExpression px = (PatternExpression) this.astNode;
		PatternProperty prop = px.getLastElement().getLastProperty();
		prop.setValue(values);
		return this.element;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>specify a property value (of a node or relation) to be matched or created in a pattern;</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>takes a JCypher value expression</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>e.g. MATCH.node(a).relation(r).property("name")
	 * <br/>.<b>value(a.stringProperty("name").concat("<->").concat(b.stringProperty("name")))</b><br/>.node(b)</b></i></div>
	 * <br/>
	 */
	public T value(ValueElement expression)
	{
		PatternExpression px = (PatternExpression) this.astNode;
		PatternProperty prop = px.getLastElement().getLastProperty();
		prop.setValue(expression);
		return this.element;
	}
}
