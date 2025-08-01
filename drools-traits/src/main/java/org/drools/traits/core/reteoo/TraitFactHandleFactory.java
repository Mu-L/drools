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
package org.drools.traits.core.reteoo;

import org.drools.core.WorkingMemoryEntryPoint;
import org.drools.core.common.DefaultFactHandle;
import org.drools.base.reteoo.InitialFactImpl;
import org.drools.core.reteoo.ReteooFactHandleFactory;
import org.drools.core.rule.accessor.FactHandleFactory;
import org.drools.traits.core.common.TraitDefaultFactHandle;

public class TraitFactHandleFactory extends ReteooFactHandleFactory {

    @Override
    public DefaultFactHandle newInitialFactHandle(WorkingMemoryEntryPoint wmEntryPoint) {
        return new TraitDefaultFactHandle(0, InitialFactImpl.getInstance(), 0, wmEntryPoint);
    }

    @Override
    public FactHandleFactory newInstance() {
        return new TraitFactHandleFactory();
    }

    @Override
    public DefaultFactHandle createDefaultFactHandle(long id, Object object, long recency, WorkingMemoryEntryPoint entryPoint) {
        return new TraitDefaultFactHandle(id, object, recency, entryPoint);
    }
}
