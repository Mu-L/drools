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
package org.drools.traits.core.factmodel;

import java.util.Calendar;
import java.util.Date;

import org.drools.core.base.ClassFieldAccessorCache;
import org.drools.mvel.accessors.ClassFieldAccessorStore;
import org.drools.compiler.builder.impl.classbuilder.ClassBuilder;
import org.drools.base.factmodel.ClassDefinition;
import org.drools.base.factmodel.FieldDefinition;
import org.drools.core.rule.JavaDialectRuntimeData;
import org.drools.wiring.dynamic.PackageClassLoader;
import org.junit.jupiter.api.Test;
import org.drools.wiring.api.classloader.ProjectClassLoader;

import static org.drools.util.ClassUtils.convertClassToResourcePath;

/**
 * @version $Id$
 */
public class InstancesHashcodedTest {


/*
        <dimension name="bck_cycle_dm"  item-expression="cycleDimension"
                   mode="ignore" cache="10" export-to="cycle_id">
            <id    name="objid"            type="long"             generator-sql="select bck_dimensions_uid.nextval from dual"/>
            <field name="cut_date"         type="java.util.Date"   value="cutDate"     key="true"/>
            <field name="due_date"         type="java.util.Date"   value="dueDate"     key="true"/>
            <field name="cycle_code"       type="java.lang.String" value="cycleCode"/>
            <field name="issue_date"       type="java.util.Date"   value="issueDate"/>
        </dimension>
 */

    private Class build(ClassBuilder builder, ClassDefinition classDef) throws Exception {
        ProjectClassLoader prjClassLoader = ProjectClassLoader.createProjectClassLoader();
        byte[] d = builder.buildClass( classDef, prjClassLoader );
        JavaDialectRuntimeData data = new JavaDialectRuntimeData();
        data.write( convertClassToResourcePath(classDef.getClassName()), d );
        ClassLoader classLoader = new PackageClassLoader(data.getStore(), prjClassLoader);
        
        ClassFieldAccessorStore store = new ClassFieldAccessorStore();
        store.setClassFieldAccessorCache( new ClassFieldAccessorCache( classLoader ) );
        store.setEagerWire( true );
        
        Class clazz = classLoader.loadClass( classDef.getClassName() );
        classDef.setDefinedClass( clazz );
        
        return clazz;
        
    }

    @Test
    public void testInstanceHashcodes() {

        ClassDefinition cd = new ClassDefinition( "br.com.auster.TestClass2", null, new String[]{} );
        cd.addField(new FieldDefinition("cutDate", "java.util.Date", true));
        cd.addField(new FieldDefinition("dueDate", "java.util.Date", true));
        cd.addField(new FieldDefinition("issueDate", "java.util.Date", false));
        cd.addField(new FieldDefinition("cycleCode", "java.lang.String", false));

        ClassBuilder cb = new TraitClassBuilderFactory().getBeanClassBuilder();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2006);
        cal.set(Calendar.MONTH, Calendar.JUNE);
        cal.set(Calendar.DAY_OF_MONTH, 01);
        Date cut = cal.getTime();

        cal.set(Calendar.MONTH, Calendar.JULY);
        cal.set(Calendar.DAY_OF_MONTH, 13);
        Date d1 = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, 15);
        Date d2 = cal.getTime();

        try {
            Class klass = build(cb, cd);
            Object o1 = klass.newInstance();
            cd.getField("cutDate").getFieldAccessor().setValue(o1, cut);
            cd.getField("dueDate").getFieldAccessor().setValue(o1, d1);

            Object o2 = klass.newInstance();
            cd.getField("cutDate").getFieldAccessor().setValue(o2, cut);
            cd.getField("dueDate").getFieldAccessor().setValue(o2, d2);
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }
}
