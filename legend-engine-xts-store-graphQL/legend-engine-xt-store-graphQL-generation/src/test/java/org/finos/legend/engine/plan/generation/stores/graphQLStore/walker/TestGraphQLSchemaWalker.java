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

package org.finos.legend.engine.plan.generation.stores.graphQLStore.walker;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionField;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionInputValue;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionSchema;
import org.finos.legend.engine.protocol.graphQLStore.introspection.GraphQLIntrospectionType;
import org.finos.legend.engine.protocol.graphQLStore.introspection.UnwrappedType;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestGraphQLSchemaWalker
{
    private final GraphQLSchemaWalker walker = new GraphQLSchemaWalker();

    private static GraphQLIntrospectionType scalar(String name)
    {
        GraphQLIntrospectionType t = new GraphQLIntrospectionType();
        t.kind = "SCALAR";
        t.name = name;
        return t;
    }

    private static GraphQLIntrospectionType enumType(String name)
    {
        GraphQLIntrospectionType t = new GraphQLIntrospectionType();
        t.kind = "ENUM";
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

    private static GraphQLIntrospectionType nonNull(GraphQLIntrospectionType of)
    {
        GraphQLIntrospectionType t = new GraphQLIntrospectionType();
        t.kind = "NON_NULL";
        t.ofType = of;
        return t;
    }

    private static GraphQLIntrospectionType list(GraphQLIntrospectionType of)
    {
        GraphQLIntrospectionType t = new GraphQLIntrospectionType();
        t.kind = "LIST";
        t.ofType = of;
        return t;
    }

    private static GraphQLIntrospectionField field(String name, GraphQLIntrospectionType type)
    {
        GraphQLIntrospectionField f = new GraphQLIntrospectionField();
        f.name = name;
        f.type = type;
        return f;
    }

    private static GraphQLIntrospectionField fieldWithArgs(String name, GraphQLIntrospectionType type, GraphQLIntrospectionType... argTypes)
    {
        GraphQLIntrospectionField f = field(name, type);
        f.args = Lists.mutable.empty();
        for (int i = 0; i < argTypes.length; i++)
        {
            GraphQLIntrospectionInputValue arg = new GraphQLIntrospectionInputValue();
            arg.name = "arg" + i;
            arg.type = argTypes[i];
            f.args.add(arg);
        }
        return f;
    }

    @Test
    public void testUnwrapPlainScalarIsNotListNotNonNull()
    {
        UnwrappedType unwrapped = walker.unwrap(scalar("String"), null);
        Assert.assertFalse(unwrapped.isList);
        Assert.assertFalse(unwrapped.isNonNull);
        Assert.assertEquals("String", unwrapped.baseType.name);
    }

    @Test
    public void testUnwrapNonNullListOfNonNullObjectsIsCorrectlyDetectedAsList()
    {
        GraphQLIntrospectionType employee = object("Employee");
        GraphQLIntrospectionType type = nonNull(list(nonNull(employee)));

        UnwrappedType unwrapped = walker.unwrap(type, null);
        Assert.assertTrue(unwrapped.isList);
        Assert.assertTrue(unwrapped.isNonNull);
        Assert.assertEquals("Employee", unwrapped.baseType.name);
    }

    @Test
    public void testUnwrapBareListIsListButNotNonNull()
    {
        GraphQLIntrospectionType employee = object("Employee");
        UnwrappedType unwrapped = walker.unwrap(list(employee), null);
        Assert.assertTrue(unwrapped.isList);
        Assert.assertFalse(unwrapped.isNonNull);
        Assert.assertEquals("Employee", unwrapped.baseType.name);
    }

    @Test
    public void testUnwrapHandlesPathologicallyDeepNonCyclicWrapperChain()
    {
        GraphQLIntrospectionType employee = object("Employee");
        GraphQLIntrospectionType type = employee;
        for (int i = 0; i < 10_000; i++)
        {
            type = (i % 2 == 0) ? nonNull(type) : list(type);
        }

        UnwrappedType unwrapped = walker.unwrap(type, null);
        Assert.assertTrue(unwrapped.isList);
        Assert.assertEquals("Employee", unwrapped.baseType.name);
    }

    private static GraphQLIntrospectionType relayConnection(String name, GraphQLIntrospectionType nodeType)
    {
        GraphQLIntrospectionType edge = object(name + "Edge", field("node", nodeType));
        GraphQLIntrospectionType pageInfo = object("PageInfo", field("hasNextPage", scalar("Boolean")), field("endCursor", scalar("String")));
        return object(name + "Connection", field("edges", list(edge)), field("pageInfo", pageInfo));
    }

    @Test
    public void testRelayConnectionShapeIsDetected()
    {
        GraphQLIntrospectionType employee = object("Employee");
        GraphQLIntrospectionType connection = relayConnection("Employee", employee);
        Assert.assertTrue(walker.isRelayConnection(connection, null));
        Assert.assertEquals("Employee", walker.relayNodeType(connection, null).name);
    }

    @Test
    public void testPlainObjectWithUnrelatedFieldsIsNotMistakenForRelayConnection()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        Assert.assertFalse(walker.isRelayConnection(employee, null));
    }

    @Test
    public void testObjectWithEdgesButNoValidPageInfoIsNotRelayConnection()
    {
        GraphQLIntrospectionType employee = object("Employee");
        GraphQLIntrospectionType edge = object("EmployeeEdge", field("node", employee));
        GraphQLIntrospectionType almostConnection = object("EmployeeAlmostConnection", field("edges", list(edge)));
        Assert.assertFalse(walker.isRelayConnection(almostConnection, null));
    }

    @Test
    public void testPureTypeForGraphQLScalarMapsAllFiveRecognizedScalarsAndRejectsUnknown()
    {
        Assert.assertEquals("Integer", GraphQLSchemaWalker.pureTypeForGraphQLScalar("Int"));
        Assert.assertEquals("String", GraphQLSchemaWalker.pureTypeForGraphQLScalar("String"));
        Assert.assertEquals("Boolean", GraphQLSchemaWalker.pureTypeForGraphQLScalar("Boolean"));
        Assert.assertEquals("Float", GraphQLSchemaWalker.pureTypeForGraphQLScalar("Float"));
        Assert.assertEquals("String", GraphQLSchemaWalker.pureTypeForGraphQLScalar("ID"));
        Assert.assertNull(GraphQLSchemaWalker.pureTypeForGraphQLScalar("DateTime"));
        Assert.assertNull(GraphQLSchemaWalker.pureTypeForGraphQLScalar(null));
    }

    private static GraphQLIntrospectionSchema schemaWithQueryFields(GraphQLIntrospectionField... queryFields)
    {
        GraphQLIntrospectionType queryType = object("Query", queryFields);
        GraphQLIntrospectionSchema schema = new GraphQLIntrospectionSchema();
        GraphQLIntrospectionType queryTypeRef = new GraphQLIntrospectionType();
        queryTypeRef.name = "Query";
        schema.queryType = queryTypeRef;
        schema.types = Lists.mutable.with(queryType);
        return schema;
    }

    private static QueryWrapperFieldCandidate findFor(List<QueryWrapperFieldCandidate> candidates, String fieldName)
    {
        for (QueryWrapperFieldCandidate candidate : candidates)
        {
            if (candidate.field.name.equals(fieldName))
            {
                return candidate;
            }
        }
        return null;
    }

    @Test
    public void testZeroArgFieldBecomesPlainPropertyCandidate()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(field("employees", list(employee)));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        QueryWrapperFieldCandidate candidate = findFor(candidates, "employees");
        Assert.assertNotNull(candidate);
        Assert.assertTrue(candidate.isPlainProperty);
        Assert.assertTrue(skipReasons.toString(), skipReasons.isEmpty());
    }

    @Test
    public void testArgumentBearingFieldBecomesQualifiedPropertyCandidate()
    {
        GraphQLIntrospectionType employee = object("Employee", field("id", scalar("ID")), field("firstName", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(fieldWithArgs("employee", employee, scalar("Int")));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        QueryWrapperFieldCandidate candidate = findFor(candidates, "employee");
        Assert.assertNotNull(candidate);
        Assert.assertFalse(candidate.isPlainProperty);
        Assert.assertTrue(skipReasons.toString(), skipReasons.isEmpty());
    }

    @Test
    public void testMultipleArgumentBearingFieldsForSameTypeAllBecomeCandidates()
    {
        GraphQLIntrospectionType employee = object("Employee", field("id", scalar("ID")), field("firstName", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(
                fieldWithArgs("employeeById", employee, scalar("Int")),
                fieldWithArgs("employeeByName", employee, scalar("String")));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNotNull(findFor(candidates, "employeeById"));
        Assert.assertNotNull(findFor(candidates, "employeeByName"));
        Assert.assertFalse(findFor(candidates, "employeeById").isPlainProperty);
        Assert.assertFalse(findFor(candidates, "employeeByName").isPlainProperty);
        Assert.assertTrue(skipReasons.toString(), skipReasons.isEmpty());
    }

    @Test
    public void testMultiplePluralFieldsForSameTypeBothBecomeCandidates()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(
                field("employees", list(employee)),
                field("activeEmployees", list(employee)));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNotNull(findFor(candidates, "employees"));
        Assert.assertNotNull(findFor(candidates, "activeEmployees"));
        Assert.assertTrue(skipReasons.toString(), skipReasons.isEmpty());
    }

    @Test
    public void testMultiArgumentFieldWithAllScalarArgumentsBecomesQualifiedPropertyCandidate()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(fieldWithArgs("employeeByIdAndRegion", employee, scalar("Int"), scalar("String")));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        QueryWrapperFieldCandidate candidate = findFor(candidates, "employeeByIdAndRegion");
        Assert.assertNotNull(candidate);
        Assert.assertFalse(candidate.isPlainProperty);
        Assert.assertTrue(skipReasons.toString(), skipReasons.isEmpty());
    }

    @Test
    public void testRelayConnectionFieldIsSkippedAsUnsupported()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionType connection = relayConnection("Employee", employee);
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(field("employees", connection));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNull(findFor(candidates, "employees"));
        Assert.assertTrue(skipReasons.toString(), skipReasons.stream().anyMatch(s -> s.contains("employees") && s.contains("pagination is not supported")));
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
    public void testInterfaceFieldWithKnownPossibleTypesIsNowSupported()
    {
        GraphQLIntrospectionType worker = object("Worker", field("id", scalar("String")));
        GraphQLIntrospectionType testParentObject = interfaceType("TestParentObject", new GraphQLIntrospectionType[] {worker}, field("id", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(field("testParentObject", testParentObject));
        schema.types.add(worker);
        schema.types.add(testParentObject);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNotNull("a Query-root field returning an interface is no longer rejected:\n" + skipReasons, findFor(candidates, "testParentObject"));
        Assert.assertTrue(skipReasons.toString(), skipReasons.isEmpty());
    }

    @Test
    public void testArgumentWithNoMatchingFieldNameOnTargetTypeStillBecomesQualifiedPropertyCandidate()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(fieldWithArgs("employee", employee, scalar("Int")));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNotNull(findFor(candidates, "employee"));
        Assert.assertTrue(skipReasons.toString(), skipReasons.isEmpty());
    }

    @Test
    public void testFieldWithEnumArgumentIsNowSupported()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(fieldWithArgs("employeesByStatus", list(employee), enumType("Status")));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNotNull("expected an Enum-typed argument to be supported, using the same argument-translation machinery proven for hand-written models", findFor(candidates, "employeesByStatus"));
        Assert.assertTrue(skipReasons.toString(), skipReasons.stream().noneMatch(s -> s.contains("employeesByStatus")));
    }

    @Test
    public void testFieldWithObjectTypedArgumentIsStillSkippedAndReported()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionType statusFilter = object("StatusFilter", field("status", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(fieldWithArgs("employeesByFilter", list(employee), statusFilter));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNull(findFor(candidates, "employeesByFilter"));
        Assert.assertTrue(skipReasons.toString(), skipReasons.stream().anyMatch(s -> s.contains("employeesByFilter")));
    }

    @Test
    public void testFieldWithListOfScalarArgumentIsAccepted()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(fieldWithArgs("employeesByIds", list(employee), list(scalar("Int"))));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNotNull("a list argument is only unsupported when its own element kind is unsupported - a list of a supported scalar is fine", findFor(candidates, "employeesByIds"));
    }

    @Test
    public void testFieldWithListOfUnsupportedElementKindArgumentIsSkippedAndReported()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionType filter = object("Filter", field("key", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(fieldWithArgs("employeesByIds", list(employee), list(filter)));
        schema.types.add(employee);
        schema.types.add(filter);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNull(findFor(candidates, "employeesByIds"));
        Assert.assertTrue(skipReasons.toString(), skipReasons.stream().anyMatch(s -> s.contains("employeesByIds") && s.contains("not a supported scalar type")));
    }

    @Test
    public void testInvalidFieldNameIsSkippedWithReasonNotSilentlyDropped()
    {
        GraphQLIntrospectionType employee = object("Employee", field("firstName", scalar("String")));
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(field("Weird\"Name", list(employee)));
        schema.types.add(employee);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertTrue(candidates.isEmpty());
        Assert.assertTrue(skipReasons.toString(), skipReasons.stream().anyMatch(s -> s.contains("not a valid GraphQL identifier")));
    }

    @Test
    public void testFieldWithUnrepresentableReturnTypeIsSkippedAndReported()
    {
        GraphQLIntrospectionType node = new GraphQLIntrospectionType();
        node.kind = "INPUT_OBJECT";
        node.name = "Node";
        GraphQLIntrospectionSchema schema = schemaWithQueryFields(field("node", node));
        schema.types.add(node);

        MutableList<String> skipReasons = Lists.mutable.empty();
        List<QueryWrapperFieldCandidate> candidates = walker.selectQueryWrapperFields(schema, skipReasons);

        Assert.assertNull(findFor(candidates, "node"));
        Assert.assertTrue(skipReasons.toString(), skipReasons.stream().anyMatch(s -> s.contains("node") && s.contains("not representable")));
    }
}
