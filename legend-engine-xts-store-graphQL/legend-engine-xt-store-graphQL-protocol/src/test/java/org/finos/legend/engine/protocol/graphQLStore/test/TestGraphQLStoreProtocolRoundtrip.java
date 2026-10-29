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

package org.finos.legend.engine.protocol.graphQLStore.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.protocol.graphQL.metamodel.ExecutableDocument;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.FieldDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.NamedTypeReference;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.ObjectTypeDefinition;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLClassMapping;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLConnection;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLStore;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TestGraphQLStoreProtocolRoundtrip
{
    private static final ObjectMapper objectMapper = PureProtocolObjectMapperFactory.getNewObjectMapper();

    @Test
    public void testGraphQLStoreRoundtrip() throws Exception
    {
        GraphQLStore store = new GraphQLStore();
        store.name = "TestStore";

        String json = objectMapper.writeValueAsString(store);
        GraphQLStore deserialized = objectMapper.readValue(json, GraphQLStore.class);

        Assert.assertEquals(store.name, deserialized.name);
    }

    @Test
    public void testGraphQLStoreWithSchemaRoundtrip() throws Exception
    {
        FieldDefinition firstNameField = new FieldDefinition()._name("firstName")._type(new NamedTypeReference()._name("String"));
        ObjectTypeDefinition queryType = new ObjectTypeDefinition()._name("Query")._fields(Collections.singletonList(firstNameField));
        ExecutableDocument document = (ExecutableDocument) new ExecutableDocument()._definitions(Collections.singletonList(queryType));

        GraphQLStore store = new GraphQLStore();
        store.name = "TestStore";
        store.schema = document;

        String json = objectMapper.writeValueAsString(store);
        GraphQLStore deserialized = objectMapper.readValue(json, GraphQLStore.class);

        Assert.assertEquals(store.name, deserialized.name);
        Assert.assertNotNull(deserialized.schema);
        Assert.assertEquals(1, deserialized.schema.definitions.size());
        ObjectTypeDefinition deserializedQueryType = (ObjectTypeDefinition) deserialized.schema.definitions.get(0);
        Assert.assertEquals("Query", deserializedQueryType.name);
        Assert.assertEquals(1, deserializedQueryType.fields.size());
        Assert.assertEquals("firstName", deserializedQueryType.fields.get(0).name);
    }

    @Test
    public void testGraphQLConnectionRoundtrip() throws Exception
    {
        GraphQLConnection connection = new GraphQLConnection();
        connection.url = "https://api.example.com/graphql";

        String json = objectMapper.writeValueAsString((Connection) connection);
        Connection deserialized = objectMapper.readValue(json, Connection.class);

        Assert.assertTrue(deserialized instanceof GraphQLConnection);
        Assert.assertEquals(connection.url, ((GraphQLConnection) deserialized).url);
    }

    @Test
    public void testGraphQLClassMappingRoundtrip() throws Exception
    {
        GraphQLClassMapping classMapping = new GraphQLClassMapping();
        classMapping.storePath = "test::TestStore";
        classMapping.typeName = "Query";
        classMapping.bindingPath = "test::TestBinding";

        String json = objectMapper.writeValueAsString((ClassMapping) classMapping);
        ClassMapping deserialized = objectMapper.readValue(json, ClassMapping.class);

        Assert.assertTrue(deserialized instanceof GraphQLClassMapping);
        GraphQLClassMapping deserializedMapping = (GraphQLClassMapping) deserialized;
        Assert.assertEquals(classMapping.storePath, deserializedMapping.storePath);
        Assert.assertEquals(classMapping.typeName, deserializedMapping.typeName);
        Assert.assertEquals(classMapping.bindingPath, deserializedMapping.bindingPath);
    }
}
