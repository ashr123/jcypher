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

package test.domainquery;

import test.domainquery.model.*;
import test.domainquery.model.EContact.EContactType;

import java.util.ArrayList;
import java.util.List;

public class Population
{

	private Area earth;
	private Area northAmerica;
	private Area usa;
	private Area california;
	private Area sanFrancisco;
	private Area europe;
	private Area germany;
	private Area munich;
	private Area newYork;
	private Area newYorkCity;
	private Area austria;
	private Area vienna;
	private Area vienna_17;
	private Area vienna_01;
	private EArea electronicAreaSF;
	private EArea electronicAreaUSA;

	private Address marketStreet_20;
	private Address schwedenPlatz_32;
	private Address stachus_1;
	private Person john_smith;

	private List<Object> berghammers;
	private List<Object> smiths;
	private List<Object> angelina_smith;
	private List<Object> john_jery_smith;
	private List<Object> caro_angie_smith;
	private List<Object> angie_clark;
	private List<Object> angies;
	private List<Object> watson_company;
	private List<Object> maier_clark;
	private List<Object> smiths_christa_berghammer_globcom;
	private List<Object> berghammers_globcom;
	private List<Object> christa_berghammer_globcom;
	private List<Object> john_smith_addresses;
	private EContact john_smith_econtact;
	private List<Object> john_smith_globcom;
	private List<Object> john_smith_globcom_contacts;
	private List<Object> smithFamily_addressee;
	private List<Object> smithFamily_no_john;
	private List<Object> subjectsInEurope;
	private List<Object> subjectsInUsa;
	private List<Object> areas_sf_vienna_01_munich;
	private List<Object> areas_sf_vienna_munich;
	private List<Object> areas_calif_vienna_munich_up;
	private List<Object> all_PointsOfContact = new ArrayList<Object>();

	public Population()
	{
		super();
	}

	/**
	 * create the population,
	 *
	 * @return a list of root objects of the created object graph.
	 */
	public List<Object> createPopulation()
	{
		List<Object> domainObjects = new ArrayList<Object>();

		createPlaces();
		createSmithFamily(domainObjects);
		createBerghammers(domainObjects, false);
		createMore(domainObjects);
		createCompanies(domainObjects, false);
		createAddressees(domainObjects);

		return domainObjects;
	}

	public List<Object> createBerghammerFamily()
	{
		List<Object> domainObjects = new ArrayList<Object>();

		createPlaces();
		createBerghammers(domainObjects, true);

		return domainObjects;
	}

	public List<Object> createCompanies()
	{
		List<Object> domainObjects = new ArrayList<Object>();

		if (earth == null)
			createPlaces();
		createCompanies(domainObjects, true);

		return domainObjects;
	}

	public List<Object> getBerghammers()
	{
		return berghammers;
	}

	public List<Object> getSmiths()
	{
		return smiths;
	}

	public List<Object> getAngelina_smith()
	{
		return angelina_smith;
	}

	public Person getJohn_smith()
	{
		return john_smith;
	}

	public EContact getJohn_smith_econtact()
	{
		return john_smith_econtact;
	}

	public List<Object> getJohn_jery_smith()
	{
		return john_jery_smith;
	}

	public List<Object> getCaro_angie_smith()
	{
		return caro_angie_smith;
	}

	public List<Object> getAngie_clark()
	{
		return angie_clark;
	}

	public List<Object> getAngies()
	{
		return angies;
	}

	public List<Object> getWatson_company()
	{
		return watson_company;
	}

	public List<Object> getMaier_clark()
	{
		return maier_clark;
	}

	public List<Object> getSmiths_christa_berghammer_globcom()
	{
		return smiths_christa_berghammer_globcom;
	}

	public List<Object> getBerghammers_globcom()
	{
		return berghammers_globcom;
	}

	public List<Object> getChrista_berghammer_globcom()
	{
		return christa_berghammer_globcom;
	}

