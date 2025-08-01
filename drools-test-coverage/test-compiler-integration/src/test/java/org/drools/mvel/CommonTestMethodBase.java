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
package org.drools.mvel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.function.Predicate;

import org.drools.compiler.builder.impl.KnowledgeBuilderConfigurationImpl;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.drl.ast.descr.PackageDescr;
import org.drools.core.common.InternalAgenda;
import org.drools.kiesession.rulebase.InternalKnowledgeBase;
import org.drools.core.impl.RuleBaseFactory;
import org.drools.core.integrationtests.SerializationHelper;
import org.drools.kiesession.rulebase.KnowledgeBaseFactory;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.definition.KiePackage;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.marshalling.Marshaller;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.api.runtime.conf.KieSessionOption;
import org.kie.internal.builder.InternalKieBuilder;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderConfiguration;
import org.kie.internal.builder.KnowledgeBuilderError;
import org.kie.internal.builder.KnowledgeBuilderErrors;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.marshalling.MarshallerFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * This contains methods common to many of the tests in drools-compiler. </p>
 * The {@link #createKnowledgeSession(KnowledgeBase)} method has been made
 * common so that tests in drools-compiler can be reused (with persistence) in
 * drools-persistence-jpa.
 */
public class CommonTestMethodBase {

    private static Logger logger = LoggerFactory.getLogger(CommonTestMethodBase.class);

    protected KieSession createKieSession(KieBase kbase) {
        return kbase.newKieSession();
    }

    protected KieSession createKieSession(KieBase kbase, KieSessionOption option) {
        KieSessionConfiguration ksconf = RuleBaseFactory.newKnowledgeSessionConfiguration();
        ksconf.setOption(option);
        return kbase.newKieSession(ksconf, null);
    }

    protected KieSession createKieSession(KieBase kbase, KieSessionConfiguration sessionConfiguration, Environment env) {
        return kbase.newKieSession(sessionConfiguration, env);
    }
    
    protected KieSession createKnowledgeSession(KieBase kbase) {
        return kbase.newKieSession();
    }

    protected KieSession createKnowledgeSession(KieBase kbase, KieSessionOption option) {
        KieSessionConfiguration ksconf = RuleBaseFactory.newKnowledgeSessionConfiguration();
        ksconf.setOption(option);
        return kbase.newKieSession(ksconf, null);
    }

    protected KieSession createKnowledgeSession(KieBase kbase, KieSessionConfiguration ksconf) {
        return kbase.newKieSession(ksconf, null);
    }

    protected KieSession createKnowledgeSession(KieBase kbase, KieSessionConfiguration ksconf, Environment env) {
        return kbase.newKieSession(ksconf, env);
    }

    protected StatelessKieSession createStatelessKnowledgeSession(KieBase kbase) {
        return kbase.newStatelessKieSession();
    }

    protected KieBase loadKnowledgeBaseFromString(String... drlContentStrings) {
        return loadKnowledgeBaseFromString(null, null, drlContentStrings);
    }

    protected KieBase loadKnowledgeBaseFromString( KnowledgeBuilderConfiguration config, KieBaseConfiguration kBaseConfig, String... drlContentStrings) {
        KnowledgeBuilder kbuilder = config == null ? KnowledgeBuilderFactory.newKnowledgeBuilder() : KnowledgeBuilderFactory.newKnowledgeBuilder(config);
        for (String drlContentString : drlContentStrings) {
            kbuilder.add(ResourceFactory.newByteArrayResource(drlContentString
                    .getBytes()), ResourceType.DRL);
        }

        if (kbuilder.hasErrors()) {
            fail(kbuilder.getErrors().toString());
        }
        if (kBaseConfig == null) {
            kBaseConfig = RuleBaseFactory.newKnowledgeBaseConfiguration();
        }
        InternalKnowledgeBase kbase = kBaseConfig == null ? KnowledgeBaseFactory.newKnowledgeBase() : KnowledgeBaseFactory.newKnowledgeBase(kBaseConfig);
        kbase.addPackages( kbuilder.getKnowledgePackages());
        return kbase;
    }

    protected KieBase loadKnowledgeBase(KnowledgeBuilderConfiguration kbuilderConf, KieBaseConfiguration kbaseConf, String... classPathResources) {
        Collection<KiePackage> knowledgePackages = loadKnowledgePackages(kbuilderConf, classPathResources);

        if (kbaseConf == null) {
            kbaseConf = RuleBaseFactory.newKnowledgeBaseConfiguration();
        }
        InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase(kbaseConf);
        kbase.addPackages(knowledgePackages);
        try {
            kbase = SerializationHelper.serializeObject(kbase);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return kbase;
    }

    protected KieBase loadKnowledgeBase(PackageDescr descr) {
        return loadKnowledgeBase(null, null, descr);
    }

    protected KieBase loadKnowledgeBase(KnowledgeBuilderConfiguration kbuilderConf,KieBaseConfiguration kbaseConf, PackageDescr descr) {
        Collection<KiePackage> knowledgePackages = loadKnowledgePackages(kbuilderConf, descr);

        if (kbaseConf == null) {
            kbaseConf = RuleBaseFactory.newKnowledgeBaseConfiguration();
        }
        InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase(kbaseConf);
        kbase.addPackages(knowledgePackages);
        try {
            kbase = SerializationHelper.serializeObject(kbase);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return kbase;
    }

    public Collection<KiePackage> loadKnowledgePackages(String... classPathResources) {
        return loadKnowledgePackages(null, classPathResources);
    }

    public Collection<KiePackage> loadKnowledgePackages(PackageDescr descr) {
        return loadKnowledgePackages(null, descr);
    }

    public Collection<KiePackage> loadKnowledgePackages(KnowledgeBuilderConfiguration kbuilderConf, PackageDescr descr) {
        if (kbuilderConf == null) {
            kbuilderConf = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
        }
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(kbuilderConf);
        kbuilder.add(ResourceFactory.newDescrResource(descr), ResourceType.DESCR);
        if (kbuilder.hasErrors()) {
            fail(kbuilder.getErrors().toString());
        }
        Collection<KiePackage> knowledgePackages = kbuilder.getKnowledgePackages();
        return knowledgePackages;
    }

    public Collection<KiePackage> loadKnowledgePackages( KnowledgeBuilderConfiguration kbuilderConf, String... classPathResources) {
        return loadKnowledgePackages(kbuilderConf, true, classPathResources);
    }

    public Collection<KiePackage> loadKnowledgePackages( KnowledgeBuilderConfiguration kbuilderConf, boolean serialize, String... classPathResources) {
        if (kbuilderConf == null) {
            kbuilderConf = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
        }

        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(kbuilderConf);
        for (String classPathResource : classPathResources) {
            kbuilder.add(ResourceFactory.newClassPathResource(classPathResource, getClass()), ResourceType.DRL);
        }

        if (kbuilder.hasErrors()) {
            fail(kbuilder.getErrors().toString());
        }

        Collection<KiePackage> knowledgePackages;
        if ( serialize ) {
            try {
                knowledgePackages = SerializationHelper.serializeObject(kbuilder.getKnowledgePackages(),  ((KnowledgeBuilderConfigurationImpl)kbuilderConf).getClassLoader() );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            knowledgePackages = kbuilder.getKnowledgePackages();
        }
        return knowledgePackages;
    }

    public Collection<KiePackage> loadKnowledgePackagesFromString(String... content) {
        return loadKnowledgePackagesFromString(null, content);
    }

    public Collection<KiePackage> loadKnowledgePackagesFromString(KnowledgeBuilderConfiguration kbuilderConf, String... content) {
        if (kbuilderConf == null) {
            kbuilderConf = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
        }
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(kbuilderConf);
        for (String r : content) {
            kbuilder.add(ResourceFactory.newByteArrayResource(r.getBytes()),ResourceType.DRL);
        }
        if (kbuilder.hasErrors()) {
            fail(kbuilder.getErrors().toString());
        }
        Collection<KiePackage> knowledgePackages = kbuilder.getKnowledgePackages();
        return knowledgePackages;
    }

    protected KieBase loadKnowledgeBase(KnowledgeBuilderConfiguration kbuilderConf,String... classPathResources) {
        return loadKnowledgeBase(kbuilderConf, null, classPathResources);
    }

    protected KieBase loadKnowledgeBase(KieBaseConfiguration kbaseConf, String... classPathResources) {
        return loadKnowledgeBase(null, kbaseConf, classPathResources);
    }


    protected KieBase getKnowledgeBase() {
        KieBaseConfiguration kBaseConfig = RuleBaseFactory.newKnowledgeBaseConfiguration();
        return getKnowledgeBase(kBaseConfig);
    }

    protected KieBase getKnowledgeBase(KieBaseConfiguration kBaseConfig) {
        KieBase kbase = KnowledgeBaseFactory.newKnowledgeBase(kBaseConfig);
        try {
            kbase = SerializationHelper.serializeObject(kbase, ((InternalKnowledgeBase) kbase).getRootClassLoader());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return kbase;
    }

    protected KieBase loadKnowledgeBase(String... classPathResources) {
        return loadKnowledgeBase(null, null, classPathResources);
    }

    protected InternalAgenda getInternalAgenda(StatefulKnowledgeSession session) {
        return (InternalAgenda) session.getAgenda();
    }

    protected void waitBusy(final long waitDuration) {
        final long waitEndTime = System.currentTimeMillis() + waitDuration;
        while (System.currentTimeMillis() < waitEndTime) {
            // do nothing, only spin.
        }
    }

    protected void testInvalidDrl(final String drl) {
        final KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newByteArrayResource(drl.getBytes()), ResourceType.DRL);

        if (kbuilder.hasErrors()) {
            logger.warn(kbuilder.getErrors().toString());
        }
        assertThat(kbuilder.hasErrors()).isTrue();
    }

    public static byte[] createJar(KieServices ks,
            ReleaseId releaseId,
            String... drls) {
        KieFileSystem kfs = ks.newKieFileSystem().generateAndWritePomXML(
                releaseId);
        for (int i = 0; i < drls.length; i++) {
            if (drls[i] != null) {
                kfs.write("src/main/resources/r" + i + ".drl", drls[i]);
            }
        }
        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        assertThat(kb.getResults().hasMessages(org.kie.api.builder.Message.Level.ERROR)).as(kb.getResults().getMessages(org.kie.api.builder.Message.Level.ERROR).toString()).isFalse();
        InternalKieModule kieModule = (InternalKieModule) ks.getRepository()
                .getKieModule(releaseId);
        byte[] jar = kieModule.getBytes();
        return jar;
    }

    public static KieModule createAndDeployJar(KieServices ks, ReleaseId releaseId, String... drls) {
        byte[] jar = createJar(ks, releaseId, drls);

        // Deploy jar into the repository
        KieModule km = deployJarIntoRepository(ks, jar);
        return km;
    }

    public static KieModule createAndDeployJar(KieServices ks, String kmoduleContent, ReleaseId releaseId, Resource... resources) {
        return createAndDeployJar(ks, kmoduleContent, o -> true, releaseId, resources);
    }

    public static KieModule createAndDeployJar(KieServices ks,
            String kmoduleContent,
            Predicate<String> classFilter,
            ReleaseId releaseId,
            Resource... resources) {
        byte[] jar = createJar(ks, kmoduleContent, classFilter, releaseId, resources);

        KieModule km = deployJarIntoRepository(ks, jar);
        return km;
    }

    public static byte[] createJar(KieServices ks, String kmoduleContent, Predicate<String> classFilter, ReleaseId releaseId, Resource... resources) {
        KieFileSystem kfs = ks.newKieFileSystem().generateAndWritePomXML(releaseId).writeKModuleXML(kmoduleContent);
        for (int i = 0; i < resources.length; i++) {
            if (resources[i] != null) {
                kfs.write(resources[i]);
            }
        }
        KieBuilder kieBuilder = ks.newKieBuilder(kfs);
        ((InternalKieBuilder) kieBuilder).buildAll(classFilter);
        Results results = kieBuilder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(results.getMessages(Message.Level.ERROR).toString());
        }
        InternalKieModule kieModule = (InternalKieModule) ks.getRepository()
                .getKieModule(releaseId);
        byte[] jar = kieModule.getBytes();
        return jar;
    }

    private static KieModule deployJarIntoRepository(KieServices ks, byte[] jar) {
        Resource jarRes = ks.getResources().newByteArrayResource(jar);
        KieModule km = ks.getRepository().addKieModule(jarRes);
        return km;
    }

    public static byte[] createKJar(KieServices ks,
            ReleaseId releaseId,
            Resource pom,
            Resource... resources) {
        KieFileSystem kfs = ks.newKieFileSystem();
        if (pom != null) {
            kfs.write(pom);
        } else {
            kfs.generateAndWritePomXML(releaseId);
        }
        for (int i = 0; i < resources.length; i++) {
            if (resources[i] != null) {
                kfs.write(resources[i]);
            }
        }
        ks.newKieBuilder(kfs).buildAll();
        InternalKieModule kieModule = (InternalKieModule) ks.getRepository()
                .getKieModule(releaseId);
        byte[] jar = kieModule.getBytes();
        return jar;
    }

    public static byte[] createKJar(KieServices ks,
            ReleaseId releaseId,
            String pom,
            String... drls) {
        return createKJar(ks, null, releaseId, pom, drls);
    }

    public static byte[] createKJar(KieServices ks,
            KieModuleModel kproj,
            ReleaseId releaseId,
            String pom,
            String... drls) {
        KieFileSystem kfs = ks.newKieFileSystem();
        if (kproj != null) {
            kfs.writeKModuleXML(kproj.toXML());
        }
        if (pom != null) {
            kfs.write("pom.xml", pom);
        } else {
            kfs.generateAndWritePomXML(releaseId);
        }
        for (int i = 0; i < drls.length; i++) {
            if (drls[i] != null) {
                kfs.write("src/main/resources/r" + i + ".drl", drls[i]);
            }
        }
        return buildKJar(ks, kfs, releaseId);
    }

    public static byte[] buildKJar(KieServices ks, KieFileSystem kfs, ReleaseId releaseId) {
        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            for (Message result : kb.getResults().getMessages()) {
                System.out.println(result.getText());
            }
            return null;
        }
        InternalKieModule kieModule = (InternalKieModule) ks.getRepository()
                .getKieModule(releaseId);
        byte[] jar = kieModule.getBytes();
        return jar;
    }

    public static KieModule deployJar(KieServices ks, byte[] jar) {
        // Deploy jar into the repository
        Resource jarRes = ks.getResources().newByteArrayResource(jar);
        KieModule km = ks.getRepository().addKieModule(jarRes);
        return km;
    }

    public static KieSession marshallAndUnmarshall(KieServices ks, KieBase kbase, KieSession ksession) {
        return marshallAndUnmarshall(ks, kbase, ksession, null);
    }

    public static KieSession marshallAndUnmarshall(KieServices ks, KieBase kbase, KieSession ksession, KieSessionConfiguration sessionConfig) {
        // Serialize and Deserialize
        try {
            Marshaller marshaller = ks.getMarshallers().newMarshaller(kbase);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshall(baos, ksession);
            marshaller = MarshallerFactory.newMarshaller(kbase);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            baos.close();
            ksession = marshaller.unmarshall(bais, sessionConfig, null);
            bais.close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("unexpected exception :" + e.getMessage());
        }
        return ksession;
    }
    
    public KieSession genSession(String source) {
        return genSession(new String[] {source},0);
    }

    public KieSession genSession(String source, int numerrors)  {
        return genSession(new String[] {source},numerrors);
    }

    public KieSession genSession(String[] sources, int numerrors)  {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        for (String source : sources)
            kbuilder.add( ResourceFactory.newClassPathResource(source, getClass()), ResourceType.DRL );
        KnowledgeBuilderErrors errors = kbuilder.getErrors();
        if ( kbuilder.getErrors().size() > 0 ) {
            for ( KnowledgeBuilderError error : kbuilder.getErrors() ) {
                System.err.println( error );
            }
        }
        assertThat(errors.size()).isEqualTo(numerrors);

        InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();

        kbase.addPackages( kbuilder.getKnowledgePackages() );

        return createKnowledgeSession(kbase);

    }
}
