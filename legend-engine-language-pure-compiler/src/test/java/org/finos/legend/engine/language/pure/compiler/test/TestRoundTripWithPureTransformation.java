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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_protocols_pure_vX_X_X_metamodel_domain_Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class TestRoundTripWithPureTransformation
{

    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testNewFunctionRoundtripWithPureProtocolTransformation() throws Exception
    {
        testFunctionRoundtripWithPureProtocolTransformation("   ^meta::legend::test::model::model::ClassWithConstraint(a='testAssociation')\n");
    }

    @Test
    public void testFunctionWithHackedClassParameterRoundtripWithPureProtocolTransformation() throws Exception
    {
        testFunctionRoundtripWithPureProtocolTransformation("   $c->subType(@meta::legend::test::model::model::ClassWithConstraint)\n");
    }

    private void testFunctionRoundtripWithPureProtocolTransformation(String body) throws Exception
    {
        String pureFunctionCode = "function test::package::newClassWithConstraint(c: meta::legend::test::model::model::ClassWithConstraint[1]): meta::legend::test::model::model::ClassWithConstraint[1]\n{\n" + body + "}";
        PureModel pm = compileIntoPureModel(pureFunctionCode);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> pureFunction = pm.getConcreteFunctionDefinition_safe("test::package::newClassWithConstraint_ClassWithConstraint_1__ClassWithConstraint_1_");
        Function pureFunctionAsPureEngineProtocol = transformPureFunctionToPureProtocolViaPure(pureFunction, pm.getExecutionSupport());
        String pureCodeViaEngineComposer = pureFunctionAsPureEngineProtocol.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build());
        Assert.assertEquals(pureFunctionCode, pureCodeViaEngineComposer);
        PureGrammarParser.newInstance().parseModel(pureCodeViaEngineComposer);
    }

    private Function transformPureFunctionToPureProtocolViaPure(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> pureFunction, ExecutionSupport executionSupport) throws Exception
    {
        String version = "vX_X_X";
        Class<?> cl = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + version + "_transfers_metamodel");
        Method graphFetchProtocolMethod = cl.getMethod("Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_transformFunction_ConcreteFunctionDefinition_1__Extension_MANY__Function_1_", ConcreteFunctionDefinition.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
        Root_meta_protocols_pure_vX_X_X_metamodel_domain_Function pureFuctionAsProtocol = (Root_meta_protocols_pure_vX_X_X_metamodel_domain_Function) graphFetchProtocolMethod.invoke(null, pureFunction, FastList.newList(), executionSupport);
        return objectMapper.readValue(org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(pureFuctionAsProtocol, executionSupport), Function.class);
    }

    private PureModel compileIntoPureModel(String pureCode)
    {
        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(pureCode);
        return new PureModel(pmcd, FastList.newList(), DeploymentMode.TEST);
    }

}