	public List<Object> getJohn_smith_addresses()
	{
		return john_smith_addresses;
	}

	public List<Object> getJohn_smith_globcom()
	{
		return john_smith_globcom;
	}

	public List<Object> getJohn_smith_globcom_contacts()
	{
		return john_smith_globcom_contacts;
	}

	public List<Object> getAreas_sf_vienna_01_munich()
	{
		return areas_sf_vienna_01_munich;
	}

	public List<Object> getAreas_sf_vienna_munich()
	{
		return areas_sf_vienna_munich;
	}

	public List<Object> getAreas_calif_vienna_munich_up()
	{
		return areas_calif_vienna_munich_up;
	}

	public Address getMarketStreet_20()
	{
		return marketStreet_20;
	}

	public Address getSchwedenPlatz_32()
	{
		return schwedenPlatz_32;
	}

	public Address getStachus_1()
	{
		return stachus_1;
	}

	public Area getSanFrancisco()
	{
		return sanFrancisco;
	}

	public Area getMunich()
	{
		return munich;
	}

	public Area getEurope()
	{
		return europe;
	}

	public Area getAustria()
	{
		return austria;
	}

	public EArea getElectronicAreaUSA()
	{
		return electronicAreaUSA;
	}

	public List<Object> getSmithFamily_addressee()
	{
		return smithFamily_addressee;
	}

	public List<Object> getAll_PointsOfContact()
	{
		return all_PointsOfContact;
	}

	public List<Object> getSubjectsInEurope()
	{
		return subjectsInEurope;
	}

	public List<Object> getSubjectsInUsa()
	{
		return subjectsInUsa;
	}

	public List<Object> getSmithFamily_no_john()
	{
		return smithFamily_no_john;
	}

	private void createPlaces()
	{
		earth = new Area(null, "Earth", AreaType.PLANET);
		northAmerica = new Area(null, "North America", AreaType.CONTINENT);
		northAmerica.setPartOf(earth);
		usa = new Area("1", "USA", AreaType.COUNTRY);
		usa.setPartOf(northAmerica);
		california = new Area(null, "California", AreaType.STATE);
		california.setPartOf(usa);
		sanFrancisco = new Area(null, "San Francisco", AreaType.CITY);
		sanFrancisco.setPartOf(california);
		europe = new Area(null, "Europe", AreaType.CONTINENT);
		europe.setPartOf(earth);
		germany = new Area("2", "Germany", AreaType.COUNTRY);
		germany.setPartOf(europe);
		munich = new Area(null, "Munich", AreaType.CITY);
		munich.setPartOf(germany);
		newYork = new Area(null, "New York", AreaType.STATE);
		newYork.setPartOf(usa);
		newYorkCity = new Area(null, "New York City", AreaType.CITY);
		newYorkCity.setPartOf(newYork);
		austria = new Area(null, "Austria", AreaType.COUNTRY);
		austria.setPartOf(europe);
		vienna = new Area("1", "Vienna", AreaType.CITY);
		vienna.setPartOf(austria);
		vienna_17 = new Area("1170", "Hernals", AreaType.URBAN_DISTRICT);
		vienna_17.setPartOf(vienna);
		vienna_01 = new Area("1010", "Innere Stadt", AreaType.URBAN_DISTRICT);
		vienna_01.setPartOf(vienna);
		electronicAreaUSA = new EArea("region_1", AreaType.ELECTRONIC);
		electronicAreaSF = new EArea("region_11", AreaType.ELECTRONIC);
		electronicAreaSF.setPartOf(electronicAreaUSA);

		this.areas_sf_vienna_01_munich = new ArrayList<Object>();
		this.areas_sf_vienna_01_munich.add(this.sanFrancisco);
		this.areas_sf_vienna_01_munich.add(this.munich);
		this.areas_sf_vienna_01_munich.add(this.vienna_01);

		this.areas_sf_vienna_munich = new ArrayList<Object>();
		this.areas_sf_vienna_munich.add(this.sanFrancisco);
		this.areas_sf_vienna_munich.add(this.vienna);
		this.areas_sf_vienna_munich.add(this.munich);

		this.areas_calif_vienna_munich_up = new ArrayList<Object>();
		this.areas_calif_vienna_munich_up.add(california);
		this.areas_calif_vienna_munich_up.add(usa);
		this.areas_calif_vienna_munich_up.add(northAmerica);
		this.areas_calif_vienna_munich_up.add(earth);
		this.areas_calif_vienna_munich_up.add(vienna);
		this.areas_calif_vienna_munich_up.add(austria);
		this.areas_calif_vienna_munich_up.add(europe);
		this.areas_calif_vienna_munich_up.add(germany);
	}

