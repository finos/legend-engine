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

package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.MemSqlDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class MemSqlGrammarComposerExtension implements IRelationalGrammarComposerExtension
{
    @Override
    public List<Function2<DatasourceSpecification, PureGrammarComposerContext, String>> getExtraDataSourceSpecificationComposers()
    {
        return Lists.mutable.with((_spec, context) ->
        {
            if (_spec instanceof MemSqlDatasourceSpecification)
            {
                MemSqlDatasourceSpecification spec = (MemSqlDatasourceSpecification) _spec;
                int baseIndentation = 1;
                return "MemSql\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "host: " + convertString(spec.host, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "port: " + convertString(String.valueOf(spec.port), true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "databaseName: " + convertString(spec.databaseName, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "useSsl: " + convertString(String.valueOf(spec.useSsl), true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            return null;
        });
    }
}
