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

package test.genericmodel;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.domain.DomainAccessFactory;
import iot.jcypher.domain.IGenericDomainAccess;
import iot.jcypher.domain.genericmodel.DOType;
import iot.jcypher.domain.genericmodel.DOType.DOClassBuilder;
import iot.jcypher.domain.genericmodel.DOType.DOEnumBuilder;
import iot.jcypher.domain.genericmodel.DOType.DOInterfaceBuilder;
import iot.jcypher.domain.genericmodel.DOTypeBuilderFactory;
import iot.jcypher.domain.genericmodel.DomainObject;
import iot.jcypher.domain.genericmodel.InternalAccess;
import iot.jcypher.domain.genericmodel.internal.DOWalker;
import iot.jcypher.domain.internal.IIntDomainAccess;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.result.JcResultException;
import iot.jcypher.query.writer.Format;
import iot.jcypher.transaction.ITransaction;
import iot.jcypher.util.QueriesPrintObserver;
import iot.jcypher.util.QueriesPrintObserver.ContentToObserve;
import iot.jcypher.util.QueriesPrintObserver.QueryToObserve;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.AbstractTestSuite;
import test.DBAccessSettings;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.*;

//@Ignore
public class CreateGenericModelTest extends AbstractTestSuite
{

	public static IDBAccess dbAccess;
	public static String domainName;

	@BeforeClass
	public static void before()
	{
		domainName = "PEOPLE-DOMAIN"; // "QTEST-DOMAIN";
		dbAccess = DBAccessSettings.createDBAccess();

		QueriesPrintObserver.addOutputStream(System.out);

		QueriesPrintObserver.addToEnabledQueries(QueryToObserve.COUNT_QUERY, ContentToObserve.CYPHER);
		QueriesPrintObserver.addToEnabledQueries(QueryToObserve.DOM_QUERY, ContentToObserve.CYPHER);
		QueriesPrintObserver.addToEnabledQueries(QueryToObserve.DOMAIN_INFO, ContentToObserve.CYPHER);
		QueriesPrintObserver.addToEnabledQueries(QueryToObserve.QUERY_CONCRETE_TYPE, ContentToObserve.CYPHER);
		QueriesPrintObserver.addToEnabledQueries(QueryToObserve.LOAD_BY_TYPE_QUERY, ContentToObserve.CYPHER);
		//QueriesPrintObserver.addToEnabledQueries(QueryToObserve.UPDATE_QUERY, ContentToObserve.CYPHER);
	}

	@AfterClass
	public static void after()
	{
		if (dbAccess != null)
		{
			dbAccess.close();
			dbAccess = null;
		}
		QueriesPrintObserver.removeFromEnabledQueries(QueryToObserve.DOMAIN_INFO);
		QueriesPrintObserver.removeFromEnabledQueries(QueryToObserve.QUERY_CONCRETE_TYPE);
		QueriesPrintObserver.removeFromEnabledQueries(QueryToObserve.LOAD_BY_TYPE_QUERY);
	}

	@Test
	public void testTransactions_01()
	{
		IGenericDomainAccess gda;
		List<JcError> errors;

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		gda = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);

		boolean classNotFound = false;
		try
		{
			gda.loadByType("mytest.model.Person", -1, 0, -1);
		} catch (Throwable e)
		{
			if (e.getCause() instanceof ClassNotFoundException)
				classNotFound = true;
			else if (e instanceof RuntimeException)
				throw e;
			else
				throw new RuntimeException(e);
		}
		assertTrue(classNotFound);

		ITransaction tx = gda.beginTX();
		DOTypeBuilderFactory tpf = gda.getTypeBuilderFactory();

		DOEnumBuilder subjectTypesBuilder = tpf.createEnumBuilder("mytest.model.SubjectTypes");
		subjectTypesBuilder.addEnumValue("NAT_PERSON");
		subjectTypesBuilder.addEnumValue("JUR_PERSON");
		DOType subjectTypes = subjectTypesBuilder.build();

		DOInterfaceBuilder pointOfContactBuilder = tpf.createInterfaceBuilder("mytest.model.PointOfContact");
		DOType pointOfContact = pointOfContactBuilder.build();

		DOClassBuilder addressBuilder = tpf.createClassBuilder("mytest.model.Address");
		addressBuilder.addInterface(pointOfContact);
		addressBuilder.addField("street", String.class.getName());
		addressBuilder.addField("number", int.class.getName());
		DOType addressType = addressBuilder.build();

		DOClassBuilder subjectTypeBuilder = tpf.createClassBuilder("mytest.model.Subject");
		subjectTypeBuilder.setAbstract();
		subjectTypeBuilder.addField("subjectType", subjectTypes.getName());
		subjectTypeBuilder.addListField("pointsOfContact", "mytest.model.PointOfContact");
		DOType subject = subjectTypeBuilder.build();