	private void createSmithFamily(List<Object> domainObjects)
	{
		Address smith_address = new Address("Market Street", 20);
		this.marketStreet_20 = smith_address;
		this.all_PointsOfContact.add(smith_address);
		smith_address.setArea(sanFrancisco);
		Address smith_address_2 = new Address("Schweden Platz", 32);
		smith_address_2.setArea(vienna_01);
		this.schwedenPlatz_32 = smith_address_2;
		this.all_PointsOfContact.add(smith_address_2);
		Address smith_address_3 = new Address("Karlsplatz Stachus", 1);
		smith_address_3.setArea(munich);
		this.stachus_1 = smith_address_3;
		this.all_PointsOfContact.add(smith_address_3);
		EContact jsmith_eContact = new EContact(EContactType.EMAIL, "j.smith@email.smith");
		jsmith_eContact.setArea(electronicAreaSF);
		this.john_smith_econtact = jsmith_eContact;
		this.all_PointsOfContact.add(jsmith_eContact);

		john_smith = new Person("John", "Smith", Gender.MALE);
		john_smith.setMatchString("smith");
		john_smith.getPointsOfContact().add(smith_address);
		john_smith.getPointsOfContact().add(smith_address_2);
		john_smith.getPointsOfContact().add(smith_address_3);
		john_smith.getPointsOfContact().add(jsmith_eContact);
		Person caroline_smith = new Person("Caroline", "Smith", Gender.FEMALE);
		caroline_smith.setMatchString("smith");
		caroline_smith.getPointsOfContact().add(smith_address);
		Person angie_smith = new Person("Angelina", "Smith", Gender.FEMALE);
		angie_smith.setMatchString("smith");
		angie_smith.getPointsOfContact().add(smith_address);
		angie_smith.setMother(caroline_smith);
		angie_smith.setFather(john_smith);
		Person jery_smith = new Person("Jeremy", "Smith", Gender.MALE);
		jery_smith.setMatchString("smith");
		jery_smith.getPointsOfContact().add(smith_address);
		jery_smith.setMother(caroline_smith);
		jery_smith.setFather(john_smith);

		this.smiths = new ArrayList<Object>();
		this.smiths.add(john_smith);
		this.smiths.add(caroline_smith);
		this.smiths.add(angie_smith);
		this.smiths.add(jery_smith);

		this.angelina_smith = new ArrayList<Object>();
		this.angelina_smith.add(angie_smith);
		this.john_jery_smith = new ArrayList<Object>();
		this.john_jery_smith.add(john_smith);
		this.john_jery_smith.add(jery_smith);
		this.caro_angie_smith = new ArrayList<Object>();
		this.caro_angie_smith.add(caroline_smith);
		this.caro_angie_smith.add(angie_smith);
		this.angies = new ArrayList<Object>();
		this.angies.add(angie_smith);
		this.smiths_christa_berghammer_globcom = new ArrayList<Object>();
		this.smiths_christa_berghammer_globcom.add(john_smith);
		this.smiths_christa_berghammer_globcom.add(caroline_smith);
		this.smiths_christa_berghammer_globcom.add(angie_smith);
		this.smiths_christa_berghammer_globcom.add(jery_smith);
		this.john_smith_addresses = new ArrayList<Object>();
		this.john_smith_addresses.add(smith_address);
		this.john_smith_addresses.add(smith_address_2);
		this.john_smith_addresses.add(smith_address_3);
		this.john_smith_globcom = new ArrayList<Object>();
		this.john_smith_globcom.add(john_smith);

		this.john_smith_globcom_contacts = new ArrayList<Object>();
		this.john_smith_globcom_contacts.add(smith_address_3);
		this.john_smith_globcom_contacts.add(smith_address);
		this.john_smith_globcom_contacts.add(smith_address_2);
		this.john_smith_globcom_contacts.add(jsmith_eContact);

		this.smithFamily_addressee = new ArrayList<Object>();
		this.smithFamily_addressee.add(john_smith);
		this.smithFamily_addressee.add(caroline_smith);
		this.smithFamily_addressee.add(angie_smith);
		this.smithFamily_addressee.add(jery_smith);

		this.smithFamily_no_john = new ArrayList<Object>();
		this.smithFamily_no_john.add(caroline_smith);
		this.smithFamily_no_john.add(angie_smith);
		this.smithFamily_no_john.add(jery_smith);

		this.subjectsInEurope = new ArrayList<Object>();
		this.subjectsInEurope.add(john_smith);
		this.subjectsInUsa = new ArrayList<Object>();
		this.subjectsInUsa.add(john_smith);
		this.subjectsInUsa.add(caroline_smith);
		this.subjectsInUsa.add(angie_smith);
		this.subjectsInUsa.add(jery_smith);

		domainObjects.add(john_smith);
		domainObjects.add(caroline_smith);
		domainObjects.add(angie_smith);
		domainObjects.add(jery_smith);
	}

