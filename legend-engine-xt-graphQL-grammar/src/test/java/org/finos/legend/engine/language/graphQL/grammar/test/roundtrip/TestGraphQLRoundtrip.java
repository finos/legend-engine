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
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLParserException;
import org.finos.legend.engine.language.graphQL.grammar.to.GraphQLGrammarComposer;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
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
    public void testParsingError()
    {
        try
        {
            GraphQLGrammarParser parser = GraphQLGrammarParser.newInstance();
            parser.parseDocument("type Car {\n" +
                    "}");
            Assert.fail();
        }
        catch (GraphQLParserException e)
        {
            Assert.assertEquals(1, e.getSourceInformation().startColumn);
            Assert.assertEquals(2, e.getSourceInformation().startLine);
            Assert.assertEquals("Unexpected token", e.getMessage());
        }
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
        check("enum Direction {\n" +
                "  NORTH\n" +
                "  SOUTH\n" +
                "  EAST\n" +
                "  WEST\n" +
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
                "fragment properties on Person {\n" +
                "  firstname(x: null, x: $ok)\n" +
                "}");
    }

    @Test
    public void testSchemaQueryRoundtrip()
    {
        check("query IntrospectionQuery {\n" +
                "  __schema {\n" +
                "    queryType {\n" +
                "      name\n" +
                "    }\n" +
                "    mutationType {\n" +
                "      name\n" +
                "    }\n" +
                "    subscriptionType {\n" +
                "      name\n" +
                "    }\n" +
                "    types {\n" +
                "      ... FullType\n" +
                "    }\n" +
                "    directives {\n" +
                "      name\n" +
                "      description\n" +
                "      locations\n" +
                "      args {\n" +
                "        ... InputValue\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n\n" +
                "fragment FullType on __Type {\n" +
                "  kind\n" +
                "  name\n" +
                "  description\n" +
                "  fields(includeDeprecated: true) {\n" +
                "    name\n" +
                "    description\n" +
                "    args {\n" +
                "      ... InputValue\n" +
                "    }\n" +
                "    type {\n" +
                "      ... TypeRef\n" +
                "    }\n" +
                "    isDeprecated\n" +
                "    deprecationReason\n" +
                "  }\n" +
                "  inputFields {\n" +
                "    ... InputValue\n" +
                "  }\n" +
                "  interfaces {\n" +
                "    ... TypeRef\n" +
                "  }\n" +
                "  enumValues(includeDeprecated: true) {\n" +
                "    name\n" +
                "    description\n" +
                "    isDeprecated\n" +
                "    deprecationReason\n" +
                "  }\n" +
                "  possibleTypes {\n" +
                "    ... TypeRef\n" +
                "  }\n" +
                "}\n\n" +
                "fragment InputValue on __InputValue {\n" +
                "  name\n" +
                "  description\n" +
                "  type {\n" +
                "    ... TypeRef\n" +
                "  }\n" +
                "  defaultValue\n" +
                "}\n\n" +
                "fragment TypeRef on __Type {\n" +
                "  kind\n" +
                "  name\n" +
                "  ofType {\n" +
                "    kind\n" +
                "    name\n" +
                "    ofType {\n" +
                "      kind\n" +
                "      name\n" +
                "      ofType {\n" +
                "        kind\n" +
                "        name\n" +
                "        ofType {\n" +
                "          kind\n" +
                "          name\n" +
                "          ofType {\n" +
                "            kind\n" +
                "            name\n" +
                "            ofType {\n" +
                "              kind\n" +
                "              name\n" +
                "              ofType {\n" +
                "                kind\n" +
                "                name\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
    }

    @Test
    public void testIntrospectionRoundTrip()
    {
        check("type __Schema {\n" +
                "  types: [__Type!]!\n" +
                "  queryType: __Type!\n" +
                "  mutationType: __Type\n" +
                "  subscriptionType: __Type\n" +
                "  directives: [__Directive!]!\n" +
                "}\n" +
                "\n" +
                "type __Type {\n" +
                "  kind: __TypeKind!\n" +
                "  name: String\n" +
                "  description: String\n" +
                "  fields(includeDeprecated: Boolean = false): [__Field!]\n" +
                "  interfaces: [__Type!]\n" +
                "  possibleTypes: [__Type!]\n" +
                "  enumValues(includeDeprecated: Boolean = false): [__EnumValue!]\n" +
                "  inputFields: [__InputValue!]\n" +
                "  ofType: __Type\n" +
                "}\n" +
                "\n" +
                "type __Field {\n" +
                "  name: String!\n" +
                "  description: String\n" +
                "  args: [__InputValue!]!\n" +
                "  type: __Type!\n" +
                "  isDeprecated: Boolean!\n" +
                "  deprecationReason: String\n" +
                "}\n" +
                "\n" +
                "type __InputValue {\n" +
                "  name: String!\n" +
                "  description: String\n" +
                "  type: __Type!\n" +
                "  defaultValue: String\n" +
                "}\n" +
                "\n" +
                "type __EnumValue {\n" +
                "  name: String!\n" +
                "  description: String\n" +
                "  isDeprecated: Boolean!\n" +
                "  deprecationReason: String\n" +
                "}\n" +
                "\n" +
                "enum __TypeKind {\n" +
                "  SCALAR\n" +
                "  OBJECT\n" +
                "  INTERFACE\n" +
                "  UNION\n" +
                "  ENUM\n" +
                "  INPUT_OBJECT\n" +
                "  LIST\n" +
                "  NON_NULL\n" +
                "}\n" +
                "\n" +
                "type __Directive {\n" +
                "  name: String!\n" +
                "  description: String\n" +
                "  locations: [__DirectiveLocation!]!\n" +
                "  args: [__InputValue!]!\n" +
                "}\n" +
                "\n" +
                "enum __DirectiveLocation {\n" +
                "  QUERY\n" +
                "  MUTATION\n" +
                "  SUBSCRIPTION\n" +
                "  FIELD\n" +
                "  FRAGMENT_DEFINITION\n" +
                "  FRAGMENT_SPREAD\n" +
                "  INLINE_FRAGMENT\n" +
                "  SCHEMA\n" +
                "  SCALAR\n" +
                "  OBJECT\n" +
                "  FIELD_DEFINITION\n" +
                "  ARGUMENT_DEFINITION\n" +
                "  INTERFACE\n" +
                "  UNION\n" +
                "  ENUM\n" +
                "  ENUM_VALUE\n" +
                "  INPUT_OBJECT\n" +
                "  INPUT_FIELD_DEFINITION\n" +
                "}");
    }

    protected void check(String value)
    {
        GraphQLGrammarParser parser = GraphQLGrammarParser.newInstance();
        Document document = parser.parseDocument(value);
        GraphQLGrammarComposer composer = GraphQLGrammarComposer.newInstance();
        String result = composer.renderDocument(document);
        Assert.assertEquals(value, result);
    }
}
