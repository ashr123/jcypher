/************************************************************************
 * Copyright (c) 2016 IoT-Solutions e.U.
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

package iot.jcypher.domainquery.internal;

import iot.jcypher.domainquery.ast.Parameter;
import iot.jcypher.domainquery.internal.RecordedQuery.*;
import iot.jcypher.query.writer.Format;
import iot.jcypher.query.writer.JSONWriter;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

public class JSONConverter
{

	private static final String TYPE_KEY = "type";
	private static final String ON_OBJECT_KEY = "onObjectRef";
	private static final String RETURN_OBJECT_KEY = "returnObjectRef";
	private static final String METHOD_KEY = "method";

	private static final String STATEMENTS = "statements";
	private static final String INVOCATION = "Invocation";
	private static final String ASSIGNMENT = "Assignment";
	private static final String LITERAL = "Literal";
	private static final String PARAM = "Parameter";
	private static final String PARAM_NAME = "paramName";
	private static final String REFERENCE = "Reference";
	private static final String PARAMETERS = "parameters";
	private static final String AUGMENTATIONS = "augmentations";
	private static final String GENERIC = "generic";

	private static final String LITERAL_TYPE = "LiteralType";
	private static final String LITERAL_VALUE = "LiteralValue";
	private static final String REF_ID = "refId";
	private static final String REF_TYPE = "RefType";
	private static final String REF_VALUE = "RefValue";
	private static final String DO_MATCH_REF = "DOMatchRef";
	private static final String REF = "ref";

	private static final String KEY = "key";
	private static final String VALUE = "value";

	private static final String ARRAY_PREF = "Array(";
	private static final String ARRAY_POST = ")";

	private Format prettyFormat = Format.NONE;

	/**
	 * Answer a JSON representation of a recorded query
	 *
	 * @param query
	 * @return
	 */
	public String toJSON(RecordedQuery query)
	{
		StringWriter sw = new StringWriter();
		JsonGenerator generator;
		if (this.prettyFormat != Format.NONE)
		{
			JsonGeneratorFactory gf = JSONWriter.getPrettyGeneratorFactory();
			generator = gf.createGenerator(sw);
		} else
			generator = Json.createGenerator(sw);

		generator.writeStartObject();
		writeQuery(query, generator);
		generator.writeEnd();

		generator.flush();
		return sw.toString();
	}

	/**
	 * Build a recorded query from it's JSON representation
	 *
	 * @param json
	 * @return
	 */
	public RecordedQuery fromJSON(String json)
	{
		RecordedQuery ret = new RecordedQuery(false);
		StringReader sr = new StringReader(json);
		JsonReader reader = Json.createReader(sr);
		JsonObject jsonResult = reader.readObject();

		ret.setGeneric(jsonResult.getBoolean(GENERIC));
		JsonArray augmentations = jsonResult.getJsonArray(AUGMENTATIONS);
		if (augmentations != null)
		{
			Map<String, String> augs = new HashMap<String, String>();
			Iterator<JsonValue> ait = augmentations.iterator();
			while (ait.hasNext())
			{
				JsonObject a = (JsonObject) ait.next();
				augs.put(a.getString(KEY), a.getString(VALUE));
			}
			ret.setAugmentations(augs);
		}

		JsonArray statements = jsonResult.getJsonArray(STATEMENTS);
		Iterator<JsonValue> it = statements.iterator();
		while (it.hasNext())
		{
			JsonValue s = it.next();
			readStatement(s, ret.getStatements(), ret);
		}
		return ret;
	}

	void readStatement(JsonValue s, List<Statement> statements, RecordedQuery rq)
	{
		Statement statement = null;
		if (s instanceof JsonObject)
		{
			JsonObject jobj = (JsonObject) s;
			String typ = jobj.getString(TYPE_KEY);
			if (ASSIGNMENT.equals(typ) || INVOCATION.equals(typ))
			{
				JsonArray params = jobj.getJsonArray(PARAMETERS);
				List<Statement> parameters = new ArrayList<Statement>();
				Iterator<JsonValue> it = params.iterator();
				while (it.hasNext())
				{
					JsonValue v = it.next();
					readStatement(v, parameters, rq);
				}
				if (ASSIGNMENT.equals(typ))
					statement = rq.createStatement(Assignment.class);
				else
					statement = rq.createStatement(Invocation.class);
				Invocation inv = (Invocation) statement;
				inv.setParams(parameters);
				inv.setOnObjectRef(jobj.getString(ON_OBJECT_KEY));
				inv.setMethod(jobj.getString(METHOD_KEY));
				inv.setReturnObjectRef(jobj.getString(RETURN_OBJECT_KEY));
			} else if (LITERAL.equals(typ) || PARAM.equals(typ))
			{
				boolean isParam = PARAM.equals(typ);
				statement = rq.createStatement(Literal.class);
				String lTyp = jobj.getString(LITERAL_TYPE);
				JsonValue jsVal = jobj.get(LITERAL_VALUE);
				Object val = ConversionUtil.fromJSON(lTyp, jsVal);
				if (isParam)
				{
					String pName = jobj.getString(PARAM_NAME);
					Parameter param = rq.getCreateParameter(pName); // get or create
					param.setValue(val);
					val = param;
				}
				((Literal) statement).setValue(val);
			} else if (REFERENCE.equals(typ))
			{
				statement = rq.createStatement(Reference.class);
				((Reference) statement).setRefId(jobj.getString(REF_ID));
				String rTyp = jobj.getString(REF_TYPE);
				String rVal = jobj.getString(REF_VALUE);
				Object val = ConversionUtil.from(rTyp, rVal);
				((Reference) statement).setValue(val);
			} else if (DO_MATCH_REF.equals(typ))
			{
				statement = rq.createStatement(DOMatchRef.class);
				((DOMatchRef) statement).setRef(jobj.getString(REF));
			}
		}
		if (statement != null)
			statements.add(statement);
	}

	private void writeQuery(RecordedQuery query, JsonGenerator generator)
	{
		generator.write(GENERIC, query.isGeneric());
		if (query.getAugmentations() != null)
		{
			generator.writeStartArray(AUGMENTATIONS);
			Iterator<Entry<String, String>> it = query.getAugmentations().entrySet().iterator();
			while (it.hasNext())
			{
				Entry<String, String> entry = it.next();
				generator.writeStartObject();
				generator.write(KEY, entry.getKey());
				generator.write(VALUE, entry.getValue());
				generator.writeEnd();
			}
			generator.writeEnd();
		}
		generator.writeStartArray(STATEMENTS);
		for (Statement s : query.getStatements())
		{
			writeStatement(s, generator);
		}
		generator.writeEnd();
	}

	private void writeStatement(Statement statement, JsonGenerator generator)
	{
		generator.writeStartObject();
		if (statement instanceof Invocation)
		{
			Invocation inv = (Invocation) statement;
			if (statement instanceof Assignment)
				generator.write(TYPE_KEY, ASSIGNMENT);
			else
				generator.write(TYPE_KEY, INVOCATION);
			generator.write(ON_OBJECT_KEY, inv.getOnObjectRef());
			generator.write(METHOD_KEY, inv.getMethod());
			generator.write(RETURN_OBJECT_KEY, inv.getReturnObjectRef());
			generator.writeStartArray(PARAMETERS);
			for (Statement s : inv.getParams())
			{
				writeStatement(s, generator);
			}
			generator.writeEnd();
		} else if (statement instanceof Literal)
		{
			Object val = ((Literal) statement).getRawValue();
			writeLiteral(val, generator);
		} else if (statement instanceof Reference)
		{
			Reference ref = (Reference) statement;
			generator.write(TYPE_KEY, REFERENCE);
			generator.write(REF_ID, ref.getRefId());
			Object val = ref.getValue();
			if (val != null)
			{
				generator.write(REF_TYPE, val.getClass().getName());
				generator.write(REF_VALUE, val.toString());
			}
		} else if (statement instanceof DOMatchRef)
		{
			generator.write(TYPE_KEY, DO_MATCH_REF);
			generator.write(REF, ((DOMatchRef) statement).getRef());
		}
		generator.writeEnd();
	}

	private void writeLiteral(Object val, JsonGenerator generator)
	{
		boolean isParam = val instanceof iot.jcypher.domainquery.ast.Parameter;
		if (isParam)
		{
			generator.write(TYPE_KEY, PARAM);
			generator.write(PARAM_NAME, ((iot.jcypher.domainquery.ast.Parameter) val).getName());
			val = ((iot.jcypher.domainquery.ast.Parameter) val).getValue();
		} else
			generator.write(TYPE_KEY, LITERAL);
		if (val != null)
		{
			boolean isColl = Collection.class.isAssignableFrom(val.getClass());
			boolean isArray = val.getClass().isArray();
			if (isArray)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(ARRAY_PREF);
				sb.append(val.getClass().getComponentType().getName());
				sb.append(ARRAY_POST);
				generator.write(LITERAL_TYPE, sb.toString());
			} else
				generator.write(LITERAL_TYPE, val.getClass().getName());
			if (isArray || isColl)
			{
				generator.writeStartArray(LITERAL_VALUE);
				if (isColl)
				{
					Iterator<?> it = ((Collection<?>) val).iterator();
					while (it.hasNext())
					{
						generator.writeStartObject();
						writeLiteral(it.next(), generator);
						generator.writeEnd();
					}
				} else
				{ // an array
					for (int i = 0; i < Array.getLength(val); i++)
					{
						generator.writeStartObject();
						writeLiteral(Array.get(val, i), generator);
						generator.writeEnd();
					}
				}
				generator.writeEnd();
			} else
				generator.write(LITERAL_VALUE, val.toString());
		}
	}

	/**
	 * Set the format for creating JSON representations (i.e use of indentation and new lines),
	 * <br/>the default is 'no pretty printing'.
	 *
	 * @param prettyFormat
	 * @return
	 */
	public JSONConverter setPrettyFormat(Format prettyFormat)
	{
		this.prettyFormat = prettyFormat;
		return this;
	}
}
