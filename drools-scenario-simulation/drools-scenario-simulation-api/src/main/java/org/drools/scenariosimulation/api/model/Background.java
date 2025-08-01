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
package org.drools.scenariosimulation.api.model;

import java.util.List;

/**
 * Envelop class that wrap the definition of the <b>Background</b> grid and its data
 */
public class Background extends AbstractScesimModel<BackgroundData> {

    public List<BackgroundDataWithIndex> getBackgroundDataWithIndex() {
        return toScesimDataWithIndex(BackgroundDataWithIndex::new);
    }

    @Override
    public BackgroundData addData(int index) {
        if (index < 0 || index > scesimData.size()) {
            throw new IndexOutOfBoundsException(new StringBuilder().append("Index out of range ").append(index).toString());
        }
        BackgroundData backgroundData = new BackgroundData();
        scesimData.add(index, backgroundData);
        return backgroundData;
    }

    @Override
    public Background cloneModel() {
        Background toReturn = new Background();
        final List<FactMapping> originalFactMappings = this.scesimModelDescriptor.getUnmodifiableFactMappings();
        for (int i = 0; i < originalFactMappings.size(); i++) {
            final FactMapping originalFactMapping = originalFactMappings.get(i);
            toReturn.scesimModelDescriptor.addFactMapping(i, originalFactMapping);
        }
        this.scesimData.forEach(backgroundData -> toReturn.scesimData.add(backgroundData.cloneInstance()));
        return toReturn;
    }
}