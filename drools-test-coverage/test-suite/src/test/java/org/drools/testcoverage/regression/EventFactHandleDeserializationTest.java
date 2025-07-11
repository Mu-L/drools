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
package org.drools.testcoverage.regression;

import org.assertj.core.api.SoftAssertions;
import org.drools.core.common.DefaultFactHandle;
import org.junit.jupiter.api.Test;
import org.drools.core.common.DefaultEventHandle;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.utils.KieHelper;


/**
 * Reproducer for BZ 1264525.
 */
public class EventFactHandleDeserializationTest {

    @Test
    public void testDisconnectedEventFactHandle() {
        // DROOLS-924
        final String drl =
                "declare String \n" +
                "  @role(event)\n" +
                "end\n";

        final KieSession ksession = new KieHelper().addContent(drl, ResourceType.DRL)
                .build()
                .newKieSession();

        final DefaultFactHandle helloHandle = (DefaultFactHandle) ksession.insert("hello");
        final DefaultFactHandle goodbyeHandle = (DefaultFactHandle) ksession.insert("goodbye");

        final SoftAssertions softly = new SoftAssertions();

        FactHandle key = DefaultFactHandle.createFromExternalFormat(helloHandle.toExternalForm());
        softly.assertThat(key).isInstanceOf(DefaultEventHandle.class);
        softly.assertThat(ksession.getObject(key)).isEqualTo("hello");

        key = DefaultFactHandle.createFromExternalFormat(goodbyeHandle.toExternalForm());
        softly.assertThat(key).isInstanceOf(DefaultEventHandle.class);
        softly.assertThat(ksession.getObject(key)).isEqualTo("goodbye");

        softly.assertAll();
    }
}
