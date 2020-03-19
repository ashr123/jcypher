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

package test.domainquery;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.domain.DomainAccessFactory;
import iot.jcypher.domain.IDomainAccess;
import iot.jcypher.domain.IDomainAccess.DomainLabelUse;
import iot.jcypher.domainquery.DomainQuery;
import iot.jcypher.domainquery.DomainQueryResult;
import iot.jcypher.domainquery.api.DomainObjectMatch;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.result.JcResultException;
import iot.jcypher.util.QueriesPrintObserver;
import iot.jcypher.util.QueriesPrintObserver.ContentToObserve;
import iot.jcypher.util.QueriesPrintObserver.QueryToObserve;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.AbstractTestSuite;
import test.DBAccessSettings;
import test.domainquery.model.*;
import util.TestDataReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiDomainTest extends AbstractTestSuite
{

	public static IDBAccess dbAccess;
	public static String domainName;
	public static String domainName2;
	private static List<Object> storedDomainObjects;
	private static ByteArrayOutputStream queriesStream;

	@BeforeClass
	public static void before()
	{
		domainName = "QTEST-DOMAIN";
		domainName2 = "QTEST-DOMAIN_2";
		dbAccess = DBAccessSettings.createDBAccess();

		QueriesPrintObserver.addOutputStream(System.out);
		queriesStream = new ByteArrayOutputStream();
		QueriesPrintObserver.addOutputStream(queriesStream);

		QueriesPrintObserver.addToEnabledQueries(QueryToObserve.COUNT_QUERY, ContentToObserve.CYPHER);
		QueriesPrintObserver.addToEnabledQueries(QueryToObserve.DOM_QUERY, ContentToObserve.CYPHER);

//		QueriesPrintObserver.addToEnabledQueries(QueryToObserve.DOMAIN_INFO, ContentToObserve.CYPHER);
//		QueriesPrintObserver.addToEnabledQueries(QueryToObserve.DOMAINACCESS_EXECUTE_INTERNAL, ContentToObserve.CYPHER);

		clearDB();
		before(domainName, DomainLabelUse.AUTO);
	}

	private static void clearDB()
	{
		List<JcError> errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
	}

	private static void before(String dName, DomainLabelUse dlUse)
	{
		// init db
		Population population = new Population();

		storedDomainObjects = population.createPopulation();

		NumberHolder n1 = new NumberHolder("n_123", new int[]{1, 2, 3});
		NumberHolder n2 = new NumberHolder("n_456", new int[]{4, 5, 6});
		SubNumberHolder sn1 = new SubNumberHolder("subn_347", new int[]{3, 4, 7});
		List<Object> nhs = new ArrayList<Object>();
		nhs.add(n1);
		nhs.add(n2);
		nhs.add(sn1);

		List<Date> dates = new ArrayList<Date>();
		long millis = 292517846786l;
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(1960, 1, 8, 0, 0, 0);
		Date date = cal.getTime();
		long tm = (date.getTime() / 1000) * 1000;
		dates.add(new Timestamp(tm));
		dates.add(new Timestamp(millis));
		DateHolder dh = new DateHolder("w", date);
		dh.setSqlTimestamp(new Timestamp(millis));
		nhs.add(dh);
		cal = GregorianCalendar.getInstance();
		cal.set(1963, 9, 24, 0, 0, 0);
		date = cal.getTime();
		tm = (date.getTime() / 1000) * 1000;
		dates.add(new Timestamp(tm));
		dh.setDates(dates);
		DateHolder dh1 = new DateHolder("d", date);
		dh1.setSqlTimestamp(new Timestamp(millis));
		nhs.add(dh1);

		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, dName, dlUse);
		List<JcError> errors = da.store(storedDomainObjects);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		errors = da.store(nhs);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
	}

	@AfterClass
	public static void after()
	{
		if (dbAccess != null)
		{
			dbAccess.close();
			dbAccess = null;
		}
		storedDomainObjects = null;
		try
		{
			queriesStream.close();
		} catch (IOException e)
		{
		}
		queriesStream = null;
		QueriesPrintObserver.removeAllEnabledQueries();
		QueriesPrintObserver.removeAllOutputStreams();
	}

	@Test
	public void testMultiDomainAuto_01()
	{
		IDomainAccess da1;
		DomainQuery q;
		DomainQueryResult result = null;
		String testId;
		String qCypher;

		TestDataReader tdr = new TestDataReader("/test/domainquery/Test_MULTIDOMAIN_01.txt");

		Population population = new Population();
		population.createPopulation();

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);

		/** 01 ****************************************/
		testId = "MULTIDOM_01";
		queriesStream.reset();

		q = da1.createQuery();
		DomainObjectMatch<Person> subjects = q.createMatch(Person.class);
		DomainObjectMatch<Area> europe = q.createMatch(Area.class);
		DomainObjectMatch<Person> j_smith = q.createMatch(Person.class);

		q.WHERE(j_smith.atttribute("lastName")).EQUALS("Smith");
		q.WHERE(j_smith.atttribute("firstName")).EQUALS("John");

		q.WHERE(europe.atttribute("name")).EQUALS("Europe");

		DomainObjectMatch<Object> addresses = q.TRAVERSE_FROM(subjects).FORTH("pointsOfContact").TO(Object.class);
		DomainObjectMatch<AbstractArea> areas = q.TRAVERSE_FROM(subjects).FORTH("pointsOfContact").FORTH("area")
				.FORTH("partOf").DISTANCE(1, -1).TO(AbstractArea.class);

		DomainObjectMatch<Person> num_addresses = q.SELECT_FROM(subjects).ELEMENTS(
				q.WHERE(addresses.COUNT()).EQUALS(4),
				q.WHERE(areas.COUNT()).EQUALS(9)
		);
		result = q.execute();

		List<Person> num_addressesResult = result.resultOf(num_addresses);
		List<Person> j_smithResult = result.resultOf(j_smith);
		assertEquals(1, num_addressesResult.size());
		assertEquals(1, j_smithResult.size());
		assertTrue(num_addressesResult.get(0) == j_smithResult.get(0));
		qCypher = TestDataReader.trimComments(queriesStream.toString().trim());
		assertQuery(testId, qCypher, tdr.getTestData(testId));

		/** 02 ****************************************/

		before(domainName2, DomainLabelUse.AUTO);

		IDomainAccess da2 = DomainAccessFactory.createDomainAccess(dbAccess, domainName2);

		testId = "MULTIDOM_02";
		queriesStream.reset();

		q = da2.createQuery();
		subjects = q.createMatch(Person.class);
		europe = q.createMatch(Area.class);
		j_smith = q.createMatch(Person.class);

		q.WHERE(j_smith.atttribute("lastName")).EQUALS("Smith");
		q.WHERE(j_smith.atttribute("firstName")).EQUALS("John");

		q.WHERE(europe.atttribute("name")).EQUALS("Europe");

		addresses = q.TRAVERSE_FROM(subjects).FORTH("pointsOfContact").TO(Object.class);
		areas = q.TRAVERSE_FROM(subjects).FORTH("pointsOfContact").FORTH("area")
				.FORTH("partOf").DISTANCE(1, -1).TO(AbstractArea.class);

		num_addresses = q.SELECT_FROM(subjects).ELEMENTS(
				q.WHERE(addresses.COUNT()).EQUALS(4),
				q.WHERE(areas.COUNT()).EQUALS(9)
		);
		result = q.execute();

		num_addressesResult = result.resultOf(num_addresses);
		j_smithResult = result.resultOf(j_smith);
		assertEquals(1, num_addressesResult.size());
		assertEquals(1, j_smithResult.size());
		assertTrue(num_addressesResult.get(0) == j_smithResult.get(0));
		qCypher = TestDataReader.trimComments(queriesStream.toString().trim());
		assertQuery(testId, qCypher, tdr.getTestData(testId));

		/** 03 ****************************************/

		clearDB();
		before(domainName, DomainLabelUse.ALWAYS);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);

		testId = "MULTIDOM_03";
		queriesStream.reset();

		q = da1.createQuery();
		subjects = q.createMatch(Person.class);
		europe = q.createMatch(Area.class);
		j_smith = q.createMatch(Person.class);

		q.WHERE(j_smith.atttribute("lastName")).EQUALS("Smith");
		q.WHERE(j_smith.atttribute("firstName")).EQUALS("John");

		q.WHERE(europe.atttribute("name")).EQUALS("Europe");

		addresses = q.TRAVERSE_FROM(subjects).FORTH("pointsOfContact").TO(Object.class);
		areas = q.TRAVERSE_FROM(subjects).FORTH("pointsOfContact").FORTH("area")
				.FORTH("partOf").DISTANCE(1, -1).TO(AbstractArea.class);

		num_addresses = q.SELECT_FROM(subjects).ELEMENTS(
				q.WHERE(addresses.COUNT()).EQUALS(4),
				q.WHERE(areas.COUNT()).EQUALS(9)
		);
		result = q.execute();

		num_addressesResult = result.resultOf(num_addresses);
		j_smithResult = result.resultOf(j_smith);
		assertEquals(1, num_addressesResult.size());
		assertEquals(1, j_smithResult.size());
		assertTrue(num_addressesResult.get(0) == j_smithResult.get(0));
		qCypher = TestDataReader.trimComments(queriesStream.toString().trim());
		assertQuery(testId, qCypher, tdr.getTestData(testId));

		return;
	}

	/***************************************/
	public static class SubjectComparator implements Comparator<Subject>
	{

		@Override
		public int compare(Subject o1, Subject o2)
		{
			if (o1.getClass() != o2.getClass())
				return o1.getClass().getName().compareTo(o2.getClass().getName());
			if (o1.getClass().equals(Company.class))
				return ((Company) o2).getName().compareTo(((Company) o1).getName());
			if (o1.getClass().equals(Person.class))
			{
				Person p1 = (Person) o1;
				Person p2 = (Person) o2;
				if (!p1.getLastName().equals(p2.getLastName()))
					return p1.getLastName().compareTo(p2.getLastName());
				return p2.getFirstName().compareTo(p1.getFirstName());
			}
			return 0;
		}

	}
}
