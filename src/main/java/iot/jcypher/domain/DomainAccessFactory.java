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

package iot.jcypher.domain;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.domain.IDomainAccess.DomainLabelUse;

/**
 * A Factory for creating domain accessors.
 */
public class DomainAccessFactory
{

	/**
	 * Create a domain accessor.
	 *
	 * @param dbAccess   the graph database connection
	 * @param domainName
	 * @return
	 */
	public static IDomainAccess createDomainAccess(IDBAccess dbAccess, String domainName)
	{
		return IDomainAccessFactory.INSTANCE.createDomainAccess(dbAccess, domainName);
	}

	/**
	 * Create a domain accessor.
	 *
	 * @param dbAccess       the graph database connection
	 * @param domainName
	 * @param domainLabelUse --<b>Note:</b> Consistency may be corrupted, if you change domainLabelUse
	 *                       on different creations of DomainAccess to the same domain.
	 * @return
	 */
	public static IDomainAccess createDomainAccess(IDBAccess dbAccess, String domainName,
	                                               DomainLabelUse domainLabelUse)
	{
		return IDomainAccessFactory.INSTANCE.createDomainAccess(dbAccess, domainName, domainLabelUse);
	}

	/**
	 * Create a domain accessor which works with a generic domain model.
	 *
	 * @param dbAccess   the graph database connection
	 * @param domainName
	 * @return
	 */
	public static IGenericDomainAccess createGenericDomainAccess(IDBAccess dbAccess, String domainName)
	{
		return IDomainAccessFactory.INSTANCE.createGenericDomainAccess(dbAccess, domainName);
	}

	/**
	 * Create a domain accessor which works with a generic domain model.
	 *
	 * @param dbAccess       the graph database connection
	 * @param domainName
	 * @param domainLabelUse --<b>Note:</b> Consistency may be corrupted, if you change domainLabelUse
	 *                       on different creations of DomainAccess to the same domain.
	 * @return
	 */
	public static IGenericDomainAccess createGenericDomainAccess(IDBAccess dbAccess, String domainName,
	                                                             DomainLabelUse domainLabelUse)
	{
		return IDomainAccessFactory.INSTANCE.createGenericDomainAccess(dbAccess, domainName, domainLabelUse);
	}
}
