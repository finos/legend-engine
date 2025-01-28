// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.FixedSizeSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.m3.relationship.Association;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.m3.type.Enumeration;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.type.Measure;
import org.finos.legend.engine.protocol.pure.m3.extension.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TestDependencyManagement
{
    private final java.lang.Class<SectionIndex> SECTION_INDEX_CLASS = SectionIndex.class;
    private final java.lang.Class<Measure> MEASURE_CLASS = Measure.class;
    private final java.lang.Class<Profile> PROFILE_CLASS = Profile.class;
    private final java.lang.Class<DataElement> DATA_ELEMENT_CLASS = DataElement.class;
    private final java.lang.Class<Enumeration> ENUMERATION_CLASS = Enumeration.class;
    private final java.lang.Class<Class> CLASS_CLASS = Class.class;
    private final java.lang.Class<Association> ASSOCIATION_CLASS = Association.class;
    private final java.lang.Class<Mapping> MAPPING_CLASS = Mapping.class;
    private final java.lang.Class<PackageableConnection> PACKAGEABLE_CONNECTION_CLASS = PackageableConnection.class;
    private final java.lang.Class<PackageableRuntime> PACKAGEABLE_RUNTIME_CLASS = PackageableRuntime.class;
    private final java.lang.Class<Function> FUNCTION_CLASS = Function.class;
    private final java.lang.Class<Binding> BINDING_CLASS = Binding.class;

    @Test
    public void testProcessDependencyGraph()
    {
        MutableMap<java.lang.Class<? extends PackageableElement>, Collection<java.lang.Class<? extends PackageableElement>>> dependencyGraph = Maps.mutable.empty();
        dependencyGraph.put(SECTION_INDEX_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(PROFILE_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(CLASS_CLASS, Lists.fixedSize.with(MEASURE_CLASS));
        dependencyGraph.put(ASSOCIATION_CLASS, Lists.fixedSize.with(CLASS_CLASS));
        dependencyGraph.put(MAPPING_CLASS, Lists.fixedSize.with(ENUMERATION_CLASS, CLASS_CLASS, ASSOCIATION_CLASS, BINDING_CLASS));
        dependencyGraph.put(PACKAGEABLE_CONNECTION_CLASS, Lists.fixedSize.with(MAPPING_CLASS));
        dependencyGraph.put(PACKAGEABLE_RUNTIME_CLASS, Lists.fixedSize.with(MAPPING_CLASS, PACKAGEABLE_CONNECTION_CLASS));
        dependencyGraph.put(FUNCTION_CLASS, Lists.fixedSize.with(DATA_ELEMENT_CLASS, CLASS_CLASS, ASSOCIATION_CLASS, MAPPING_CLASS, BINDING_CLASS));

        DependencyManagement dependencyManagement = new DependencyManagement();
        dependencyManagement.processDependencyGraph(dependencyGraph);

        dependencyGraph.put(MEASURE_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(BINDING_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(ENUMERATION_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(DATA_ELEMENT_CLASS, Lists.fixedSize.empty());

        Assert.assertEquals(dependencyGraph, dependencyManagement.getDependentToDependencies());
    }

    @Test
    public void testDetectCircularDependency()
    {
        MutableMap<java.lang.Class<? extends PackageableElement>, Collection<java.lang.Class<? extends PackageableElement>>> dependencyGraph = Maps.mutable.empty();
        dependencyGraph.put(SECTION_INDEX_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(PROFILE_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(CLASS_CLASS, Lists.fixedSize.with(MEASURE_CLASS, FUNCTION_CLASS));
        dependencyGraph.put(ASSOCIATION_CLASS, Lists.fixedSize.with(CLASS_CLASS));
        dependencyGraph.put(MAPPING_CLASS, Lists.fixedSize.with(ENUMERATION_CLASS, CLASS_CLASS, ASSOCIATION_CLASS, BINDING_CLASS));
        dependencyGraph.put(PACKAGEABLE_CONNECTION_CLASS, Lists.fixedSize.with(MAPPING_CLASS));
        dependencyGraph.put(PACKAGEABLE_RUNTIME_CLASS, Lists.fixedSize.with(MAPPING_CLASS, PACKAGEABLE_CONNECTION_CLASS));
        dependencyGraph.put(FUNCTION_CLASS, Lists.fixedSize.with(DATA_ELEMENT_CLASS, CLASS_CLASS, ASSOCIATION_CLASS, MAPPING_CLASS, BINDING_CLASS));

        DependencyManagement dependencyManagement = new DependencyManagement();
        dependencyManagement.processDependencyGraph(dependencyGraph);
        try
        {
            dependencyManagement.detectCircularDependency();
            Assert.fail("Expected compilation error with message: Detected a circular dependency in dependencyGraph; but no error occurred");
        }
        catch (EngineException e)
        {
            Assert.assertEquals("Detected a circular dependency in dependencyGraph", e.getMessage());
        }
    }

    @Test
    public void testAcyclicDependencyGraph()
    {
        MutableMap<java.lang.Class<? extends PackageableElement>, Collection<java.lang.Class<? extends PackageableElement>>> dependencyGraph = Maps.mutable.empty();
        dependencyGraph.put(SECTION_INDEX_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(PROFILE_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(CLASS_CLASS, Lists.fixedSize.with(MEASURE_CLASS));
        dependencyGraph.put(ASSOCIATION_CLASS, Lists.fixedSize.with(CLASS_CLASS));
        dependencyGraph.put(MAPPING_CLASS, Lists.fixedSize.with(ENUMERATION_CLASS, CLASS_CLASS, ASSOCIATION_CLASS, BINDING_CLASS));
        dependencyGraph.put(PACKAGEABLE_CONNECTION_CLASS, Lists.fixedSize.with(MAPPING_CLASS));
        dependencyGraph.put(PACKAGEABLE_RUNTIME_CLASS, Lists.fixedSize.with(MAPPING_CLASS, PACKAGEABLE_CONNECTION_CLASS));
        dependencyGraph.put(FUNCTION_CLASS, Lists.fixedSize.with(DATA_ELEMENT_CLASS, CLASS_CLASS, ASSOCIATION_CLASS, MAPPING_CLASS, BINDING_CLASS));

        DependencyManagement dependencyManagement = new DependencyManagement();
        dependencyManagement.processDependencyGraph(dependencyGraph);
        dependencyManagement.detectCircularDependency();

        MutableMap<java.lang.Class<? extends PackageableElement>, Set<java.lang.Class<? extends PackageableElement>>> expectedDependencyToDependents = Maps.mutable.empty();
        expectedDependencyToDependents.put(SECTION_INDEX_CLASS, Sets.fixedSize.empty());
        expectedDependencyToDependents.put(PROFILE_CLASS, Sets.fixedSize.empty());
        expectedDependencyToDependents.put(DATA_ELEMENT_CLASS, Sets.fixedSize.with(FUNCTION_CLASS));
        expectedDependencyToDependents.put(MEASURE_CLASS, Sets.fixedSize.with(CLASS_CLASS));
        expectedDependencyToDependents.put(BINDING_CLASS, Sets.fixedSize.with(MAPPING_CLASS, FUNCTION_CLASS));
        expectedDependencyToDependents.put(ENUMERATION_CLASS, Sets.fixedSize.with(MAPPING_CLASS));
        expectedDependencyToDependents.put(CLASS_CLASS, Sets.fixedSize.with(MAPPING_CLASS, ASSOCIATION_CLASS, FUNCTION_CLASS));
        expectedDependencyToDependents.put(ASSOCIATION_CLASS, Sets.fixedSize.with(MAPPING_CLASS, FUNCTION_CLASS));
        expectedDependencyToDependents.put(MAPPING_CLASS, Sets.fixedSize.with(PACKAGEABLE_CONNECTION_CLASS, PACKAGEABLE_RUNTIME_CLASS, FUNCTION_CLASS));
        expectedDependencyToDependents.put(PACKAGEABLE_CONNECTION_CLASS, Sets.fixedSize.with(PACKAGEABLE_RUNTIME_CLASS));
        expectedDependencyToDependents.put(PACKAGEABLE_RUNTIME_CLASS, Sets.fixedSize.empty());
        expectedDependencyToDependents.put(FUNCTION_CLASS, Sets.fixedSize.empty());

        MutableMap<java.lang.Class<? extends PackageableElement>, Collection<java.lang.Class<? extends PackageableElement>>> dependencyToDependents = dependencyManagement.getDependencyToDependents();
        MutableMap<java.lang.Class<? extends PackageableElement>, Set<java.lang.Class<? extends PackageableElement>>> actualDependencyToDependents = Maps.mutable.empty();
        dependencyToDependents.forEach((dependency, dependents) -> actualDependencyToDependents.put(dependency, new HashSet<>(dependents)));
        Assert.assertEquals(expectedDependencyToDependents, actualDependencyToDependents);

        MutableList<MutableSet<java.lang.Class<? extends PackageableElement>>> expectedTopologicallyOrderedClasses = Lists.mutable.empty();
        MutableSet<java.lang.Class<? extends PackageableElement>> firstLevelIndependentClasses = Sets.mutable.of(DATA_ELEMENT_CLASS, MEASURE_CLASS, PROFILE_CLASS, BINDING_CLASS, SECTION_INDEX_CLASS, ENUMERATION_CLASS);
        MutableSet<java.lang.Class<? extends PackageableElement>> secondLevelIndependentClasses = Sets.fixedSize.with(CLASS_CLASS);
        MutableSet<java.lang.Class<? extends PackageableElement>> thirdLevelIndependentClasses = Sets.fixedSize.with(ASSOCIATION_CLASS);
        MutableSet<java.lang.Class<? extends PackageableElement>> fourthLevelIndependentClasses = Sets.fixedSize.with(MAPPING_CLASS);
        MutableSet<java.lang.Class<? extends PackageableElement>> fifthLevelIndependentClasses = Sets.fixedSize.with(FUNCTION_CLASS, PACKAGEABLE_CONNECTION_CLASS);
        MutableSet<java.lang.Class<? extends PackageableElement>> sixthLevelIndependentClasses = Sets.fixedSize.with(PACKAGEABLE_RUNTIME_CLASS);
        expectedTopologicallyOrderedClasses.add(firstLevelIndependentClasses);
        expectedTopologicallyOrderedClasses.add(secondLevelIndependentClasses);
        expectedTopologicallyOrderedClasses.add(thirdLevelIndependentClasses);
        expectedTopologicallyOrderedClasses.add(fourthLevelIndependentClasses);
        expectedTopologicallyOrderedClasses.add(fifthLevelIndependentClasses);
        expectedTopologicallyOrderedClasses.add(sixthLevelIndependentClasses);
        Assert.assertEquals(expectedTopologicallyOrderedClasses, dependencyManagement.getTopologicallyOrderedClasses());
    }

    @Test
    public void testGetDisjointDependencyGraphs()
    {
        MutableMap<java.lang.Class<? extends PackageableElement>, Collection<java.lang.Class<? extends PackageableElement>>> dependencyGraph = Maps.mutable.empty();
        dependencyGraph.put(SECTION_INDEX_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(PROFILE_CLASS, Lists.fixedSize.empty());
        dependencyGraph.put(CLASS_CLASS, Lists.fixedSize.with(MEASURE_CLASS));
        dependencyGraph.put(ASSOCIATION_CLASS, Lists.fixedSize.with(CLASS_CLASS));
        dependencyGraph.put(MAPPING_CLASS, Lists.fixedSize.with(ENUMERATION_CLASS, CLASS_CLASS, ASSOCIATION_CLASS, BINDING_CLASS));
        dependencyGraph.put(PACKAGEABLE_CONNECTION_CLASS, Lists.fixedSize.with(MAPPING_CLASS));
        dependencyGraph.put(PACKAGEABLE_RUNTIME_CLASS, Lists.fixedSize.with(MAPPING_CLASS, PACKAGEABLE_CONNECTION_CLASS));
        dependencyGraph.put(FUNCTION_CLASS, Lists.fixedSize.with(DATA_ELEMENT_CLASS, CLASS_CLASS, ASSOCIATION_CLASS, MAPPING_CLASS, BINDING_CLASS));

        DependencyManagement dependencyManagement = new DependencyManagement();
        dependencyManagement.processDependencyGraph(dependencyGraph);
        dependencyManagement.detectCircularDependency();

        MutableSet<MutableSet<java.lang.Class<? extends PackageableElement>>> expectedDisjointDependencyGraphs = Sets.mutable.empty();
        MutableSet<java.lang.Class<? extends PackageableElement>> disjointDependencyGraph1 = Sets.fixedSize.with(SECTION_INDEX_CLASS);
        MutableSet<java.lang.Class<? extends PackageableElement>> disjointDependencyGraph2 = Sets.fixedSize.with(PROFILE_CLASS);
        MutableSet<java.lang.Class<? extends PackageableElement>> disjointDependencyGraph3 = Sets.mutable.of(BINDING_CLASS, MEASURE_CLASS, DATA_ELEMENT_CLASS, ENUMERATION_CLASS, CLASS_CLASS, ASSOCIATION_CLASS, MAPPING_CLASS, PACKAGEABLE_CONNECTION_CLASS, PACKAGEABLE_RUNTIME_CLASS, FUNCTION_CLASS);
        expectedDisjointDependencyGraphs.add(disjointDependencyGraph1);
        expectedDisjointDependencyGraphs.add(disjointDependencyGraph2);
        expectedDisjointDependencyGraphs.add(disjointDependencyGraph3);
        Assert.assertEquals(expectedDisjointDependencyGraphs, dependencyManagement.getDisjointDependencyGraphs());
    }

    @Test
    public void testTopologicallySortElements()
    {
        DependencyManagement dependencyManagement = new DependencyManagement();
        Mapping m1 = new Mapping();
        m1._package = "test";
        m1.name = "M1";
        Mapping m2 = new Mapping();
        m2._package = "test";
        m2.name = "M2";
        Mapping m3 = new Mapping();
        m3._package = "test";
        m3.name = "M3";
        Mapping m4 = new Mapping();
        m4._package = "test";
        m4.name = "M4";
        Mapping m5 = new Mapping();
        m5._package = "test";
        m5.name = "M5";
        Mapping m6 = new Mapping();
        m6._package = "test";
        m6.name = "M6";
        Mapping m7 = new Mapping();
        m7._package = "test";
        m7.name = "M7";
        FixedSizeList<PackageableElement> testMappings = Lists.fixedSize.with(m1, m2, m3, m4, m5, m6, m7);

        DependencyManagement.PackageableElementsByDependencyLevel packageableElementsByDependencyLevel = dependencyManagement.new PackageableElementsByDependencyLevel(testMappings.stream()
                .map(m -> Tuples.pair(m, m.getPath()))
                .collect(Collectors.toList()));
        FastListMultimap<java.lang.Class<? extends PackageableElement>, DependencyManagement.PackageableElementsByDependencyLevel> classToElements = FastListMultimap.newMultimap(Tuples.pair(Mapping.class, packageableElementsByDependencyLevel));

        MutableMap<String, MutableSet<String>> mappingPrerequisiteGraphs = Maps.mutable.empty();
        mappingPrerequisiteGraphs.put(m2.getPath(), Sets.fixedSize.with(m1.getPath()));
        mappingPrerequisiteGraphs.put(m3.getPath(), Sets.fixedSize.with(m1.getPath()));
        mappingPrerequisiteGraphs.put(m4.getPath(), Sets.fixedSize.with(m3.getPath()));
        mappingPrerequisiteGraphs.put(m5.getPath(), Sets.fixedSize.with(m3.getPath()));
        mappingPrerequisiteGraphs.put(m6.getPath(), Sets.fixedSize.with(m4.getPath()));
        mappingPrerequisiteGraphs.put(m7.getPath(), Sets.fixedSize.with(m4.getPath()));
        MutableMap<java.lang.Class<? extends PackageableElement>, MutableMap<String, MutableSet<String>>> elementPrerequisitesByClass = Maps.mutable.with(Mapping.class, mappingPrerequisiteGraphs);

        FastListMultimap<java.lang.Class<? extends PackageableElement>, DependencyManagement.PackageableElementsByDependencyLevel> classToElementsSortedByDependencyLevel = dependencyManagement.topologicallySortElements(classToElements, elementPrerequisitesByClass);
        MutableList<DependencyManagement.PackageableElementsByDependencyLevel> allMappingsByDependencyLevel = classToElementsSortedByDependencyLevel.get(Mapping.class);
        DependencyManagement.PackageableElementsByDependencyLevel mappingsInFirstLevel = allMappingsByDependencyLevel.get(0);
        FixedSizeList<Pair<PackageableElement, String>> independentElementAndPathPairsInFirstLevel = mappingsInFirstLevel.getIndependentElementAndPathPairs();
        Assert.assertEquals(1, independentElementAndPathPairsInFirstLevel.size());
        Assert.assertEquals(m1.getPath(), independentElementAndPathPairsInFirstLevel.get(0).getTwo());

        DependencyManagement.PackageableElementsByDependencyLevel mappingsInSecondLevel = allMappingsByDependencyLevel.get(1);
        FixedSizeList<Pair<PackageableElement, String>> independentElementAndPathPairsInSecondLevel = mappingsInSecondLevel.getIndependentElementAndPathPairs();
        FixedSizeSet<String> elementPathsInSecondLevel = Sets.fixedSize.with(independentElementAndPathPairsInSecondLevel.get(0).getTwo(), independentElementAndPathPairsInSecondLevel.get(1).getTwo());
        Assert.assertEquals(2, elementPathsInSecondLevel.size());
        Assert.assertTrue(elementPathsInSecondLevel.contains(m2.getPath()));
        Assert.assertTrue(elementPathsInSecondLevel.contains(m3.getPath()));

        DependencyManagement.PackageableElementsByDependencyLevel mappingsInThirdLevel = allMappingsByDependencyLevel.get(2);
        FixedSizeList<Pair<PackageableElement, String>> independentElementAndPathPairsInThirdLevel = mappingsInThirdLevel.getIndependentElementAndPathPairs();
        FixedSizeSet<String> elementPathsInThirdLevel = Sets.fixedSize.with(independentElementAndPathPairsInThirdLevel.get(0).getTwo(), independentElementAndPathPairsInThirdLevel.get(1).getTwo());
        Assert.assertEquals(2, elementPathsInThirdLevel.size());
        Assert.assertTrue(elementPathsInThirdLevel.contains(m4.getPath()));
        Assert.assertTrue(elementPathsInThirdLevel.contains(m5.getPath()));

        DependencyManagement.PackageableElementsByDependencyLevel mappingsInFourthLevel = allMappingsByDependencyLevel.get(3);
        FixedSizeList<Pair<PackageableElement, String>> independentElementAndPathPairsInFourthLevel = mappingsInFourthLevel.getIndependentElementAndPathPairs();
        FixedSizeSet<String> elementPathsInFourthLevel = Sets.fixedSize.with(independentElementAndPathPairsInFourthLevel.get(0).getTwo(), independentElementAndPathPairsInFourthLevel.get(1).getTwo());
        Assert.assertEquals(2, elementPathsInFourthLevel.size());
        Assert.assertTrue(elementPathsInFourthLevel.contains(m6.getPath()));
        Assert.assertTrue(elementPathsInFourthLevel.contains(m7.getPath()));
    }

    @Test
    public void testCircularDependencyInElements()
    {
        DependencyManagement dependencyManagement = new DependencyManagement();
        Mapping m1 = new Mapping();
        m1._package = "test";
        m1.name = "M1";
        Mapping m2 = new Mapping();
        m2._package = "test";
        m2.name = "M2";
        Mapping m3 = new Mapping();
        m3._package = "test";
        m3.name = "M3";
        Mapping m4 = new Mapping();
        m4._package = "test";
        m4.name = "M4";
        Mapping m5 = new Mapping();
        m5._package = "test";
        m5.name = "M5";
        Mapping m6 = new Mapping();
        m6._package = "test";
        m6.name = "M6";
        Mapping m7 = new Mapping();
        m7._package = "test";
        m7.name = "M7";
        FixedSizeList<PackageableElement> testMappings = Lists.fixedSize.with(m1, m2, m3, m4, m5, m6, m7);

        DependencyManagement.PackageableElementsByDependencyLevel packageableElementsByDependencyLevel = dependencyManagement.new PackageableElementsByDependencyLevel(testMappings.stream()
                .map(m -> Tuples.pair(m, m.getPath()))
                .collect(Collectors.toList()));
        FastListMultimap<java.lang.Class<? extends PackageableElement>, DependencyManagement.PackageableElementsByDependencyLevel> classToElements = FastListMultimap.newMultimap(Tuples.pair(Mapping.class, packageableElementsByDependencyLevel));

        MutableMap<String, MutableSet<String>> mappingPrerequisiteGraphs = Maps.mutable.empty();
        mappingPrerequisiteGraphs.put(m2.getPath(), Sets.fixedSize.with(m1.getPath()));
        mappingPrerequisiteGraphs.put(m3.getPath(), Sets.fixedSize.with(m1.getPath(), m7.getPath()));
        mappingPrerequisiteGraphs.put(m4.getPath(), Sets.fixedSize.with(m3.getPath()));
        mappingPrerequisiteGraphs.put(m5.getPath(), Sets.fixedSize.with(m3.getPath()));
        mappingPrerequisiteGraphs.put(m6.getPath(), Sets.fixedSize.with(m4.getPath()));
        mappingPrerequisiteGraphs.put(m7.getPath(), Sets.fixedSize.with(m4.getPath()));
        MutableMap<java.lang.Class<? extends PackageableElement>, MutableMap<String, MutableSet<String>>> elementPrerequisitesByClass = Maps.mutable.with(Mapping.class, mappingPrerequisiteGraphs);

        String expectedErrorMessage = "Detected a circular dependency in element prerequisites graph in the following metamodel: class org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.\nCycle: test::M7 -> test::M4 -> test::M3 -> test::M7";
        try
        {
            dependencyManagement.topologicallySortElements(classToElements, elementPrerequisitesByClass);
            Assert.fail("Expected compilation error with message: " + expectedErrorMessage + "; but no error occurred");
        }
        catch (EngineException e)
        {
            Assert.assertEquals(expectedErrorMessage, e.getMessage());
        }
    }
}
