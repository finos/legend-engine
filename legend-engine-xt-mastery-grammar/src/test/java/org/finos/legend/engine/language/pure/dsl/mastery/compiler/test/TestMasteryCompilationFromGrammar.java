// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;

//import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_MasterRecordDefinition;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_identity_IdentityResolution;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_identity_ResolutionQuery;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_LambdaFunction_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestMasteryCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    public static String MAPPING_AND_CONNECTION = "###Pure\n" +
            "Class test::connection::class\n" +
            "{\n" +
            "}\n\n\n" +
            "###Mapping\n" +
            "Mapping test::Mapping\n(\n)\n\n\n" +
            "###Connection\n" +
            "JsonModelConnection test::connection\n" +
            "{\n" +
            "  class: test::connection::class;\n" +
            "  url: 'test/connection/url';\n" +
            "}\n\n\n";
    public static String WIDGET_SERVICE_BODY =  "{\n" +
            "  pattern: 'test';\n" +
            "  documentation: 'test';\n" +
            "  autoActivateUpdates: true;\n" +
            "  execution: Single\n" +
            "  {\n" +
            "    query: src: org::dataeng::Widget[1]|$src.widgetId;\n" +
            "    mapping: test::Mapping;\n" +
            "    runtime:\n" +
            "    #{\n" +
