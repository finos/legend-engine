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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.compiler;

import java.util.List;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.LiteralOrExpression;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_LiteralOrExpression;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_LiteralOrExpression_Impl;

public final class LiteralOrExpressionCompiler
{
    private final CompileContext context;

    public LiteralOrExpressionCompiler(CompileContext context)
    {
        this.context = context;
    }

    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_LiteralOrExpression<String> compileString(LiteralOrExpression<String> val)
    {
        return this.compile(val);
    }

    public RichIterable<Root_meta_external_store_elasticsearch_v7_metamodel_specification_LiteralOrExpression<String>> compileString(List<LiteralOrExpression<String>> val)
    {
        return ListAdapter.adapt(val).collect(this::compileString);
    }

    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_LiteralOrExpression<Long> compileLong(LiteralOrExpression<Long> val)
    {
        return this.compile(val);
    }

    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_LiteralOrExpression<Boolean> compileBoolean(LiteralOrExpression<Boolean> val)
    {
        return this.compile(val);
    }

    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_LiteralOrExpression<? extends Number> compileNumber(LiteralOrExpression<Number> val)
    {
        return this.compile(val);
    }

    private <T> Root_meta_external_store_elasticsearch_v7_metamodel_specification_LiteralOrExpression<T> compile(LiteralOrExpression<T> val)
    {
        if (val == null)
        {
            return null;
        }
        // todo handle T on classifier?
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type classifier = this.context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::LiteralOrExpression");
        return new Root_meta_external_store_elasticsearch_v7_metamodel_specification_LiteralOrExpression_Impl<T>("", null, classifier)
                ._classifierGenericType(context.pureModel.getGenericType(classifier))
                ._value(val.value)
                ._expression(val.expression);
    }
}
