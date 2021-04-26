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

package org.finos.legend.engine.language.pure.compiler.test.fromProtocol;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromProtocol;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.junit.Assert;
import org.junit.Test;

public class TestLambdaCompilationFromProtocol extends TestCompilationFromProtocol.TestCompilationFromProtocolTestSuite
{
    private static final String WALK_TREE = "main::walkTree_main::Person_MANY__String_MANY_";

    @Test
    public void testTwoParameterLambdaTypeInference()
    {
        PureModel pureModel = testWithProtocolPath("twoParameterLambdas.json");

        ConcreteFunctionDefinition walkTree = pureModel.getConcreteFunctionDefinition(WALK_TREE, null);
        SimpleFunctionExpression fold = (SimpleFunctionExpression) walkTree._expressionSequence().getFirst();
        InstanceValue iv = (InstanceValue) ((FastList) fold._parametersValues()).get(1);
        SimpleFunctionExpression concat = (SimpleFunctionExpression) ((LambdaFunction) iv._values().getFirst())._expressionSequence().getFirst();

        Assert.assertEquals(pureModel.getType("String"), fold._genericType()._rawType());
        Assert.assertEquals(pureModel.getType("String"), concat._genericType()._rawType());
    }
}