	private void createBerghammers(List<Object> domainObjects, boolean bhOnly)
	{
		Address berghammer_address = new Address("Hochstrasse", 4);
		berghammer_address.setArea(munich);
		this.all_PointsOfContact.add(berghammer_address);

		Person hans_berghammer = new Person("Hans", "Berghammer", Gender.MALE);
		hans_berghammer.setMatchString("berghammer");
		hans_berghammer.getPointsOfContact().add(berghammer_address);
		Person gerda_berghammer = new Person("Gerda", "Berghammer", Gender.FEMALE);
		gerda_berghammer.setMatchString("berghammer");
		gerda_berghammer.getPointsOfContact().add(berghammer_address);
		Person christa_berghammer = new Person("Christa", "Berghammer", Gender.FEMALE);
		christa_berghammer.setMatchString("berghammer");
		christa_berghammer.getPointsOfContact().add(berghammer_address);
		christa_berghammer.setMother(gerda_berghammer);
		christa_berghammer.setFather(hans_berghammer);

		this.berghammers = new ArrayList<Object>();
		this.berghammers.add(hans_berghammer);
		this.berghammers.add(gerda_berghammer);
		this.berghammers.add(christa_berghammer);
		if (!bhOnly)
		{
			this.smiths_christa_berghammer_globcom.add(christa_berghammer);
			this.berghammers_globcom = new ArrayList<Object>();
			this.berghammers_globcom.add(hans_berghammer);
			this.berghammers_globcom.add(gerda_berghammer);
			this.berghammers_globcom.add(christa_berghammer);
			this.christa_berghammer_globcom = new ArrayList<Object>();
			this.christa_berghammer_globcom.add(christa_berghammer);

			this.subjectsInEurope.add(hans_berghammer);
			this.subjectsInEurope.add(gerda_berghammer);
			this.subjectsInEurope.add(christa_berghammer);
		}

		domainObjects.add(hans_berghammer);
		domainObjects.add(gerda_berghammer);
		domainObjects.add(christa_berghammer);
	}

