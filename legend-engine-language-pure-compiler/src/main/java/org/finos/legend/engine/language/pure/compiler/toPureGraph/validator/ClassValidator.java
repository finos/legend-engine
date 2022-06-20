// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.compiler.toPureGraph.validator;

import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ClassValidator
{
    public void validate(PureModel pureModel, PureModelContextData pureModelContextData)
    {
        // Create map of elements in V1 since when we try to resolve the generalization in M3, we will get stuffs like Annotated Element, PackageableElement
        // Also using V1 element is more convenient for extracting the source information purpose
        Map<String, Class> classes = new LinkedHashMap<>(); // ensure we validate element in order they are inserted in the pure model context data
        LazyIterate.selectInstancesOf(pureModelContextData.getElements(), Class.class)
                .forEach(_class -> classes.put(pureModel.buildPackageString(_class._package, _class.name), _class));
        this.validateGeneralization(pureModel, classes);
        this.validateProperties(pureModel, classes);
        // TODO Check what Pure does and try to follow
    }

    private void validateProperties(PureModel pureModel, Map<String, Class> classes)
    {
        classes.values().forEach(c ->
        {
            // TODO: check for derived properties as well?
            MutableMultimap<String, Property> prop = Iterate.groupBy(c.properties, p -> p.name);
            pureModel.addWarnings(prop.multiValuesView().flatCollect(a -> a.size() > 1 ? Lists.mutable.with(new Warning(a.getFirst().sourceInformation, "Found duplicated property '" + a.getFirst().name + "' in class '" + pureModel.buildPackageString(c._package, c.name) + "'")) : Lists.mutable.empty()));
        });
    }

    private void visitClassSuperType(Class _class, PureModel pureModel, Map<String, Class> classes, Set<Class> visitedClasses, Set<Class> discoveredClasses)
    {
        discoveredClasses.add(_class);
        _class.superTypes.forEach(superTypePath ->
        {
            Class superType = classes.get(superTypePath);
            if (superType != null)
            {
                if (discoveredClasses.contains(superType))
                {
                    throw new EngineException(pureModel.buildPackageString(_class._package, _class.name) + " -> " + pureModel.buildPackageString(superType._package, superType.name));
                }
                else if (!visitedClasses.contains(superType))
                {
                    try
                    {
                        this.visitClassSuperType(superType, pureModel, classes, visitedClasses, discoveredClasses);
                    }
                    catch (Exception e)
                    {
                        throw new EngineException(pureModel.buildPackageString(_class._package, _class.name) + " -> " + e.getMessage());
                    }
                }
            }
            // NOTE: if the super type is not the map, it means the super type is either some system classes or it
            // belongs to another project, and by right, we should ensure there is not circular dependency in project
            // dependency chain, so we don't have to check that case.
            // For the case the super type class does not exist, other compilation flow would catch that error
        });
        discoveredClasses.remove(_class);
        visitedClasses.add(_class);
    }

    /**
     * Validate if there is a cycle in class supertype/generalization hierarchy
     */
    public void validateGeneralization(PureModel pureModel, Map<String, Class> classes)
    {
        // Detect cycle in directed graph (using DFS), here the edge is from a class to its supertypes -- O(V+E)
        // See https://stackoverflow.com/a/53995651
        Set<Class> visitedClasses = new HashSet<>();
        Set<Class> discoveredClasses = new HashSet<>();
        classes.values().forEach(_class ->
        {
            if (!visitedClasses.contains(_class) || !discoveredClasses.contains(_class))
            {
                try
                {
                    this.visitClassSuperType(_class, pureModel, classes, visitedClasses, discoveredClasses);
                }
                catch (Exception e)
                {
                    throw new EngineException("Cycle detected in class supertype hierarchy: " + e.getMessage(), _class.sourceInformation, EngineErrorType.COMPILATION);
                }
            }
        });
    }
}
