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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRelationValidation;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_dataquality_generation_dataquality;
import org.finos.legend.pure.generated.core_external_format_json_toJSON;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class DataQualityLambdaGenerator
{

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateLambda(PureModel pureModel, String qualifiedPath, String validationName, Boolean runQuery, Integer resultLimit)
    {
        PackageableElement packageableElement = pureModel.getPackageableElement(qualifiedPath);
        return generateLambda(pureModel, packageableElement, validationName, runQuery, resultLimit);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateLambda(PureModel pureModel, PackageableElement packageableElement, String validationName, Boolean runQuery, Integer resultLimit)
    {
        if (packageableElement instanceof Root_meta_external_dataquality_DataQuality)
        {
            return generateModelConstraintLambda(pureModel, null, (Root_meta_external_dataquality_DataQuality) packageableElement);
        }
        else if (packageableElement instanceof Root_meta_external_dataquality_DataQualityRelationValidation)
        {
            return generateRelationValidationLambda(pureModel, (Root_meta_external_dataquality_DataQualityRelationValidation) packageableElement, validationName, runQuery, resultLimit);
        }
        throw new EngineException("Unsupported Dataquality element! " + packageableElement.getClass().getSimpleName(), ExceptionCategory.USER_EXECUTION_ERROR);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateLambdaForTrial(PureModel pureModel, String qualifiedPath, Integer queryLimit, String validationName, Boolean runQuery)
    {
        PackageableElement packageableElement = pureModel.getPackageableElement(qualifiedPath);
        if (packageableElement instanceof Root_meta_external_dataquality_DataQuality)
        {
            return generateModelConstraintLambda(pureModel, queryLimit, (Root_meta_external_dataquality_DataQuality) packageableElement);
        }
        else if (packageableElement instanceof Root_meta_external_dataquality_DataQualityRelationValidation)
        {
            return generateRelationValidationLambda(pureModel, (Root_meta_external_dataquality_DataQualityRelationValidation) packageableElement, validationName, runQuery, queryLimit);
        }
        throw new EngineException("Unsupported Dataquality element! " + packageableElement.getClass().getSimpleName(), ExceptionCategory.USER_EXECUTION_ERROR);
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction generateModelConstraintLambda(PureModel pureModel, Integer queryLimit, Root_meta_external_dataquality_DataQuality packageableElement)
    {
        return core_dataquality_generation_dataquality.Root_meta_external_dataquality_generateDataQualityQuery_DataQuality_1__Integer_$0_1$__LambdaFunction_1_(packageableElement, Objects.isNull(queryLimit) ? null : queryLimit.longValue(), pureModel.getExecutionSupport());
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateRelationValidationLambda(PureModel pureModel, Root_meta_external_dataquality_DataQualityRelationValidation packageableElement, String validationName, Boolean runQuery, Integer resultLimit)
    {
        if (Boolean.TRUE.equals(runQuery))
        {
            return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object>) packageableElement._query();
        }
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object>) core_dataquality_generation_dataquality.Root_meta_external_dataquality_generateDataqualityRelationValidationLambda_DataQualityRelationValidation_1__String_1__Integer_$0_1$__LambdaFunction_1_(packageableElement, validationName, Objects.isNull(resultLimit) ? null : resultLimit.longValue(), pureModel.getExecutionSupport());
    }

    public static LambdaFunction transformLambda(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> lambda, PureModel pureModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions)
    {
        Object protocol = transformToVersionedModel(lambda, PureClientVersions.production, extensions.apply(pureModel), pureModel.getExecutionSupport());
        return transform(protocol, LambdaFunction.class, pureModel);
    }

    public static String transformLambdaAsJson(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> lambda, PureModel pureModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions)
    {
        Object protocol = transformToVersionedModel(lambda, "vX_X_X", extensions.apply(pureModel), pureModel.getExecutionSupport());
        return transform(protocol, pureModel);
    }

    public static Object transformToVersionedModel(FunctionDefinition<?> lambda, String version, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, ExecutionSupport executionSupport)
    {
        try
        {
            Class cl = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + version + "_transfers_valueSpecification");
            Method method = cl.getMethod("Root_meta_protocols_pure_"
                            + version
                            + (PureClientVersions.versionAGreaterThanVersionB(version, "v1_33_0") ?
                            "_transformation_fromPureGraph_transformLambda_FunctionDefinition_1__Extension_MANY__LambdaFunction_1_" :
                            "_transformation_fromPureGraph_transformLambda_FunctionDefinition_1__Extension_MANY__Lambda_1_"),
                    FunctionDefinition.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
            return method.invoke(null, lambda, extensions, executionSupport);
        }
        catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static <T> T transform(Object object, java.lang.Class<T> clazz, PureModel pureModel)
    {
        String json = serializeToJSON(object, pureModel, true);
        try
        {
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(json, clazz);
        }
        catch (Exception e)
        {
            throw new EngineException(e.getMessage());
        }
    }

    private static String transform(Object object, PureModel pureModel)
    {
        return serializeToJSON(object, pureModel, true);
    }

    private static String serializeToJSON(Object pureObject, PureModel pureModel, Boolean alloyJSON)
    {
        return alloyJSON
                ? org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(pureObject, pureModel.getExecutionSupport())
                : core_external_format_json_toJSON.Root_meta_json_toJSON_Any_MANY__Integer_$0_1$__Config_1__String_1_(
                org.eclipse.collections.api.factory.Lists.mutable.with(pureObject),
                1000L,
                core_external_format_json_toJSON.Root_meta_json_config_Boolean_1__Boolean_1__Boolean_1__Boolean_1__Config_1_(true, false, false, false, pureModel.getExecutionSupport()),
                pureModel.getExecutionSupport()
        );
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateRelationValidationMainQueryRowCount(PureModel pureModel, String qualifiedPath)
    {
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object>) core_dataquality_generation_dataquality.Root_meta_external_dataquality_generateMainQueryCountLambda_DataQualityRelationValidation_1__LambdaFunction_1_((Root_meta_external_dataquality_DataQualityRelationValidation) pureModel.getPackageableElement(qualifiedPath), pureModel.getExecutionSupport());
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object> generateRelationValidationMainQueryRowCount(PureModel pureModel, Root_meta_external_dataquality_DataQualityRelationValidation packageableElement)
    {
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<Object>) core_dataquality_generation_dataquality.Root_meta_external_dataquality_generateMainQueryCountLambda_DataQualityRelationValidation_1__LambdaFunction_1_(packageableElement, pureModel.getExecutionSupport());
    }

}
