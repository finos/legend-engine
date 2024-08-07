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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction1;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.*;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.RowContainer;

import java.util.List;

public class RelationExtensionCompiled implements CompiledExtension
{
    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(
                new Map(),
                new Limit(),
                new Size(),
                new Filter(),
                new Columns(),
                new Concatenate(),
                new Join(),
                new Extend(),
                new ExtendArray(),
                new ExtendAgg(),
                new ExtendAggArray(),
                new ExtendWindowAgg(),
                new ExtendWindowAggArray(),
                new ExtendWindowFunc(),
                new ExtendWindowFuncArray(),
                new First(),
                new Last(),
                new Drop(),
                new Sort(),
                new Rename(),
                new Project(),
                new GroupBy(),
                new GroupByArray(),
                new Slice(),
                new Distinct(),
                new Select(),
                new SelectArray(),
                new Offset()
        );
    }

    @Override
    public String getRelatedRepository()
    {
        return "core_functions_relation";
    }

    public static CompiledExtension extension()
    {
        return new RelationExtensionCompiled();
    }

    @Override
    public Function3<CoreInstance, CoreInstance, ProcessorContext, String> getExtraFunctionGeneration()
    {
        return (CoreInstance function, CoreInstance functionExpression, ProcessorContext processorContext) ->
        {
            if (processorContext.getSupport().instance_instanceOf(function, M3Paths.Column))
            {
                CoreInstance firstParam = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport()).getFirst();
                String processedOwnerInstance = ValueSpecificationProcessor.processValueSpecification(null, firstParam, processorContext);

                ProcessorSupport processorSupport = processorContext.getSupport();
                CoreInstance nativeFunction = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.func, processorSupport);
                CoreInstance functionType = processorSupport.function_getFunctionType(nativeFunction);
                String returnType = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), true, processorSupport);

                return "(" + returnType + ")((org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.RowContainer)" + processedOwnerInstance + ").apply(\"" + Instance.getValueForMetaPropertyToOneResolved(function, M3Properties.name, processorContext.getSupport()).getName() + "\")";
            }
            return null;
        };
    }

    @Override
    public PureFunction1<Object, Object> getExtraFunctionEvaluation(Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        if (func instanceof Column)
        {
            return (o, executionSupport) -> ((RowContainer) o).apply(func._name());
        }
        return null;
    }
}
