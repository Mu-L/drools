/**
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
package org.drools.model.functions;

import java.io.Serializable;

public interface Function0<R> extends Serializable {
    R apply();

    final class Null implements Function0 {

        public static final Null INSTANCE = new Null();

        private Null() { }

        @Override
        public Object apply() {
            return null;
        }
    }

    class Impl<R> extends IntrospectableLambda implements Function0<R> {

        private final Function0<R> function;

        public Impl(Function0<R> function) {
            this.function = function;
        }

        @Override
        public R apply() {
            return function.apply();
        }

        @Override
        public Object getLambda() {
            return function;
        }
    }
}
