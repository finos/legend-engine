// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.language.pure.compiler;

import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.junit.Assert;
import org.junit.Test;

public class CompilerTest
{
    @Test
    public void getLambdaReturnGenericTypeForPrimitive()
    {
        LambdaFunction lambda = PureGrammarParser.newInstance().parseLambda("|1234");
        GenericType genericType = Compiler.getLambdaReturnGenericType(lambda, PureModel.getCorePureModel());
        Assert.assertTrue(genericType.rawType instanceof PackageableType);
        Assert.assertEquals("Integer", ((PackageableType) genericType.rawType).fullPath);
    }

    @Test
    public void getLambdaReturnGenericTypeWithTypeArguments()
    {
        LambdaFunction lambda = PureGrammarParser.newInstance().parseLambda("|pair(1, '2')");
        GenericType genericType = Compiler.getLambdaReturnGenericType(lambda, PureModel.getCorePureModel());
        Assert.assertTrue(genericType.rawType instanceof PackageableType);
        Assert.assertEquals("meta::pure::functions::collection::Pair", ((PackageableType) genericType.rawType).fullPath);
        Assert.assertEquals(Lists.mutable.with("Integer", "String"), genericType.typeArguments.stream().map(x -> ((PackageableType) x.rawType).fullPath).collect(Collectors.toList()));
    }
}
