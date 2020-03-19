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

package iot.jcypher.database.remote;

import iot.jcypher.database.DBProperties;
import iot.jcypher.database.internal.DBUtil;
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.writer.ContextAccess;
import iot.jcypher.query.writer.JSONWriter;
import iot.jcypher.query.writer.WriterContext;
import iot.jcypher.transaction.ITransaction;
import iot.jcypher.util.Base64CD;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.security.InternalAuthToken;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.*;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoteDBAccess extends AbstractRemoteDBAccess
{

	static final String authHeader = "Authorization";
	private static final String transactionalURLPostfix = "db/data/transaction/commit";
	private static final String locationHeader = "Location";
	private static final String authBasic = "Basic";

	private ThreadLocal<RTransactionImpl> transaction = new ThreadLocal<RTransactionImpl>();
	private String auth;
	private Client restClient;
	private WebTarget transactionalTarget;
	private Invocation.Builder invocationBuilder;

	public RemoteDBAccess()
	{
		super();
	}

	@Override
	public List<JcQueryResult> execute(List<JcQuery> queries)
	{
		WriterContext context = new WriterContext();
		ContextAccess.getResultDataContents(context).add("rest");
		ContextAccess.getResultDataContents(context).add("graph");
		JSONWriter.toJSON(queries, context);
		String json = context.buffer.toString();

		Response response = null;
		Throwable exception = null;
		Builder iBuilder;
		RTransactionImpl tx = null;
		if ((tx = this.transaction.get()) != null)
		{
			iBuilder = tx.getInvocationBuilder();
		} else
		{
			iBuilder = getInvocationBuilder();
		}
		try
		{
			response = iBuilder.post(Entity.entity(json, MediaType.APPLICATION_JSON_TYPE));
		} catch (Throwable e)
		{
			exception = e;
			tx = null;
		}

		if (tx != null)
		{
			String txLocation = response.getHeaderString(locationHeader);
			tx.setTxLocation(txLocation);
		}

		JsonObject jsonResult = null;
		StatusType status = null;
		if (exception == null)
		{
			status = response.getStatusInfo();
			String result = response.readEntity(String.class);
			if (result != null && result.length() > 0)
			{
				StringReader sr = new StringReader(result);
				JsonReader reader = Json.createReader(sr);
				jsonResult = reader.readObject();
			}
		}

		List<JcQueryResult> ret = new ArrayList<JcQueryResult>(queries.size());
		for (int i = 0; i < queries.size(); i++)
		{
			JcQueryResult qr = new JcQueryResult(jsonResult, i, this);
			ret.add(qr);
			if (exception != null)
			{
				String typ = exception.getClass().getSimpleName();
				String msg = exception.getLocalizedMessage();
				qr.addGeneralError(new JcError(typ, msg, DBUtil.getStacktrace(exception)));
			} else if (status != null && status.getStatusCode() >= 400)
			{
				String code = String.valueOf(status.getStatusCode());
				String msg = status.getReasonPhrase();
				qr.addGeneralError(new JcError(code, msg, null));
			}
		}
		return ret;
	}

	@Override
	public ITransaction beginTX()
	{
		RTransactionImpl tx = this.transaction.get();
		if (tx == null)
		{
			tx = new RTransactionImpl(this);
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

		this.restClient = null;
		this.transactionalTarget = null;
		this.invocationBuilder = null;
	}

	@Override
	public void setAuth(String userId, String password)
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			sb.append(userId);
			sb.append(':');
			sb.append(password);
			byte[] bytes = sb.toString().getBytes("UTF-8");
			sb = new StringBuilder();
			sb.append(authBasic);
			sb.append(' ');
			sb.append(new String(Base64CD.encode(bytes)));
			this.auth = sb.toString();
		} catch (Throwable e)
		{
			throw new RuntimeException(e);
		}
	}

	public String getAuth()
	{ // public for testing
		return this.auth;
	}

	@Override
	public void setAuth(AuthToken authToken)
	{
		if (authToken instanceof InternalAuthToken)
		{
			Map<String, Value> map = ((InternalAuthToken) authToken).toMap();
			Value scheme = map.get("scheme");
			if (scheme != null)
			{
				if ("basic".equals(scheme.asString()))
				{
					String uid = map.get("principal") != null ? map.get("principal").asString() : null;
					String pw = map.get("credentials") != null ? map.get("credentials").asString() : null;
					if (uid != null && pw != null)
						this.setAuth(uid, pw);
				}
			}
		}
	}

	@Override
	protected void shutDown()
	{
		try
		{
			if (this.restClient != null)
				this.restClient.close();
		} catch (Throwable e)
		{
			// do nothing
		}
	}

	synchronized Client getRestClient()
	{
		if (this.restClient == null)
		{
			this.restClient = ClientBuilder.newClient();
			this.shutdownHook = registerShutdownHook();
		}
		return this.restClient;
	}

	String getServerRootUri()
	{
		return this.properties.getProperty(DBProperties.SERVER_ROOT_URI);
	}

	void removeTx()
	{
		this.transaction.remove();
	}

	private synchronized WebTarget getTransactionalTarget()
	{
		if (this.transactionalTarget == null)
		{
			WebTarget serverRootTarget = getRestClient().target(
					this.properties.getProperty(DBProperties.SERVER_ROOT_URI));
			this.transactionalTarget = serverRootTarget.path(transactionalURLPostfix);
		}
		return this.transactionalTarget;
	}

	private synchronized Invocation.Builder getInvocationBuilder()
	{
		if (this.invocationBuilder == null)
		{
			this.invocationBuilder = getTransactionalTarget().request(MediaType.APPLICATION_JSON_TYPE);
			if (this.auth != null)
				this.invocationBuilder = this.invocationBuilder.header(authHeader, this.auth);
		}
		return this.invocationBuilder;
	}
}
