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

package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

class BloombergFixtures
{
    private static final String COMMON_RECORD_START = "  Record\n" +
            "  {\n" +
            "    SECURITY                        : STRING;\n" +
            "    ERROR_COUNT                     : INTEGER;\n" +
            "    FIELD_COUNT                     : INTEGER;\n" +
            "    ID_BB_UNIQUE                    : STRING;\n" +
            "    ID_BB_COMPANY                   : INTEGER;\n" +
            "    ID_BB_SECURITY                  : INTEGER;\n" +
            "    ID_BB_GLOBAL                    : STRING;\n" +
            "    ID_ISIN                         : STRING(optional);\n" +
            "    ID_BB_SEC_NUM_DES               : STRING(optional);\n" +
            "    FEED_SOURCE                     : STRING(optional);\n";

    static final String FLT_CPN_HIST_SECTION_GRAMMAR = "section fltCpnHist: BloombergData\n" +
            "{\n" +
            "  filter: ['DATA=FLT_CPN_HIST', 'REGION=euro'];\n" +
            COMMON_RECORD_START +
            "    FLT_CPN_HIST_DT : DATE;\n" +
            "    FLT_CPN_HIST_RT : DECIMAL;\n" +
            "  }\n" +
            "}";

    static final String CALL_SCHEDULE_SECTION_GRAMMAR = "section callSchedule: BloombergData\n" +
            "{\n" +
            "  filter: 'DATA=CALL_SCHEDULE';\n" +
            COMMON_RECORD_START +
            "    CALL_SCHEDULE_DT  : DATE;\n" +
            "    CALL_SCHEDULE_PCT : DECIMAL;\n" +
            "  }\n" +
            "}";

    static final String PUT_SCHEDULE_SECTION_GRAMMAR = "section putSchedule: BloombergData\n" +
            "{\n" +
            "  filter: 'DATA=PUT_SCHEDULE';\n" +
            COMMON_RECORD_START +
            "    PUT_SCHEDULE_DT  : DATE;\n" +
            "    PUT_SCHEDULE_PCT : DECIMAL;\n" +
            "  }\n" +
            "}";

    static final String PARTLY_PAID_SCHEDULE_SECTION_GRAMMAR = "section partlyPaidSchedule: BloombergData\n" +
            "{\n" +
            "  filter: 'DATA=PARTLY_PAID_SCHEDULE';\n" +
            COMMON_RECORD_START +
            "    PARTLY_PAID_SCHEDULE_DT  : DATE;\n" +
            "    PARTLY_PAID_SCHEDULE_PCT : DECIMAL;\n" +
            "  }\n" +
            "}";

    private static final String METADATA_SECTION_GRAMMAR = "section %s: BloombergMetadata\n" +
            "{\n" +
            "  Record\n" +
            "  {\n" +
            "    PROGRAMNAME  : STRING;\n" +
            "    DATEFORMAT   : STRING;\n" +
            "    REGION       : STRING;\n" +
            "    TYPE         : STRING;\n" +
            "    DATARECORDS  : INTEGER;\n" +
            "    TIMESTARTED  : DATETIME(format='EEE MMM d HH:mm:ss zz yyyy');\n" +
            "    TIMEFINISHED : DATETIME(format='EEE MMM d HH:mm:ss zz yyyy');\n" +
            "  }\n" +
            "}";

    static String metadataGrammar(String id)
    {
        return String.format(METADATA_SECTION_GRAMMAR, id);
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class BloombergRecord
    {
        public String security;
        public long errorCount;
        public long fieldCount;
        public String idBbUnique;
        public long idBbCompany;
        public long idBbSecurity;
        public String idBbGlobal;
        public String idIsin;
        public String idBbSecNumDes;
        public String feedSource;
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class FltCpnHist extends BloombergRecord
    {
        public LocalDate fltCpnHistDt;
        public BigDecimal fltCpnHistRt;
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class CallSchedule extends BloombergRecord
    {
        public LocalDate callScheduleDt;
        public BigDecimal callSchedulePct;
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class PutSchedule extends BloombergRecord
    {
        public LocalDate putScheduleDt;
        public BigDecimal putSchedulePct;
    }

    @SuppressWarnings({"unused"})  // Required for reflective access, empty section test
    public static class PartlyPaidSchedule extends BloombergRecord
    {
        public LocalDate partlyPaidScheduleDt;
        public BigDecimal partlyPaidSchedulePct;
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class BloombergMetadata
    {
        public String programName;
        public String region;
        public String type;
        public String dateFormat;
        public long dataRecords;
        public Instant timeStarted;
        public Instant timeFinished;
    }
}
