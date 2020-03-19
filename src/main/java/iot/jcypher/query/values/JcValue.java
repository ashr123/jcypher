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

package iot.jcypher.query.values;

import iot.jcypher.domainquery.internal.QueryRecorder;

public class JcValue extends ValueElement
{

	private String name;

	JcValue()
	{
		super();
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>create a value which is identified by a name</i></div>
	 * <br/>
	 */
	public JcValue(String name)
	{
		this(name, null, null);
	}

	JcValue(String name, ValueElement predecessor, IOperatorOrFunction opf)
	{
		super(predecessor, opf);
		this.name = name;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>return a JcCollection representing elements of the receivers type</i></div>
	 * <br/>
	 */
	public JcCollection<JcValue> asCollection()
	{
		JcCollection<JcValue> ret = new JcCollection<JcValue>(null, this, null);
		QueryRecorder.recordInvocationConditional(this, "asCollection", ret);
		return ret;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>return the receiver as a JcNumber</i></div>
	 * <br/>
	 */
	public JcNumber asNumber()
	{
		JcNumber ret = new JcNumber(null, this, null);
		QueryRecorder.recordInvocationConditional(this, "asNumber", ret);
		return ret;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>return the receiver as a JcString</i></div>
	 * <br/>
	 */
	public JcString asString()
	{
		JcString ret = new JcString(null, this, null);
		QueryRecorder.recordInvocationConditional(this, "asString", ret);
		return ret;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>return the receiver as a JcBoolean</i></div>
	 * <br/>
	 */
	public JcBoolean asBoolean()
	{
		JcBoolean ret = new JcBoolean(null, this, null);
		QueryRecorder.recordInvocationConditional(this, "asBoolean", ret);
		return ret;
	}

	String getName()
	{
		return name;
	}

	void setName(String name)
	{
		this.name = name;
	}

	protected void copyShallowTo(ValueElement target)
	{
		super.copyShallowTo(target);
		((JcValue) target).name = this.name;
	}

}
