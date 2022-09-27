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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestMilestonedPropertyUsageInFunctionExpresions extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{

    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Pure\n" +
                "Class anything::somethingelse\n" +
                "{\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testCompilationOfConstraintsWithMilestonedPropertyAndNoDateSupplied()
    {
        test("###Pure\n" +
                "Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product" +
                "[\n" +
                "   CST\n" +
                "   (\n" +
                "      ~function: $this.classification.exchangeName->startsWith('a') \n" +
                "      ~message: 'Ensure parent (this) milestoning context propagated through project'\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   classification : meta::test::milestoning::domain::Classification[1];\n" +
                "}\n" +
                "Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Classification{\n" +
                "   exchangeName : String[1];\n" +
                "}\n");
    }

    @Test
    public void testDatePropagationFromBusinessTemporalSourceToBusinessTemporalTarget()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class <<temporal.businesstemporal>> main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::Firm\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): String[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2022-12-12).firm.name\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___String_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (((SimpleFunctionExpression) functionExpression._parametersValues().toList().get(0))._parametersValues().toList().get(1)))._values().toList().get(0);
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0);
        InstanceValue parameterValue = (InstanceValue) firm_functionExpression._parametersValues().toList().get(1);
        Assert.assertEquals(2, firm_functionExpression._parametersValues().size());
        Assert.assertEquals(DateFunctions.newPureDate(2022, 12, 12), parameterValue._values().toList().get(0));
    }

    @Test
    public void testDatePropagationFromBiTemporalSourceToBusinessTemporalTarget()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class <<temporal.bitemporal>> main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::Firm\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): String[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2022-12-12, %2022-12-13).firm.name\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___String_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (((SimpleFunctionExpression) functionExpression._parametersValues().toList().get(0))._parametersValues().toList().get(1)))._values().toList().get(0);
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0);
        InstanceValue parameterValue = (InstanceValue) firm_functionExpression._parametersValues().toList().get(1);
        Assert.assertEquals(2, firm_functionExpression._parametersValues().size());
        Assert.assertEquals(DateFunctions.newPureDate(2022, 12, 13), parameterValue._values().toList().get(0));
    }

    @Test
    @Ignore
    public void testDatePropagationNotSupportedFromProcessingTemporalSourceToBusinessTemporalTarget()
    {
        test("###Pure\n" +
                "Class <<temporal.processingtemporal>> main::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  firm: main::Firm[1];\n" +
                "}\n" +
                "\n" +
                "Class <<temporal.businesstemporal>> main::Firm\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "function main::walkTree(): String[*]\n" +
                "{\n" +
                "   main::Person.all(%2022-12-12, %2022-12-12).firm.name\n" +
                "}\n", "COMPILATION error at [13:1-16:1]: Error in 'main::walkTree': No-Arg milestoned property: 'firm' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    public void testDatePropagationSupportedInProjectFromBitemporalToBusinessTemporalWithMultipleProjectColumns()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class <<temporal.bitemporal>> main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "  firm1: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.bitemporal>> main::Firm1\n" +
                        "{\n" +

                        "}\n" +
                        "Class <<temporal.bitemporal>> main::Firm extends main::Firm1\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  test: main::City[1];\n" +
                        "  city(d: StrictDate[1]) {$this.testAllVersions-> toOne()}: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): Any[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2021-11-12, %2021-11-13)->project([x| $x.name, x| $x.firm.name], ['City name', 'name'])\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___Any_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (functionExpression._parametersValues().toList().get(1)))._values().toList().get(1);
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) ((SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0))._parametersValues().toList().get(0);
        InstanceValue parameterValue1 = (InstanceValue)  firm_functionExpression._parametersValues().toList().get(1);
        InstanceValue parameterValue2 = (InstanceValue) firm_functionExpression._parametersValues().toList().get(2);
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), parameterValue1._values().toList().get(0));
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 13), parameterValue2._values().toList().get(0));
    }

    @Test
    public void testDatePropagationSupportedInProjectFromBitemporalToBusinessTemporalWithProject()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class <<temporal.bitemporal>> main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "  firm1: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.bitemporal>> main::Firm1\n" +
                        "{\n" +

                        "}\n" +
                        "Class <<temporal.bitemporal>> main::Firm extends main::Firm1\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  test: main::City[1];\n" +
                        "  city(d: StrictDate[1]) {$this.testAllVersions-> toOne()}: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): Any[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2021-11-12, %2021-11-13)->project([x| $x.firm.name + $x.firm.name], ['name'])\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___Any_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (functionExpression._parametersValues().toList().get(1)))._values().toList().get(0);
        InstanceValue plus_functionExpression = ((InstanceValue) ((SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0))._parametersValues().toList().get(0));
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) ((SimpleFunctionExpression)  plus_functionExpression._values().toList().get(0))._parametersValues().toList().get(0);
        InstanceValue parameterValue1 = (InstanceValue)  firm_functionExpression._parametersValues().toList().get(1);
        InstanceValue parameterValue2 = (InstanceValue) firm_functionExpression._parametersValues().toList().get(2);
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), parameterValue1._values().toList().get(0));
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 13), parameterValue2._values().toList().get(0));
        SimpleFunctionExpression firm1_functionExpression = (SimpleFunctionExpression) ((SimpleFunctionExpression)  plus_functionExpression._values().toList().get(1))._parametersValues().toList().get(0);
        InstanceValue firm1_parameterValue1 = (InstanceValue)  firm1_functionExpression._parametersValues().toList().get(1);
        InstanceValue firm1_parameterValue2 = (InstanceValue) firm1_functionExpression._parametersValues().toList().get(2);
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), firm1_parameterValue1._values().toList().get(0));
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 13), firm1_parameterValue2._values().toList().get(0));
    }

    @Test
    public void testDatePropagationSupportedInProjectFromBitemporalToBusinessTemporalWithIfStatement()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class <<temporal.bitemporal>> main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "  firm1: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.bitemporal>> main::Firm1\n" +
                        "{\n" +

                        "}\n" +
                        "Class <<temporal.bitemporal>> main::Firm extends main::Firm1\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  test: main::City[1];\n" +
                        "  city(d: StrictDate[1]) {$this.testAllVersions-> toOne()}: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): Any[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2021-11-12, %2021-11-13)->project([x| if($x.firm.name->startsWith('a'), | 'Firm' , |'Name')], ['City name', 'name'])\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___Any_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (functionExpression._parametersValues().toList().get(1)))._values().toList().get(0);
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) ((SimpleFunctionExpression) ((SimpleFunctionExpression) ((SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0))._parametersValues().toList().get(0))._parametersValues().toList().get(0))._parametersValues().toList().get(0);
        InstanceValue parameterValue1 = (InstanceValue)  firm_functionExpression._parametersValues().toList().get(1);
        InstanceValue parameterValue2 = (InstanceValue) firm_functionExpression._parametersValues().toList().get(2);
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), parameterValue1._values().toList().get(0));
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 13), parameterValue2._values().toList().get(0));
    }

    @Test
    public void testDatePropagationSupportedWhenProjectionColumnHasDerivedproperty()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class <<temporal.businesstemporal>> main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "  firm1: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::Firm1\n" +
                        "{\n" +

                        "}\n" +
                        "Class <<temporal.businesstemporal>> main::Firm extends main::Firm1\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  city: main::City[1];\n" +
                        "  test: main::City[1];\n" +
                        "  city(d: StrictDate[1]) {$this.testAllVersions-> toOne()}: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): Any[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2021-11-12)->project([x| $x.firm.city(%2021-11-12)->filter(y | $y.name == '').name, x| $x.firm1.city(%2021-11-12)->filter(y | $y.name == '').name], ['City name1', 'City Name'])\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___Any_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (functionExpression._parametersValues().toList().get(1)))._values().toList().get(0);
        LambdaFunction lambdaFunction_1 = (LambdaFunction) ((InstanceValue) (functionExpression._parametersValues().toList().get(1)))._values().toList().get(1);
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) ((SimpleFunctionExpression) (((SimpleFunctionExpression) ((SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0))._parametersValues().toList().get(0))._parametersValues().toList().get(0)))._parametersValues().toList().get(0);
        SimpleFunctionExpression firm1_functionExpression = (SimpleFunctionExpression) ((SimpleFunctionExpression) (((SimpleFunctionExpression) ((SimpleFunctionExpression) lambdaFunction_1._expressionSequence().toList().get(0))._parametersValues().toList().get(0))._parametersValues().toList().get(0)))._parametersValues().toList().get(0);
        InstanceValue firm_parameterValue = (InstanceValue) firm_functionExpression._parametersValues().toList().get(1);
        InstanceValue firm1_parameterValue = (InstanceValue) firm1_functionExpression._parametersValues().toList().get(1);
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), firm_parameterValue._values().toList().get(0));
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), firm1_parameterValue._values().toList().get(0));
    }

    @Test
    public void testDatePropagationSupportedInProjectFromBitemporalToBusinessTemporal()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.bitemporal>> main::Firm\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  city: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): Any[*]\n" +
                        "{\n" +
                        "   main::Person.all()->project([x| $x.firm(%2021-11-12, %2021-11-12).city.name], ['City name'])\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___Any_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (functionExpression._parametersValues().toList().get(1)))._values().toList().get(0);
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0);
        InstanceValue parameterValue = (InstanceValue) ((SimpleFunctionExpression) firm_functionExpression._parametersValues().toList().get(0))._parametersValues().toList().get(1);
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), parameterValue._values().toList().get(0));
    }

    @Test
    public void testDatePropagationSupportedInProjectFromBitemporalToProcessingTemporal()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.bitemporal>> main::Firm\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  city: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.processingtemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): Any[*]\n" +
                        "{\n" +
                        "   main::Person.all()->project([x| $x.firm(%2021-11-12, %2021-11-12).city.name], ['City name'])\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___Any_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (functionExpression._parametersValues().toList().get(1)))._values().toList().get(0);
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0);
        InstanceValue parameterValue = (InstanceValue) ((SimpleFunctionExpression) firm_functionExpression._parametersValues().toList().get(0))._parametersValues().toList().get(1);
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), parameterValue._values().toList().get(0));
    }

    @Test
    public void testBusinessDatePropagatedToBitemporalWhenProcessingDateSupplied()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::Firm\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  city: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.bitemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): Any[*]\n" +
                        "{\n" +
                        "   main::Person.all()->project([x| $x.firm(%2021-11-12).city(%2021-11-13).name], ['City name'])\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___Any_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (functionExpression._parametersValues().toList().get(1)))._values().toList().get(0);
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0);
        InstanceValue parameterValue = (InstanceValue) ((SimpleFunctionExpression) firm_functionExpression._parametersValues().toList().get(0))._parametersValues().toList().get(2);
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), parameterValue._values().toList().get(0));
    }

    @Test
    public void testProcessingDatePropagatedToBitemporalWhenBusinessDateSupplied()
    {
        Pair<PureModelContextData, PureModel> modelWithInput =
                test("###Pure\n" +
                        "Class main::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  firm: main::Firm[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.processingtemporal>> main::Firm\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  city: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.bitemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): Any[*]\n" +
                        "{\n" +
                        "   main::Person.all()->project([x| $x.firm(%2021-11-12).city(%2021-11-13).name], ['City name'])\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree___Any_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression functionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        LambdaFunction lambdaFunction = (LambdaFunction) ((InstanceValue) (functionExpression._parametersValues().toList().get(1)))._values().toList().get(0);
        SimpleFunctionExpression firm_functionExpression = (SimpleFunctionExpression) lambdaFunction._expressionSequence().toList().get(0);
        InstanceValue parameterValue = (InstanceValue) ((SimpleFunctionExpression) firm_functionExpression._parametersValues().toList().get(0))._parametersValues().toList().get(1);
        Assert.assertEquals(DateFunctions.newPureDate(2021, 11, 12), parameterValue._values().toList().get(0));
    }

    @Test
    @Ignore
    public void testProcessingDateDoesntPropagateToBusinessContext()
    {
        test("###Pure\n" +
                "Class <<temporal.processingtemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.processingtemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Exchange{\n" +
                "   name : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-05-02).classification.exchange.name}\n" +
                "}\n", "COMPILATION error at [11:1-14:1]: Error in 'main::walkTree': No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    @Ignore
    public void testBusinessDateDoesntPropagateToBusinessContext()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class  <<temporal.processingtemporal>> test::Exchange{\n" +
                "   name : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classification.exchange.name}\n" +
                "}\n", "COMPILATION error at [11:1-14:1]: Error in 'main::walkTree': No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [processingDate] parameters");
    }

    @Test
    public void testProcessingMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughMapToNoArgMilestonedPropertyInLambda()
    {
        test("###Pure\n" +
                "Class <<temporal.processingtemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.processingtemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classification->map(c|$c.exchangeName)}\n" +
                "}\n");
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughMapToNoArgMilestonedPropertyInLambda()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(businessDate=$bd)} : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classification->map(t|$t.exchange.exchangeName)}\n" +
                "}\n");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateFromEdgePointPropertyThroughMapToNoArgMilestonedPropertyInLambda()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classificationAllVersions->map(t|$t.exchange.exchangeName)}\n" +
                "}\n", "COMPILATION error at [15:73-80]: No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateFromDerivedPropertyThroughMapToNoArgMilestonedPropertyInLambda()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(businessDate=$bd)} : test::Classification[*];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).myClassification(%2022-11-12)->map(t|$t.exchange.exchangeName)}\n" +
                "}\n", "COMPILATION error at [16:77-84]: No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateFromNoArgQualifiedPropertyThroughMapToFilter()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(businessDate=$bd)} : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classification->map(c|^test::Classification(businessDate=%2022-11-12))->filter(t|$t.exchange.exchangeName == '')}\n" +
                "}\n", "COMPILATION error at [16:121-128]: No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    public void testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughFilterToNoArgMilestonedPropertyInLambda(boolean extraFilter)
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(name='',businessDate=$bd)} : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   name : String[1];\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classification" + (extraFilter ? "->filter(t|$t.name == '')" : "") + "->filter(t2|$t2.exchange.exchangeName == '')}\n" +
                "}\n");
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughFilterToNoArgMilestonedPropertyInLambda()
    {
        testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughFilterToNoArgMilestonedPropertyInLambda(false);
        testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughFilterToNoArgMilestonedPropertyInLambda(true);
    }

    public void testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedPropertyInFilter(boolean extraFilter, String errorNo)
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Classification{\n" +
                "   name : String[1];\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classificationAllVersions" + (extraFilter ? "->filter(t|$t.name == '')" : "") + "->filter(t2|$t2.exchange.exchangeName == '')}\n" +
                "}\n", "COMPILATION error at " + errorNo + ": No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedPropertyInFilter()
    {
        testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedPropertyInFilter(false, "[16:78-85]");
        testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedPropertyInFilter(true, "[16:103-110]");
    }

    public void testMilestoningContextNotAllowedToPropagateFromAllThroughDerivedPropertyToNoArgMilestonedPropertyInFilter(boolean extraFilter, String errorNo)
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(name='',businessDate=$bd)} : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   name : String[1];\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).myClassification(%2022-11-12)" + (extraFilter ? "->filter(t|$t.name == '')" : "") + "->filter(t|$t.exchange.exchangeName == '')}\n" +
                "}\n", "COMPILATION error at " + errorNo + ": No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateFromAllThroughDerivedPropertyToNoArgMilestonedPropertyInFilter()
    {
        testMilestoningContextNotAllowedToPropagateFromAllThroughDerivedPropertyToNoArgMilestonedPropertyInFilter(false, "[17:80-87]");
        testMilestoningContextNotAllowedToPropagateFromAllThroughDerivedPropertyToNoArgMilestonedPropertyInFilter(true, "[17:105-112]");
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughFilterThroughExistsToNoArgMilestonedPropertyInLambda()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(businessDate=$bd)} : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12)->filter(p|$p.classification->exists(c|$c.exchange.exchangeName == ''))}\n" +
                "}\n");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateFromEdgePointPropertyThroughExistsToNoArgMilestonedPropertyInLambda()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12)->filter(p|$p.classificationAllVersions->exists(c|$c.exchange.exchangeName == ''))}\n" +
                "}\n", "COMPILATION error at [15:89-96]: No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateFromDerivedPropertyThroughExistsToNoArgMilestonedPropertyInLambda()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(businessDate=$bd)} : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-12-12)->filter(p|$p.myClassification(%2022-11-12)->exists(c|$c.exchange.exchangeName == ''))}\n" +
                "}\n", "COMPILATION error at [16:93-100]: No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    public void testMilestoningContextAllowedToPropagateThroughSubTypeToNoArgMilestonedProperty()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(businessDate=$bd)} : test::Classification[*];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classification->subType(@test::ExtendedClassification).exchange.exchangeName}\n" +
                "}\n");
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromNoArgQualifiedPropertyThroughMapThroughSubTypeToNoArgMilestonedPropertyInLambda()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(businessDate=$bd)} : test::Classification[*];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::ExtendedClassification extends test::Classification{\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classification->map(t|$t->subType(@test::ExtendedClassification).exchange.exchangeName)}\n" +
                "}\n");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateThroughFunctionWhichDoesNotAllowMilestoningContextPropagation()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   classificationType : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-15)->test::outOfScopeFunction(p: test::Product[1]|$p.classification.classificationType == '')}\n" +
                "}" +
                "function test::outOfScopeFunction(value:test::Product[*], func:Function[1]):test::Product[*]{$value}\n", "COMPILATION error at [10:86-99]: No-Arg milestoned property: 'classification' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllToMultipleNoArgQualifiedProperties()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).classification.exchange.exchangeName}\n" +
                "}\n");
    }

    @Test
    public void testMilestoningContextAllowedToPropagateAsVariableFromAllToNoArgQualifiedProperty()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   classificationName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   let bdVar=%2022-11-12;" +
                "   {|test::Product.all($bdVar).classification.classificationName};" +
                "}\n");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateFromMilestonedQualifiedPropertyToNoArgMilestonedProperty()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "   myClassification(bd:Date[1]){^test::Classification(businessDate=$bd)} : test::Classification[1];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[0..1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12).myClassification(%2022-11-12).exchange.exchangeName}\n" +
                "}\n", "COMPILATION error at [12:1-15:1]: Error in 'main::walkTree': No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    @Ignore
    public void testMilestoningContextNotAllowedToPropagateFromAllThroughEdgePointPropertyToNoArgMilestonedProperty()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[1];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-13).classificationAllVersions.exchange.exchangeName}\n" +
                "}\n", "COMPILATION error at [11:1-14:1]: Error in 'main::walkTree': No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    @Ignore
    public void testProcessingErrorWhenMilestoningContextIsNotAvailableToNoArgMilestonedProperty()
    {
        test("###Pure\n" +
                "Class test::Order{\n" +
                "   product : test::Product[1];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Product{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Order.all().product.name}\n" +
                "}\n", "COMPILATION error at [8:1-11:1]: Error in 'main::walkTree': No-Arg milestoned property: 'product' must be either called in a milestoning context or supplied with [businessDate] parameters");
    }

    @Test
    public void testNoProcessingErrorWhenMilestoningContextIsNotAvailableFromSourceQualifiedMilestonedPropertyWithDateParam()
    {
        test("###Pure\n" +
                "Class test::Order{\n" +
                "   product : test::Product[1];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Product{\n" +
                "   name : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Order.all().product(%2016-5-1).name}\n" +
                "}\n");
    }

    @Test
    public void testMilestoningContextAllowedToPropagateThroughAutoMappedQualifiedMilestonedPropertyWithDateParam()
    {
        test("###Pure\n" +
                "Class test::Order{\n" +
                "   orderEvents : test::OrderEvents[*];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::OrderEvents{\n" +
                "   classification : test::Classification[1];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   classificationType : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Order.all().orderEvents(%2016-5-1).classification.classificationType}\n" +
                "}\n");
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughProjectToNoArgMilestonedPropertyInLambda()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[1];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchange : test::Exchange[1];\n" +
                "   classificationType : String[1];\n" +
                "}\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{\n" +
                "   exchangeName : String[1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.all(%2022-11-12)->project([p|$p.classification.classificationType, p|$p.classification.exchange.exchangeName],['exchangeType','classificationType'])}\n" +
                "}\n");
    }

    @Test
    @Ignore
    public void testBiTemporalDatesNotSupplied()
    {
        test("###Pure\n" +
                "Class test::Order { createdLocation : test::Location[0..1]; }\n" +
                "Class <<temporal.bitemporal>> test::Location{ place : String[1];}\n" +
                "function main::walkTree(): Any[*] { {|test::Order.all().createdLocation.place} }\n", "COMPILATION error at [4:1-80]: Error in 'main::walkTree': No-Arg milestoned property: 'createdLocation' must be either called in a milestoning context or supplied with [processingDate, businessDate] parameters");
    }

    @Test
    public void testBiTemporalDatesArePropagatedFromBiTemporalRoot()
    {
        test("###Pure\n" +
                "Class <<temporal.bitemporal>> test::Order { createdLocation : test::Location[0..1]; }\n" +
                "Class <<temporal.bitemporal>> test::Location{ place : String[1];}\n" +
                "function main::walkTree(): Any[*] { {|test::Order.all(%2022-11-12, %2022-11-12).createdLocation.place} }\n");
    }

    @Test
    public void testBiTemporalDatesArePropagatedFromBiTemporalToBiTemporalInProject()
    {
        test("###Pure\n" +
                "Class  test::Order { createdLocation : test::Location[0..1]; }\n" +
                "Class <<temporal.bitemporal>> test::Location{ place : test::Place[1];}\n" +
                "Class <<temporal.bitemporal>> test::Place{ name : String[1];}\n" +
                "function main::walkTree(): Any[*] { {|test::Order.all().createdLocation(%2022-11-12, %2022-11-12).place.name} }\n");
    }

    @Test
    public void testBusinessTemporalDatesArePropagatedFromBusinessTemporal()
    {
        test("###Pure\n" +
                "Class  test::Order { createdLocation : test::Location[0..1]; }\n" +
                "Class <<temporal.businesstemporal>> test::Location{ place : test::Place[1];}\n" +
                "Class <<temporal.businesstemporal>> test::Place{ name : String[1];}\n" +
                "function main::walkTree(): Any[*] { {|test::Order.all().createdLocation(%2020-11-12).place.name} }\n");
    }

    @Test
    @Ignore
    public void testBiTemporalPropertyUsageWhenOnlyOneDatePropagated()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Order { createdLocation : test::Location[0..1]; }\n" +
                "Class <<temporal.bitemporal>> test::Location{ place : String[1];}\n" +
                "function main::walkTree(): Any[*] { {|test::Order.all(%2022-11-12).createdLocation.place} }\n", "COMPILATION error at [4:1-91]: Error in 'main::walkTree': No-Arg milestoned property: 'createdLocation' must be either called in a milestoning context or supplied with [processingDate, businessDate] parameters");
    }

    @Test
    public void testBusinessDatePropagatedToBiTemporalTypeWhenProcessingDateSupplied()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Order { createdLocation : test::Location[0..1]; }\n" +
                "Class <<temporal.bitemporal>> test::Location{ place : String[1];}\n" +
                "function main::walkTree(): Any[*] { {|test::Order.all(%2022-11-12).createdLocation(%2022-11-12).place} }\n");
    }

    @Test
    public void testProcessingDatePropagatedToBiTemporalTypeWhenBusinessDateSupplied()
    {
        test("###Pure\n" +
                "Class <<temporal.processingtemporal>> test::Order { createdLocation : test::Location[0..1]; }\n" +
                "Class <<temporal.bitemporal>> test::Location{ place : String[1];}\n" +
                "function main::walkTree(): Any[*] { {|test::Order.all(%2022-11-12).createdLocation(%2022-11-12).place} }\n");
    }

    @Test
    public void testPropagationOfSingleDateFromBiTemporalAll()
    {
        test("###Pure\n" +
                "Class <<temporal.bitemporal>> test::Product { exchange : test::Exchange[0..1]; }\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{ location : test::Location[1];}\n" +
                "Class <<temporal.businesstemporal>> test::Location{ street : String[1];}\n" +
                "function main::walkTree(): Any[*] { {|test::Product.all(%2022-11-12, %2012-11-12).exchange.location.street} }\n");
    }

    @Test
    public void testPropagationOfSingleDateFromBiTemporalQualifiedProperty()
    {
        test("###Pure\n" +
                "Class test::Product { exchange : test::Exchange[0..1]; }\n" +
                "Class <<temporal.businesstemporal>> test::Exchange{ location : test::Location[1];}\n" +
                "Class <<temporal.businesstemporal>> test::Location{ street : String[1];}\n" +
                "function main::walkTree(): Any[*] { {|test::Product.all().exchange(%2022-11-12).location.street} }\n");
    }

    //alloy doesn't support allVersionsInRange
    @Test
    @Ignore
    public void testAllVersionsInRangePropertyUsageForBusinessTemporalWithNoDatesSupplied()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange}\n" +
                "}\n", "Compilation error at (resource:test.pure line:4 column:55), \"The property 'classificationAllVersionsInRange' is milestoned with stereotypes: [ businesstemporal ] and requires 2 date parameters : [start, end]");
    }

    @Test
    @Ignore
    public void testAllVersionsInRangePropertyUsageForBusinessTemporalWithOneDateSupplied()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1)}\n" +
                "}\n", "Compilation error at (resource:test.pure line:4 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1])");
    }

    @Test
    @Ignore
    public void testAllVersionsInRangePropertyUsageForBusinessTemporalWithThreeDatesSupplied()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-5, %2018-1-9)}\n" +
                "}\n", "Compilation error at (resource:test.pure line:4 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1],_:StrictDate[1],_:StrictDate[1])");
    }

    @Test
    @Ignore
    public void testAllVersionsInRangePropertyUsageForBusinessTemporal()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}\n" +
                "}\n");
    }

    @Test
    @Ignore
    public void testAllVersionsInRangePropertyUsageForProcessingTemporalWithNoDatesSupplied()
    {
        test("###Pure\n" +
                "Class <<temporal.processingtemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.processingtemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange}\n" +
                "}\n", "Compilation error at (resource:test.pure line:4 column:55), \"The property 'classificationAllVersionsInRange' is milestoned with stereotypes: [ processingtemporal ] and requires 2 date parameters : [start, end]");
    }

    @Test
    @Ignore
    public void testAllVersionsInRangePropertyUsageForProcessingTemporalWithOneDateSupplied()
    {
        test("###Pure\n" +
                "Class <<temporal.processingtemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.processingtemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1)}\n" +
                "}\n", "Compilation error at (resource:test.pure line:4 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1])");
    }

    @Test
    @Ignore
    public void testAllVersionsInRangePropertyUsageForProcessingTemporalWithThreeDatesSupplied()
    {
        test("###Pure\n" +
                "Class <<temporal.processingtemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.processingtemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-5, %2018-1-9)}\n" +
                "}\n", "Compilation error at (resource:test.pure line:4 column:55), \"The system can't find a match for the function: classificationAllVersionsInRange(_:Product[1],_:StrictDate[1],_:StrictDate[1],_:StrictDate[1])");
    }

    @Test
    @Ignore
    public void testAllVersionsInRangePropertyUsageForProcessingTemporal()
    {
        test("###Pure\n" +
                "Class <<temporal.processingtemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.processingtemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%2018-1-1, %2018-1-9)}\n" +
                "}\n");
    }

    @Test
    @Ignore
    public void testAllVersionsInRangePropertyUsageForLatestDate()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Product{\n" +
                "   classification : test::Classification[*];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> test::Classification{\n" +
                "   exchangeName : String[0..1];\n" +
                "}\n" +
                "function main::walkTree(): Any[*]\n" +
                "{\n" +
                "   {|test::Product.allVersionsInRange(%2018-1-1, %2018-1-9).classificationAllVersionsInRange(%latest, %latest)}\n" +
                "}\n", "Compilation error at (resource:sourceId.pure line:10 column:55), \"%latest not a valid parameter for AllVersionsInRange()");
    }
}
