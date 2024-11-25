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
import org.finos.legend.engine.postgres.handler.txn.TxnIsolationSessionHandler;
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
        assertEmptySessionHandler("SET A=B");
    }

    @Test
    public void testSelectInformationSchema()
    {
        assertMetadataSessionHandler("SELECT * FROM information_schema.TABLES");
    }

    @Test
    public void testSelectPgCatalog()
    {
        assertMetadataSessionHandler("SELECT * FROM pg_catalog.schemata");
    }

    @Test
    public void testSelectConstant()
    {
        assertMetadataSessionHandler("SELECT 1");
    }

    @Test
    public void testSelectTableFunction()
    {
        assertDataSessionHandler("SELECT * FROM service('/testService')");
    }

    @Test
    public void testInformationSchemaJoins()
    {
        assertMetadataSessionHandler("SELECT n.nspname, c.relname, d.description " +
                "FROM pg_catalog.pg_namespace n, pg_catalog.pg_class c " +
                "LEFT JOIN pg_catalog.pg_description d ON(c.oid = d.objoid AND d.objsubid = 0) " +
                "LEFT JOIN pg_catalog.pg_class dc ON(d.classoid = dc.oid AND dc.relname = 'pg_class') " +
                "LEFT JOIN pg_catalog.pg_namespace dn ON (dn.oid = dc.relnamespace AND dn.nspname = 'pg_catalog')");
    }

    @Test
    public void testSelectTableName()
    {
        assertDataSessionHandler("SELECT * FROM service.\"/testService\"");
    }

    @Test
    public void testShowTxnLevel()
    {
        assertTxnIsoSessionHandler("SHOW TRANSACTION ISOLATION LEVEL");
        assertTxnIsoSessionHandler("SHOW transaction_isolation");
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

    private static void assertTxnIsoSessionHandler(String query)
    {
        SessionHandler sessionHandler = getSessionHandler(query);
        Assert.assertTrue(sessionHandler instanceof TxnIsolationSessionHandler);
    }

    private static void assertDataSessionHandler(String query)
    {
        SessionHandler sessionHandler = getSessionHandler(query);
        Assert.assertSame(dataSessionHandler, sessionHandler);
    }

    private static SessionHandler getSessionHandler(String query)
    {
        SqlBaseParser parser = SQLGrammarParser.getSqlBaseParser(query, "query");
        return parser.singleStatement().accept(dispatcher);
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