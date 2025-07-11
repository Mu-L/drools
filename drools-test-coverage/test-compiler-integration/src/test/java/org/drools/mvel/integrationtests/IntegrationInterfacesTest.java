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
package org.drools.mvel.integrationtests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.drools.mvel.compiler.Cheese;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieBaseUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieBase;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.KieSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IntegrationInterfacesTest {

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseCloudConfigurations(true).stream();
    }

    private KieBase getKnowledgeBase(KieBaseTestConfiguration kieBaseTestConfiguration, final String resourceName) {
        return KieBaseUtil.getKieBaseFromClasspathResources(getClass(), kieBaseTestConfiguration, resourceName);
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testGlobals(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final KieBase kbase = getKnowledgeBase(kieBaseTestConfiguration, "globals_rule_test.drl" );
        KieSession ksession = kbase.newKieSession();

        final List<Object> list = mock( List.class );
        ksession.setGlobal( "list",
                            list );
        ksession.setGlobal( "string",
                            "stilton" );

        final Cheese stilton = new Cheese( "stilton",
                                           5 );
        ksession.insert( stilton );

        ksession = SerializationHelper.getSerialisedStatefulKnowledgeSession( ksession,
                                                                              true );
        ksession.fireAllRules();

        verify( list,
                times( 1 ) ).add( new Integer( 5 ) );
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testGlobals2(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        final KieBase kbase = getKnowledgeBase(kieBaseTestConfiguration, "test_globalsAsConstraints.drl" );
        KieSession ksession = kbase.newKieSession();

        final List<Object> results = mock( List.class );
        ksession.setGlobal( "results",
                            results );

        final List<String> cheeseTypes = mock( List.class );
        ksession.setGlobal( "cheeseTypes",
                            cheeseTypes );

        when( cheeseTypes.contains( "stilton" ) ).thenReturn( Boolean.TRUE );
        when( cheeseTypes.contains( "muzzarela" ) ).thenReturn( Boolean.TRUE );

        final Cheese stilton = new Cheese( "stilton",
                                           5 );
        ksession.insert( stilton );
        ksession = SerializationHelper.getSerialisedStatefulKnowledgeSession( ksession,
                                                                              true );
        ksession.fireAllRules();

        verify( results,
                times( 1 ) ).add( "memberOf" );

        final Cheese brie = new Cheese( "brie",
                                        5 );
        ksession.insert( brie );
        ksession = SerializationHelper.getSerialisedStatefulKnowledgeSession( ksession,
                                                                              true );
        ksession.fireAllRules();

        verify( results,
                times( 1 ) ).add( "not memberOf" );
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testGlobalMerge(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        // from JBRULES-1512
        String rule1 = "package com.sample\n" + 
                       "rule \"rule 1\"\n" + 
                       "    salience 10\n" + 
                       "    when\n" + 
                       "        l : java.util.List()\n" + 
                       "    then\n" + 
                       "        l.add( \"rule 1 executed\" );\n" + 
                       "end\n";
        String rule2 = "package com.sample\n" + 
                       "global String str;\n" + 
                       "rule \"rule 2\"\n" + 
                       "    when\n" + 
                       "        l : java.util.List()\n" + 
                       "    then\n" + 
                       "        l.add( \"rule 2 executed \" + str);\n" + 
                       "end\n";

        KieBase kbase = KieBaseUtil.getKieBaseFromKieModuleFromDrl("test", kieBaseTestConfiguration, rule1, rule2);
        KieSession ksession = kbase.newKieSession();

        ksession.setGlobal( "str",
                            "boo" );
        List<String> list = new ArrayList<String>();
        ksession.insert( list );
        ksession.fireAllRules();
        assertThat(list.get(0)).isEqualTo("rule 1 executed");
        assertThat(list.get(1)).isEqualTo("rule 2 executed boo");
    }
    
    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testChannels(KieBaseTestConfiguration kieBaseTestConfiguration) throws IOException, ClassNotFoundException {
        KieBase kbase = getKnowledgeBase(kieBaseTestConfiguration, "test_Channels.drl" );
        KieSession ksession = kbase.newKieSession();
        
        Channel someChannel = mock( Channel.class );
        ksession.registerChannel( "someChannel", someChannel );
        
        ksession.insert( new Cheese( "brie", 30 ) );
        ksession.insert( new Cheese( "stilton", 5 ) );
        
        ksession.fireAllRules();
        
        verify( someChannel ).send( "brie" );
        verify( someChannel,  never() ).send( "stilton" );
    }

}
