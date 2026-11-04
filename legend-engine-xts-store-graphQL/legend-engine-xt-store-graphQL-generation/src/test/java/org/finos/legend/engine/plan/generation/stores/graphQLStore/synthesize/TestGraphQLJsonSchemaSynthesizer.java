// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.generation.stores.graphQLStore.synthesize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.generation.stores.graphQLStore.walker.GraphQLSchemaWalker;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionEnumValue;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionField;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class TestGraphQLJsonSchemaSynthesizer
{
    private final GraphQLSchemaWalker walker = new GraphQLSchemaWalker();
    private final GraphQLJsonSchemaSynthesizer synthesizer = new GraphQLJsonSchemaSynthesizer(walker);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static GraphQLIntrospectionType scalar(String name)
    {
        GraphQLIntrospectionType t = new GraphQLIntrospectionType();
        t.kind = "SCALAR";
        t.name = name;
        return t;
    }

    private static GraphQLIntrospectionType object(String name, GraphQLIntrospectionField... fields)
    {
        GraphQLIntrospectionType t = new GraphQLIntrospectionType();
        t.kind = "OBJECT";
        t.name = name;
        t.fields = Lists.mutable.with(fields);
        return t;
    }

    private static GraphQLIntrospectionField field(String name, GraphQLIntrospectionType type)
    {
        GraphQLIntrospectionField f = new GraphQLIntrospectionField();
        f.name = name;
        f.type = type;
        return f;
    }

    private static GraphQLIntrospectionType enumType(String name, String... values)
    {
        GraphQLIntrospectionType t = new GraphQLIntrospectionType();
        t.kind = "ENUM";
        t.name = name;
        t.enumValues = Lists.mutable.empty();
        for (String value : values)
        {
            GraphQLIntrospectionEnumValue v = new GraphQLIntrospectionEnumValue();
            v.name = value;
            t.enumValues.add(v);
        }
        return t;
    }

    @Test
    public void rendersRefAndDefinitionsForDeepAndCyclicNesting()
    {
        GraphQLIntrospectionType country = object("Country", field("name", scalar("String")));
        GraphQLIntrospectionType address = object("Address", field("street", scalar("String")), field("country", country));
        GraphQLIntrospectionType employee = object("Employee",
                field("firstName", scalar("String")),
                field("manager", null),
                field("address", address));
        employee.fields.get(1).type = employee;

        Map<String, GraphQLIntrospectionType> typesByName = Maps.mutable.<String, GraphQLIntrospectionType>empty()
                .withKeyValue("Employee", employee).withKeyValue("Address", address).withKeyValue("Country", country);

        String json = synthesizer.toJsonSchemaText(employee, "test::pkg::Employee", typesByName, Lists.mutable.empty());

        JsonNode root = readTree(json);
        Assert.assertEquals("#/definitions/test::pkg::Employee", root.get("$ref").asText());

        JsonNode definitions = root.get("definitions");
        Assert.assertTrue("expected an Employee definition", definitions.has("test::pkg::Employee"));
        Assert.assertTrue("expected an Address definition (depth 2)", definitions.has("test::pkg::Address"));
        Assert.assertTrue("expected a Country definition (depth 3)", definitions.has("test::pkg::Country"));

        JsonNode employeeDef = definitions.get("test::pkg::Employee");
        Assert.assertEquals("#/definitions/test::pkg::Employee", employeeDef.get("properties").get("manager").get("$ref").asText());
        Assert.assertEquals("#/definitions/test::pkg::Address", employeeDef.get("properties").get("address").get("$ref").asText());

        JsonNode addressDef = definitions.get("test::pkg::Address");
        Assert.assertEquals("#/definitions/test::pkg::Country", addressDef.get("properties").get("country").get("$ref").asText());

        Assert.assertEquals(3, definitions.size());
    }

    @Test
    public void rendersRefAndDefinitionsForEnumTypedField()
    {
        GraphQLIntrospectionType status = enumType("Status", "ACTIVE", "INACTIVE");
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")), field("status", status));

        Map<String, GraphQLIntrospectionType> typesByName = Maps.mutable.<String, GraphQLIntrospectionType>empty()
                .withKeyValue("Employee", employee).withKeyValue("Status", status);

        String json = synthesizer.toJsonSchemaText(employee, "test::pkg::Employee", typesByName, Lists.mutable.empty());

        JsonNode definitions = readTree(json).get("definitions");
        JsonNode employeeDef = definitions.get("test::pkg::Employee");
        Assert.assertEquals("#/definitions/test::pkg::Status", employeeDef.get("properties").get("status").get("$ref").asText());

        JsonNode statusDef = definitions.get("test::pkg::Status");
        Assert.assertEquals("string", statusDef.get("type").asText());
        Assert.assertEquals(2, statusDef.get("enum").size());
        Assert.assertEquals("ACTIVE", statusDef.get("enum").get(0).asText());
        Assert.assertEquals("INACTIVE", statusDef.get("enum").get(1).asText());
    }

    @Test
    public void rendersEnumDefinitionOnlyOnceWhenReferencedFromMultipleFields()
    {
        GraphQLIntrospectionType status = enumType("Status", "ACTIVE", "INACTIVE");
        GraphQLIntrospectionType employee = object("Employee",
                field("currentStatus", status),
                field("previousStatus", status));

        Map<String, GraphQLIntrospectionType> typesByName = Maps.mutable.<String, GraphQLIntrospectionType>empty()
                .withKeyValue("Employee", employee).withKeyValue("Status", status);

        String json = synthesizer.toJsonSchemaText(employee, "test::pkg::Employee", typesByName, Lists.mutable.empty());

        JsonNode definitions = readTree(json).get("definitions");
        Assert.assertEquals(2, definitions.size());
        Assert.assertTrue(definitions.has("test::pkg::Status"));
    }

    private static GraphQLIntrospectionType interfaceType(String name, GraphQLIntrospectionType[] possibleTypes, GraphQLIntrospectionField... fields)
    {
        GraphQLIntrospectionType t = new GraphQLIntrospectionType();
        t.kind = "INTERFACE";
        t.name = name;
        t.fields = Lists.mutable.with(fields);
        t.possibleTypes = Lists.mutable.with(possibleTypes);
        return t;
    }

    @Test
    public void rendersAllOfAndDiscriminatorForInterfaceTypedFieldAndItsPossibleTypes()
    {
        GraphQLIntrospectionType worker = object("Worker", field("id", scalar("String")), field("employeeId", scalar("String")));
        GraphQLIntrospectionType candidate = object("Candidate", field("id", scalar("String")), field("applicationId", scalar("String")));
        GraphQLIntrospectionType testParentObject = interfaceType("TestParentObject", new GraphQLIntrospectionType[] {worker, candidate}, field("id", scalar("String")));
        GraphQLIntrospectionType query = object("Query", field("testParentObject", testParentObject));

        Map<String, GraphQLIntrospectionType> typesByName = Maps.mutable.<String, GraphQLIntrospectionType>empty()
                .withKeyValue("Query", query).withKeyValue("TestParentObject", testParentObject)
                .withKeyValue("Worker", worker).withKeyValue("Candidate", candidate);

        String json = synthesizer.toJsonSchemaText(query, "test::pkg::Query", typesByName, Lists.mutable.empty());

        JsonNode definitions = readTree(json).get("definitions");
        JsonNode queryDef = definitions.get("test::pkg::Query");
        Assert.assertEquals("#/definitions/test::pkg::TestParentObject_OneOf", queryDef.get("properties").get("testParentObject").get("$ref").asText());

        JsonNode testParentObjectDef = definitions.get("test::pkg::TestParentObject");
        Assert.assertTrue("expected TestParentObject's own 'id' field", testParentObjectDef.get("properties").has("id"));
        JsonNode discriminator = testParentObjectDef.get("discriminator");
        Assert.assertEquals("__typename", discriminator.get("propertyName").asText());
        Assert.assertEquals("#/definitions/test::pkg::Worker", discriminator.get("mapping").get("Worker").asText());
        Assert.assertEquals("#/definitions/test::pkg::Candidate", discriminator.get("mapping").get("Candidate").asText());

        JsonNode wrapperDef = definitions.get("test::pkg::TestParentObject_OneOf");
        Assert.assertEquals(2, wrapperDef.get("oneOf").size());
        Assert.assertEquals("#/definitions/test::pkg::Worker", wrapperDef.get("oneOf").get(0).get("$ref").asText());
        Assert.assertEquals("#/definitions/test::pkg::Candidate", wrapperDef.get("oneOf").get(1).get("$ref").asText());
        JsonNode wrapperDiscriminator = wrapperDef.get("discriminator");
        Assert.assertEquals("__typename", wrapperDiscriminator.get("propertyName").asText());
        Assert.assertEquals("#/definitions/test::pkg::Worker", wrapperDiscriminator.get("mapping").get("Worker").asText());
        Assert.assertEquals("#/definitions/test::pkg::Candidate", wrapperDiscriminator.get("mapping").get("Candidate").asText());

        JsonNode workerDef = definitions.get("test::pkg::Worker");
        Assert.assertEquals("#/definitions/test::pkg::TestParentObject", workerDef.get("allOf").get(0).get("$ref").asText());
        Assert.assertFalse("expected Worker's own definition to not re-declare the inherited 'id' field", workerDef.get("properties").has("id"));
        Assert.assertTrue("expected Worker's own 'employeeId' field", workerDef.get("properties").has("employeeId"));

        JsonNode candidateDef = definitions.get("test::pkg::Candidate");
        Assert.assertFalse("expected Candidate's own definition to not re-declare the inherited 'id' field", candidateDef.get("properties").has("id"));
        Assert.assertTrue("expected Candidate's own 'applicationId' field", candidateDef.get("properties").has("applicationId"));
    }

    @Test
    public void omitsInterfaceTypedFieldWithNoPossibleTypesAndWarns()
    {
        GraphQLIntrospectionType emptyInterface = interfaceType("Empty", new GraphQLIntrospectionType[0], field("id", scalar("String")));
        GraphQLIntrospectionType query = object("Query", field("thing", emptyInterface));
        Map<String, GraphQLIntrospectionType> typesByName = Maps.mutable.<String, GraphQLIntrospectionType>empty()
                .withKeyValue("Query", query).withKeyValue("Empty", emptyInterface);

        MutableList<String> warnings = Lists.mutable.empty();
        String json = synthesizer.toJsonSchemaText(query, "test::pkg::Query", typesByName, warnings);

        JsonNode definitions = readTree(json).get("definitions");
        JsonNode queryDef = definitions.get("test::pkg::Query");
        Assert.assertFalse("expected the interface-typed field to be omitted since it has no possible types", queryDef.get("properties").has("thing"));
        Assert.assertTrue(warnings.toString(), warnings.anySatisfy(w -> w.contains("no known implementations")));
    }

    private JsonNode readTree(String json)
    {
        try
        {
            return objectMapper.readTree(json);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
