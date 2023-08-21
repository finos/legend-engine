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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.PropertyGraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.RootGraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TestMilestonedPropertyUsageInGraphFetch extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
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
    public void testDatePropagationInGraphFetch()
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
                        "   main::Person.all(%2022-12-12)->graphFetch(#{main::Person{firm{name}}}#)->serialize(#{main::Person{firm{name}}}#)\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree__String_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression serializeFunctionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        RootGraphFetchTree serializeTree = (RootGraphFetchTree)((InstanceValue) serializeFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);
        SimpleFunctionExpression graphFetchFunctionExpression = (SimpleFunctionExpression) serializeFunctionExpression._parametersValues().toList().get(0);
        RootGraphFetchTree graphTree = (RootGraphFetchTree)((InstanceValue) graphFetchFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);

        List<RootGraphFetchTree> gfts = Lists.mutable.with(graphTree, serializeTree);

        for (RootGraphFetchTree rootGFT:gfts)
        {
            PropertyGraphFetchTree firmPropertyGFT = propTree(rootGFT, "firm");
            assertSingleDateInPropertyTree().apply(firmPropertyGFT,"2022-12-12");
        }
    }

    @Test
    public void testDatePropagationInGraphFetchWithDateVariable()
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
                        "function main::walkTree(bdate:DateTime[1]): String[*]\n" +
                        "{\n" +
                        "   main::Person.all($bdate)->graphFetch(#{main::Person{firm{name}}}#)->serialize(#{main::Person{firm{name}}}#)\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree_DateTime_1__String_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression serializeFunctionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        RootGraphFetchTree serializeTree = (RootGraphFetchTree)((InstanceValue) serializeFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);
        SimpleFunctionExpression graphFetchFunctionExpression = (SimpleFunctionExpression) serializeFunctionExpression._parametersValues().toList().get(0);
        RootGraphFetchTree graphTree = (RootGraphFetchTree)((InstanceValue) graphFetchFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);

        List<RootGraphFetchTree> gfts = Lists.mutable.with(graphTree, serializeTree);

        for (RootGraphFetchTree rootGFT:gfts)
        {
            PropertyGraphFetchTree firmPropertyGFT = propTree(rootGFT, "firm");
            assertPropertyTreeHasSufficientParameters(firmPropertyGFT);

            VariableExpression parameterValue = (VariableExpression) firmPropertyGFT._parameters().toList().get(0);
            Assert.assertEquals("bdate", parameterValue._name());
        }
    }

    @Test
    public void testDatePropagationInGraphFetchChecked()
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
                        "   main::Person.all(%2022-12-12)->graphFetchChecked(#{main::Person{firm{name}}}#)->serialize(#{main::Person{firm{name}}}#)\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree__String_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression serializeFunctionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        RootGraphFetchTree serializeTree = (RootGraphFetchTree)((InstanceValue) serializeFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);
        SimpleFunctionExpression graphFetchFunctionExpression = (SimpleFunctionExpression) serializeFunctionExpression._parametersValues().toList().get(0);
        RootGraphFetchTree graphTree = (RootGraphFetchTree)((InstanceValue) graphFetchFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);

        List<RootGraphFetchTree> gfts = Lists.mutable.with(graphTree, serializeTree);

        for (RootGraphFetchTree rootGFT:gfts)
        {
            PropertyGraphFetchTree firmPropertyGFT = propTree(rootGFT, "firm");
            assertSingleDateInPropertyTree().apply(firmPropertyGFT,"2022-12-12");
        }
    }

    @Test
    public void testDatePropagationInGraphFetchOnPropertyWithMissingDate()
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
                        "  city: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): String[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2022-12-12).firm->graphFetch(#{main::Firm{city{name}}}#)->serialize(#{main::Firm{city{name}}}#)\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree__String_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression serializeFunctionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        RootGraphFetchTree serializeTree = (RootGraphFetchTree)((InstanceValue) serializeFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);
        SimpleFunctionExpression graphFetchFunctionExpression = (SimpleFunctionExpression) serializeFunctionExpression._parametersValues().toList().get(0);
        RootGraphFetchTree graphTree = (RootGraphFetchTree)((InstanceValue) graphFetchFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);

        List<RootGraphFetchTree> gfts = Lists.mutable.with(graphTree, serializeTree);

        for (RootGraphFetchTree rootGFT:gfts)
        {
            PropertyGraphFetchTree firmPropertyGFT = propTree(rootGFT, "city");
            assertSingleDateInPropertyTree().apply(firmPropertyGFT,"2022-12-12");
        }
    }

    @Test
    public void testDatePropagationInGraphFetchDeepWithDifferentDates()
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
                        "  city: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): String[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2022-12-12)->graphFetch(#{main::Person{firm(%2020-12-12T00:00:00){name,city{name}}}}#)->serialize(#{main::Person{firm(%2020-12-12T00:00:00){name,city{name}}}}#)\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree__String_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression serializeFunctionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        RootGraphFetchTree serializeTree = (RootGraphFetchTree)((InstanceValue) serializeFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);
        SimpleFunctionExpression graphFetchFunctionExpression = (SimpleFunctionExpression) serializeFunctionExpression._parametersValues().toList().get(0);
        RootGraphFetchTree graphTree = (RootGraphFetchTree)((InstanceValue) graphFetchFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);

        List<RootGraphFetchTree> gfts = Lists.mutable.with(graphTree, serializeTree);

        for (RootGraphFetchTree rootGFT:gfts)
        {
            PropertyGraphFetchTree firmPropertyGFT = propTree(rootGFT, "firm");
            assertSingleDateInPropertyTree().apply(firmPropertyGFT,"2020-12-12");

            PropertyGraphFetchTree cityPropertyGFT = propTree(firmPropertyGFT, "city");
            assertSingleDateInPropertyTree().apply(cityPropertyGFT,"2020-12-12");
        }
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateFromEdgePointPropertyToNoArgMilestonedPropertyInGraphFetch()
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
                        "  city: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): String[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2022-12-12)->graphFetch(#{main::Person{firmAllVersions{name,city{name}}}}#)->serialize(#{main::Person{firmAllVersions{name,city{name}}}}#)\n" +
                        "}\n");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree__String_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression serializeFunctionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        RootGraphFetchTree serializeTree = (RootGraphFetchTree)((InstanceValue) serializeFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);
        SimpleFunctionExpression graphFetchFunctionExpression = (SimpleFunctionExpression) serializeFunctionExpression._parametersValues().toList().get(0);
        RootGraphFetchTree graphTree = (RootGraphFetchTree)((InstanceValue) graphFetchFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);

        List<RootGraphFetchTree> gfts = Lists.mutable.with(graphTree, serializeTree);

        for (RootGraphFetchTree rootGFT:gfts)
        {
            PropertyGraphFetchTree firmPropertyGFT = propTree(rootGFT, "firmAllVersions");
            assertNoDateInPropertyTree().apply(firmPropertyGFT);

            PropertyGraphFetchTree cityPropertyGFT = propTree(firmPropertyGFT, "city");
            assertNoDateInPropertyTree().apply(cityPropertyGFT);
            //TODO : this should raise compiler error that city needs to be supplied with Date
        }
    }

    @Test
    public void testDatePropagationInGraphFetchDeepWithFromAndSerialize()
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
                        "  city: main::City[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> main::City\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function main::walkTree(): String[*]\n" +
                        "{\n" +
                        "   main::Person.all(%2022-12-12)" +
                        "       ->graphFetch(#{main::Person{firm(%2020-12-12T00:00:00){name,city{name}}}}#)" +
                        "       ->from(main::dummyMapping, main::dummyRuntime) " +
                        "       ->serialize(#{main::Person{firm(%2020-12-12T00:00:00){name,city{name}}}}#)\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping main::dummyMapping()\n" +
                        "\n" +
                        "###Runtime\n" +
                        "Runtime main::dummyRuntime\n" +
                        "{\n" +
                        "  mappings:\n" +
                        "  [\n" +
                        "    main::dummyMapping\n" +
                        "  ];\n" +
                        "  connections:\n" +
                        "  [\n" +
                        "  ];\n" +
                        "}");
        PureModel pureModel = modelWithInput.getTwo();
        String WALK_TREE = "main::walkTree__String_MANY_";
        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression serializeFunctionExpression = (SimpleFunctionExpression) walkTree._expressionSequence().toList().get(0);
        RootGraphFetchTree serializeTree = (RootGraphFetchTree)((InstanceValue) serializeFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);

        SimpleFunctionExpression fromFunctionExpression = (SimpleFunctionExpression) serializeFunctionExpression._parametersValues().toList().get(0);
        SimpleFunctionExpression graphFetchFunctionExpression = (SimpleFunctionExpression) fromFunctionExpression._parametersValues().toList().get(0);
        RootGraphFetchTree graphTree = (RootGraphFetchTree)((InstanceValue) graphFetchFunctionExpression._parametersValues().toList().get(1))._values().toList().get(0);

        List<RootGraphFetchTree> gfts = Lists.mutable.with(graphTree, serializeTree);

        for (RootGraphFetchTree rootGFT:gfts)
        {
            PropertyGraphFetchTree firmPropertyGFT = propTree(rootGFT, "firm");
            assertSingleDateInPropertyTree().apply(firmPropertyGFT,"2020-12-12");

            PropertyGraphFetchTree cityPropertyGFT = propTree(firmPropertyGFT, "city");
            assertSingleDateInPropertyTree().apply(cityPropertyGFT,"2020-12-12");
        }
    }


    public PropertyGraphFetchTree propTree(GraphFetchTree gft, String propName)
    {
       return (PropertyGraphFetchTree) gft._subTrees().detect(p -> ((PropertyGraphFetchTree)p)._property()._name().equals(propName));
    }

    Function<PropertyGraphFetchTree, Void> assertNoDateInPropertyTree()
    {
        return (propTree) ->
        {
            Assert.assertEquals(propTree._parameters().toList().size(), 0);
            return null;
        };
    }

    BiFunction<PropertyGraphFetchTree,String, Void> assertSingleDateInPropertyTree()
    {
        return (propTree, date) ->
        {
            assertPropertyTreeHasSufficientParameters(propTree);
            InstanceValue parameterValue = (InstanceValue) propTree._parameters().toList().get(0);
            String ds = ((PureDate)parameterValue._values().toList().get(0)).format("yyyy-MM-dd");
            Assert.assertEquals(ds,date);
            return null;
        };
    }

    public void assertPropertyTreeHasSufficientParameters(PropertyGraphFetchTree propTree)
    {
        AbstractProperty prop = propTree._property();

        if (prop instanceof Property)
        {
            Assert.assertEquals(propTree._parameters().size(), 0);
        }
        else if (prop instanceof QualifiedProperty)
        {
            FunctionType propFuncType = (FunctionType) propTree._property()._classifierGenericType()._typeArguments().getFirst()._rawType();
            Assert.assertEquals(propFuncType._parameters().size(), propTree._parameters().size() + 1);  // propertyTree doesnt contain property owner param for property
        }
    }

}
