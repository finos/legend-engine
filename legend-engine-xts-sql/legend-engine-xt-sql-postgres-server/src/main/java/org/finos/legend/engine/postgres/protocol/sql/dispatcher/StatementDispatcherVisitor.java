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

package org.finos.legend.engine.postgres.protocol.sql.dispatcher;

import java.util.List;

import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParserBaseVisitor;
import org.finos.legend.engine.postgres.PostgresServerException;
import org.finos.legend.engine.protocol.sql.metamodel.QualifiedName;

public class StatementDispatcherVisitor extends SqlBaseParserBaseVisitor<ExecutionType>
{
    private static final TableNameExtractor EXTRACTOR = new TableNameExtractor();

    @Override
    public ExecutionType visitBegin(SqlBaseParser.BeginContext ctx)
    {
        return ExecutionType.Empty;
    }

    @Override
    public ExecutionType visitSet(SqlBaseParser.SetContext ctx)
    {
        // TODO: Handle set queries instead of returning empty result set
        return ExecutionType.Empty;
    }

    @Override
    public ExecutionType visitShowTransaction(SqlBaseParser.ShowTransactionContext ctx)
    {
        return ExecutionType.TX;
    }

    @Override
    public ExecutionType visitShowSessionParameter(SqlBaseParser.ShowSessionParameterContext ctx)
    {
        return ExecutionType.Metadata;
    }

    @Override
    public ExecutionType visitDefault(SqlBaseParser.DefaultContext ctx)
    {
        List<QualifiedName> qualifiedNames = ctx.accept(EXTRACTOR);
        boolean isMetadataQuery = qualifiedNames.isEmpty() || qualifiedNames.stream().flatMap(i -> i.parts.stream()).anyMatch(SystemSchemas::contains);
        if (isMetadataQuery)
        {
            return ExecutionType.Metadata;
        }
        else
        {
            return ExecutionType.Legend;
        }
    }

    @Override
    protected ExecutionType aggregateResult(ExecutionType aggregate, ExecutionType nextResult)
    {
        if (aggregate != null)
        {
            if (nextResult == null)
            {
                return aggregate;
            }
            else
            {
                if (nextResult == aggregate)
                {
                    return aggregate;
                }
                else
                {
                    throw new PostgresServerException("Conflicting handlers for query");
                }
            }
        }
        else
        {
            return nextResult;
        }
    }
}
