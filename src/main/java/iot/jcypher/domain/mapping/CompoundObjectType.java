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

package iot.jcypher.domain.mapping;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompoundObjectType
{

	public static final String SEPARATOR = ", ";

	private Class<?> type;
	private CType cType;
	private CompoundObjectType next;

	public CompoundObjectType(Class<?> type)
	{
		super();
		this.type = type;
		this.cType = MappingUtil.mapsToProperty(type) ? CType.SIMPLE : CType.COMPLEX;
	}

	public static boolean isConcrete(Class<?> typ)
	{
		return (!typ.isInterface() && !Modifier.isAbstract(typ.getModifiers()) &&
				!typ.equals(Object.class));
	}

	/**
	 * @param typ
	 * @return true if the type was added to the compound,
	 * false if the type was already contained in the compound
	 */
	public boolean addType(Class<?> typ)
	{
		CompoundObjectType cur = null;
		Iterator<CompoundObjectType> it = typeIterator();
		while (it.hasNext())
		{
			cur = it.next();
			if (cur.type.equals(typ)) // was already added
				return false;
		}
		CompoundObjectType nextOne = new CompoundObjectType(typ);
		cur.next = nextOne;
		CType t = nextOne.cType;
		it = typeIterator();
		while (it.hasNext())
		{
			cur = it.next();
			if (cur.cType != t)
				cur.cType = CType.MIXED;
		}
		return true;
	}

	public Class<?> getType()
	{
		return this.type;
	}

	/**
	 * Answer a list of types of this CompoundObjectType.
	 *
	 * @param noAbstractTypes if true, no abstract types and no interfaces are answered,
	 *                        else all types of the CompoundObjectType are answered.
	 * @return a list of java Classes
	 */
	public List<Class<?>> getTypes(boolean noAbstractTypes)
	{
		List<Class<?>> typeList = new ArrayList<Class<?>>();
		this.addType(typeList, noAbstractTypes);
		return typeList;
	}

	public CType getCType()
	{
		return this.cType;
	}

	public Iterator<CompoundObjectType> typeIterator()
	{
		return new TypeIterator();
	}

	public String getTypeListString()
	{
		StringBuilder sb = new StringBuilder();
		Iterator<CompoundObjectType> it = typeIterator();
		int idx = 0;
		while (it.hasNext())
		{
			if (idx > 0)
				sb.append(SEPARATOR);
			sb.append(it.next().type.getName());
			idx++;
		}
		return sb.toString();
	}

	private void addType(List<Class<?>> typeList, boolean noAbstractTypes)
	{
		if (!noAbstractTypes || isConcrete(this.type))
			typeList.add(this.type);
		if (this.next != null)
			next.addType(typeList, noAbstractTypes);
	}

	/********************************************/
	public static enum CType
	{
		SIMPLE, COMPLEX, MIXED
	}

	/********************************************/
	public class TypeIterator implements Iterator<CompoundObjectType>
	{

		CompoundObjectType current;

		TypeIterator()
		{
			super();
			this.current = CompoundObjectType.this;
		}

		@Override
		public boolean hasNext()
		{
			return current != null;
		}

		@Override
		public CompoundObjectType next()
		{
			CompoundObjectType ctype = this.current;
			if (ctype != null)
				this.current = ctype.next;

			return ctype;
		}

		@Override
		public void remove()
		{
			throw new RuntimeException("operation not supported");
		}
	}
}