	private void createMore(List<Object> domainObjects)
	{
		Address watson_address = new Address("Broadway", 53);
		watson_address.setArea(newYorkCity);
		this.all_PointsOfContact.add(watson_address);
		Person jim_watson = new Person("Jim", "Watson", Gender.MALE);
		jim_watson.setMatchString("match_2");
		jim_watson.getPointsOfContact().add(watson_address);

		Address clark_address = new Address("Pearl Street", 124);
		clark_address.setArea(newYorkCity);
		this.all_PointsOfContact.add(clark_address);
		Person angie_clark = new Person("Angelina", "Clark", Gender.FEMALE);
		angie_clark.setMatchString("match_1");
		angie_clark.getPointsOfContact().add(clark_address);

		Address maier_address = new Address("Lackner Gasse", 12);
		maier_address.setArea(vienna_17);
		this.all_PointsOfContact.add(maier_address);
		Person herbert_maier = new Person("Herbert", "Maier", Gender.MALE);
		herbert_maier.setMatchString("match_1");
		herbert_maier.getPointsOfContact().add(maier_address);
		Person sarah_maier = new Person("Sarah", "Maier", Gender.FEMALE);
		sarah_maier.setMatchString("match_2");
		sarah_maier.getPointsOfContact().add(maier_address);

		this.angie_clark = new ArrayList<Object>();
		this.angie_clark.add(angie_clark);
		this.angies.add(angie_clark);
		this.watson_company = new ArrayList<Object>();
		this.watson_company.add(jim_watson);
		this.maier_clark = new ArrayList<Object>();
		this.maier_clark.add(sarah_maier);
		this.maier_clark.add(herbert_maier);
		this.maier_clark.add(angie_clark);

		this.subjectsInEurope.add(herbert_maier);
		this.subjectsInEurope.add(sarah_maier);

		this.subjectsInUsa.add(jim_watson);
		this.subjectsInUsa.add(angie_clark);

		domainObjects.add(jim_watson);
		domainObjects.add(angie_clark);
		domainObjects.add(herbert_maier);
		domainObjects.add(sarah_maier);
	}

	private void createCompanies(List<Object> domainObjects, boolean compsOnly)
	{
		Address globCom_address = new Address("Kearny Street", 29);
		globCom_address.setArea(sanFrancisco);
		EContact globCom_phone = new EContact(EContactType.TELEPHONE, "12345");
		if (!compsOnly)
		{
			this.all_PointsOfContact.add(globCom_address);
			this.all_PointsOfContact.add(globCom_phone);
		}

		Company globCom = new Company();
		globCom.setMatchString("match_1");
		globCom.setName("Global Company");
		globCom.getPointsOfContact().add(globCom_address);
		globCom.getPointsOfContact().add(globCom_phone);

		Address smallCom_address = new Address("Schiller Strasse", 15);
		smallCom_address.setArea(munich);
		if (!compsOnly)
			this.all_PointsOfContact.add(smallCom_address);

		Company smallCom = new Company();
		smallCom.setMatchString("match_5");
		smallCom.setName("Small Company");
		smallCom.getPointsOfContact().add(smallCom_address);

		if (!compsOnly)
		{
			this.watson_company.add(globCom);
			this.smiths_christa_berghammer_globcom.add(globCom);
			this.berghammers_globcom.add(globCom);
			this.christa_berghammer_globcom.add(globCom);

			this.john_smith_globcom.add(0, globCom);
			this.john_smith_globcom_contacts.add(1, globCom_address);
			this.john_smith_globcom_contacts.add(globCom_phone);

			this.subjectsInEurope.add(smallCom);
			this.subjectsInUsa.add(globCom);
		}

		domainObjects.add(globCom);
		domainObjects.add(smallCom);
	}

	private void createAddressees(List<Object> domainObjects)
	{
		Addressee add = new Addressee();
		add.setName("addressee_01");
		add.setPointsOfContact(this.marketStreet_20);

		this.smithFamily_addressee.add(0, add);

		domainObjects.add(add);
	}
}
