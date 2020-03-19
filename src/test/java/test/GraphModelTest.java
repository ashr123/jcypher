package test;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.graph.*;
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.result.JcError;
import iot.jcypher.query.values.JcNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//@Ignore
public class GraphModelTest extends AbstractTestSuite
{

	private static IDBAccess dbAccess;

	@BeforeClass
	public static void before()
	{
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
	public void testGraphModel_01()
	{
		createDB_01();
		queryDB_01();
	}

	private void createDB_01()
	{
		Graph graph = Graph.create(dbAccess);

		GrNode matrix1 = graph.createNode();
		matrix1.addLabel("Movie");
		matrix1.addProperty("title", "The Matrix");
		matrix1.addProperty("year", "1999-03-31");

		GrNode matrix2 = graph.createNode();
		matrix2.addLabel("Movie");
		matrix2.addProperty("title", "The Matrix Reloaded");
		matrix2.addProperty("year", "2003-05-07");

		GrNode matrix3 = graph.createNode();
		matrix3.addLabel("Movie");
		matrix3.addProperty("title", "The Matrix Revolutions");
		matrix3.addProperty("year", "2003-10-27");

		GrNode keanu = graph.createNode();
		keanu.addLabel("Actor");
		keanu.addProperty("name", "Keanu Reeves");
		keanu.addProperty("rating", 5);

		GrNode laurence = graph.createNode();
		laurence.addLabel("Actor");
		laurence.addProperty("name", "Laurence Fishburne");
		laurence.addProperty("rating", 6);

		GrNode carrieanne = graph.createNode();
		carrieanne.addLabel("Actor");
		carrieanne.addProperty("name", "Carrie-Anne Moss");
		carrieanne.addProperty("rating", 7);

		GrRelation rel = graph.createRelation("ACTS_IN", keanu, matrix1);
		rel.addProperty("role", "Neo");
		rel = graph.createRelation("ACTS_IN", keanu, matrix2);
		rel.addProperty("role", "Neo");
		rel = graph.createRelation("ACTS_IN", keanu, matrix3);
		rel.addProperty("role", "Neo");

		rel = graph.createRelation("ACTS_IN", laurence, matrix1);
		rel.addProperty("role", "Morpheus");
		rel = graph.createRelation("ACTS_IN", laurence, matrix2);
		rel.addProperty("role", "Morpheus");
		rel = graph.createRelation("ACTS_IN", laurence, matrix3);
		rel.addProperty("role", "Morpheus");

		rel = graph.createRelation("ACTS_IN", carrieanne, matrix1);
		rel.addProperty("role", "Trinity");
		rel = graph.createRelation("ACTS_IN", carrieanne, matrix2);
		rel.addProperty("role", "Trinity");
		rel = graph.createRelation("ACTS_IN", carrieanne, matrix3);
		rel.addProperty("role", "Trinity");

		List<JcError> errors = graph.store();
		assertTrue(errors.isEmpty());
	}

	private void queryDB_01()
	{
		setDoPrint(true);
		setDoAssert(true);

		JcQueryResult result;

		JcNode movie = new JcNode("movie");
		JcNode actor = new JcNode("actor");

		/*******************************/
		JcQuery query = new JcQuery();
		query.setClauses(new IClause[]{
				MATCH.node(actor).label("Actor").relation().out().type("ACTS_IN").node(movie),
				RETURN.value(actor),
				RETURN.value(movie)
		});
		result = dbAccess.execute(query);
		if (result.hasErrors())
			printErrors(result);
		assertFalse(result.hasErrors());

		List<GrNode> actors = result.resultOf(actor);
		List<GrNode> movies = result.resultOf(movie);

		assertTrue(containsProperty("name", "Keanu Reeves", actors));
		assertTrue(containsProperty("name", "Laurence Fishburne", actors));
		assertTrue(containsProperty("name", "Carrie-Anne Moss", actors));

		assertTrue(containsProperty("title", "The Matrix", movies));
		assertTrue(containsProperty("title", "The Matrix Reloaded", movies));
		assertTrue(containsProperty("title", "The Matrix Revolutions", movies));
	}

	private boolean containsProperty(String propertyName, Object propertyValue, List<GrNode> pcs)
	{
		for (GrPropertyContainer pc : pcs)
		{
			for (GrProperty prop : pc.getProperties())
			{
				if (prop.getName().equals(propertyName) &&
						prop.getValue().equals(propertyValue))
					return true;
			}
		}
		return false;
	}
}
