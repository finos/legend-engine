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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_protocols_pure_vX_X_X_metamodel_PackageableElement;
import org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_metamodel;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.junit.Assert;
import org.junit.Test;

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

    @Test
    public void testClassWithDefaultValue() throws Exception
    {
        roundTripElement("test::Person","Class test::Person extends meta::pure::metamodel::type::Any\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  age: Integer[1] = 5;\n" +
                "}");
    }

    private void testFunctionRoundtripWithPureProtocolTransformation(String body) throws Exception
    {
        String pureFunctionCode = "function test::package::newClassWithConstraint(c: meta::legend::test::model::model::ClassWithConstraint[1]): meta::legend::test::model::model::ClassWithConstraint[1]\n{\n" + body + "}";
        roundTripElement("test::package::newClassWithConstraint_ClassWithConstraint_1__ClassWithConstraint_1_", pureFunctionCode);
    }

    private void roundTripElement(String element, String pureFunctionCode) throws Exception
    {
        PureModel pm = compileIntoPureModel(pureFunctionCode);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pm.getPackageableElement(element);
        PackageableElement pureElementAsPureEngineProtocol = transformPureElementToPureProtocolViaPure(packageableElement, pm.getExecutionSupport());
        String pureCodeViaEngineComposer = pureElementAsPureEngineProtocol.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build());
        Assert.assertEquals(pureFunctionCode, pureCodeViaEngineComposer);
        PureGrammarParser.newInstance().parseModel(pureCodeViaEngineComposer);
    }

    private PackageableElement transformPureElementToPureProtocolViaPure(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement, ExecutionSupport executionSupport) throws Exception
    {
        Root_meta_protocols_pure_vX_X_X_metamodel_PackageableElement pureElementAsProtocol = core_pure_protocol_vX_X_X_transfers_metamodel.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_domain_transformPackageableElement_PackageableElement_1__Extension_MANY__PackageableElement_1_(packageableElement, Lists.fixedSize.empty(), executionSupport);
        return objectMapper.readValue(org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(pureElementAsProtocol, executionSupport), PackageableElement.class);
    }

    private PureModel compileIntoPureModel(String pureCode)
    {
        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(pureCode);
        return new PureModel(pmcd, FastList.newList(), DeploymentMode.TEST);
    }

}