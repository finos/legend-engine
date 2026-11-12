// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.query.graphQL.api.execute;

import net.javacrumbs.jsonunit.JsonAssert;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Field;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationDefinition;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.OperationType;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.Selection;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.IntValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.ObjectField;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.ObjectValue;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.Value;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestHelpers
{
    @Test
    public void testIsARootField()
    {
        GraphQLExecute graphQLExecute = new GraphQLExecute(null, null, null, null, null);
        OperationDefinition operationDefinition = new OperationDefinition();
        operationDefinition.name = "query";
        operationDefinition.type = OperationType.query;
        List<Selection> selectionList = new ArrayList<>();
        Field f = new Field();
        f.name = "getContacts";
        selectionList.add(f);
        operationDefinition.selectionSet = selectionList;
        Assert.assertFalse(GraphQLExecutionHelper.isARootField("abc", operationDefinition));
        Assert.assertTrue(GraphQLExecutionHelper.isARootField("getContacts", operationDefinition));
    }

    @Test
    public void testWhere()
    {
        ObjectValue obj = new ObjectValue();
        obj.fields = Lists.mutable.empty();
        ObjectField objectField = new ObjectField();
        objectField.name = "id";
        ObjectValue obj1 = new ObjectValue();
        obj1.fields = Lists.mutable.empty();
        ObjectField objectField1 = new ObjectField();
        objectField1.name = "_eq";
        IntValue intValue = new IntValue();
        intValue.value = 1L;
        objectField1.value = intValue;
        obj1.fields.add(objectField1);
        objectField.value = obj1;
        obj.fields.add(objectField);
        Assert.assertTrue(
                GraphQLExecutionHelper.visitWhere(
                    obj,
                    "where"
                ).containsKey("where_id__eq")
        );
    }

    private static OperationDefinition parseOperation(String query)
    {
        return GraphQLExecutionHelper.findQuery(GraphQLGrammarParser.newInstance().parseDocument(query));
    }

    private static Value firstArgument(OperationDefinition operation)
    {
        return ((Field) operation.selectionSet.get(0)).arguments.get(0).value;
    }

    private static Object resolveArgument(String query, String variablesJson)
    {
        OperationDefinition operation = parseOperation(query);
        return GraphQLExecutionHelper.argumentValueToObject(firstArgument(operation), GraphQLExecutionHelper.VariableContext.from(operation, variablesJson));
    }

    private static void assertMessageContains(Throwable e, String expectedFragment)
    {
        Assert.assertTrue(e.getMessage(), e.getMessage().contains(expectedFragment));
    }

    private static void assertResolutionFails(String query, String variablesJson, String expectedMessageFragment)
    {
        assertMessageContains(Assert.assertThrows(RuntimeException.class, () -> resolveArgument(query, variablesJson)), expectedMessageFragment);
    }

    private static Map<String, Result> visitWhere(String valueText, String prefix)
    {
        return GraphQLExecutionHelper.visitWhere(firstArgument(parseOperation("query { f(a: " + valueText + ") }")), prefix);
    }

    private static Object resultValue(Map<String, Result> results, String key)
    {
        return ((ConstantResult) results.get(key)).getValue();
    }

    private static void assertJsonList(Object actual, String... expectedJson)
    {
        List<?> list = (List<?>) actual;
        Assert.assertEquals(expectedJson.length, list.size());
        for (int i = 0; i < expectedJson.length; i++)
        {
            JsonAssert.assertJsonEquals(expectedJson[i], list.get(i));
        }
    }

    // --- literal values: argumentValueToObject

    @Test
    public void testArgumentValueToObject_Boolean()
    {
        Assert.assertEquals(true, resolveArgument("query { f(a: true) }", null));
    }

    @Test
    public void testArgumentValueToObject_Enum()
    {
        Assert.assertEquals("ACTIVE", resolveArgument("query { f(a: ACTIVE) }", null));
    }

    @Test
    public void testArgumentValueToObject_Float()
    {
        Assert.assertEquals(1.5, resolveArgument("query { f(a: 1.5) }", null));
    }

    @Test
    public void testArgumentValueToObject_Int()
    {
        Assert.assertEquals(42L, resolveArgument("query { f(a: 42) }", null));
    }

    @Test
    public void testArgumentValueToObject_String()
    {
        Assert.assertEquals("hello", resolveArgument("query { f(a: \"hello\") }", null));
    }

    @Test
    public void testArgumentValueToObject_Null()
    {
        Assert.assertNull(resolveArgument("query { f(a: null) }", null));
    }

    @Test
    public void testArgumentValueToObject_List()
    {
        Assert.assertEquals(Lists.mutable.with(1L, 2L), resolveArgument("query { f(a: [1, 2]) }", null));
    }

    @Test
    public void testArgumentValueToObject_Object_IsSerializedAsJsonString()
    {
        JsonAssert.assertJsonEquals("{\"a\":1}", resolveArgument("query { f(a: {a: 1}) }", null));
    }

    @Test
    public void testArgumentValueToObject_ObjectSerializesEveryScalarKindAndKeepsNull()
    {
        JsonAssert.assertJsonEquals("{\"e\":\"LLC\",\"f\":2.5,\"b\":true,\"n\":null}", resolveArgument("query { f(a: {e: LLC, f: 2.5, b: true, n: null}) }", null));
    }

    @Test
    public void testArgumentValueToObject_ObjectKeepsNullFieldsAlongsideOtherFields()
    {
        JsonAssert.assertJsonEquals("{\"a\":null,\"b\":1}", resolveArgument("query { f(a: {a: null, b: 1}) }", null));
    }

    @Test
    public void testArgumentValueToObject_NestedObjectKeepsNullFields()
    {
        JsonAssert.assertJsonEquals("{\"outer\":{\"a\":null,\"b\":1},\"list\":[{\"c\":null}]}", resolveArgument("query { f(a: {outer: {a: null, b: 1}, list: [{c: null}]}) }", null));
    }

    @Test
    public void testArgumentValueToObject_ObjectKeepsNullFieldBoundFromVariable()
    {
        JsonAssert.assertJsonEquals("{\"a\":null,\"b\":1}", resolveArgument("query($v: Int) { f(a: {a: $v, b: 1}) }", "{\"v\":null}"));
    }

    // --- literal values: visitWhere

    @Test
    public void testVisitWhere_Boolean()
    {
        Assert.assertEquals(true, resultValue(visitWhere("true", "where"), "where"));
    }

    @Test
    public void testVisitWhere_Enum()
    {
        Assert.assertEquals("ACTIVE", resultValue(visitWhere("ACTIVE", "where"), "where"));
    }

    @Test
    public void testVisitWhere_Float()
    {
        Assert.assertEquals(1.5, resultValue(visitWhere("1.5", "where"), "where"));
    }

    @Test
    public void testVisitWhere_Int()
    {
        Assert.assertEquals(42L, resultValue(visitWhere("42", "where"), "where"));
    }

    @Test
    public void testVisitWhere_String()
    {
        Assert.assertEquals("hello", resultValue(visitWhere("\"hello\"", "where"), "where"));
    }

    @Test
    public void testVisitWhere_Null_ReturnsEmptyMap()
    {
        Assert.assertTrue(visitWhere("null", "where").isEmpty());
    }

    @Test
    public void testVisitWhere_List_InSuffixCollectsRawValues()
    {
        Assert.assertEquals(Lists.mutable.with(1L, 2L), resultValue(visitWhere("[1, 2]", "where_id__in"), "where_id__in"));
    }

    @Test
    public void testVisitWhere_List_NonInSuffixRecursesWithIndexedPrefix()
    {
        Assert.assertEquals(1L, resultValue(visitWhere("[1]", "where_tags"), "where_tags0"));
    }

    @Test
    public void testVisitWhere_Object_ContainsFieldSpecialCasesPrefix()
    {
        // "_contains" strips underscores from both prefix and field name rather than joining with "_"
        Assert.assertTrue(visitWhere("{_contains: \"abc\"}", "where_name").containsKey("wherenamecontains"));
    }

    @Test
    public void testVisitWhere_Variable_ThrowsClearUnsupportedMessage()
    {
        UnsupportedOperationException e = Assert.assertThrows(UnsupportedOperationException.class, () -> visitWhere("$id", "where"));
        Assert.assertEquals(GraphQLExecutionHelper.WHERE_VARIABLES_UNSUPPORTED_MESSAGE, e.getMessage());
    }

    // --- variables: resolution

    @Test
    public void testVariable_ResolvesScalarFromVariables()
    {
        Assert.assertEquals(42L, resolveArgument("query($id: Int!) { f(a: $id) }", "{\"id\": 42}"));
    }

    @Test
    public void testVariable_UsesDefaultValueWhenOmitted()
    {
        Assert.assertEquals(7L, resolveArgument("query($id: Int = 7) { f(a: $id) }", "{}"));
    }

    @Test
    public void testVariable_NullableOmittedResolvesToNull()
    {
        Assert.assertNull(resolveArgument("query($id: Int) { f(a: $id) }", "{}"));
    }

    @Test
    public void testVariable_RequiredMissingFailsClearly()
    {
        assertResolutionFails("query($id: Int!) { f(a: $id) }", "{}", "Missing value for required variable '$id'");
    }

    @Test
    public void testVariable_UndeclaredFailsClearly()
    {
        assertResolutionFails("query { f(a: $notDeclared) }", null, "'$notDeclared' is used but not declared");
    }

    @Test
    public void testVariable_ListTyped()
    {
        Assert.assertEquals(Lists.mutable.with("a", "b"), resolveArgument("query($tags: [String!]) { f(a: $tags) }", "{\"tags\": [\"a\", \"b\"]}"));
    }

    @Test
    public void testVariable_FloatDeclaredCoercesIntegralJsonNumber()
    {
        Object result = resolveArgument("query($amount: Float!) { f(a: $amount) }", "{\"amount\": 5}");
        Assert.assertEquals(5.0, result);
        Assert.assertTrue(result instanceof Double);
    }

    @Test
    public void testVariable_ObjectTypedResolvesToTheSameJsonAsALiteral()
    {
        Object fromVariable = resolveArgument("query($f: FilterInput) { f(a: $f) }", "{\"f\": {\"key\": \"a\"}}");
        JsonAssert.assertJsonEquals(resolveArgument("query { f(a: {key: \"a\"}) }", null), fromVariable);
    }

    @Test
    public void testVariable_NestedInsideObject()
    {
        JsonAssert.assertJsonEquals("{\"id\":42}", resolveArgument("query($id: Int!) { f(a: {id: $id}) }", "{\"id\": 42}"));
    }

    @Test
    public void testVariable_NestedTwoObjectsDeep()
    {
        JsonAssert.assertJsonEquals("{\"outer\":{\"inner\":7}}", resolveArgument("query($x: Int!) { f(a: {outer: {inner: $x}}) }", "{\"x\": 7}"));
    }

    @Test
    public void testVariable_NestedInsideListInsideObject()
    {
        JsonAssert.assertJsonEquals("{\"tags\":[\"a\"]}", resolveArgument("query($tag: String!) { f(a: {tags: [$tag]}) }", "{\"tag\": \"a\"}"));
    }

    @Test
    public void testVariable_ExplicitNullForNonNullTypeFailsEvenWhenDefaultDeclared()
    {
        assertResolutionFails("query($x: Int! = 5) { f(a: $x) }", "{\"x\": null}", "non-null");
    }

    @Test
    public void testVariable_ExplicitNullForNullableTypeIsNotReplacedByDefault()
    {
        Assert.assertNull(resolveArgument("query($x: Int = 5) { f(a: $x) }", "{\"x\": null}"));
    }

    // --- lists of objects

    @Test
    public void testListOfObjects_Literal()
    {
        assertJsonList(resolveArgument("query { f(a: [{firstName: \"John\"}, {firstName: \"Peter\"}]) }", null), "{\"firstName\":\"John\"}", "{\"firstName\":\"Peter\"}");
    }

    @Test
    public void testListOfObjects_VariableInsideAnElement()
    {
        assertJsonList(resolveArgument("query($p: String!) { f(a: [{firstName: \"John\"}, {firstName: $p}]) }", "{\"p\": \"Peter\"}"), "{\"firstName\":\"John\"}", "{\"firstName\":\"Peter\"}");
    }

    @Test
    public void testListOfObjects_WholeListAsVariable()
    {
        assertJsonList(resolveArgument("query($fs: [FilterInput!]!) { f(a: $fs) }", "{\"fs\": [{\"firstName\": \"John\"}, {\"firstName\": \"Peter\"}]}"), "{\"firstName\":\"John\"}", "{\"firstName\":\"Peter\"}");
    }

    // --- variables: payload and declarations

    @Test
    public void testVariableContext_RejectsVariablesPayloadThatIsNotAJsonObject()
    {
        OperationDefinition operation = parseOperation("query($id: Int) { f(a: $id) }");
        assertMessageContains(Assert.assertThrows(RuntimeException.class, () -> GraphQLExecutionHelper.VariableContext.from(operation, "[1, 2]")), "JSON object");
    }

    @Test
    public void testVariableContext_IgnoresVariablesPayloadWhenOperationDeclaresNoVariables()
    {
        Assert.assertSame(GraphQLExecutionHelper.VariableContext.EMPTY, GraphQLExecutionHelper.VariableContext.from(parseOperation("query { f(a: 1) }"), "[1, 2]"));
    }

    @Test
    public void testVariableContext_IgnoresVariablesPayloadForMutationThatDeclaresVariables()
    {
        Assert.assertSame(GraphQLExecutionHelper.VariableContext.MUTATION, GraphQLExecutionHelper.VariableContext.from(parseOperation("mutation($id: Int!) { f(a: 1) }"), "[1, 2]"));
    }

    @Test
    public void testVariableContext_RejectsVariableDeclaredMoreThanOnce()
    {
        OperationDefinition operation = parseOperation("query($a: Int $a: Int) { f(a: $a) }");
        assertMessageContains(Assert.assertThrows(RuntimeException.class, () -> GraphQLExecutionHelper.VariableContext.from(operation, "{}")), "'$a' is declared more than once");
    }

    // --- variables: declared type validation

    @Test
    public void testVariable_IntRejectsString()
    {
        assertResolutionFails("query($x: Int!) { f(a: $x) }", "{\"x\": \"abc\"}", "expected Int");
    }

    @Test
    public void testVariable_IntRejectsFractionalNumber()
    {
        assertResolutionFails("query($x: Int!) { f(a: $x) }", "{\"x\": 1.5}", "expected Int");
    }

    @Test
    public void testVariable_IntRejectsValueBeyondLongRange()
    {
        assertResolutionFails("query($x: Int!) { f(a: $x) }", "{\"x\": 99999999999999999999}", "64-bit range");
    }

    @Test
    public void testVariable_StringRejectsNumber()
    {
        assertResolutionFails("query($x: String!) { f(a: $x) }", "{\"x\": 5}", "expected String");
    }

    @Test
    public void testVariable_BooleanRejectsString()
    {
        assertResolutionFails("query($x: Boolean!) { f(a: $x) }", "{\"x\": \"true\"}", "expected Boolean");
    }

    @Test
    public void testVariable_IdAcceptsNumberAsString()
    {
        Assert.assertEquals("5", resolveArgument("query($x: ID!) { f(a: $x) }", "{\"x\": 5}"));
    }

    @Test
    public void testVariable_CustomEnumTypeAcceptsString()
    {
        Assert.assertEquals("ACTIVE", resolveArgument("query($x: Status!) { f(a: $x) }", "{\"x\": \"ACTIVE\"}"));
    }

    @Test
    public void testVariable_NonListTypeRejectsArray()
    {
        assertResolutionFails("query($x: String!) { f(a: $x) }", "{\"x\": [\"a\"]}", "non-list");
    }

    @Test
    public void testVariable_ListTypeCoercesASingleValueToAOneElementList()
    {
        Assert.assertEquals(Lists.mutable.with(5L), resolveArgument("query($ids: [Int]) { f(a: $ids) }", "{\"ids\": 5}"));
    }

    @Test
    public void testVariable_ListTypeCoercesASingleValueAtEveryNestingLevel()
    {
        Assert.assertEquals(Lists.mutable.with(Lists.mutable.with(1L), Lists.mutable.with(2L)), resolveArgument("query($ids: [[Int]]) { f(a: $ids) }", "{\"ids\": [1, 2]}"));
    }

    @Test
    public void testVariable_ListTypeStillValidatesTheCoercedElement()
    {
        assertResolutionFails("query($ids: [Int]) { f(a: $ids) }", "{\"ids\": \"abc\"}", "expected Int");
    }

    @Test
    public void testVariable_IntegerBeyondLongRangeInsideAnObjectIsRejectedNotRoundedToADouble()
    {
        assertResolutionFails("query($f: FilterInput) { f(a: $f) }", "{\"f\": {\"id\": 12345678901234567890}}", "64-bit range");
    }

    @Test
    public void testVariable_NonNullListItemRejectsNull()
    {
        assertResolutionFails("query($tags: [String!]) { f(a: $tags) }", "{\"tags\": [\"a\", null]}", "non-null");
    }

    // --- parameter maps

    @Test
    public void testGetParameterMap_ParsedQueryResolvesVariableFromVariablesJson()
    {
        OperationDefinition operation = parseOperation("query($legalName: String!) { firmByLegalName(legalName: $legalName) { legalName } }");

        Map<String, Result> parameterMap = GraphQLExecutionHelper.getParameterMap(operation, "firmByLegalName",
                GraphQLExecutionHelper.VariableContext.from(operation, "{\"legalName\": \"Firm X\"}"));

        Assert.assertEquals("Firm X", resultValue(parameterMap, "legalName"));
    }

    @Test
    public void testGetParameterMap_ParsedQueryUsesDeclaredDefaultWhenVariableOmitted()
    {
        OperationDefinition operation = parseOperation("query($legalName: String = \"Firm A\") { firmByLegalName(legalName: $legalName) { legalName } }");

        Map<String, Result> parameterMap = GraphQLExecutionHelper.getParameterMap(operation, "firmByLegalName",
                GraphQLExecutionHelper.VariableContext.from(operation, null));

        Assert.assertEquals("Firm A", resultValue(parameterMap, "legalName"));
    }

    // --- variables inside where are rejected

    private static void assertVariablesInWhereRejected(String query)
    {
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () -> GraphQLExecutionHelper.rejectVariablesInWhereArguments(parseOperation(query)));
        assertMessageContains(e, GraphQLExecutionHelper.WHERE_VARIABLES_UNSUPPORTED_MESSAGE);
        assertMessageContains(e, "allFirms");
    }

    @Test
    public void testRejectVariablesInWhere_VariableAsOperatorValue()
    {
        assertVariablesInWhereRejected("query($id: Int!) { allFirms(where: {id: {_eq: $id}}) { legalName } }");
    }

    @Test
    public void testRejectVariablesInWhere_VariableAsWholeWhere()
    {
        assertVariablesInWhereRejected("query($filter: FirmWhere) { allFirms(where: $filter) { legalName } }");
    }

    @Test
    public void testRejectVariablesInWhere_VariableInsideListInsideWhere()
    {
        assertVariablesInWhereRejected("query($x: Int) { allFirms(where: {id: {_in: [1, $x]}}) { legalName } }");
    }

    @Test
    public void testRejectVariablesInWhere_AllowsLiteralWhere()
    {
        GraphQLExecutionHelper.rejectVariablesInWhereArguments(parseOperation("query { allFirms(where: {id: {_eq: 1}}) { legalName } }"));
    }

    @Test
    public void testRejectVariablesInWhere_AllowsVariableInAnotherArgumentBesideLiteralWhere()
    {
        GraphQLExecutionHelper.rejectVariablesInWhereArguments(parseOperation("query($n: String!) { allFirms(legalName: $n, where: {id: {_eq: 1}}) { legalName } }"));
    }
}
