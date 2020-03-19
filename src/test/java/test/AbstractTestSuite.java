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

package test;

import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.values.ValueElement;
import iot.jcypher.query.values.ValueWriter;
import iot.jcypher.query.writer.CypherWriter;
import iot.jcypher.query.writer.Format;
import iot.jcypher.query.writer.JSONWriter;
import iot.jcypher.query.writer.WriterContext;
import util.TestDataReader;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AbstractTestSuite
{

	private boolean print = true;
	private boolean doAssert = true;

	public static String printErrors(JcQueryResult result, boolean doPrint)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("---------------General Errors:");
		appendErrorList(result.getGeneralErrors(), sb);
		sb.append("\n---------------DB Errors:");
		appendErrorList(result.getDBErrors(), sb);
		sb.append("\n---------------end Errors:");
		String str = sb.toString();
		if (doPrint)
		{
			System.out.println("");
			System.out.println(str);
		}
		return str;
	}

	/**
	 * print errors to System.out
	 *
	 * @param result
	 */
	protected static void printErrors(List<JcError> errors)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("---------------Errors:");
		appendErrorList(errors, sb);
		sb.append("\n---------------end Errors:");
		String str = sb.toString();
		System.out.println("");
		System.out.println(str);
	}

	private static void appendErrorList(List<JcError> errors, StringBuilder sb)
	{
		int num = errors.size();
		for (int i = 0; i < num; i++)
		{
			JcError err = errors.get(i);
			sb.append('\n');
			if (i > 0)
			{
				sb.append("-------------------\n");
			}
			sb.append("codeOrType: ");
			sb.append(err.getCodeOrType());
			sb.append("\nmessage: ");
			sb.append(err.getMessage());
			if (err.getAdditionalInfo() != null)
			{
				sb.append("\ninfo: ");
				sb.append(err.getAdditionalInfo());
			}
		}
	}

	protected void setDoPrint(boolean print)
	{
		this.print = print;
	}

	protected void setDoAssert(boolean doAssert)
	{
		this.doAssert = doAssert;
	}

	protected void assertQuery(String testId, String query, String testData)
	{
		assertQueryByLines(testId, query, testData);
	}

	protected void assertQueryByLines(String testId, String query, String testData)
	{
		if (this.doAssert)
		{
			LineNumberReader lrq = new LineNumberReader(new CharArrayReader(query.toCharArray()));
			LineNumberReader lrt = new LineNumberReader(new CharArrayReader(testData.toCharArray()));
			boolean opt = false;
			boolean optPlusOne = false;
			boolean inChoice = false;
			String tLine;
			String qLine = "";
			List<String> tChoiseLines = null;
			List<String> qChoiseLines = null;
			while ((tLine = this.readLine(lrt)) != null)
			{
				if (tLine.startsWith(TestDataReader.TEST_CHOICE_START))
				{
					tChoiseLines = new ArrayList<String>();
					qChoiseLines = new ArrayList<String>();
					inChoice = true;
				} else if (inChoice)
				{
					if (tLine.startsWith(TestDataReader.TEST_CHOICE_END))
					{
						inChoice = false;
						assertLines(tChoiseLines, qChoiseLines);
					} else
					{
						tChoiseLines.add(tLine);
						qChoiseLines.add(this.readLine(lrq));
					}
				} else
				{
					if (opt)
					{
						opt = false;
						if (tLine.equals(qLine))
						{ // optional line is not there
							continue;
						}
						if (tLine.startsWith(TestDataReader.TEST_IGNORE_LINE))
						{
							qLine = this.readLine(lrq);
							optPlusOne = true;
							continue;
						}
					}
					if (optPlusOne)
					{
						optPlusOne = false;
						if (tLine.equals(qLine))
							continue;
					}
					qLine = this.readLine(lrq);
					assertNotNull(qLine);
					if (tLine.startsWith(TestDataReader.TEST_OPTIONAL_LINE))
					{
						opt = true;
						continue;
					}
					if (!tLine.startsWith(TestDataReader.TEST_IGNORE_LINE))
					{
						if (!tLine.equals(qLine))
						{
							assertEquals(testId, testData, query);
						}
					}
				}
			}
		}
	}

	private void assertLines(List<String> tChoiseLines, List<String> qChoiseLines)
	{
		boolean res = tChoiseLines.size() == qChoiseLines.size();
		if (res)
		{
			for (String tLine : tChoiseLines)
			{
				res = false;
				for (String qLine : qChoiseLines)
				{
					if (tLine.equals(qLine))
					{
						res = true;
						break;
					} else if (Math.abs(qLine.length() - tLine.length()) == 1)
					{ // they differ by one, might be the comma at the end
						// we will ignore that
						boolean tres;
						if (qLine.length() > tLine.length())
							tres = tLine.equals(qLine.substring(0, qLine.length() - 1));
						else
							tres = qLine.equals(tLine.substring(0, tLine.length() - 1));
						if (tres)
						{
							res = true;
							break;
						}
					}
				}
				if (!res)
					break;
			}
		}
		assertTrue(res);
	}

	private String readLine(LineNumberReader lnr)
	{
		String ret;
		try
		{
			ret = lnr.readLine();
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		return ret;
	}

	protected String print(IClause iclause, Format pretty)
	{
		WriterContext context = new WriterContext();
		context.cypherFormat = pretty;
		CypherWriter.toCypherExpression(iclause, 0, context);
		if (this.print)
		{
			System.out.println("");
			System.out.println(context.buffer.toString());
		}
		return context.buffer.toString();
	}

	protected String print(IClause[] iclauses, Format pretty)
	{
		WriterContext context = new WriterContext();
		context.cypherFormat = pretty;
		CypherWriter.toCypherExpression(iclauses, 0, context);
		if (this.print)
		{
			System.out.println("");
			System.out.println(context.buffer.toString());
		}
		return context.buffer.toString();
	}

	protected String print(ValueElement valueElem, Format pretty)
	{
		WriterContext context = new WriterContext();
		context.cypherFormat = pretty;
		ValueWriter.toValueExpression(valueElem, context, context.buffer);
		if (this.print)
		{
			System.out.println("");
			System.out.println(context.buffer.toString());
		}
		return context.buffer.toString();
	}

	protected String print(JcQuery query, Format pretty)
	{
		WriterContext context = new WriterContext();
		context.cypherFormat = pretty;
		CypherWriter.toCypherExpression(query, context);
		if (this.print)
		{
			System.out.println("");
			System.out.println(context.buffer.toString());
		}
		return context.buffer.toString();
	}

	protected void print(String resultString)
	{
		if (this.print)
		{
			System.out.println("");
			System.out.println(resultString);
		}
	}

	protected String printJSON(JcQuery query, Format pretty)
	{
		WriterContext context = new WriterContext();
		context.cypherFormat = pretty;
		JSONWriter.toJSON(query, context);
		if (this.print)
		{
			System.out.println("");
			System.out.println(context.buffer.toString());
		}
		return context.buffer.toString();
	}

	protected String printErrors(JcQueryResult result)
	{
		return printErrors(result, this.print);
	}
}
