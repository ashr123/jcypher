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

package iot.jcypher.query.factories.clause;

import iot.jcypher.query.api.cases.CaseFactory;
import iot.jcypher.query.api.cases.CaseTerminal;
import iot.jcypher.query.ast.ClauseType;
import iot.jcypher.query.ast.cases.CaseExpression;
import iot.jcypher.query.values.JcValue;

/**
 * <div color='red' style="font-size:24px;color:red"><b><i>JCYPHER CLAUSE</i></b></div>
 */
public class CASE
{

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>start a CASE expression on the result of the preceding RETURN statement</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>e.g. <b>CASE.result()</b></i></div>
	 * <br/>
	 */
	public static CaseTerminal result()
	{
		CaseExpression cx = new CaseExpression();
		CaseTerminal ret = CaseFactory.createCaseTerminal(cx);
		cx.setClauseType(ClauseType.CASE);
		return ret;
	}

	/**
	 * <div color='red' style="font-size:24px;color:red"><b><i><u>JCYPHER</u></i></b></div>
	 * <div color='red' style="font-size:18px;color:red"><i>start a CASE expression on a value calculated from the result of the preceding RETURN statement</i></div>
	 * <div color='red' style="font-size:18px;color:red"><i>e.g. <b>RETURN.value(n)
	 * <br/>CASE.result(n.property("name"))</b></i></div>
	 * <br/>
	 */
	public static CaseTerminal resultOf(JcValue value)
	{
		CaseExpression cx = new CaseExpression();
		CaseTerminal ret = CaseFactory.createCaseTerminal(cx);
		cx.setClauseType(ClauseType.CASE);
		cx.setCaseValue(value);
		return ret;
	}
}
