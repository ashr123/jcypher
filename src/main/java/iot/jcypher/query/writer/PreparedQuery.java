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

package iot.jcypher.query.writer;

import java.util.List;

public class PreparedQuery
{

	private String cypher;
	private String json;
	private PQContext context;
	private boolean dSLParams;

	PreparedQuery()
	{
	}

	public String getCypher()
	{
		return cypher;
	}

	void setCypher(String cypher)
	{
		this.cypher = cypher;
	}

	public String getJson()
	{
		return json;
	}

	void setJson(String json)
	{
		this.json = json;
	}

	PQContext getContext()
	{
		if (this.context == null)
			this.context = new PQContext();
		return this.context;
	}

	boolean hasdSLParams()
	{
		return dSLParams;
	}

	void setdSLParams()
	{
		this.dSLParams = true;
	}

	/********************************/
	public class PQContext
	{

		Format cypherFormat;
		boolean extractParams;
		boolean useTransationalEndpoint;
		List<String> resultDataContents;
		List<IQueryParam> queryParams;

		private PQContext()
		{
			super();
		}

		void fillContext(WriterContext ctxt)
		{
			ctxt.cypherFormat = this.cypherFormat;
			ctxt.extractParams = this.extractParams;
			ctxt.useTransactionalEndpoint = this.useTransationalEndpoint;
			ctxt.queryParams = this.queryParams;
			ContextAccess.setResultDataContents(ctxt, this.resultDataContents);
		}
	}
}
