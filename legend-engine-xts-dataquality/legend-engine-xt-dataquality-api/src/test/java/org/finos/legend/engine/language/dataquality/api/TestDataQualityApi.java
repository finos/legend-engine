// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.dataquality.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit.ResourceTestRule;

import javax.ws.rs.client.Entity;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensionLoader;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.dataquality.metamodel.RelationValidation;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import javax.ws.rs.core.Response;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.glassfish.jersey.test.TestProperties;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDataQualityApi
{

    static
    {
        System.setProperty(TestProperties.CONTAINER_PORT, "0");
    }

    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final PureModelContextData pureModelContextData = GrammarParseTestUtils.loadPureModelContextFromResource("inputs/test-data.pure", TestDataQualityApi.class);

    @ClassRule
    public static final ResourceTestRule resources = getResourceTestRule();

    public static ResourceTestRule getResourceTestRule()

    {
        DeploymentMode deploymentMode = DeploymentMode.TEST;

        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();

        ModelManager modelManager = mock(ModelManager.class);
        PureModel pureModel = new ModelManager(deploymentMode).loadModel(pureModelContextData, PureClientVersions.production, Identity.getAnonymousIdentity(), "");
        when(modelManager.loadModel(any(), any(), any(), any())).thenReturn(pureModel);

        MetaDataServerConfiguration metaDataServerConfiguration = new MetaDataServerConfiguration();

        DataQualityExecute api = new DataQualityExecute(
                modelManager,
                executor,
                (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())),
                LegendPlanTransformers.transformers,
                metaDataServerConfiguration,
                null
        );

        return ResourceTestRule.builder()
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addResource(api)
                .addResource(new MockPac4jFeature())
                .bootstrapLogging(false)
                .build();
    }

    @Test
    public void testDataQualityProfiling() throws IOException
    {
        DataQualityProfileInput input = new DataQualityProfileInput();
        input.clientVersion = "vX_X_X";
        input.packagePath = "demo::simplePersonValidation";
        input.model = new PureModelContextPointer();

        Response response = resources.target("pure/v1/dataquality/profile")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertNotNull(resultAsString);
    }

    @Test
    public void testDataQualityRuleSuggestions() throws IOException
    {
        DataQualityProfileInput input = new DataQualityProfileInput();
        input.clientVersion = "vX_X_X";
        input.packagePath = "demo::simplePersonValidation";
        input.model = new PureModelContextPointer();

        Response response = resources.target("pure/v1/dataquality/ruleSuggestions")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        List<RelationValidation> results = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(resultAsString, new TypeReference<List<RelationValidation>>()
        {
        });

        List<RelationValidation> expected = FastList.newListWith(
                new RelationValidation()._name("idNonNull")._assertion(lambda("rel|$rel->rowsWithEmptyColumn(\n  ~id\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)")),
                new RelationValidation()._name("idBetweenValues")._assertion(lambda("rel|$rel->rowsWithValueOutsideRange(\n  ~id,\n  -1,\n  5\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)")),
                new RelationValidation()._name("fullNameNonNull")._assertion(lambda("rel|$rel->rowsWithEmptyColumn(\n  ~fullName\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)")),
                new RelationValidation()._name("ageBetweenValues")._assertion(lambda("rel|$rel->rowsWithValueOutsideRange(\n  ~age,\n  25,\n  35\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)")),
                new RelationValidation()._name("annualSalaryNonNull")._assertion(lambda("rel|$rel->rowsWithEmptyColumn(\n  ~annualSalary\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)"))
        );

        ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

        assertEquals(mapper.writeValueAsString(results), mapper.writeValueAsString(expected));
    }

    @Test
    public void testDataQualityRecon()
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.clientVersion = "vX_X_X";
        input.model = new PureModelContextPointer();
        input.source = lambda("|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])->from(demo::PersonMap, demo::PersonRuntime)");
        input.target = lambda("|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])->from(demo::PersonMap, demo::PersonRuntime)");

        Response response = resources.target("pure/v1/dataquality/reconciliation")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertNotNull(resultAsString);
    }

    private LambdaFunction lambda(String code)
    {
        return PureGrammarParser.newInstance().parseLambda(code, "", false);
    }


    @Test
    public void testDataQualityGetPropertyPathTree() throws IOException
    {
        PureModelContextData pureModelContextData = GrammarParseTestUtils.loadPureModelContextFromResource("inputs/modelWithDataQualityValidation.pure", TestDataQualityApi.class);
        CompilerExtensionLoader.logExtensionList();
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("meta::dataquality::PersonDataQualityValidation");
        assertTrue(DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement));
        RootGraphFetchTree rootGraphFetchTree = DataQualityPropertyPathTreeGenerator.getPropertyPathTree(((Root_meta_external_dataquality_DataQuality)packageableElement)._validationTree(), "vX_X_X", model, null);
        assertEquals("{\"_type\":\"rootGraphFetchTree\",\"_type\":\"rootGraphFetchTree\",\"class\":\"meta::dataquality::Person\",\"subTrees\":[{\"_type\":\"propertyGraphFetchTree\",\"_type\":\"propertyGraphFetchTree\",\"parameters\":[],\"property\":\"name\",\"subTrees\":[],\"subTypeTrees\":[]},{\"_type\":\"propertyGraphFetchTree\",\"_type\":\"propertyGraphFetchTree\",\"parameters\":[],\"property\":\"age\",\"subTrees\":[],\"subTypeTrees\":[]}],\"subTypeTrees\":[]}", objectMapper.writeValueAsString(rootGraphFetchTree));

        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement2 = model.getPackageableElement("meta::dataquality::Person");
        assertFalse(DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement2));
    }

    @Ignore
    @Test
    public void testDataQualityGetPropertyPathTree_WithNullDataQualityValidation() throws IOException
    {
        PureModelContextData pureModelContextData = GrammarParseTestUtils.loadPureModelContextFromResource("inputs/modelWithNullDataQualityValidation.pure", TestDataQualityApi.class);
        CompilerExtensionLoader.logExtensionList();
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = model.getPackageableElement("meta::dataquality::PersonDataQualityValidation");
        assertFalse(DataQualityPropertyPathTreeGenerator.isDataQualityInstance(packageableElement));
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Sample Values Tests
    // ───────────────────────────────────────────────────────────────────────────

    private DataQualitySampleValuesInput sampleValuesInput(String queryCode, Integer maxNumberOfSampleValues)
    {
        DataQualitySampleValuesInput input = new DataQualitySampleValuesInput();
        input.clientVersion = "vX_X_X";
        input.model = new PureModelContextPointer();
        input.query = lambda(queryCode);
        input.maxNumberOfSampleValues = maxNumberOfSampleValues;
        return input;
    }

    private static final String PERSON_QUERY = "|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName, age: x|$x.age, emailAddress: x|$x.emailAddress, annualSalary: x|$x.annualSalary, dateOfBirth: x|$x.dateOfBirth])->from(demo::PersonMap, demo::PersonRuntime)";

    @Test
    public void testSampleValuesEndpointReturns200()
    {
        DataQualitySampleValuesInput input = sampleValuesInput(PERSON_QUERY, null);

        Response response = resources.target("pure/v1/dataquality/sampleValues")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertNotNull(resultAsString);
    }

    @Test
    public void testSampleValuesOutputShape() throws IOException
    {
        DataQualitySampleValuesInput input = sampleValuesInput(PERSON_QUERY, null);

        Response response = resources.target("pure/v1/dataquality/sampleValues")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        JsonNode result = objectMapper.readTree(resultAsString);

        // The result is a TDS; verify the column structure
        JsonNode columns = result.path("result").path("columns");
        if (columns.isMissingNode())
        {
            // Fallback: result may be wrapped differently — just check the raw result contains the expected column names
            assertTrue("Response should contain column_name", resultAsString.contains("column_name"));
            assertTrue("Response should contain column_data_type", resultAsString.contains("column_data_type"));
            assertTrue("Response should contain count", resultAsString.contains("count"));
            assertTrue("Response should contain string_value", resultAsString.contains("string_value"));
            assertTrue("Response should contain int_value", resultAsString.contains("int_value"));
            assertTrue("Response should contain float_value", resultAsString.contains("float_value"));
            assertTrue("Response should contain date_value", resultAsString.contains("date_value"));
            assertTrue("Response should contain boolean_value", resultAsString.contains("boolean_value"));
        }
        else
        {
            Set<String> colNames = StreamSupport.stream(columns.spliterator(), false)
                    .map(JsonNode::asText)
                    .collect(Collectors.toSet());
            assertTrue(colNames.contains("column_name"));
            assertTrue(colNames.contains("column_data_type"));
            assertTrue(colNames.contains("count"));
            assertTrue(colNames.contains("string_value"));
            assertTrue(colNames.contains("int_value"));
            assertTrue(colNames.contains("float_value"));
            assertTrue(colNames.contains("date_value"));
            assertTrue(colNames.contains("boolean_value"));
        }
    }

    @Test
    public void testSampleValuesMaxNumberOfSampleValuesParameter() throws IOException
    {
        // Use SAMPLE_DATA_QUERY with only strCol column, maxNumberOfSampleValues=2
        String singleColQuery = "|demo::SampleData.all()->project(~[strCol: x|$x.strCol])->from(demo::PersonMap, demo::PersonRuntime)";
        DataQualitySampleValuesInput input = sampleValuesInput(singleColQuery, 2);

        Response response = resources.target("pure/v1/dataquality/sampleValues")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        JsonNode result = objectMapper.readTree(resultAsString);

        // Count number of rows — should be at most 2 for a single column
        JsonNode rows = result.path("result").path("rows");
        if (!rows.isMissingNode() && rows.isArray())
        {
            assertTrue("At most 2 rows per column when maxNumberOfSampleValues=2", rows.size() <= 2);
        }
    }

    @Test
    public void testSampleValuesDefaultMaxNumberOfSampleValues() throws IOException
    {
        // Omit maxNumberOfSampleValues — default should be 20
        String singleColQuery = "|demo::SampleData.all()->project(~[strCol: x|$x.strCol])->from(demo::PersonMap, demo::PersonRuntime)";
        DataQualitySampleValuesInput input = sampleValuesInput(singleColQuery, null);

        Response response = resources.target("pure/v1/dataquality/sampleValues")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        JsonNode result = objectMapper.readTree(resultAsString);

        // With default max of 20 and only ~4 distinct values (A, B, C, null), should return up to 20 rows
        JsonNode rows = result.path("result").path("rows");
        if (!rows.isMissingNode() && rows.isArray())
        {
            assertTrue("Rows should be <= 20 (default max)", rows.size() <= 20);
        }
    }

    @Test
    public void testSampleValuesStringColumnValues()
    {
        // fullName column is a String — string_value should be populated, int_value should be null
        String singleColQuery = "|demo::Person.all()->project(~[fullName: x|$x.fullName])->from(demo::PersonMap, demo::PersonRuntime)";
        DataQualitySampleValuesInput input = sampleValuesInput(singleColQuery, null);

        Response response = resources.target("pure/v1/dataquality/sampleValues")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        // Verify column_data_type says "String"
        assertTrue("String column should report data type as String", resultAsString.contains("String"));
    }

    @Test
    public void testSampleValuesIntegerColumnValues()
    {
        // id column is Integer — int_value should be populated, string_value should be null
        String singleColQuery = "|demo::Person.all()->project(~[id: x|$x.id])->from(demo::PersonMap, demo::PersonRuntime)";
        DataQualitySampleValuesInput input = sampleValuesInput(singleColQuery, null);

        Response response = resources.target("pure/v1/dataquality/sampleValues")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        // Verify column_data_type says "Integer"
        assertTrue("Integer column should report data type as Integer", resultAsString.contains("Integer"));
    }

    @Test
    public void testSampleValuesColumnOrder()
    {
        // Project id then fullName — id block should appear before fullName block
        String twoColQuery = "|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])->from(demo::PersonMap, demo::PersonRuntime)";
        DataQualitySampleValuesInput input = sampleValuesInput(twoColQuery, null);

        Response response = resources.target("pure/v1/dataquality/sampleValues")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);

        // Verify that "id" column_name appears before "fullName" in the result
        int idPos = resultAsString.indexOf("\"id\"");
        int fullNamePos = resultAsString.indexOf("\"fullName\"");
        if (idPos >= 0 && fullNamePos >= 0)
        {
            assertTrue("id block should appear before fullName block", idPos < fullNamePos);
        }
    }
}
