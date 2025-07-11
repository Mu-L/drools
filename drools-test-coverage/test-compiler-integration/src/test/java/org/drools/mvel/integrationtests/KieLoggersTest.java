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

import java.io.File;
import java.util.stream.Stream;

import org.drools.mvel.compiler.Message;
import org.drools.testcoverage.common.util.KieBaseTestConfiguration;
import org.drools.testcoverage.common.util.KieUtil;
import org.drools.testcoverage.common.util.TestParametersUtil2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.io.Resource;
import org.kie.api.logger.KieRuntimeLogger;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.io.ResourceFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class KieLoggersTest {

    public static Stream<KieBaseTestConfiguration> parameters() {
        return TestParametersUtil2.getKieBaseCloudConfigurations(true).stream();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testKieConsoleLogger(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        String drl = "package org.drools.integrationtests\n" + 
                "import org.drools.mvel.compiler.Message;\n" +
                "rule \"Hello World\"\n" + 
                "    when\n" + 
                "        m : Message( myMessage : message )\n" + 
                "    then\n" + 
                "end";
        // get the resource
        Resource dt = ResourceFactory.newByteArrayResource( drl.getBytes() ).setTargetPath( "org/drools/integrationtests/hello.drl" );
        
        // create the builder
        KieSession ksession = getKieSession(kieBaseTestConfiguration, dt );
        KieRuntimeLogger logger = KieServices.Factory.get().getLoggers().newConsoleLogger( ksession );

        ksession.insert( new Message("Hello World") );
        int fired = ksession.fireAllRules();
        assertThat(fired).isEqualTo(1); 
        
        logger.close();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testDeclarativeKieConsoleLogger(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        String drl = "package org.drools.integrationtests\n" +
                     "import org.drools.mvel.compiler.Message;\n" +
                     "rule \"Hello World\"\n" +
                     "    when\n" +
                     "        m : Message( myMessage : message )\n" +
                     "    then\n" +
                     "end";

        KieServices ks = KieServices.Factory.get();
        KieModuleModel kproj = ks.newKieModuleModel();

        kproj.newKieBaseModel("KBase1").newKieSessionModel("KSession1").setConsoleLogger("logger");

        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.writeKModuleXML(kproj.toXML());
        kfs.write("src/main/resources/KBase1/rule.drl", drl);

        final KieBuilder kieBuilder = KieUtil.getKieBuilderFromKieFileSystem(kieBaseTestConfiguration, kfs, false);
        KieModule kieModule = kieBuilder.getKieModule();
        KieContainer kieContainer = ks.newKieContainer(kieModule.getReleaseId());

        KieSession ksession = kieContainer.newKieSession("KSession1");
        ksession.insert( new Message("Hello World") );
        int fired = ksession.fireAllRules();
        assertThat(fired).isEqualTo(1);

        KieRuntimeLogger logger = ksession.getLogger();
        assertThat(logger).isNotNull();
        logger.close();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testKieConsoleLoggerStateless(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        String drl = "package org.drools.integrationtests\n" + 
                "import org.drools.mvel.compiler.Message;\n" +
                "rule \"Hello World\"\n" + 
                "    when\n" + 
                "        m : Message( myMessage : message )\n" + 
                "    then\n" + 
                "end";
        // get the resource
        Resource dt = ResourceFactory.newByteArrayResource( drl.getBytes() ).setTargetPath("org/drools/integrationtests/hello.drl");
        
        // create the builder
        StatelessKieSession ksession = getStatelessKieSession(kieBaseTestConfiguration, dt);
        KieRuntimeLogger logger = KieServices.Factory.get().getLoggers().newConsoleLogger( ksession );

        AgendaEventListener ael = mock( AgendaEventListener.class );
        ksession.addEventListener( ael );
        
        ksession.execute( new Message("Hello World") );
        
        verify( ael ).afterMatchFired( any(AfterMatchFiredEvent.class) );
        
        logger.close();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testDeclarativeKieConsoleLoggerStateless(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        String drl = "package org.drools.integrationtests\n" +
                     "import org.drools.mvel.compiler.Message;\n" +
                     "rule \"Hello World\"\n" +
                     "    when\n" +
                     "        m : Message( myMessage : message )\n" +
                     "    then\n" +
                     "end";

        KieServices ks = KieServices.Factory.get();
        KieModuleModel kproj = ks.newKieModuleModel();

        kproj.newKieBaseModel("KBase1")
             .newKieSessionModel("KSession1")
             .setType(KieSessionModel.KieSessionType.STATELESS)
             .setConsoleLogger("logger");

        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.writeKModuleXML(kproj.toXML());
        kfs.write("src/main/resources/KBase1/rule.drl", drl);

        final KieBuilder kieBuilder = KieUtil.getKieBuilderFromKieFileSystem(kieBaseTestConfiguration, kfs, false);
        KieModule kieModule = kieBuilder.getKieModule();
        KieContainer kieContainer = ks.newKieContainer(kieModule.getReleaseId());

        StatelessKieSession ksession = kieContainer.newStatelessKieSession("KSession1");
        ksession.execute( new Message("Hello World") );

        KieRuntimeLogger logger = ksession.getLogger();
        assertThat(logger).isNotNull();
        logger.close();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testKieFileLogger(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        String drl = "package org.drools.integrationtests\n" +
                     "import org.drools.mvel.compiler.Message;\n" +
                     "rule \"Hello World\"\n" +
                     "    when\n" +
                     "        m : Message( myMessage : message )\n" +
                     "    then\n" +
                     "end";
        // get the resource
        Resource dt = ResourceFactory.newByteArrayResource(drl.getBytes()).setTargetPath( "org/drools/integrationtests/hello.drl" );

        // create the builder
        KieSession ksession = getKieSession(kieBaseTestConfiguration, dt);

        String fileName = "target/testKieFileLogger";
        File file = new File(fileName + ".log");
        if( file.exists() ) {
            file.delete();
        }
        KieRuntimeLogger logger = KieServices.Factory.get().getLoggers().newFileLogger( ksession,
                                                                                        fileName );

        ksession.insert(new Message("Hello World"));
        int fired = ksession.fireAllRules();
        assertThat(fired).isEqualTo(1);

        logger.close();

        file = new File( fileName + ".log" );
        assertThat(file.exists()).isTrue();
        assertThat(file.length() > 0).isTrue();
        file.delete();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testKieFileLoggerWithImmediateFlushing(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        // DROOLS-991
        String drl = "package org.drools.integrationtests\n" +
                     "import org.drools.mvel.compiler.Message;\n" +
                     "rule \"Hello World\"\n" +
                     "    when\n" +
                     "        m : Message( myMessage : message )\n" +
                     "    then\n" +
                     "end";
        // get the resource
        Resource dt = ResourceFactory.newByteArrayResource(drl.getBytes()).setTargetPath( "org/drools/integrationtests/hello.drl" );

        // create the builder
        KieSession ksession = getKieSession(kieBaseTestConfiguration, dt);

        String fileName = "target/testKieFileLogger";
        File file = new File(fileName + ".log");
        if( file.exists() ) {
            file.delete();
        }

        // Setting maxEventsInMemory to 0 makes all events to be immediately flushed to the file
        KieRuntimeLogger logger = KieServices.Factory.get().getLoggers().newFileLogger( ksession, fileName, 0 );

        ksession.insert(new Message("Hello World"));
        int fired = ksession.fireAllRules();
        assertThat(fired).isEqualTo(1);

        // check that the file has been populated before closing it
        file = new File( fileName + ".log" );
        assertThat(file.exists()).isTrue();
        assertThat(file.length() > 0).isTrue();

        logger.close();
        file.delete();
    }

    @ParameterizedTest(name = "KieBase type={0}")
    @MethodSource("parameters")
    public void testDeclarativeKieFileLogger(KieBaseTestConfiguration kieBaseTestConfiguration) throws Exception {
        String drl = "package org.drools.integrationtests\n" + 
                "import org.drools.mvel.compiler.Message;\n" +
                "rule \"Hello World\"\n" + 
                "    when\n" + 
                "        m : Message( myMessage : message )\n" + 
                "    then\n" + 
                "end";

        String fileName = "target/testKieFileLogger";
        File file = new File(fileName + ".log");
        if( file.exists() ) {
            file.delete();
        }

        KieServices ks = KieServices.Factory.get();
        KieModuleModel kproj = ks.newKieModuleModel();

        kproj.newKieBaseModel("KBase1").newKieSessionModel("KSession1").setFileLogger( fileName );

        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.writeKModuleXML(kproj.toXML());
        kfs.write("src/main/resources/KBase1/rule.drl", drl);

        final KieBuilder kieBuilder = KieUtil.getKieBuilderFromKieFileSystem(kieBaseTestConfiguration, kfs, false);
        KieModule kieModule = kieBuilder.getKieModule();
        KieContainer kieContainer = ks.newKieContainer(kieModule.getReleaseId());

        KieSession ksession = kieContainer.newKieSession("KSession1");
        
        ksession.insert( new Message("Hello World") );
        int fired = ksession.fireAllRules();
        assertThat(fired).isEqualTo(1);

        // disposing the ksession also flushes and closes the logger
        ksession.dispose();

        file = new File( fileName+".log" );
        assertThat(file.exists()).isTrue();
        file.delete();
    }

    private KieSession getKieSession(KieBaseTestConfiguration kieBaseTestConfiguration, Resource dt) {
        KieServices ks = populateKieFileSystem(kieBaseTestConfiguration, dt );

        // get the session
        KieSession ksession = ks.newKieContainer(ks.getRepository().getDefaultReleaseId()).newKieSession();
        return ksession;
    }

    private StatelessKieSession getStatelessKieSession(KieBaseTestConfiguration kieBaseTestConfiguration, Resource dt) {
        KieServices ks = populateKieFileSystem(kieBaseTestConfiguration, dt );

        // get the session
        StatelessKieSession ksession = ks.newKieContainer(ks.getRepository().getDefaultReleaseId()).newStatelessKieSession();
        return ksession;
    }

    private KieServices populateKieFileSystem(KieBaseTestConfiguration kieBaseTestConfiguration, Resource dt) {
        KieServices ks = KieServices.Factory.get();

        KieFileSystem kfs = ks.newKieFileSystem().write( dt );
        KieBuilder kb = KieUtil.getKieBuilderFromKieFileSystem(kieBaseTestConfiguration, kfs, false);
        assertThat(kb.getResults().getMessages().isEmpty()).isTrue();
        return ks;
    }
}
