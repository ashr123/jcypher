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

package test.dsl_queryparams;

import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryParameter;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.JC;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.MERGE;
import iot.jcypher.query.factories.clause.ON_CREATE;
import iot.jcypher.query.factories.clause.ON_MATCH;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcRelation;
import iot.jcypher.query.writer.Format;
import iot.jcypher.query.writer.PreparedQueries;
import iot.jcypher.query.writer.PreparedQuery;
import iot.jcypher.util.Util;
import org.junit.Test;
import test.AbstractTestSuite;
import util.TestDataReader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//@Ignore
public class QueryWithParamsTest extends AbstractTestSuite
{

	@Test
	public void testQueryParams_01()
	{
		String cypher;
		String json;
		setDoPrint(true);
		setDoAssert(true);

		TestDataReader tdr = new TestDataReader("/test/dsl_queryparams/Test_DSL_QUERYPARAMS_01.txt");

		/*******************************/

		JcQueryParameter memberName = new JcQueryParameter("memberName");
		JcQueryParameter songName = new JcQueryParameter("songName");

		JcNode n = new JcNode("n");
		JcNode s = new JcNode("s");
		JcRelation r = new JcRelation("r");
		IClause[] clauses = new IClause[]{
				// match nodes (in this sample for 'John' and 'Song_1')
				MATCH.node(n).label("Member").property("name").value(memberName),
				MATCH.node(s).label("Song").property("name").value(songName),
				// match (or create if not exists) a relation
				MERGE.node(n).relation(r).out().type("PLAYED").node(s),

				// initialize the 'views' property to 1
				ON_CREATE.SET(r.property("views")).to(1),
				ON_CREATE.SET(r.property("created")).byExpression(JC.timeStamp()),

				// increment the 'views' property
				ON_MATCH.SET(r.property("views")).byExpression(
						r.numberProperty("views").plus(1)),
		};

		JcQuery query = new JcQuery();
		query.setClauses(clauses);

		PreparedQuery pq = Util.toPreparedQuery(query, Format.PRETTY_1);
		//System.out.println(pq.getJson());
		assertEquals(pq.getJson(), tdr.getTestData("QPARAMS_01"));

		/*******************************/

		memberName.setValue("John");
		songName.setValue("Song_1");
		json = Util.toJSON(pq);
		//System.out.println(json);
		assertEquals(json, tdr.getTestData("QPARAMS_02"));

		/*******************************/

		JcQueryParameter memberName_1 = new JcQueryParameter("memberName_1");
		JcQueryParameter songName_1 = new JcQueryParameter("songName_1");
		IClause[] clauses_1 = new IClause[]{
				// match nodes (in this sample for 'John' and 'Song_1')
				MATCH.node(n).label("Member").property("name").value(memberName_1),
				MATCH.node(s).label("Song").property("name").value(songName_1),
				// match (or create if not exists) a relation
				MERGE.node(n).relation(r).out().type("PLAYED").node(s),

				// initialize the 'views' property to 1
				ON_CREATE.SET(r.property("views")).to(3),
				ON_CREATE.SET(r.property("created")).byExpression(JC.timeStamp()),

				// increment the 'views' property
				ON_MATCH.SET(r.property("views")).byExpression(
						r.numberProperty("views").plus(3)),
		};
		JcQuery query_1 = new JcQuery();
		query_1.setClauses(clauses_1);

		List<JcQuery> queries = new ArrayList<JcQuery>();
		queries.add(query);
		queries.add(query_1);

		PreparedQueries pqs = Util.toPreparedQueries(queries, Format.PRETTY_1);
		//System.out.println(pqs.getJson());
		assertEquals(pqs.getJson(), tdr.getTestData("QPARAMS_03"));

		/*******************************/

		memberName_1.setValue("Angie");
		songName_1.setValue("My Song");
		json = Util.toJSON(pqs);
		//System.out.println(json);
		assertEquals(json, tdr.getTestData("QPARAMS_04"));

		/*******************************/

		memberName.setValue("Johnny");
		songName.setValue("Song 2");
		memberName_1.setValue("Angelina");
		songName_1.setValue("My Other Song");
		json = Util.toJSON(pqs);
		//System.out.println(json);
		assertEquals(json, tdr.getTestData("QPARAMS_05"));

		/*******************************/

		IClause[] clauses_2 = new IClause[]{
				// match nodes (in this sample for 'John' and 'Song_1')
				MATCH.node(n).label("Member").property("name").value("Herbert"),
				MATCH.node(s).label("Song").property("name").value("Herberts Song"),
				// match (or create if not exists) a relation
				MERGE.node(n).relation(r).out().type("PLAYED").node(s),

				// initialize the 'views' property to 1
				ON_CREATE.SET(r.property("views")).to(5),
				ON_CREATE.SET(r.property("created")).byExpression(JC.timeStamp()),

				// increment the 'views' property
				ON_MATCH.SET(r.property("views")).byExpression(
						r.numberProperty("views").plus(3)),
		};
		JcQuery query_2 = new JcQuery();
		query_2.setClauses(clauses_2);

		PreparedQuery pq_2 = Util.toPreparedQuery(query_2, Format.PRETTY_1);
		//System.out.println(pq_2.getJson());
		assertEquals(pq_2.getJson(), tdr.getTestData("QPARAMS_06"));

		json = Util.toJSON(pq_2);
		assertTrue(pq_2.getJson() == json);


		return;
	}
}
