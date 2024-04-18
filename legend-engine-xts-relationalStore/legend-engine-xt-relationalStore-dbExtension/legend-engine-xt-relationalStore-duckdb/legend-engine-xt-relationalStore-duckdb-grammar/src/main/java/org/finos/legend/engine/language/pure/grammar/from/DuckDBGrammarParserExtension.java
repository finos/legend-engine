// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DuckDBParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DuckDBDatasourceSpecification;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DuckDBLexerGrammar;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class DuckDBGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "DuckDB");
    }
    
    @Override
    public List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.singletonList(code -> null);
    }

    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("DuckDB".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, DuckDBLexerGrammar::new, DuckDBParserGrammar::new,
                        p -> visitDuckDBDatasourceSpecification(code, p.duckDBDatasourceSpecification()));
            }
            return null;
        });
    }

    public DuckDBDatasourceSpecification visitDuckDBDatasourceSpecification(DataSourceSpecificationSourceCode code, DuckDBParserGrammar.DuckDBDatasourceSpecificationContext dbSpecCtx)
    {
        DuckDBDatasourceSpecification dsSpec = new DuckDBDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();

        DuckDBParserGrammar.PathContext pathCtx = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.path(), "path", dsSpec.sourceInformation);
        dsSpec.path = pathCtx != null ? PureGrammarParserUtility.fromGrammarString(pathCtx.STRING().getText(), true) : null;

        return dsSpec;
    }
}
