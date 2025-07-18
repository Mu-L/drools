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
package org.drools.mvel.integrationtests.session;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.drools.mvel.compiler.Person;
import org.drools.mvel.compiler.PolymorphicFact;
import org.drools.mvel.compiler.Primitives;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieBase;
import org.kie.api.definition.type.FactType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeCoercionTest {

    public static Stream<KieBaseTestConfiguration> parameters() {
        // TODO: EM failed with some tests. File JIRAs
        return TestParametersUtil2.getKieBaseCloudConfigurations(false).stream();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testRuntimeTypeCoercion(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        KieBase kbase = KieBaseUtil.getKieBaseFromClasspathResources(getClass(), kieBaseTestConfiguration, "test_RuntimeTypeCoercion.drl");
        KieSession ksession = kbase.newKieSession();

        final List list = new ArrayList();
        ksession.setGlobal("results", list);

        final PolymorphicFact fact = new PolymorphicFact(10);
        final FactHandle handle = ksession.insert(fact);

        ksession.fireAllRules();

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0)).isEqualTo(fact.getData());

        fact.setData("10");
        ksession.update(handle, fact);
        ksession.fireAllRules();

        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(1)).isEqualTo(fact.getData());

        fact.setData(Boolean.TRUE);
        ksession.update(handle, fact);

        assertThat(list.size()).isEqualTo(2);
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testRuntimeTypeCoercion2(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        KieBase kbase = KieBaseUtil.getKieBaseFromClasspathResources(getClass(), kieBaseTestConfiguration, "test_RuntimeTypeCoercion2.drl");
        KieSession ksession = kbase.newKieSession();

        final List list = new ArrayList();
        ksession.setGlobal("results", list);

        final Primitives fact = new Primitives();
        fact.setBooleanPrimitive(true);
        fact.setBooleanWrapper(Boolean.TRUE);
        fact.setObject(Boolean.TRUE);
        fact.setCharPrimitive('X');
        final FactHandle handle = ksession.insert(fact);

        ksession.fireAllRules();

        int index = 0;
        assertThat(list.size()).as(list.toString()).isEqualTo(4);
        assertThat(list.get(index++)).isEqualTo("boolean");
        assertThat(list.get(index++)).isEqualTo("boolean wrapper");
        assertThat(list.get(index++)).isEqualTo("boolean object");
        assertThat(list.get(index++)).isEqualTo("char");

        fact.setBooleanPrimitive(false);
        fact.setBooleanWrapper(null);
        fact.setCharPrimitive('\0');
        fact.setObject('X');
        ksession.update(handle, fact);
        ksession.fireAllRules();
        assertThat(list.size()).isEqualTo(5);
        assertThat(list.get(index++)).isEqualTo("char object");

        fact.setObject(null);
        ksession.update(handle, fact);
        ksession.fireAllRules();
        assertThat(list.size()).isEqualTo(6);
        assertThat(list.get(index)).isEqualTo("null object");
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testUnwantedCoersion(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final String rule = "package org.drools.compiler\n" +
                "import " + InnerBean.class.getCanonicalName() + ";\n" +
                "import " + OuterBean.class.getCanonicalName() + ";\n" +
                "rule \"Test.Code One\"\n" +
                "when\n" +
                "   OuterBean($code : inner.code in (\"1.50\", \"2.50\"))\n" +
                "then\n" +
                "   System.out.println(\"Code compared values: 1.50, 2.50 - actual code value: \" + $code);\n" +
                "end\n" +
                "rule \"Test.Code Two\"\n" +
                "when\n" +
                "   OuterBean($code : inner.code in (\"1.5\", \"2.5\"))\n" +
                "then\n" +
                "   System.out.println(\"Code compared values: 1.5, 2.5 - actual code value: \" + $code);\n" +
                "end\n" +
                "rule \"Big Test ID One\"\n" +
                "when\n" +
                "   OuterBean($id : id in (\"3.5\", \"4.5\"))\n" +
                "then\n" +
                "   System.out.println(\"ID compared values: 3.5, 4.5 - actual ID value: \" + $id);\n" +
                "end\n" +
                "rule \"Big Test ID Two\"\n" +
                "when\n" +
                "   OuterBean($id : id in ( \"3.0\", \"4.0\"))\n" +
                "then\n" +
                "   System.out.println(\"ID compared values: 3.0, 4.0 - actual ID value: \" + $id);\n" +
                "end";

        KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, rule);
        final KieSession ksession = kbase.newKieSession();

        final InnerBean innerTest = new InnerBean();
        innerTest.setCode("1.500");
        ksession.insert(innerTest);

        final OuterBean outerTest = new OuterBean();
        outerTest.setId("3");
        outerTest.setInner(innerTest);
        ksession.insert(outerTest);

        final OuterBean outerTest2 = new OuterBean();
        outerTest2.setId("3.0");
        outerTest2.setInner(innerTest);
        ksession.insert(outerTest2);

        final int rules = ksession.fireAllRules();
        assertThat(rules).isEqualTo(1);
    }

    public static class InnerBean {
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }
    }

    public static class OuterBean {
        private InnerBean inner;
        private String    id;

        public InnerBean getInner() {
            return inner;
        }

        public void setInner(final InnerBean inner) {
            this.inner = inner;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testCoercionOfStringValueWithoutQuotes(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        // JBRULES-3080
        final String str = "package org.drools.mvel.compiler.test; \n" +
                "declare A\n" +
                "   field : String\n" +
                "end\n" +
                "rule R when\n" +
                "   A( field == 12 )\n" +
                "then\n" +
                "end\n";

        KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, str);
        final KieSession ksession = kbase.newKieSession();

        final FactType typeA = kbase.getFactType("org.drools.mvel.compiler.test", "A");
        final Object a = typeA.newInstance();
        typeA.set(a, "field", "12");
        ksession.insert(a);

        assertThat(ksession.fireAllRules()).isEqualTo(1);
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testPrimitiveToBoxedCoercionInMethodArgument(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final String str = "package org.drools.mvel.compiler.test;\n" +
                "import " + TypeCoercionTest.class.getName() + "\n" +
                "import org.drools.mvel.compiler.*\n" +
                "rule R1 when\n" +
                "   Person( $ag1 : age )" +
                "   $p2 : Person( name == TypeCoercionTest.integer2String($ag1) )" +
                "then\n" +
                "end\n";

        KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, str);
        final KieSession ksession = kbase.newKieSession();

        final Person p = new Person("42", 42);
        ksession.insert(p);
        assertThat(ksession.fireAllRules()).isEqualTo(1);
    }

    public static String integer2String(final Integer value) {
        return "" + value;
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testStringCoercion(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-1688
        final String drl = "package org.drools.mvel.compiler.test;\n" +
                           "import " + Person.class.getCanonicalName() + "\n" +
                           " rule R1 when\n" +
                           "     Person(name == \"12\")\n" +
                           " then end\n" +
                           " rule R2 when\n" +
                           "     Person(name == 11)\n " +
                           " then\n end\n" +
                           " rule R3 when\n" +
                           "    Person(name == \"11\")\n" +
                           " then end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, drl);
        KieSession kieSession = kieBase.newKieSession();

        kieSession.insert(new Person("11"));
        assertThat(kieSession.fireAllRules()).isEqualTo(2);
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testIntCoercion(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-1688
        final String drl = "package org.drools.mvel.compiler.test;\n" +
                           "import " + Person.class.getCanonicalName() + "\n" +
                           " rule R1 when\n" +
                           "     Person(age == 12)\n" +
                           " then end\n" +
                           " rule R2 when\n" +
                           "     Person(age == \"11\")\n " +
                           " then\n end\n" +
                           " rule R3 when\n" +
                           "    Person(age == 11)\n" +
                           " then end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, drl);
        KieSession kieSession = kieBase.newKieSession();

        kieSession.insert(new Person("Mario", 11));
        assertThat(kieSession.fireAllRules()).isEqualTo(2);
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testCoercionInJoin(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-2695
        final String drl =
                " rule R1 when\n" +
                "     Integer($i : intValue)\n" +
                "     String(this == $i)\n" +
                " then end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, drl);
        KieSession kieSession = kieBase.newKieSession();

        kieSession.insert(2);
        kieSession.insert("2");
        assertThat(kieSession.fireAllRules()).isEqualTo(1);
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testCoercionInJoinOnField(KieBaseTestConfiguration kieBaseTestConfiguration) {
        // DROOLS-2695
        final String drl =
                "import " + Person.class.getCanonicalName() + "\n" +
                " rule R1 when\n" +
                "     Integer($i : intValue)\n" +
                "     Person(name == $i)\n" +
                " then end\n";

        KieBase kieBase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, drl);
        KieSession kieSession = kieBase.newKieSession();

        kieSession.insert(2);
        kieSession.insert(new Person("2", 11));
        assertThat(kieSession.fireAllRules()).isEqualTo(1);
    }
}
