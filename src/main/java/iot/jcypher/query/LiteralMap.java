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

package iot.jcypher.query;

import iot.jcypher.query.values.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains name (key) - value pairs; values are literal values.
 */
public class LiteralMap
{

	private Map<String, Object> map;

	LiteralMap()
	{
		super();
		this.map = new HashMap<String, Object>();
	}

	/**
	 * Answer the value found under the given number (JcNumber) key.
	 *
	 * @param number
	 * @return
	 */
	public BigDecimal get(JcNumber number)
	{
		Object val = this.getFor(number);
		if (val != null)
			return (BigDecimal) val;
		return null;
	}

	/**
	 * Answer the value found under the given string (JcString) key.
	 *
	 * @param string
	 * @return
	 */
	public String get(JcString string)
	{
		Object val = this.getFor(string);
		if (val != null)
			return (String) val;
		return null;
	}

	/**
	 * Answer the value found under the given boolean (JcBoolean) key.
	 *
	 * @param bool
	 * @return
	 */
	public Boolean get(JcBoolean bool)
	{
		Object val = this.getFor(bool);
		if (val != null)
			return (Boolean) val;
		return null;
	}

	/**
	 * Answer the value found under the given key.
	 *
	 * @param key
	 * @return
	 */
	public Object get(String key)
	{
		return this.map.get(key);
	}

	void put(JcPrimitive key, Object value)
	{
		this.map.put(ValueAccess.getName(key), value);
	}

	private Object getFor(JcPrimitive key)
	{
		return this.get(ValueAccess.getName(key));
	}

	@Override
	public String toString()
	{
		return this.map.toString();
	}

}
