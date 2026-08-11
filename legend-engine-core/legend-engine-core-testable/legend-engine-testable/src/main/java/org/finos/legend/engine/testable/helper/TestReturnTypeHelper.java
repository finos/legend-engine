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

package org.finos.legend.engine.testable.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.RelationTypeHelper;
import org.finos.legend.engine.protocol.pure.m3.relation.Column;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_protocols_pure_vX_X_X_metamodel_m3_type_generics_GenericType;
import org.finos.legend.pure.generated.core_pure_protocol_protocol;
import org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_metamodel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationTypeCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._RelationType;

import java.util.Collections;
import java.util.List;

public class TestReturnTypeHelper
{
    private TestReturnTypeHelper()
    {
    }

    public static org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType getReturnGenericType(FunctionDefinition<?> functionDefinition, PureModel pureModel)
    {
        GenericType metamodelReturnType = getReturnGenericTypeCoreInstance(functionDefinition, pureModel);
        if (metamodelReturnType == null)
        {
            return null;
        }
        try
        {
            Root_meta_protocols_pure_vX_X_X_metamodel_m3_type_generics_GenericType intermediate =
                    core_pure_protocol_vX_X_X_transfers_metamodel.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_domain_transformGenericType_GenericType_1__GenericType_1_(metamodelReturnType, pureModel.getExecutionSupport());
            String json = core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(intermediate, pureModel.getExecutionSupport());
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(json, org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType.class);
        }
        catch (JsonProcessingException e)
        {
            return null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static GenericType getReturnGenericTypeCoreInstance(FunctionDefinition<?> functionDefinition, PureModel pureModel)
    {
        if (functionDefinition == null || pureModel == null)
        {
            return null;
        }
        try
        {
            GenericType classifierGenericType = functionDefinition._classifierGenericType();
            if (classifierGenericType == null || classifierGenericType._typeArguments() == null || classifierGenericType._typeArguments().isEmpty())
            {
                return null;
            }
            Type rawType = classifierGenericType._typeArguments().getFirst()._rawType();
            if (!(rawType instanceof FunctionType))
            {
                return null;
            }
            GenericType returnType = ((FunctionType) rawType)._returnType();
            if (returnType == null || returnType._rawType() == null)
            {
                return null;
            }
            return returnType;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static boolean isRelationReturnType(FunctionDefinition<?> functionDefinition, PureModel pureModel)
    {
        GenericType returnType = getReturnGenericTypeCoreInstance(functionDefinition, pureModel);
        return returnType != null && isRelationSubtype(returnType._rawType(), pureModel);
    }

    public static List<Column> getRelationReturnColumns(FunctionDefinition<?> functionDefinition, PureModel pureModel)
    {
        List<Column> declared = extractColumnsFromRelationGenericType(getReturnGenericTypeCoreInstance(functionDefinition, pureModel), pureModel);
        if (!declared.isEmpty())
        {
            return declared;
        }
        return extractColumnsFromBody(functionDefinition, pureModel);
    }

    private static boolean isRelationSubtype(Type rawType, PureModel pureModel)
    {
        if (rawType == null || pureModel == null)
        {
            return false;
        }
        try
        {
            ProcessorSupport processorSupport = pureModel.getExecutionSupport().getProcessorSupport();
            return processorSupport.type_subTypeOf(rawType, processorSupport.package_getByUserPath(M3Paths.Relation));
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static List<Column> extractColumnsFromRelationGenericType(GenericType relationGenericType, PureModel pureModel)
    {
        try
        {
            if (relationGenericType == null || relationGenericType._typeArguments() == null || relationGenericType._typeArguments().isEmpty())
            {
                return Collections.emptyList();
            }
            Type rowType = relationGenericType._typeArguments().getFirst()._rawType();
            ProcessorSupport processorSupport = pureModel.getExecutionSupport().getProcessorSupport();
            if (!_RelationType.isRelationType(rowType, processorSupport))
            {
                return Collections.emptyList();
            }
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType<?> relationType = RelationTypeCoreInstanceWrapper.toRelationType(rowType);
            List<Column> columns = RelationTypeHelper.convert(relationType, pureModel).columns;
            return columns == null ? Collections.emptyList() : columns;
        }
        catch (Exception e)
        {
            return Collections.emptyList();
        }
    }

    private static List<Column> extractColumnsFromBody(FunctionDefinition<?> functionDefinition, PureModel pureModel)
    {
        try
        {
            if (functionDefinition._expressionSequence() == null || functionDefinition._expressionSequence().isEmpty())
            {
                return Collections.emptyList();
            }
            GenericType lastExprType = functionDefinition._expressionSequence().toList().getLast()._genericType();
            if (lastExprType == null || !isRelationSubtype(lastExprType._rawType(), pureModel))
            {
                return Collections.emptyList();
            }
            return extractColumnsFromRelationGenericType(lastExprType, pureModel);
        }
        catch (Exception e)
        {
            return Collections.emptyList();
        }
    }
}
