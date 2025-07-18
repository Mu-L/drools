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
package org.drools.core.marshalling;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.marshalling.ObjectMarshallingStrategyAcceptor;

public class SerializablePlaceholderResolverStrategy
    implements
    ObjectMarshallingStrategy {

    private int index;
    
    private ObjectMarshallingStrategyAcceptor acceptor;
    
    public SerializablePlaceholderResolverStrategy(ObjectMarshallingStrategyAcceptor acceptor) {
        this.acceptor = acceptor;
    }
    
    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Object read(ObjectInputStream os) throws IOException,
                                                       ClassNotFoundException {
        return  os.readObject();
    }

    public void write(ObjectOutputStream os,
                      Object object) throws IOException {
        os.writeObject( object );
    }

    public boolean accept(Object object) {
        return acceptor.accept( object );
    }

    public byte[] marshal(Context context,
                          ObjectOutputStream os,
                          Object object) throws IOException {
        
        SerializablePlaceholderStrategyContext ctx = (SerializablePlaceholderStrategyContext)context;
        int index = ctx.data.size();
        ctx.data.add( object );
        return intToByteArray( index );
    }

    public Object unmarshal(Context context,
                            ObjectInputStream is,
                            byte[] object, 
                            ClassLoader classloader) throws IOException, ClassNotFoundException {
        SerializablePlaceholderStrategyContext ctx = (SerializablePlaceholderStrategyContext)context;
        return ctx.data.get( byteArrayToInt( object ) );
    }
    
    public Context createContext() {
        return new SerializablePlaceholderStrategyContext();
    }
    
    protected static class SerializablePlaceholderStrategyContext implements Context {
        // this data map is used when marshalling out objects in order
        // to preserve graph references without cloning objects all over
        // the place.
        public List<Object> data = new ArrayList<>();

        @SuppressWarnings("unchecked")
        public void read(ObjectInputStream ois) throws IOException,
                                               ClassNotFoundException {
            this.data = (List<Object>) ois.readObject();
        }

        public void write(ObjectOutputStream oos) throws IOException {
            oos.writeObject( this.data );
        }
    }

    @Override
    public String toString() {
        return "SerializablePlaceholderResolverStrategy{" +
                "acceptor=" + acceptor +
                '}';
    }

    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte) ((value >>> 24) & 0xFF),
                (byte) ((value >>> 16) & 0xFF),
                (byte) ((value >>> 8) & 0xFF),
                (byte) (value  & 0xFF) };
    }

    public static int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
    }
}