		DOClassBuilder personTypeBuilder = tpf.createClassBuilder("mytest.model.Person");
		personTypeBuilder.addField("firstName", String.class.getName());
		personTypeBuilder.addField("lastName", String.class.getName());
		personTypeBuilder.addField("birthDate", Date.class.getName());
		personTypeBuilder.setSuperType(subject);
		DOType personType = personTypeBuilder.build();

		DomainObject anAddress = new DomainObject(addressType);
		anAddress.setFieldValue("street", "Market Street");
		anAddress.setFieldValue("number", 42);

		DomainObject aPerson = new DomainObject(personType);
		aPerson.setFieldValue("firstName", "Maxwell");
		aPerson.setFieldValue("lastName", "Smart");
		GregorianCalendar cal = new GregorianCalendar(1940, 0, 22);
		Date birthDate = cal.getTime();
		aPerson.setFieldValue("birthDate", birthDate);
		aPerson.setFieldValue("subjectType", subjectTypes.getEnumValue("NAT_PERSON"));

		aPerson.addListFieldValue("pointsOfContact", anAddress);

		Object address = aPerson.getListFieldValue("pointsOfContact", 0);

		assertTrue(anAddress == address);
		assertTrue(InternalAccess.getRawObject(anAddress) == InternalAccess.getRawObject((DomainObject) address));

		String nursery_1 = ((IIntDomainAccess) gda.getDomainAccess()).getInternalDomainAccess().nurseryAsString();
		assertEquals("[mytest.model.Address(1), mytest.model.Person(1)]", nursery_1);

		errors = gda.store(aPerson);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		String nursery_2 = ((IIntDomainAccess) gda.getDomainAccess()).getInternalDomainAccess().nurseryAsString();
		assertEquals("[mytest.model.Address(1)]", nursery_2);

		Object addr = aPerson.getListFieldValue("pointsOfContact", 0);
		assertTrue(address == addr);
		assertTrue(InternalAccess.getRawObject((DomainObject) address) == InternalAccess.getRawObject((DomainObject) addr));
		String nursery_3 = ((IIntDomainAccess) gda.getDomainAccess()).getInternalDomainAccess().nurseryAsString();
		assertEquals("[]", nursery_3);

		tx.failure();
		errors = tx.close();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		addr = aPerson.getListFieldValue("pointsOfContact", 0);
		assertTrue(addr instanceof DomainObject); // nursery has been reset correctly

		IGenericDomainAccess gda1 = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		List<DomainObject> objects_2 = gda1.loadByType("mytest.model.Person", -1, 0, -1);
		assertTrue(objects_2.isEmpty());

		errors = gda.store(aPerson);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		gda1 = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		objects_2 = gda1.loadByType("mytest.model.Person", -1, 0, -1);
		assertTrue(objects_2.size() == 1);

		DOToString doToString = new DOToString(Format.PRETTY_1);
		DOWalker walker = new DOWalker(aPerson, doToString);
		walker.walkDOGraph();
		String str = doToString.getBuffer().toString();

		doToString = new DOToString(Format.PRETTY_1);
		walker = new DOWalker(objects_2, doToString);
		walker.walkDOGraph();
		String str2 = doToString.getBuffer().toString();

		assertEquals(str, str2);

