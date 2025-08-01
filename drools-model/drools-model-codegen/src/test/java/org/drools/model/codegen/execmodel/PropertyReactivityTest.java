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
package org.drools.model.codegen.execmodel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.drools.model.codegen.execmodel.domain.Address;
import org.drools.model.codegen.execmodel.domain.Person;
import org.drools.model.codegen.execmodel.domain.Pet;
import org.drools.model.codegen.execmodel.domain.Result;
import org.drools.model.codegen.execmodel.domain.VariousCasePropFact;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.Message.Level;
import org.kie.api.definition.type.Modifies;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyReactivityTest extends BaseModelTest {

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPropertyReactivity(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( name == \"Mario\" )\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPropertyReactivityWithUpdate(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( name == \"Mario\" )\n" +
                "then\n" +
                "    $p.setAge( $p.getAge()+1 );\n" +
                "    update($p);\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPropertyReactivityMvel(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R dialect\"mvel\" when\n" +
                "    $p : Person( name == \"Mario\" )\n" +
                "then\n" +
                "    modify($p) { age = $p.age+1 };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPropertyReactivityMvelWithUpdate(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R dialect\"mvel\" when\n" +
                "    $p : Person( name == \"Mario\" )\n" +
                "then\n" +
                "    $p.age = $p.age+1;\n" +
                "    update($p);\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testWatch(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( age < 50 ) @watch(!age)\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testWatchAll(RUN_TYPE runType) {
        // DROOLS-4509

        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( name == \"Mario\" ) @watch(*)\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(10);

        assertThat(p.getAge()).isEqualTo(50);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testWatchAllBeforeBeta(RUN_TYPE runType) {
        // DROOLS-4509

        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "import " + Address.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( name == \"Mario\" ) @watch(*)\n" +
                "    Address() \n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.insert( new Address( "Milan" ) );
        ksession.fireAllRules(10);

        assertThat(p.getAge()).isEqualTo(50);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testWatchAllBeforeFrom(RUN_TYPE runType) {
        // DROOLS-4509

        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "import " + Address.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( name == \"Mario\" ) @watch(*)\n" +
                "    Address() from $p.addresses\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        p.addAddress( new Address( "Milan" ) );
        p.addAddress( new Address( "Rome" ) );
        ksession.insert( p );
        ksession.fireAllRules(10);

        assertThat(p.getAge()).isEqualTo(50);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testImplicitWatch(RUN_TYPE runType) {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $r : Result()\n" +
                "  $p1 : Person()\n" +
                "  $p2 : Person(name != \"Mark\", this != $p1, age > $p1.age)\n" +
                "then\n" +
                "  $r.setValue($p2.getName() + \" is older than \" + $p1.getName());\n" +
                "end";

        KieSession ksession = getKieSession(runType, str);

        Result result = new Result();
        ksession.insert(result);

        Person mark = new Person("Mark", 37);
        Person edson = new Person("Edson", 35);
        Person mario = new Person("Mario", 40);

        FactHandle markFH = ksession.insert(mark);
        FactHandle edsonFH = ksession.insert(edson);
        FactHandle marioFH = ksession.insert(mario);

        ksession.fireAllRules();
        assertThat(result.getValue()).isEqualTo("Mario is older than Mark");

        result.setValue(null);
        ksession.delete(marioFH);
        ksession.fireAllRules();
        assertThat(result.getValue()).isNull();

        mark.setAge(34);
        ksession.update(markFH, mark, "age");

        ksession.fireAllRules();
        assertThat(result.getValue()).isEqualTo("Edson is older than Mark");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testImplicitWatchWithDeclaration(RUN_TYPE runType) {
        String str =
                "import " + Result.class.getCanonicalName() + ";" +
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R when\n" +
                "  $r : Result()\n" +
                "  $p1 : Person( $a : address )\n" +
                "  $p2 : Person(name != \"Mark\", this != $p1, age > $p1.age)\n" +
                "then\n" +
                "  $r.setValue($p2.getName() + \" is older than \" + $p1.getName());\n" +
                "end";

        KieSession ksession = getKieSession(runType, str);

        Result result = new Result();
        ksession.insert(result);

        Person mark = new Person("Mark", 37);
        Person edson = new Person("Edson", 35);
        Person mario = new Person("Mario", 40);

        FactHandle markFH = ksession.insert(mark);
        FactHandle edsonFH = ksession.insert(edson);
        FactHandle marioFH = ksession.insert(mario);

        ksession.fireAllRules();
        assertThat(result.getValue()).isEqualTo("Mario is older than Mark");

        result.setValue(null);
        ksession.delete(marioFH);
        ksession.fireAllRules();
        assertThat(result.getValue()).isNull();

        mark.setAge(34);
        ksession.update(markFH, mark, "age");

        ksession.fireAllRules();
        assertThat(result.getValue()).isEqualTo("Edson is older than Mark");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testImmutableField(RUN_TYPE runType) {
        final String str =
                "declare Integer @propertyReactive end\n" +
                "declare Long @propertyReactive end\n" +
                "rule R when\n" +
                "    $i : Integer( intValue > 0 )\n" +
                "    Long( $l : intValue == $i )\n" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        ksession.insert( 42 );
        ksession.insert( 42L );
        assertThat(ksession.fireAllRules()).isEqualTo(1);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    @Timeout(5000)
    public void testPRAfterAccumulate(RUN_TYPE runType) {
        // DROOLS-2427
        final String str =
                "import " + Order.class.getCanonicalName() + "\n" +
                "import " + OrderLine.class.getCanonicalName() + "\n" +
                "rule R when\n" +
                "        $o: Order($lines: orderLines)\n" +
                "        Number(intValue >= 15) from accumulate(\n" +
                "            OrderLine($q: quantity) from $lines\n" +
                "            , sum($q)\n" +
                "        )\n" +
                "    then\n" +
                "        modify($o) { setPrice(10) }\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Order order = new Order( Arrays.asList(new OrderLine( 9 ), new OrderLine( 8 )), 12 );
        ksession.insert( order );
        ksession.fireAllRules();

        assertThat(order.getPrice()).isEqualTo(10);
    }

    public static class Order {
        private final List<OrderLine> orderLines;

        private int price;

        public Order( List<OrderLine> orderLines, int price ) {
            this.orderLines = orderLines;
            this.price = price;
        }

        public List<OrderLine> getOrderLines() {
            return orderLines;
        }

        public int getPrice() {
            return price;
        }

        public void setPrice( int price ) {
            this.price = price;
        }
    }

    public static class OrderLine {
        private final int quantity;

        public OrderLine( int quantity ) {
            this.quantity = quantity;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    public static class Bean {
        private final List<String> firings = new ArrayList<>();

        public List<String> getFirings() {
            return firings;
        }

        public String getValue() {
            return "Bean";
        }

        public void setValue(String value) {}
    }

    @ParameterizedTest
	@MethodSource("parameters")
    @Timeout(5000)
    public void testPRWithAddOnList(RUN_TYPE runType) {
        final String str =
                "import " + Bean.class.getCanonicalName() + "\n" +
                "rule R when\n" +
                "    $b : Bean( firings not contains \"R\" )\n" +
                "then\n" +
                "    $b.getFirings().add(\"R\");\n" +
                "    update($b);\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        ksession.insert( new Bean() );
        assertThat(ksession.fireAllRules()).isEqualTo(1);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPRWithUpdateOnList(RUN_TYPE runType) {
        final String str =
                "import " + List.class.getCanonicalName() + "\n" +
                "rule R1 when\n" +
                "    $l : List( empty == true )\n" +
                "then\n" +
                "    $l.add(\"test\");\n" +
                "    update($l);\n" +
                "end\n" +
                "rule R2 when\n" +
                "    $l : List( !this.contains(\"test\") )\n" +
                "then\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        ksession.insert( new ArrayList() );
        assertThat(ksession.fireAllRules()).isEqualTo(1);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPROnAtomic(RUN_TYPE runType) {
        final String str =
                "import " + AtomicInteger.class.getCanonicalName() + "\n" +
                "rule R2 when\n" +
                "    $i : AtomicInteger( get() < 3 )\n" +
                "then\n" +
                "    $i.incrementAndGet();" +
                "    insert(\"test\" + $i.get());" +
                "    update($i);" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        ksession.insert(new AtomicInteger(0));
        assertThat(ksession.fireAllRules()).isEqualTo(3);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    @Timeout(10000)
    public void testPropertyReactivityWith2Rules(RUN_TYPE runType) {
        checkPropertyReactivityWith2Rules(runType, "age == 41\n");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    @Timeout(10000)
    public void testPropertyReactivityWith2RulesUsingAccessor(RUN_TYPE runType) {
        checkPropertyReactivityWith2Rules(runType, "getAge() == 41\n");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    @Timeout(10000)
    public void testPropertyReactivityWith2RulesLiteralFirst(RUN_TYPE runType) {
        checkPropertyReactivityWith2Rules(runType, "41 == age\n");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    @Timeout(10000)
    public void testPropertyReactivityWith2RulesLiteralFirstUsingAccessor(RUN_TYPE runType) {
        checkPropertyReactivityWith2Rules(runType, "41 == getAge()\n");
    }

    private void checkPropertyReactivityWith2Rules(RUN_TYPE runType, String constraint) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R1 when\n" +
                "    $p : Person( age == 40 )\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n" +
                "rule R2 when\n" +
                "    $p : Person( " + constraint + " )\n" +
                "then\n" +
                "    modify($p) { setEmployed( true ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person( "Mario", 40 );
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(41);
        assertThat(p.getEmployed()).isTrue();
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testReassignment(RUN_TYPE runType) {
        // DROOLS-4884
        final String str =
                "package com.example\n" +
                "\n" +
                "declare Counter\n" +
                "    value1: int\n" +
                "    value2: int\n" +
                "end\n" +
                "\n" +
                "rule \"Init\" when\n" +
                "    not Counter()\n" +
                "then\n" +
                "    drools.insert(new Counter(0, 0));\n" +
                "end\n" +
                "\n" +
                "rule \"Loop\"\n" +
                "when\n" +
                "    $c: Counter( value1 == 0 )\n" +
                "then\n" +
                "    $c = new Counter(0, 0);\n" +
                "    $c.setValue2(1);\n" +
                "    update($c);\n" +
                "end\n\n";

        KieSession ksession = getKieSession(runType, str);

        assertThat(ksession.fireAllRules(5)).isEqualTo(5);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testReassignment2(RUN_TYPE runType) {
        // DROOLS-4884
        final String str =
                "package com.example\n" +
                "\n" +
                "declare Counter\n" +
                "    value1: int\n" +
                "    value2: int\n" +
                "end\n" +
                "\n" +
                "rule \"Init\" when\n" +
                "    not Counter()\n" +
                "then\n" +
                "    drools.insert(new Counter(0, 0));\n" +
                "end\n" +
                "\n" +
                "rule \"Loop\"\n" +
                "when\n" +
                "    $c: Counter( value1 == 0 )\n" +
                "then\n" +
                "    $c = new Counter(0, 0);\n" +
                "    $c.setValue1(1);\n" +
                "    update($c);\n" +
                "end\n\n";

        KieSession ksession = getKieSession(runType, str);

        assertThat(ksession.fireAllRules(5)).isEqualTo(2);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testMultipleFieldUpdate(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R1 when\n" +
                "    $p : Person( name == \"Mario\" )\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ), setLikes(\"Cheese\") };\n" +
                "end\n" +
                "rule R2 when\n" +
                "    $p : Person( name == \"Mario\", likes == \"Cheese\" )\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        p.setLikes("Beer");
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(42);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testComplexSetterArgument(RUN_TYPE runType) {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                "rule R \n" +
                "when\n" +
                "    $p: Person(address.street == \"street1\")\n" +
                "then\n" +
                "    modify($p) { setLikes( String.valueOf(($p.getAddress().getStreet() + $p.getAddress().getCity()))) };\n" +
                "end";

        KieSession ksession = getKieSession(runType, str);

        Person me = new Person( "Mario", 40 );
        me.setAddress(new Address("street1", 2, "city1"));
        ksession.insert( me );

        assertThat(ksession.fireAllRules(10)).isEqualTo(1);

        assertThat(me.getLikes()).isEqualTo("street1city1");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void thisWithGetter(RUN_TYPE runType) {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                        "rule R \n" +
                        "when\n" +
                        "    $p: Person(this.getAddress() != null)\n" +
                        "then\n" +
                        "    modify($p) { setLikes(\"Cheese\") };\n" +
                        "end";

        KieSession ksession = getKieSession(runType, str);

        Person me = new Person( "Mario", 40 );
        me.setAddress(new Address("street1", 2, "city1"));
        ksession.insert( me );

        assertThat(ksession.fireAllRules(10)).as("should not loop").isEqualTo(1);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void nullSafeDereferencing(RUN_TYPE runType) {
        String str =
                "import " + Person.class.getCanonicalName() + ";" +
                        "rule R \n" +
                        "when\n" +
                        "    $p: Person(address!.street == \"street1\")\n" +
                        "then\n" +
                        "    modify($p) { setLikes(\"Cheese\") };\n" +
                        "end";

        KieSession ksession = getKieSession(runType, str);

        Person me = new Person( "Mario", 40 );
        me.setAddress(new Address("street1", 2, "city1"));
        ksession.insert( me );

        assertThat(ksession.fireAllRules(10)).as("should not loop").isEqualTo(1);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testNestedPropInRHS(RUN_TYPE runType) throws Exception {
        // Property Reactivity for "owner"
        final String str =
                "package org.drools.test;\n" +
                           "import " + Pet.class.getCanonicalName() + ";\n" +
                           "rule R1\n" +
                           "when \n" +
                           "  $pet : Pet(age == 3)\n" +
                           "then\n" +
                           "  modify ($pet) { getOwner().setLikes(\"Cookie\") };\n" +
                           "end\n" +
                           "rule R2\n" +
                           "when \n" +
                           "  Pet(owner.likes == \"Cookie\")\n" +
                           "then\n" +
                           "end";

        final KieSession ksession = getKieSession(runType, str);

        Pet pet = new Pet(Pet.PetType.cat);
        Person person = new Person("John");
        person.setLikes("Meat");
        pet.setOwner(person);
        pet.setAge(3);

        ksession.insert(pet);
        assertThat(ksession.fireAllRules()).isEqualTo(2);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testDeeplyNestedPropInRHS(RUN_TYPE runType) throws Exception {
        // Property Reactivity for "owner"
        final String str =
                "package org.drools.test;\n" +
                           "import " + Pet.class.getCanonicalName() + ";\n" +
                           "rule R1\n" +
                           "when \n" +
                           "  $pet : Pet(age == 3)\n" +
                           "then\n" +
                           "  modify ($pet) { getOwner().getAddress().setStreet(\"XYZ street\") };\n" +
                           "end\n" +
                           "rule R2\n" +
                           "when \n" +
                           "  Pet(owner.address.street == \"XYZ street\")\n" +
                           "then\n" +
                           "end";

        final KieSession ksession = getKieSession(runType, str);

        Pet pet = new Pet(Pet.PetType.cat);
        Person person = new Person("John");
        person.setAddress(new Address("ABC street"));
        pet.setOwner(person);
        pet.setAge(3);

        ksession.insert(pet);
        assertThat(ksession.fireAllRules()).isEqualTo(2);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testOutsideModifyBlockWithGetterAsArgument(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "\n" +
                           "rule R1 when\n" +
                           "    $p : Person( name == \"Mario\" )\n" +
                           "then\n" +
                           "    System.out.println(\"name = \" + $p.getName());\n" +
                           "    modify($p) { setAge(41) };\n" +
                           "end\n";

        final KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert(p);
        int fired = ksession.fireAllRules(10);

        assertThat(fired).isEqualTo(1);
        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testOutsideModifyBlockWithNonGetterAsArgument(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "\n" +
                           "rule R1 when\n" +
                           "    $p : Person( name == \"Mario\" )\n" +
                           "then\n" +
                           "    System.out.println(\"name.length = \" + $p.getName().length());\n" +
                           "    modify($p) { setAge(41) };\n" +
                           "end\n";

        final KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert(p);
        int fired = ksession.fireAllRules(10);

        assertThat(fired).isEqualTo(1);
        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testMultipleModifyBlocksWithNonGetterAsArgument(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "import " + Address.class.getCanonicalName() + ";\n" +

                           "\n" +
                           "rule R1 when\n" +
                           "    $p : Person( name == \"Mario\" )\n" +
                           "    $a : Address( street == \"Street\" )\n" +
                           "then\n" +
                           "    System.out.println(\"name.length = \" + $p.getName().length());\n" +
                           "    modify($p) { setAge(41) };\n" +
                           "    System.out.println(\"street.length = \" + $a.getStreet().length());\n" +
                           "    modify($a) { setNumber(20) };\n" +
                           "end\n";

        final KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert(p);

        Address a = new Address("Street", 10, "City");
        ksession.insert(a);

        int fired = ksession.fireAllRules(10);

        assertThat(fired).isEqualTo(1);
        assertThat(p.getAge()).isEqualTo(41);
        assertThat(a.getNumber()).isEqualTo(20);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUpdateWithGetterAsArgument(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "\n" +
                           "rule R1 when\n" +
                           "    $p : Person( name == \"Mario\" )\n" +
                           "then\n" +
                           "    System.out.println(\"name = \" + $p.getName());\n" +
                           "    $p.setAge(41);\n" +
                           "    update($p);\n" +
                           "end\n";

        final KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert(p);
        int fired = ksession.fireAllRules(10);

        assertThat(fired).isEqualTo(1);
        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUpdateWithNonGetterAsArgument(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "\n" +
                           "rule R1 when\n" +
                           "    $p : Person( name == \"Mario\" )\n" +
                           "then\n" +
                           "    System.out.println(\"name.length = \" + $p.getName().length());\n" +
                           "    $p.setAge(41);\n" +
                           "    update($p);\n" +
                           "end\n";

        final KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert(p);
        int fired = ksession.fireAllRules(10);

        assertThat(fired).isEqualTo(1);
        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUpdateWithNonGetterAsDeclaration(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "\n" +
                           "rule R1 when\n" +
                           "    $p : Person( name == \"Mario\" )\n" +
                           "then\n" +
                           "    int length = $p.getName().length();\n" +
                           "    System.out.println(\"length = \" + length);\n" +
                           "    $p.setAge(41);\n" +
                           "    update($p);\n" +
                           "end\n";

        final KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert(p);
        int fired = ksession.fireAllRules(10);

        assertThat(fired).isEqualTo(1);
        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUpdateWithNonGetterIntentinalLoop(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "\n" +
                           "rule R1 when\n" +
                           "    $p : Person( name == \"Mario\" )\n" +
                           "then\n" +
                           "    $p.getName().length();\n" +
                           "    $p.setAge(41);\n" +
                           "    update($p);\n" +
                           "end\n";

        final KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert(p);
        int fired = ksession.fireAllRules(10);

        // this is not likely an expected loop but standard-drl considers getter+otherMethod modifies the prop "name".
        // anyway, such "read" method is not written like this (= without assigning to a variable or as an argument of other method)
        // This test is to ensure the same behavior on stadard-drl and executable-model.
        assertThat(fired).isEqualTo(10);
        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPropertyReactivityOnBoundVariable(RUN_TYPE runType) {
        // RHDM-1387
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( $n : name, $n == \"Mario\" )\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(5);

        assertThat(p.getAge()).isEqualTo(41);
    }

    public static int dummy(int i) {
        return i;
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testWatchCallingExternalMethod(RUN_TYPE runType) {
        // DROOLS-5514
        final String str =
                "import static " + this.getClass().getCanonicalName() + ".dummy;\n" +
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( dummy(age) < 50 ) @watch(!*, age)\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(50);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testWatchCallingExternalMethod2(RUN_TYPE runType) {
        // DROOLS-5514
        final String str =
                "import static " + this.getClass().getCanonicalName() + ".dummy;\n" +
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( dummy(age) < 50 ) @watch(*)\n" +
                "then\n" +
                "    modify($p) { setName( $p.getName()+\"1\" ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getName()).isEqualTo("Mario111");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testWatchCallingExternalMethod3(RUN_TYPE runType) {
        // DROOLS-5514
        final String str =
                "import static " + this.getClass().getCanonicalName() + ".dummy;\n" +
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( dummy(age) < 50 ) @watch(!*, age)\n" +
                "then\n" +
                "    modify($p) { setName( $p.getName()+\"1\" ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getName()).isEqualTo("Mario1");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void test2PropertiesInOneExpression(RUN_TYPE runType) {
        // DROOLS-5677
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R1a\n" +
                "agenda-group \"group1\"\n" +
                "when\n" +
                "    $p : Person( age == 0 )\n" +
                "then\n" +
                "    modify($p) { setAge( 20 ) };\n" +
                "end\n" +
                "rule R1b \n" +
                "agenda-group \"group1\"\n" +
                "when\n" +
                "    $p : Person( salary == 0 )\n" +
                "then\n" +
                "    modify($p) { setSalary( 20 ) };\n" +
                "end\n" +
                "rule R2 \n" +
                "agenda-group \"group2\"\n" +
                "when\n" +
                "    $p : Person( age > salary )\n" +
                "then\n" +
                "    modify($p) { setSalary( 100 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("John", 0);
        p.setSalary(0);
        ksession.insert(p);
        ksession.getAgenda().getAgendaGroup("group1").setFocus();
        ksession.fireAllRules();
        ksession.getAgenda().getAgendaGroup("group2").setFocus();
        ksession.fireAllRules();

        assertThat(p.getSalary().intValue()).isEqualTo(20); // R2 should be cancelled
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void test3PropertiesInOneExpression(RUN_TYPE runType) {
        // DROOLS-5677
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R1a\n" +
                "agenda-group \"group1\"\n" +
                "when\n" +
                "    $p : Person( age == 0 )\n" +
                "then\n" +
                "    modify($p) { setAge( 20 ) };\n" +
                "end\n" +
                "rule R1b \n" +
                "agenda-group \"group1\"\n" +
                "when\n" +
                "    $p : Person( salary == 0 )\n" +
                "then\n" +
                "    modify($p) { setSalary( 10 ) };\n" +
                "end\n" +
                "rule R1c \n" +
                "agenda-group \"group1\"\n" +
                "when\n" +
                "    $p : Person( id == 0 )\n" +
                "then\n" +
                "    modify($p) { setId( 10 ) };\n" +
                "end\n" +
                "rule R2 \n" +
                "agenda-group \"group2\"\n" +
                "when\n" +
                "    $p : Person( age > salary + id )\n" +
                "then\n" +
                "    modify($p) { setSalary( 100 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("John", 0);
        p.setSalary(0);
        p.setId(0);
        ksession.insert(p);
        ksession.getAgenda().getAgendaGroup("group1").setFocus();
        ksession.fireAllRules();
        ksession.getAgenda().getAgendaGroup("group2").setFocus();
        ksession.fireAllRules();

        assertThat(p.getSalary().intValue()).isEqualTo(10); // R2 should be cancelled
    }

    public static class Fact {
        private int a;
        private int b;
        private String result;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    public static class AnotherFact {
        private int a;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }
    }

    public static String convertToString(int num) {
        if (num < 1000) {
            return "SMALL";
        }
        return "BIG";
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testExternalFunction(RUN_TYPE runType) {
        // BAPL-1773
        final String str =
                "import " + Fact.class.getCanonicalName() + ";\n" +
                "import static " + PropertyReactivityTest.class.getCanonicalName() + ".*;\n" +
                "\n" +
                "rule R1 when\n" +
                "    $fact: Fact(convertToString(a) == \"BIG\")\n" +
                "then\n" +
                "    modify($fact) { setResult(\"OK\") };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Fact fact = new Fact();
        fact.setA(99999);
        fact.setResult("NG");

        ksession.insert(fact);
        assertThat(ksession.fireAllRules(3)).isEqualTo(1);
        assertThat(fact.getResult()).isEqualTo("OK");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testExternalFunction2(RUN_TYPE runType) {
        // BAPL-1773
        final String str =
                "import " + Fact.class.getCanonicalName() + ";\n" +
                "import static " + PropertyReactivityTest.class.getCanonicalName() + ".*;\n" +
                "\n" +
                "rule R1 when\n" +
                "    $fact: Fact(convertToString(a) == \"BIG\")\n" +
                "then\n" +
                "    modify($fact) { setA(99999) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Fact fact = new Fact();
        fact.setA(99999);
        fact.setResult("NG");

        ksession.insert(fact);
        assertThat(ksession.fireAllRules(3)).isEqualTo(3);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void externalFunctionWithBindVariable_shouldNotCauseInfiniteLoop(RUN_TYPE runType) {
        // DROOLS-7372
        final String str = "import " + Fact.class.getCanonicalName() + ";\n" +
                           "import static " + PropertyReactivityTest.class.getCanonicalName() + ".*;\n" +
                           "\n" +
                           "rule R1 when\n" +
                           "    $fact: Fact($id : a == 99999, convertToString($id) == \"BIG\")\n" +
                           "then\n" +
                           "    modify($fact) { setResult(\"OK\") };\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);

        Fact bigString = new Fact();
        bigString.setA(99999);
        bigString.setResult("NG");

        ksession.insert(bigString);
        assertThat(ksession.fireAllRules(3))
                .as("'$id' is resolved as property 'a'. Hence, no class reactive, so the rule shouldn't loop")
                .isEqualTo(1);
        assertThat(bigString.getResult()).isEqualTo("OK");
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void externalFunctionWithBindVariableFromAnotherPatternOfSameType_shouldTriggerClassReactive(RUN_TYPE runType) {
        // DROOLS-7398
        final String str =
                "import " + Fact.class.getCanonicalName() + ";\n" +
                           "import static " + PropertyReactivityTest.class.getCanonicalName() + ".*;\n" +
                           "rule R when\n" +
                           "    $fact1 : Fact( $id : a )\n" +
                           "    $fact2 : Fact( convertToString($id) == \"BIG\" )\n" +
                           "then\n" +
                           "    modify($fact2) { setResult(\"OK\") };\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);

        Fact bigStringFact = new Fact();
        bigStringFact.setA(99999);
        bigStringFact.setResult("NG");
        ksession.insert(bigStringFact);
        int fired = ksession.fireAllRules(10); // intentional loop

        assertThat(fired).as("$id comes from a different pattern, so it triggers class reactivity, not property reactivity.")
                         .isEqualTo(10);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void multipleExternalFunctionsWithBindVariablesFromAnotherPatternOfSameType_shouldTriggerClassReactive(RUN_TYPE runType) {
        // DROOLS-7398
        final String str =
                "import " + Fact.class.getCanonicalName() + ";\n" +
                           "import static " + PropertyReactivityTest.class.getCanonicalName() + ".*;\n" +
                           "rule R when\n" +
                           "    $fact1 : Fact( $id_a : a, $id_b : b )\n" +
                           "    $fact2 : Fact( convertToString($id_a) == convertToString($id_b) )\n" +
                           "then\n" +
                           "    modify($fact2) { setResult(\"OK\") };\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);

        Fact bigStringFact = new Fact();
        bigStringFact.setA(99999);
        bigStringFact.setB(99999);
        bigStringFact.setResult("NG");
        ksession.insert(bigStringFact);
        int fired = ksession.fireAllRules(10); // intentional loop

        assertThat(fired).as("$id comes from a different pattern, so it triggers class reactivity, not property reactivity.")
                         .isEqualTo(10);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void externalFunctionWithBindVariableFromAnotherPatternOfDifferentType_shouldTriggerClassReactive(RUN_TYPE runType) {
        // DROOLS-7390
        final String str =
                "import " + Fact.class.getCanonicalName() + ";\n" +
                        "import " + AnotherFact.class.getCanonicalName() + ";\n" +
                           "import static " + PropertyReactivityTest.class.getCanonicalName() + ".*;\n" +
                           "rule R when\n" +
                           "    $fact1 : AnotherFact( $id : a )\n" +
                           "    $fact2 : Fact( convertToString($id) == \"BIG\" )\n" +
                           "then\n" +
                           "    modify($fact2) { setResult(\"OK\") };\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);

        AnotherFact bigStringAnotherFact = new AnotherFact();
        bigStringAnotherFact.setA(99999);
        ksession.insert(bigStringAnotherFact);

        Fact smallStringFact = new Fact();
        smallStringFact.setA(1);
        smallStringFact.setResult("NG");
        ksession.insert(smallStringFact);
        int fired = ksession.fireAllRules(10); // intentional loop

        assertThat(fired).as("$id comes from a different pattern, so it triggers class reactivity, not property reactivity.")
                         .isEqualTo(10);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUnwatch(RUN_TYPE runType) {
        // RHDM-1553
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( age < 50 ) @watch(!*)\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge() + 1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUnwatchWithFieldBinding(RUN_TYPE runType) {
        // RHDM-1553
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( $age : age < 50 ) @watch(!*)\n" +
                "then\n" +
                "    modify($p) { setAge( $age + 1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUnwatchWithFieldBindingAndMvel(RUN_TYPE runType) {
        // RHDM-1553
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R dialect \"mvel\" when\n" +
                "    $p : Person( $age : age < 50 ) @watch(!*)\n" +
                "then\n" +
                "    modify($p) { age = $age + 1 };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUnwatchWithWatchedField(RUN_TYPE runType) {
        // RHDM-1553
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( name == \"Mario\" ) @watch(!*, age)\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge() + 1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getAge()).isEqualTo(43);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testNoConstraint(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( )\n" +
                "then\n" +
                "    modify($p) { setAge( $p.getAge()+1 ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(10);

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testNoConstraintWithUpdate(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( )\n" +
                "then\n" +
                "    $p.setAge( $p.getAge()+1 );\n" +
                "    update($p);\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(10);

        assertThat(p.getAge()).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testModifiesAnnotation(RUN_TYPE runType) {
        final String str =
                "import " + Light.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $l : Light( name == \"Alert\")\n" +
                "then\n" +
                "    modify($l) { turnOn() };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Light l = new Light("Alert");
        ksession.insert( l );
        int fired = ksession.fireAllRules(10);

        assertThat(fired).isEqualTo(1);
    }

    public static class Light {
        private boolean on;
        private String name;

        public Light(String name) {
            this.name = name;
        }

        public boolean isOn() {
            return on;
        }

        public void setOn(boolean on) {
            this.on = on;
        }

        @Modifies( { "on" } )
        public void turnOn() {
            setOn(true);
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testSettersInAndOutModifyBlock(RUN_TYPE runType) {
        // RHDM-1552
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( age < 50 )\n" +
                "then\n" +
                "    $p.setAge( $p.getAge() + 1 );\n" +
                "    modify($p) { setName( \"Mario\" ) };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getAge()).isEqualTo(43);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testSettersInAndOutModifyBlockMvel(RUN_TYPE runType) {
        // RHDM-1552
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R dialect \"mvel\" when\n" +
                "    $p : Person( age < 50 )\n" +
                "then\n" +
                "    $p.age = $p.age + 1;\n" +
                "    modify($p) { name = \"Mario\" };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getAge()).isEqualTo(43);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testMvelModifyBlockWithComma(RUN_TYPE runType) {
        // RHDM-1552
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R dialect \"mvel\" when\n" +
                "    $p : Person( age < 50 )\n" +
                "then\n" +
                "    modify($p) { setName(\"Mario\"), age = $p.age + 1 };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getAge()).isEqualTo(43);
    }

    public static class AssessmentContext {

        private Deque<StackFrame> stackFrames;

        public AssessmentContext() {
            stackFrames = new ArrayDeque<StackFrame>(8);
            stackFrames.push(new StackFrame());
        }

        public Long getTopQuestionGroupId() {
            return stackFrames.peek().questionGroupId;
        }

        public int getTopPhase() {
            return stackFrames.peek().phase;
        }

        public void setTopPhase(int i) {
            stackFrames.peek().phase = i;
        }

        public void pushStackFrame(Long id) {
            StackFrame sf = new StackFrame();
            sf.questionGroupId = id;
            sf.phase = 0;
            stackFrames.push(sf);
        }

        public void popStackFrame() {
            if (stackFrames.size() > 1) {
                stackFrames.pop();
            }
        }

        private static class StackFrame {
            private Long questionGroupId = null;
            private int phase = 0;
        }
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUpdateNonPropertyInMvel(RUN_TYPE runType) {
        // DROOLS-6096
        final String str =
                "import " + AssessmentContext.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R1 \n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    $ac : AssessmentContext(topQuestionGroupId == 51795 , topPhase == 3)\n" +
                "  then\n" +
                "  $ac.popStackFrame();\n" +
                "  update($ac);\n" +
                "end\n" +
                "\n" +
                "rule R2 \n" +
                "  dialect \"mvel\"\n" +
                "  when\n" +
                "    $ac : AssessmentContext(topQuestionGroupId == null, topPhase == 0)\n" +
                "  then\n" +
                "    $ac.setTopPhase(2);\n" +
                "    $ac.pushStackFrame(223L);\n" +
                "    update($ac);\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        AssessmentContext ac1 = new AssessmentContext();
        ac1.pushStackFrame(null);
        ac1.pushStackFrame(51795L);
        ac1.setTopPhase(3);
        ksession.insert( ac1 );

        assertThat(ksession.fireAllRules(2)).isEqualTo(2);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPropertyReactivityWithPublicField(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "rule R1 when\n" +
                           "    $p : Person()\n" +
                           "then\n" +
                           "    modify($p) { publicAge = 41 };\n" +
                           "end\n" +
                           "rule R2 when\n" +
                           "    $p : Person(name == \"John\")\n" +
                           "then\n" +
                           "end\n" +
                           "rule R3 when\n" +
                           "    $p : Person(publicAge == 41)\n" +
                           "then\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("John");
        p.publicAge = 40;
        ksession.insert(p);
        int fired = ksession.fireAllRules(10);

        assertThat(fired).isEqualTo(3);
        assertThat(p.publicAge).isEqualTo(41);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testUnknownPropertyNameInWatch(RUN_TYPE runType) throws Exception {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "global java.util.List result;\n" +
                           "rule R1 when\n" +
                           "    $p : Person( name == \"John\" ) @watch( ageX )\n" +
                           "then\n" +
                           "    result.add($p.getName());\n" +
                           "    modify($p) { setLikes(\"stilton\") }\n" +
                           "end\n" +
                           "rule R2 when\n" +
                           "    $p : Person(likes == \"stilton\")\n" +
                           "then\n" +
                           "    modify($p) { setAge(20) }\n" +
                           "end\n";

        KieBuilder kbuilder = createKieBuilder(runType, str);
        assertThat(kbuilder.getResults().hasMessages(Level.ERROR)).isTrue();
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testSetterWithoutGetter(RUN_TYPE runType) {
        // DROOLS-6523
        final String str =
                "import " + ClassWithValue.class.getCanonicalName() + ";\n" +
                "rule R1 no-loop when\n" +
                "        $cwv : ClassWithValue()\n" +
                "    then\n" +
                "        $cwv.setDoubleValue(ClassWithValue.DOUBLE_VALUE);\n" +
                "        update($cwv);\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        ClassWithValue cwv = new ClassWithValue();
        ksession.insert(cwv);
        int fired = ksession.fireAllRules(10);

        assertThat(fired).isEqualTo(1);
        assertThat(cwv.getDoubleValues().size()).isEqualTo(1);
    }

    public class ClassWithValue {

        public static final double DOUBLE_VALUE = 5.5;

        private List<Double> doubleValues = new ArrayList<>();

        public List<Double> getDoubleValues() {
            return doubleValues;
        }

        public void setDoubleValue(double doubleValue) {

            this.doubleValues.clear();
            this.doubleValues.add(doubleValue);
        }

        public void addDoubleValue(double doubleValue) {
            this.doubleValues.add(doubleValue);
        }
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPropertyReactivityOn2Properties(RUN_TYPE runType) {
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( name == \"Mario\" )\n" +
                "then\n" +
                "    modify($p) { " +
                "        setAge( $p.getAge()+1 ), " +
                "        setId( 1 ) " +
                "    };\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(41);
        assertThat(p.getId()).isEqualTo(1);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPropertyReactivityOn2PropertiesWithWrongSeparator(RUN_TYPE runType) {
        // DROOLS-6480
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( name == \"Mario\" )\n" +
                "then\n" +
                "    modify($p) { " +
                "        setAge( $p.getAge()+1 ); " +
                "        setId( 1 ) " +
                "    };\n" +
                "end\n";

        KieBuilder kbuilder = createKieBuilder(runType, str);
        assertThat(kbuilder.getResults().hasMessages(Level.ERROR)).isTrue();
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testMvelModifyAfterSingleQuote(RUN_TYPE runType) {
        // DROOLS-6542
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "dialect \"mvel\"\n" +
                "\n" +
                "rule R1 when\n" +
                "    $p : Person( name == \"Mario\" )\n" +
                "then\n" +
                "    System.out.println(\"Mario isn't young\");\n" +
                "    modify($p) { age = $p.age+1 };\n" +
                "end\n" +
                "rule R2 when\n" +
                "    Person( age == 41 )\n" +
                "then\n" +
                "    insert(\"ok\");\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules();

        assertThat(p.getAge()).isEqualTo(41);
        assertThat(ksession.getObjects((Object object) -> object.equals("ok")).size()).isEqualTo(1);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testOnlyFirstLetterIsUpperCaseProperty(RUN_TYPE runType) {
        // See JavaBeans 1.01 spec : 8.8 Capitalization of inferred names
        // “FooBah” becomes “fooBah”
        testVariousCasePropFact(runType, "modify($f) { MyTarget = \"123\" };", "R1", "R2"); // Actually, this modifies "myTarget" property (backed by private "MyTarget" field). This shouldn't react R1
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testTwoFirstLettersAreUpperCaseProperty(RUN_TYPE runType) {
        // See JavaBeans 1.01 spec : 8.8 Capitalization of inferred names
        // “URL” becomes “URL”
        testVariousCasePropFact(runType, "modify($f) { URL = \"123\" };", "R1", "R2"); // This shouldn't react R1
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testFirstLetterIsMultibyteProperty(RUN_TYPE runType) {
        // Multibyte is not mentioned in JavaBeans spec
        testVariousCasePropFact(runType, "modify($f) { 名前 = \"123\" };", "R1", "R2"); // This shouldn't react R1
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testOnlyFirstLetterIsUpperCaseAndMultibyteProperty(RUN_TYPE runType) {
        // Multibyte is not mentioned in JavaBeans spec
        testVariousCasePropFact(runType, "modify($f) { My名前 = \"123\" };", "R1", "R2"); // Actually, this modifies "my名前" property (backed by private "My名前" field). This shouldn't react R1
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testOnlyFirstLetterIsUpperCasePublicFieldProperty(RUN_TYPE runType) {
        testVariousCasePropFact(runType, "modify($f) { MyPublicTarget = \"123\" };", "R1", "R2"); // this modifies "MyPublicTarget" public field directly. This shouldn't react R1
    }

    private void testVariousCasePropFact(RUN_TYPE runType, String modifyStatement, String... expectedResults) {
        final String str =
                "import " + VariousCasePropFact.class.getCanonicalName() + ";\n" +
                           "dialect \"mvel\"\n" +
                           "global java.util.List results;\n" +
                           "rule R1\n" +
                           "salience 100\n" +
                           "when\n" +
                           "    $f : VariousCasePropFact( value == \"A\" )\n" +
                           "then\n" +
                           "    results.add(\"R1\")\n" +
                           "end\n" +
                           "rule R2\n" +
                           "no-loop\n" +
                           "when\n" +
                           "    $f : VariousCasePropFact( value == \"A\" )\n" +
                           "then\n" +
                           "    results.add(\"R2\");\n" +
                           modifyStatement + "\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);
        List<String> results = new ArrayList<>();
        ksession.setGlobal("results", results);

        VariousCasePropFact fact = new VariousCasePropFact();
        fact.setValue("A");
        ksession.insert(fact);
        ksession.fireAllRules();

        assertThat(results).containsExactly(expectedResults);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void bindOnlyPropertyReacts(RUN_TYPE runType) {
        // DROOLS-7214
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "dialect \"mvel\"\n" +
                           "rule R when\n" +
                           "    $p : Person( name == \"Mario\", $age : age )\n" +
                           "then\n" +
                           "    modify($p) { age = $age + 1 };\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert(p);
        int fired = ksession.fireAllRules(10); // intentional loop

        assertThat(fired).isEqualTo(10);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void bindOnlyMapPropertyReacts(RUN_TYPE runType) {
        // DROOLS-7214
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "dialect \"mvel\"\n" +
                           "rule R when\n" +
                           "    $p : Person( name == \"Mario\", $map : itemsString )\n" +
                           "then\n" +
                           "    $p.itemsString[\"B\"] = \"itemB\";\n" +
                           "    modify($p) { itemsString = $p.itemsString };\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        p.getItemsString().put("A", "itemA");
        ksession.insert(p);
        int fired = ksession.fireAllRules(10); // intentional loop

        assertThat(fired).isEqualTo(10);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void bindOnlyMapPropertyWithAccessOperatorReacts(RUN_TYPE runType) {
        // DROOLS-7214
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "dialect \"mvel\"\n" +
                           "rule R when\n" +
                           "    $p : Person( name == \"Mario\", $mapDataA : itemsString[\"A\"] )\n" +
                           "then\n" +
                           "    $p.itemsString[\"B\"] = \"itemB\";\n" +
                           "    modify($p) { itemsString = $p.itemsString };\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        p.getItemsString().put("A", "itemA");
        ksession.insert(p);
        int fired = ksession.fireAllRules(10); // intentional loop

        assertThat(fired).isEqualTo(10);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void bindOnlyListPropertyWithAccessOperatorReacts(RUN_TYPE runType) {
        // DROOLS-7214
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                           "import " + Address.class.getCanonicalName() + ";\n" +
                           "dialect \"mvel\"\n" +
                           "rule R when\n" +
                           "    $p : Person( name == \"Mario\", $listData0 : addresses[0] )\n" +
                           "then\n" +
                           "    $p.addresses.add(new Address(\"C\"));\n" +
                           "    modify($p) { addresses = $p.addresses };\n" +
                           "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        p.getAddresses().add(new Address("A"));
        p.getAddresses().add(new Address("B"));
        ksession.insert(p);
        int fired = ksession.fireAllRules(10); // intentional loop

        assertThat(fired).isEqualTo(10);
    }

    @ParameterizedTest
	@MethodSource("parameters")
    public void testPropertyReactivityWithRedundantVariableDeclaration(RUN_TYPE runType) {
        // KIE-DROOLS-5943
        final String str =
                "import " + Person.class.getCanonicalName() + ";\n" +
                "\n" +
                "rule R when\n" +
                "    $p : Person( $p.name == \"Mario\" )\n" +
                "then\n" +
                "    $p.setAge( $p.getAge()+1 );\n" +
                "    update($p);\n" +
                "end\n";

        KieSession ksession = getKieSession(runType, str);

        Person p = new Person("Mario", 40);
        ksession.insert( p );
        ksession.fireAllRules(3);

        assertThat(p.getAge()).isEqualTo(41);
    }
}
