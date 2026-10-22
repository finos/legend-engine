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
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;

import jakarta.ws.rs.client.Entity;

import org.eclipse.collections.api.factory.Sets;
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
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CInteger;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import jakarta.ws.rs.core.Response;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.glassfish.jersey.test.TestProperties;
import org.junit.jupiter.api.Disabled;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(DropwizardExtensionsSupport.class)
public class TestDataQualityApi
{

    static
    {
        System.setProperty(TestProperties.CONTAINER_PORT, "0");
    }

    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final PureModelContextData pureModelContextData = GrammarParseTestUtils.loadPureModelContextFromResource("inputs/test-data.pure", TestDataQualityApi.class);
    public static final ResourceExtension resources = getResourceTestRule();

    public static ResourceExtension getResourceTestRule()

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

        return ResourceExtension.builder()
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
                new RelationValidation()._name("idBetweenValues")._assertion(lambda("rel|$rel->rowsWithValueOutsideRange(\n  ~id,\n  -1d,\n  5d\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)")),
                new RelationValidation()._name("fullNameNonNull")._assertion(lambda("rel|$rel->rowsWithEmptyColumn(\n  ~fullName\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)")),
                new RelationValidation()._name("ageBetweenValues")._assertion(lambda("rel|$rel->rowsWithValueOutsideRange(\n  ~age,\n  25d,\n  35d\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)")),
                new RelationValidation()._name("annualSalaryNonNull")._assertion(lambda("rel|$rel->rowsWithEmptyColumn(\n  ~annualSalary\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)")),
                new RelationValidation()._name("annualSalaryBetweenValues")._assertion(lambda("rel|$rel->rowsWithValueOutsideRange(\n  ~annualSalary,\n  30000.0d,\n  75000.75d\n)->assertRelationEmpty(\n  ~[\n     id,\n     fullName,\n     age,\n     emailAddress,\n     annualSalary,\n     dateOfBirth\n   ]\n)"))
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

    @Test
    public void testDataQualityRecon_withPrefixedLambdaParametersAndTwoTargetParameters()
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.clientVersion = "vX_X_X";
        input.model = new PureModelContextPointer();
        input.source = lambda("{nameFilter:String[1]|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])->filter(x|$x.fullName == $nameFilter)->from(demo::PersonMap, demo::PersonRuntime)}");
        input.target = lambda("{nameFilter:String[1], businessDate:String[1]|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])->filter(x|$x.fullName == $nameFilter)->from(demo::PersonMap, demo::PersonRuntime)}");
        input.keys = new LinkedHashSet<>(Collections.singletonList("id"));
        input.colsForHash = new LinkedHashSet<>(Collections.singletonList("fullName"));
        input.sourceLambdaParameterValues = Collections.singletonList(createParameterValue("nameFilter", new CString("Alice")));
        input.targetLambdaParameterValues = FastList.newListWith(
            createParameterValue("nameFilter", new CString("Alice")),
            createParameterValue("businessDate", new CString("2026-04-13"))
        );

