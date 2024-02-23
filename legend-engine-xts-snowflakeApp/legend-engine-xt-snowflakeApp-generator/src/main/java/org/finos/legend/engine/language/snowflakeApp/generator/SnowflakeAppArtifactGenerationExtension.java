// Copyright 2023 Goldman Sachs
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


package org.finos.legend.engine.language.snowflakeApp.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.snowflakeApp.deployment.SnowflakeAppArtifact;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeApp_SnowflakeApp;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.slf4j.Logger;

import java.util.List;

public class SnowflakeAppArtifactGenerationExtension implements ArtifactGenerationExtension
{
    private static  final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SnowflakeAppArtifactGenerationExtension.class);
    private static final String ROOT_PATH = "snowflakeApp";
    private static final String FILE_NAME = "snowflakeAppArtifact.json";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Snowflake");
    }

    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
         return element instanceof Root_meta_external_function_activator_snowflakeApp_SnowflakeApp;
    }


    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        List<Artifact> result = Lists.mutable.empty();
        Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));
        SnowflakeAppArtifact artifact  = SnowflakeAppGenerator.generateArtifact(pureModel, (Root_meta_external_function_activator_snowflakeApp_SnowflakeApp) element, data, routerExtensions);
        try
        {
            LOGGER.info("Generating snowflake artifact for " + element.getName());
            String content = mapper.writeValueAsString(artifact);
            result.add((new Artifact(content, FILE_NAME, "json")));
            LOGGER.info("Generated snowflake artifact for " + element.getName());

        }
        catch (Exception e)
        {
            LOGGER.error("Error generating snowflake artifact for " + element.getName() + " reason: " + e.getMessage());
        }
        return result;

    }

}
