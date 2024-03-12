/*
 *  Copyright 2023 Goldman Sachs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.finos.legend.engine.generation;

import java.util.Collections;
import java.util.List;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_analytics_search_metamodel_BaseRootDocument;
import org.finos.legend.pure.generated.Root_meta_analytics_search_metamodel_ProjectCoordinates;
import org.finos.legend.pure.generated.Root_meta_analytics_search_metamodel_ProjectCoordinates_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.core_analytics_search_trans;
import org.finos.legend.pure.generated.core_pure_protocol_protocol;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.slf4j.Logger;

public class SearchDocumentArtifactGenerationExtension implements ArtifactGenerationExtension
{
    public final String ROOT_PATH = "searchDocuments";
    public static final String FILE_NAME = "SearchDocumentResult.json";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Generation", "Artifact", "Search_Document");
    }

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SearchDocumentArtifactGenerationExtension.class);

    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        return !((element instanceof Root_meta_legend_service_metamodel_Service) && (element._stereotypes().anySatisfy(stereotype ->
                stereotype._profile()._name().equals("devStatus") && stereotype._profile()._p_stereotypes().anySatisfy(s -> s._value().equals("inProgress"))))
        );
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        try
        {
            Root_meta_analytics_search_metamodel_BaseRootDocument document = core_analytics_search_trans.Root_meta_analytics_search_transformation_buildDocument_PackageableElement_1__ProjectCoordinates_1__BaseRootDocument_1_(element, buildProjectCoordinates(data), pureModel.getExecutionSupport());

            String stringResult = core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(document, pureModel.getExecutionSupport());
            Artifact output = new Artifact(stringResult, FILE_NAME, "json");
            return Collections.singletonList(output);
        }
        catch (Exception exception)
        {
            LOGGER.warn(exception.toString());
            return FastList.newList();
        }
    }

    protected Root_meta_analytics_search_metamodel_ProjectCoordinates buildProjectCoordinates(PureModelContextData data)
    {
        Root_meta_analytics_search_metamodel_ProjectCoordinates projectCoordinates = new Root_meta_analytics_search_metamodel_ProjectCoordinates_Impl("Anonymous_NoCounter");

        if (data.origin != null && data.origin.sdlcInfo != null)
        {
            AlloySDLC sdlcInfo = (AlloySDLC) data.origin.sdlcInfo;
            projectCoordinates._groupId(sdlcInfo.groupId);
            projectCoordinates._artifactId(sdlcInfo.artifactId);
            projectCoordinates._versionId(sdlcInfo.version);
        }
        else
        {
            projectCoordinates._groupId("UNKNOWN");
            projectCoordinates._artifactId("UNKNOWN");
            projectCoordinates._versionId("UNKNOWN");
        }

        return projectCoordinates;
    }
}
