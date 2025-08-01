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
package org.kie.internal.runtime;

import org.kie.api.runtime.KieRuntime;
import org.kie.api.runtime.KieSession;

/**
 * StatefulKnowledgeSession is the most common way to interact with the engine. A StatefulKnowledgeSession
 * allows the application to establish an iterative conversation with the engine, where the state of the
 * session is kept across invocations.  The reasoning process may be triggered multiple times for the
 * same set of data. After the application finishes using the session, though, it <b>must</b> call the
 * <code>dispose()</code> method in order to free the resources and used memory.
 *
 * <p>
 * Simple example showing a stateful session executing rules for a given collection of java objects.
 * </p>
 * <pre>
 * KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
 * kbuilder.add( ResourceFactory.newFileSystemResource( fileName ), ResourceType.DRL );
 * assertFalse( kbuilder.hasErrors() );
 * if (kbuilder.hasErrors() ) {
 *     System.out.println( kbuilder.getErrors() );
 * }
 * KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
 * kbase.addKnowledgePackages( kbuilder.getKnowledgePackages() );
 *
 * StatefulKnowledgeSession ksession = kbase.newKieSession();
 * for( Object fact : facts ) {
 *     ksession.insert( fact );
 * }
 * ksession.fireAllRules();
 * ksession.dispose();
 * </pre>
 *
 * <p>
 * Simple example showing a stateful session executing processes.
 * </p>
 * <pre>
 * KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
 * kbuilder.add( ResourceFactory.newFileSystemResource( fileName ), ResourceType.BPMN2 );
 * KnowledgeBase kbase = kbuilder.newKnowledgeBase();
 * StatefulKnowledgeSession ksession = kbase.newKieSession();
 * ksession.startProcess("com.sample.processid");
 * ksession.signalEvent("SomeEvent", null);
 * ksession.startProcess("com.sample.processid");
 * ksession.dispose();
 * </pre>
 *
 * <p>
 * StatefulKnowledgeSessions support globals. Globals are used to pass information into the engine
 * (like data or service callbacks that can be used in your rules and processes), but they should not
 * be used to reason over. If you need to reason over your data, make sure you insert
 * it as a fact, not a global.</p>
 * <p>Globals are shared among ALL your rules and processes, so be especially careful of (and avoid
 * as much as possible) mutable globals. Also, it is a good practice to set your globals before
 * inserting your facts or starting your processes. Rules engines evaluate rules at fact insertion
 * time, and so, if you are using a global to constraint a fact pattern, and the global is not set,
 * you may receive a <code>NullPointerException</code>. </p>
 * <p>Globals can be resolved in two ways. The StatefulKnowledgeSession supports getGlobals() which
 * returns the internal Globals, which itself can take a delegate. Calling of setGlobal(String, Object)
 * will set the global on an internal Collection. Identifiers in this internal
 * Collection will have priority over the externally supplied Globals delegate. If an identifier cannot be found in
 * the internal Collection, it will then check the externally supplied Globals delegate, if one has been set.
 * </p>
 *
 * <p>Code snippet for setting a global:</p>
 * <pre>
 * StatefulKnowledgeSession ksession = kbase.newKieSession();
 * ksession.setGlobal( "hbnSession", hibernateSession ); // sets a global hibernate session, that can be used for DB interactions in the rules.
 * for( Object fact : facts ) {
 *     ksession.insert( fact );
 * }
 * ksession.fireAllRules(); // this will now execute and will be able to resolve the "hbnSession" identifier.
 * ksession.dispose();
 * </pre>
 *
 * <p>
 * Like StatelessKnowledgeSession this also implements CommandExecutor which can be used to script a StatefulKnowledgeSession. See CommandExecutor
 * for more details.
 * </p>
 *
 * @see org.kie.api.runtime.Globals
 */
public interface StatefulKnowledgeSession extends KieSession, KieRuntime {

}
