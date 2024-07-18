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

package org.finos.legend.engine.generation.dataquality;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.dataquality.model.DataQualityMetadata;
import org.finos.legend.engine.protocol.dataquality.model.DataQualityRule;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRule;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_dataquality_generation_dataquality;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.slf4j.Logger;

import java.util.List;

public class DataQualityValidationArtifactGenerationExtension implements ArtifactGenerationExtension
{
    private final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataQualityValidationArtifactGenerationExtension.class);
    private final ObjectMapper mapper = ObjectMapperFactory.withStandardConfigurations(PureProtocolObjectMapperFactory.withPureProtocolExtensions(new ObjectMapper()));

    public static final String ROOT_PATH = "dataQualityValidation";
    public static final String META_DATA_FILE_NAME = "dataQualityRulesMetadata.json";
    public static final String EXECUTION_PLAN_FILE_NAME = "dataQualityValidationExecutionPlan.json";


    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
       return element instanceof Root_meta_external_dataquality_DataQuality
                && ((Root_meta_external_dataquality_DataQuality) element)._validationTree() != null;
    }

    @Override
    public List<Artifact> generate(PackageableElement packageableElement, PureModel pureModel, PureModelContextData pureModelContextData, String clientVersion)
    {
        List<Artifact> artifacts = Lists.mutable.empty();

        try
        {
            RichIterable<? extends Root_meta_external_dataquality_DataQualityRule> dataQualityRules =
                    core_dataquality_generation_dataquality.Root_meta_external_dataquality_generateDQMetaDataForDQValidation_DataQuality_1__DataQualityRule_MANY_((Root_meta_external_dataquality_DataQuality)packageableElement, pureModel.getExecutionSupport());
            List<DataQualityRule> dataQualityRuleList = Lists.mutable.withAll(dataQualityRules).collect(d -> new DataQualityRule(d._constraintName(), d._constraintType(), d._constraintGrammar(), d._propertyPath()));
            DataQualityMetadata dataQualityMetadata = new DataQualityMetadata();
            dataQualityMetadata.dqRules = dataQualityRuleList;
            artifacts.add(new Artifact(mapper.writeValueAsString(dataQualityMetadata), META_DATA_FILE_NAME, "json"));

            Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));
            // 1. call DQ PURE func to generate lambda
            LambdaFunction dqLambdaFunction = DataQualityLambdaGenerator.generateLambda(pureModel, packageableElement);
            // 2. Generate Plan from the lambda generated in the previous step
            SingleExecutionPlan singleExecutionPlan = PlanGenerator.generateExecutionPlan(dqLambdaFunction, null, null, null, pureModel, clientVersion, PlanPlatform.JAVA, null,
                    routerExtensions.apply(pureModel), LegendPlanTransformers.transformers);// since lambda has from function we dont need mapping, runtime and context
            artifacts.add(new Artifact(mapper.writeValueAsString(singleExecutionPlan), EXECUTION_PLAN_FILE_NAME, "json"));
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to compute dataQuality validation artifact for dqValidation: " + packageableElement.getName() + ". Exception:" + e.getMessage());
        }
        return artifacts;
    }

}
