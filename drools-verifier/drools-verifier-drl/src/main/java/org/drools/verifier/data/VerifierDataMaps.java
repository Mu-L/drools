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
package org.drools.verifier.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.drools.verifier.components.EntryPoint;
import org.drools.verifier.components.Field;
import org.drools.verifier.components.Import;
import org.drools.verifier.components.ObjectType;
import org.drools.verifier.components.Pattern;
import org.drools.verifier.components.Restriction;
import org.drools.verifier.components.RulePackage;
import org.drools.verifier.components.Variable;
import org.drools.verifier.components.VerifierComponentType;
import org.drools.verifier.components.VerifierRule;
import org.drools.verifier.misc.Multimap;

class VerifierDataMaps
        implements
        VerifierData {

    private Map<VerifierComponentType, Map<String, VerifierComponent>> all = new TreeMap<>();

    private Map<String, RulePackage> packagesByName = new TreeMap<>(STRING_NULL_SAFE_COMPARATOR);
    private Map<String, ObjectType> objectTypesByFullName = new TreeMap<>(STRING_NULL_SAFE_COMPARATOR);
    private Map<String, Field> fieldsByObjectTypeAndFieldName = new TreeMap<>(STRING_NULL_SAFE_COMPARATOR);
    private Multimap<String, Field> fieldsByObjectTypeId = new Multimap<>();
    private Multimap<String, Pattern> patternsByObjectTypeId = new Multimap<>();
    private Multimap<String, Pattern> patternsByRuleName = new Multimap<>();
    private Multimap<String, Restriction> restrictionsByFieldId = new Multimap<>();
    private Map<String, Variable> variablesByRuleAndVariableName = new TreeMap<>(STRING_NULL_SAFE_COMPARATOR);
    private Map<String, EntryPoint> entryPointsByEntryId = new TreeMap<>(STRING_NULL_SAFE_COMPARATOR);
    private Map<String, VerifierRule> rulesByName = new TreeMap<>(STRING_NULL_SAFE_COMPARATOR);
    private Map<String, Import> importsByName = new TreeMap<>(STRING_NULL_SAFE_COMPARATOR);
    private Multimap<String, VerifierRule> rulesByCategory = new Multimap<>();

    public Collection<ObjectType> getObjectTypesByRuleName(String ruleName) {
        Set<ObjectType> set = new HashSet<>();

        for (Pattern pattern : patternsByRuleName.get(ruleName)) {
            ObjectType objectType = (ObjectType) getVerifierObject(VerifierComponentType.OBJECT_TYPE,
                    pattern.getObjectTypePath());
            set.add(objectType);
        }

        return set;
    }

    public ObjectType getObjectTypeByFullName(String name) {
        return objectTypesByFullName.get(name);
    }

    public Field getFieldByObjectTypeAndFieldName(String objectTypeName,
                                                  String fieldName) {
        return fieldsByObjectTypeAndFieldName.get(objectTypeName + "." + fieldName);
    }

    public Variable getVariableByRuleAndVariableName(String ruleName,
                                                     String variableName) {
        return variablesByRuleAndVariableName.get(ruleName + "." + variableName);
    }

    public Collection<VerifierComponent> getAll() {
        List<VerifierComponent> objects = new ArrayList<>();

        for (VerifierComponentType type : all.keySet()) {
            objects.addAll(all.get(type).values());
        }

        return objects;
    }

    public Collection<Field> getFieldsByObjectTypeId(String id) {
        return fieldsByObjectTypeId.get(id);
    }

    public Collection<VerifierRule> getRulesByObjectTypePath(String id) {
        Set<VerifierRule> rules = new HashSet<>();

        for (Pattern pattern : patternsByObjectTypeId.get(id)) {

            rules.add((VerifierRule) getVerifierObject(VerifierComponentType.RULE,
                    pattern.getRulePath()));
        }

        return rules;
    }

    public Collection<VerifierRule> getRulesByFieldPath(String id) {

        Set<VerifierRule> rules = new HashSet<>();

        for (Restriction restriction : restrictionsByFieldId.get(id)) {

            rules.add((VerifierRule) getVerifierObject(VerifierComponentType.RULE,
                    restriction.getRulePath()));
        }

        return rules;
    }

    public RulePackage getPackageByName(String name) {
        return packagesByName.get(name);
    }

    public Collection<Restriction> getRestrictionsByFieldPath(String id) {
        return restrictionsByFieldId.get(id);
    }

    public void add(VerifierComponent object) {
        if (VerifierComponentType.FIELD.equals(object.getVerifierComponentType())) {
            Field field = (Field) object;
            ObjectType objectType = (ObjectType) getVerifierObject(VerifierComponentType.OBJECT_TYPE,
                    field.getObjectTypePath());
            fieldsByObjectTypeAndFieldName.put(objectType.getFullName() + "." + field.getName(),
                    field);

            fieldsByObjectTypeId.put(field.getObjectTypePath(),
                    field);
        } else if (VerifierComponentType.RULE.equals(object.getVerifierComponentType())) {
            VerifierRule rule = (VerifierRule) object;
            rulesByName.put(rule.getName(),
                    rule);
            if (rule.getMetadata().containsKey("category")) {
                rulesByCategory.put(rule.getMetaAttribute("category"),
                        rule);
            }
        } else if (isAVariable(object)) {
            Variable variable = (Variable) object;
            variablesByRuleAndVariableName.put(variable.getRuleName() + "." + variable.getName(),
                    variable);
        } else if (VerifierComponentType.PATTERN.equals(object.getVerifierComponentType())) {
            Pattern pattern = (Pattern) object;

            patternsByObjectTypeId.put(pattern.getObjectTypePath(),
                    pattern);
            patternsByRuleName.put(pattern.getRuleName(),
                    pattern);
        } else if (VerifierComponentType.RESTRICTION.equals(object.getVerifierComponentType())) {
            Restriction restriction = (Restriction) object;

            restrictionsByFieldId.put(restriction.getFieldPath(),
                    restriction);
        } else if (VerifierComponentType.RULE_PACKAGE.equals(object.getVerifierComponentType())) {
            RulePackage rulePackage = (RulePackage) object;

            packagesByName.put(rulePackage.getName(),
                    rulePackage);
        } else if (VerifierComponentType.IMPORT.equals(object.getVerifierComponentType())) {
            Import objectImport = (Import) object;
            importsByName.put(objectImport.getName(),
                    objectImport);
        } else if (VerifierComponentType.OBJECT_TYPE.equals(object.getVerifierComponentType())) {
            ObjectType objectType = (ObjectType) object;
            objectTypesByFullName.put(objectType.getFullName(),
                    objectType);
        } else if (VerifierComponentType.ENTRY_POINT_DESCR.equals(object.getVerifierComponentType())) {
            EntryPoint entryPoint = (EntryPoint) object;
            entryPointsByEntryId.put(entryPoint.getEntryPointName(),
                    entryPoint);
        }

        Map<String, VerifierComponent> map = all.get(object.getVerifierComponentType());

        if (map == null) {
            map = new TreeMap<>();
            all.put(object.getVerifierComponentType(),
                    map);
        }

        String path = object.getPath();

        map.put(path,
                object);

    }

    private boolean isAVariable(VerifierComponent object) {
        return VerifierComponentType.PATTERN_LEVEL_VARIABLE.equals(object.getVerifierComponentType()) || VerifierComponentType.FIELD_LEVEL_VARIABLE.equals(object.getVerifierComponentType());
    }

    public Collection<VerifierRule> getRulesByCategoryName(String categoryName) {
        return rulesByCategory.get(categoryName);
    }

    //    public <T extends VerifierComponent> Collection<T> getAll(VerifierComponentType type) {
    public Collection<? extends VerifierComponent> getAll(VerifierComponentType type) {
        Map<String, VerifierComponent> result = all.get(type);

        if (result == null) {
            return Collections.emptyList();
        } else {
            return result.values();
        }
    }

    //    public <T extends VerifierComponent> T getVerifierObject(VerifierComponentType type,
    //                                                             String path) {
    public VerifierComponent getVerifierObject(VerifierComponentType type,
                                               String path) {
        return all.get(type).get(path);
    }

    public EntryPoint getEntryPointByEntryId(String entryId) {
        return entryPointsByEntryId.get(entryId);
    }

    public VerifierRule getRuleByName(String name) {
        return rulesByName.get(name);
    }

    public Import getImportByName(String name) {
        return importsByName.get(name);
    }

    public ObjectType getObjectTypeByObjectTypeNameAndPackageName(String factTypeName,
                                                                  String packageName) {

        for (VerifierComponent verifierComponent : getAll(VerifierComponentType.IMPORT)) {
            Import objectImport = (Import) verifierComponent;

            if (objectImport.getPackageName().equals(packageName) && objectImport.getShortName().equals(factTypeName)) {
                return this.objectTypesByFullName.get(objectImport.getName());
            }
        }

        return null;
    }

    private static final NullSafeComparator<String> STRING_NULL_SAFE_COMPARATOR = new NullSafeComparator<>();

    public static class NullSafeComparator<T extends Comparable<T>> implements Comparator<T> {
        public int compare(T o1, T o2) {
            return o1 == null ? (o2 == null ? 0 : -1) : (o2 == null ? 1 : o1.compareTo(o2));
        }
    }
}
