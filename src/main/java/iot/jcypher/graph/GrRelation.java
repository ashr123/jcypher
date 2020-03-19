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

import iot.jcypher.graph.internal.GrId;
import iot.jcypher.query.result.util.ResultHandler;

public class GrRelation extends GrPropertyContainer
{

	private GrId startNodeId;
	private GrId endNodeId;
	private String type;

	GrRelation(ResultHandler resultHandler, GrId id,
	           GrId startNodeId, GrId endNodeId, int rowIdx)
	{
		super(resultHandler, id, rowIdx);
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
	}

	GrRelation(ResultHandler resultHandler, GrId id,
	           GrId startNodeId, GrId endNodeId, String typ)
	{
		super(resultHandler, id, -1);
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
		this.type = typ;
	}

	public GrNode getStartNode()
	{
		return this.resultHandler.getNode(this.startNodeId, this.rowIndex);
	}

	public GrNode getEndNode()
	{
		return this.resultHandler.getNode(this.endNodeId, this.rowIndex);
	}

	/**
	 * Note: Relations must have exactly one type
	 *
	 * @return the type of the Relation
	 */
	public String getType()
	{
		if (this.type == null)
			this.type = this.resultHandler.getRelationType(getId(), this.rowIndex);
		return this.type;
	}

	@Override
	void setToSynchronized()
	{
		super.setToSynchronized();
		setSyncState(SyncState.SYNC);
	}

}
