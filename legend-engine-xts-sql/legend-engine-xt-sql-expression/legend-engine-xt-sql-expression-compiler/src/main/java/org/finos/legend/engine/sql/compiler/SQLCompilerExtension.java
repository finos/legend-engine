// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.sql.compiler;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.sql.expression.protocol.SQLExpressionProtocol;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_expression_SQLExpression_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.core_external_query_sql_binding_fromPure_fromPure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.Map;

public class SQLCompilerExtension implements CompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.mutable.empty();
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Query", "SQL");
    }

    @Override
    public Map<String, Function3<Object, CompileContext, ProcessingContext, ValueSpecification>> getExtraClassInstanceProcessors()
    {
        return Maps.mutable.with("SQL", (obj, context, processingContext) ->
                {
                    String sqlText = ((SQLExpressionProtocol)obj).sql;
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<? extends java.lang.Object> x = core_external_query_sql_binding_fromPure_fromPure.Root_meta_external_query_sql_transformation_queryToPure_sqlToPure_String_1__Function_1_(sqlText, context.getExecutionSupport());
                    org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType relationType = ((FunctionType)x._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._typeArguments().getFirst();

                    return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                            ._genericType(context.pureModel.getGenericType("meta::external::query::sql::expression::SQLExpression")._typeArguments(Lists.mutable.with(relationType)))
                            ._multiplicity(context.pureModel.getMultiplicity("one"))
                            ._values(
                                    Lists.mutable.with(
                                            new Root_meta_external_query_sql_expression_SQLExpression_Impl<Object>("", null, context.pureModel.getClass("meta::external::query::sql::expression::SQLExpression"))
                                                    ._sqlString(sqlText)
                                                    ._pureFunction(x)

                                    )
                            );
                }
        );
    }

    @Override
    public MutableMap<String, MutableSet<String>> getExtraSubtypesForFunctionMatching()
    {
        return Maps.mutable.with("cov_relation_Relation", Sets.mutable.with("SQLExpression"));
    }

    @Override
    public CompilerExtension build()
    {
        return new SQLCompilerExtension();
    }
}
