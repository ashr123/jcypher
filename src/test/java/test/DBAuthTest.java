package test;

import iot.jcypher.database.DBAccessFactory;
import iot.jcypher.database.DBProperties;
import iot.jcypher.database.DBType;
import iot.jcypher.database.IDBAccess;
import iot.jcypher.database.remote.RemoteDBAccess;
import org.junit.Test;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class DBAuthTest
{

	@Test
	public void testRemoteBasicAuth()
	{
		Properties props = new Properties();
		props.setProperty(DBProperties.SERVER_ROOT_URI, "http://localhost:7474"); // not Bolt
		props.setProperty(DBProperties.DATABASE_DIR, "C:/NEO4J_DBS/01");
		DBType dbType = DBType.REMOTE;

		AuthToken auth = AuthTokens.basic("user", "password");

		IDBAccess dba = DBAccessFactory.createDBAccess(dbType, props, auth);
		String bauth = ((RemoteDBAccess) dba).getAuth();
		assertEquals("Basic dXNlcjpwYXNzd29yZA==", bauth);
		return;
	}

}