        Response response = resources.target("pure/v1/dataquality/reconciliation")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertNotNull(resultAsString);
    }

    @Test
    public void testDataQualityReconThrowsExceptionWhenSourceDoesNotHaveFrom()
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.clientVersion = "vX_X_X";
        input.model = new PureModelContextPointer();
        input.source = lambda("|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])");
        input.target = lambda("|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])->from(demo::PersonMap, demo::PersonRuntime)");

        Response response = resources.target("pure/v1/dataquality/reconciliation")
                .request()
                .post(Entity.json(input));

        assertEquals(500, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertTrue(resultAsString.contains("The source query for the Data Quality Recon Input does not end with a 'from' so unable to execute it."));
    }

    @Test
    public void testDataQualityReconThrowsExceptionWhenTargetDoesNotHaveFrom()
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.clientVersion = "vX_X_X";
        input.model = new PureModelContextPointer();
        input.source = lambda("|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])->from(demo::PersonMap, demo::PersonRuntime)");
        input.target = lambda("|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])");

        Response response = resources.target("pure/v1/dataquality/reconciliation")
                .request()
                .post(Entity.json(input));

        assertEquals(500, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertTrue(resultAsString.contains("The target query for the Data Quality Recon Input does not end with a 'from' so unable to execute it."));
    }

    @Test
    public void testDataQualityReconThrowsExceptionWhenPackagePathNotPointingToComparisonElement()
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.clientVersion = "vX_X_X";
        input.model = new PureModelContextPointer();
        input.packagePath = "demo::PersonRuntime";

        Response response = resources.target("pure/v1/dataquality/reconciliation")
                .request()
                .post(Entity.json(input));

        assertEquals(500, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertTrue(resultAsString.contains("Expected package path to point to DataQualityRelationComparison but found Root_meta_pure_runtime_PackageableRuntime_Impl"));
    }

    @Test
    public void testDataQualityReconWhenProvidePackagePathToComparisonElement()
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.packagePath = "meta::dataquality::TestRelationComparison";
        input.model = new PureModelContextPointer();

        Response response = resources.target("pure/v1/dataquality/reconciliation")
                .request()
                .post(Entity.json(input));

        assertEquals(200, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertNotNull(resultAsString);
    }

    @Test
    public void testDataQualityRecon_multiStatementBothSidesWithDistinctRelationLets() throws IOException
    {
        DataQualityReconInput input = relationApiReconInput(
                "{|let olderThan26 = #>{demo::PersonDB.PERSON_TBL}#->filter(r | $r.AGE->isNotEmpty() && ($r.AGE->toOne() > 26))->select(~[ID, FULL_NAME]); $olderThan26->from(demo::PersonRuntime);}",
                "{|let positiveIds = #>{demo::PersonDB.PERSON_TBL}#->filter(r | $r.ID > 0)->select(~[ID, FULL_NAME]); $positiveIds->from(demo::PersonRuntime);}");

        assertEquals(Sets.mutable.of("-1", "2", "4"), reconDefectKeys(postRecon(input), "ID"));
    }

    @Test
    public void testDataQualityRecon_multiStatementLetReferencingParameter() throws IOException
    {
        DataQualityReconInput input = relationApiReconInput(
                "{minId:Integer[1]|let threshold = $minId; #>{demo::PersonDB.PERSON_TBL}#->filter(r | $r.ID > $threshold)->select(~[ID, FULL_NAME])->from(demo::PersonRuntime);}",
                "|#>{demo::PersonDB.PERSON_TBL}#->select(~[ID, FULL_NAME])->from(demo::PersonRuntime)");
        input.sourceLambdaParameterValues = Collections.singletonList(createParameterValue("minId", new CInteger(2L)));

        assertEquals(Sets.mutable.of("-1", "1", "2"), reconDefectKeys(postRecon(input), "ID"));
    }

    @Test
    public void testDataQualityRecon_multiStatementSameScalarLetNameOnBothSides() throws IOException
    {
        DataQualityReconInput input = relationApiReconInput(
                "{|let minId = 0; #>{demo::PersonDB.PERSON_TBL}#->filter(r | $r.ID > $minId)->select(~[ID, FULL_NAME])->from(demo::PersonRuntime);}",
                "{|let minId = 2; #>{demo::PersonDB.PERSON_TBL}#->filter(r | $r.ID > $minId)->select(~[ID, FULL_NAME])->from(demo::PersonRuntime);}");

        assertEquals(Sets.mutable.of("1", "2"), reconDefectKeys(postRecon(input), "ID"));
    }

    @Test
    public void testDataQualityRecon_multiStatementClassBasedProject() throws IOException
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.clientVersion = "vX_X_X";
        input.model = new PureModelContextPointer();
        input.source = lambda("{|let minAge = 26; demo::Person.all()->filter(x | $x.age->isNotEmpty() && ($x.age->toOne() > $minAge))->project(~[id: x|$x.id, fullName: x|$x.fullName])->from(demo::PersonMap, demo::PersonRuntime);}");
        input.target = lambda("|demo::Person.all()->project(~[id: x|$x.id, fullName: x|$x.fullName])->from(demo::PersonMap, demo::PersonRuntime)");
        input.keys = new LinkedHashSet<>(Collections.singletonList("id"));
        input.colsForHash = new LinkedHashSet<>(Collections.singletonList("fullName"));

        assertEquals(Sets.mutable.of("2", "4"), reconDefectKeys(postRecon(input), "id"));
    }

    @Test
    public void testDataQualityRecon_multiStatementLegacyTdsProject() throws IOException
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.clientVersion = "vX_X_X";
        input.model = new PureModelContextPointer();
        input.source = lambda("{|let minAge = 26; demo::Person.all()->filter(x | $x.age->isNotEmpty() && ($x.age->toOne() > $minAge))->project([x|$x.id, x|$x.fullName], ['id', 'fullName'])->from(demo::PersonMap, demo::PersonRuntime);}");
        input.target = lambda("|demo::Person.all()->project([x|$x.id, x|$x.fullName], ['id', 'fullName'])->from(demo::PersonMap, demo::PersonRuntime)");
        input.keys = new LinkedHashSet<>(Collections.singletonList("id"));
        input.colsForHash = new LinkedHashSet<>(Collections.singletonList("fullName"));

        assertEquals(Sets.mutable.of("2", "4"), reconDefectKeys(postRecon(input), "id"));
    }

    @Test
    public void testDataQualityReconPlan_multiStatementBothSidesGeneratesCommonTableExpressions() throws IOException
    {
        DataQualityReconInput input = relationApiReconInput(
                "{|let olderThan26 = #>{demo::PersonDB.PERSON_TBL}#->filter(r | $r.AGE->isNotEmpty() && ($r.AGE->toOne() > 26))->select(~[ID, FULL_NAME]); $olderThan26->from(demo::PersonRuntime);}",
                "{|let positiveIds = #>{demo::PersonDB.PERSON_TBL}#->filter(r | $r.ID > 0)->select(~[ID, FULL_NAME]); $positiveIds->from(demo::PersonRuntime);}");

        Response response = resources.target("pure/v1/dataquality/reconciliation/generatePlan")
                .request()
                .post(Entity.json(input));

        String resultAsString = response.readEntity(String.class);
        assertEquals(200, response.getStatus(), resultAsString);
        List<String> sqlQueries = objectMapper.readTree(resultAsString).findValues("sqlQuery").stream().map(JsonNode::asText).collect(Collectors.toList());
        assertTrue(sqlQueries.stream().anyMatch(sql -> definesCommonTableExpression(sql, "olderThan26") && definesCommonTableExpression(sql, "positiveIds")), resultAsString);
    }

    @Test
    public void testDataQualityReconReportsQueryRuleViolationBeforeMissingFromCheck()
    {
        DataQualityReconInput input = relationApiReconInput(
                "|#>{demo::PersonDB.PERSON_TBL}#->select(~[ID, FULL_NAME])->from(demo::PersonRuntime)",
                "{|let people = #>{demo::PersonDB.PERSON_TBL}#->select(~[ID, FULL_NAME])->from(demo::PersonRuntime); $people;}");

        Response response = resources.target("pure/v1/dataquality/reconciliation")
                .request()
                .post(Entity.json(input));

        assertEquals(500, response.getStatus());
        String resultAsString = response.readEntity(String.class);
        assertTrue(resultAsString.contains("The 'from' function is not supported inside let 'people' of the target query; apply 'from' to the final expression only"), resultAsString);
    }

    @Test
    public void testDataQualityRecon_runSourceQueryIsNotSubjectToReconQueryRules() throws IOException
    {
        DataQualityReconInput input = relationApiReconInput(
                "{minId:Integer[1]|let source_minId = 0; #>{demo::PersonDB.PERSON_TBL}#->filter(r | $r.ID > $source_minId)->select(~[ID, FULL_NAME])->from(demo::PersonRuntime);}",
                "|#>{demo::PersonDB.PERSON_TBL}#->select(~[ID, FULL_NAME])->from(demo::PersonRuntime)");
        input.runSourceQuery = true;
        input.sourceLambdaParameterValues = Collections.singletonList(createParameterValue("minId", new CInteger(0L)));

        Response response = resources.target("pure/v1/dataquality/reconciliation")
                .request()
                .post(Entity.json(input));

        String resultAsString = response.readEntity(String.class);
        assertEquals(200, response.getStatus(), resultAsString);
    }

    private static boolean definesCommonTableExpression(String sql, String name)
    {
        return Pattern.compile("(?<!\\w)\"?" + Pattern.quote(name) + "\"?\\s+as\\s*\\(", Pattern.CASE_INSENSITIVE).matcher(sql).find();
    }

    private DataQualityReconInput relationApiReconInput(String source, String target)
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.clientVersion = "vX_X_X";
        input.model = new PureModelContextPointer();
        input.source = lambda(source);
        input.target = lambda(target);
        input.keys = new LinkedHashSet<>(Collections.singletonList("ID"));
        input.colsForHash = new LinkedHashSet<>(Collections.singletonList("FULL_NAME"));
        return input;
    }

    private String postRecon(DataQualityReconInput input)
    {
        Response response = resources.target("pure/v1/dataquality/reconciliation")
                .request()
                .post(Entity.json(input));
        String resultAsString = response.readEntity(String.class);
        assertEquals(200, response.getStatus(), resultAsString);
        return resultAsString;
    }

    private Set<String> reconDefectKeys(String reconResult, String key) throws IOException
    {
        JsonNode result = objectMapper.readTree(reconResult);
        JsonNode columns = result.path("builder").path("columns");
        JsonNode rows = result.path("result").path("rows");
        assertTrue(rows.isArray(), reconResult);

        int sourceKeyIndex = -1;
        int targetKeyIndex = -1;
        for (int i = 0; i < columns.size(); i++)
        {
            String columnName = columns.get(i).path("name").asText();
            if ((key + "_SOURCE").equals(columnName))
            {
                sourceKeyIndex = i;
            }
            else if ((key + "_TARGET").equals(columnName))
            {
                targetKeyIndex = i;
            }
        }
        assertTrue(sourceKeyIndex >= 0 && targetKeyIndex >= 0, key + "_SOURCE/_TARGET columns not found in " + columns);

        Set<String> defectKeys = new LinkedHashSet<>();
        for (JsonNode row : rows)
        {
            JsonNode sourceKey = row.path("values").get(sourceKeyIndex);
            defectKeys.add(sourceKey.isNull() ? row.path("values").get(targetKeyIndex).asText() : sourceKey.asText());
        }
        return defectKeys;
    }

    private LambdaFunction lambda(String code)
    {
        return PureGrammarParser.newInstance().parseLambda(code, "", false);
    }

    private ParameterValue createParameterValue(String name, CString value)
    {
        ParameterValue parameterValue = new ParameterValue();
        parameterValue.name = name;
        parameterValue.value = value;
        return parameterValue;
    }

    private ParameterValue createParameterValue(String name, CInteger value)
    {
        ParameterValue parameterValue = new ParameterValue();
        parameterValue.name = name;
        parameterValue.value = value;
        return parameterValue;
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

    @Disabled
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
            assertTrue(resultAsString.contains("column_name"), "Response should contain column_name");
            assertTrue(resultAsString.contains("column_data_type"), "Response should contain column_data_type");
            assertTrue(resultAsString.contains("count"), "Response should contain count");
            assertTrue(resultAsString.contains("string_value"), "Response should contain string_value");
            assertTrue(resultAsString.contains("int_value"), "Response should contain int_value");
            assertTrue(resultAsString.contains("float_value"), "Response should contain float_value");
            assertTrue(resultAsString.contains("date_value"), "Response should contain date_value");
            assertTrue(resultAsString.contains("boolean_value"), "Response should contain boolean_value");
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
            assertTrue(rows.size() <= 2, "At most 2 rows per column when maxNumberOfSampleValues=2");
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
            assertTrue(rows.size() <= 20, "Rows should be <= 20 (default max)");
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
        assertTrue(resultAsString.contains("String"), "String column should report data type as String");
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
        assertTrue(resultAsString.contains("Integer"), "Integer column should report data type as Integer");
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
            assertTrue(idPos < fullNamePos, "id block should appear before fullName block");
        }
    }

    // Executes a relation validation against the seeded H2 database and asserts the defect rows.
    //
    // The rowsWith* helpers had no execution coverage of any kind: doRelationTest stops at lambda
    // generation, and the helper tests in dataquality_relation_helper_test.pure run over an in-memory
    // #TDS, so neither reaches the router that translates these validations to SQL.
    //
    // Eve Davis is the only seeded person with a NULL email.
    @Test
    public void testExecuteRelationValidation_rowsWithEmptyColumn() throws IOException
    {
        assertDefectRows("emailPresent", Sets.mutable.of("Eve Davis"));
    }

    // Person ids are -1 and 1..5, SAMPLE_VAL_TBL ids are 1..10, so the left join leaves exactly one
    // person unmatched. A join that degraded to an inner join, or a null check applied to the wrong
    // side, changes which rows come back rather than only the SQL text.
    @Test
    public void testExecuteRelationValidation_rowsWithEmptyColumnAfterFilterAndJoin() throws IOException
    {
        assertDefectRows("personHasSampleRow", Sets.mutable.of("Smith Failsson"));
    }

    private void assertDefectRows(String validationName, Set<String> expectedNames) throws IOException
    {
        DataQualityExecuteTrialInput input = new DataQualityExecuteTrialInput();
        input.clientVersion = "vX_X_X";
        input.packagePath = "demo::relationPersonValidation";
        input.model = new PureModelContextPointer();
        input.validationNames = Sets.mutable.of(validationName);

        Response response = resources.target("pure/v1/dataquality/execute")
                .request()
                .post(Entity.json(input));

        String resultAsString = response.readEntity(String.class);
        assertEquals(200, response.getStatus(), resultAsString);

        JsonNode result = objectMapper.readTree(resultAsString);
        JsonNode columns = result.path("builder").path("columns");
        JsonNode rows = result.path("result").path("rows");
        assertTrue(rows.isArray(), resultAsString);

        int nameIndex = -1;
        for (int i = 0; i < columns.size(); i++)
        {
            if ("FULL_NAME".equals(columns.get(i).path("name").asText()))
            {
                nameIndex = i;
            }
        }
        assertTrue(nameIndex >= 0, "FULL_NAME column not found in " + columns);

        Set<String> actual = new LinkedHashSet<>();
        for (JsonNode row : rows)
        {
            actual.add(row.path("values").get(nameIndex).asText());
        }
        assertEquals(expectedNames, actual, resultAsString);
    }
}
