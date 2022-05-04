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

import org.finos.legend.engine.external.format.flatdata.shared.driver.AbstractDriverTest;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class TestMultiFileData extends AbstractDriverTest
{
    @Test
    public void canReadMultiFileDataAndMetadataInOrder()
    {
        test(BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("fltCpnHistMeta"),
                BloombergFixtures.CALL_SCHEDULE_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("callScheduleMeta"),
                BloombergFixtures.PUT_SCHEDULE_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("putScheduleMeta"),
                BloombergFixtures.PARTLY_PAID_SCHEDULE_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("partlyPaidScheduleMeta"));
    }

    @Test
    public void canReadMultiFileDataAndMetadataOutOfOrder()
    {
        test(BloombergFixtures.CALL_SCHEDULE_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("callScheduleMeta"),
                BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("fltCpnHistMeta"),
                BloombergFixtures.PARTLY_PAID_SCHEDULE_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("partlyPaidScheduleMeta"),
                BloombergFixtures.PUT_SCHEDULE_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("putScheduleMeta"));
    }

    @Test
    public void canReadMultiFileDataAndMetadataSkippingASection()
    {
        Deserializer<BloombergFixtures.BloombergRecord> deserializer = deserializer(BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("fltCpnHistMeta"),
                BloombergFixtures.PUT_SCHEDULE_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("putScheduleMeta"),
                BloombergFixtures.PARTLY_PAID_SCHEDULE_SECTION_GRAMMAR,
                BloombergFixtures.metadataGrammar("partlyPaidScheduleMeta"));
        List<IChecked<BloombergFixtures.BloombergRecord>> returned = deserializer.deserialize();

        Assert.assertEquals(4, returned.size());
    }

    private void test(String... grammar)
    {
        Deserializer<BloombergFixtures.BloombergRecord> deserializer = deserializer(grammar);
        List<IChecked<BloombergFixtures.BloombergRecord>> returned = deserializer.deserialize();

        Assert.assertEquals(9, returned.size());

        List<IChecked<BloombergFixtures.FltCpnHist>> fltCpnHistRecords = deserializer.recordsCreatedBy("fltCpnHist");
        Assert.assertEquals(3, fltCpnHistRecords.size());
        fltCpnHistRecords.forEach(this::assertNoDefects);
        BloombergFixtures.FltCpnHist fltCpnHist = fltCpnHistRecords.get(0).getValue();
        Assert.assertEquals("EC5111111 Corp", fltCpnHist.security);
        Assert.assertEquals(LocalDate.parse("2001-02-20"), fltCpnHist.fltCpnHistDt);
        Assert.assertEquals("3.6", fltCpnHist.fltCpnHistRt.toPlainString());

        List<IChecked<BloombergFixtures.BloombergMetadata>> fltCpnHistMetadataRecords = deserializer.recordsCreatedBy("fltCpnHistMeta");
        Assert.assertEquals(1, fltCpnHistMetadataRecords.size());
        assertNoDefects(fltCpnHistMetadataRecords.get(0));
        BloombergFixtures.BloombergMetadata fltCpnHistMeta = fltCpnHistMetadataRecords.get(0).getValue();
        Assert.assertEquals("getdata", fltCpnHistMeta.programName);
        Assert.assertEquals(3, fltCpnHistMeta.dataRecords);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:42Z"), fltCpnHistMeta.timeStarted);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:43Z"), fltCpnHistMeta.timeFinished);

        List<IChecked<BloombergFixtures.CallSchedule>> callScheduleRecords = deserializer.recordsCreatedBy("callSchedule");
        Assert.assertEquals(5, callScheduleRecords.size());
        callScheduleRecords.forEach(this::assertNoDefects);
        BloombergFixtures.CallSchedule callSchedule = callScheduleRecords.get(0).getValue();
        Assert.assertEquals("EC4111111 Corp", callSchedule.security);
        Assert.assertEquals(LocalDate.parse("2003-09-25"), callSchedule.callScheduleDt);
        Assert.assertEquals("100", callSchedule.callSchedulePct.toPlainString());

        List<IChecked<BloombergFixtures.BloombergMetadata>> callScheduleMetadataRecords = deserializer.recordsCreatedBy("callScheduleMeta");
        Assert.assertEquals(1, callScheduleMetadataRecords.size());
        assertNoDefects(callScheduleMetadataRecords.get(0));
        BloombergFixtures.BloombergMetadata callScheduleMeta = callScheduleMetadataRecords.get(0).getValue();
        Assert.assertEquals("getdata", callScheduleMeta.programName);
        Assert.assertEquals(5, callScheduleMeta.dataRecords);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:43Z"), callScheduleMeta.timeStarted);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:44Z"), callScheduleMeta.timeFinished);

        List<IChecked<BloombergFixtures.PutSchedule>> putScheduleRecords = deserializer.recordsCreatedBy("putSchedule");
        Assert.assertEquals(1, putScheduleRecords.size());
        putScheduleRecords.forEach(this::assertNoDefects);
        BloombergFixtures.PutSchedule putSchedule = putScheduleRecords.get(0).getValue();
        Assert.assertEquals("BR6111111 Corp", putSchedule.security);
        Assert.assertEquals(LocalDate.parse("2021-10-14"), putSchedule.putScheduleDt);
        Assert.assertEquals("100", putSchedule.putSchedulePct.toPlainString());

        List<IChecked<BloombergFixtures.BloombergMetadata>> putScheduleMetadataRecords = deserializer.recordsCreatedBy("putScheduleMeta");
        Assert.assertEquals(1, putScheduleMetadataRecords.size());
        assertNoDefects(putScheduleMetadataRecords.get(0));
        BloombergFixtures.BloombergMetadata putScheduleMeta = putScheduleMetadataRecords.get(0).getValue();
        Assert.assertEquals("getdata", putScheduleMeta.programName);
        Assert.assertEquals(1, putScheduleMeta.dataRecords);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:44Z"), putScheduleMeta.timeStarted);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:44Z"), putScheduleMeta.timeFinished);

        List<IChecked<BloombergFixtures.PartlyPaidSchedule>> partlyPaidScheduleRecords = deserializer.recordsCreatedBy("partlyPaidSchedule");
        Assert.assertEquals(0, partlyPaidScheduleRecords.size());

        List<IChecked<BloombergFixtures.BloombergMetadata>> partlyPaidScheduleMetadataRecords = deserializer.recordsCreatedBy("partlyPaidScheduleMeta");
        Assert.assertEquals(1, partlyPaidScheduleMetadataRecords.size());
        assertNoDefects(partlyPaidScheduleMetadataRecords.get(0));
        BloombergFixtures.BloombergMetadata partlyPaidScheduleMeta = partlyPaidScheduleMetadataRecords.get(0).getValue();
        Assert.assertEquals("getdata", partlyPaidScheduleMeta.programName);
        Assert.assertEquals(0, partlyPaidScheduleMeta.dataRecords);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:45Z"), partlyPaidScheduleMeta.timeStarted);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:45Z"), partlyPaidScheduleMeta.timeFinished);
    }

    private Deserializer<BloombergFixtures.BloombergRecord> deserializer(String... grammar)
    {
        FlatData flatData = parseFlatData(String.join("\n", grammar));

        return new Deserializer<BloombergFixtures.BloombergRecord>(flatData, resource("bloomberg/bbg_multi_file"))
                .withSectionDetails("fltCpnHist", BloombergFixtures.FltCpnHist.class, true)
                .withSectionDetails("fltCpnHistMeta", BloombergFixtures.BloombergMetadata.class, false)
                .withSectionDetails("callSchedule", BloombergFixtures.CallSchedule.class, true)
                .withSectionDetails("callScheduleMeta", BloombergFixtures.BloombergMetadata.class, false)
                .withSectionDetails("putSchedule", BloombergFixtures.PutSchedule.class, true)
                .withSectionDetails("putScheduleMeta", BloombergFixtures.BloombergMetadata.class, false)
                .withSectionDetails("partlyPaidSchedule", BloombergFixtures.PutSchedule.class, true)
                .withSectionDetails("partlyPaidScheduleMeta", BloombergFixtures.BloombergMetadata.class, false);
    }
}
