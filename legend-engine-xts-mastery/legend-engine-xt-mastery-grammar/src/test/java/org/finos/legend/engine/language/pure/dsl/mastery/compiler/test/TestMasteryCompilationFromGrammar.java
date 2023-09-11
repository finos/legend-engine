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

import com.google.common.collect.Lists;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestMasteryCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    public static String MAPPING_AND_CONNECTION = "###Pure\n" +
            "Class test::connection::class\n" +
            "{\n" +
            "}\n\n\n" +
            "###Mapping\n" +
            "Mapping test::Mapping\n(\n)\n\n\n" +
            "###Connection\n" +
            "JsonModelConnection test::connection::modelConnection\n" +
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
            "      connections:\n" +
            "      [\n" +
            "        ModelStore:\n" +
            "        [\n" +
            "          id1: test::connection::modelConnection\n" +
            "        ]\n" +
            "      ];\n" +
            "    }#;\n" +
            "  }\n" +
            "  testSuites:\n" +
            "  [\n" +
            "\n" +
            "  ]\n" +
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
            "Service org::dataeng::TransformWidget\n" + WIDGET_SERVICE_BODY +
            "\n\n###Mastery\n" +
            "MasterRecordDefinition alloy::mastery::WidgetMasterRecord\n" +
            "{\n" +
            "  modelClass: org::dataeng::Widget;\n" +
            "  identityResolution: \n" +
            "  {\n" +
            "    resolutionQueries:\n" +
            "      [\n" +
            "        {\n" +
            "          queries: [ {input: org::dataeng::Widget[1]|org::dataeng::Widget.all()->filter(widget|$widget.widgetId == $input.widgetId)}\n" +
            "                   ];\n" +
            "          keyType: GeneratedPrimaryKey;\n" +
            "          precedence: 1;\n" +
            "        },\n" +
            "        {\n" +
            "          queries: [ {input: org::dataeng::Widget[1],EFFECTIVE_DATE: StrictDate[1]|org::dataeng::Widget.all()->filter(widget|((($widget.identifiers.identifierType == 'ISIN') && ($input.identifiers->filter(idType|$idType.identifierType == 'ISIN').identifier == $widget.identifiers->filter(idType|$idType.identifierType == 'ISIN').identifier)) && ($widget.identifiers.FROM_Z->toOne() <= $EFFECTIVE_DATE)) && ($widget.identifiers.THRU_Z->toOne() > $EFFECTIVE_DATE))}\n" +
            "                   ];\n" +
            "          keyType: AlternateKey;\n" +
            "          precedence: 2;\n" +
            "        }\n" +
            "      ]\n" +
            "  }\n" +
            "  precedenceRules: [\n" +
            "    DeleteRule: {\n" +
            "      path: org::dataeng::Widget.identifiers;\n" +
            "      ruleScope: [\n" +
            "        RecordSourceScope {widget-rest-source}\n" +
            "      ];\n" +
            "    },\n" +
            "    CreateRule: {\n" +
            "      path: org::dataeng::Widget{$.widgetId == 1234}.identifiers.identifierType;\n" +
            "      ruleScope: [\n" +
            "        RecordSourceScope {widget-file-source-ftp},\n" +
            "        DataProviderTypeScope {Aggregator}\n" +
            "      ];\n" +
            "    },\n" +
            "    ConditionalRule: {\n" +
            "      predicate: {incoming: org::dataeng::Widget[1],current: org::dataeng::Widget[1]|$incoming.widgetId == $current.widgetId};\n" +
            "      path: org::dataeng::Widget.identifiers.identifierType;\n" +
            "    },\n" +
            "    SourcePrecedenceRule: {\n" +
            "      path: org::dataeng::Widget.identifiers{$.identifier == 'XLON'};\n" +
            "      action: Overwrite;\n" +
            "      ruleScope: [\n" +
            "        RecordSourceScope {widget-file-source-sftp, precedence: 1},\n" +
            "        DataProviderTypeScope {Exchange, precedence: 2}\n" +
            "      ];\n" +
            "    },\n" +
            "    SourcePrecedenceRule: {\n" +
            "      path: org::dataeng::Widget.identifiers;\n" +
            "      action: Overwrite;\n" +
            "      ruleScope: [\n" +
            "        RecordSourceScope {widget-rest-source, precedence: 2}\n" +
            "      ];\n" +
            "    }\n" +
            "  ]\n" +
            "  postCurationEnrichmentService: org::dataeng::ParseWidget;\n" +
            "  recordSources:\n" +
            "  [\n" +
            "    widget-file-source-ftp: {\n" +
            "      description: 'Widget FTP File source';\n" +
            "      status: Development;\n" +
            "      recordService: {\n" +
            "        parseService: org::dataeng::ParseWidget;\n" +
            "        transformService: org::dataeng::TransformWidget;\n" +
            "        acquisitionProtocol: File #{\n" +
            "          fileType: CSV;\n" +
            "          filePath: '/download/day-file.csv';\n" +
            "          headerLines: 0;\n" +
            "          connection: alloy::mastery::connection::FTPConnection;\n" +
            "        }#;\n" +
            "      };\n" +
            "      dataProvider: alloy::mastery::dataprovider::Bloomberg;\n" +
            "      trigger: Manual;\n" +
            "      sequentialData: true;\n" +
            "      stagedLoad: false;\n" +
            "      createPermitted: true;\n" +
            "      createBlockedException: false;\n" +
            "      allowFieldDelete: true;\n" +
            "    },\n" +
            "    widget-file-source-sftp: {\n" +
            "      description: 'Widget SFTP File source';\n" +
            "      status: Production;\n" +
            "      recordService: {\n" +
            "        transformService: org::dataeng::TransformWidget;\n" +
            "        acquisitionProtocol: File #{\n" +
            "          fileType: XML;\n" +
            "          filePath: '/download/day-file.xml';\n" +
            "          headerLines: 2;\n" +
            "          connection: alloy::mastery::connection::SFTPConnection;\n" +
            "        }#;\n" +
            "      };\n" +
            "      dataProvider: alloy::mastery::dataprovider::FCA;\n" +
            "      trigger: Cron #{\n" +
            "        minute: 30;\n" +
            "        hour: 22;\n" +
            "        timezone: 'UTC';\n" +
            "        frequency: Daily;\n" +
            "        days: [ Monday, Tuesday, Wednesday, Thursday, Friday ];\n" +
            "      }#;\n" +
            "      sequentialData: false;\n" +
            "      stagedLoad: true;\n" +
            "      createPermitted: false;\n" +
            "      createBlockedException: true;\n" +
            "    },\n" +
            "    widget-file-source-http: {\n" +
            "      description: 'Widget HTTP File Source.';\n" +
            "      status: Production;\n" +
            "      recordService: {\n" +
            "        parseService: org::dataeng::ParseWidget;\n" +
            "        transformService: org::dataeng::TransformWidget;\n" +
            "        acquisitionProtocol: File #{\n" +
            "          fileType: JSON;\n" +
            "          filePath: '/download/day-file.json';\n" +
            "          headerLines: 0;\n" +
            "          recordsKey: 'name';\n" +
            "          fileSplittingKeys: [ 'record', 'name' ];\n" +
            "          connection: alloy::mastery::connection::HTTPConnection;\n" +
            "        }#;\n" +
            "      };\n" +
            "      trigger: Manual;\n" +
            "      sequentialData: false;\n" +
            "      stagedLoad: true;\n" +
            "      createPermitted: false;\n" +
            "      createBlockedException: true;\n" +
            "    },\n" +
            "    widget-rest-source: {\n" +
            "      description: 'Widget Rest Source.';\n" +
            "      status: Production;\n" +
            "      recordService: {\n" +
            "        transformService: org::dataeng::TransformWidget;\n" +
            "        acquisitionProtocol: REST;\n" +
            "      };\n" +
            "      trigger: Manual;\n" +
            "      sequentialData: false;\n" +
            "      stagedLoad: true;\n" +
            "      createPermitted: false;\n" +
            "      createBlockedException: true;\n" +
            "    },\n" +
            "    widget-kafka-source: {\n" +
            "      description: 'Multiple partition source.';\n" +
            "      status: Production;\n" +
            "      recordService: {\n" +
            "        transformService: org::dataeng::TransformWidget;\n" +
            "        acquisitionProtocol: Kafka #{\n" +
            "          dataType: JSON;\n" +
            "          connection: alloy::mastery::connection::KafkaConnection;\n" +
            "        }#;\n" +
            "      };\n" +
            "      trigger: Manual;\n" +
            "      sequentialData: false;\n" +
            "      stagedLoad: true;\n" +
            "      createPermitted: false;\n" +
            "      createBlockedException: true;\n" +
            "    },\n" +
            "    widget-legend-service-source: {\n" +
            "      description: 'Widget Legend Service source.';\n" +
            "      status: Production;\n" +
            "      recordService: {\n" +
            "        acquisitionProtocol: org::dataeng::TransformWidget;\n" +
            "      };\n" +
            "      trigger: Manual;\n" +
            "      sequentialData: false;\n" +
            "      stagedLoad: true;\n" +
            "      createPermitted: false;\n" +
            "      createBlockedException: true;\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +

            // Data Provider
            "ExchangeDataProvider alloy::mastery::dataprovider::LSE;\n\n\n" +

            "RegulatorDataProvider alloy::mastery::dataprovider::FCA;\n\n\n" +

            "AggregatorDataProvider alloy::mastery::dataprovider::Bloomberg;\n\n\n" +

            "MasteryConnection alloy::mastery::connection::SFTPConnection\n" +
            "{\n" +
            "    specification: FTP #{\n" +
            "      host: 'site.url.com';\n" +
            "      port: 30;\n" +
            "      secure: true;\n" +
            "    }#;\n" +
            "}\n\n" +

            "MasteryConnection alloy::mastery::connection::FTPConnection\n" +
            "{\n" +
            "    specification: FTP #{\n" +
            "      host: 'site.url.com';\n" +
            "      port: 30;\n" +
            "    }#;\n" +
            "}\n\n" +

            "MasteryConnection alloy::mastery::connection::HTTPConnection\n" +
            "{\n" +
            "    specification: HTTP #{\n" +
            "      url: 'https://some.url.com';\n" +
            "      proxy: {\n" +
            "        host: 'proxy.url.com';\n" +
            "        port: 85;\n" +
            "      };\n" +
            "    }#;\n" +
            "}\n\n" +

            "MasteryConnection alloy::mastery::connection::KafkaConnection\n" +
            "{\n" +
            "    specification: Kafka #{\n" +
            "      topicName: 'my-topic-name';\n" +
            "      topicUrls: [\n" +
            "        'some.url.com:2100',\n" +
            "        'another.url.com:2100'\n" +
            "      ];\n" +
            "    }#;\n" +
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
            "{\n" +
            "  modelClass: org::dataeng::Widget;\n" +
            "  identityResolution: \n" +
            "  {\n" +
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
            "    widget-producer: {\n" +
            "      description: 'REST Acquisition source.';\n" +
            "      status: Development;\n" +
            "      recordService: {\n" +
            "        acquisitionProtocol: REST;\n" +
            "      };\n" +
            "      trigger: Manual;\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    //This is to ensure we can still compile the old Mastery spec which is now deprecated so that old projects do not break
    private static String DEPRECATED_MASTERY_MODEL = "###Pure\n" +
            "Class org::dataeng::Widget\n" +
            "{\n" +
            "  widgetId: String[0..1];\n" +
            "  description: String[0..1];\n" +
            "}\n\n" +
            MAPPING_AND_CONNECTION +
            "###Service\n" +
            "Service org::dataeng::ParseWidget\n" + WIDGET_SERVICE_BODY + "\n" +
            "Service org::dataeng::TransformWidget\n" + WIDGET_SERVICE_BODY + "\n" +
            "\n" +
            "###Mastery\n" + "MasterRecordDefinition alloy::mastery::WidgetMasterRecord" +
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
            "        }\n" +
            "      ]\n" +
            "  }\n" +
            "  recordSources:\n" +
            "  [\n" +
            "    widget-file-single-partition-14: {\n" +
            "      description: 'Single partition source.';\n" +
            "      status: Development;\n" +
            "      parseService: org::dataeng::ParseWidget;\n" +
            "      transformService: org::dataeng::TransformWidget;\n" +
            "      sequentialData: true;\n" +
            "      stagedLoad: false;\n" +
            "      createPermitted: true;\n" +
            "      createBlockedException: false;\n" +
            "      partitions:\n" +
            "      [\n" +
            "        partition-1-of-5: {\n" +
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
            "    widget-file: {\n" +
            "      description: 'Widget source.';\n" +
            "      status: Development;\n" +
            "      sequentialData: true;\n" +
            "      recordService: {\n" +
            "          acquisitionProtocol: REST;" +
            "       };\n" +
            "      trigger: Manual;" +
            "      stagedLoad: false;\n" +
            "      createPermitted: true;\n" +
            "      createBlockedException: false;\n" +
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

       assertDataProviders(model);
       assertConnections(model);

        // MasterRecord Definition modelClass
        Root_meta_pure_mastery_metamodel_MasterRecordDefinition masterRecordDefinition = (Root_meta_pure_mastery_metamodel_MasterRecordDefinition) packageableElement;
        assertEquals("Widget", masterRecordDefinition._modelClass()._name());

        // IdentityResolution
        Root_meta_pure_mastery_metamodel_identity_IdentityResolution idRes = masterRecordDefinition._identityResolution();
        assertNotNull(idRes);

        // enrichment service
        assertNotNull(masterRecordDefinition._postCurationEnrichmentService());

        // Resolution Queries
        Object[] queriesArray = idRes._resolutionQueries().toArray();
        assertEquals(1, ((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[0])._precedence());
        assertEquals("GeneratedPrimaryKey", ((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[0])._keyType()._name());
        assertResolutionQueryLambdas(((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[0])._queries().toList());

        assertEquals(2, ((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[1])._precedence());
        assertEquals("AlternateKey", ((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[1])._keyType()._name());
        assertResolutionQueryLambdas(((Root_meta_pure_mastery_metamodel_identity_ResolutionQuery) queriesArray[0])._queries().toList());

        //PrecedenceRule
        RichIterable<? extends Root_meta_pure_mastery_metamodel_precedence_PrecedenceRule> precedenceRules = masterRecordDefinition._precedenceRules();
        assertEquals(6, precedenceRules.size());
        ListIterate.forEachWithIndex(precedenceRules.toList(), (source, i) ->
        {
            if (i == 0)
            {
                assertTrue(source instanceof Root_meta_pure_mastery_metamodel_precedence_DeleteRule);
                //path
                List<? extends Root_meta_pure_mastery_metamodel_precedence_PropertyPath> paths =  source._paths().toList();
                assertEquals(1, paths.size());

                Root_meta_pure_mastery_metamodel_precedence_PropertyPath propertyPath = paths.get(0);
                //path property
                assertEquals("identifiers", propertyPath._property()._name());
                assertEquals("Widget", propertyPath._property()._owner()._name());
                //path filter
                assertEquals("true", getSimpleLambdaValue(propertyPath._filter()));

                //masterRecordFilter
                assertEquals("true", getSimpleLambdaValue(source._masterRecordFilter()));

                //scope
                List<? extends Root_meta_pure_mastery_metamodel_precedence_RuleScope> scopes = source._scope().toList();
                assertEquals(1, scopes.size());
                assertEquals("widget-rest-source", getRecordSourceIdAtIndex(scopes, 0));
            }
            else if (i == 1)
            {
                assertTrue(source instanceof Root_meta_pure_mastery_metamodel_precedence_CreateRule);
                //path
                List<? extends Root_meta_pure_mastery_metamodel_precedence_PropertyPath> paths =  source._paths().toList();
                assertEquals(2, paths.size());


                //path property
                Root_meta_pure_mastery_metamodel_precedence_PropertyPath firstPropertyPath = paths.get(0);
                assertEquals("identifiers", firstPropertyPath._property()._name());
                assertEquals("Widget", firstPropertyPath._property()._owner()._name());
                assertEquals("true", getSimpleLambdaValue(firstPropertyPath._filter()));

                Root_meta_pure_mastery_metamodel_precedence_PropertyPath secondPropertyPath = paths.get(1);
                assertEquals("identifierType", secondPropertyPath._property()._name());
                assertEquals("MilestonedIdentifier", secondPropertyPath._property()._owner()._name());
                assertEquals("true", getSimpleLambdaValue(secondPropertyPath._filter()));

                //masterRecordFilter
                Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl complexLambda = getComplexLambda(source._masterRecordFilter());
                List<? extends ValueSpecification> lambdaParameters = complexLambda._parametersValues().toList();

                assertEquals("Widget", getFunctionProperty(lambdaParameters.get(0))._owner()._name());
                assertEquals("widgetId", getFunctionProperty(lambdaParameters.get(0))._name());

                assertEquals("equal", complexLambda._functionName());

                assertEquals("1234", getInstanceValue(lambdaParameters.get(1)));

                //scope
                List<? extends Root_meta_pure_mastery_metamodel_precedence_RuleScope> scopes = source._scope().toList();
                assertEquals(2, scopes.size());
                assertEquals("widget-file-source-ftp", getRecordSourceIdAtIndex(scopes, 0));
                assertEquals("Aggregator", getDataProviderTypeAtIndex(scopes, 1));
            }
            else if (i == 2)
            {
                assertTrue(source instanceof Root_meta_pure_mastery_metamodel_precedence_ConditionalRule);
                //path
                List<? extends Root_meta_pure_mastery_metamodel_precedence_PropertyPath> paths =  source._paths().toList();
                assertEquals(2, paths.size());


                //path property
                Root_meta_pure_mastery_metamodel_precedence_PropertyPath firstPropertyPath = paths.get(0);
                assertEquals("identifiers", firstPropertyPath._property()._name());
                assertEquals("Widget", firstPropertyPath._property()._owner()._name());
                assertEquals("true", getSimpleLambdaValue(firstPropertyPath._filter()));

                Root_meta_pure_mastery_metamodel_precedence_PropertyPath secondPropertyPath = paths.get(1);
                assertEquals("identifierType", secondPropertyPath._property()._name());
                assertEquals("MilestonedIdentifier", secondPropertyPath._property()._owner()._name());
                assertEquals("true", getSimpleLambdaValue(secondPropertyPath._filter()));

                //masterRecordFilter
                assertEquals("true", getSimpleLambdaValue(source._masterRecordFilter()));

                //predicate
                LambdaFunction<?> lambda = ((Root_meta_pure_mastery_metamodel_precedence_ConditionalRule) source)._predicate();
                assertTrue(lambda instanceof Root_meta_pure_metamodel_function_LambdaFunction_Impl);
            }
            else if (i == 3)
            {
                assertTrue(source instanceof Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule);

                //precedence
                assertEquals(1, ((Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule) source)._precedence());

                //path
                List<? extends Root_meta_pure_mastery_metamodel_precedence_PropertyPath> paths =  source._paths().toList();
                assertEquals(1, paths.size());

                //path property
                Root_meta_pure_mastery_metamodel_precedence_PropertyPath firstPropertyPath = paths.get(0);
                assertEquals("identifiers", firstPropertyPath._property()._name());
                assertEquals("Widget", firstPropertyPath._property()._owner()._name());

                //path filter
                Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl complexLambda = getComplexLambda(firstPropertyPath._filter());
                List<? extends ValueSpecification> lambdaParameters = complexLambda._parametersValues().toList();

                assertEquals("MilestonedIdentifier", getFunctionProperty(lambdaParameters.get(0))._owner()._name());
                assertEquals("identifier", getFunctionProperty(lambdaParameters.get(0))._name());
                assertEquals("equal", complexLambda._functionName());
                assertEquals("XLON", getInstanceValue(lambdaParameters.get(1)));

                //masterRecordFilter
                assertEquals("true", getSimpleLambdaValue(source._masterRecordFilter()));

                //scope
                List<? extends Root_meta_pure_mastery_metamodel_precedence_RuleScope> scopes = source._scope().toList();
                assertEquals(1, scopes.size());
                assertEquals("widget-file-source-sftp", getRecordSourceIdAtIndex(scopes, 0));
            }
            else if (i == 4)
            {
                assertTrue(source instanceof Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule);
                //precedence
                assertEquals(2, ((Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule) source)._precedence());

                //path
                List<? extends Root_meta_pure_mastery_metamodel_precedence_PropertyPath> paths =  source._paths().toList();
                assertEquals(1, paths.size());

                //path property
                Root_meta_pure_mastery_metamodel_precedence_PropertyPath firstPropertyPath = paths.get(0);
                assertEquals("identifiers", firstPropertyPath._property()._name());
                assertEquals("Widget", firstPropertyPath._property()._owner()._name());

                //path filter
                Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl complexLambda = getComplexLambda(firstPropertyPath._filter());
                List<? extends ValueSpecification> lambdaParameters = complexLambda._parametersValues().toList();

                assertEquals("MilestonedIdentifier", getFunctionProperty(lambdaParameters.get(0))._owner()._name());
                assertEquals("identifier", getFunctionProperty(lambdaParameters.get(0))._name());
                assertEquals("equal", complexLambda._functionName());
                assertEquals("XLON", getInstanceValue(lambdaParameters.get(1)));

                //masterRecordFilter
                assertEquals("true", getSimpleLambdaValue(source._masterRecordFilter()));

                //scope
                List<? extends Root_meta_pure_mastery_metamodel_precedence_RuleScope> scopes = source._scope().toList();
                assertEquals(1, scopes.size());
                assertEquals("Exchange", getDataProviderTypeAtIndex(scopes, 0));

            }
            else if (i == 5)
            {
                assertTrue(source instanceof Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule);

                assertTrue(source instanceof Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule);
                //precedence
                assertEquals(2, ((Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule) source)._precedence());

                //path
                List<? extends Root_meta_pure_mastery_metamodel_precedence_PropertyPath> paths =  source._paths().toList();
                assertEquals(1, paths.size());

                Root_meta_pure_mastery_metamodel_precedence_PropertyPath propertyPath = paths.get(0);
                //path property
                assertEquals("identifiers", propertyPath._property()._name());
                assertEquals("Widget", propertyPath._property()._owner()._name());

                //path filter
                assertEquals("true", getSimpleLambdaValue(propertyPath._filter()));

                //masterRecordFilter
                assertEquals("true", getSimpleLambdaValue(source._masterRecordFilter()));

                //scope
                List<? extends Root_meta_pure_mastery_metamodel_precedence_RuleScope> scopes = source._scope().toList();
                assertEquals(1, scopes.size());
                assertEquals("widget-rest-source", getRecordSourceIdAtIndex(scopes, 0));
            }
        });

        //RecordSources
        assertEquals(6, masterRecordDefinition._sources().size());
        ListIterate.forEachWithIndex(masterRecordDefinition._sources().toList(), (source, i) ->
        {
            if (i == 0)
            {
                assertEquals("widget-file-source-ftp", source._id());
                assertEquals("Development", source._status().getName());
                assertEquals(true, source._sequentialData());
                assertEquals(false, source._stagedLoad());
                assertEquals(true, source._createPermitted());
                assertEquals(false, source._createBlockedException());

                assertTrue(source._allowFieldDelete());
                assertTrue(source._trigger() instanceof Root_meta_pure_mastery_metamodel_trigger_ManualTrigger);
                assertNotNull(source._recordService()._parseService());
                assertNotNull(source._recordService()._transformService());

                Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol acquisitionProtocol = (Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol) source._recordService()._acquisitionProtocol();
                assertEquals(acquisitionProtocol._filePath(), "/download/day-file.csv");
                assertEquals(acquisitionProtocol._headerLines(), 0);
                assertNotNull(acquisitionProtocol._fileType());
                assertTrue(acquisitionProtocol._connection() instanceof Root_meta_pure_mastery_metamodel_connection_FTPConnection);

                assertNotNull(source._dataProvider());

            }
            else if (i == 1)
            {
                assertEquals("widget-file-source-sftp", source._id());
                assertEquals("Production", source._status().getName());
                assertEquals(false, source._sequentialData());
                assertEquals(true, source._stagedLoad());
                assertEquals(false, source._createPermitted());
                assertEquals(true, source._createBlockedException());

                Root_meta_pure_mastery_metamodel_trigger_CronTrigger cronTrigger = (Root_meta_pure_mastery_metamodel_trigger_CronTrigger) source._trigger();
                assertEquals(30, cronTrigger._minute());
                assertEquals(22, cronTrigger._hour());
                assertEquals("UTC", cronTrigger._timezone());
                assertEquals(5, cronTrigger._days().size());

                assertNotNull(source._recordService()._transformService());

                Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol acquisitionProtocol = (Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol) source._recordService()._acquisitionProtocol();
                assertEquals(acquisitionProtocol._filePath(), "/download/day-file.xml");
                assertEquals(acquisitionProtocol._headerLines(), 2);
                assertNotNull(acquisitionProtocol._fileType());
                assertTrue(acquisitionProtocol._connection() instanceof Root_meta_pure_mastery_metamodel_connection_FTPConnection);

                assertNotNull(source._dataProvider());
            }
            else if (i == 2)
            {
                assertEquals("widget-file-source-http", source._id());
                assertEquals("Production", source._status().getName());
                assertEquals(false, source._sequentialData());
                assertEquals(true, source._stagedLoad());
                assertEquals(false, source._createPermitted());
                assertEquals(true, source._createBlockedException());

                assertTrue(source._trigger() instanceof Root_meta_pure_mastery_metamodel_trigger_ManualTrigger);
                assertNotNull(source._recordService()._transformService());
                assertNotNull(source._recordService()._parseService());


                Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol acquisitionProtocol = (Root_meta_pure_mastery_metamodel_acquisition_FileAcquisitionProtocol) source._recordService()._acquisitionProtocol();
                assertEquals(acquisitionProtocol._filePath(), "/download/day-file.json");
                assertEquals(acquisitionProtocol._headerLines(), 0);
                assertEquals(acquisitionProtocol._recordsKey(), "name");
                assertNotNull(acquisitionProtocol._fileType());
                assertEquals(Lists.newArrayList("record", "name"), acquisitionProtocol._fileSplittingKeys().toList());
                assertTrue(acquisitionProtocol._connection() instanceof Root_meta_pure_mastery_metamodel_connection_HTTPConnection);
            }
            else if (i == 3)
            {
                assertEquals("widget-rest-source", source._id());
                assertEquals("Production", source._status().getName());
                assertEquals(false, source._sequentialData());
                assertEquals(true, source._stagedLoad());
                assertEquals(false, source._createPermitted());
                assertEquals(true, source._createBlockedException());


                assertNotNull(source._recordService()._transformService());
                assertTrue(source._trigger() instanceof Root_meta_pure_mastery_metamodel_trigger_ManualTrigger);
                assertTrue(source._recordService()._acquisitionProtocol() instanceof Root_meta_pure_mastery_metamodel_acquisition_RestAcquisitionProtocol);
            }
            else if (i == 4)
            {
                assertEquals("widget-kafka-source", source._id());
                assertEquals("Production", source._status().getName());
                assertEquals(false, source._sequentialData());
                assertEquals(true, source._stagedLoad());
                assertEquals(false, source._createPermitted());
                assertEquals(true, source._createBlockedException());

                assertTrue(source._trigger() instanceof Root_meta_pure_mastery_metamodel_trigger_ManualTrigger);
                assertNotNull(source._recordService()._transformService());

                Root_meta_pure_mastery_metamodel_acquisition_KafkaAcquisitionProtocol acquisitionProtocol = (Root_meta_pure_mastery_metamodel_acquisition_KafkaAcquisitionProtocol) source._recordService()._acquisitionProtocol();
                assertNotNull(acquisitionProtocol._dataType());
                assertTrue(acquisitionProtocol._connection() instanceof Root_meta_pure_mastery_metamodel_connection_KafkaConnection);
            }
            else if (i == 5)
            {
                assertEquals("widget-legend-service-source", source._id());
                assertEquals("Production", source._status().getName());
                assertEquals(false, source._sequentialData());
                assertEquals(true, source._stagedLoad());
                assertEquals(false, source._createPermitted());
                assertEquals(true, source._createBlockedException());

                assertTrue(source._trigger() instanceof Root_meta_pure_mastery_metamodel_trigger_ManualTrigger);

                Root_meta_pure_mastery_metamodel_acquisition_LegendServiceAcquisitionProtocol acquisitionProtocol = (Root_meta_pure_mastery_metamodel_acquisition_LegendServiceAcquisitionProtocol) source._recordService()._acquisitionProtocol();
                assertNotNull(acquisitionProtocol._service());
            }

        });
    }

    @Test
    public void testMasteryMinimumCorrectModel()
    {
        Pair<PureModelContextData, PureModel> result = test(MINIMUM_CORRECT_MASTERY_MODEL);
        PureModel model = result.getTwo();

        PackageableElement packageableElement = model.getPackageableElement("alloy::mastery::WidgetMasterRecord");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_mastery_metamodel_MasterRecordDefinition);
        Root_meta_pure_mastery_metamodel_MasterRecordDefinition masterRecordDefinition = (Root_meta_pure_mastery_metamodel_MasterRecordDefinition) packageableElement;
        assertEquals("Widget", masterRecordDefinition._modelClass()._name());
    }

    @Test
    public void testMasteryDeprecatedModelCanStillCompile()
    {
        Pair<PureModelContextData, PureModel> result = test(DEPRECATED_MASTERY_MODEL);
        PureModel model = result.getTwo();

        PackageableElement packageableElement = model.getPackageableElement("alloy::mastery::WidgetMasterRecord");
        assertNotNull(packageableElement);
        assertTrue(packageableElement instanceof Root_meta_pure_mastery_metamodel_MasterRecordDefinition);
        Root_meta_pure_mastery_metamodel_MasterRecordDefinition masterRecordDefinition = (Root_meta_pure_mastery_metamodel_MasterRecordDefinition) packageableElement;
        assertEquals("Widget", masterRecordDefinition._modelClass()._name());
    }

    private void assertDataProviders(PureModel model)
    {
        PackageableElement lseDataProvider = model.getPackageableElement("alloy::mastery::dataprovider::LSE");
        PackageableElement fcaDataProvider = model.getPackageableElement("alloy::mastery::dataprovider::FCA");
        PackageableElement bloombergDataProvider = model.getPackageableElement("alloy::mastery::dataprovider::Bloomberg");

        assertNotNull(lseDataProvider);
        assertNotNull(fcaDataProvider);
        assertNotNull(bloombergDataProvider);

        assertTrue(lseDataProvider instanceof Root_meta_pure_mastery_metamodel_DataProvider);
        Root_meta_pure_mastery_metamodel_DataProvider dataProvider = (Root_meta_pure_mastery_metamodel_DataProvider) lseDataProvider;
        assertEquals(dataProvider._dataProviderId(), "alloy_mastery_dataprovider_LSE");
        assertEquals(dataProvider._dataProviderType(), "Exchange");

        assertTrue(fcaDataProvider instanceof Root_meta_pure_mastery_metamodel_DataProvider);
        dataProvider = (Root_meta_pure_mastery_metamodel_DataProvider) fcaDataProvider;
        assertEquals(dataProvider._dataProviderId(), "alloy_mastery_dataprovider_FCA");
        assertEquals(dataProvider._dataProviderType(), "Regulator");

        assertTrue(bloombergDataProvider instanceof Root_meta_pure_mastery_metamodel_DataProvider);
        dataProvider = (Root_meta_pure_mastery_metamodel_DataProvider) bloombergDataProvider;
        assertEquals(dataProvider._dataProviderId(), "alloy_mastery_dataprovider_Bloomberg");
        assertEquals(dataProvider._dataProviderType(), "Aggregator");
    }

    private void assertConnections(PureModel model)
    {
        PackageableElement httpConnection = model.getPackageableElement("alloy::mastery::connection::HTTPConnection");
        PackageableElement ftpConnection = model.getPackageableElement("alloy::mastery::connection::FTPConnection");
        PackageableElement sftpConnection = model.getPackageableElement("alloy::mastery::connection::SFTPConnection");
        PackageableElement kafkaConnection = model.getPackageableElement("alloy::mastery::connection::KafkaConnection");

        assertTrue(httpConnection instanceof Root_meta_pure_mastery_metamodel_connection_HTTPConnection);
        Root_meta_pure_mastery_metamodel_connection_HTTPConnection httpConnection1 = (Root_meta_pure_mastery_metamodel_connection_HTTPConnection) httpConnection;
        assertEquals(httpConnection1._url(), "https://some.url.com");
        assertEquals(httpConnection1._proxy()._host(), "proxy.url.com");
        assertEquals(httpConnection1._proxy()._port(), 85);


        assertTrue(ftpConnection instanceof Root_meta_pure_mastery_metamodel_connection_FTPConnection);
        Root_meta_pure_mastery_metamodel_connection_FTPConnection ftpConnection1 = (Root_meta_pure_mastery_metamodel_connection_FTPConnection) ftpConnection;
        assertEquals(ftpConnection1._host(), "site.url.com");
        assertEquals(ftpConnection1._port(), 30);
        assertNull(ftpConnection1._secure());

        assertTrue(sftpConnection instanceof Root_meta_pure_mastery_metamodel_connection_FTPConnection);
        Root_meta_pure_mastery_metamodel_connection_FTPConnection sftpConnection1 = (Root_meta_pure_mastery_metamodel_connection_FTPConnection) sftpConnection;
        assertEquals(sftpConnection1._host(), "site.url.com");
        assertEquals(sftpConnection1._port(), 30);
        assertEquals(sftpConnection1._secure(), true);

        assertTrue(kafkaConnection instanceof Root_meta_pure_mastery_metamodel_connection_KafkaConnection);
        Root_meta_pure_mastery_metamodel_connection_KafkaConnection kafkaConnection1 = (Root_meta_pure_mastery_metamodel_connection_KafkaConnection) kafkaConnection;
        assertEquals(kafkaConnection1._topicName(), "my-topic-name");
        assertEquals(kafkaConnection1._topicUrls(), newArrayList("some.url.com:2100", "another.url.com:2100"));
    }

    private String getSimpleLambdaValue(LambdaFunction<?> lambdaFunction)
    {
        return getInstanceValue(lambdaFunction._expressionSequence().toList().get(0));
    }

    private String getInstanceValue(CoreInstance coreInstance)
    {
        return ((Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl) coreInstance)._values().toList().get(0).toString();
    }

    private Root_meta_pure_metamodel_function_property_Property_Impl getFunctionProperty(CoreInstance coreInstance)
    {
        return (Root_meta_pure_metamodel_function_property_Property_Impl) ((Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl) coreInstance)._func();
    }

    private Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl getComplexLambda(LambdaFunction<?> lambdaFunction)
    {
        return ((Root_meta_pure_metamodel_valuespecification_SimpleFunctionExpression_Impl) lambdaFunction._expressionSequence().toList().get(0));
    }

    private String getRecordSourceIdAtIndex(List<? extends Root_meta_pure_mastery_metamodel_precedence_RuleScope> scopes, int index)
    {
        return ((Root_meta_pure_mastery_metamodel_precedence_RecordSourceScope) scopes.get(index))._recordSourceId();
    }

    private String getDataProviderTypeAtIndex(List<? extends Root_meta_pure_mastery_metamodel_precedence_RuleScope> scopes, int index)
    {
        return ((Root_meta_pure_mastery_metamodel_precedence_DataProviderTypeScope) scopes.get(index))._dataProviderType();
    }

    private void assertResolutionQueryLambdas(Iterable<?> list)
    {
        list.forEach(resQuery -> assertTrue(resQuery instanceof Root_meta_pure_metamodel_function_LambdaFunction_Impl));
    }

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return DUPLICATE_ELEMENT_MODEL;
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [8:1-35:1]: Duplicated element 'org::dataeng::Widget'";
    }
}