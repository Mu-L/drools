/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.base.base;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.drools.base.InitialFact;
import org.drools.base.common.DroolsObjectInput;
import org.drools.base.reteoo.InitialFactImpl;
import org.drools.util.ClassUtils;
import org.drools.util.bitmask.BitMask;
import org.kie.api.runtime.rule.Match;

/**
 * Java class semantics <code>ObjectType</code>.
 *
 *
 * @version $Id: ClassObjectType.java,v 1.5 2005/02/04 02:13:36 mproctor Exp $
 */
public class ClassObjectType
    implements
    ObjectType,
    ClassWireable,
    Externalizable {
    
    public static final ClassObjectType InitialFact_ObjectType = new ClassObjectType( InitialFactImpl.class );
    
    public static final ClassObjectType DroolsQuery_ObjectType = new ClassObjectType( DroolsQuery.class );
    
    public static final ClassObjectType Map_ObjectType = new ClassObjectType( Map.class );
    
    public static final ClassObjectType ObjectArray_ObjectType = new ClassObjectType( Object[].class );
    
    public static final ClassObjectType Match_ObjectType = new ClassObjectType( Match.class );
    

    private static final long serialVersionUID = 510l;

    /** Java object class. */
    protected Class<?>        cls;

    protected String          clsName;

    protected ValueType       valueType;

    private boolean           isEvent;

    private transient Map<String, BitMask> transformedMasks;

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------

    public ClassObjectType() {
    }

    /**
     * Creates a new class object type with shadow disabled.
     *
     * @param objectTypeClass
     *            Java object class.
     */
    public ClassObjectType(final Class<?> objectTypeClass) {
        this( objectTypeClass,
              false );
    }

    /**
     * Creates a new class object type
     *
     * @param objectTypeClass the class represented by this class object type
     * @param isEvent true if it is an event class, false otherwise
     */
    public ClassObjectType(final Class<?> objectTypeClass,
                           final boolean isEvent) {
        this.cls = objectTypeClass;
        this.isEvent = isEvent;
        this.clsName = this.cls.getName();
        this.valueType = ValueType.determineValueType( objectTypeClass );
    }


    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        this.clsName = in.readUTF();
        
        // we handle these directly as they never gets written to the packagestore for rewiring
        if ( InitialFact.class.getName().equals( clsName ) || InitialFactImpl.class.getName().equals( clsName )  ) {
            setClassType( InitialFactImpl.class );
            this.valueType = ValueType.OBJECT_TYPE;
        } else if ( DroolsQuery.class.getName().equals( clsName )  ){
            setClassType( DroolsQuery.class );
            this.valueType = ValueType.QUERY_TYPE;
        } else {
            try {
                Class<?> clazz = in instanceof DroolsObjectInput ?
                                 ClassUtils.getClassFromName( clsName, false, ( (DroolsObjectInput) in ).getClassLoader() ) :
                                 ClassUtils.getClassFromName( clsName );
                setClassType( clazz );
            } catch (ClassNotFoundException e) {
                throw new RuntimeException( e );
            }
        }

        this.isEvent = in.readBoolean();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF( clsName );
        out.writeBoolean( isEvent );
    }

    public Class<?> getClassType() {
        return this.cls;
    }

    @Override
    public Object getTypeKey() {
        return getClassType();
    }

    @Override
    public String getClassName() {
        return this.clsName;
    }

    @Override
    public boolean hasField(String name) {
        return ClassUtils.getFieldOrAccessor(cls, name) != null;
    }

    public void setClassType(Class<?> cls) {
        this.cls = cls;
        this.valueType = ValueType.determineValueType( cls );
    }

    @Override
    public boolean isAssignableFrom(ObjectType objectType) {
        return objectType instanceof ClassObjectType && isAssignableFrom( ( (ClassObjectType) objectType ).getClassType() );
    }

    @Override
    public boolean isAssignableFrom(Class<?> clazz) {
        return this.cls.isAssignableFrom( clazz );
    }

    @Override
    public boolean isAssignableTo(Class<?> clazz) {
        return clazz.isAssignableFrom( this.cls );
    }

    @Override
    public ValueType getValueType() {
        return this.valueType;
    }

    @Override
    public boolean isEvent() {
        return isEvent;
    }

    public void setEvent(boolean isEvent) {
        this.isEvent = isEvent;
    }

    @Override
    public boolean isPrototype() {
        return false;
    }

    @Override
    public String toString() {
        return "[ClassObjectType " + (this.isEvent ? "event=" : "class=") + getClassType().getName() + "]";
    }

    /**
     * Determine if another object is equal to this.
     *
     * @param object
     *            The object to test.
     *
     * @return <code>true</code> if <code>object</code> is equal to this,
     *         otherwise <code>false</code>.
     */
    public boolean equals(final Object object) {
        if ( this == object ) {
            return true;
        }

        if ( object == null || object.getClass() != ClassObjectType.class ) {
            return false;
        }

        return this.clsName.equals( ((ClassObjectType) object).clsName );
    }

    public int hashCode() {
        return this.clsName.hashCode();
    }

    public void wire( Class<?> klass ) {
        this.cls = klass;
    }

    public BitMask getTransformedMask(Class<?> modifiedClass, BitMask modificationMask) {
        if (transformedMasks == null) {
            return null;
        }
        String key = modifiedClass.getName() + ":" + modificationMask;
        return transformedMasks.get(key);
    }

    public void storeTransformedMask(Class<?> modifiedClass, BitMask modificationMask, BitMask transforedMask) {
        if (transformedMasks == null) {
            transformedMasks = new ConcurrentHashMap<>();
        }
        String key = modifiedClass.getName() + ":" + modificationMask;
        transformedMasks.put(key, transforedMask);
    }
}
