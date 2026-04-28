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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;

public class TestReturnTypeHelper
{
    private TestReturnTypeHelper()
    {
    }

    public static boolean isRelationReturnType(FunctionDefinition<?> functionDefinition, PureModel pureModel)
    {
        if (functionDefinition == null || pureModel == null)
        {
            return false;
        }
        try
        {
            ProcessorSupport processorSupport = pureModel.getExecutionSupport().getProcessorSupport();
            GenericType classifierGenericType = functionDefinition._classifierGenericType();
            if (classifierGenericType == null || classifierGenericType._typeArguments() == null || classifierGenericType._typeArguments().isEmpty())
            {
                return false;
            }
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type rawType = classifierGenericType._typeArguments().getFirst()._rawType();
            if (!(rawType instanceof FunctionType))
            {
                return false;
            }
            FunctionType functionType = (FunctionType) rawType;
            GenericType returnType = functionType._returnType();
            if (returnType == null || returnType._rawType() == null)
            {
                return false;
            }
            return processorSupport.type_subTypeOf(
                    returnType._rawType(),
                    processorSupport.package_getByUserPath(M3Paths.Relation)
            );
        }
        catch (Exception e)
        {
            return false;
        }
    }
}

