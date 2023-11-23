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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.protocol.sql.metamodel.Statement;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.core.TableSourceArgument;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class TableSourceExtractorTest
{
    private static final TableSource TABLE_1 = new TableSource("service", FastList.newListWith(new TableSourceArgument(null, 0, "table1")));
    private static final TableSource TABLE_2 = new TableSource("service", FastList.newListWith(new TableSourceArgument(null, 0, "table2")));

    private static final TableSource TABLE_1_ARGS = new TableSource("service", FastList.newListWith(
            new TableSourceArgument(null, 0, "table1"),
            new TableSourceArgument("a", null, 1L),
            new TableSourceArgument("b", null, "abc"),
            new TableSourceArgument("c", null, true)));

    private void test(String sql, TableSource... expected)
    {
        TableSourceExtractor extractor = new TableSourceExtractor();
        SQLGrammarParser parser = SQLGrammarParser.newInstance();
        Statement statement = parser.parseStatement(sql);

        Set<TableSource> sources = extractor.visit(statement);
        Assert.assertEquals(Sets.mutable.with(expected), sources);
    }

    @Test
    public void testSimpleTable()
    {
        test("select * from service.table1", TABLE_1);
    }

    @Test
    public void testSimpleTableFunc()
    {
        test("select * from service('table1')", TABLE_1);
    }

    @Test
    public void testUnionDuplicate()
    {
        test("select * from service('table1') UNION select * from service.table1", TABLE_1);
    }

    @Test
    public void testUnionNoDuplicate()
    {
        test("select * from service('table1') UNION select * from service.table2", TABLE_1, TABLE_2);
    }

    @Test
    public void testUnionDuplicateArgs()
    {
        test("select * from service('table1', a => 1, b => 'abc', c => true) UNION select * from service('table1', a => 1, b => 'abc', c => true)", TABLE_1_ARGS);
    }

    @Test
    public void testUnionNoDuplicateArgs()
    {
        TableSource T1 = new TableSource("service", FastList.newListWith(
                new TableSourceArgument(null, 0, "table1"),
                new TableSourceArgument("a", null, 1L),
                new TableSourceArgument("b", null, "abc"),
                new TableSourceArgument("c", null, false)));

        test("select * from service('table1', a => 1, b => 'abc', c => true) UNION select * from service('table1', a => 1, b => 'abc', c => false)", TABLE_1_ARGS, T1);
    }

}
