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

import com.google.common.collect.Iterables;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.protocol.sql.metamodel.QualifiedName;
import org.junit.Test;

import java.util.List;
import static org.junit.Assert.assertEquals;

public class TableNameExtractorTest
{
    private static final TableNameExtractor extractor = new TableNameExtractor();


    @Test
    public void testGetSchemaAndTable()
    {
        List<QualifiedName> qualifiedNames = getQualifiedNames("SELECT * FROM schema1.table1");
        assertEquals(1, qualifiedNames.size());
        QualifiedName qualifiedName = Iterables.getOnlyElement(qualifiedNames);
        assertEquals(Lists.mutable.of("schema1", "table1"), qualifiedName.parts);
    }

    @Test
    public void testSetQuery()
    {
        List<QualifiedName> qualifiedNames = getQualifiedNames("SET A=B");
        assertEquals(0, qualifiedNames.size());
    }

    @Test
    public void testSelectWithoutTable()
    {
        List<QualifiedName> qualifiedNames = getQualifiedNames("SELECT 1");
        assertEquals(0, qualifiedNames.size());
    }

    @Test
    public void testFunctionCall()
    {
        List<QualifiedName> qualifiedNames = getQualifiedNames("SELECT * FROM service('/my/service')");
        assertEquals(1, qualifiedNames.size());
        QualifiedName qualifiedName = Iterables.getOnlyElement(qualifiedNames);
        assertEquals(Lists.mutable.of("service"), qualifiedName.parts);
    }

    private static List<QualifiedName> getQualifiedNames(String query)
    {
        SqlBaseParser parser = SQLGrammarParser.getSqlBaseParser(query, "query");
        return parser.singleStatement().accept(extractor);
    }
}