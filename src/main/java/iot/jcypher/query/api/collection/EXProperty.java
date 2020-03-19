/************************************************************************
 * Copyright (c) 2014 IoT-Solutions e.U.
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

package iot.jcypher.query.api.collection;

import iot.jcypher.query.api.APIObject;
import iot.jcypher.query.ast.collection.CollectExpression;
import iot.jcypher.query.ast.collection.PropertyEvalExpresssion;

public class EXProperty<T> extends APIObject
{

	private T connector;

	EXProperty(CollectExpression cx, T connector)
	{
		super();
		this.astNode = cx;
		this.connector = connector;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>select the property for a collect expression</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>e.g. ...COLLECT().<b>property("name")</b>.from(nds)</i></div>
	 * <br/>
	 */
	public T property(String propertyName)
	{
		((PropertyEvalExpresssion) ((CollectExpression) this.astNode).getEvalExpression())
				.setPropertyName(propertyName);
		return this.connector;
	}
}
