// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.finos.legend.engine.pure.code.core.CoreCodeRepositoryProvider;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestIdBuilderCore
{
    private static PureRuntime runtime;
    private static ProcessorSupport processorSupport;

    @BeforeClass
    public static void setUp()
    {
        MutableCodeStorage codeStorage = PureCodeStorage.createCodeStorage(null, Lists.immutable.with(CodeRepository.newPlatformCodeRepository(), new CoreCodeRepositoryProvider().repository()));
        runtime = new PureRuntimeBuilder(codeStorage).setTransactionalByDefault(false).buildAndInitialize();
        processorSupport = runtime.getProcessorSupport();
    }

    @Test
    public void testIdUniqueness()
    {
        testIdUniqueness(IdBuilder.newIdBuilder(processorSupport));
    }

    @Test
    public void testIdUniquenessWithPrefix()
    {
        testIdUniqueness(IdBuilder.newIdBuilder("$core$", processorSupport));
    }

    @Test
    public void testBadIds()
    {
        IdBuilder idBuilder = IdBuilder.newIdBuilder(processorSupport);
        MutableSet<CoreInstance> excludedClassifiers = PrimitiveUtilities.getPrimitiveTypes(processorSupport).toSet();
        MutableMap<CoreInstance, MutableSet<String>> badIdsByClassifier = Maps.mutable.empty();
        GraphNodeIterable.fromModelRepository(runtime.getModelRepository())
                .forEach(instance ->
                {
                    CoreInstance classifier = instance.getClassifier();
                    if (!excludedClassifiers.contains(classifier))
                    {
                        String id = idBuilder.buildId(instance);
                        if (ModelRepository.isAnonymousInstanceName(id))
                        {
                            badIdsByClassifier.getIfAbsentPut(classifier, Sets.mutable::empty).add(id);
                        }
                    }
                });
        if (badIdsByClassifier.notEmpty())
        {
            MutableMap<CoreInstance, String> classifierPaths = badIdsByClassifier.keysView().toMap(c -> c, PackageableElement::getUserPathForPackageableElement);
            StringBuilder builder = new StringBuilder("The following ids have conflicts:");
            badIdsByClassifier.keysView().toSortedListBy(classifierPaths::get).forEach(classifier ->
            {
                builder.append("\n\t").append(classifierPaths.get(classifier)).append(":\n\t\t");
                badIdsByClassifier.get(classifier).toSortedList().appendString(builder, ", ");
            });
            Assert.fail(builder.toString());
        }
    }

    private void testIdUniqueness(IdBuilder idBuilder)
    {
        // ImportGroup is excluded because of a known issue with conflicts with ImportGroup ids independent of IdBuilder
        MutableSet<CoreInstance> excludedClassifiers = PrimitiveUtilities.getPrimitiveTypes(processorSupport).toSet().with(processorSupport.package_getByUserPath(M3Paths.ImportGroup));
        MutableMap<CoreInstance, MutableSet<String>> classifierIds = Maps.mutable.empty();
        MutableMap<CoreInstance, MutableObjectIntMap<String>> idConflicts = Maps.mutable.empty();
        GraphNodeIterable.fromModelRepository(runtime.getModelRepository())
                .forEach(instance ->
                {
                    CoreInstance classifier = instance.getClassifier();
                    if (!excludedClassifiers.contains(classifier))
                    {
                        String id = idBuilder.buildId(instance);
                        if (!classifierIds.getIfAbsentPut(classifier, Sets.mutable::empty).add(id))
                        {
                            idConflicts.getIfAbsentPut(classifier, ObjectIntMaps.mutable::empty).updateValue(id, 1, n -> n + 1);
                        }
                    }
                });
        if (idConflicts.notEmpty())
        {
            MutableMap<CoreInstance, String> classifierPaths = idConflicts.keysView().toMap(c -> c, PackageableElement::getUserPathForPackageableElement);
            StringBuilder builder = new StringBuilder("The following ids have conflicts:");
            idConflicts.keysView().toSortedListBy(classifierPaths::get).forEach(classifier ->
            {
                builder.append("\n\t").append(classifierPaths.get(classifier));
                ObjectIntMap<String> classifierIdConflicts = idConflicts.get(classifier);
                classifierIdConflicts.forEachKeyValue((id, count) -> builder.append("\n\t\t").append(id).append(": ").append(count).append(" instances"));
            });
            Assert.fail(builder.toString());
        }
    }
}
