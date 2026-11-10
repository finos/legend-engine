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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_protocols_pure_vX_X_X_metamodel_m3_PackageableElement;
import org.finos.legend.pure.generated.Root_meta_protocols_pure_vX_X_X_metamodel_mapping_Mapping;
import org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_mapping;
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
        testFunctionRoundtripWithPureProtocolTransformation("  ^meta::legend::test::model::model::ClassWithConstraint(a='testAssociation')\n");
    }

    @Test
    public void testFunctionWithHackedClassParameterRoundtripWithPureProtocolTransformation() throws Exception
    {
        testFunctionRoundtripWithPureProtocolTransformation("  $c->subType(@meta::legend::test::model::model::ClassWithConstraint)\n");
    }

    @Test
    public void testClassWithDefaultValue() throws Exception
    {
        roundTripElement("test::Person","Class test::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  age: Integer[1] = 5;\n" +
                "}");
    }

    @Test
    public void testRelationMappingJoinConstructsProtocolTransferRoundTrip() throws Exception
    {
        roundTripMapping("my::testMapping",
                "Class my::Person\n" +
                        "{\n" +
                        "  firstName: String[1];\n" +
                        "  firmId: Integer[1];\n" +
                        "  firm: my::Firm[0..1];\n" +
                        "}\n" +
                        "Class my::Firm\n" +
                        "{\n" +
                        "  id: Integer[1];\n" +
                        "  legalName: String[1];\n" +
                        "}\n" +
                        "function my::personRel(): meta::pure::metamodel::relation::Relation<(FIRSTNAME:String[1], FIRMID:Integer[1])>[1]\n" +
                        "{\n" +
                        "  1->cast(@meta::pure::metamodel::relation::Relation<(FIRSTNAME:String[1], FIRMID:Integer[1])>);\n" +
                        "}\n" +
                        "function my::firmRel(): meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1])>[1]\n" +
                        "{\n" +
                        "  1->cast(@meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1])>);\n" +
                        "}\n",
                "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~src my::personRel()\n" +
                        "    ~primaryKey: FIRSTNAME\n" +
                        "    firstName: $row.FIRSTNAME,\n" +
                        "    +firmName: String[1]: $src->navigationJoin(my::firmRel(), {p, f|$p.FIRMID == $f.ID}).LEGALNAME,\n" +
                        "    +ownName: String[1]: $src->filterJoin(my::firmRel(), {p, f|$p.FIRMID == $f.ID}).FIRSTNAME,\n" +
                        // Inline modelJoin's condition compiles to the same _mj_src/_mj_tgt sentinels
                        // classic Association ModelJoin uses, so transfer-back-to-protocol shows those
                        // literal names rather than the user's originally-typed parameter names — a
                        // lossy-but-stable round-trip, the same precedent Association ModelJoin already
                        // sets by showing property names instead of the user's typed names.
                        "    firm: meta::pure::functions::relation::modelJoin({_mj_src, _mj_tgt|$_mj_src.firmId == $_mj_tgt.id}),\n" +
                        "    +employer: my::Firm[0..1]: meta::pure::functions::relation::modelJoin({_mj_src, _mj_tgt|$_mj_src.firmId == $_mj_tgt.id})\n" +
                        "  }\n" +
                        "  *my::Firm[firm]: Relation\n" +
                        "  {\n" +
                        "    ~src my::firmRel()\n" +
                        "    ~primaryKey: ID\n" +
                        "    id: $row.ID,\n" +
                        "    legalName: $row.LEGALNAME\n" +
                        "  }\n" +
                        ")");
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

    private void roundTripMapping(String mappingPath, String domainCode, String mappingCode) throws Exception
    {
        PureModel pm = compileIntoPureModel(domainCode + "###Mapping\n" + mappingCode);
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping) pm.getPackageableElement(mappingPath);
        Root_meta_protocols_pure_vX_X_X_metamodel_mapping_Mapping mappingAsPureProtocol = core_pure_protocol_vX_X_X_transfers_mapping.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_mapping_transformMapping_Mapping_1__Extension_MANY__Mapping_1_(mapping, Lists.fixedSize.empty(), pm.getExecutionSupport());
        PackageableElement mappingAsProtocol = objectMapper.readValue(org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(mappingAsPureProtocol, pm.getExecutionSupport()), PackageableElement.class);
        Assert.assertEquals(mappingCode, mappingAsProtocol.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build()));
    }

    private PackageableElement transformPureElementToPureProtocolViaPure(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement, ExecutionSupport executionSupport) throws Exception
    {
        Root_meta_protocols_pure_vX_X_X_metamodel_m3_PackageableElement pureElementAsProtocol = core_pure_protocol_vX_X_X_transfers_metamodel.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_domain_transformPackageableElement_PackageableElement_1__Extension_MANY__PackageableElement_1_(packageableElement, Lists.fixedSize.empty(), executionSupport);
        return objectMapper.readValue(org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(pureElementAsProtocol, executionSupport), PackageableElement.class);
    }

    private PureModel compileIntoPureModel(String pureCode)
    {
        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(pureCode);
        return new PureModel(pmcd, Identity.getAnonymousIdentity().getName(), DeploymentMode.TEST);
    }

}