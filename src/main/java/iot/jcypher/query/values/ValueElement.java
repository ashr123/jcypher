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

package iot.jcypher.query.values;

import iot.jcypher.domainquery.api.IPredicateOperand1;
import iot.jcypher.domainquery.internal.QueryRecorder;
import iot.jcypher.query.values.functions.FUNCTION;

import java.util.HashMap;
import java.util.Map;

public class ValueElement implements IFragment, IPredicateOperand1
{

	private ValueElement predecessor;
	private IOperatorOrFunction operatorOrFunction;
	// allows to maintain additional info
	private Map<String, Object> hint;

	ValueElement()
	{
		super();
	}

	protected ValueElement(ValueElement pred, IOperatorOrFunction opf)
	{
		super();
		this.predecessor = pred;
		this.operatorOrFunction = opf;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>convert to a string, return a <b>JcString</b></i></div>
	 * <br/>
	 */
	public JcString str()
	{
		JcString ret = new JcString(null, this,
				new FunctionInstance(FUNCTION.Common.STR, 1));
		QueryRecorder.recordInvocationConditional(this, "str", ret);
		return ret;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>convert the argument to an integer, return a <b>JcNumber</b>, if the conversion fails return NULL</i></div>
	 * <br/>
	 */
	public JcNumber toInt()
	{
		JcNumber ret = new JcNumber(null, this,
				new FunctionInstance(FUNCTION.Common.TOINT, 1));
		QueryRecorder.recordInvocationConditional(this, "toInt", ret);
		return ret;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>convert the argument to a float, return a <b>JcNumber</b>, if the conversion fails return NULL</i></div>
	 * <br/>
	 */
	public JcNumber toFloat()
	{
		JcNumber ret = new JcNumber(null, this,
				new FunctionInstance(FUNCTION.Common.TOFLOAT, 1));
		QueryRecorder.recordInvocationConditional(this, "toFloat", ret);
		return ret;
	}

	ValueElement getPredecessor()
	{
		return predecessor;
	}

	void setPredecessor(ValueElement predecessor)
	{
		this.predecessor = predecessor;
	}

	IOperatorOrFunction getOperatorOrFunction()
	{
		return operatorOrFunction;
	}

	void setOperatorOrFunction(IOperatorOrFunction operatorOrFunction)
	{
		this.operatorOrFunction = operatorOrFunction;
	}

	Object getHint(String key)
	{
		if (this.hint != null)
			return this.hint.get(key);
		return null;
	}

	void setHint(String key, Object value)
	{
		if (this.hint == null)
			this.hint = new HashMap<String, Object>();
		this.hint.put(key, value);
	}

	ValueElement cloneShallow()
	{
		try
		{
			ValueElement ret = getClass().newInstance();
			copyShallowTo(ret);
			return ret;
		} catch (Throwable e)
		{
			throw new RuntimeException(e);
		}
	}

	protected void copyShallowTo(ValueElement target)
	{
		target.operatorOrFunction = this.operatorOrFunction;
		target.hint = this.hint;
	}
}
