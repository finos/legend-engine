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

package org.finos.legend.engine.postgres;

import java.util.List;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParserBaseVisitor;
import org.finos.legend.engine.postgres.handler.SessionHandler;
import org.finos.legend.engine.postgres.handler.empty.EmptySessionHandler;
import org.finos.legend.engine.postgres.handler.txn.TxnIsolationSessionHandler;
import org.finos.legend.engine.protocol.sql.metamodel.QualifiedName;

public class ExecutionDispatcher extends SqlBaseParserBaseVisitor<SessionHandler>
{
    private static final TableNameExtractor EXTRACTOR = new TableNameExtractor();
    private static final SessionHandler EMPTY_SESSION_HANDLER = new EmptySessionHandler();
    private static final SessionHandler TXN_ISOLATION_HANDLER = new TxnIsolationSessionHandler();
    private final SessionHandler dataSessionHandler;
    private final SessionHandler metaDataSessionHandler;

    public ExecutionDispatcher(SessionHandler dataSessionHandler, SessionHandler metaDataSessionHandler)
    {
        this.dataSessionHandler = dataSessionHandler;
        this.metaDataSessionHandler = metaDataSessionHandler;
    }

    @Override
    public SessionHandler visitBegin(SqlBaseParser.BeginContext ctx)
    {
        return EMPTY_SESSION_HANDLER;
    }

    @Override
    public SessionHandler visitSet(SqlBaseParser.SetContext ctx)
    {
        // TODO: Handle set queries instead of returning empty result set
        return EMPTY_SESSION_HANDLER;
    }

    @Override
    public SessionHandler visitShowTransaction(SqlBaseParser.ShowTransactionContext ctx)
    {
        return TXN_ISOLATION_HANDLER;
    }

    /**
     * Visit the <code>SELECT</code> query context.
     * Select query gets the name default from the antlr definition
     *
     * @param ctx the parse tree
     * @return the session handler responsible for handling given query
     */
    @Override
    public SessionHandler visitDefault(SqlBaseParser.DefaultContext ctx)
    {
        List<QualifiedName> qualifiedNames = ctx.accept(EXTRACTOR);
        boolean isMetadataQuery = qualifiedNames.isEmpty() || qualifiedNames.stream().flatMap(i -> i.parts.stream()).anyMatch(SystemSchemas::contains);
        if (isMetadataQuery)
        {
            return metaDataSessionHandler;
        }
        else
        {
            return dataSessionHandler;
        }
    }

    @Override
    protected SessionHandler aggregateResult(SessionHandler aggregate, SessionHandler nextResult)
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

    public static SessionHandler getEmptySessionHandler()
    {
        return EMPTY_SESSION_HANDLER;
    }

}
