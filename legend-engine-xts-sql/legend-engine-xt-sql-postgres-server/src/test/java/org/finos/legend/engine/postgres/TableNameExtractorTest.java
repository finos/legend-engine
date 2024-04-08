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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.protocol.sql.metamodel.QualifiedName;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TableNameExtractorTest
{
    @Test
    public void testGetSchemaAndTable()
    {
        test("SELECT * FROM schema1.table1", Lists.fixedSize.of("schema1.table1"), new TableNameExtractor());
    }

    @Test
    public void testSetQuery()
    {
        test("SET A=B", Lists.fixedSize.empty(), new TableNameExtractor());
    }

    @Test
    public void testSelectWithoutTable()
    {
        test("SELECT 1", FastList.newList(), new TableNameExtractor());
    }

    @Test
    public void testExtractingDifferentTypes()
    {
        test("SELECT * FROM service('/my/service') UNION SELECT * from myTable", Lists.fixedSize.of("service", "myTable"), new TableNameExtractor());
        test("SELECT * FROM service('/my/service') UNION SELECT * from myTable", Lists.fixedSize.of("myTable"), new TableNameExtractor(true, false));
        test("SELECT * FROM service('/my/service') UNION SELECT * from myTable", Lists.fixedSize.of("service"), new TableNameExtractor(false, true));
        test("SELECT * FROM service('/my/service') UNION SELECT * from myTable", Lists.fixedSize.empty(), new TableNameExtractor(false, false));
    }

    private void test(String sql, List<String> expected, TableNameExtractor extractor)
    {
        SqlBaseParser parser = SQLGrammarParser.getSqlBaseParser(sql, "query");
        List<QualifiedName> qualifiedNames = parser.singleStatement().accept(extractor);

        List<String> result = ListIterate.collect(qualifiedNames, q -> StringUtils.join(q.parts, "."));

        assertEquals(expected.size(), result.size());
        assertTrue(ListIterate.allSatisfy(expected, result::contains));
    }
}