//            "      connections: [];\n" + - Failed intermittently so added a connection.
            "      connections:\n" +
            "      [\n" +
            "        ModelStore:\n" +
            "        [\n" +
            "          id1: test::connection\n" +
            "        ]\n" +
            "      ];\n" +
            "    }#;\n" +
            "  }\n" +
            "  test: Single\n" +
            "  {\n" +
            "    data: 'test';\n" +
            "    asserts:\n    [\n    ];\n" +
            "  }\n" +
            "}\n";
    public static String COMPLETE_CORRECT_MASTERY_MODEL = "###Pure\n" +
            "Class org::dataeng::Widget\n" +
            "{\n" +
            "  widgetId: String[0..1];\n" +
            "  identifiers: org::dataeng::MilestonedIdentifier[*];\n" +
            "}\n\n" +
            "Class org::dataeng::MilestonedIdentifier\n" +
            "{\n" +
            "  identifierType: String[1];\n" +
            "  identifier: String[1];\n" +
            "  FROM_Z: StrictDate[0..1];\n" +
            "  THRU_Z: StrictDate[0..1];\n" +
            "}\n\n\n" +
            MAPPING_AND_CONNECTION +
            "###Service\n" +
            "Service org::dataeng::ParseWidget\n" + WIDGET_SERVICE_BODY + "\n" +
            "Service org::dataeng::TransformWidget\n" + WIDGET_SERVICE_BODY + "\n" +
            "\n" +
            "###Mastery\n" + "MasterRecordDefinition alloy::mastery::WidgetMasterRecord" +
            "\n" +
            //"\nMasterRecordDefinition " + ListAdapter.adapt(keywords).makeString("::") + "\n" + //Fails on the use of import
            "{\n" +
            "  modelClass: org::dataeng::Widget;\n" +
            "  identityResolution: \n" +
            "  {\n" +
            "    modelClass: org::dataeng::Widget;\n" +
            "    resolutionQueries:\n" +
            "      [\n" +
            "        {\n" +
            "          queries: [ {input: org::dataeng::Widget[1]|org::dataeng::Widget.all()->filter(widget|$widget.widgetId == $input.widgetId)}\n" +
            "                   ];\n" +
            "          keyType: GeneratedPrimaryKey;\n" +
            "          precedence: 1;\n" +
            "        },\n" +
            "        {\n" +
            "          queries: [ {input: org::dataeng::Widget[1],EFFECTIVE_DATE: StrictDate[1]|org::dataeng::Widget.all()->filter(widget|" +
            "((($widget.identifiers.identifierType == 'ISIN') && " +
            "($input.identifiers->filter(idType|$idType.identifierType == 'ISIN').identifier == $widget.identifiers->filter(idType|$idType.identifierType == 'ISIN').identifier)) && " +
            "($widget.identifiers.FROM_Z->toOne() <= $EFFECTIVE_DATE)) && " +
            "($widget.identifiers.THRU_Z->toOne() > $EFFECTIVE_DATE))}\n" +
            "                   ];\n" +
            "          keyType: AlternateKey;\n" +
            "          precedence: 2;\n" +
            "        }\n" +
            "      ]\n" +
            "  }\n" +
            "  recordSources:\n" +
            "  [\n" +
            "    {\n" +
            "      id: \'widget-file-single-partition\';\n" +
            "      description: \'Single partition source.\';\n" +
            "      status: Development;\n" +
            "      parseService: org::dataeng::ParseWidget;\n" +
            "      transformService: org::dataeng::TransformWidget;\n" +
            "      sequentialData: true;\n" +
            "      stagedLoad: false;\n" +
            "      createPermitted: true;\n" +
            "      createBlockedException: false;\n" +
            "      tags: [\'Refinitive DSP\'];\n" +
            "      partitions:\n" +
            "      [\n" +
            "        {\n" +
            "          id: \'partition-1\';\n" +
            "          tags: [\'Equity\', \'Global\', \'Full-Universe\'];\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      id: \'widget-file-multiple-partitions\';\n" +
            "      description: \'Multiple partition source.\';\n" +
            "      status: Production;\n" +
            "      parseService: org::dataeng::ParseWidget;\n" +
            "      transformService: org::dataeng::TransformWidget;\n" +
            "      sequentialData: false;\n" +
            "      stagedLoad: true;\n" +
            "      createPermitted: false;\n" +
            "      createBlockedException: true;\n" +
            "      tags: [\'Refinitive DSP Delta Files\'];\n" +
            "      partitions:\n" +
            "      [\n" +
            "        {\n" +
            "          id: \'ASIA-Equity\';\n" +
            "          tags: [\'Equity\', \'ASIA\'];\n" +
            "        },\n" +
            "        {\n" +
            "          id: \'EMEA-Equity\';\n" +
            "          tags: [\'Equity\', \'EMEA\'];\n" +
            "        },\n" +
            "        {\n" +
            "          id: \'US-Equity\';\n" +
            "          tags: [\'Equity\', \'US\'];\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    public static String MINIMUM_CORRECT_MASTERY_MODEL = "###Pure\n" +
            "Class org::dataeng::Widget\n" +
            "{\n" +
            "  widgetId: String[0..1];\n" +
            "  identifiers: org::dataeng::MilestonedIdentifier[*];\n" +
            "}\n\n" +
            "Class org::dataeng::MilestonedIdentifier\n" +
            "{\n" +
            "  identifierType: String[1];\n" +
            "  identifier: String[1];\n" +
            "  FROM_Z: StrictDate[0..1];\n" +
            "  THRU_Z: StrictDate[0..1];\n" +
            "}\n\n\n" +
            MAPPING_AND_CONNECTION +
            "###Service\n" +
            "Service org::dataeng::ParseWidget\n" + WIDGET_SERVICE_BODY + "\n" +
            "Service org::dataeng::TransformWidget\n" + WIDGET_SERVICE_BODY + "\n" +
            "\n" +
            "###Mastery\n" + "MasterRecordDefinition alloy::mastery::WidgetMasterRecord" +
            "\n" +
            //"\nMasterRecordDefinition " + ListAdapter.adapt(keywords).makeString("::") + "\n" + //Fails on the use of import
            "{\n" +
            "  modelClass: org::dataeng::Widget;\n" +
            "  identityResolution: \n" +
            "  {\n" +
            "    modelClass: org::dataeng::Widget;\n" +
            "    resolutionQueries:\n" +
            "      [\n" +
            "        {\n" +
            "          queries: [ {input: org::dataeng::Widget[1]|org::dataeng::Widget.all()->filter(widget|$widget.widgetId == $input.widgetId)}\n" +
            "                   ];\n" +
            "          keyType: GeneratedPrimaryKey;\n" +
            "          precedence: 1;\n" +
            "        }\n" +
            "      ]\n" +
            "  }\n" +
            "  recordSources:\n" +
            "  [\n" +
            "    {\n" +
            "      id: \'widget-file-single-partition\';\n" +
            "      description: \'Single partition source.\';\n" +
            "      status: Development;\n" +
            "      transformService: org::dataeng::TransformWidget;\n" +
            "      partitions:\n" +
            "      [\n" +
            "        {\n" +
            "          id: \'partition-1\';\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    public static String DUPLICATE_ELEMENT_MODEL = "###Pure\n" +
            "Class org::dataeng::Widget\n" +
            "{\n" +
            "  widgetId: String[0..1];\n" +
            "}\n\n" +
            "###Mastery\n" + "MasterRecordDefinition org::dataeng::Widget" +
            "\n" +
            "{\n" +
            "  modelClass: org::dataeng::Widget;\n" +
            "  identityResolution: \n" +
            "  {\n" +
            "    modelClass: org::dataeng::Widget;\n" +
            "    resolutionQueries:\n" +
            "      [\n" +
            "        {\n" +
            "          queries: [ {input: org::dataeng::Widget[1]|org::dataeng::Widget.all()->filter(widget|$widget.widgetId == $input.widgetId)}\n" +
            "                   ];\n" +
            "          keyType: GeneratedPrimaryKey;\n" +
            "          precedence: 1;\n" +
            "        }" +
            "      ]\n" +
            "  }\n" +
            "  recordSources:\n" +
            "  [\n" +
            "    {\n" +
            "      id: \'widget-file-single-partition\';\n" +
            "      description: \'Single partition source.\';\n" +
            "      status: Development;\n" +
            "      parseService: org::dataeng::ParseWidget;\n" +
            "      transformService: org::dataeng::TransformWidget;\n" +
            "      sequentialData: true;\n" +
            "      stagedLoad: false;\n" +
            "      createPermitted: true;\n" +
            "      createBlockedException: false;\n" +
            "      tags: [\'Refinitive DSP\'];\n" +
            "      partitions:\n" +
            "      [\n" +
            "        {\n" +
            "          id: \'partition-1\';\n" +
            "          tags: [\'Equity\'];\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    //Simple test from ### Text
    @Test
    public void testMasteryFullModel()
    {
        Pair<PureModelContextData, PureModel> result = test(COMPLETE_CORRECT_MASTERY_MODEL);
        PureModel model = result.getTwo();

        PackageableElement packageableElement = model.getPackageableElement("alloy::mastery::WidgetMasterRecord");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_mastery_metamodel_MasterRecordDefinition);

        //MasterRecord Definition modelClass
        Root_meta_pure_mastery_metamodel_MasterRecordDefinition masterRecordDefinition = (Root_meta_pure_mastery_metamodel_MasterRecordDefinition) packageableElement;
        assertEquals("Widget", masterRecordDefinition._modelClass()._name());

        //IdentityResolution
        Root_meta_pure_mastery_metamodel_identity_IdentityResolution idRes = masterRecordDefinition._identityResolution();
        assert (idRes instanceof Root_meta_pure_mastery_metamodel_identity_IdentityResolution);
        assertEquals("Widget", idRes._modelClass()._name());

        //Resolution Queries
        Object[] queriesArray = idRes._resolutionQueries().toArray();
        assertEquals(1, ((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[0])._precedence());
        assertEquals("GeneratedPrimaryKey", ((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[0])._keyType()._name());
        assertPureLambdas(((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[0])._queries().toList());

        assertEquals(2, ((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[1])._precedence());
        assertEquals("AlternateKey", ((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[1])._keyType()._name());
        assertPureLambdas(((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[0])._queries().toList());


        //RecordSources
        ListIterate.forEachWithIndex(masterRecordDefinition._sources().toList(), (source, i) ->
        {
            if (i == 0)
            {
                assertEquals("\'widget-file-single-partition\'", source._id());
                assertEquals("Development", source._status().getName());
                assertEquals(true, source._sequentialData());
                assertEquals(false, source._stagedLoad());
                assertEquals(true, source._createPermitted());
                assertEquals(false, source._createBlockedException());
                assertEquals("[\'Refinitive DSP\']", source._tags().toString());
                ListIterate.forEachWithIndex(source._partitions().toList(), (partition, j) ->
                {
                    assertEquals("\'partition-1\'", partition._id());
                    assertEquals("[\'Equity\', \'Global\', \'Full-Universe\']", partition._tags().toString());
                });
            }
            else if (i == 1)
            {
                assertEquals("\'widget-file-multiple-partitions\'", source._id());
                assertEquals("Production", source._status().getName());
                assertEquals(false, source._sequentialData());
                assertEquals(true, source._stagedLoad());
                assertEquals(false, source._createPermitted());
                assertEquals(true, source._createBlockedException());
                assertEquals("[\'Refinitive DSP Delta Files\']", source._tags().toString());
                ListIterate.forEachWithIndex(source._partitions().toList(), (partition, j) ->
                {
                    if (j == 0)
                    {
                        assertEquals("\'ASIA-Equity\'", partition._id());
                        assertEquals("[\'Equity\', \'ASIA\']", partition._tags().toString());
                    }
                    else if (j == 1)
                    {
                        assertEquals("\'EMEA-Equity\'", partition._id());
                        assertEquals("[\'Equity\', \'EMEA\']", partition._tags().toString());
                    }
                    else if (j == 2)
                    {
                        assertEquals("\'US-Equity\'", partition._id());
                        assertEquals("[\'Equity\', \'US\']", partition._tags().toString());
                    }
                    else
                    {
                        fail("Didn't expect a partition at index:" + j);
                    }

                });
            }
            else
            {
                fail("Didn't expect a source at index:" + i);
            }
        });
    }

    private void assertPureLambdas(List list)
    {
        ListIterate.forEach(list, (resQuery) ->
        {
            assert (resQuery instanceof Root_meta_pure_metamodel_function_LambdaFunction_Impl);
        });
    }

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return DUPLICATE_ELEMENT_MODEL;
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [8:1-45:1]: Duplicated element 'org::dataeng::Widget'";
    }
}