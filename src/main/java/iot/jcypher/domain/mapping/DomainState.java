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

package iot.jcypher.domain.mapping;

import iot.jcypher.domain.ResolutionDepth;
import iot.jcypher.domain.genericmodel.DomainObject;
import iot.jcypher.domain.mapping.surrogate.InnerClassSurrogate;
import iot.jcypher.domain.mapping.surrogate.SurrogateState;

import java.util.*;
import java.util.Map.Entry;

public class DomainState
{

	private Map<Object, LoadInfo> object2IdMap;
	private Map<IRelation, RelationLoadInfo> relation2IdMap;

	// in a domain there can only exist unambiguous mappings between objects and nodes
	private Map<Long, Object> id2ObjectMap;
	private Map<Object, List<IRelation>> object2RelationsMap;
	private Map<SourceField2TargetKey, List<KeyedRelation>> objectField2KeyedRelationsMap;
	private Map<SourceFieldKey, List<KeyedRelation>> multiRelationsMap;
	private SurrogateState surrogateState;

	public DomainState()
	{
		super();
		this.object2IdMap = new HashMap<Object, LoadInfo>();
		this.relation2IdMap = new HashMap<IRelation, RelationLoadInfo>();
		this.id2ObjectMap = new HashMap<Long, Object>();
		this.object2RelationsMap = new HashMap<Object, List<IRelation>>();
		this.objectField2KeyedRelationsMap = new HashMap<SourceField2TargetKey, List<KeyedRelation>>();
		this.multiRelationsMap = new HashMap<SourceFieldKey, List<KeyedRelation>>();
		this.surrogateState = new SurrogateState();
	}

	public DomainState createCopy()
	{
		DomainState ret = new DomainState();
		Iterator<Entry<Object, LoadInfo>> it_1 = this.object2IdMap.entrySet().iterator();
		while (it_1.hasNext())
		{
			Entry<Object, LoadInfo> entry = it_1.next();
			ret.object2IdMap.put(entry.getKey(), entry.getValue().createCopy());
		}

		Map<IRelation, IRelation> copiedRels = new IdentityHashMap<IRelation, IRelation>();

		Iterator<Entry<IRelation, RelationLoadInfo>> it_2 = this.relation2IdMap.entrySet().iterator();
		while (it_2.hasNext())
		{
			Entry<IRelation, RelationLoadInfo> entry = it_2.next();
			ret.relation2IdMap.put(copyRelation(entry.getKey(), copiedRels), entry.getValue().createCopy());
		}
		Iterator<Entry<Long, Object>> it_3 = this.id2ObjectMap.entrySet().iterator();
		while (it_3.hasNext())
		{
			Entry<Long, Object> entry = it_3.next();
			ret.id2ObjectMap.put(entry.getKey(), entry.getValue());
		}
		Iterator<Entry<Object, List<IRelation>>> it_4 = this.object2RelationsMap.entrySet().iterator();
		while (it_4.hasNext())
		{
			Entry<Object, List<IRelation>> entry = it_4.next();
			ret.object2RelationsMap.put(entry.getKey(), copyRelationsList(entry.getValue(), copiedRels));
		}

		Map<SourceField2TargetKey, SourceField2TargetKey> copiedSftks =
				new IdentityHashMap<SourceField2TargetKey, SourceField2TargetKey>();
		Iterator<Entry<SourceField2TargetKey, List<KeyedRelation>>> it_5 = this.objectField2KeyedRelationsMap.entrySet().iterator();
		while (it_5.hasNext())
		{
			Entry<SourceField2TargetKey, List<KeyedRelation>> entry = it_5.next();
			ret.objectField2KeyedRelationsMap.put(
					copySftks(entry.getKey(), copiedSftks),
					copyRelationsList(entry.getValue(), copiedRels));
		}

		Map<SourceFieldKey, SourceFieldKey> copiedSfks =
				new IdentityHashMap<SourceFieldKey, SourceFieldKey>();
		Iterator<Entry<SourceFieldKey, List<KeyedRelation>>> it_6 = this.multiRelationsMap.entrySet().iterator();
		while (it_6.hasNext())
		{
			Entry<SourceFieldKey, List<KeyedRelation>> entry = it_6.next();
			ret.multiRelationsMap.put(copySfks(entry.getKey(), copiedSfks),
					copyRelationsList(entry.getValue(), copiedRels));
		}

		ret.surrogateState = this.surrogateState.createCopy(copiedRels, ret);

		return ret;
	}

