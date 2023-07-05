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

package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class TestHelperModelBuilder
{
    @Test
    public void testFunctionSignature()
    {
        Pair<PureModelContextData, PureModel> modelWithInput = TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test("###Pure \n" +
                        "function model::test(): String[*] \n" +
                        "{ \n" +
                        "    'test'; \n" +
                        "} \n");
        PureModelContextData pmcd = modelWithInput.getOne();
        String testFuntionName = "model::test__String_MANY_";
        List<PackageableElement> testFunction = pmcd.getElements().stream().filter(el -> testFuntionName.equals(el.getPath())).collect(Collectors.toList());
        Assert.assertEquals(1, testFunction.size());
        Assert.assertEquals("test__String_MANY_", HelperModelBuilder.getSignature((Function) testFunction.get(0)));
        Assert.assertEquals("test", HelperModelBuilder.getFunctionNameWithoutSignature((Function) testFunction.get(0)));
        Assert.assertEquals("test__String_MANY_", HelperModelBuilder.getTerseSignature((Function) testFunction.get(0)));
    }
}
