/*
 *  Copyright 2022 Goldman Sachs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.finos.legend.engine.service.post.validation.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_;

public class TestLegendServicePostValidationRunner
{
    private Response test(String serviceModelPath, String servicePath, String assertionId, SerializationFormat serializationFormat) throws Exception
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(serviceModelPath));
        PureModelContextData pureModelContextData = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(url, PureModelContextData.class);
        Service service = pureModelContextData.getElementsOfType(Service.class).stream().filter(s -> s.getPath().equals(servicePath)).findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find service with path '" + servicePath + "'"));
        PureModelContextData dataWithoutService = PureModelContextData.newBuilder().withOrigin(pureModelContextData.getOrigin()).withSerializer(pureModelContextData.getSerializer()).withElements(LazyIterate.select(pureModelContextData.getElements(), e -> e != service)).build();
        PureModel pureModel = new PureModel(dataWithoutService, Identity.getAnonymousIdentity().getName(), DeploymentMode.PROD);

        Response validationResult = this.runValidation(service, pureModel, assertionId, serializationFormat);
        Assert.assertNotNull(validationResult);
        Assert.assertEquals(200, validationResult.getStatus());

        Object result = validationResult.getEntity();
        Assert.assertTrue(result instanceof PostValidationAssertionStreamingOutput);

        return validationResult;
    }

    private Response runValidation(Service service, PureModel pureModel, String assertionId, SerializationFormat serializationFormat)
    {
        Root_meta_legend_service_metamodel_Service pureService = compileService(service, pureModel.getContext(service));

        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(Relational.build(), InMemory.build());
        LegendServicePostValidationRunner servicePostValidationRunner = new LegendServicePostValidationRunner(pureModel, pureService, ((PureExecution) service.execution).func.parameters, Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()), LegendPlanTransformers.transformers, "vX_X_X", Identity.getAnonymousIdentity(), serializationFormat,planExecutor);
        try
        {
            return servicePostValidationRunner.runValidationAssertion(assertionId);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private String responseAsString(Response response) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamingOutput output = (StreamingOutput) response.getEntity();
        output.write(baos);
        return baos.toString("UTF-8");
    }

    public Root_meta_legend_service_metamodel_Service compileService(Service service, CompileContext compileContext)
    {
        // If we're recompiling an existing service remove the original first
        Package pkg = compileContext.pureModel.getOrCreatePackage(service._package);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement existing = pkg._children().detect(c -> c._name().equals(service.name));
        if (existing != null)
        {
            pkg._childrenRemove(existing);
        }

        Root_meta_legend_service_metamodel_Service compiledService = (Root_meta_legend_service_metamodel_Service) compileContext.processFirstPass(service);
        compileContext.processSecondPass(service);
        compileContext.processThirdPass(service);
        return compiledService;
    }

    @Test
    public void testSucceedingTDSService() throws Exception
    {
        String result = responseAsString(test("legend-test-tds-services-with-validation.json", "meta::validation::test::DemoPassingService", "noFirstNamesWithLetterX", SerializationFormat.PURE_TDSOBJECT));

        Assert.assertEquals("{\"id\": \"noFirstNamesWithLetterX\", \"message\": \"Expected no first names to begin with the letter X\", \"violations\": []}", result);
    }

    @Test
    public void testFailingTDSService() throws Exception
    {
        String result = responseAsString(test("legend-test-tds-services-with-validation.json", "meta::validation::test::DemoFailingService", "noFirstNamesWithLetterT", SerializationFormat.PURE_TDSOBJECT));

        Assert.assertEquals("{\"id\": \"noFirstNamesWithLetterT\", \"message\": \"Expected no first names to begin with the letter T\", \"violations\": [{\"firstName\":\"Tom\",\"lastName\":\"Wilson\",\"age\":24}]}", result);
    }

    @Test
    public void testSucceedingObjectService() throws Exception
    {
        String result = responseAsString(test("legend-test-object-services-with-validation.json", "meta::validation::test::DemoPassingService", "noFirstNamesWithLetterX", SerializationFormat.DEFAULT));
        ObjectNode node = new ObjectMapper().readValue(result, ObjectNode.class);
        Assert.assertEquals("noFirstNamesWithLetterX", node.get("id").asText());
        Assert.assertEquals("Expected no first names to begin with the letter X", node.get("message").asText());
        Assert.assertEquals("{\"builder\":{\"_type\":\"classBuilder\",\"mapping\":\"meta::validation::test::PersonMapping\",\"classMappings\":[{\"setImplementationId\":\"meta_validation_test_Person\",\"properties\":[{\"property\":\"firstName\",\"type\":\"String\"},{\"property\":\"lastName\",\"type\":\"String\"},{\"property\":\"age\",\"type\":\"Integer\"}],\"class\":\"meta::validation::test::Person\"}],\"class\":\"meta::validation::test::Person\"},\"activities\":[{\"_type\":\"RelationalExecutionActivity\",\"sql\":\"select \\\"root\\\".ID as \\\"pk_0\\\", \\\"root\\\".FIRSTNAME as \\\"firstName\\\", \\\"root\\\".LASTNAME as \\\"lastName\\\", \\\"root\\\".AGE as \\\"age\\\" from PersonTable as \\\"root\\\" where \\\"root\\\".FIRSTNAME like 'X%'\"}],\"objects\":[]}", RelationalResultToJsonDefaultSerializer.removeComment(node.get("violations").toString()));
    }

    @Test
    public void testFailingObjectService() throws Exception
    {
        String result = responseAsString(test("legend-test-object-services-with-validation.json", "meta::validation::test::DemoFailingService", "noFirstNamesWithLetterT", SerializationFormat.DEFAULT));
        ObjectNode node = new ObjectMapper().readValue(result, ObjectNode.class);
        Assert.assertEquals("noFirstNamesWithLetterT", node.get("id").asText());
        Assert.assertEquals("Expected no first names to begin with the letter T", node.get("message").asText());
        Assert.assertEquals("{\"builder\":{\"_type\":\"classBuilder\",\"mapping\":\"meta::validation::test::PersonMapping\",\"classMappings\":[{\"setImplementationId\":\"meta_validation_test_Person\",\"properties\":[{\"property\":\"firstName\",\"type\":\"String\"},{\"property\":\"lastName\",\"type\":\"String\"},{\"property\":\"age\",\"type\":\"Integer\"}],\"class\":\"meta::validation::test::Person\"}],\"class\":\"meta::validation::test::Person\"},\"activities\":[{\"_type\":\"RelationalExecutionActivity\",\"sql\":\"select \\\"root\\\".ID as \\\"pk_0\\\", \\\"root\\\".FIRSTNAME as \\\"firstName\\\", \\\"root\\\".LASTNAME as \\\"lastName\\\", \\\"root\\\".AGE as \\\"age\\\" from PersonTable as \\\"root\\\" where \\\"root\\\".FIRSTNAME like 'T%'\"}],\"objects\":[{\"firstName\":\"Tom\",\"lastName\":\"Wilson\",\"age\":24,\"alloyStoreObjectReference$\":\"ASOR:MDAxOjAxMDowMDAwMDAwMDEwOlJlbGF0aW9uYWw6MDAwMDAwMDAzNzptZXRhOjp2YWxpZGF0aW9uOjp0ZXN0OjpQZXJzb25NYXBwaW5nOjAwMDAwMDAwMjc6bWV0YV92YWxpZGF0aW9uX3Rlc3RfUGVyc29uOjAwMDAwMDAwMjc6bWV0YV92YWxpZGF0aW9uX3Rlc3RfUGVyc29uOjAwMDAwMDA0Nzg6eyJfdHlwZSI6IlJlbGF0aW9uYWxEYXRhYmFzZUNvbm5lY3Rpb24iLCJhdXRoZW50aWNhdGlvblN0cmF0ZWd5Ijp7Il90eXBlIjoiaDJEZWZhdWx0In0sImRhdGFzb3VyY2VTcGVjaWZpY2F0aW9uIjp7Il90eXBlIjoiaDJMb2NhbCIsInRlc3REYXRhU2V0dXBTcWxzIjpbIkRST1AgVEFCTEUgSUYgRVhJU1RTIFBlcnNvblRhYmxlOyIsIkNSRUFURSBUQUJMRSBQZXJzb25UYWJsZSAoSUQgaW50LCBGSVJTVE5BTUUgdmFyY2hhcigyMDApLCBMQVNUTkFNRSB2YXJjaGFyKDIwMCksIEFHRSBpbnQpOyIsIklOU0VSVCBJTlRPIFBlcnNvblRhYmxlIFZBTFVFUyAoMSwgJ1RvbScsICdXaWxzb24nLCAyNCk7IiwiSU5TRVJUIElOVE8gUGVyc29uVGFibGUgVkFMVUVTICgyLCAnRGlodWknLCAnQmFvJywgMzIpOyJdfSwiZWxlbWVudCI6IiIsInBvc3RQcm9jZXNzb3JXaXRoUGFyYW1ldGVyIjpbXSwicG9zdFByb2Nlc3NvcnMiOltdLCJ0eXBlIjoiSDIifTowMDAwMDAwMDExOnsicGskXzAiOjF9\"}]}", RelationalResultToJsonDefaultSerializer.removeComment(node.get("violations").toString()));
    }

    @Test
    public void testServiceWithStaticParam() throws Exception
    {
        String result = responseAsString(test("legend-test-services-with-validation-and-parameters.json", "meta::validation::test::DemoServiceWithStaticParam", "noFirstNamesWithLetterT", SerializationFormat.PURE_TDSOBJECT));

        Assert.assertEquals("{\"id\": \"noFirstNamesWithLetterT\", \"message\": \"Expected no first names to begin with the letter T\", \"violations\": [{\"firstName\":\"Tom\",\"lastName\":\"Wilson\",\"age\":24}]}", result);
    }

    @Test
    public void testServiceWithQueryParam() throws Exception
    {
        String result = responseAsString(test("legend-test-services-with-validation-and-parameters.json", "meta::validation::test::DemoServiceWithQueryParam", "noFirstNamesWithLetterT", SerializationFormat.PURE_TDSOBJECT));

        Assert.assertEquals("{\"id\": \"noFirstNamesWithLetterT\", \"message\": \"Expected no first names to begin with the letter T\", \"violations\": [{\"firstName\":\"Tom\",\"lastName\":\"Wilson\",\"age\":24}]}", result);
    }

    @Test
    public void testMultiExecutionService() throws Exception
    {
        String result = responseAsString(test("legend-test-services-with-validation-multi-execution.json", "meta::validation::test::DemoServiceWithParamsMultiExecution", "noFirstNamesWithLetterT", SerializationFormat.PURE_TDSOBJECT));

        Assert.assertEquals("{\"id\": \"noFirstNamesWithLetterT\", \"message\": \"Expected no first names to begin with the letter T\", \"violations\": [{\"firstName\":\"Tom\",\"lastName\":\"Wilson\",\"age\":24}]}", result);
    }
}
