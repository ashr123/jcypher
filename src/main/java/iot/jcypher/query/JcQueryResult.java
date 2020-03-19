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

package iot.jcypher.query;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.graph.GrNode;
import iot.jcypher.graph.GrPath;
import iot.jcypher.graph.GrRelation;
import iot.jcypher.graph.Graph;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.result.util.ResultHandler;
import iot.jcypher.query.values.*;
import org.neo4j.driver.Result;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class JcQueryResult
{

	private JsonObject jsonResult;
	private List<JcError> generalErrors;
	private List<JcError> dbErrors;
	private ResultHandler resultHandler;

	public JcQueryResult(JsonObject jsonResult, int queryIndex, IDBAccess dbAccess)
	{
		super();
		this.jsonResult = jsonResult;
		this.resultHandler = new ResultHandler(jsonResult, queryIndex, dbAccess);
	}

	public JcQueryResult(Result statementResult, IDBAccess dbAccess)
	{
		this.resultHandler = new ResultHandler(statementResult, dbAccess);
	}

	/**
	 * @return the JsonObject representing the query result. Is null when BOLT protocol is used.
	 */
	public JsonObject getJsonResult()
	{
		return jsonResult;
	}

	/**
	 * @param node
	 * @return an unmodifiable list of nodes (i.e a result column)
	 */
	public List<GrNode> resultOf(JcNode node)
	{
		return this.resultHandler.getNodes(node);
	}

	/**
	 * @param relation
	 * @return an unmodifiable list of relations (i.e a result column)
	 */
	public List<GrRelation> resultOf(JcRelation relation)
	{
		return this.resultHandler.getRelations(relation);
	}

	/**
	 * Note: After locally modifying the graph the result may be inconsistent with the changes
	 *
	 * @param path
	 * @return an unmodifiable list of paths (i.e a result column)
	 */
	public List<GrPath> resultOf(JcPath path)
	{
		return this.resultHandler.getPaths(path);
	}

	public List<BigDecimal> resultOf(JcNumber number)
	{
		return this.resultHandler.getNumbers(number);
	}

	public List<String> resultOf(JcString string)
	{
		return this.resultHandler.getStrings(string);
	}

	public List<Boolean> resultOf(JcBoolean bool)
	{
		return this.resultHandler.getBooleans(bool);
	}

	public List<List<?>> resultOf(JcCollection collection)
	{
		return this.resultHandler.getCollections(collection);
	}

	public List<?> resultOf(JcValue val)
	{
		return this.resultHandler.getObjects(val);
	}

	/**
	 * answer a list of literal maps containing result values for the given keys
	 *
	 * @param key a variable number of keys which are used to calculate result-values to fill the resulting maps
	 * @return a list of LiteralMap(s)
	 */
	public LiteralMapList resultMapListOf(JcPrimitive... key)
	{
		List<List<?>> results = new ArrayList<List<?>>();
		LiteralMapList ret = new LiteralMapList();
		int size = -1;
		ResultHandler.includeNullValues.set(Boolean.TRUE);
		try
		{
			for (JcPrimitive k : key)
			{
				List<?> r = this.resultOf(k);
				if (size == -1)
					size = r.size();
				results.add(r);
				for (int i = 0; i < r.size(); i++)
				{
					LiteralMap map;
					if (i > ret.size() - 1)
					{
						map = new LiteralMap();
						ret.add(map);
					} else
						map = ret.get(i);
					map.put(k, r.get(i));
				}
			}
		} finally
		{
			ResultHandler.includeNullValues.remove();
		}
		return ret;
	}

	public Graph getGraph()
	{
		return this.resultHandler.getGraph();
	}

	/**
	 * @return a list of general errors (e.g. connection errors).
	 */
	public List<JcError> getGeneralErrors()
	{
		if (this.generalErrors == null)
			this.generalErrors = new ArrayList<JcError>();
		return this.generalErrors;
	}

	/**
	 * @return a list of database errors
	 * <br/>(the database was successfully accessed, but the query produced error(s)).
	 */
	public List<JcError> getDBErrors()
	{
		if (this.dbErrors == null)
		{
			this.dbErrors = new ArrayList<JcError>();
			JsonObject obj = getJsonResult();
			if (obj != null)
			{
				JsonArray errs = obj.getJsonArray("errors");
				int size = errs.size();
				for (int i = 0; i < size; i++)
				{
					JsonObject err = errs.getJsonObject(i);
					String info = null;
					if (err.containsKey("info"))
						info = err.getString("info");
					this.dbErrors.add(new JcError(err.getString("code"),
							err.getString("message"), info));
				}
			}
		}
		return this.dbErrors;
	}

	/**
	 * Add a general (system) error
	 *
	 * @param generalError
	 */
	public void addGeneralError(JcError generalError)
	{
		getGeneralErrors().add(generalError);
	}

	/**
	 * @return true, if the query result contains any error(s).
	 */
	public boolean hasErrors()
	{
		return !this.getGeneralErrors().isEmpty() || !this.getDBErrors().isEmpty();
	}

	ResultHandler getResultHandler()
	{
		return this.resultHandler;
	}
}
