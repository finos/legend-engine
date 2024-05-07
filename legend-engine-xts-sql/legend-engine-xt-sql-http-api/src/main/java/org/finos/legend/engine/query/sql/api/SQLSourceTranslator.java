// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.protocol.sql.metamodel.ProtocolToMetamodelTranslator;
import org.finos.legend.engine.query.sql.providers.core.SQLSource;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;

import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.List;
import java.util.Objects;

public class SQLSourceTranslator
{
    public RichIterable<Root_meta_external_query_sql_transformation_queryToPure_SQLPlaceholderParameter> translate(List<SQLQueryParameter> parameters, PureModel pureModel)
    {
        return ListIterate.collect(parameters, p -> translate(p, pureModel));
    }

    public Root_meta_external_query_sql_transformation_queryToPure_SQLPlaceholderParameter translate(SQLQueryParameter parameter, PureModel pureModel)
    {
        Root_meta_external_query_sql_transformation_queryToPure_SQLPlaceholderParameter p = new Root_meta_external_query_sql_transformation_queryToPure_SQLPlaceholderParameter_Impl("");

        ValueSpecificationBuilder vsb = new ValueSpecificationBuilder(pureModel.getContext(), FastList.newList(), new ProcessingContext("build query parameter"));
        VariableExpression variable = (VariableExpression) parameter.getVariable().accept(vsb);

        ProtocolToMetamodelTranslator translator = new ProtocolToMetamodelTranslator();
        Root_meta_external_query_sql_metamodel_Expression value = translator.translate(parameter.getValue(), pureModel);

        p._value(value);
        p._variable(variable);

        return p;
    }

    public RichIterable<Root_meta_external_query_sql_transformation_queryToPure_SQLSource> translate(RichIterable<SQLSource> sources, PureModel pureModel)
    {
        return IterableIterate.collect(sources, s -> translate(s, pureModel));
    }

    private Root_meta_external_query_sql_transformation_queryToPure_SQLSource translate(SQLSource source, PureModel pureModel)
    {
        RichIterable<? extends Root_meta_external_query_sql_transformation_queryToPure_SQLSourceArgument> keys = ListIterate.collect(source.getKey(), this::translate);

        Root_meta_external_query_sql_transformation_queryToPure_SQLSource compiled = new Root_meta_external_query_sql_transformation_queryToPure_SQLSource_Impl("");

        compiled._type(source.getType());
        compiled._func(HelperValueSpecificationBuilder.buildLambda(source.getFunc(), pureModel.getContext()));

        if (source.getMapping() != null)
        {
            compiled._mapping(pureModel.getMapping(source.getMapping()));
        }

        if (source.getRuntime() != null)
        {
            compiled._runtime(HelperRuntimeBuilder.buildPureRuntime(source.getRuntime(), pureModel.getContext()));
        }

        if (source.getExecutionOptions() != null)
        {
            compiled._executionOptions(ListIterate.collect(source.getExecutionOptions(), e -> processExecutionOption(e, pureModel.getContext())));
        }

        if (source.getExecutionContext() != null)
        {
            compiled._executionContext(process(source.getExecutionContext(), pureModel.getContext()));
        }

        compiled._key(keys);

        return compiled;
    }

    private Root_meta_external_query_sql_transformation_queryToPure_SQLSourceArgument translate(SQLSourceArgument key)
    {
        Root_meta_external_query_sql_transformation_queryToPure_SQLSourceArgument compiled = new Root_meta_external_query_sql_transformation_queryToPure_SQLSourceArgument_Impl("");
        compiled._name(key.getName());

        if (key.getIndex() != null)
        {
            compiled._index(key.getIndex().longValue());
        }

        RichIterable<?> value = key.getValue() instanceof RichIterable ? (RichIterable<?>) key.getValue() : FastList.newListWith(key.getValue());
        compiled._value(value);

        return compiled;
    }

    private static Root_meta_pure_executionPlan_ExecutionOption processExecutionOption(ExecutionOption executionOption, CompileContext context)
    {
        return context.getCompilerExtensions().getExtraExecutionOptionProcessors().stream()
                .map(processor -> processor.value(executionOption, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported execution option type '" + executionOption.getClass() + "'"));
    }

    private Root_meta_pure_runtime_ExecutionContext process(ExecutionContext executionContext, CompileContext context)
    {
        return context.getCompilerExtensions().getExtraExecutionContextProcessors().stream()
                .map(processor -> processor.value(executionContext, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported execution option type '" + executionContext.getClass() + "'"));
    }
}
