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

package iot.jcypher.database.remote;

import iot.jcypher.database.DBProperties;
import iot.jcypher.database.internal.DBUtil;
import iot.jcypher.database.util.QParamsUtil;
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.writer.CypherWriter;
import iot.jcypher.query.writer.QueryParam;
import iot.jcypher.query.writer.WriterContext;
import iot.jcypher.transaction.ITransaction;
import org.neo4j.driver.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import org.neo4j.driver.AuthToken;

public class BoltDBAccess extends AbstractRemoteDBAccess
{

	private static final String pathPrefix = "://";
	private static final String bolt = "bolt";

	private ThreadLocal<BoltTransactionImpl> transaction = new ThreadLocal<BoltTransactionImpl>();
	private AuthToken authToken;
	private Driver driver;
	private Session session;

	public BoltDBAccess()
	{
		super();
	}

	public BoltDBAccess(Driver driver)
	{
		super();
		this.driver = driver;
	}

	public static boolean isBoltProtocol(String uri)
	{
		boolean ret = false;
		if (uri != null)
		{
			int idx = uri.indexOf(pathPrefix);
			if (idx > 0)
			{
				String prot = uri.substring(0, idx);
				ret = bolt.equalsIgnoreCase(prot);
			}
		}
		return ret;
	}

	@Override
	public List<JcQueryResult> execute(List<JcQuery> queries)
	{
		List<Statement> statements = new ArrayList<Statement>(queries.size());
		for (JcQuery query : queries)
		{
			WriterContext context = new WriterContext();
			QueryParam.setExtractParams(query.isExtractParams(), context);
			CypherWriter.toCypherExpression(query, context);
			String cypher = context.buffer.toString();
			Map<String, Object> paramsMap = QParamsUtil.createQueryParams(context);
			statements.add(new Statement(cypher, paramsMap));
		}

		Transaction tx;
		BoltTransactionImpl btx = this.transaction.get();
		if (btx != null)
			tx = btx.getTransaction();
		else
			tx = getSession().beginTransaction();

		Throwable dbException = null;
		List<JcQueryResult> ret = new ArrayList<JcQueryResult>(queries.size());
		Result result;
		try
		{
			for (Statement statement : statements)
			{
				if (statement.parameterMap != null)
					result = tx.run(statement.cypher, statement.parameterMap);
				else
					result = tx.run(statement.cypher);
				ret.add(new JcQueryResult(result, this));
			}
			//TODO fix!!!!
//			if (btx == null)
//				tx.success();
		} catch (Throwable e)
		{
			dbException = e;
			if (btx != null)
				btx.failure();
			//TODO fix!!!!
//			tx.failure();
		} finally
		{
			if (btx == null && tx != null)
			{
				try
				{
					tx.close();
				} catch (Throwable e1)
				{
					dbException = e1;
				}
			}
		}

		if (dbException != null)
		{
			String typ = dbException.getClass().getSimpleName();
			String msg = dbException.getLocalizedMessage();
			JcError err = new JcError(typ, msg, DBUtil.getStacktrace(dbException));
			if (ret.size() < queries.size())
			{
				for (int i = ret.size(); i < queries.size(); i++)
				{
					JcQueryResult res = new JcQueryResult(null, this);
					res.getDBErrors().add(err);
					ret.add(res);
				}
			} else
			{
				JcQueryResult res = ret.get(ret.size() - 1);
				res.getDBErrors().add(err); // the last one must have been erroneous
			}
		}

		return ret;
	}

	@Override
	public ITransaction beginTX()
	{
		BoltTransactionImpl tx = this.transaction.get();
		if (tx == null)
		{
			tx = new BoltTransactionImpl(this);
			this.transaction.set(tx);
		}
		return tx;
	}

	@Override
	public ITransaction getTX()
	{
		return this.transaction.get();
	}

	@Override
	public synchronized void close()
	{
		super.close();

		this.session = null;
		this.driver = null;
	}

	@Override
	public void setAuth(String userId, String password)
	{
		if (userId != null && password != null)
			this.authToken = AuthTokens.basic(userId, password);
	}

	@Override
	public void setAuth(AuthToken authToken)
	{
		this.authToken = authToken;
	}

	@Override
	protected void shutDown()
	{
		try
		{
			if (session != null)
				session.close();
		} catch (Throwable e)
		{
			// do nothing
		}
		try
		{
			if (driver != null)
				driver.close();
		} catch (Throwable e)
		{
			// do nothing
		}
	}

	void removeTx()
	{
		this.transaction.remove();
	}

	private Driver getDriver()
	{
		if (this.driver == null)
		{
			String uri = this.properties.getProperty(DBProperties.SERVER_ROOT_URI);
			if (this.authToken != null)
				this.driver = GraphDatabase.driver(uri, this.authToken);
			else
				this.driver = GraphDatabase.driver(uri);
			this.shutdownHook = registerShutdownHook();
		}
		return this.driver;
	}

	public synchronized Session getSession()
	{
		if (this.session == null)
			this.session = getDriver().session();
		return this.session;
	}

	/*******************************************/
	private static class Statement
	{
		private String cypher;
		private Map<String, Object> parameterMap;

		private Statement(String cypher, Map<String, Object> parameterMap)
		{
			super();
			this.cypher = cypher;
			this.parameterMap = parameterMap;
		}
	}
}
