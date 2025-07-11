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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.drools.model.codegen.execmodel.domain.Person;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;

public class ConstraintTest extends BaseModelTest {

    private static final String RULE_STRING = "package constraintexpression\n" +
            "\n" +
            "import " + Person.class.getCanonicalName() + "\n" +
            "import java.util.List; \n" +
            "import java.math.BigDecimal; \n" +
            "global List<BigDecimal> bigDecimalListGlobal; \n" +
            "rule \"r1\"\n" +
            "when \n" +
            "    $p : Person($amount: (money == null ? BigDecimal.valueOf(100.0) : money))\n" +
            "then \n" +
            "    System.out.println($amount); \n" +
            "    System.out.println($p); \n" +
            "    bigDecimalListGlobal.add($amount); \n " +
            "end \n";

    private KieSession ksession;

    private List<BigDecimal> bigDecimalListGlobal;

    @BeforeEach
    public void setup() {
        bigDecimalListGlobal = new ArrayList<>();
    }
    
    @AfterEach
    public void tearDown() {
    	if (ksession != null) {
    		ksession.dispose();
    	}
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testConstraintWithMoney(RUN_TYPE runType) {
        ksession = getKieSession(runType, RULE_STRING);
		ksession.setGlobal("bigDecimalListGlobal", bigDecimalListGlobal);
		BigDecimal money = BigDecimal.valueOf(34.45);
		Person person = new Person("", money);
		ksession.insert(person);
		int rulesFired = ksession.fireAllRules();
		assertThat(rulesFired).isEqualTo(1);
		assertThat(bigDecimalListGlobal).isNotEmpty().containsExactly(money);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testConstraintWithoutMoney(RUN_TYPE runType) {
        ksession = getKieSession(runType, RULE_STRING);
		ksession.setGlobal("bigDecimalListGlobal", bigDecimalListGlobal);
        Person person = new Person();
		ksession.insert(person);
		int rulesFired = ksession.fireAllRules();
		assertThat(rulesFired).isEqualTo(1);
		assertThat(bigDecimalListGlobal).isNotEmpty().containsExactly(BigDecimal.valueOf(100.0));
    }
}
