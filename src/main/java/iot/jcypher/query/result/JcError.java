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

package iot.jcypher.query.result;

public class JcError
{

	private String codeOrType;
	private String message;
	private String additionalInfo;

	public JcError(String codeOrType, String message, String info)
	{
		super();
		this.codeOrType = codeOrType;
		this.message = message;
		this.additionalInfo = info;
	}

	public String getCodeOrType()
	{
		return codeOrType;
	}

	public String getMessage()
	{
		return message;
	}

	/**
	 * may be null
	 *
	 * @return
	 */
	public String getAdditionalInfo()
	{
		return additionalInfo;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("CodeOrType: ");
		sb.append(this.codeOrType != null ? this.codeOrType : "");
		sb.append(", Message: ");
		sb.append(this.message != null ? this.message : "");
		if (this.additionalInfo != null)
		{
			sb.append("\nAdditionalInfo: ");
			sb.append(this.additionalInfo);
		}
		return sb.toString();
	}

}
