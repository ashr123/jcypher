/************************************************************************
 * Copyright (c) 2014 IoT-Solutions e.U.
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

package test.domainmapping;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.domain.DomainAccessFactory;
import iot.jcypher.domain.DomainInformation;
import iot.jcypher.domain.DomainInformation.DomainObjectType;
import iot.jcypher.domain.IDomainAccess;
import iot.jcypher.domain.SyncInfo;
import iot.jcypher.graph.GrNode;
import iot.jcypher.graph.GrProperty;
import iot.jcypher.graph.GrPropertyContainer;
import iot.jcypher.graph.GrRelation;
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.factories.clause.SEPARATE;
import iot.jcypher.query.factories.clause.WHERE;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.result.JcResultException;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcRelation;
import iot.jcypher.util.Util;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Test;
import test.AbstractTestSuite;
import test.DBAccessSettings;
import test.domainmapping.ambiguous.*;
import test.domainmapping.inner.MyClass;
import test.domainmapping.inner.MyClass.MyInnerClass;
import test.domainmapping.inner.MyClass.MyInnerClass.MyInnerInnerClass;
import test.domainmapping.inner.MyClass.MyStaticInnerClass;
import test.domainmapping.inner.MyOtherClass;
import test.domainmapping.maps.MapContainer;
import test.domainmapping.maps.Mark;
import test.domainmapping.maps.MultiDimMapsLists;
import test.domainmapping.resolutiondepth.LinkedElement;
import test.domainmapping.util.CompareUtil;
import test.domainmapping.util.CompareUtil_2;
import test.domainmapping.util.CompareUtil_3;
import test.domainmapping.util.CompareUtil_4;

import java.util.*;
import java.util.Map.Entry;

import static org.junit.Assert.*;

public class DomainMappingTest extends AbstractTestSuite
{

	private static IDBAccess dbAccess;
	private static String domainName;

	@BeforeClass
	public static void before()
	{
		domainName = "TEST-DOMAIN";
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
	}

	@Test
	public void testClearDomain()
	{
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		List<JcError> errors;

		Person john = new Person();
		Address address = new Address();
		Contact phone = new Contact();
		Contact email = new Contact();
		Person james = new Person();

		buildInitialDomainObjects_1(john, james, address, phone, email, null, null);

		List<Object> domainObjects = new ArrayList<Object>();
		domainObjects.add(john);
		domainObjects.add(james);

		errors = da.store(domainObjects);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		boolean check = dbAccess.isDatabaseEmpty();
		assertFalse("Test Domain not empty", check);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		check = dbAccess.isDatabaseEmpty();
		assertTrue("Test Domain is empty", check);
		return;
	}

	@Test
	public void testInnerClass()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		boolean equals;
		MyClass myClass = new MyClass();
		myClass.setName("My Class");
		MyInnerClass myInnerClass = myClass.createInnerClass();
		myInnerClass.setName("My inner Class");
		MyInnerClass myInnerClass_2 = myClass.createInnerClass();
		myInnerClass_2.setName("My inner Class 2");
		MyInnerInnerClass myInnerInnerClass = myInnerClass.createInnerInnerClass();
		myInnerInnerClass.setName("My inner inner Class");
		MyInnerInnerClass myInnerInnerClass_2 = myInnerClass_2.createInnerInnerClass();
		myInnerInnerClass_2.setName("My inner inner Class 2");
		myInnerClass.setMyInnerInnerClass(myInnerInnerClass);
		MyStaticInnerClass myStaticInnerClass = new MyStaticInnerClass();
		myStaticInnerClass.setName("My static inner Class");

		MyOtherClass myOtherClass = new MyOtherClass();
		myOtherClass.setMyInnerClass(myInnerClass);
		myOtherClass.setMyInnerInnerClass(myInnerInnerClass_2);
		myOtherClass.setMyStaticInnerClass(myStaticInnerClass);

		myClass = new MyClass();
		myClass.setName("My Class next");
		myInnerClass = myClass.createInnerClass();
		myInnerClass.setName("My inner Class next");
		myInnerInnerClass = myInnerClass.createInnerInnerClass();
		myInnerInnerClass.setName("My inner inner Class next");
		myStaticInnerClass = new MyStaticInnerClass();
		myStaticInnerClass.setName("My static inner Class next");
		myClass.setMyInnerClass(myInnerClass);
		myInnerClass.setMyInnerInnerClass(myInnerInnerClass);
		myClass.setMyStaticInnerClass(myStaticInnerClass);

//		myClass.setMyInterface(new MyInterface() {
//			
//			@Override
//			public int getNumber() {
//				return 0;
//			}
//		});
		MyClass myClass_1;
		MyOtherClass myOtherClass_1;

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(myOtherClass);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		errors = da.store(myClass);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(myOtherClass);
		SyncInfo syncInfo_2 = da.getSyncInfo(myClass);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		myOtherClass_1 = da1.loadById(MyOtherClass.class, -1, syncInfo_1.getId());
		myClass_1 = da1.loadById(MyClass.class, -1, syncInfo_2.getId());

		equals = myOtherClass.getMyInnerClass().getName().equals(myOtherClass_1.getMyInnerClass().getName()) &&
				myOtherClass.getMyInnerClass().getMyInnerInnerClass().getName().equals(
						myOtherClass_1.getMyInnerClass().getMyInnerInnerClass().getName()) &&
				myOtherClass.getMyInnerInnerClass().getName().equals(myOtherClass_1.getMyInnerInnerClass().getName()) &&
				myOtherClass.getMyStaticInnerClass().getName().equals(myOtherClass_1.getMyStaticInnerClass().getName());
		assertTrue(equals);

		equals = myClass.getName().equals(myClass_1.getName()) &&
				myClass.getMyInnerClass().getName().equals(myClass_1.getMyInnerClass().getName()) &&
				myClass.getMyInnerClass().getMyInnerInnerClass().getName().equals(
						myClass_1.getMyInnerClass().getMyInnerInnerClass().getName()) &&
				myClass.getMyStaticInnerClass().getName().equals(myClass_1.getMyStaticInnerClass().getName());
		assertTrue(equals);

		return;
	}

	@Test
	public void testUniqueLabels()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		boolean equals;
		test.domainmapping.unique_label.Person person, person_1;
		test.domainmapping.unique_label.other.Person otherPerson, otherPerson_1;

		person = new test.domainmapping.unique_label.Person();
		person.setFirstName("John");
		person.setLastName("Smith");

		otherPerson = new test.domainmapping.unique_label.other.Person();
		otherPerson.setName("John Smith");

		List<Object> domainObjects = new ArrayList<Object>();
		domainObjects.add(person);
		domainObjects.add(otherPerson);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(domainObjects);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(person);
		SyncInfo syncInfo_2 = da.getSyncInfo(otherPerson);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		person_1 = da1.loadById(test.domainmapping.unique_label.Person.class, -1, syncInfo_1.getId());
		otherPerson_1 = da1.loadById(test.domainmapping.unique_label.other.Person.class, -1, syncInfo_2.getId());

		equals = person.getFirstName().equals(person_1.getFirstName()) &&
				person.getLastName().equals(person_1.getLastName()) &&
				otherPerson.getName().equals(otherPerson_1.getName());
		assertTrue(equals);

		return;
	}

	@Test
	public void testRecursiveArrays()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MultiDimMapsLists multiDimMapsLists = new MultiDimMapsLists();
		MultiDimMapsLists multiDimMapsLists_1;
		boolean equals;

		Object[] array = new Object[1];
		array[0] = array;

		multiDimMapsLists.setMultiDimArray(array);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiDimMapsLists);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		return;
	}

	@Test
	public void testMultiArrays()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MultiDimMapsLists multiDimMapsLists = new MultiDimMapsLists();
		MultiDimMapsLists multiDimMapsLists_1;
		boolean equals;
		Address first = new Address();
		DistrictAddress second = new DistrictAddress();
		Address third = new Address();

		buildMapTestAny2Any(first, second, third);
		List<Object> multiDimList = new ArrayList<Object>();
		Object[] array = new Object[]{first, second, third, multiDimList};

		multiDimList.add(first);
		multiDimList.add(second);
		multiDimList.add(third);
		multiDimList.add(array);
		multiDimMapsLists.setMultiDimList(multiDimList);

		multiDimMapsLists.setMultiDimArray(array);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiDimMapsLists);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		return;
	}

	@Test
	public void testArrays()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MultiDimMapsLists multiDimMapsLists = new MultiDimMapsLists();
		MultiDimMapsLists multiDimMapsLists_1;
		boolean equals;
		Address first = new Address();
		DistrictAddress second = new DistrictAddress();
		Address third = new Address();

		buildMapTestAny2Any(first, second, third);
		Object[] array = new Object[]{first, second, third};

		multiDimMapsLists.setMultiDimArray(array);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiDimMapsLists);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		array[0] = 1;
		array[1] = 2;
		array[2] = 3;

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		array[0] = first;
		array[1] = second;
		array[2] = third;

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		multiDimMapsLists.setMultiDimArray(null);

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);
	}

	@Test
	public void testLoadByType()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		Broker broker1 = new Broker();
		Broker broker2 = new Broker();
		MultiBroker multiBroker = new MultiBroker();

		List<Object> addresses = buildAmbiguousTestObjects(broker1, broker2, multiBroker);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		List<Object> domainObjects = new ArrayList<Object>();
		domainObjects.add(broker1);
		domainObjects.add(broker2);

		errors = da.store(domainObjects);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		IDomainAccess da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		List<Address> result = da1.loadByType(Address.class, -1, 0, -1);
		boolean equals = CompareUtil_3.equalsUnorderedList(addresses, result);
		assertTrue(equals);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		List<Object> result_1 = da1.loadByType(Object.class, -1, 0, -1);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		List<Object> result_2 = da1.loadByType(Object.class, -1, 5, 3);
		List<Object> compare = result_1.subList(5, 8);
		equals = CompareUtil_3.equalsList(compare, result_2);
		assertTrue(equals);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		List<Object> result_3 = da1.loadByType(Object.class, -1, 4, 3);
		compare = result_1.subList(4, 7);
		equals = CompareUtil_3.equalsList(compare, result_3);
		assertTrue(equals);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		result_1 = da1.loadByType(Object.class, -1, 0, -1);

		result_2 = da1.loadByType(Object.class, -1, 5, 3);
		equals = result_2.get(0) == result_1.get(5) &&
				result_2.get(1) == result_1.get(6) &&
				result_2.get(2) == result_1.get(7);
		assertTrue(equals);

		result_3 = da1.loadByType(Object.class, -1, 4, 3);
		equals = result_3.get(0) == result_1.get(4) &&
				result_3.get(1) == result_1.get(5) &&
				result_3.get(2) == result_1.get(6);
		assertTrue(equals);
		return;
	}

	@Test
	public void testDomainInformation()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, "TEST-1-DOMAIN");
		IDomainAccess da1 = DomainAccessFactory.createDomainAccess(dbAccess, "TEST-2-DOMAIN");

		Address address = new Address();
		address.setCity("Vienna");
		address.setStreet("Alserstrasse");
		address.setNumber(10);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(address);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da1.store(address);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		List<String> available = DomainInformation.availableDomains(dbAccess);
		try
		{
			assertEquals("[TEST-1-DOMAIN, TEST-2-DOMAIN]", available.toString());
		} catch (ComparisonFailure e)
		{
			assertEquals("[TEST-2-DOMAIN, TEST-1-DOMAIN]", available.toString());
		}

		return;
	}

	@Test
	public void testDomainInformation_02()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		DomainInformation di;
		Broker broker1 = new Broker();
		Broker broker2 = new Broker();
		MultiBroker multiBroker = new MultiBroker();

		buildAmbiguousTestObjects(broker1, broker2, multiBroker);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		List<Object> domainObjects = new ArrayList<Object>();
		domainObjects.add(broker1);
		domainObjects.add(broker2);

		errors = da.store(domainObjects);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		di = DomainInformation.forDomain(dbAccess, domainName);
		List<DomainObjectType> doTypes = di.getDomainObjectTypes();
		assertEquals("[DomainObjectType [typeName=test.domainmapping.Address]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.Broker]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.District]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.DistrictAddress]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.JPerson]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.NPerson]]", doTypes.toString());

		// once more to check for stored infoNodeId
		List<DomainObjectType> doTypes_1 = di.getDomainObjectTypes();
		assertEquals("[DomainObjectType [typeName=test.domainmapping.Address]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.Broker]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.District]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.DistrictAddress]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.JPerson]," +
				" DomainObjectType [typeName=test.domainmapping.ambiguous.NPerson]]", doTypes_1.toString());

		List<Class<?>> types = di.getRawTypes(doTypes_1);
		IDomainAccess da1 = di.getDomainAccess();
		List<Long> instancesCount = da1.numberOfInstancesOf(types);
		assertEquals("[7, 2, 2, 1, 1, 1]", instancesCount.toString());
		return;
	}

	@Test
	public void testResolutionDepth()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		boolean equals;
		LinkedElement last = new LinkedElement("6");
		LinkedElement elem = new LinkedElement("0",
				new LinkedElement("1",
						new LinkedElement("2",
								new LinkedElement("3",
										new LinkedElement("4",
												new LinkedElement("5",
														last))))));
		LinkedElement elem_1;

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(elem);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(elem);

		int depth = -1;
		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		elem_1 = da1.loadById(LinkedElement.class, depth, syncInfo_1.getId());
		equals = CompareUtil_4.equalsLinkedElements(elem, elem_1, depth);
		assertTrue(equals);
		int resolvedDepth = CompareUtil_4.getDepth(elem_1);
		assertEquals("resolution depth", 6, resolvedDepth);

		depth = 5;
		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		elem_1 = da1.loadById(LinkedElement.class, depth, syncInfo_1.getId());
		equals = CompareUtil_4.equalsLinkedElements(elem, elem_1, depth);
		assertTrue(equals);
		resolvedDepth = CompareUtil_4.getDepth(elem_1);
		assertEquals("resolution depth", depth, resolvedDepth);

		depth = 4;
		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		elem_1 = da1.loadById(LinkedElement.class, depth, syncInfo_1.getId());
		equals = CompareUtil_4.equalsLinkedElements(elem, elem_1, depth);
		assertTrue(equals);
		resolvedDepth = CompareUtil_4.getDepth(elem_1);
		assertEquals("resolution depth", depth, resolvedDepth);

		depth = 3;
		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		elem_1 = da1.loadById(LinkedElement.class, depth, syncInfo_1.getId());
		equals = CompareUtil_4.equalsLinkedElements(elem, elem_1, depth);
		assertTrue(equals);
		resolvedDepth = CompareUtil_4.getDepth(elem_1);
		assertEquals("resolution depth", depth, resolvedDepth);

		depth = 2;
		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		elem_1 = da1.loadById(LinkedElement.class, depth, syncInfo_1.getId());
		equals = CompareUtil_4.equalsLinkedElements(elem, elem_1, depth);
		assertTrue(equals);
		resolvedDepth = CompareUtil_4.getDepth(elem_1);
		assertEquals("resolution depth", depth, resolvedDepth);

		depth = 1;
		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		elem_1 = da1.loadById(LinkedElement.class, depth, syncInfo_1.getId());
		equals = CompareUtil_4.equalsLinkedElements(elem, elem_1, depth);
		assertTrue(equals);
		resolvedDepth = CompareUtil_4.getDepth(elem_1);
		assertEquals("resolution depth", depth, resolvedDepth);

		depth = 0;
		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		elem_1 = da1.loadById(LinkedElement.class, depth, syncInfo_1.getId());
		equals = CompareUtil_4.equalsLinkedElements(elem, elem_1, depth);
		assertTrue(equals);
		resolvedDepth = CompareUtil_4.getDepth(elem_1);
		assertEquals("resolution depth", depth, resolvedDepth);

		// close loop
		last.setNext(elem);

		errors = da.store(elem);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		depth = -1;
		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		elem_1 = da1.loadById(LinkedElement.class, depth, syncInfo_1.getId());
		equals = CompareUtil_4.equalsLinkedElements(elem, elem_1, depth);
		assertTrue(equals);
		resolvedDepth = CompareUtil_4.getDepth(elem_1);
		assertEquals("resolution depth", -1, resolvedDepth); // -1 ... loop

		List<Object> changed = new ArrayList<Object>();
		LinkedElement el = elem;
		changed.add(el);
		LinkedElement nel = el.getNext();
		while (nel != null)
		{
			el.setNext(null);
			el = nel;
			if (!changed.contains(el))
				changed.add(el);
			nel = el.getNext();
		}

		errors = da.store(changed);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		List<RelationsToCheck> rtc = new ArrayList<RelationsToCheck>();
		rtc.add(new RelationsToCheck("next", 0));

		boolean check = checkForNodesAndRelations(null, rtc);
		assertTrue("Test for relations in graph", check);

		return;
	}

	@Test
	public void testLists_Maps()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MultiDimMapsLists multiDimMapsLists = new MultiDimMapsLists();
		MultiDimMapsLists multiDimMapsLists_1;
		boolean equals;

		List<Object> multiDimList = new ArrayList<Object>();
		List<Object> multiDimList_1 = new ArrayList<Object>();

		Map<Object, Object> multiDimMap = new HashMap<Object, Object>();
		Map<Object, Object> multiDimMap_1 = new HashMap<Object, Object>();

		multiDimList.add(new Mark("list_root"));
		multiDimMap.put("mark", new Mark("map_root"));
		multiDimList.add(multiDimMap);
		multiDimMap.put("list_root", multiDimList);

		multiDimMapsLists.setMultiDimList(multiDimList);
		multiDimMapsLists.setMultiDimMap(multiDimMap);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiDimMapsLists);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		Object obj = da1.loadById(Object.class, -1, syncInfo_1.getId());
		assertTrue(obj instanceof MultiDimMapsLists);
		multiDimMapsLists_1 = (MultiDimMapsLists) obj;
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		multiDimList_1.add(new Mark("list_1"));
		multiDimMap_1.put("mark", new Mark("map_1"));
		multiDimList_1.add(multiDimMap_1);
		multiDimMap_1.put("list_1", multiDimList_1);

		multiDimList.add(multiDimList_1);
		multiDimMap.put("map_1", multiDimMap_1);

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		return;
	}

	@Test
	public void testMultiList()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MultiDimMapsLists multiDimMapsLists = new MultiDimMapsLists();
		MultiDimMapsLists multiDimMapsLists_1;
		boolean equals;
		Address first = new Address();
		DistrictAddress second = new DistrictAddress();
		Address third = new Address();
		Address first_1 = new Address();
		DistrictAddress second_1 = new DistrictAddress();
		Address third_1 = new Address();
		Address first_2 = new Address();
		DistrictAddress second_2 = new DistrictAddress();
		Address third_2 = new Address();

		buildMapTestAny2Any(first, second, third);
		buildMapTestAny2Any(first_1, second_1, third_1);
		buildMapTestAny2Any(first_2, second_2, third_2);

		// init multiDimList
		List<Object> multiDimList = new ArrayList<Object>();
		List<Object> multiDimList_1 = new ArrayList<Object>();
		List<Object> multiDimList_2 = new ArrayList<Object>();
		List<Object> multiDimList_3 = new ArrayList<Object>();

		multiDimList_1.add(first_1);
		multiDimList_1.add(second_1);
		multiDimList_1.add(third_1);
		multiDimList_2.add("first");
		multiDimList_2.add("second");
		multiDimList_2.add("third");
		multiDimList_3.add(100);
		multiDimList_3.add(200);
		multiDimList_3.add(300);

		multiDimList.add(first);
		multiDimList.add(multiDimList_1);
		multiDimList.add(multiDimList_2);
		multiDimList.add(multiDimList_3);

//		multiDimMapsLists.setMultiDimList(multiDimList_3);
		multiDimMapsLists.setMultiDimList(multiDimList);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiDimMapsLists);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		multiDimList_3.clear();
		multiDimList_3.add(second_1);

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		boolean identical = ((List<?>) multiDimMapsLists_1.getMultiDimList().get(1)).get(1) ==
				((List<?>) multiDimMapsLists_1.getMultiDimList().get(3)).get(0);
		assertTrue(identical);

		multiDimList_3.clear();
		multiDimList_3.add(multiDimMapsLists.getMultiDimList());

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		return;
	}

	@Test
	public void testMultiList_loop()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MultiDimMapsLists multiDimMapsLists = new MultiDimMapsLists();
		MultiDimMapsLists multiDimMapsLists_1;
		boolean equals;

		// init multiDimList
		List<Object> multiDimList = new ArrayList<Object>();
		List<Object> multiDimList_1 = new ArrayList<Object>();

		multiDimList_1.add(multiDimList);

		multiDimList.add(multiDimList_1);

		multiDimMapsLists.setMultiDimList(multiDimList);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiDimMapsLists);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		return;
	}

	@Test
	public void testMultiList_loop_02()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MultiDimMapsLists multiDimMapsLists = new MultiDimMapsLists();
		MultiDimMapsLists multiDimMapsLists_1;
		boolean equals;

		// init multiDimList
		List<Object> multiDimList = new ArrayList<Object>();
		List<Object> multiDimList_1 = new ArrayList<Object>();
		List<Object> multiDimList_2 = new ArrayList<Object>();
		List<Object> multiDimList_3 = new ArrayList<Object>();

		multiDimList_3.add(new Mark("three"));
		multiDimList_3.add(multiDimList_1);
		multiDimList_3.add(multiDimList_1);
		multiDimList_3.add(multiDimList);

		multiDimList_2.add(new Mark("two"));
		multiDimList_2.add(multiDimList_3);
		multiDimList_2.add(multiDimList_1);
		multiDimList_2.add(multiDimList_2);

		multiDimList_1.add(new Mark("one"));
		multiDimList_1.add(multiDimList_2);

		multiDimList.add(new Mark("root"));
		multiDimList.add(multiDimList_1);

		multiDimMapsLists.setMultiDimList(multiDimList);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiDimMapsLists);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		return;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMultiMap()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MultiDimMapsLists multiDimMapsLists = new MultiDimMapsLists();
		MultiDimMapsLists multiDimMapsLists_1;
		boolean equals;
		Address first = new Address();
		DistrictAddress second = new DistrictAddress();
		Address third = new Address();
		Address first_1 = new Address();
		DistrictAddress second_1 = new DistrictAddress();
		Address third_1 = new Address();
		Address first_2 = new Address();
		DistrictAddress second_2 = new DistrictAddress();
		Address third_2 = new Address();

		buildMapTestAny2Any(first, second, third);
		buildMapTestAny2Any(first_1, second_1, third_1);
		buildMapTestAny2Any(first_2, second_2, third_2);

		// init multiDimMap
		Map<Object, Object> multiDimMap = new HashMap<Object, Object>();
		Map<Object, Object> multiDimMap_1 = new HashMap<Object, Object>();
		Map<Object, Object> multiDimMap_2 = new HashMap<Object, Object>();
		Map<Object, Object> multiDimMap_2_2 = new HashMap<Object, Object>();

		multiDimMap_1.put(first_1, second_1);
		multiDimMap_1.put("third", third_1);
		multiDimMap_1.put(third_1, "third again");
		multiDimMap_1.put("four", 4);
		multiDimMap_1.put(5, "five");

		multiDimMap_2_2.put("simple_key", "simple_value");

		multiDimMap_2.put(first_2, second_2);
		multiDimMap_2.put("third", third_2);
		multiDimMap_2.put(third_2, "third again");
		multiDimMap_2.put("six", 6);
		multiDimMap_2.put(7, "seven");
		multiDimMap_2.put(multiDimMap_2_2, 100);

		multiDimMap.put(multiDimMap_1, multiDimMap_2);
		multiDimMap.put("first", multiDimMap_1);
		multiDimMap.put("second", multiDimMap_1);

		multiDimMapsLists.setMultiDimMap(multiDimMap);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiDimMapsLists);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		Map<Object, Object> mDimMap;
		Map<Object, Object> mDimMap_11 = null;
		Map<Object, Object> mDimMap_12 = null;
		Map<Object, Object> mDimMap_13 = null;
		mDimMap = multiDimMapsLists_1.getMultiDimMap();
		Iterator<Entry<Object, Object>> it = mDimMap.entrySet().iterator();
		while (it.hasNext())
		{
			Entry<Object, Object> entry = it.next();
			if (entry.getKey() instanceof Map<?, ?>)
				mDimMap_11 = (Map<Object, Object>) entry.getKey();
			else if (entry.getKey().equals("first"))
				mDimMap_12 = (Map<Object, Object>) entry.getValue();
			else if (entry.getKey().equals("second"))
				mDimMap_13 = (Map<Object, Object>) entry.getValue();
		}
		assertTrue(mDimMap_11 != null && mDimMap_11 == mDimMap_12);
		assertTrue(mDimMap_12 == mDimMap_13);

		// modify - remove reference to multiDimMap_2
		mDimMap.put(mDimMap_11, "was multiDimMap_2");

		errors = da1.store(multiDimMapsLists_1);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists = da.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		return;
	}

	@Test
	public void testMultiMap_EmptyAndNull()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		boolean equals;
		MultiDimMapsLists multiDimMapsLists = new MultiDimMapsLists();
		MultiDimMapsLists multiDimMapsLists_1;
		Address first = new Address();
		DistrictAddress second = new DistrictAddress();
		Address third = new Address();

		buildMapTestAny2Any(first, second, third);
		Map<Object, Object> multiDimMap = new HashMap<Object, Object>();

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		// null map
		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiDimMapsLists);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		// empty map
		multiDimMapsLists.setMultiDimMap(multiDimMap);
		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		multiDimMap.put(first, 100);
		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		multiDimMap.put(first, second);
		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);

		multiDimMap.put(first, "first");
		errors = da.store(multiDimMapsLists);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiDimMapsLists_1 = da1.loadById(MultiDimMapsLists.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMultiDimMapsLists(multiDimMapsLists, multiDimMapsLists_1);
		assertTrue(equals);
		return;
	}

	@Test
	public void testMapAny2Any()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MapContainer mapContainer = new MapContainer();
		MapContainer mapContainer_1;
		Address first = new Address();
		DistrictAddress second = new DistrictAddress();
		Address third = new Address();
		boolean equals;

		buildMapTestAny2Any(first, second, third);

		// init any2Any
		Map<Object, Object> anyMap = new HashMap<Object, Object>();
		anyMap.put(first, second);
		anyMap.put("third", third);
		anyMap.put(third, "third again");
		anyMap.put("four", 4);
		anyMap.put(5, "five");

		mapContainer.setAny2AnyMap(new HashMap<Object, Object>());

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(mapContainer);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(mapContainer);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		mapContainer_1 = da1.loadById(MapContainer.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMapContainer(mapContainer, mapContainer_1);
		assertTrue(equals);

		mapContainer.setAny2AnyMap(anyMap);

		errors = da.store(mapContainer);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		mapContainer_1 = da1.loadById(MapContainer.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMapContainer(mapContainer, mapContainer_1);
		assertTrue(equals);

		// modify any2Any
		anyMap.put(first, third);
		anyMap.put("third", third);
		anyMap.remove(third);
		anyMap.put(second, "second");
		anyMap.put("four", 44);
		anyMap.remove(5);

		// store modification
		errors = da.store(mapContainer);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		mapContainer_1 = da1.loadById(MapContainer.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMapContainer(mapContainer, mapContainer_1);
		assertTrue(equals);

		// modify any2Any
		anyMap.remove("third");
		anyMap.remove(second);
		anyMap.remove("four");

		// store modification
		errors = da.store(mapContainer);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		mapContainer_1 = da1.loadById(MapContainer.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMapContainer(mapContainer, mapContainer_1);
		assertTrue(equals);

		// modify any2Any
		mapContainer.setAny2AnyMap(null);

		// store modification
		errors = da.store(mapContainer);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		mapContainer_1 = da1.loadById(MapContainer.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMapContainer(mapContainer, mapContainer_1);
		assertTrue(equals);

		// modify any2Any
		mapContainer.setAny2AnyMap(new HashMap<Object, Object>());

		// store modification
		errors = da.store(mapContainer);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		mapContainer_1 = da1.loadById(MapContainer.class, -1, syncInfo_1.getId());
		equals = CompareUtil_3.equalsMapContainer(mapContainer, mapContainer_1);
		assertTrue(equals);

		return;
	}

	@Test
	public void testMapSimple2Simple()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MapContainer addressMap = new MapContainer();
		MapContainer addressMap1;
		boolean equals;

		buildMapTestSimple2Simple(addressMap);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(addressMap);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(addressMap);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		addressMap1 = da1.loadById(MapContainer.class, -1, syncInfo_1.getId());
//		equals = CompareUtil_2.equalsBroker(addressMap, addressMap1);
//		assertTrue(equals);

		// modify simple2Simple
		addressMap.getString2IntegerMap().put("one", 2);
		addressMap.getString2IntegerMap().put("two", 3);
		addressMap.getString2IntegerMap().remove("three");


		// store modification
		errors = da.store(addressMap);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		// modify simple2Simple
		addressMap.getString2IntegerMap().remove("one");
		addressMap.getString2IntegerMap().remove("two");

		// store modification
		errors = da.store(addressMap);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		return;
	}

	@Test
	public void testMap()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		MapContainer addressMap = new MapContainer();
		MapContainer addressMap1;
		boolean equals;

		//buildMapTestSimple2Complex(addressMap);
		//buildMapTestComplex2Simple(addressMap);
		//buildMapTestComplex2Complex(addressMap);
		//buildMapTestComplex2Complex_2(addressMap);
		buildMapTestSimple2Simple(addressMap);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(addressMap);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(addressMap);

		// modify simple2Simple
		addressMap.getString2IntegerMap().put("one", 2);
		addressMap.getString2IntegerMap().put("two", 3);
		addressMap.getString2IntegerMap().remove("three");


		// store modification
		errors = da.store(addressMap);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		// modify simple2Simple
		addressMap.getString2IntegerMap().remove("one");
		addressMap.getString2IntegerMap().remove("two");

		// store modification
		errors = da.store(addressMap);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}


		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		addressMap1 = da1.loadById(MapContainer.class, -1, syncInfo_1.getId());
//		equals = CompareUtil_2.equalsBroker(addressMap, addressMap1);
//		assertTrue(equals);

		return;
	}

	@Test
	public void testAmbiguous()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		Broker broker1 = new Broker();
		Broker broker2 = new Broker();
		MultiBroker multiBroker = new MultiBroker();
		Broker broker21;
		Broker broker22;
		boolean equals;

		buildAmbiguousTestObjects(broker1, broker2, multiBroker);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		List<Object> domainObjects = new ArrayList<Object>();

//		((DistrictAddress)((JPerson)broker2.getWorksWith()).getPostalAddress()).setDistrict(null);
//		((JPerson)broker2.getWorksWith()).setCompanyAddress(null);
//		((JPerson)broker2.getWorksWith()).setContactAddress(null);
//		((JPerson)broker2.getWorksWith()).setPostalAddress(null);

		domainObjects.add(broker1);
		domainObjects.add(broker2);

		errors = da.store(domainObjects);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		IPerson person1 = broker1.getWorksWith();
		IPerson person2 = broker2.getWorksWith();
		SyncInfo syncInfo_1 = da.getSyncInfo(broker1);
		SyncInfo syncInfo_2 = da.getSyncInfo(broker2);
		SyncInfo syncInfo_3 = da.getSyncInfo(person1);
		SyncInfo syncInfo_4 = da.getSyncInfo(person2);

		IPerson person21;
		IPerson person22;

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
//		person21 = da1.loadById(IPerson.class, syncInfo_3.getId());
//		equals = CompareUtil_2.equalsIPerson(person1, person21);
//		assertTrue(equals);
//		
//		person22 = da1.loadById(IPerson.class, syncInfo_4.getId());
//		equals = CompareUtil_2.equalsIPerson(person2, person22);
//		assertTrue(equals);

		broker21 = da1.loadById(Broker.class, -1, syncInfo_1.getId());
		equals = CompareUtil_2.equalsBroker(broker1, broker21);
		assertTrue(equals);

		broker22 = da1.loadById(Broker.class, -1, syncInfo_2.getId());
		equals = CompareUtil_2.equalsBroker(broker2, broker22);
		assertTrue(equals);

		((DistrictAddress) ((JPerson) broker2.getWorksWith()).getPostalAddress()).setDistrict(null);
		errors = da.store(broker2);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		broker22 = da1.loadById(Broker.class, -1, syncInfo_2.getId());
		equals = CompareUtil_2.equalsBroker(broker2, broker22);
		assertTrue(equals);

		((JPerson) broker2.getWorksWith()).setCompanyAddress(null);
		errors = da.store(broker2);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		broker22 = da1.loadById(Broker.class, -1, syncInfo_2.getId());
		equals = CompareUtil_2.equalsBroker(broker2, broker22);
		assertTrue(equals);

		((JPerson) broker2.getWorksWith()).setPostalAddress(null);
		errors = da.store(broker2);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		broker22 = da1.loadById(Broker.class, -1, syncInfo_2.getId());
		equals = CompareUtil_2.equalsBroker(broker2, broker22);
		assertTrue(equals);

		((JPerson) broker2.getWorksWith()).setContactAddress(null);
		errors = da.store(broker2);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		broker22 = da1.loadById(Broker.class, -1, syncInfo_2.getId());
		equals = CompareUtil_2.equalsBroker(broker2, broker22);
		assertTrue(equals);

		return;
	}

	@Test
	public void testAmbiguous_02()
	{
		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		Broker broker1 = new Broker();
		Broker broker2 = new Broker();
		MultiBroker multiBroker = new MultiBroker();
		MultiBroker multiBroker1;
		boolean equals;

		buildAmbiguousTestObjects(broker1, broker2, multiBroker);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		errors = da.store(multiBroker);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo_1 = da.getSyncInfo(multiBroker);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		multiBroker1 = da1.loadById(MultiBroker.class, -1, syncInfo_1.getId());
		equals = CompareUtil_2.equalsMultiBroker(multiBroker, multiBroker1);
		assertTrue(equals);

		return;
	}

	@SuppressWarnings({"unused", "rawtypes", "unchecked"})
	@Test
	public void testUpdateComplex_EmptyList2NotEmptyList()
	{

		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;
		Person john_1;
		boolean equals;

		Person john = new Person();

		buildInitialDomainObjects_2(john);
		List addresses = john.getAddresses();
		john.setAddresses(new ArrayList());

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		// Store empty array without generics
		errors = da.store(john);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		SyncInfo syncInfo = da.getSyncInfo(john);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		john_1 = da1.loadById(Person.class, -1, syncInfo.getId());
		equals = CompareUtil.equalsPerson(john, john_1);
		assertTrue(equals);

		// Store null array
		john.setAddresses(null);
		errors = da.store(john);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		john_1 = da1.loadById(Person.class, -1, syncInfo.getId());
		equals = CompareUtil.equalsPerson(john, john_1);
		assertTrue(equals);

		// Store non-empty array without generics
		john.setAddresses(addresses);
		errors = da.store(john);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		// test if property for empty collection has been removed
//		da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
//		errors = da.store(john);
//		if (errors.size() > 0) {
//			printErrors(errors);
//			throw new JcResultException(errors);
//		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		john_1 = da1.loadById(Person.class, -1, syncInfo.getId());
		equals = CompareUtil.equalsPerson(john, john_1);
		assertTrue(equals);

		Address addr = (Address) john.getAddresses().remove(john.getAddresses().size() - 1);
		john.getAddresses().add(0, addr);
		errors = da.store(john);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		john_1 = da1.loadById(Person.class, -1, syncInfo.getId());
		equals = CompareUtil.equalsPerson(john, john_1);
		assertTrue(equals);

		return;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateSimple_EmptyList2NotEmptyList()
	{

		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;

		Person john = new Person();
		Address address = new Address();
		Contact phone = new Contact();
		Contact email = new Contact();
		Person james = new Person();
		Company skynet = new Company();
		Company globCom = new Company();

		buildInitialDomainObjects_1(john, james, address, phone, email, skynet, globCom);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		// Store empty array without generics
		errors = da.store(globCom);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		SyncInfo syncInfo = da.getSyncInfo(globCom);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		Company globCom_1;
		globCom_1 = da1.loadById(Company.class, -1, syncInfo.getId());

		boolean isEqual = CompareUtil.equalsCompany(globCom, globCom_1);
		assertTrue("Test for equality of domain objects", isEqual);

		globCom.getAreaCodes().add(2);
		globCom.getAreaCodes().add(3);
		// Store non-empty array without generics
		errors = da.store(globCom);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		globCom_1 = da1.loadById(Company.class, -1, syncInfo.getId());

		isEqual = CompareUtil.equalsCompany(globCom, globCom_1);
		assertTrue("Test for equality of domain objects", isEqual);

		// Store non-empty array with generics
		james.setLuckyNumbers(new ArrayList<Integer>());
		errors = da.store(james);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		syncInfo = da.getSyncInfo(james);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		Person james_1;
		james_1 = da1.loadById(Person.class, -1, syncInfo.getId());

		isEqual = CompareUtil.equalsPerson(james, james_1);
		assertTrue("Test for equality of domain objects", isEqual);

		james.getLuckyNumbers().add(24);
		james.getLuckyNumbers().add(48);
		errors = da.store(james);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		james_1 = da1.loadById(Person.class, -1, syncInfo.getId());

		isEqual = CompareUtil.equalsPerson(james, james_1);
		assertTrue("Test for equality of domain objects", isEqual);

		return;
	}

	@Test
	public void testLoadEmptyLists()
	{

		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);

		Person john = new Person();
		Address address = new Address();
		Contact phone = new Contact();
		Contact email = new Contact();
		Person james = new Person();
		Company skynet = new Company();
		Company globCom = new Company();

		buildInitialDomainObjects_1(john, james, address, phone, email, skynet, globCom);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		// Test empty array without generics
		errors = da.store(globCom);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		SyncInfo syncInfo = da.getSyncInfo(globCom);

		da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		Company globCom_1;
		globCom_1 = da.loadById(Company.class, -1, syncInfo.getId());

		boolean isEqual = CompareUtil.equalsCompany(globCom, globCom_1);
		assertTrue("Test for equality of domain objects", isEqual);

		// Test empty array with generics
		james.setLuckyNumbers(new ArrayList<Integer>());
		errors = da.store(james);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		syncInfo = da.getSyncInfo(james);

		da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		Person james_1;
		james_1 = da.loadById(Person.class, -1, syncInfo.getId());

		isEqual = CompareUtil.equalsPerson(james, james_1);
		assertTrue("Test for equality of domain objects", isEqual);

		return;
	}

	@Test
	public void testStoreDomainObjects()
	{
		Person john_1, james_1;
		Address addr_1;

		List<JcError> errors;
		IDomainAccess da = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		IDomainAccess da1;

		Person john = new Person();
		Address address = new Address();
		Contact phone = new Contact();
		Contact email = new Contact();
		Person james = new Person();
		Company skynet = new Company();
		Company globCom = new Company();

		buildInitialDomainObjects_1(john, james, address, phone, email, skynet, globCom);

		List<Object> domainObjects = new ArrayList<Object>();
		domainObjects.add(john);
		domainObjects.add(james);

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		// check for an empty graph
		boolean check = dbAccess.isDatabaseEmpty();
		assertTrue("Test for empty graph", check);

		errors = da.store(domainObjects);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		SyncInfo syncInfo = da.getSyncInfo(john);
		john_1 = da.loadById(Person.class, -1, syncInfo.getId());
		boolean isIdentical = john == john_1;
		assertTrue("Test for identity of domain objects", isIdentical);

		// add a new class to the domain to check another
		// initialize DomainInfo scenario
		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		errors = da1.store(skynet);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}
		check = checkDomainInfoNodeAgainst("[Address=test.domainmapping.Address," +
				" Collection=iot.jcypher.domain.mapping.surrogate.Collection, Company=test.domainmapping.Company," +
				" Contact=test.domainmapping.Contact, Person=test.domainmapping.Person]");
		assertTrue("Test Domain Info node", check);

		da1 = DomainAccessFactory.createDomainAccess(dbAccess, domainName);
		try
		{
			john_1 = da1.loadById(Person.class, -1, syncInfo.getId());
		} catch (Exception e)
		{
			if (e instanceof JcResultException)
			{
				errors = ((JcResultException) e).getErrors();
				printErrors(errors);
				return;
			}
			throw e;
		}
		boolean isEqual = CompareUtil.equalsPerson(john, john_1);
		assertTrue("Test for equality of domain objects", isEqual);

		List<NodesToCheck> ntc = new ArrayList<NodesToCheck>();
		ntc.add(new NodesToCheck("Person", 2));
		ntc.add(new NodesToCheck("Address", 2));
		ntc.add(new NodesToCheck("Contact", 1));
		ntc.add(new NodesToCheck("Company", 1));
		ntc.add(new NodesToCheck("Collection", 2));
		List<RelationsToCheck> rtc = new ArrayList<RelationsToCheck>();
		rtc.add(new RelationsToCheck("address", 1));
		rtc.add(new RelationsToCheck("mainAddress", 1));
		rtc.add(new RelationsToCheck("contact", 1));
		rtc.add(new RelationsToCheck("luckyNumbers", 1));
		rtc.add(new RelationsToCheck("areaCodes", 1));

		check = checkForNodesAndRelations(ntc, rtc);
		assertTrue("Test for nodes and relations in graph", check);
		return;
	}

	private boolean checkDomainInfoNodeAgainst(String expected)
	{
		JcNode info = new JcNode("info");
		JcQuery query = new JcQuery();
		query.setClauses(new IClause[]{
				MATCH.node(info).label("DomainInfo"),
				WHERE.valueOf(info.property("name"))
						.EQUALS(domainName),
				RETURN.value(info)
		});
		JcQueryResult result = dbAccess.execute(query);
		List<JcError> errors = Util.collectErrors(result);
		if (!errors.isEmpty())
		{
			throw new JcResultException(errors);
		}
		GrNode rInfo = result.resultOf(info).get(0);
		GrProperty prop = rInfo.getProperty("label2ClassMap");
		Object val = prop.getValue();
		String returned = val.toString();
		return expected.equals(returned);
	}

	private void buildMapTestAny2Any(Address first, DistrictAddress second, Address third)
	{
		first.setCity("Munich");
		first.setStreet("Main Street");
		first.setNumber(9);

		second.setCity("San Francisco");
		second.setStreet("Embarcadero Center");
		second.setNumber(1);
		District district = new District();
		district.setName("District thirteen");
		second.setDistrict(district);
		district = new District();
		district.setName("Subdistrict four");
		second.setSubDistrict(district);

		third.setCity("New York");
		third.setStreet("52nd Street");
		third.setNumber(35);
	}

	private void buildMapTestSimple2Simple(MapContainer addressMap)
	{
		Map<String, Integer> s2i = new HashMap<String, Integer>();
		addressMap.setString2IntegerMap(s2i);

		s2i.put("one", 1);
		s2i.put("two", 2);
		s2i.put("three", 3);
	}

	private void buildMapTestSimple2Complex(MapContainer addressMap)
	{
		Map<String, Address> addresses = new HashMap<String, Address>();
		addressMap.setString2AddressMap(addresses);

		Address address = new Address();
		address.setCity("Munich");
		address.setStreet("Main Street");
		address.setNumber(9);
		addresses.put("first", address);

		DistrictAddress dAddress = new DistrictAddress();
		dAddress.setCity("San Francisco");
		dAddress.setStreet("Embarcadero Center");
		dAddress.setNumber(1);
		District district = new District();
		district.setName("District thirteen");
		dAddress.setDistrict(district);
		district = new District();
		district.setName("Subdistrict four");
		dAddress.setSubDistrict(district);
		addresses.put("second", dAddress);

		address = new Address();
		address.setCity("New York");
		address.setStreet("52nd Street");
		address.setNumber(35);
		addresses.put("third", address);

	}

	private void buildMapTestComplex2Simple(MapContainer addressMap)
	{
		Map<Address, String> addresses = new HashMap<Address, String>();
		addressMap.setAddress2StringMap(addresses);

		Address address = new Address();
		address.setCity("Munich");
		address.setStreet("Main Street");
		address.setNumber(9);
		addresses.put(address, "first");

		DistrictAddress dAddress = new DistrictAddress();
		dAddress.setCity("San Francisco");
		dAddress.setStreet("Embarcadero Center");
		dAddress.setNumber(1);
		District district = new District();
		district.setName("District thirteen");
		dAddress.setDistrict(district);
		district = new District();
		district.setName("Subdistrict four");
		dAddress.setSubDistrict(district);
		addresses.put(dAddress, "second");

		address = new Address();
		address.setCity("New York");
		address.setStreet("52nd Street");
		address.setNumber(35);
		addresses.put(address, "third");

	}

	private void buildMapTestComplex2Complex(MapContainer addressMap)
	{
		Map<Address, Address> addresses = new HashMap<Address, Address>();
		addressMap.setAddress2AddressMap(addresses);

		Address first = new Address();
		first.setCity("Munich");
		first.setStreet("Main Street");
		first.setNumber(9);

		DistrictAddress second = new DistrictAddress();
		second.setCity("San Francisco");
		second.setStreet("Embarcadero Center");
		second.setNumber(1);
		District district = new District();
		district.setName("District thirteen");
		second.setDistrict(district);
		district = new District();
		district.setName("Subdistrict four");
		second.setSubDistrict(district);

		Address third = new Address();
		third.setCity("New York");
		third.setStreet("52nd Street");
		third.setNumber(35);

		addresses.put(first, second);
		addresses.put(second, third);
		addresses.put(third, first);

	}

	private void buildMapTestComplex2Complex_2(MapContainer addressMap)
	{
		Map<Address, Address> addresses = new HashMap<Address, Address>();
		addressMap.setAddress2AddressMap(addresses);

		Address first = new Address();
		first.setCity("Munich");
		first.setStreet("Main Street");
		first.setNumber(9);

		Address first_1 = new Address();
		first_1.setCity("Munich");
		first_1.setStreet("Main Street");
		first_1.setNumber(9);

		DistrictAddress second = new DistrictAddress();
		second.setCity("San Francisco");
		second.setStreet("Embarcadero Center");
		second.setNumber(1);
		District district = new District();
		district.setName("District thirteen");
		second.setDistrict(district);
		district = new District();
		district.setName("Subdistrict four");
		second.setSubDistrict(district);

		DistrictAddress second_1 = new DistrictAddress();
		second_1.setCity("San Francisco");
		second_1.setStreet("Embarcadero Center");
		second_1.setNumber(1);
		district = new District();
		district.setName("District thirteen");
		second_1.setDistrict(district);
		district = new District();
		district.setName("Subdistrict four");
		second_1.setSubDistrict(district);

		Address third = new Address();
		third.setCity("New York");
		third.setStreet("52nd Street");
		third.setNumber(35);

		Address third_1 = new Address();
		third_1.setCity("New York");
		third_1.setStreet("52nd Street");
		third_1.setNumber(35);

		addresses.put(first, second_1);
		addresses.put(second, third_1);
		addresses.put(third, first_1);

	}

	private List<Object> buildAmbiguousTestObjects(Broker broker1, Broker broker2,
	                                               MultiBroker multiBroker)
	{
		List<Object> ret = new ArrayList<Object>();

		Address address = new Address();
		address.setCity("Munich");
		address.setStreet("Main Street");
		address.setNumber(9);

		ret.add(address);

		NPerson nPerson = new NPerson();
		nPerson.setNamePart1("Sam");
		nPerson.setNamePart2("Smith");
		nPerson.setSocialSecurityNumber("123456");
		nPerson.setHomeAddress(address);
		address = new Address();
		address.setCity("Munich");
		address.setStreet("Bahnhofplatz");
		address.setNumber(2);
		nPerson.setWorkAddress(address);

		ret.add(address);

		address = new Address();
		address.setCity("San Francisco");
		address.setStreet("Kearny Street");
		address.setNumber(28);

		ret.add(address);

		JPerson jPerson = new JPerson();
		jPerson.setNamePart1("Global Company");
		jPerson.setNamePart2("incorporated");
		jPerson.setCompanyNumber(42);
		jPerson.setCompanyAddress(address);
		address = new Address();
		address.setCity("San Francisco");
		address.setStreet("Market Street");
		address.setNumber(29);
		jPerson.setContactAddress(address);

		ret.add(address);

		DistrictAddress dAddress = new DistrictAddress();
		dAddress.setCity("San Francisco");
		dAddress.setStreet("Embarcadero Center");
		dAddress.setNumber(1);
		District district = new District();
		district.setName("District thirteen");
		dAddress.setDistrict(district);
		district = new District();
		district.setName("Subdistrict four");
		dAddress.setSubDistrict(district);
		jPerson.setPostalAddress(dAddress);

		ret.add(dAddress);

		broker1.setWorksWith(nPerson);
		address = new Address();
		address.setCity("New York");
		address.setStreet("52nd Street");
		address.setNumber(35);
		broker1.setAddress(address);

		ret.add(address);

		broker2.setWorksWith(jPerson);
		address = new Address();
		address.setCity("Brussels");
		address.setStreet("Main Road");
		address.setNumber(168);
		broker2.setAddress(address);

		ret.add(address);

		List<IPerson> persons = new ArrayList<IPerson>();
		persons.add(jPerson);
		persons.add(nPerson);
		multiBroker.setCanBroker(persons);
		multiBroker.setName("Jack Broker");
		address = new Address();
		address.setCity("Vienna");
		address.setStreet("Am Graben");
		address.setNumber(5);
		multiBroker.setAddress(address);

//		ret.add(address);

		return ret;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void buildInitialDomainObjects_1(Person john, Person james,
	                                         Address address, Contact phone, Contact email, Company skynet,
	                                         Company globCom)
	{
		john.setFirstName("John");
		john.setLastName("Reeves");
		Calendar cal = Calendar.getInstance();
		cal.set(1964, 8, 2, 0, 0, 0);
		clearMillis(cal);
		john.setBirthDate(cal.getTime());

		List<Integer> luckyNumbers = new ArrayList<Integer>();
		luckyNumbers.add(3);
		luckyNumbers.add(9);
		luckyNumbers.add(12);
		luckyNumbers.add(15);
		luckyNumbers.add(23);
		john.setLuckyNumbers(luckyNumbers);

		address.setCity("Vienna");
		address.setStreet("Main Street");
		address.setNumber(9);
		john.setMainAddress(address);

		phone.setType(ContactType.TELEPHONE);
		phone.setNummer("12345");
		john.setContact(phone);

		email.setType(ContactType.EMAIL);
		email.setNummer("dj@nowhere.org");

		james.setFirstName("James");
		james.setLastName("Fishburne");
		cal = Calendar.getInstance();
		cal.set(1961, 6, 30, 0, 0, 0);
		clearMillis(cal);
		james.setBirthDate(cal.getTime());

		if (skynet != null)
		{
			skynet.setName("Sky-Net");
			Address addr = new Address();
			addr.setCity("Global City");
			addr.setStreet("Graphstreet");
			addr.setNumber(42);
			skynet.setAddress(addr);

			List areaCodes = new ArrayList();
			areaCodes.add(42);
			areaCodes.add(43);
			areaCodes.add(44);
			skynet.setAreaCodes(areaCodes);
		}

		if (globCom != null)
		{
			globCom.setName("Glob-Com");
			Address addr = new Address();
			addr.setCity("Global City");
			addr.setStreet("Mainstreet");
			addr.setNumber(1);
			globCom.setAddress(addr);

			globCom.setAreaCodes(new ArrayList());
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void buildInitialDomainObjects_2(Person john)
	{
		john.setFirstName("John");
		john.setLastName("Reeves");
		Calendar cal = Calendar.getInstance();
		cal.set(1964, 8, 2, 0, 0, 0);
		clearMillis(cal);
		john.setBirthDate(cal.getTime());
		john.setAddresses(new ArrayList());

		Address address = new Address();
		address.setCity("Munich");
		address.setStreet("Main Street");
		address.setNumber(9);
		john.getAddresses().add(address);

		address = new Address();
		address.setCity("San Francisco");
		address.setStreet("Kearny Street");
		address.setNumber(28);
		john.getAddresses().add(address);

		address = new Address();
		address.setCity("Paris");
		address.setStreet("boulevard de clichy");
		address.setNumber(108);
		john.getAddresses().add(address);
	}

	/**
	 * @param nodesToCheck
	 * @param relationsToCheck
	 * @return true for success
	 */
	private boolean checkForNodesAndRelations(List<NodesToCheck> nodesToCheck,
	                                          List<RelationsToCheck> relationsToCheck)
	{
		boolean ret = true;
		boolean checkForNodes = nodesToCheck != null && nodesToCheck.size() > 0;
		boolean checkForRelations = relationsToCheck != null && relationsToCheck.size() > 0;
		if (checkForNodes || checkForRelations)
		{
			List<IClause> clauses = new ArrayList<IClause>();
			if (checkForNodes)
			{
				int idx = -1;
				for (NodesToCheck ntc : nodesToCheck)
				{
					idx++;
					JcNode n = new JcNode("n_".concat(String.valueOf(idx)));
					if (idx > 0)
						clauses.add(SEPARATE.nextClause());
					if (ntc.label != null)
						clauses.add(MATCH.node(n).label(ntc.label));
					else
						clauses.add(MATCH.node(n));
				}
			}

			if (checkForRelations)
			{
				int idx = -1;
				for (RelationsToCheck rtc : relationsToCheck)
				{
					idx++;
					JcRelation r = new JcRelation("r_".concat(String.valueOf(idx)));
					if (clauses.size() > 0)
						clauses.add(SEPARATE.nextClause());
					if (rtc.type != null)
						clauses.add(MATCH.node().relation(r).type(rtc.type).node());
					else
						clauses.add(MATCH.node().relation(r).node());
				}
			}
			clauses.add(RETURN.ALL());
			JcQuery query = new JcQuery();
			query.setClauses(clauses.toArray(new IClause[clauses.size()]));
//			Util.printQuery(query, "CHECK", Format.PRETTY_1);
			JcQueryResult result = dbAccess.execute(query);
			if (result.hasErrors())
			{
				List<JcError> errors = Util.collectErrors(result);
				throw new JcResultException(errors);
			}
//			Util.printResult(result, "CHECK", Format.PRETTY_1);

			// perform check
			if (checkForNodes)
			{
				int idx = -1;
				for (NodesToCheck ntc : nodesToCheck)
				{
					idx++;
					JcNode n = new JcNode("n_".concat(String.valueOf(idx)));
					List<GrNode> nodes = result.resultOf(n);
					nodes = removeMultiple(nodes);
					ret = nodes.size() == ntc.count;
					if (!ret)
						break;
				}
			}

			if (checkForRelations && ret)
			{
				int idx = -1;
				for (RelationsToCheck rtc : relationsToCheck)
				{
					idx++;
					JcRelation r = new JcRelation("r_".concat(String.valueOf(idx)));
					List<GrRelation> relations = result.resultOf(r);
					relations = removeMultiple(relations);
					ret = relations.size() == rtc.count;
					if (!ret)
						break;
				}
			}
		} else
		{ // check for an empty graph
			ret = dbAccess.isDatabaseEmpty();
		}
		return ret;
	}

	private <T extends GrPropertyContainer> List<T> removeMultiple(List<T> source)
	{
		List<T> ret = new ArrayList<T>();
		for (T elem : source)
		{
			if (!containsElement(ret, elem))
				ret.add(elem);
		}
		return ret;
	}

	private <T extends GrPropertyContainer> boolean containsElement(List<T> elems, T elem)
	{
		for (T pc : elems)
		{
			if (pc.getId() == elem.getId())
				return true;
		}
		return false;
	}

	private void clearMillis(Calendar cal)
	{
		long millis = cal.getTimeInMillis();
		long nMillis = millis / 1000;
		nMillis = nMillis * 1000;
		nMillis = nMillis - 1000;
		cal.setTimeInMillis(nMillis);
	}

	/************************************/
	private static class NodesToCheck
	{
		private String label;
		private int count;

		private NodesToCheck(String label, int count)
		{
			super();
			this.label = label;
			this.count = count;
		}
	}

	/************************************/
	private static class RelationsToCheck
	{
		private String type;
		private int count;

		private RelationsToCheck(String type, int count)
		{
			super();
			this.type = type;
			this.count = count;
		}
	}
}
