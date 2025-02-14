// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.dataquality.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRootGraphFetchTree;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_dataquality_generation_dataquality;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_;

public class DataQualityPropertyPathTreeGenerator
{
    public static RootGraphFetchTree getPropertyPathTree(Root_meta_external_dataquality_DataQualityRootGraphFetchTree dataQualityRootGraphFetchTree, String clientVersion, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions) throws JsonProcessingException
    {
         org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.RootGraphFetchTree rootGraphFetchTree = core_dataquality_generation_dataquality.Root_meta_external_dataquality_getEnrichedTreeForStructuralValidations_DataQualityRootGraphFetchTree_1__RootGraphFetchTree_1_(dataQualityRootGraphFetchTree, pureModel.getExecutionSupport());
         return serializeToJSON(rootGraphFetchTree, clientVersion, pureModel, extensions);
    }

    private static RootGraphFetchTree serializeToJSON(org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.RootGraphFetchTree purePlan, String clientVersion, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> extensions) throws JsonProcessingException
    {
        String cl = (clientVersion == null || !supports(clientVersion)) ? PureClientVersions.production : clientVersion;
        Object transformed = transformToVersionedModel(purePlan, cl, extensions, pureModel.getExecutionSupport());
        return serializeToJSON(transformed, pureModel);
    }

    private static Object transformToVersionedModel(org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.RootGraphFetchTree pureGraph, String version, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, ExecutionSupport executionSupport)
    {
        try
        {
            Class cl = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + version + "_transfers_valueSpecification");
            Method graphFetchProtocolMethod = cl.getMethod("Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_valueSpecification_transformGraphFetchTree_GraphFetchTree_1__String_MANY__Map_1__Extension_MANY__GraphFetchTree_1_", GraphFetchTree.class, RichIterable.class, PureMap.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
            return graphFetchProtocolMethod.invoke(null, pureGraph, Lists.mutable.empty(), new PureMap(Maps.mutable.empty()), extensions, executionSupport);
        }
        catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static RootGraphFetchTree serializeToJSON(Object protocolPlan, PureModel pureModel) throws JsonProcessingException
    {
        String asJSON = Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(protocolPlan, pureModel.getExecutionSupport());
        return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(asJSON, RootGraphFetchTree.class);
    }

    public static boolean isDataQualityInstance(PackageableElement element)
    {
        return element instanceof Root_meta_external_dataquality_DataQuality
                && ((Root_meta_external_dataquality_DataQuality) element)._validationTree() != null;
    }

    private static boolean supports(String version)
    {
        return "vX_X_X".equals(version) || PureClientVersions.versionAGreaterThanOrEqualsVersionB(version, "v1_20_0");
    }
}
