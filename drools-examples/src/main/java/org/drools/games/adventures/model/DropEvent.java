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
package org.drools.games.adventures.model;

import org.kie.api.definition.type.Position;

public class DropEvent extends GameEvent {

    @Position(0)
    private Character character;

    @Position(1)
    private Thing     thing;

    public DropEvent(Character character, Thing thing) {
        this.character = character;
        this.thing = thing;
    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public Thing getThing() {
        return thing;
    }

    public void setThing(Thing thing) {
        this.thing = thing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        DropEvent that = (DropEvent) o;

        if (!character.equals(that.character)) { return false; }
        if (!thing.equals(that.thing)) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = character.hashCode();
        result = 31 * result + thing.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DropEvent{" +
               "character=" + character +
               ", thing=" + thing +
               '}';
    }
}
