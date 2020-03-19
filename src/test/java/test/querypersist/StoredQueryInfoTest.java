/************************************************************************
 * Copyright (c) 2015-2016 IoT-Solutions e.U.
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

package test.querypersist;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.domain.DomainAccessFactory;
import iot.jcypher.domain.IDomainAccess;
import iot.jcypher.query.result.JcError;
import iot.jcypher.util.QueriesPrintObserver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.AbstractTestSuite;
import test.DBAccessSettings;
import test.genericmodel.LoadUtil;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//@Ignore
public class StoredQueryInfoTest extends AbstractTestSuite
{

	public static IDBAccess dbAccess;
	public static String domainName;

	@BeforeClass
	public static void before()
	{
		domainName = "PEOPLE-DOMAIN"; // "QTEST-DOMAIN";
		dbAccess = DBAccessSettings.createDBAccess();
	}

	@AfterClass
	public static void after()
	{
		if (dbAccess != null)
		{
			dbAccess.close();
			dbAccess = null;
		}
		QueriesPrintObserver.removeAllEnabledQueries();
	}

	@Test
	public void testQueryNames_01()
	{
		List<JcError> errors = dbAccess.clearDatabase();
		assertTrue(errors.isEmpty());

		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		List<String> names = da.getStoredQueryNames();
		assertTrue(names.isEmpty());

		LoadUtil.loadPeopleDomainWithQuery(dbAccess);

		List<String> names2 = da.getStoredQueryNames();
		assertEquals("[Smiths_In_Europe]", names2.toString());

		return;
	}
}
