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

package iot.jcypher.database;

import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.result.JcError;
import iot.jcypher.transaction.ITransaction;

import java.util.List;

/**
 * Provides access to a graph database.
 */
public interface IDBAccess
{

	/**
	 * execute a query against a database
	 *
	 * @param query a JcQuery
	 * @return a JcQueryResult
	 */
	public JcQueryResult execute(JcQuery query);

	/**
	 * execute a list of queries against a database
	 *
	 * @param queries
	 * @return a list of 'JcQueryResult's
	 */
	public List<JcQueryResult> execute(List<JcQuery> queries);

	/**
	 * removes all nodes and relations form the graph database
	 *
	 * @return a list of errors which is empty if no errors occurred
	 */
	public List<JcError> clearDatabase();

	/**
	 * create a transaction
	 *
	 * @return an instance of ITransaction
	 */
	public ITransaction beginTX();

	/**
	 * Answer the transaction which was opened in the context of the current thread.
	 * Answer null if currently there is no open transaction for the current thread.
	 *
	 * @return
	 */
	public ITransaction getTX();

	/**
	 * @return true if not a single node or relation is contained in the graph database
	 */
	public boolean isDatabaseEmpty();

	/**
	 * Answer the type of database access (REMOTE, EMBEDDED, IN_MEMORY)
	 *
	 * @return a DBType
	 */
	public DBType getDBType();

	/**
	 * close the database connection
	 */
	public void close();

	/**
	 * Removes the shutdown hook, which is added by default.
	 * The shutdown hook disposes all DB resources when the VM exits (even if you "Ctrl-C" the
	 * running application).
	 *
	 * @return this
	 */
	public IDBAccess removeShutdownHook();

	/**
	 * Adds a shutdown hook (only needed after a removeShutdownHook(), because a shutdown hook is added by default).
	 * The shutdown hook disposes all DB resources when the VM exits (even if you "Ctrl-C" the
	 * running application).
	 *
	 * @return this
	 */
	public IDBAccess addShutdownHook();
}