	private SourceFieldKey copySfks(SourceFieldKey toCopy,
	                                Map<SourceFieldKey, SourceFieldKey> copiedSfks)
	{
		SourceFieldKey ret = copiedSfks.get(toCopy);
		if (ret == null)
		{
			ret = new SourceFieldKey(toCopy.getSource(), toCopy.getFieldName());
			copiedSfks.put(toCopy, ret);
		}
		return ret;
	}

	private SourceField2TargetKey copySftks(SourceField2TargetKey toCopy,
	                                        Map<SourceField2TargetKey, SourceField2TargetKey> copiedSftks)
	{
		SourceField2TargetKey ret = copiedSftks.get(toCopy);
		if (ret == null)
		{
			SourceFieldKey sfk = toCopy.getSourceFieldKey();
			ret = new SourceField2TargetKey(sfk.getSource(), sfk.getFieldName(), toCopy.getTarget());
			copiedSftks.put(toCopy, ret);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	private <T extends IRelation> T copyRelation(T toCopy, Map<IRelation, IRelation> copiedRels)
	{
		T crel = (T) copiedRels.get(toCopy);
		if (crel == null)
		{
			crel = (T) toCopy.createCopy(this);
			copiedRels.put(toCopy, crel);
		}
		return crel;
	}

	private <T extends IRelation> List<T> copyRelationsList(List<T> toCopy,
	                                                        Map<IRelation, IRelation> copiedRels)
	{
		List<T> ret = null;
		if (toCopy != null)
		{
			ret = new ArrayList<T>();
			for (T rel : toCopy)
			{
				ret.add(copyRelation(rel, copiedRels));
			}
		}
		return ret;
	}

	private void addTo_Object2IdMap(Object key, Long value, int version, ResolutionDepth resolutionDepth)
	{
		LoadInfo loadInfo = new LoadInfo();
		loadInfo.id = value;
		loadInfo.version = version;
		loadInfo.resolutionDepth = resolutionDepth;
		this.object2IdMap.put(key, loadInfo);
	}

	public ResolutionDepth getResolutionDepth(Object key)
	{
		LoadInfo loadInfo = this.object2IdMap.get(key);
		if (loadInfo != null)
			return loadInfo.resolutionDepth;
		return null;
	}

	public Long getIdFrom_Object2IdMap(Object key)
	{
		LoadInfo loadInfo = this.object2IdMap.get(key);
		if (loadInfo != null)
			return loadInfo.id;
		return null;
	}

	public LoadInfo getLoadInfoFrom_Object2IdMap(Object key)
	{
		return this.object2IdMap.get(key);
	}

	public void add_Id2Relation(IRelation relat, Long id, int version)
	{
		RelationLoadInfo rli = new RelationLoadInfo();
		rli.id = id;
		rli.version = version;
		add_Info2Relation(relat, rli);
	}

	public void add_Info2Relation(IRelation relat, RelationLoadInfo rli)
	{
		if (!relat.isDeferred())
		{
			IRelation toPut = relat;
			if (relat instanceof KeyedRelationToChange)
			{
				KeyedRelation oldOne = ((KeyedRelationToChange) relat).existingOne;
				this.relation2IdMap.remove(oldOne);
				SourceField2TargetKey key = new SourceField2TargetKey(oldOne.getStart(),
						oldOne.getType(), oldOne.getEnd());
				List<KeyedRelation> rels = this.objectField2KeyedRelationsMap.get(key);
				if (rels != null)
				{
					rels.remove(oldOne);
				}
				SourceFieldKey fieldKey = key.getSourceFieldKey();
				rels = this.multiRelationsMap.get(fieldKey);
				if (rels != null)
				{
					rels.remove(oldOne);
				}
				KeyedRelation newOne = ((KeyedRelationToChange) relat).getNewOne();
				toPut = newOne;
			}
			this.relation2IdMap.put(toPut, rli);

			if (toPut instanceof KeyedRelation)
			{
				SourceField2TargetKey key = new SourceField2TargetKey(toPut.getStart(),
						toPut.getType(), toPut.getEnd());
				List<KeyedRelation> rels = this.objectField2KeyedRelationsMap.get(key);
				if (rels == null)
				{
					rels = new ArrayList<KeyedRelation>();
					this.objectField2KeyedRelationsMap.put(key, rels);
				}
				if (!rels.contains(toPut))
					rels.add((KeyedRelation) toPut);
				SourceFieldKey fieldKey = key.getSourceFieldKey();
				rels = this.multiRelationsMap.get(fieldKey);
				if (rels == null)
				{
					rels = new ArrayList<KeyedRelation>();
					this.multiRelationsMap.put(fieldKey, rels);
				}
				if (!rels.contains(toPut))
					rels.add((KeyedRelation) toPut);
			} else
			{
				List<IRelation> rels = this.object2RelationsMap.get(toPut.getStart());
				if (rels == null)
				{
					rels = new ArrayList<IRelation>();
					this.object2RelationsMap.put(toPut.getStart(), rels);
				}
				if (!rels.contains(toPut))
					rels.add(toPut);
			}
		} else
		{
			relat.setDomainState(this);
			relat.setId(rli.id.longValue());
		}
	}

	public RelationLoadInfo getFrom_Relation2IdMap(IRelation relat)
	{
		IRelation key;
		if (relat instanceof KeyedRelationToChange)
			key = ((KeyedRelationToChange) relat).existingOne;
		else
			key = relat;
		return this.relation2IdMap.get(key);
	}

	public void addTo_Id2ObjectMap(Object obj, Long id)
	{
		this.id2ObjectMap.put(id, obj);
	}

	public void add_Id2Object(Object obj, Long id, int version, ResolutionDepth resolutionDepth)
	{
		this.addTo_Id2ObjectMap(obj, id);
		this.addTo_Object2IdMap(obj, id, version, resolutionDepth);
	}

	public void replace_Id2Object(Object old, Object obj, Long id)
	{
		this.addTo_Id2ObjectMap(obj, id);
		LoadInfo li = this.object2IdMap.remove(old);
		this.addTo_Object2IdMap(obj, id, li.version, li.resolutionDepth);
	}

	public boolean existsRelation(IRelation relat)
	{
		List<IRelation> rels = this.object2RelationsMap.get(relat.getStart());
		if (rels != null)
		{
			for (IRelation r : rels)
			{
				if (r.equals(relat))
					return true;
			}
		}
		return false;
	}

	public List<KeyedRelation> getKeyedRelations(SourceField2TargetKey key)
	{
		return this.objectField2KeyedRelationsMap.get(key);
	}

	public List<KeyedRelation> getKeyedRelations(SourceFieldKey key)
	{
		return this.multiRelationsMap.get(key);
	}

	public IRelation findRelation(Object start, String type)
	{
		List<IRelation> rels = this.object2RelationsMap.get(start);
		if (rels != null)
		{
			for (IRelation r : rels)
			{
				if (r.getType().equals(type))
					return r;
			}
		}
		return null;
	}

	public void removeRelation(IRelation relat)
	{
		if (relat instanceof KeyedRelation)
		{
			SourceField2TargetKey key = new SourceField2TargetKey(relat.getStart(),
					relat.getType(), relat.getEnd());
			List<KeyedRelation> rels = this.objectField2KeyedRelationsMap.get(key);
			if (rels != null)
			{
				rels.remove(relat);
			}
			SourceFieldKey fieldKey = key.getSourceFieldKey();
			rels = this.multiRelationsMap.get(fieldKey);
			if (rels != null)
			{
				rels.remove(relat);
			}
		} else
		{
			List<IRelation> rels = this.object2RelationsMap.get(relat.getStart());
			if (rels != null)
			{
				rels.remove(relat);
			}
		}
		this.relation2IdMap.remove(relat);
	}

	public Object getFrom_Id2ObjectMap(Long id)
	{
		return this.id2ObjectMap.get(id);
	}

	public SurrogateState getSurrogateState()
	{
		return surrogateState;
	}

	/***********************************/
	public interface IRelation
	{
		public String getType();

		public Object getStart();

		public Object getEnd();

		public void setDomainState(DomainState domainState);

		public void setId(long id);

		public IRelation createCopy(DomainState ds);

		/**
		 * @return true if start and / or end is an InnerClassSurrogate
		 */
		public boolean isDeferred();
	}

	/***********************************/
	public static class Relation implements IRelation
	{
		private String type;
		private Object start;
		private Object end;
		private DomainState domainState;
		private long id;

		public Relation(String type, Object start, Object end)
		{
			super();
			this.type = type;
			Object strt = start;
			if (start instanceof InnerClassSurrogate)
			{
				Object ro = ((InnerClassSurrogate) start).getRealObject();
				if (ro == null)
					((InnerClassSurrogate) start).addRelationUpdate(new RelationUpdate(true));
				else
					strt = ro;
			}
			this.start = strt;
			Object nd = end;
			if (end instanceof InnerClassSurrogate)
			{
				Object ro = ((InnerClassSurrogate) end).getRealObject();
				if (ro == null)
					((InnerClassSurrogate) end).addRelationUpdate(new RelationUpdate(false));
				else
					nd = ro;
			}
			this.end = nd;
		}

		@Override
		public IRelation createCopy(DomainState ds)
		{
			Relation ret = new Relation(this.type, this.start, this.end);
			ret.domainState = ds;
			ret.id = this.id;
			return ret;
		}

		@Override
		public String getType()
		{
			return type;
		}

		@Override
		public Object getStart()
		{
			return start;
		}

		@Override
		public Object getEnd()
		{
			return end;
		}

		@Override
		public boolean isDeferred()
		{
			return this.start instanceof InnerClassSurrogate ||
					this.end instanceof InnerClassSurrogate;
		}

		@Override
		public void setDomainState(DomainState domainState)
		{
			this.domainState = domainState;
		}

		protected long getId()
		{
			return this.id;
		}

		@Override
		public void setId(long id)
		{
			this.id = id;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((end == null) ? 0 : end.hashCode());
			result = prime * result + ((start == null) ? 0 : start.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Relation other = (Relation) obj;
			if (end == null)
			{
				if (other.end != null)
					return false;
			} else if (!end.equals(other.end))
				return false;
			if (start == null)
			{
				if (other.start != null)
					return false;
			} else if (!start.equals(other.start))
				return false;
			if (type == null)
			{
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

		@Override
		public String toString()
		{
			return "Relation [type=" + type + ", start=" + start + ", end="
					+ end + "]";
		}

		/**********************************/
		public class RelationUpdate
		{

			private boolean start;

			private RelationUpdate(boolean start)
			{
				super();
				this.start = start;
			}

			public void updateWith(Object startOrEnd)
			{
				RelationLoadInfo rli = Relation.this.domainState.getFrom_Relation2IdMap(Relation.this);
				int v = rli != null ? rli.getVersion() : -1;
				if (this.start)
					Relation.this.start = startOrEnd;
				else
					Relation.this.end = startOrEnd;
				Relation.this.domainState.add_Id2Relation(Relation.this,
						Relation.this.id, v);
			}
		}
	}

	/***********************************/
	public static class KeyedRelation extends Relation
	{

		// must be of simple type so that it can be mapped to a property
		private Object key;
		// must be either null or
		// must be of simple type so that it can be mapped to a property
		private Object value;

		public KeyedRelation(String type, Object key, Object start, Object end)
		{
			super(type, start, end);
			this.key = key;
		}

		@Override
		public IRelation createCopy(DomainState ds)
		{
			KeyedRelation ret = new KeyedRelation(getType(), this.key, getStart(), getEnd());
			ret.setDomainState(ds);
			ret.setId(getId());
			return ret;
		}

		public Object getKey()
		{
			return this.key;
		}

		public Object getValue()
		{
			return value;
		}

		public void setValue(Object value)
		{
			this.value = value;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			KeyedRelation other = (KeyedRelation) obj;
			if (key == null)
			{
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			if (value == null)
			{
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString()
		{
			return "KeyedRelation [key=" + key + ", getType()=" + getType()
					+ ", getStart()=" + getStart() + ", getEnd()=" + getEnd()
					+ "]";
		}

	}

	/***********************************/
	public static class KeyedRelationToChange implements IRelation
	{
		private KeyedRelation existingOne;
		private KeyedRelation newOne;

		public KeyedRelationToChange(KeyedRelation existingOne, KeyedRelation newOne)
		{
			super();
			this.existingOne = existingOne;
			this.newOne = newOne;
		}

		@Override
		public IRelation createCopy(DomainState ds)
		{
			KeyedRelationToChange ret = new KeyedRelationToChange(
					(KeyedRelation) this.existingOne.createCopy(ds),
					(KeyedRelation) this.newOne.createCopy(ds));
			return ret;
		}

		@Override
		public String getType()
		{
			return this.existingOne.getType();
		}

		@Override
		public Object getStart()
		{
			return this.existingOne.getStart();
		}

		@Override
		public Object getEnd()
		{
			return this.existingOne.getEnd();
		}

		@Override
		public boolean isDeferred()
		{
			return this.newOne.isDeferred() || this.existingOne.isDeferred();
		}

		@Override
		public void setDomainState(DomainState domainState)
		{
			if (this.newOne.isDeferred())
				this.newOne.setDomainState(domainState);
			if (this.existingOne.isDeferred())
				this.existingOne.setDomainState(domainState);
		}

		@Override
		public void setId(long id)
		{
			if (this.newOne.isDeferred())
				this.newOne.setId(id);
			if (this.existingOne.isDeferred())
				this.existingOne.setId(id);
		}

		public KeyedRelation getNewOne()
		{
			return this.newOne;
		}
	}

	/********************************/
	public static class SourceField2TargetKey
	{
		private Object source;
		private String fieldName;
		private Object target;

		public SourceField2TargetKey(Object src, String fieldName, Object target)
		{
			super();
			this.source = src;
			this.fieldName = fieldName;
			this.target = target;
		}

		public SourceField2TargetKey(SourceFieldKey sfk, Object target)
		{
			super();
			this.source = sfk.getSource();
			this.fieldName = sfk.getFieldName();
			this.target = target;
		}

		public Object getTarget()
		{
			return this.target;
		}

		public SourceFieldKey getSourceFieldKey()
		{
			return new SourceFieldKey(this.source, this.fieldName);
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((fieldName == null) ? 0 : fieldName.hashCode());
			result = prime * result
					+ ((source == null) ? 0 : source.hashCode());
			result = prime * result
					+ ((target == null) ? 0 : target.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SourceField2TargetKey other = (SourceField2TargetKey) obj;
			if (fieldName == null)
			{
				if (other.fieldName != null)
					return false;
			} else if (!fieldName.equals(other.fieldName))
				return false;
			if (source == null)
			{
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (target == null)
			{
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}
	}

	/********************************/
	public static class SourceFieldKey
	{
		private Object source;
		private String fieldName;

		public SourceFieldKey(Object src, String fieldName)
		{
			super();
			this.source = src;
			this.fieldName = fieldName;
		}

		public Object getSource()
		{
			return source;
		}

		public String getFieldName()
		{
			return fieldName;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((fieldName == null) ? 0 : fieldName.hashCode());
			result = prime * result
					+ ((source == null) ? 0 : source.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SourceFieldKey other = (SourceFieldKey) obj;
			if (fieldName == null)
			{
				if (other.fieldName != null)
					return false;
			} else if (!fieldName.equals(other.fieldName))
				return false;
			if (source == null)
			{
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}

	}

	/********************************/
	public static class RelationLoadInfo
	{
		private Long id;
		private int version;

		private RelationLoadInfo createCopy()
		{
			RelationLoadInfo ret = new RelationLoadInfo();
			ret.id = this.id;
			ret.version = this.version;
			return ret;
		}

		public Long getId()
		{
			return id;
		}

		public int getVersion()
		{
			return version;
		}
	}

	/********************************/
	public static class LoadInfo
	{
		private Long id;
		private int version;
		private ResolutionDepth resolutionDepth;
		private DomainObject domainObject;

		public LoadInfo()
		{
			super();
		}

		private LoadInfo createCopy()
		{
			LoadInfo ret = new LoadInfo();
			ret.id = this.id;
			ret.version = this.version;
			ret.resolutionDepth = this.resolutionDepth;
			ret.domainObject = this.domainObject;
			return ret;
		}

		public Long getId()
		{
			return id;
		}

		public int getVersion()
		{
			return version;
		}

		public void setVersion(int v)
		{
			this.version = v;
		}

		public ResolutionDepth getResolutionDepth()
		{
			return resolutionDepth;
		}

		public LoadInfo setResolutionDepth(ResolutionDepth resolutionDepth)
		{
			this.resolutionDepth = resolutionDepth;
			return this;
		}

		public DomainObject getDomainObject()
		{
			return domainObject;
		}

		public void setDomainObject(DomainObject domainObject)
		{
			this.domainObject = domainObject;
		}
	}
}
