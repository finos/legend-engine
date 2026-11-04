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

package org.finos.legend.engine.language.pure.compiler.test.store.graphql;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_FieldDefinition;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore;
import org.finos.legend.pure.generated.Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestGraphQLStoreCompilationFromGrammar
{
    private static final String TWO_STORE_MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::Employee\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  department: test::graphql::Department[1];\n" +
            "}\n" +
            "Class test::graphql::Department\n" +
            "{\n" +
            "  name: String[1];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::TestSchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/Employee.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"department\": {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}, \"required\": [\"name\"]}}, \"required\": [\"firstName\", \"department\"]}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::TestBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::TestSchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::Employee, test::graphql::Department ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::StoreA\n" +
            "{\n" +
            "}\n" +
            "GraphQLStore test::graphql::StoreB\n" +
            "{\n" +
            "}\n" +
            "###Mapping\n" +
            "Mapping test::graphql::TestMapping\n" +
            "(\n" +
            "  *test::graphql::Employee: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::StoreA] 'Employee'\n" +
            "    ~binding test::graphql::TestBinding\n" +
            "  }\n" +
            "  *test::graphql::Department: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::StoreB] 'Department'\n" +
            "    ~binding test::graphql::TestBinding\n" +
            "  }\n" +
            ")\n";

    @Test
    public void testEmployeeDepartmentFoldsLocallyRegardlessOfDepartmentsOwnSeparateStoreDeclaration()
    {
        Pair<PureModelContextData, PureModel> result = test(TWO_STORE_MODEL_AND_BINDING);

        PureModel pureModel = result.getTwo();
        Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation employeeSetImplementation =
                (Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) pureModel.getMapping("test::graphql::TestMapping")._classMappings()
                        .detect(c -> c instanceof Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation
                                && "Employee".equals(((Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) c)._typeName()));
        PropertyMapping departmentPropertyMapping = employeeSetImplementation._propertyMappings().detect(pm -> pm._property()._name().equals("department"));
        Assert.assertTrue("expected Employee.department to resolve to a real, self-sufficient embed, not a pointer into Department's own separate StoreB mapping",
                departmentPropertyMapping instanceof Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation);
    }

    // department is declared via an Association
    private static final String ASSOCIATION_SOURCED_PROPERTY_MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::assoc::Employee\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "}\n" +
            "Class test::graphql::assoc::Department\n" +
            "{\n" +
            "  name: String[1];\n" +
            "}\n" +
            "Association test::graphql::assoc::Employee_Department\n" +
            "{\n" +
            "  department: test::graphql::assoc::Department[1];\n" +
            "  employee: test::graphql::assoc::Employee[1];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::assoc::TestSchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/assoc/Employee.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::assoc::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"department\": {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}, \"required\": [\"name\"]}}, \"required\": [\"firstName\", \"department\"]}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::assoc::TestBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::assoc::TestSchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::assoc::Employee, test::graphql::assoc::Department ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::assoc::Store\n" +
            "{\n" +
            "}\n" +
            "###Mapping\n" +
            "Mapping test::graphql::assoc::TestMapping\n" +
            "(\n" +
            "  *test::graphql::assoc::Employee: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::assoc::Store] 'Employee'\n" +
            "    ~binding test::graphql::assoc::TestBinding\n" +
            "  }\n" +
            ")\n";

    @Test
    public void testAssociationSourcedPropertyBackedByRealSchemaFieldIsMapped()
    {
        Pair<PureModelContextData, PureModel> result = test(ASSOCIATION_SOURCED_PROPERTY_MODEL_AND_BINDING);

        PureModel pureModel = result.getTwo();
        Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation employeeSetImplementation =
                (Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) pureModel.getMapping("test::graphql::assoc::TestMapping")._classMappings().getFirst();
        PropertyMapping departmentPropertyMapping = employeeSetImplementation._propertyMappings().detect(pm -> pm._property()._name().equals("department"));
        Assert.assertNotNull("expected the Association-sourced 'department' property (backed by a real schema field) to already resolve to a property mapping", departmentPropertyMapping);
        Assert.assertTrue("expected it to resolve to a real embed, same as a directly-declared property would",
                departmentPropertyMapping instanceof Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation);
    }

    // ~joinAccessor names the qualified property a cross-instance Association join should call with the
    // resolved join-key value - resolveJoinAccessor validates it exists and takes exactly one parameter.
    // No tracked schema here: this validates purely against the Pure class itself, independent of
    // GraphQLStoreCompilerHelper.validateTypeNameAndQualifiedPropertiesAgainstTrackedSchema.
    private static final String JOIN_ACCESSOR_MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::Department\n" +
            "{\n" +
            "  name: String[1];\n" +
            "  departmentByJoinKeyProxy: test::graphql::Department[0..1];\n" +
            "  departmentByJoinKey(id: String[1]) { $this.departmentByJoinKeyProxy }: test::graphql::Department[0..1];\n" +
            "  departmentByTwoArgs(id: String[1], region: String[1]) { test::graphql::Department.all()->toOne() }: test::graphql::Department[0..1];\n" +
            "  departmentByJoinKeyWrongShape(id: String[1]) { test::graphql::Department.all()->toOne() }: test::graphql::Department[0..1];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::DepartmentSchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/Department.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Department\", \"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, \"departmentByJoinKeyProxy\": {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}, \"required\": [\"name\"]}}, \"required\": [\"name\"]}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::DepartmentBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::DepartmentSchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::Department ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::DepartmentStore\n" +
            "{\n" +
            "}\n";

    @Test
    public void testJoinAccessorResolvesAndCompilesFine()
    {
        Pair<PureModelContextData, PureModel> result = test(JOIN_ACCESSOR_MODEL_AND_BINDING +
                "###Mapping\n" +
                "Mapping test::graphql::TestMapping\n" +
                "(\n" +
                "  *test::graphql::Department: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::DepartmentStore] 'Department'\n" +
                "    ~binding test::graphql::DepartmentBinding\n" +
                "    ~joinAccessor departmentByJoinKey\n" +
                "  }\n" +
                ")\n");

        PureModel pureModel = result.getTwo();
        Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation setImpl =
                (Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) pureModel.getMapping("test::graphql::TestMapping")._classMappings().getFirst();
        Assert.assertNotNull("expected ~joinAccessor to resolve to a real QualifiedProperty", setImpl._joinAccessor());
        Assert.assertEquals("departmentByJoinKey", setImpl._joinAccessor()._name());
    }

    @Test
    public void testJoinAccessorNotFoundFailsClearly()
    {
        try
        {
            test(JOIN_ACCESSOR_MODEL_AND_BINDING +
                    "###Mapping\n" +
                    "Mapping test::graphql::TestMapping\n" +
                    "(\n" +
                    "  *test::graphql::Department: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::DepartmentStore] 'Department'\n" +
                    "    ~binding test::graphql::DepartmentBinding\n" +
                    "    ~joinAccessor bogus\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail("Expected a compilation error for a '~joinAccessor' naming a qualified property that doesn't exist");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("'~joinAccessor' qualified property 'bogus' not found"));
        }
    }

    @Test
    public void testJoinAccessorWithWrongParameterCountFailsClearly()
    {
        try
        {
            test(JOIN_ACCESSOR_MODEL_AND_BINDING +
                    "###Mapping\n" +
                    "Mapping test::graphql::TestMapping\n" +
                    "(\n" +
                    "  *test::graphql::Department: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::DepartmentStore] 'Department'\n" +
                    "    ~binding test::graphql::DepartmentBinding\n" +
                    "    ~joinAccessor departmentByTwoArgs\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail("Expected a compilation error for a '~joinAccessor' qualified property declaring more than one parameter");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("'~joinAccessor' qualified property 'departmentByTwoArgs' must declare exactly one parameter"));
        }
    }

    @Test
    public void testJoinAccessorWithWrongShapeBodyFailsClearly()
    {
        try
        {
            test(JOIN_ACCESSOR_MODEL_AND_BINDING +
                    "###Mapping\n" +
                    "Mapping test::graphql::TestMapping\n" +
                    "(\n" +
                    "  *test::graphql::Department: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::DepartmentStore] 'Department'\n" +
                    "    ~binding test::graphql::DepartmentBinding\n" +
                    "    ~joinAccessor departmentByJoinKeyWrongShape\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail("Expected a compilation error for a '~joinAccessor' qualified property whose body isn't a bare '{ $this.<property> }' delegate");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(),
                    e.getMessage().contains("'~joinAccessor' qualified property 'departmentByJoinKeyWrongShape' must have a body of the form '{ $this.<property> }'"));
        }
    }

    @Test
    public void testJoinAccessorWithOverloadedNameFailsClearly()
    {
        try
        {
            test("###Pure\n" +
                    "Class test::graphql::Department\n" +
                    "{\n" +
                    "  name: String[1];\n" +
                    "  departmentByJoinKey(id: String[1]) { test::graphql::Department.all()->toOne() }: test::graphql::Department[0..1];\n" +
                    "  departmentByJoinKey(id: String[1], region: String[1]) { test::graphql::Department.all()->toOne() }: test::graphql::Department[0..1];\n" +
                    "}\n" +
                    "###ExternalFormat\n" +
                    "SchemaSet test::graphql::DepartmentSchema\n" +
                    "{\n" +
                    "  format: JSON;\n" +
                    "  schemas: [\n" +
                    "    {\n" +
                    "      location: 'test/graphql/Department.json';\n" +
                    "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Department\", \"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}, \"required\": [\"name\"]}';\n" +
                    "    }\n" +
                    "  ];\n" +
                    "}\n" +
                    "Binding test::graphql::DepartmentBinding\n" +
                    "{\n" +
                    "  schemaSet: test::graphql::DepartmentSchema;\n" +
                    "  contentType: 'application/json';\n" +
                    "  modelIncludes: [ test::graphql::Department ];\n" +
                    "}\n" +
                    "###GraphQL\n" +
                    "GraphQLStore test::graphql::DepartmentStore\n" +
                    "{\n" +
                    "}\n" +
                    "###Mapping\n" +
                    "Mapping test::graphql::TestMapping\n" +
                    "(\n" +
                    "  *test::graphql::Department: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::DepartmentStore] 'Department'\n" +
                    "    ~binding test::graphql::DepartmentBinding\n" +
                    "    ~joinAccessor departmentByJoinKey\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail("Expected a compilation error for a '~joinAccessor' naming an overloaded qualified property");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(),
                    e.getMessage().contains("'~joinAccessor' qualified property 'departmentByJoinKey' is overloaded (2 signatures)") &&
                    e.getMessage().contains("cannot disambiguate by argument count/type"));
        }
    }

    // A tracked schema (GraphQLStore's own embedded ~schema SDL text) turns on real compile-time
    // validation of ~typeName's qualified properties - see
    // GraphQLStoreCompilerHelper.validateTypeNameAndQualifiedPropertiesAgainstTrackedSchema.
    private static final String TRACKED_SCHEMA_MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::Employee\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  lastName: String[1];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::TestSchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/Employee.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"lastName\": {\"type\": \"string\"}}, \"required\": [\"firstName\", \"lastName\"]}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::TestBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::TestSchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::Employee ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::TestStore\n" +
            "{\n" +
            "  schema: {\n" +
            "type Query {\n" +
            "  employees(department: String): [Employee]\n" +
            "}\n" +
            "\n" +
            "type Employee {\n" +
            "  firstName: String\n" +
            "  lastName: String\n" +
            "}\n" +
            "};\n" +
            "}\n";

    // Proves the tracked schema compiles into a real, navigable Pure object graph (GraphQLStore.document)
    // rather than being re-parsed from the raw schema string on demand. The document's own structure
    // (field/argument counts) is independent of which mapping clause anchors the class mapping.
    @Test
    public void testTrackedSchemaCompilesIntoRealDocumentObject()
    {
        Pair<PureModelContextData, PureModel> result = test(TRACKED_SCHEMA_MODEL_AND_BINDING +
                "###Mapping\n" +
                "Mapping test::graphql::TestMapping\n" +
                "(\n" +
                "  *test::graphql::Employee: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::TestStore] 'Employee'\n" +
                "    ~binding test::graphql::TestBinding\n" +
                "  }\n" +
                ")\n");

        Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore store =
                (Root_meta_external_store_graphQL_metamodel_pure_GraphQLStore) result.getTwo().getStore("test::graphql::TestStore");
        Root_meta_external_query_graphQL_metamodel_sdl_Document document = store._document();
        Assert.assertNotNull("expected the tracked schema to compile into a real Document, not stay a string", document);

        Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition queryType =
                (Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition) document._definitions()
                        .detect(d -> d instanceof Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition
                                && ((Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition) d)._name().equals("Query"));
        Assert.assertNotNull(queryType);
        Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_FieldDefinition employeesField =
                queryType._fields().detect(f -> f._name().equals("employees"));
        Assert.assertNotNull(employeesField);
        Assert.assertEquals(1, employeesField._argumentDefinitions().size());
        Assert.assertEquals("department", employeesField._argumentDefinitions().getFirst()._name());

        Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition employeeType =
                (Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition) document._definitions()
                        .detect(d -> d instanceof Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition
                                && ((Root_meta_external_query_graphQL_metamodel_sdl_typeSystem_ObjectTypeDefinition) d)._name().equals("Employee"));
        Assert.assertNotNull(employeeType);
        Assert.assertEquals(2, employeeType._fields().size());
    }

    // A '~typeName'-mapped Query-wrapper class has only qualified properties, so its binding's JSON
    // schema is a bare, property-less object. employees()/employee(id)'s bodies are never interpreted -
    // only the qualified property's name, declared parameter list, and return type matter at compile time.
    private static final String QUERY_MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::Employee\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  lastName: String[1];\n" +
            "}\n" +
            "Class test::graphql::Query\n" +
            "{\n" +
            "  employees() { test::graphql::Employee.all() }: test::graphql::Employee[*];\n" +
            "  employee(id: Integer[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::EmployeeSchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/Employee.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"lastName\": {\"type\": \"string\"}}, \"required\": [\"firstName\", \"lastName\"]}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::EmployeeBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::EmployeeSchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::Employee ];\n" +
            "}\n" +
            "SchemaSet test::graphql::QuerySchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/Query.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Query\", \"type\": \"object\", \"properties\": {}}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::QueryBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::QuerySchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::Query ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::TestStore\n" +
            "{\n" +
            "  schema: {\n" +
            "type Query {\n" +
            "  employees: [Employee]\n" +
            "  employee(id: Int): Employee\n" +
            "}\n" +
            "\n" +
            "type Employee {\n" +
            "  firstName: String\n" +
            "  lastName: String\n" +
            "}\n" +
            "};\n" +
            "}\n";

    // Employee has no qualified properties of its own, so mapping it via ~typeName 'Employee' validates
    // trivially (nothing to check) while still giving it a real class mapping, satisfying "qualified
    // property return type has a class mapping" without needing Employee reached via embedding.
    private static final String QUERY_AND_EMPLOYEE_MAPPING =
            "###Mapping\n" +
            "Mapping test::graphql::TestMapping\n" +
            "(\n" +
            "  *test::graphql::Query: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::TestStore] 'Query'\n" +
            "    ~binding test::graphql::QueryBinding\n" +
            "  }\n" +
            "  *test::graphql::Employee: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::TestStore] 'Employee'\n" +
            "    ~binding test::graphql::EmployeeBinding\n" +
            "  }\n" +
            ")\n";

    @Test
    public void testTypeNameAndQualifiedPropertiesValidateAgainstTrackedSchema()
    {
        Pair<PureModelContextData, PureModel> result = test(QUERY_MODEL_AND_BINDING + QUERY_AND_EMPLOYEE_MAPPING);

        PureModel pureModel = result.getTwo();
        Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation querySetImplementation =
                (Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) pureModel.getMapping("test::graphql::TestMapping")._classMappings()
                        .detect(c -> c instanceof Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation
                                && "Query".equals(((Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) c)._typeName()));
        Assert.assertEquals("Query", querySetImplementation._typeName());
    }

    // Neither clause sets classMapping.storePath, so this fails earlier than this feature's own
    // "typeName required" check - at the generic, store-agnostic context.resolveStore(null) call every
    // class mapping goes through first, with a correspondingly generic (pre-existing, out of scope to
    // improve) message.
    @Test
    public void testRootClassMappingWithNeitherTypeNameFailsToResolveItsStore()
    {
        try
        {
            test(QUERY_MODEL_AND_BINDING +
                    "###Mapping\n" +
                    "Mapping test::graphql::TestMapping\n" +
                    "(\n" +
                    "  *test::graphql::Query: GraphQL\n" +
                    "  {\n" +
                    "    ~binding test::graphql::QueryBinding\n" +
                    "  }\n" +
                    "  *test::graphql::Employee: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::TestStore] 'Employee'\n" +
                    "    ~binding test::graphql::EmployeeBinding\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail("Expected a compilation error for declaring no '~typeName'");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("Can't resolve"));
        }
    }

    @Test
    public void testUnknownTypeNameAgainstTrackedSchemaFailsClearly()
    {
        try
        {
            test(QUERY_MODEL_AND_BINDING +
                    "###Mapping\n" +
                    "Mapping test::graphql::TestMapping\n" +
                    "(\n" +
                    "  *test::graphql::Query: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::TestStore] 'Bogus'\n" +
                    "    ~binding test::graphql::QueryBinding\n" +
                    "  }\n" +
                    "  *test::graphql::Employee: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::TestStore] 'Employee'\n" +
                    "    ~binding test::graphql::EmployeeBinding\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail("Expected a compilation error for a '~typeName' absent from the tracked schema");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("Type 'Bogus' not found in tracked schema"));
        }
    }

    @Test
    public void testUnknownQualifiedPropertyFieldAgainstTrackedSchemaFailsClearly()
    {
        try
        {
            test("###Pure\n" +
                    "Class test::graphql::Employee\n" +
                    "{\n" +
                    "  firstName: String[1];\n" +
                    "}\n" +
                    "Class test::graphql::Query\n" +
                    "{\n" +
                    "  employeesTypo() { test::graphql::Employee.all() }: test::graphql::Employee[*];\n" +
                    "}\n" +
                    "###ExternalFormat\n" +
                    "SchemaSet test::graphql::EmployeeSchema\n" +
                    "{\n" +
                    "  format: JSON;\n" +
                    "  schemas: [\n" +
                    "    {\n" +
                    "      location: 'test/graphql/Employee.json';\n" +
                    "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}}, \"required\": [\"firstName\"]}';\n" +
                    "    }\n" +
                    "  ];\n" +
                    "}\n" +
                    "\n" +
                    "Binding test::graphql::EmployeeBinding\n" +
                    "{\n" +
                    "  schemaSet: test::graphql::EmployeeSchema;\n" +
                    "  contentType: 'application/json';\n" +
                    "  modelIncludes: [ test::graphql::Employee ];\n" +
                    "}\n" +
                    "SchemaSet test::graphql::QuerySchema\n" +
                    "{\n" +
                    "  format: JSON;\n" +
                    "  schemas: [\n" +
                    "    {\n" +
                    "      location: 'test/graphql/Query.json';\n" +
                    "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Query\", \"type\": \"object\", \"properties\": {}}';\n" +
                    "    }\n" +
                    "  ];\n" +
                    "}\n" +
                    "\n" +
                    "Binding test::graphql::QueryBinding\n" +
                    "{\n" +
                    "  schemaSet: test::graphql::QuerySchema;\n" +
                    "  contentType: 'application/json';\n" +
                    "  modelIncludes: [ test::graphql::Query ];\n" +
                    "}\n" +
                    "###GraphQL\n" +
                    "GraphQLStore test::graphql::TestStore\n" +
                    "{\n" +
                    "  schema: {\n" +
                    "type Query {\n" +
                    "  employees: [Employee]\n" +
                    "}\n" +
                    "\n" +
                    "type Employee {\n" +
                    "  firstName: String\n" +
                    "}\n" +
                    "};\n" +
                    "}\n" +
                    "###Mapping\n" +
                    "Mapping test::graphql::TestMapping\n" +
                    "(\n" +
                    "  *test::graphql::Query: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::TestStore] 'Query'\n" +
                    "    ~binding test::graphql::QueryBinding\n" +
                    "  }\n" +
                    "  *test::graphql::Employee: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::TestStore] 'Employee'\n" +
                    "    ~binding test::graphql::EmployeeBinding\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail("Expected a compilation error for a qualified property with no matching field on the '~typeName' type");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("Qualified property 'employeesTypo' has no matching field on type 'Query'"));
        }
    }

    @Test
    public void testMultiArgumentQualifiedPropertyCompilesWhenEveryParameterMatchesTheSchema()
    {
        Pair<PureModelContextData, PureModel> result = test(QUERY_MODEL_AND_BINDING
                .replace(
                        "employee(id: Integer[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];",
                        "employee(id: Integer[1], region: String[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];")
                .replace(
                        "employee(id: Int): Employee",
                        "employee(id: Int, region: String): Employee") +
                QUERY_AND_EMPLOYEE_MAPPING);

        PureModel pureModel = result.getTwo();
        Assert.assertNotNull(pureModel.getMapping("test::graphql::TestMapping"));
    }

    @Test
    public void testMultiArgumentQualifiedPropertyWithUnknownParameterNameFailsClearly()
    {
        try
        {
            test(QUERY_MODEL_AND_BINDING
                    .replace(
                            "employee(id: Integer[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];",
                            "employee(id: Integer[1], bogus: String[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];")
                    .replace(
                            "employee(id: Int): Employee",
                            "employee(id: Int, region: String): Employee") +
                    QUERY_AND_EMPLOYEE_MAPPING);
            Assert.fail("Expected a compilation error for a parameter name not matching any declared schema argument, even when it's not the only parameter");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("parameter 'bogus' does not match the name of any argument declared on field 'employee'"));
        }
    }

    @Test
    public void testQualifiedPropertyParameterNameMustMatchDeclaredSchemaArgumentFailsClearly()
    {
        try
        {
            test(QUERY_MODEL_AND_BINDING.replace(
                    "employee(id: Integer[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];",
                    "employee(employeeId: Integer[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];") +
                    QUERY_AND_EMPLOYEE_MAPPING);
            Assert.fail("Expected a compilation error for a qualified property parameter name not matching the schema's declared argument");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("parameter 'employeeId' does not match the name of any argument declared on field 'employee'"));
        }
    }

    // A QP declaring FEWER parameters than its matching schema field requires would otherwise compile
    // fine and silently send an incomplete GraphQL request at runtime (missing a required argument),
    // failing only at the server, not at compile time - the reverse direction of the "every declared
    // parameter must match a real argument" check above.
    @Test
    public void testQualifiedPropertyMissingRequiredSchemaArgumentFailsClearly()
    {
        try
        {
            test(QUERY_MODEL_AND_BINDING.replace(
                    "employee(id: Int): Employee",
                    "employee(id: Int!, region: String!): Employee") +
                    QUERY_AND_EMPLOYEE_MAPPING);
            Assert.fail("Expected a compilation error for a qualified property missing a parameter for a required schema argument");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("is missing a parameter for required argument 'region'"));
        }
    }

    @Test
    public void testQualifiedPropertyWithAllRequiredSchemaArgumentsDeclaredCompilesFine()
    {
        Pair<PureModelContextData, PureModel> result = test(QUERY_MODEL_AND_BINDING
                .replace(
                        "employee(id: Integer[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];",
                        "employee(id: Integer[1], region: String[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];")
                .replace(
                        "employee(id: Int): Employee",
                        "employee(id: Int!, region: String!): Employee") +
                QUERY_AND_EMPLOYEE_MAPPING);

        PureModel pureModel = result.getTwo();
        Assert.assertNotNull(pureModel.getMapping("test::graphql::TestMapping"));
    }

    // Known scope limit: validateTypeNameAndQualifiedPropertiesAgainstTrackedSchema only runs for
    // root+typeName class mappings - a qualified property on a NESTED (embedded, non-root) class gets no
    // schema validation at all. The generator never produces this shape, but a hand-authored mapping
    // could. Employee here is reached ONLY via embedding (Query's own "employees" plain property, backed
    // by an inline nested JSON schema in Query's own binding) - never independently mapped - so its
    // "departmentById" QP, with an argument matching no real schema field, still compiles fine.
    @Test
    public void testQualifiedPropertyOnNestedClassHasNoSchemaValidationKnownLimitation()
    {
        Pair<PureModelContextData, PureModel> result = test("###Pure\n" +
                "Class test::graphql::Employee\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  departmentById(bogus: Integer[1]) { test::graphql::Employee.all()->toOne() }: test::graphql::Employee[0..1];\n" +
                "}\n" +
                "Class test::graphql::Query\n" +
                "{\n" +
                "  employees: test::graphql::Employee[*];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::graphql::QuerySchema\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      location: 'test/graphql/Query.json';\n" +
                "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Query\", \"type\": \"object\", \"properties\": {\"employees\": {\"type\": \"array\", \"items\": {\"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}}, \"required\": [\"firstName\"]}}}}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding test::graphql::QueryBinding\n" +
                "{\n" +
                "  schemaSet: test::graphql::QuerySchema;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::graphql::Query, test::graphql::Employee ];\n" +
                "}\n" +
                "###GraphQL\n" +
                "GraphQLStore test::graphql::TestStore\n" +
                "{\n" +
                "  schema: {\n" +
                "type Query {\n" +
                "  employees: [Employee]\n" +
                "}\n" +
                "\n" +
                "type Employee {\n" +
                "  firstName: String\n" +
                "}\n" +
                "};\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::graphql::TestMapping\n" +
                "(\n" +
                "  *test::graphql::Query: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::TestStore] 'Query'\n" +
                "    ~binding test::graphql::QueryBinding\n" +
                "  }\n" +
                ")\n");

        PureModel pureModel = result.getTwo();
        Assert.assertNotNull("expected this to compile - 'bogus' matches no real schema argument, but QPs on nested/embedded classes are not validated against the tracked schema today", pureModel.getMapping("test::graphql::TestMapping"));
    }

    @Test
    public void testQualifiedPropertyReturnTypeWithNoClassMappingFailsClearly()
    {
        try
        {
            test(QUERY_MODEL_AND_BINDING +
                    "###Mapping\n" +
                    "Mapping test::graphql::TestMapping\n" +
                    "(\n" +
                    "  *test::graphql::Query: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::TestStore] 'Query'\n" +
                    "    ~binding test::graphql::QueryBinding\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail("Expected a compilation error for a qualified property whose return type has no class mapping in the Mapping");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().contains("has no class mapping in this Mapping"));
        }
    }

    // employeeByStatus's body is exactly "{ $this.employeeByStatusProxy }" - a this-threaded qualified
    // property - but the Binding's JSON schema never describes employeeByStatusProxy, so no property
    // mapping is ever built for it. Before validateThisThreadedQualifiedPropertiesHaveProxyMapping, this
    // broken model compiled fine and only failed much later, cryptically, at routing or execution time.
    private static final String THIS_THREADED_MISSING_PROXY_MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::Employee\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  employeeByStatusProxy: test::graphql::Employee[0..1];\n" +
            "  employeeByStatus(status: String[1]) { $this.employeeByStatusProxy }: test::graphql::Employee[0..1];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::EmployeeSchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/Employee.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}}, \"required\": [\"firstName\"]}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::EmployeeBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::EmployeeSchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::Employee ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::TestStore\n" +
            "{\n" +
            "}\n" +
            "###Mapping\n" +
            "Mapping test::graphql::TestMapping\n" +
            "(\n" +
            "  *test::graphql::Employee: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::TestStore] 'Employee'\n" +
            "    ~binding test::graphql::EmployeeBinding\n" +
            "  }\n" +
            ")\n";

    @Test
    public void testThisThreadedQualifiedPropertyWithUnmappedProxyFailsClearly()
    {
        try
        {
            test(THIS_THREADED_MISSING_PROXY_MODEL_AND_BINDING);
            Assert.fail("Expected a compilation error for a this-threaded qualified property whose delegate property has no mapping");
        }
        catch (EngineException e)
        {
            Assert.assertTrue("Unexpected error message: " + e.getMessage(),
                    e.getMessage().contains("delegates to '$this.employeeByStatusProxy'") && e.getMessage().contains("has no mapping"));
        }
    }

    // Same this-threaded shape as the negative test above, but the delegate property's type (Manager) has
    // its own independent class mapping elsewhere in the same Mapping - same proven pattern as
    // testEmployeeDepartmentFoldsLocallyRegardlessOfDepartmentsOwnSeparateStoreDeclaration, just with a QP
    // delegate instead of a plain property - so the embed succeeds and the new validation must not
    // false-positive on this already-correct, already-shipped shape.
    @Test
    public void testThisThreadedQualifiedPropertyWithMappedProxyCompilesFine()
    {
        Pair<PureModelContextData, PureModel> result = test("###Pure\n" +
                "Class test::graphql::Manager\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "Class test::graphql::Employee\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  employeeByStatusProxy: test::graphql::Manager[0..1];\n" +
                "  employeeByStatus(status: String[1]) { $this.employeeByStatusProxy }: test::graphql::Manager[0..1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::graphql::EmployeeSchema\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      location: 'test/graphql/Employee.json';\n" +
                "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"employeeByStatusProxy\": {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}, \"required\": [\"name\"]}}, \"required\": [\"firstName\"]}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding test::graphql::EmployeeBinding\n" +
                "{\n" +
                "  schemaSet: test::graphql::EmployeeSchema;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::graphql::Employee, test::graphql::Manager ];\n" +
                "}\n" +
                "SchemaSet test::graphql::ManagerSchema\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      location: 'test/graphql/Manager.json';\n" +
                "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Manager\", \"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}, \"required\": [\"name\"]}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding test::graphql::ManagerBinding\n" +
                "{\n" +
                "  schemaSet: test::graphql::ManagerSchema;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::graphql::Manager ];\n" +
                "}\n" +
                "###GraphQL\n" +
                "GraphQLStore test::graphql::TestStore\n" +
                "{\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::graphql::TestMapping\n" +
                "(\n" +
                "  *test::graphql::Employee: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::TestStore] 'Employee'\n" +
                "    ~binding test::graphql::EmployeeBinding\n" +
                "  }\n" +
                "  *test::graphql::Manager: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::TestStore] 'Manager'\n" +
                "    ~binding test::graphql::ManagerBinding\n" +
                "  }\n" +
                ")\n");

        PureModel pureModel = result.getTwo();
        Assert.assertNotNull("expected this to compile - employeeByStatusProxy has a real (embedded) property mapping", pureModel.getMapping("test::graphql::TestMapping"));
    }

    // Two class mappings resolve to Manager - the standalone root=true one and the
    // EmbeddedGraphQLSetImplementation folded in from Employee.manager. A this-threaded qualified
    // property resolves its real target via its own body's navigation at routing time, never by a
    // mapping-wide, type-only lookup, so other class mappings resolving to the same class are irrelevant.
    @Test
    public void testQualifiedPropertyReturnTypeWithMultipleClassMappingsForSameReturnTypeCompilesFine()
    {
        Pair<PureModelContextData, PureModel> result = test("###Pure\n" +
                    "Class test::graphql::Manager\n" +
                    "{\n" +
                    "  name: String[1];\n" +
                    "}\n" +
                    "Class test::graphql::Employee\n" +
                    "{\n" +
                    "  firstName: String[1];\n" +
                    "  manager: test::graphql::Manager[1];\n" +
                    "}\n" +
                    "Class test::graphql::Query\n" +
                    "{\n" +
                    "  managers() { test::graphql::Manager.all() }: test::graphql::Manager[*];\n" +
                    "}\n" +
                    "###ExternalFormat\n" +
                    "SchemaSet test::graphql::EmployeeSchema\n" +
                    "{\n" +
                    "  format: JSON;\n" +
                    "  schemas: [\n" +
                    "    {\n" +
                    "      location: 'test/graphql/Employee.json';\n" +
                    "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"manager\": {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}, \"required\": [\"name\"]}}, \"required\": [\"firstName\", \"manager\"]}';\n" +
                    "    }\n" +
                    "  ];\n" +
                    "}\n" +
                    "\n" +
                    "Binding test::graphql::EmployeeBinding\n" +
                    "{\n" +
                    "  schemaSet: test::graphql::EmployeeSchema;\n" +
                    "  contentType: 'application/json';\n" +
                    "  modelIncludes: [ test::graphql::Employee, test::graphql::Manager ];\n" +
                    "}\n" +
                    "SchemaSet test::graphql::ManagerSchema\n" +
                    "{\n" +
                    "  format: JSON;\n" +
                    "  schemas: [\n" +
                    "    {\n" +
                    "      location: 'test/graphql/Manager.json';\n" +
                    "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Manager\", \"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}, \"required\": [\"name\"]}';\n" +
                    "    }\n" +
                    "  ];\n" +
                    "}\n" +
                    "\n" +
                    "Binding test::graphql::ManagerBinding\n" +
                    "{\n" +
                    "  schemaSet: test::graphql::ManagerSchema;\n" +
                    "  contentType: 'application/json';\n" +
                    "  modelIncludes: [ test::graphql::Manager ];\n" +
                    "}\n" +
                    "SchemaSet test::graphql::QuerySchema\n" +
                    "{\n" +
                    "  format: JSON;\n" +
                    "  schemas: [\n" +
                    "    {\n" +
                    "      location: 'test/graphql/Query.json';\n" +
                    "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Query\", \"type\": \"object\", \"properties\": {}}';\n" +
                    "    }\n" +
                    "  ];\n" +
                    "}\n" +
                    "\n" +
                    "Binding test::graphql::QueryBinding\n" +
                    "{\n" +
                    "  schemaSet: test::graphql::QuerySchema;\n" +
                    "  contentType: 'application/json';\n" +
                    "  modelIncludes: [ test::graphql::Query ];\n" +
                    "}\n" +
                    "###GraphQL\n" +
                    "GraphQLStore test::graphql::TestStore\n" +
                    "{\n" +
                    "  schema: {\n" +
                    "type Query {\n" +
                    "  employees: [Employee]\n" +
                    "  managers: [Manager]\n" +
                    "}\n" +
                    "\n" +
                    "type Employee {\n" +
                    "  firstName: String\n" +
                    "  manager: Manager\n" +
                    "}\n" +
                    "\n" +
                    "type Manager {\n" +
                    "  name: String\n" +
                    "}\n" +
                    "};\n" +
                    "}\n" +
                    "###Mapping\n" +
                    "Mapping test::graphql::TestMapping\n" +
                    "(\n" +
                    "  *test::graphql::Query: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::TestStore] 'Query'\n" +
                    "    ~binding test::graphql::QueryBinding\n" +
                    "  }\n" +
                    "  *test::graphql::Employee: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::TestStore] 'Employee'\n" +
                    "    ~binding test::graphql::EmployeeBinding\n" +
                    "  }\n" +
                    "  *test::graphql::Manager: GraphQL\n" +
                    "  {\n" +
                    "    ~typeName [test::graphql::TestStore] 'Manager'\n" +
                    "    ~binding test::graphql::ManagerBinding\n" +
                    "  }\n" +
                    ")\n");

        PureModel pureModel = result.getTwo();
        Assert.assertNotNull(pureModel.getMapping("test::graphql::TestMapping"));
    }

    @Test
    public void testDependentOnlyClassMappingNeedsNoTypeName()
    {
        Pair<PureModelContextData, PureModel> result = test("###Pure\n" +
                "Class test::graphql::Employee\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  department: test::graphql::Department[1];\n" +
                "}\n" +
                "Class test::graphql::Department\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "###ExternalFormat\n" +
                "SchemaSet test::graphql::TestSchema\n" +
                "{\n" +
                "  format: JSON;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      location: 'test/graphql/Employee.json';\n" +
                "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Employee\", \"type\": \"object\", \"properties\": {\"firstName\": {\"type\": \"string\"}, \"department\": {\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}, \"required\": [\"name\"]}}, \"required\": [\"firstName\", \"department\"]}';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding test::graphql::TestBinding\n" +
                "{\n" +
                "  schemaSet: test::graphql::TestSchema;\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [ test::graphql::Employee, test::graphql::Department ];\n" +
                "}\n" +
                "###GraphQL\n" +
                "GraphQLStore test::graphql::TestStore\n" +
                "{\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::graphql::TestMapping\n" +
                "(\n" +
                "  *test::graphql::Employee: GraphQL\n" +
                "  {\n" +
                "    ~typeName [test::graphql::TestStore] 'Employee'\n" +
                "    ~binding test::graphql::TestBinding\n" +
                "  }\n" +
                ")\n");

        PureModel pureModel = result.getTwo();
        Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation departmentEmbed =
                (Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation) pureModel.getMapping("test::graphql::TestMapping")._classMappings()
                        .detect(c -> c instanceof Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation);
        Assert.assertNotNull(departmentEmbed);
    }

    // Compile-time risk check for interface/union polymorphism support: a GraphQL ClassMapping using the
    // core, store-agnostic `extends(SuperClassMappingId)` clause must wire _superSetImplementationId and
    // resolve the subtype's full (inherited + own) property set via the generic Binding-validation
    // framework (bindingDetail.mappedPropertiesForClass), not just compile without erroring.
    private static final String INHERITANCE_MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::TestParentObject\n" +
            "{\n" +
            "  id: String[1];\n" +
            "}\n" +
            "Class test::graphql::Worker extends test::graphql::TestParentObject\n" +
            "{\n" +
            "  employeeId: String[1];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::TestSchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/Worker.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Worker\", \"type\": \"object\", \"properties\": {\"id\": {\"type\": \"string\"}, \"employeeId\": {\"type\": \"string\"}}, \"required\": [\"id\", \"employeeId\"]}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::TestBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::TestSchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::TestParentObject, test::graphql::Worker ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::TestStore\n" +
            "{\n" +
            "}\n" +
            "###Mapping\n" +
            "Mapping test::graphql::TestMapping\n" +
            "(\n" +
            "  *test::graphql::TestParentObject[TestParentObjectMapping]: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::TestStore] 'TestParentObject'\n" +
            "    ~binding test::graphql::TestBinding\n" +
            "  }\n" +
            "  *test::graphql::Worker[WorkerMapping] extends[TestParentObjectMapping]: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::TestStore] 'Worker'\n" +
            "    ~binding test::graphql::TestBinding\n" +
            "  }\n" +
            ")\n";

    @Test
    public void testClassMappingExtendsCompilesAndResolvesInheritedProperties()
    {
        Pair<PureModelContextData, PureModel> result = test(INHERITANCE_MODEL_AND_BINDING);

        PureModel pureModel = result.getTwo();
        Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation workerSetImplementation =
                (Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) pureModel.getMapping("test::graphql::TestMapping")._classMappings()
                        .detect(c -> c instanceof Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation
                                && "Worker".equals(((Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) c)._typeName()));

        Assert.assertEquals("WorkerMapping", workerSetImplementation._id());
        Assert.assertEquals("expected Worker's class mapping to wire _superSetImplementationId from its 'extends(...)' clause",
                "TestParentObjectMapping", workerSetImplementation._superSetImplementationId());
        Assert.assertTrue("expected Worker's own property mapping for 'employeeId'",
                workerSetImplementation._propertyMappings().anySatisfy(pm -> pm._property()._name().equals("employeeId")));
        Assert.assertTrue("expected Worker's property mappings to also include the inherited 'id' property from TestParentObject",
                workerSetImplementation._propertyMappings().anySatisfy(pm -> pm._property()._name().equals("id")));
    }

    // Verifies a NESTED (not root) interface-typed property: Department.manager is declared as TestParentObject,
    // with Worker/Candidate as real Pure subtypes - but neither gets its own GraphQL ClassMapping. Only
    // Department is mapped; the embedded mapping for 'manager' should carry just TestParentObject's own field
    // ('id'), with Worker/Candidate's extra fields resolved purely via internalize's discriminator
    // mechanism at execution time, independent of the Mapping/PropertyMapping structure.
    private static final String NESTED_POLYMORPHIC_PROPERTY_MODEL_AND_BINDING =
            "###Pure\n" +
            "Class test::graphql::TestParentObject\n" +
            "{\n" +
            "  id: String[1];\n" +
            "}\n" +
            "Class test::graphql::Worker extends test::graphql::TestParentObject\n" +
            "{\n" +
            "  employeeId: String[1];\n" +
            "}\n" +
            "Class test::graphql::Candidate extends test::graphql::TestParentObject\n" +
            "{\n" +
            "  applicationId: String[1];\n" +
            "}\n" +
            "Class test::graphql::Department\n" +
            "{\n" +
            "  name: String[1];\n" +
            "  manager: test::graphql::TestParentObject[1];\n" +
            "}\n" +
            "###ExternalFormat\n" +
            "SchemaSet test::graphql::TestSchema\n" +
            "{\n" +
            "  format: JSON;\n" +
            "  schemas: [\n" +
            "    {\n" +
            "      location: 'test/graphql/Department.json';\n" +
            "      content: '{\"$schema\": \"http://json-schema.org/draft-07/schema#\", \"title\": \"test::graphql::Department\", \"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, \"manager\": {\"$ref\": \"#/definitions/test::graphql::TestParentObject\"}}, \"required\": [\"name\", \"manager\"], \"definitions\": {\"test::graphql::TestParentObject\": {\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"string\"}}, \"required\": [\"id\"], \"discriminator\": {\"propertyName\": \"__typename\", \"mapping\": {\"Worker\": \"#/definitions/test::graphql::Worker\", \"Candidate\": \"#/definitions/test::graphql::Candidate\"}}}, \"test::graphql::Worker\": {\"allOf\": [{\"$ref\": \"#/definitions/test::graphql::TestParentObject\"}], \"type\": \"object\", \"properties\": {\"employeeId\": {\"type\": \"string\"}}}, \"test::graphql::Candidate\": {\"allOf\": [{\"$ref\": \"#/definitions/test::graphql::TestParentObject\"}], \"type\": \"object\", \"properties\": {\"applicationId\": {\"type\": \"string\"}}}}}';\n" +
            "    }\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "Binding test::graphql::TestBinding\n" +
            "{\n" +
            "  schemaSet: test::graphql::TestSchema;\n" +
            "  contentType: 'application/json';\n" +
            "  modelIncludes: [ test::graphql::Department, test::graphql::TestParentObject, test::graphql::Worker, test::graphql::Candidate ];\n" +
            "}\n" +
            "###GraphQL\n" +
            "GraphQLStore test::graphql::TestStore\n" +
            "{\n" +
            "}\n" +
            "###Mapping\n" +
            "Mapping test::graphql::TestMapping\n" +
            "(\n" +
            "  *test::graphql::Department: GraphQL\n" +
            "  {\n" +
            "    ~typeName [test::graphql::TestStore] 'Department'\n" +
            "    ~binding test::graphql::TestBinding\n" +
            "  }\n" +
            ")\n";

    @Test
    public void testNestedPolymorphicPropertyEmbedsOnlyBaseTypeFieldsWithNoSubtypeClassMappings()
    {
        Pair<PureModelContextData, PureModel> result = test(NESTED_POLYMORPHIC_PROPERTY_MODEL_AND_BINDING);

        PureModel pureModel = result.getTwo();
        Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation departmentSetImplementation =
                (Root_meta_external_store_graphQL_metamodel_pure_RootGraphQLInstanceSetImplementation) pureModel.getMapping("test::graphql::TestMapping")._classMappings().getFirst();

        PropertyMapping managerPropertyMapping = departmentSetImplementation._propertyMappings().detect(pm -> pm._property()._name().equals("manager"));
        Assert.assertTrue("expected 'manager' to resolve to a real embed", managerPropertyMapping instanceof Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation);

        Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation managerEmbed = (Root_meta_external_store_graphQL_metamodel_pure_EmbeddedGraphQLSetImplementation) managerPropertyMapping;
        Assert.assertEquals("TestParentObject", managerEmbed._class()._name());
        Assert.assertEquals("expected the embed to carry only TestParentObject's own field, not Worker/Candidate's",
                1, managerEmbed._propertyMappings().size());
        Assert.assertTrue(managerEmbed._propertyMappings().anySatisfy(pm -> pm._property()._name().equals("id")));

        Assert.assertEquals("expected exactly Department (root) and its TestParentObject embed - no separate Worker/Candidate class mappings",
                2, pureModel.getMapping("test::graphql::TestMapping")._classMappings().size());
        Assert.assertFalse("expected no class mapping for Worker - it needs none",
                pureModel.getMapping("test::graphql::TestMapping")._classMappings().anySatisfy(c -> "Worker".equals(c._class()._name())));
        Assert.assertFalse("expected no class mapping for Candidate - it needs none",
                pureModel.getMapping("test::graphql::TestMapping")._classMappings().anySatisfy(c -> "Candidate".equals(c._class()._name())));
    }
}
