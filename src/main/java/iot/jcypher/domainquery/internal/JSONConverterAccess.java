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

package iot.jcypher.domainquery.internal;

import iot.jcypher.domainquery.internal.RecordedQuery.Statement;

import javax.json.JsonValue;
import java.util.List;

public class JSONConverterAccess
{

	public static void readStatement(JSONConverter converter, JsonValue s, List<Statement> statements, RecordedQuery rq)
	{
		converter.readStatement(s, statements, rq);
	}
}