		return;
	}

	@Test
	public void testCreateGenericModel_01()
	{
		IGenericDomainAccess gda;
		List<JcError> errors;

		errors = dbAccess.clearDatabase();
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		gda = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		DOTypeBuilderFactory tpf = gda.getTypeBuilderFactory();

		DOEnumBuilder subjectTypesBuilder = tpf.createEnumBuilder("mytest.model.SubjectTypes");
		subjectTypesBuilder.addEnumValue("NAT_PERSON");
		subjectTypesBuilder.addEnumValue("JUR_PERSON");
		DOType subjectTypes = subjectTypesBuilder.build();

		DOInterfaceBuilder pointOfContactBuilder = tpf.createInterfaceBuilder("mytest.model.PointOfContact");
		DOType pointOfContact = pointOfContactBuilder.build();

		DOClassBuilder addressBuilder = tpf.createClassBuilder("mytest.model.Address");
		addressBuilder.addInterface(pointOfContact);
		addressBuilder.addField("street", String.class.getName());
		addressBuilder.addField("number", int.class.getName());
		DOType addressType = addressBuilder.build();

		DOClassBuilder subjectTypeBuilder = tpf.createClassBuilder("mytest.model.Subject");
		subjectTypeBuilder.setAbstract();
		subjectTypeBuilder.addField("subjectType", subjectTypes.getName());
		subjectTypeBuilder.addListField("pointsOfContact", "mytest.model.PointOfContact");
		DOType subject = subjectTypeBuilder.build();

		DOClassBuilder personTypeBuilder = tpf.createClassBuilder("mytest.model.Person");
		personTypeBuilder.addField("firstName", String.class.getName());
		personTypeBuilder.addField("lastName", String.class.getName());
		personTypeBuilder.addField("birthDate", Date.class.getName());
		personTypeBuilder.setSuperType(subject);
		DOType personType = personTypeBuilder.build();

		String domModel_0 = ((IIntDomainAccess) gda.getDomainAccess()).getInternalDomainAccess().domainModelAsString();

		DomainObject anAddress = new DomainObject(addressType);
		anAddress.setFieldValue("street", "Market Street");
		anAddress.setFieldValue("number", 42);

		DomainObject aPerson = new DomainObject(personType);
		aPerson.setFieldValue("firstName", "Maxwell");
		aPerson.setFieldValue("lastName", "Smart");
		GregorianCalendar cal = new GregorianCalendar(1940, 0, 22);
		Date birthDate = cal.getTime();
		aPerson.setFieldValue("birthDate", birthDate);
		aPerson.setFieldValue("subjectType", subjectTypes.getEnumValue("NAT_PERSON"));

		aPerson.addListFieldValue("pointsOfContact", anAddress);

		Object address = aPerson.getListFieldValue("pointsOfContact", 0);

		assertTrue(anAddress == address);
		assertTrue(InternalAccess.getRawObject(anAddress) == InternalAccess.getRawObject((DomainObject) address));

		String domModel_1 = ((IIntDomainAccess) gda.getDomainAccess()).getInternalDomainAccess().domainModelAsString();
		assertEquals(domModel_0, domModel_1);

		Object[] enumVals = subjectTypes.getEnumValues();
		assertEquals("NAT_PERSON", enumVals[0].toString());
		assertEquals("JUR_PERSON", enumVals[1].toString());
		List<DomainObject> persons = new ArrayList<DomainObject>();
		persons.add(aPerson);

		String nursery_1 = ((IIntDomainAccess) gda.getDomainAccess()).getInternalDomainAccess().nurseryAsString();
		assertEquals("[mytest.model.Address(1), mytest.model.Person(1)]", nursery_1);

		//		errors = gda.store(aPerson);
		errors = gda.store(persons);
		if (errors.size() > 0)
		{
			printErrors(errors);
			throw new JcResultException(errors);
		}

		String nursery_2 = ((IIntDomainAccess) gda.getDomainAccess()).getInternalDomainAccess().nurseryAsString();
		assertEquals("[mytest.model.Address(1)]", nursery_2);

		String domModel_2 = ((IIntDomainAccess) gda.getDomainAccess()).getInternalDomainAccess().domainModelAsString();

		// remove model version info
		domModel_1 = domModel_1.substring(domModel_1.indexOf('{'), domModel_1.length());
		domModel_2 = domModel_2.substring(domModel_2.indexOf('{'), domModel_2.length());
		assertEquals(domModel_1, domModel_2);

		List<DomainObject> objects = gda.loadByType("mytest.model.Person", -1, 0, -1);
		assertTrue(aPerson == objects.get(0));
		assertTrue(InternalAccess.getRawObject(aPerson) == InternalAccess.getRawObject((DomainObject) objects.get(0)));

		Object addr = aPerson.getListFieldValue("pointsOfContact", 0);
		assertTrue(address == addr);
		assertTrue(InternalAccess.getRawObject((DomainObject) address) == InternalAccess.getRawObject((DomainObject) addr));
		String nursery_3 = ((IIntDomainAccess) gda.getDomainAccess()).getInternalDomainAccess().nurseryAsString();
		assertEquals("[]", nursery_3);

		IGenericDomainAccess gda1 = DomainAccessFactory.createGenericDomainAccess(dbAccess, domainName);
		List<DomainObject> objects_2 = gda1.loadByType("mytest.model.Person", -1, 0, -1);

		assertFalse(objects.get(0) == objects_2.get(0));
		assertFalse(InternalAccess.getRawObject(objects.get(0)) == InternalAccess.getRawObject((DomainObject) objects_2.get(0)));

		Object addr_2 = ((DomainObject) objects_2.get(0)).getListFieldValue("pointsOfContact", 0);
		assertFalse(address == addr_2);
		assertFalse(InternalAccess.getRawObject((DomainObject) address) == InternalAccess.getRawObject((DomainObject) addr_2));

		DOToString doToString = new DOToString(Format.PRETTY_1);
		DOWalker walker = new DOWalker(aPerson, doToString);
		walker.walkDOGraph();
		String str = doToString.getBuffer().toString();

		doToString = new DOToString(Format.PRETTY_1);
		walker = new DOWalker(objects_2.get(0), doToString);
		walker.walkDOGraph();
		String str2 = doToString.getBuffer().toString();

		assertEquals(str, str2);

		return;
	}
}
