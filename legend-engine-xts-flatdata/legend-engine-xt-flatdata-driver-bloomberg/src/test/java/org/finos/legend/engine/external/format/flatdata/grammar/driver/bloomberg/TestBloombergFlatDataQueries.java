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

package org.finos.legend.engine.external.format.flatdata.grammar.driver.bloomberg;

import java.util.Collections;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.generated.core_external_format_flatdata_externalFormatContract;
import org.finos.legend.pure.generated.core_external_format_flatdata_java_platform_binding_legendJavaPlatformBinding_descriptor;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestBloombergFlatDataQueries extends TestExternalFormatQueries
{
    @BeforeClass
    public static void setup()
    {
        ExecutionSupport executionSupport = Compiler
                .compile(PureModelContextData.newPureModelContextData(), null, IdentityFactoryProvider.getInstance().getAnonymousIdentity()).getExecutionSupport();
        formatExtensions = Collections
                .singletonList(core_external_format_flatdata_externalFormatContract.Root_meta_external_format_flatdata_extension_flatDataFormatExtension__Extension_1_(executionSupport));
        formatDescriptors = Collections.singletonList(core_external_format_flatdata_java_platform_binding_legendJavaPlatformBinding_descriptor.Root_meta_external_format_flatdata_executionPlan_platformBinding_legendJava_flatDataJavaBindingDescriptor__ExternalFormatLegendJavaPlatformBindingDescriptor_1_(executionSupport));
    }

    @Test
    public void testbbgSingle()
    {
        String modelGrammar = bbgModelSingle();
        PureModelContextData generated = PureGrammarParser.newInstance().parseModel(modelGrammar);
        String result = runTest(generated,
                "data: Byte[*]|demo::externalFormat::flatdata::bloomberg::singleFile::models::FltCpnHistRecordRecord->internalize(\n"
                        + "  demo::externalFormat::flatdata::bloomberg::singleFile::binding::BloombergSingleFileBinding,\n"
                        + "  $data\n"
                        + ")->serialize(\n"
                        + "  #{\n"
                        + "    demo::externalFormat::flatdata::bloomberg::singleFile::models::FltCpnHistRecordRecord{\n"
                        + "      SECURITY,\n"
                        + "      ERROR_COUNT,\n"
                        + "      FIELD_COUNT,\n"
                        + "      ID_BB_UNIQUE,\n"
                        + "      ID_BB_COMPANY,\n"
                        + "      ID_BB_SECURITY,\n"
                        + "      ID_BB_GLOBAL,\n"
                        + "      ID_ISIN,\n"
                        + "      ID_BB_SEC_NUM_DES,\n"
                        + "      FEED_SOURCE,\n"
                        + "      FLT_CPN_HIST_DT,\n"
                        + "      FLT_CPN_HIST_RT\n"
                        + "    }\n"
                        + "  }#\n"
                        + ")",
                Maps.mutable.with("data", resource("bloomberg/bbg_multi_file")));
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":[{\"SECURITY\":\"EC5111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111111\",\"ID_BB_COMPANY\":111111,\"ID_BB_SECURITY\":1111111,\"ID_BB_GLOBAL\":\"XXX111111XX1\",\"ID_ISIN\":\"XS0111111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 XXXX\",\"FEED_SOURCE\":null,\"FLT_CPN_HIST_DT\":\"2001-02-20\",\"FLT_CPN_HIST_RT\":null},{\"SECURITY\":\"EC5111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111111\",\"ID_BB_COMPANY\":111111,\"ID_BB_SECURITY\":1111111,\"ID_BB_GLOBAL\":\"XXX111111XX1\",\"ID_ISIN\":\"XS0111111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 XXXX\",\"FEED_SOURCE\":null,\"FLT_CPN_HIST_DT\":\"2001-02-20\",\"FLT_CPN_HIST_RT\":null},{\"SECURITY\":\"EC5111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111111\",\"ID_BB_COMPANY\":111111,\"ID_BB_SECURITY\":1111111,\"ID_BB_GLOBAL\":\"XXX111111XX1\",\"ID_ISIN\":\"XS0111111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 XXXX\",\"FEED_SOURCE\":null,\"FLT_CPN_HIST_DT\":\"2001-02-20\",\"FLT_CPN_HIST_RT\":3.6},{\"SECURITY\":\"EC5111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111111\",\"ID_BB_COMPANY\":111111,\"ID_BB_SECURITY\":1111111,\"ID_BB_GLOBAL\":\"XXX111111XX1\",\"ID_ISIN\":\"XS0111111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 XXXX\",\"FEED_SOURCE\":null,\"FLT_CPN_HIST_DT\":\"2001-03-25\",\"FLT_CPN_HIST_RT\":3.6},{\"SECURITY\":\"EJ8111112 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEJ8111112\",\"ID_BB_COMPANY\":111112,\"ID_BB_SECURITY\":11111112,\"ID_BB_GLOBAL\":\"XXX111X2X222\",\"ID_ISIN\":\"XS0011111112\",\"ID_BB_SEC_NUM_DES\":\"XXXXX V0 09/25/21 0000\",\"FEED_SOURCE\":\"XXX\",\"FLT_CPN_HIST_DT\":\"2013-09-25\",\"FLT_CPN_HIST_RT\":4.0}]}", result);
    }

    @Test
    public void testbbgMultiple()
    {
        String modelGrammar = bbgModelMultiple();
        PureModelContextData generated = PureGrammarParser.newInstance().parseModel(modelGrammar);

        String result = runTest(generated,
                "data: Byte[*]|demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile->internalize(\n"
                        + "  demo::externalFormat::flatdata::bloomberg::bulkFiles::binding::BloombergBulkFileBinding,\n"
                        + "  $data\n"
                        + ")->graphFetch(" + bbgBulkTree()
                        + ")->serialize(" + bbgBulkTree() + ")",
                Maps.mutable.with("data", resource("bloomberg/bbg_multi_file")));
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"fltCpnHist\":[{\"SECURITY\":\"EC5111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111111\",\"ID_BB_COMPANY\":111111,\"ID_BB_SECURITY\":1111111,\"ID_BB_GLOBAL\":\"XXX111111XX1\",\"ID_ISIN\":\"XS0111111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 XXXX\",\"FEED_SOURCE\":null,\"FLT_CPN_HIST_DT\":\"2001-02-20\",\"FLT_CPN_HIST_RT\":null},{\"SECURITY\":\"EC5111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111111\",\"ID_BB_COMPANY\":111111,\"ID_BB_SECURITY\":1111111,\"ID_BB_GLOBAL\":\"XXX111111XX1\",\"ID_ISIN\":\"XS0111111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 XXXX\",\"FEED_SOURCE\":null,\"FLT_CPN_HIST_DT\":\"2001-02-20\",\"FLT_CPN_HIST_RT\":null},{\"SECURITY\":\"EC5111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111111\",\"ID_BB_COMPANY\":111111,\"ID_BB_SECURITY\":1111111,\"ID_BB_GLOBAL\":\"XXX111111XX1\",\"ID_ISIN\":\"XS0111111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 XXXX\",\"FEED_SOURCE\":null,\"FLT_CPN_HIST_DT\":\"2001-02-20\",\"FLT_CPN_HIST_RT\":3.6},{\"SECURITY\":\"EC5111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111111\",\"ID_BB_COMPANY\":111111,\"ID_BB_SECURITY\":1111111,\"ID_BB_GLOBAL\":\"XXX111111XX1\",\"ID_ISIN\":\"XS0111111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 XXXX\",\"FEED_SOURCE\":null,\"FLT_CPN_HIST_DT\":\"2001-03-25\",\"FLT_CPN_HIST_RT\":3.6},{\"SECURITY\":\"EJ8111112 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEJ8111112\",\"ID_BB_COMPANY\":111112,\"ID_BB_SECURITY\":11111112,\"ID_BB_GLOBAL\":\"XXX111X2X222\",\"ID_ISIN\":\"XS0011111112\",\"ID_BB_SEC_NUM_DES\":\"XXXXX V0 09/25/21 0000\",\"FEED_SOURCE\":\"XXX\",\"FLT_CPN_HIST_DT\":\"2013-09-25\",\"FLT_CPN_HIST_RT\":4.0}],\"fltCpnHistMetaRecord\":[{\"programname\":\"getdata\",\"dateformat\":\"yyyymmdd\",\"region\":\"euro\",\"type\":\"dif\",\"datarecords\":3,\"timestarted\":\"2021-09-26T17:57:42.000\",\"timefinished\":\"2021-09-26T17:57:43.000\"}],\"callSchedule\":[{\"SECURITY\":\"EC4111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC4111111\",\"ID_BB_COMPANY\":411111,\"ID_BB_SECURITY\":411112,\"ID_BB_GLOBAL\":\"XXX41111XXX1\",\"ID_ISIN\":\"XS0411111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 AAAA\",\"FEED_SOURCE\":null,\"CALL_SCHEDULE_DT\":\"2003-09-25\",\"CALL_SCHEDULE_PCT\":100.0},{\"SECURITY\":\"EC4111111 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC4111111\",\"ID_BB_COMPANY\":411111,\"ID_BB_SECURITY\":411112,\"ID_BB_GLOBAL\":\"XXX41111XXX1\",\"ID_ISIN\":\"XS0411111111\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 AAAA\",\"FEED_SOURCE\":null,\"CALL_SCHEDULE_DT\":\"2004-03-25\",\"CALL_SCHEDULE_PCT\":100.0},{\"SECURITY\":\"EC5111113 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111113\",\"ID_BB_COMPANY\":511113,\"ID_BB_SECURITY\":5111113,\"ID_BB_GLOBAL\":\"XXX511113XX1\",\"ID_ISIN\":\"XS0511111113\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 BBBB\",\"FEED_SOURCE\":null,\"CALL_SCHEDULE_DT\":\"2006-09-25\",\"CALL_SCHEDULE_PCT\":100.0},{\"SECURITY\":\"EC5111113 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111113\",\"ID_BB_COMPANY\":511113,\"ID_BB_SECURITY\":5111113,\"ID_BB_GLOBAL\":\"XXX511113XX1\",\"ID_ISIN\":\"XS0511111113\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 BBBB\",\"FEED_SOURCE\":null,\"CALL_SCHEDULE_DT\":\"2007-03-25\",\"CALL_SCHEDULE_PCT\":100.0},{\"SECURITY\":\"EC5111113 Corp\",\"ERROR_COUNT\":-1,\"FIELD_COUNT\":9,\"ID_BB_UNIQUE\":\"XXEC5111113\",\"ID_BB_COMPANY\":511113,\"ID_BB_SECURITY\":5111113,\"ID_BB_GLOBAL\":\"XXX511113XX1\",\"ID_ISIN\":\"XS0511111113\",\"ID_BB_SEC_NUM_DES\":\"XXXX V0 09/25/21 BBBB\",\"FEED_SOURCE\":null,\"CALL_SCHEDULE_DT\":\"2007-09-25\",\"CALL_SCHEDULE_PCT\":100.0}],\"callScheduleMetaRecord\":[{\"programname\":\"getdata\",\"dateformat\":\"yyyymmdd\",\"region\":\"euro\",\"type\":\"dif\",\"datarecords\":5,\"timestarted\":\"2021-09-26T17:57:43.000\",\"timefinished\":\"2021-09-26T17:57:44.000\"}]}}", result);
    }

    @Test
    public void testbbgMultipleCheckedNotSupported()
    {
        String modelGrammar = bbgModelMultiple();
        PureModelContextData generated = PureGrammarParser.newInstance().parseModel(modelGrammar);

        try
        {
            runTest(generated,
                    "data: Byte[*]|demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile->internalize(\n"
                            + "  demo::externalFormat::flatdata::bloomberg::bulkFiles::binding::BloombergBulkFileBinding,\n"
                            + "  $data\n"
                            + ")->graphFetchChecked(" + bbgBulkTree()
                            + ")->serialize(" + bbgBulkTree() + ")",
                    Maps.mutable.with("data", resource("bloomberg/bbg_multi_file")));
            Assert.fail("Exception expected");
        }
        catch (Exception e)
        {
            Assert.assertEquals("Assert failure at (resource:/core_external_format_flatdata_java_platform_binding/legendJavaPlatformBinding/internalize.pure line:94 column:4), \"Querying on schema class with checked functionality is not supported !!\"", e.getMessage());
        }
    }

    private String bbgModelSingle()
    {
        return "###Pure\n"
                + "Class demo::externalFormat::flatdata::bloomberg::singleFile::models::FltCpnHistRecordRecord\n"
                + "{\n"
                + "  SECURITY: String[1];\n"
                + "  ERROR_COUNT: Integer[1];\n"
                + "  FIELD_COUNT: Integer[1];\n"
                + "  ID_BB_UNIQUE: String[1];\n"
                + "  ID_BB_COMPANY: Integer[1];\n"
                + "  ID_BB_SECURITY: Integer[1];\n"
                + "  ID_BB_GLOBAL: String[1];\n"
                + "  ID_ISIN: String[0..1];\n"
                + "  ID_BB_SEC_NUM_DES: String[0..1];\n"
                + "  FEED_SOURCE: String[0..1];\n"
                + "  FLT_CPN_HIST_DT: StrictDate[1];\n"
                + "  FLT_CPN_HIST_RT: Float[0..1];\n"
                + "}\n"
                + "\n"
                + "\n"
                + "###ExternalFormat\n"
                + "Binding demo::externalFormat::flatdata::bloomberg::singleFile::binding::BloombergSingleFileBinding\n"
                + "{\n"
                + "  schemaSet: demo::externalFormat::flatdata::bloomberg::singleFile::schemaSet::BloombergSingleFileSchemaSet;\n"
                + "  contentType: 'application/x.flatdata';\n"
                + "  modelIncludes: [\n"
                + "    demo::externalFormat::flatdata::bloomberg::singleFile::models::FltCpnHistRecordRecord\n"
                + "  ];\n"
                + "}\n"
                + "\n"
                + "SchemaSet demo::externalFormat::flatdata::bloomberg::singleFile::schemaSet::BloombergSingleFileSchemaSet\n"
                + "{\n"
                + "  format: FlatData;\n"
                + "  schemas: [\n"
                + "    {\n"
                + "      id: 'default.flatdata';\n"
                + "      content: 'section FltCpnHistRecordRecord: BloombergData\\n{\\n  filter: \\'DATA=FLT_CPN_HIST\\';\\n\\n  Record\\n  {\\n    SECURITY: STRING;\\n    ERROR_COUNT: INTEGER;\\n    FIELD_COUNT: INTEGER;\\n    ID_BB_UNIQUE: STRING;\\n    ID_BB_COMPANY: INTEGER;\\n    ID_BB_SECURITY: INTEGER;\\n    ID_BB_GLOBAL: STRING;\\n    ID_ISIN: STRING(optional);\\n    ID_BB_SEC_NUM_DES: STRING(optional);\\n    FEED_SOURCE: STRING(optional);\\n    FLT_CPN_HIST_DT: DATE;\\n    FLT_CPN_HIST_RT: DECIMAL(optional);\\n  }\\n}';\n"
                + "    }\n"
                + "  ];\n"
                + "}\n";
    }

    private String bbgModelMultiple()
    {
        return "###Pure\n"
                + "Class demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile\n"
                + "{\n"
                + "}\n"
                + "\n"
                + "Class demo::externalFormat::flatdata::bloomberg::bulkFiles::models::FltCpnHistRecordRecord\n"
                + "{\n"
                + "  SECURITY: String[1];\n"
                + "  ERROR_COUNT: Integer[1];\n"
                + "  FIELD_COUNT: Integer[1];\n"
                + "  ID_BB_UNIQUE: String[1];\n"
                + "  ID_BB_COMPANY: Integer[1];\n"
                + "  ID_BB_SECURITY: Integer[1];\n"
                + "  ID_BB_GLOBAL: String[1];\n"
                + "  ID_ISIN: String[0..1];\n"
                + "  ID_BB_SEC_NUM_DES: String[0..1];\n"
                + "  FEED_SOURCE: String[0..1];\n"
                + "  FLT_CPN_HIST_DT: StrictDate[1];\n"
                + "  FLT_CPN_HIST_RT: Float[0..1];\n"
                + "}\n"
                + "\n"
                + "Class demo::externalFormat::flatdata::bloomberg::bulkFiles::models::FltCpnHistMetaRecord\n"
                + "{\n"
                + "  programname: String[1];\n"
                + "  dateformat: String[1];\n"
                + "  region: String[1];\n"
                + "  type: String[1];\n"
                + "  datarecords: Integer[1];\n"
                + "  timestarted: DateTime[1];\n"
                + "  timefinished: DateTime[1];\n"
                + "}\n"
                + "\n"
                + "Class demo::externalFormat::flatdata::bloomberg::bulkFiles::models::CallScheduleRecord\n"
                + "{\n"
                + "  SECURITY: String[1];\n"
                + "  ERROR_COUNT: Integer[1];\n"
                + "  FIELD_COUNT: Integer[1];\n"
                + "  ID_BB_UNIQUE: String[1];\n"
                + "  ID_BB_COMPANY: Integer[1];\n"
                + "  ID_BB_SECURITY: Integer[1];\n"
                + "  ID_BB_GLOBAL: String[1];\n"
                + "  ID_ISIN: String[0..1];\n"
                + "  ID_BB_SEC_NUM_DES: String[0..1];\n"
                + "  FEED_SOURCE: String[0..1];\n"
                + "  CALL_SCHEDULE_DT: StrictDate[1];\n"
                + "  CALL_SCHEDULE_PCT: Float[1];\n"
                + "}\n"
                + "\n"
                + "Class demo::externalFormat::flatdata::bloomberg::bulkFiles::models::CallScheduleMetaRecord\n"
                + "{\n"
                + "  programname: String[1];\n"
                + "  dateformat: String[1];\n"
                + "  region: String[1];\n"
                + "  type: String[1];\n"
                + "  datarecords: Integer[1];\n"
                + "  timestarted: DateTime[1];\n"
                + "  timefinished: DateTime[1];\n"
                + "}\n"
                + "\n"
                + "Association demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile_CallScheduleRecord\n"
                + "{\n"
                + "  bloombergBulkFile: demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile[1];\n"
                + "  callSchedule: demo::externalFormat::flatdata::bloomberg::bulkFiles::models::CallScheduleRecord[*];\n"
                + "}\n"
                + "Association demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile_CallScheduleMetaRecord\n"
                + "{\n"
                + "  bloombergBulkFile: demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile[1];\n"
                + "  callScheduleMetaRecord: demo::externalFormat::flatdata::bloomberg::bulkFiles::models::CallScheduleMetaRecord[*];\n"
                + "}\n"
                + "\n"
                + "\n"
                + "Association demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile_FltCpnHistRecord\n"
                + "{\n"
                + "  bloombergBulkFile: demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile[1];\n"
                + "  fltCpnHist: demo::externalFormat::flatdata::bloomberg::bulkFiles::models::FltCpnHistRecordRecord[*];\n"
                + "}\n"
                + "\n"
                + "Association demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile_FltCpnHistMetaRecord\n"
                + "{\n"
                + "  bloombergBulkFile: demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile[1];\n"
                + "  fltCpnHistMetaRecord: demo::externalFormat::flatdata::bloomberg::bulkFiles::models::FltCpnHistMetaRecord[*];\n"
                + "}\n"
                + "\n"
                + "###ExternalFormat\n"
                + "Binding demo::externalFormat::flatdata::bloomberg::bulkFiles::binding::BloombergBulkFileBinding\n"
                + "{\n"
                + "  schemaSet: demo::externalFormat::flatdata::bloomberg::singleFile::schemaSet::BloombergBulkFileSchemaSet;\n"
                + "  contentType: 'application/x.flatdata';\n"
                + "  modelIncludes: [\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::FltCpnHistRecordRecord,\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::FltCpnHistMetaRecord,\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::CallScheduleRecord,\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::CallScheduleMetaRecord,\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile,\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile_CallScheduleRecord,\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile_CallScheduleMetaRecord,\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile_FltCpnHistRecord,\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile_FltCpnHistMetaRecord\n"
                + "  ];\n"
                + "}\n"
                + "\n"
                + "SchemaSet demo::externalFormat::flatdata::bloomberg::singleFile::schemaSet::BloombergBulkFileSchemaSet\n"
                + "{\n"
                + "  format: FlatData;\n"
                + "  schemas: [\n"
                + "    {\n"
                + "      id: 'default.flatdata';\n"
                + "      content: 'section fltCpnHist: BloombergData\\n{\\n  filter: \\'DATA=FLT_CPN_HIST\\';\\n\\n  Record\\n  {\\n    SECURITY: STRING;\\n    ERROR_COUNT: INTEGER;\\n    FIELD_COUNT: INTEGER;\\n    ID_BB_UNIQUE: STRING;\\n    ID_BB_COMPANY: INTEGER;\\n    ID_BB_SECURITY: INTEGER;\\n    ID_BB_GLOBAL: STRING;\\n    ID_ISIN: STRING(optional);\\n    ID_BB_SEC_NUM_DES: STRING(optional);\\n    FEED_SOURCE: STRING(optional);\\n    FLT_CPN_HIST_DT: DATE;\\n    FLT_CPN_HIST_RT: DECIMAL(optional);\\n  }\\n}\\nsection fltCpnHistMetaRecord: BloombergMetadata\\n{\\n  Record\\n  {\\n    PROGRAMNAME  : STRING;\\n    DATEFORMAT   : STRING;\\n    REGION       : STRING;\\n    TYPE         : STRING;\\n    DATARECORDS  : INTEGER;\\n    TIMESTARTED  : DATETIME(format=\\'EEE MMM d HH:mm:ss zz yyyy\\');\\n    TIMEFINISHED : DATETIME(format=\\'EEE MMM d HH:mm:ss zz yyyy\\');\\n  }\\n}\\nsection callSchedule: BloombergData\\n{\\n  filter: \\'DATA=CALL_SCHEDULE\\';\\n\\n  Record\\n  {\\n    SECURITY: STRING;\\n    ERROR_COUNT: INTEGER;\\n    FIELD_COUNT: INTEGER;\\n    ID_BB_UNIQUE: STRING;\\n    ID_BB_COMPANY: INTEGER;\\n    ID_BB_SECURITY: INTEGER;\\n    ID_BB_GLOBAL: STRING;\\n    ID_ISIN: STRING(optional);\\n    ID_BB_SEC_NUM_DES: STRING(optional);\\n    FEED_SOURCE: STRING(optional);\\n    CALL_SCHEDULE_DT: DATE;\\n    CALL_SCHEDULE_PCT: DECIMAL;\\n  }\\n}\\nsection callScheduleMetaRecord: BloombergMetadata\\n{\\n  Record\\n  {\\n    PROGRAMNAME  : STRING;\\n    DATEFORMAT   : STRING;\\n    REGION       : STRING;\\n    TYPE         : STRING;\\n    DATARECORDS  : INTEGER;\\n    TIMESTARTED  : DATETIME(format=\\'EEE MMM d HH:mm:ss zz yyyy\\');\\n    TIMEFINISHED : DATETIME(format=\\'EEE MMM d HH:mm:ss zz yyyy\\');\\n  }\\n}';\n"
                + "    }\n"
                + "  ];\n"
                + "}\n";
    }

    private String bbgBulkTree()
    {
        return "  #{\n"
                + "    demo::externalFormat::flatdata::bloomberg::bulkFiles::models::BloombergBulkFile{\n"
                + "      fltCpnHist{\n"
                + "        SECURITY,\n"
                + "        ERROR_COUNT,\n"
                + "        FIELD_COUNT,\n"
                + "        ID_BB_UNIQUE,\n"
                + "        ID_BB_COMPANY,\n"
                + "        ID_BB_SECURITY,\n"
                + "        ID_BB_GLOBAL,\n"
                + "        ID_ISIN,\n"
                + "        ID_BB_SEC_NUM_DES,\n"
                + "        FEED_SOURCE,\n"
                + "        FLT_CPN_HIST_DT,\n"
                + "        FLT_CPN_HIST_RT\n"
                + "      },\n"
                + "      fltCpnHistMetaRecord{\n"
                + "        programname,\n"
                + "        dateformat,\n"
                + "        region,\n"
                + "        type,\n"
                + "        datarecords,\n"
                + "        timestarted,\n"
                + "        timefinished\n"
                + "      },\n"
                + "      callSchedule{\n"
                + "        SECURITY,\n"
                + "        ERROR_COUNT,\n"
                + "        FIELD_COUNT,\n"
                + "        ID_BB_UNIQUE,\n"
                + "        ID_BB_COMPANY,\n"
                + "        ID_BB_SECURITY,\n"
                + "        ID_BB_GLOBAL,\n"
                + "        ID_ISIN,\n"
                + "        ID_BB_SEC_NUM_DES,\n"
                + "        FEED_SOURCE,\n"
                + "        CALL_SCHEDULE_DT,\n"
                + "        CALL_SCHEDULE_PCT\n"
                + "      },\n"
                + "      callScheduleMetaRecord{\n"
                + "        programname,\n"
                + "        dateformat,\n"
                + "        region,\n"
                + "        type,\n"
                + "        datarecords,\n"
                + "        timestarted,\n"
                + "        timefinished\n"
                + "      }\n"
                + "    }\n"
                + "  }#\n";
    }
}
