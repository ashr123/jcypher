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
import iot.jcypher.database.DBType;
import iot.jcypher.database.IDBAccess;
import iot.jcypher.database.internal.DBUtil;
import iot.jcypher.database.internal.IDBAccessInit;
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.result.JcError;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class AbstractRemoteDBAccess implements IDBAccessInit
{

	protected Thread shutdownHook;
	protected Properties properties;
	private boolean registerShutdownHook = true;

	@Override
	public JcQueryResult execute(JcQuery query)
	{
		List<JcQuery> qList = new ArrayList<JcQuery>();
		qList.add(query);
		List<JcQueryResult> qrList = execute(qList);
		return qrList.get(0);
	}

	@Override
	public List<JcError> clearDatabase()
	{
		return DBUtil.clearDatabase(this);
	}

	@Override
	public boolean isDatabaseEmpty()
	{
		return DBUtil.isDatabaseEmpty(this);
	}

	@Override
	public DBType getDBType()
	{
		return DBType.REMOTE;
	}

	@Override
	public void close()
	{
		if (this.shutdownHook != null)
		{
			Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
			this.shutdownHook = null;
		}
		shutDown();
	}

	@Override
	public void initialize(Properties properties)
	{
		this.properties = properties;
		if (this.properties.getProperty(DBProperties.SERVER_ROOT_URI) == null)
			throw new RuntimeException("missing property: '" +
					DBProperties.SERVER_ROOT_URI + "' in database configuration");
	}

	@Override
	public IDBAccess removeShutdownHook()
	{
		this.registerShutdownHook = false;
		if (this.shutdownHook != null)
		{
			Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
			this.shutdownHook = null;
		}
		return this;
	}

	@Override
	public IDBAccess addShutdownHook()
	{
		this.registerShutdownHook = true;
		if (this.shutdownHook == null)
		{
			Thread hook = new Thread()
			{
				@Override
				public void run()
				{
					shutDown();
				}
			};
			Runtime.getRuntime().addShutdownHook(hook);
			this.shutdownHook = hook;
		}
		return this;
	}

	protected abstract void shutDown();

	protected Thread registerShutdownHook()
	{
		// Registers a shutdown hook
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Thread hook = null;
		if (this.registerShutdownHook)
		{
			hook = new Thread()
			{
				@Override
				public void run()
				{
					shutDown();
				}
			};
			Runtime.getRuntime().addShutdownHook(hook);
		}
		return hook;
	}
}
