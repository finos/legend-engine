// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.postgres.e2e;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class TestAstFromRewriter
{
    private static final Set<String> TABLES = Set.of("persons", "departments");

    private static String rewrite(ParityPath path, String sql)
    {
        return new AstFromRewriter(path, TABLES).rewrite(sql);
    }

    @Test
    public void tdsFormUsesTdsFunction()
    {
        Assertions.assertTrue(rewrite(ParityPath.TDS, "SELECT * FROM persons").contains("func('e2e::tds_persons')"));
    }

    @Test
    public void relationFormUsesRelationFunction()
    {
        Assertions.assertTrue(rewrite(ParityPath.RELATION, "SELECT * FROM persons").contains("func('e2e::rel_persons')"));
    }

    @Test
    public void grammarFormUsesMangledRelationFunction()
    {
        String sql = rewrite(ParityPath.GRAMMAR, "SELECT p.name FROM persons p JOIN departments d ON p.id = d.id");
        Assertions.assertTrue(sql.contains("func('e2e::rel_persons__Relation_1_')"), sql);
        Assertions.assertTrue(sql.contains("func('e2e::rel_departments__Relation_1_')"), sql);
    }

    @Test
    public void grammarFormLeavesCteReferencesAlone()
    {
        String sql = rewrite(ParityPath.GRAMMAR, "WITH persons_cte AS (SELECT * FROM persons) SELECT * FROM persons_cte");
        Assertions.assertTrue(sql.contains("persons_cte"), sql);
        Assertions.assertEquals(1, sql.split("func\\(", -1).length - 1, sql);
    }

    @Test
    public void joinFunctionIsMangledOnlyForGrammar()
    {
        Assertions.assertEquals("func('e2e::tds_person_with_dept')", ParityPath.TDS.joinFunctionCall());
        Assertions.assertEquals("func('e2e::rel_person_with_dept')", ParityPath.RELATION.joinFunctionCall());
        Assertions.assertEquals("func('e2e::rel_person_with_dept__Relation_1_')", ParityPath.GRAMMAR.joinFunctionCall());
    }

    @Test
    public void labelsRoundTrip()
    {
        for (ParityPath path : ParityPath.values())
        {
            Assertions.assertEquals(path, ParityPath.fromLabel(path.label()));
        }
    }
}
