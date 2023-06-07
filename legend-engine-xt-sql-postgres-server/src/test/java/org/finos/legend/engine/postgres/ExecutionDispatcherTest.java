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

import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.postgres.handler.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.handler.PostgresStatement;
import org.finos.legend.engine.postgres.handler.SessionHandler;
import org.finos.legend.engine.postgres.handler.empty.EmptySessionHandler;
import org.junit.Assert;
import org.junit.Test;

public class ExecutionDispatcherTest
{
    private static final SessionHandler dataSessionHandler = new TestSessionHandler();
    private static final SessionHandler metadataSessionHandler = new TestSessionHandler();
    private static final ExecutionDispatcher dispatcher = new ExecutionDispatcher(dataSessionHandler, metadataSessionHandler);

    @Test
    public void testSetQuery()
    {
        String query = "SET A=B";
        assertEmptySessionHandler(query);
    }

    @Test
    public void testSelectInformationSchema()
    {
        String query = "SELECT * FROM information_schema.TABLES";
        assertMetadataSessionHandler(query);
    }

    @Test
    public void testSelectPgCatalog()
    {
        String query = "SELECT * FROM pg_catalog.schemata";
        assertMetadataSessionHandler(query);
    }

    @Test
    public void testSelectConstant()
    {
        String query = "SELECT 1";
        assertMetadataSessionHandler(query);
    }

    @Test
    public void testSelectTableFunction()
    {
        String query = "SELECT * FROM service('/testService')";
        assertDataSessionHandler(query);
    }

    private static void assertEmptySessionHandler(String query)
    {
        SessionHandler sessionHandler = getSessionHandler(query);
        Assert.assertTrue(sessionHandler instanceof EmptySessionHandler);
    }

    private static void assertMetadataSessionHandler(String query)
    {
        SessionHandler sessionHandler = getSessionHandler(query);
        Assert.assertSame(metadataSessionHandler, sessionHandler);
    }

    private static void assertDataSessionHandler(String query)
    {
        SessionHandler sessionHandler = getSessionHandler(query);
        Assert.assertSame(dataSessionHandler, sessionHandler);
    }

    private static SessionHandler getSessionHandler(String query)
    {
        SqlBaseParser parser = SQLGrammarParser.getSqlBaseParser(query, "query");
        return dispatcher.visitSingleStatement(parser.singleStatement());
    }

    private static class TestSessionHandler implements SessionHandler
    {
        @Override
        public PostgresPreparedStatement prepareStatement(String query)
        {
            return null;
        }

        @Override
        public PostgresStatement createStatement()
        {
            return null;
        }
    }
}