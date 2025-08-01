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
package org.drools.core.base.accumulators;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigDecimal;

/**
 * An implementation of an accumulator capable of calculating sum of values
 */
public class BigDecimalSumAccumulateFunction extends AbstractAccumulateFunction<BigDecimalSumAccumulateFunction.SumData> {

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }

    public void writeExternal(ObjectOutput out) throws IOException { }

    protected static class SumData implements Externalizable {
        public BigDecimal total = BigDecimal.ZERO;

        public SumData() {}

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            total = (BigDecimal) in.readObject();
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject( total );
        }
    }

    public SumData createContext() {
        return new SumData();
    }

    public void init(SumData data) {
        data.total = BigDecimal.ZERO;
    }

    public void accumulate(SumData data,
                           Object value) {
        if (value != null) {
            data.total = data.total.add( (BigDecimal) value );
        }
    }

    public void reverse(SumData data, Object value) {
        if (value != null) {
            data.total = data.total.subtract( (BigDecimal) value );
        }
    }

    public Object getResult(SumData data) {
        return data.total;
    }

    public boolean supportsReverse() {
        return true;
    }

    public Class<?> getResultType() {
        return BigDecimal.class;
    }
}
