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

package iot.jcypher.query.values;

import iot.jcypher.query.values.operators.OPERATOR;

public class JcLabel extends JcString implements IHas
{

	JcLabel()
	{
		super();
	}

	JcLabel(Object val, ValueElement predecessor, IOperatorOrFunction opf)
	{
		super(null, val, predecessor, opf);
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>access a label of a node</i></div>
	 * <br/>
	 */
	public JcLabel label(String label)
	{
		return new JcLabel(label, this, OPERATOR.Node.LABEL_ACCESS);
	}
}
