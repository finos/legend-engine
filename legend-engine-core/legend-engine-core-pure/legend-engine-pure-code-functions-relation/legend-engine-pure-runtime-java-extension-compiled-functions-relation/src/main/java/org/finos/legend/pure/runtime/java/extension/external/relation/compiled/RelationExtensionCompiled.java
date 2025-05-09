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

import java.util.List;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.cast.Cast;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.variant.FromJson;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction1;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.AsOfJoin;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Columns;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Concatenate;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.CumulativeDistribution;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.DenseRank;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Distinct;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.DistinctAll;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Drop;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Extend;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ExtendAgg;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ExtendAggArray;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ExtendArray;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ExtendWindowAgg;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ExtendWindowAggArray;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ExtendWindowFunc;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ExtendWindowFuncArray;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Filter;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.First;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.GroupBy;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.GroupByArray;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Join;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Last;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Limit;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Map;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.NTile;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Nth;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Offset;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.PercentRank;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Pivot;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.PivotArray;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Project;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.ProjectRelation;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Rank;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Rename;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.RowNumber;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Select;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.SelectAll;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.SelectArray;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Size;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Slice;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Sort;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Write;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.RowContainer;

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
                new AsOfJoin(),
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
                new ProjectRelation(),
                new GroupBy(),
                new GroupByArray(),
                new Pivot(),
                new PivotArray(),
                new Slice(),
                new Distinct(),
                new DistinctAll(),
                new Select(),
                new SelectArray(),
                new SelectAll(),
                new Offset(),
                new RowNumber(),
                new Rank(),
                new NTile(),
                new CumulativeDistribution(),
                new Nth(),
                new DenseRank(),
                new PercentRank(),
                new Write()
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
                CoreInstance returnGenericType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
                String returnType = TypeProcessor.typeToJavaObjectSingle(returnGenericType, true, processorSupport);

                String getValue = "((org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.RowContainer)" + processedOwnerInstance + ").apply(\"" + Instance.getValueForMetaPropertyToOneResolved(function, M3Properties.name, processorContext.getSupport()).getName() + "\")";

                if (returnGenericType.getValueForMetaPropertyToOne(M3Properties.rawType) == processorSupport.package_getByUserPath(M3Paths.Variant))
                {
                    getValue = FromJson.class.getCanonicalName() + ".fromJson((String)" + getValue + ",es)";
                }

                getValue = "(" + returnType + ")" + getValue;

                if (GenericType.testContainsExtendedPrimitiveTypes(returnGenericType, processorSupport))
                {
                    return "(" + returnType + ")" + Cast.buildRunnableForExtendedPrimitiveType(getValue, returnGenericType, null, processorSupport) + ".value()";
                }
                else
                {
                    return getValue;
                }
            }
            return null;
        };
    }

    @Override
    public PureFunction1<Object, Object> getExtraFunctionEvaluation(Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        if (func instanceof Column)
        {
            return (o, executionSupport) ->
            {
                Column<?, ?> column = (Column<?, ?>) func;
                Object colValue = ((RowContainer) o).apply(func._name());

                if (_Column.getColumnType(column)._rawType()._name().equals(M3Paths.Variant) && !(colValue instanceof Variant))
                {
                    colValue = FromJson.fromJson((String) colValue, es);
                }

                return colValue;
            };
        }
        return null;
    }
}
