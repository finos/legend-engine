// Copyright 2026 Goldman Sachs
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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRelationComparison;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_MD5HashStrategy;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_ReconStrategy;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_datarecon_DataQualityReconInput;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_dataquality_generation_datarecon;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.slf4j.Logger;

import java.util.List;
import java.util.ServiceLoader;

public class DataQualityRelationComparisonArtifactGenerationExtension implements ArtifactGenerationExtension
{
    private final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DataQualityRelationComparisonArtifactGenerationExtension.class);
    private final ObjectMapper mapper = ObjectMapperFactory.withStandardConfigurations(PureProtocolObjectMapperFactory.withPureProtocolExtensions(new ObjectMapper()));

    public static final String ROOT_PATH = "dataQualityRelationComparison";
    public static final String EXECUTION_PLAN_FILE_NAME = "dataQualityRelationComparisonPlan.json";

    // Canonical defaults - MUST match DataQualityExecute.reconciliation cache-eligibility gate.
    public static final boolean INCLUDE_COLUMN_VALUES = true;
    public static final boolean ENRICH_DQ_COLUMNS = true;
    public static final Long DEFAULT_DEFECT_LIMIT = 8000L;

    private static final ListIterable<PlanTransformer> transformers = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class)).flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

    @Override
    public String getKey()
    {
        return ROOT_PATH;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        return element instanceof Root_meta_external_dataquality_DataQualityRelationComparison;
    }

    @Override
    public List<Artifact> generate(PackageableElement packageableElement, PureModel pureModel, PureModelContextData pureModelContextData, String clientVersion)
    {
        List<Artifact> artifacts = Lists.mutable.empty();
        try
        {
            Root_meta_external_dataquality_DataQualityRelationComparison element = (Root_meta_external_dataquality_DataQualityRelationComparison) packageableElement;
            Root_meta_external_dataquality_ReconStrategy strategy = element._strategy();
            boolean aggregatedHash = strategy instanceof Root_meta_external_dataquality_MD5HashStrategy && ((Root_meta_external_dataquality_MD5HashStrategy) strategy)._aggregatedHash();
            String sourceHash = strategy instanceof Root_meta_external_dataquality_MD5HashStrategy ? ((Root_meta_external_dataquality_MD5HashStrategy) strategy)._sourceHashColumn() : null;
            String targetHash = strategy instanceof Root_meta_external_dataquality_MD5HashStrategy ? ((Root_meta_external_dataquality_MD5HashStrategy) strategy)._targetHashColumn() : null;

            Root_meta_external_dataquality_datarecon_DataQualityReconInput reconInput = core_dataquality_generation_datarecon.Root_meta_external_dataquality_datarecon_createReconInput_LambdaFunction_1__LambdaFunction_1__String_MANY__Boolean_1__String_MANY__String_$0_1$__String_$0_1$__Boolean_1__Integer_$0_1$__Boolean_1__Boolean_1__DataQualityReconInput_1_(
                            element._source(), element._target(), element._keys(), aggregatedHash, element._columnsToCompare(), sourceHash, targetHash, INCLUDE_COLUMN_VALUES, DEFAULT_DEFECT_LIMIT, false, ENRICH_DQ_COLUMNS, pureModel.getExecutionSupport()
            );
            LambdaFunction<?> dqLambdaFunction = DataQualityReconLambdaGenerator.generateLambda(pureModel, reconInput);

            Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));
            SingleExecutionPlan singleExecutionPlan = PlanGenerator.generateExecutionPlan(dqLambdaFunction, null, null, null, pureModel, clientVersion, PlanPlatform.JAVA, null, routerExtensions.apply(pureModel), transformers);
            artifacts.add(new Artifact(mapper.writeValueAsString(singleExecutionPlan), EXECUTION_PLAN_FILE_NAME, "json"));
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to compute dataQualityRelationComparison plan artifact for element: " + packageableElement.getName() + ". Exception: " + e.getMessage(), e);
        }
        return artifacts;
    }
}
