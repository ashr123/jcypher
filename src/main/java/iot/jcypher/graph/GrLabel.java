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

package iot.jcypher.graph;


public class GrLabel extends PersistableItem
{

	private String name;

	GrLabel(String name)
	{
		super();
		this.name = name;
		this.syncState = SyncState.NEW;
	}

	public String getName()
	{
		return name;
	}

	@Override
	void setToSynchronized()
	{
		setSyncState(SyncState.SYNC);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Label: ");
		sb.append(this.name);
		return sb.toString();
	}
}
