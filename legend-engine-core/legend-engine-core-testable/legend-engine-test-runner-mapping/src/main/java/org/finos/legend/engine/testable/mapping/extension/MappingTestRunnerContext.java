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

package org.finos.legend.engine.testable.mapping.extension;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTestSuite;

import java.util.Collection;
import java.util.Map;

/**
 * Per-suite context for {@link MappingTestRunner}: the compiled and protocol
 * views of the suite plus the loaded plan-generation extensions. Its only
 * mutable state is a thread-safe, lazily-populated cache of execution plans
 * keyed on connection identity, used when the suite has no single shared plan
 * (see {@code MappingTestRunner}'s class Javadoc for the plan management
 * strategy); it is otherwise safe to share across threads.
 */
class MappingTestRunnerContext
{
    private final PureModel pureModel;
    private final PureModelContextData pureModelContextData;
    private final MutableList<PlanTransformer> executionPlanTransformers;
    private final Root_meta_pure_mapping_metamodel_MappingTestSuite metamodelTestSuite;
    private final RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions;
    private final Mapping mapping;
    private final Map<String, DataElement> dataElementIndex;
    private final ConcurrentMutableMap<SetIterable<Connection>, MappingTestRunner.GeneratedPlan> planCache = ConcurrentHashMap.newMap();

    MappingTestRunnerContext(Root_meta_pure_mapping_metamodel_MappingTestSuite metamodelTestSuite, Mapping mapping, PureModel pureModel, PureModelContextData pureModelContextData, MutableList<PlanTransformer> executionPlanTransformers, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions)
    {
        this.pureModel = pureModel;
        this.pureModelContextData = pureModelContextData;
        this.dataElementIndex = buildDataElementIndex(pureModelContextData);
        this.mapping = mapping;
        this.executionPlanTransformers = executionPlanTransformers;
        this.metamodelTestSuite = metamodelTestSuite;
        this.routerExtensions = routerExtensions;
    }

    private Map<String, DataElement> buildDataElementIndex(PureModelContextData pureModelContextData)
    {
        MutableMap<String, DataElement> result = Maps.mutable.empty();
        pureModelContextData.getElements().forEach(e ->
        {
            if (e instanceof DataElement)
            {
                result.put(e.getPath(), (DataElement) e);
            }
        });
        return result.asUnmodifiable();
    }

    PureModel getPureModel()
    {
        return this.pureModel;
    }

    Mapping getMapping()
    {
        return this.mapping;
    }

    PureModelContextData getPureModelContextData()
    {
        return this.pureModelContextData;
    }

    Map<String, DataElement> getDataElementIndex()
    {
        return this.dataElementIndex;
    }

    RichIterable<? extends Root_meta_pure_extension_Extension> getRouterExtensions()
    {
        return this.routerExtensions;
    }

    MutableList<PlanTransformer> getExecutionPlanTransformers()
    {
        return this.executionPlanTransformers;
    }

    Root_meta_pure_mapping_metamodel_MappingTestSuite getMetamodelTestSuite()
    {
        return this.metamodelTestSuite;
    }

    /**
     * Return the plan cached for these connection instances, generating and caching one (via
     * {@code planSupplier}) on a miss. Tests whose connections are the same instances - the
     * criterion by which a plan may be reused - share a plan; tests with differing connections
     * each get their own. Thread-safe: a plan is generated at most once per distinct set of
     * connection instances even under concurrent atomic-test execution.
     */
    MappingTestRunner.GeneratedPlan getOrComputePlan(Collection<? extends Connection> connections, Function0<MappingTestRunner.GeneratedPlan> planSupplier)
    {
        return this.planCache.getIfAbsentPut(UnifiedSetWithHashingStrategy.newSet(HashingStrategies.identityStrategy(), connections), planSupplier);
    }
}
