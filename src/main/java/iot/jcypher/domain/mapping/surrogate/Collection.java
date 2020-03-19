/************************************************************************
 * Copyright (c) 2014-2016 IoT-Solutions e.U.
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

package iot.jcypher.domain.mapping.surrogate;

public class Collection extends AbstractSurrogate
{

	private java.util.Collection<Object> c_content;
	private String collType;

	public Collection()
	{
		super();
	}

	public Collection(java.util.Collection<Object> content)
	{
		super();
		this.c_content = content;
	}

	@Override
	public java.util.Collection<Object> getContent()
	{
		return c_content;
	}

	public void setContent(java.util.Collection<Object> content)
	{
		this.c_content = content;
	}

	@Override
	public Object objectToUpdate()
	{
		return getContent();
	}

	public String getCollType()
	{
		return collType;
	}

	public void setCollType(String collType)
	{
		this.collType = collType;
	}
}
