//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.api.analytics;

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class DataspaceQualityAnalyticsTest
{
    private final DataspaceQualityAnalytics api = new DataspaceQualityAnalytics(new ModelManager(DeploymentMode.TEST));
    private static final String minimumPureClientVersion = "v1_20_0";

    @Test
    public void testValidCheckDataSpaceConstraints()
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(validDataspace);
        Response response = api.checkDataSpaceConstraints(new DataspaceQualityCheckInput(minimumPureClientVersion, "model::NewDataSpace", modelData), null);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDataspaceWithBadClasses()
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(dataSpaceWithBadClasses);
        Response response = api.checkDataSpaceConstraints(new DataspaceQualityCheckInput(minimumPureClientVersion, "model::NewDataSpace", modelData), null);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        ValidationRuleResult targetCompany = new ValidationRuleResult(
                false,
                "targetCompany",
                "Invalid Class Names",
                "Class name (targetCompany) does not match required standards: should start with upper case",
                "Class name should start with Upper case"
        );
        ValidationRuleResult targetPerson = new ValidationRuleResult(
                false,
                "TargetPerson",
                "Documentation not provided for entity and/or its properties",
                "Provide documentation for class TargetPerson and its properties",
                "All entities and properties must have a long description."
        );
        List<ValidationRuleResult> message = new ArrayList<>();
        message.add(targetCompany);
        message.add(targetPerson);
        ValidationResult expectedValidationResult = new ValidationResult(message, 8);
        ValidationResult actualValidationResult = (ValidationResult) response.getEntity();
        Assert.assertEquals(expectedValidationResult.getHealthScore(), actualValidationResult.getHealthScore());
    }

    @Test
    public void testDataspaceWithBadAssociations()
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(dataspaceWithBadAssociation);
        Response response = api.checkDataSpaceConstraints(new DataspaceQualityCheckInput(minimumPureClientVersion, "model::NewDataSpace", modelData), null);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<ValidationRuleResult> message = new ArrayList<>();
        ValidationRuleResult targetCompany = new ValidationRuleResult(
                false,
                "targetCompany_TargetPerson",
                "Invalid Association Name",
                "Association (targetCompany_TargetPerson) does not match required standards: Camel case must be used Association name and should be upper camel case, with an underscore between both sides of the join",
                "Camel case must be used Association name and should be upper camel case, with an underscore between both sides of the join."
        );

        message.add(targetCompany);
        ValidationResult expectedValidationResult = new ValidationResult(message, 9);
        ValidationResult actualValidationResult = (ValidationResult) response.getEntity();
        Assert.assertEquals(expectedValidationResult.getHealthScore(), actualValidationResult.getHealthScore());
    }

    @Test
    public void testDataspaceWithBadEnumerations()
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(dataspaceWithBadEnum);
        Response response = api.checkDataSpaceConstraints(new DataspaceQualityCheckInput(minimumPureClientVersion, "model::NewDataSpace", modelData), null);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<ValidationRuleResult> message = new ArrayList<>();

        ValidationRuleResult e = new ValidationRuleResult(
                false,
                "testEnum_ForMyDataspace",
                "Invalid Enumeration Names",
                "Enumeration name (testEnum_ForMyDataspace) does not match required standards: should start with upper case;should not contain '_';should not contain 'Enum'",
                "Enum name should start with Upper case, not contain underscores or the word Enum"
        );
        message.add(e);
        ValidationResult expectedValidationResult = new ValidationResult(message, 9);
        ValidationResult actualValidationResult = (ValidationResult) response.getEntity();
        Assert.assertEquals(expectedValidationResult.getHealthScore(), actualValidationResult.getHealthScore());
    }

    @Test
    public void testDataspaceWithBadFunctions()
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(dataspaceWithBadFunction);
        Response response = api.checkDataSpaceConstraints(new DataspaceQualityCheckInput(minimumPureClientVersion, "model::NewDataSpace", modelData), null);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<ValidationRuleResult> message = new ArrayList<>();
        ValidationRuleResult fn = new ValidationRuleResult(
                false,
                "",
                "Invalid Equality",
                "Function (equal) does not match required standards: Possible invalid equal check (type mismatch, Integer vs String)",
                "Check for equality checks that will always result in false due to type mismatches (e.g. \\'abc\\' == 123)"
        );
        message.add(fn);

        ValidationRuleResult fn2 = new ValidationRuleResult(
                false,
                "",
                "Invalid Contains",
                "Possible invalid contains check (type mismatch, Integer vs String)",
                "Check for contains / containsAll / containsAny checks that will always result in false due to type mismatches (e.g. [\\'abc\\']->contains(123))"
        );
        message.add(fn2);
        ValidationResult expectedValidationResult = new ValidationResult(message, 8);
        ValidationResult actualValidationResult = (ValidationResult) response.getEntity();
        Assert.assertEquals(expectedValidationResult.getHealthScore(), actualValidationResult.getHealthScore());
    }

    @Test
    public void testDataspaceWithBadClassProperties()
    {
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(dataspaceWithBadClassProperties);
        Response response = api.checkDataSpaceConstraints(new DataspaceQualityCheckInput(minimumPureClientVersion, "model::NewDataSpace", modelData), null);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<ValidationRuleResult> message = new ArrayList<>();
        ValidationRuleResult prop = new ValidationRuleResult(
                false,
                "id_TargetCompany",
                "Invalid Property Names",
                "Property name (id_TargetCompany) does not match required standards: should not contain '_'",
                "Property name should start with lower letter and in camelCase"
        );

        ValidationRuleResult prop2 = new ValidationRuleResult(
                false,
                "Name",
                "Invalid Property Names",
                "Property name (Name) does not match required standards: should start with lower case",
                "Property name should start with lower letter and in camelCase"
        );

        message.add(prop);
        message.add(prop2);
        ValidationResult expectedValidationResult = new ValidationResult(message, 8);
        ValidationResult actualValidationResult = (ValidationResult) response.getEntity();
        Assert.assertEquals(expectedValidationResult.getHealthScore(), actualValidationResult.getHealthScore());
    }

    private final String validDataspace = "###DataSpace\n" +
            "DataSpace model::NewDataSpace\n" +
            "{\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Some Context';\n" +
            "      title: 'New Execution Context';\n" +
            "      description: 'some information about the execution context';\n" +
            "      mapping: model::dummyMapping;\n" +
            "      defaultRuntime: model::dummyRuntime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Some Context';\n" +
            "  elements:\n" +
            "  [\n" +
            "    model::Doc,\n" +
            "    model::TargetCompany,\n" +
            "    model::TargetPerson,\n" +
            "    pure::TargetCompany_TargetPerson,\n" +
            "    pure::TestForMyDataspace\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Enum {meta::pure::profiles::doc.doc = 'test enum for my dataspace'} pure::TestForMyDataspace\n" +
            "{\n" +
            "  Value1,\n" +
            "  Value2\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'documentation for this class'} model::Doc\n" +
            "{\n" +
            "  {meta::pure::profiles::doc.doc = 'target company object'} targetCompanyObject: model::TargetCompany[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'target person object'} targetPersonObject: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'company object'} model::TargetCompany\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'company id'} id: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company name'} name: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company address'} addressId: Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'person object'} model::TargetPerson\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'legal name'} legalName: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s age'} age: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s address'} addressId: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'decimal value'} dec: Decimal[0..1];\n" +
            "}\n" +
            "\n" +
            "Association pure::TargetCompany_TargetPerson\n" +
            "{\n" +
            "  prop1: model::TargetCompany[1];\n" +
            "  prop2: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "function pure::ThisIsMyFunction(): String[1]\n" +
            "{\n" +
            "  'test'\n" +
            "}\n" +
            "\n" +
            "function pure::EqualityFunction(): Boolean[1]\n" +
            "{\n" +
            "  'abc' == 'abc'\n" +
            "}\n" +
            "\n" +
            "function pure::ComparisonFunction(): Boolean[1]\n" +
            "{\n" +
            "  'a'->in(\n" +
            "    [\n" +
            "      'a',\n" +
            "      'c',\n" +
            "      'c'\n" +
            "    ]\n" +
            "  )\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping model::dummyMapping\n" +
            "(\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime model::dummyRuntime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    model::dummyMapping\n" +
            "  ];\n" +
            "}\n";

    private final String dataSpaceWithBadClasses = "###DataSpace\n" +
            "DataSpace model::NewDataSpace\n" +
            "{\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Some Context';\n" +
            "      title: 'New Execution Context';\n" +
            "      description: 'some information about the execution context';\n" +
            "      mapping: model::dummyMapping;\n" +
            "      defaultRuntime: model::dummyRuntime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Some Context';\n" +
            "  elements:\n" +
            "  [\n" +
            "    model::Doc,\n" +
            "    model::targetCompany,\n" +
            "    model::TargetPerson,\n" +
            "    model::TargetCompany_TargetPerson,\n" +
            "    model::TestForMyDataspace\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Enum {meta::pure::profiles::doc.doc = 'test enum for my dataspace'} model::TestForMyDataspace\n" +
            "{\n" +
            "  Value1,\n" +
            "  Value2\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'documentation for this class'} model::Doc\n" +
            "{\n" +
            "  {meta::pure::profiles::doc.doc = 'target company object'} targetCompanyObject: model::targetCompany[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'target person object'} targetPersonObject: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'company object'} model::targetCompany\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'company id'} id: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company name'} name: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company address'} addressId: Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'person object'} model::TargetPerson\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'legal name'} legalName: String[1];\n" +
            "  age: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s address'} addressId: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'decimal value'} dec: Decimal[0..1];\n" +
            "}\n" +
            "\n" +
            "Association model::TargetCompany_TargetPerson\n" +
            "{\n" +
            "  prop1: model::targetCompany[1];\n" +
            "  prop2: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "function pure::ThisIsMyFunction(): String[1]\n" +
            "{\n" +
            "  'test'\n" +
            "}\n" +
            "\n" +
            "function pure::EqualityFunction(): Boolean[1]\n" +
            "{\n" +
            "  'abc' == 'abc'\n" +
            "}\n" +
            "\n" +
            "function pure::ComparisonFunction(): Boolean[1]\n" +
            "{\n" +
            "  'a'->in(\n" +
            "    [\n" +
            "      'a',\n" +
            "      'c',\n" +
            "      'c'\n" +
            "    ]\n" +
            "  )\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping model::dummyMapping\n" +
            "(\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime model::dummyRuntime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    model::dummyMapping\n" +
            "  ];\n" +
            "}\n";

    private final String dataspaceWithBadAssociation = "###DataSpace\n" +
            "DataSpace model::NewDataSpace\n" +
            "{\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Some Context';\n" +
            "      title: 'New Execution Context';\n" +
            "      description: 'some information about the execution context';\n" +
            "      mapping: model::dummyMapping;\n" +
            "      defaultRuntime: model::dummyRuntime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Some Context';\n" +
            "  elements:\n" +
            "  [\n" +
            "    model::Doc,\n" +
            "    model::TargetCompany,\n" +
            "    model::TargetPerson,\n" +
            "    pure::targetCompany_TargetPerson,\n" +
            "    pure::TestForMyDataspace\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Enum {meta::pure::profiles::doc.doc = 'test enum for my dataspace'} pure::TestForMyDataspace\n" +
            "{\n" +
            "  Value1,\n" +
            "  Value2\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'documentation for this class'} model::Doc\n" +
            "{\n" +
            "  {meta::pure::profiles::doc.doc = 'target company object'} targetCompanyObject: model::TargetCompany[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'target person object'} targetPersonObject: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'company object'} model::TargetCompany\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'company id'} id: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company name'} name: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company address'} addressId: Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'person object'} model::TargetPerson\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'legal name'} legalName: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s age'} age: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s address'} addressId: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'decimal value'} dec: Decimal[0..1];\n" +
            "}\n" +
            "\n" +
            "Association pure::targetCompany_TargetPerson\n" +
            "{\n" +
            "  prop1: model::TargetCompany[1];\n" +
            "  prop2: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "function pure::ThisIsMyFunction(): String[1]\n" +
            "{\n" +
            "  'test'\n" +
            "}\n" +
            "\n" +
            "function pure::EqualityFunction(): Boolean[1]\n" +
            "{\n" +
            "  'abc' == 'abc'\n" +
            "}\n" +
            "\n" +
            "function pure::ComparisonFunction(): Boolean[1]\n" +
            "{\n" +
            "  'a'->in(\n" +
            "    [\n" +
            "      'a',\n" +
            "      'c',\n" +
            "      'c'\n" +
            "    ]\n" +
            "  )\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping model::dummyMapping\n" +
            "(\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime model::dummyRuntime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    model::dummyMapping\n" +
            "  ];\n" +
            "}\n";

    private final String dataspaceWithBadEnum = "###DataSpace\n" +
            "DataSpace model::NewDataSpace\n" +
            "{\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Some Context';\n" +
            "      title: 'New Execution Context';\n" +
            "      description: 'some information about the execution context';\n" +
            "      mapping: model::dummyMapping;\n" +
            "      defaultRuntime: model::dummyRuntime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Some Context';\n" +
            "  elements:\n" +
            "  [\n" +
            "    model::Doc,\n" +
            "    model::TargetCompany,\n" +
            "    model::TargetPerson,\n" +
            "    pure::TargetCompany_TargetPerson,\n" +
            "    pure::testEnum_ForMyDataspace\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Enum {meta::pure::profiles::doc.doc = 'test enum for my dataspace'} pure::testEnum_ForMyDataspace\n" +
            "{\n" +
            "  Value1,\n" +
            "  Value2\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'documentation for this class'} model::Doc\n" +
            "{\n" +
            "  {meta::pure::profiles::doc.doc = 'target company object'} targetCompanyObject: model::TargetCompany[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'target person object'} targetPersonObject: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'company object'} model::TargetCompany\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'company id'} id: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company name'} name: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company address'} addressId: Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'person object'} model::TargetPerson\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'legal name'} legalName: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s age'} age: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s address'} addressId: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'decimal value'} dec: Decimal[0..1];\n" +
            "}\n" +
            "\n" +
            "Association pure::TargetCompany_TargetPerson\n" +
            "{\n" +
            "  prop1: model::TargetCompany[1];\n" +
            "  prop2: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "function pure::ThisIsMyFunction(): String[1]\n" +
            "{\n" +
            "  'test'\n" +
            "}\n" +
            "\n" +
            "function pure::EqualityFunction(): Boolean[1]\n" +
            "{\n" +
            "  'abc' == 'abc'\n" +
            "}\n" +
            "\n" +
            "function pure::ComparisonFunction(): Boolean[1]\n" +
            "{\n" +
            "  'a'->in(\n" +
            "    [\n" +
            "      'a',\n" +
            "      'c',\n" +
            "      'c'\n" +
            "    ]\n" +
            "  )\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping model::dummyMapping\n" +
            "(\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime model::dummyRuntime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    model::dummyMapping\n" +
            "  ];\n" +
            "}\n";

    private final String dataspaceWithBadFunction = "###DataSpace\n" +
            "DataSpace model::NewDataSpace\n" +
            "{\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Some Context';\n" +
            "      title: 'New Execution Context';\n" +
            "      description: 'some information about the execution context';\n" +
            "      mapping: model::dummyMapping;\n" +
            "      defaultRuntime: model::dummyRuntime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Some Context';\n" +
            "  elements:\n" +
            "  [\n" +
            "    model::Doc,\n" +
            "    model::TargetCompany,\n" +
            "    model::TargetPerson,\n" +
            "    pure::TargetCompany_TargetPerson,\n" +
            "    pure::TestForMyDataspace\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Enum {meta::pure::profiles::doc.doc = 'test enum for my dataspace'} pure::TestForMyDataspace\n" +
            "{\n" +
            "  Value1,\n" +
            "  Value2\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'documentation for this class'} model::Doc\n" +
            "{\n" +
            "  {meta::pure::profiles::doc.doc = 'target company object'} targetCompanyObject: model::TargetCompany[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'target person object'} targetPersonObject: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'company object'} model::TargetCompany\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'company id'} id: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company name'} name: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company address'} addressId: Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'person object'} model::TargetPerson\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'legal name'} legalName: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s age'} age: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s address'} addressId: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'decimal value'} dec: Decimal[0..1];\n" +
            "}\n" +
            "\n" +
            "Association pure::TargetCompany_TargetPerson\n" +
            "{\n" +
            "  prop1: model::TargetCompany[1];\n" +
            "  prop2: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "function pure::ThisIsMyFunction(): String[1]\n" +
            "{\n" +
            "  'test'\n" +
            "}\n" +
            "\n" +
            "function pure::EqualityFunction(): Boolean[1]\n" +
            "{\n" +
            "  123 == 'abc'\n" +
            "}\n" +
            "\n" +
            "function pure::InvalidContainsFunction(): Boolean[1]\n" +
            "{\n" +
            "  [1,2,3,4]->contains('hello');\n" +
            "}\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping model::dummyMapping\n" +
            "(\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime model::dummyRuntime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    model::dummyMapping\n" +
            "  ];\n" +
            "}\n";

    private final String dataspaceWithBadClassProperties = "###DataSpace\n" +
            "DataSpace model::NewDataSpace\n" +
            "{\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'Some Context';\n" +
            "      title: 'New Execution Context';\n" +
            "      description: 'some information about the execution context';\n" +
            "      mapping: model::dummyMapping;\n" +
            "      defaultRuntime: model::dummyRuntime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'Some Context';\n" +
            "  elements:\n" +
            "  [\n" +
            "    model::Doc,\n" +
            "    model::TargetCompany,\n" +
            "    model::TargetPerson,\n" +
            "    pure::TargetCompany_TargetPerson,\n" +
            "    pure::TestForMyDataspace\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Enum {meta::pure::profiles::doc.doc = 'test enum for my dataspace'} pure::TestForMyDataspace\n" +
            "{\n" +
            "  Value1,\n" +
            "  Value2\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'documentation for this class'} model::Doc\n" +
            "{\n" +
            "  {meta::pure::profiles::doc.doc = 'target company object'} targetCompanyObject: model::TargetCompany[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'target person object'} targetPersonObject: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'company object'} model::TargetCompany\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'company id'} id_TargetCompany: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company name'} Name: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'company address'} addressId: Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class {meta::pure::profiles::doc.doc = 'person object'} model::TargetPerson\n" +
            "{\n" +
            "  <<equality.Key>> {meta::pure::profiles::doc.doc = 'legal name'} legalName: String[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s age'} age: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'person\\'s address'} addressId: Integer[1];\n" +
            "  {meta::pure::profiles::doc.doc = 'decimal value'} dec: Decimal[0..1];\n" +
            "}\n" +
            "\n" +
            "Association pure::TargetCompany_TargetPerson\n" +
            "{\n" +
            "  prop1: model::TargetCompany[1];\n" +
            "  prop2: model::TargetPerson[1];\n" +
            "}\n" +
            "\n" +
            "function pure::ThisIsMyFunction(): String[1]\n" +
            "{\n" +
            "  'test'\n" +
            "}\n" +
            "\n" +
            "function pure::EqualityFunction(): Boolean[1]\n" +
            "{\n" +
            "  'abc' == 'abc'\n" +
            "}\n" +
            "\n" +
            "function pure::ComparisonFunction(): Boolean[1]\n" +
            "{\n" +
            "  'a'->in(\n" +
            "    [\n" +
            "      'a',\n" +
            "      'c',\n" +
            "      'c'\n" +
            "    ]\n" +
            "  )\n" +
            "}\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping model::dummyMapping\n" +
            "(\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime model::dummyRuntime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    model::dummyMapping\n" +
            "  ];\n" +
            "}\n";
}
