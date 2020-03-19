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

package iot.jcypher.query.values.operators;

import iot.jcypher.query.values.Operator;

public class OPERATOR
{

	/*********************************************/
	public static class String
	{
		public static final Operator CONCAT = new Operator("+", " + ", OPTYPE.String.CONCAT);
	}

	/*********************************************/
	public static class Common
	{
		public static final Operator COMMA_SEPARATOR = new Operator(",", ", ", OPTYPE.String.REPLACE_SEPARATOR);
	}

	/*********************************************/
	public static class Collection
	{
		public static final Operator ADD = new Operator("+", " + ", OPTYPE.Collection.ADD);
		public static final Operator ADD_ALL = new Operator("+", " + ", OPTYPE.Collection.ADD_ALL);
		public static final Operator GET = new Operator("[]", "[", "]", OPTYPE.Collection.GET);
	}

	/*********************************************/
	public static class PropertyContainer
	{
		public static final Operator PROPERTY_ACCESS = new Operator(".", ".", OPTYPE.PropertyContainer.PROPERTY_ACCESS);
	}

	/*********************************************/
	public static class Number
	{
		public static final Operator PLUS = new Operator("+", " + ", OPTYPE.Number.PLUS);
		public static final Operator MINUS = new Operator("-", " - ", OPTYPE.Number.MINUS);
		public static final Operator MULT = new Operator("*", " * ", OPTYPE.Number.MULT);
		public static final Operator DIV = new Operator("/", " / ", OPTYPE.Number.DIV);
		public static final Operator MOD = new Operator("%", " % ", OPTYPE.Number.MOD);
		public static final Operator POW = new Operator("^", " ^ ", OPTYPE.Number.POW);
	}

	/*********************************************/
	public static class Node
	{
		public static final Operator LABEL_ACCESS = new Operator(":", ":", OPTYPE.Node.LABEL_ACCESS);
	}
}
