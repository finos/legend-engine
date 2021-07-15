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

package org.finos.legend.engine.language.graphQL.grammar.test.roundtrip;

import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.graphQL.grammar.to.GraphQLGrammarComposer;
import org.finos.legend.engine.protocol.graphQL.v1.Document;
import org.junit.Assert;
import org.junit.Test;

public class TestGraphQLRoundtrip
{
    @Test
    public void testTypeRoundtrip()
    {
        check("type Car implements Vehicle & X & Z {\n" +
                "  id: ID!\n" +
                "  name: String!\n" +
                "  values: [String]\n" +
                "  length(unit: LengthUnit = METER): Float\n" +
                "}");
    }

    @Test
    public void testInterfaceRoundtrip()
    {
        check("interface Car {\n" +
                "  id: ID!\n" +
                "  name: String!\n" +
                "  values: [String]\n" +
                "  length(unit: LengthUnit = METER): Float\n" +
                "}");
    }

    @Test
    public void testScalarRoundtrip()
    {
        check("scalar MyScalar");
    }

    @Test
    public void testUnionRoundtrip()
    {
        check("union Vehicle = Car | Bike");
    }

    @Test
    public void testEnumRoundtrip()
    {
        check("enum Direction {\n"+
                    "  NORTH,\n"+
                    "  SOUTH,\n"+
                    "  EAST,\n"+
                    "  WEST\n"+
                    "}");
    }

    @Test
    public void testDirectiveDefinitionRoundtrip()
    {
        check("directive @doc(value: String) on UNION | FIELD\n\n" +
                    "directive @cool on UNION | FIELD");
    }

    @Test
    public void testSchemaDefinitionRoundtrip()
    {
        check("schema {\n" +
                "  query : MyQueryType\n" +
                "  mutation : MyOwn\n" +
                "}");
    }

    @Test
    public void testQueryRoundtrip()
    {
        check("query getUserWithProjects($a: INT = 1) {\n" +
                "  user(id: 2) {\n" +
                "    firstname(x: null, x: $ok)\n" +
                "    lastname(test: true)\n" +
                "    projects(other: \"oo\") {\n" +
                "      name\n" +
                "      tasks(val: en) {\n" +
                "        description(p: 3.2, z: [1, 2, \"ok\"])\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testMutationRoundtrip()
    {
        check("mutation setUserWithProjects($a: INT) {\n" +
                "  user(id: 2) {\n" +
                "    firstname(x: null, x: $ok)\n" +
                "    lastname(test: true)\n" +
                "    projects(other: \"oo\") {\n" +
                "      name\n" +
                "      tasks(val: en) {\n" +
                "        description(p: 3.2, z: [1, 2, \"ok\"])\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testSubscriptionRoundtrip()
    {
        check("subscription sub($a: BLA) {\n" +
                "  user(id: 2) {\n" +
                "    firstname(x: null, x: $ok)\n" +
                "  }\n" +
                "}");
    }


    @Test
    public void testFragmentRoundtrip()
    {
        check("subscription sub($a: BLA) {\n" +
                "  user(id: 2) {\n" +
                "    ... properties\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "fragment properties {\n" +
                "  firstname(x: null, x: $ok)\n" +
                "}");
    }

    private void check(String value)
    {
        GraphQLGrammarParser parser = GraphQLGrammarParser.newInstance();
        Document document = parser.parseDocument(value);
        GraphQLGrammarComposer composer = GraphQLGrammarComposer.newInstance();
        String result = composer.renderDocument(document);
        Assert.assertEquals(value, result);
    }
}
