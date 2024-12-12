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

package org.finos.legend.engine.language.pure.compiler;

import java.io.IOException;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModelProcessParameter;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.RelationTypeHelper;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.type.GenericType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_protocols_pure_vX_X_X_metamodel_type_GenericType;
import org.finos.legend.pure.generated.core_pure_protocol_protocol;
import org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_metamodel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;

public class Compiler
{
    public static PureModel compile(PureModelContextData model, DeploymentMode deploymentMode, String user)
    {
        return compile(model, deploymentMode, user, (String) null, null);
    }

    public static PureModel compile(PureModelContextData model, DeploymentMode deploymentMode, String user, String packageOffset)
    {
        return compile(model, deploymentMode, user, packageOffset, null);
    }

    public static PureModel compile(PureModelContextData model, DeploymentMode deploymentMode, String user, Metadata metaData, PureModelProcessParameter pureModelProcessParameter)
    {
        return new PureModel(model, user, deploymentMode, pureModelProcessParameter, metaData);
    }

    public static PureModel compile(PureModelContextData model, DeploymentMode deploymentMode, String user, String packageOffset, Metadata metaData)
    {
        PureModelProcessParameter pureModelProcessParameter = new PureModelProcessParameter(packageOffset);
        return new PureModel(model, user, deploymentMode, pureModelProcessParameter, metaData);
    }

    public static ValueSpecification getLambdaRawType(Lambda lambda, PureModel pureModel)
    {
        return HelperValueSpecificationBuilder.buildLambdaWithContext(lambda.body, lambda.parameters, new CompileContext.Builder(pureModel).build(), new ProcessingContext("Processing return type for lambda"))._expressionSequence().getLast();
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType<?> buildLambdaRelationType(Lambda lambda, PureModel pureModel)
    {
        ValueSpecification valueSpecification = getLambdaRawType(lambda, pureModel);
        Type type = valueSpecification._genericType()._typeArguments().getFirst()._rawType();
        Assert.assertTrue(type instanceof RelationType, () -> "Relation type expected in lambda");
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType<?>) type;
    }

    public static org.finos.legend.engine.protocol.pure.v1.model.type.relationType.RelationType getLambdaRelationType(Lambda lambda, PureModel pureModel)
    {
        return RelationTypeHelper.convert(buildLambdaRelationType(lambda, pureModel));
    }

    public static String getLambdaReturnType(Lambda lambda, PureModel pureModel)
    {
        ValueSpecification valueSpecification = getLambdaRawType(lambda, pureModel);
        return HelperModelBuilder.getTypeFullPath(valueSpecification._genericType()._rawType(), pureModel.getExecutionSupport());
    }

    public static GenericType getLambdaReturnGenericType(Lambda lambda, PureModel pureModel)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType genericType = getLambdaRawType(lambda, pureModel)._genericType();
        Root_meta_protocols_pure_vX_X_X_metamodel_type_GenericType protocolGenericType = core_pure_protocol_vX_X_X_transfers_metamodel.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_domain_transformGenericType_GenericType_1__GenericType_1_(genericType, pureModel.getExecutionSupport());
        String json = core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(protocolGenericType, pureModel.getExecutionSupport());
        try
        {
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(json, GenericType.class);
        }
        catch (IOException e)
        {
            throw new UnsupportedOperationException(e);
        }
    }
}
