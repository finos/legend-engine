package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.AbstractDriverTest;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class TestSingleFileData extends AbstractDriverTest
{
    @Test
    public void canReadSingleFileData()
    {
        FlatData flatData = parseFlatData(BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR);

        List<IChecked<BloombergFixtures.FltCpnHist>> records = deserialize(BloombergFixtures.FltCpnHist.class, flatData, resource("bloomberg/bbg_single_file"));

        Assert.assertEquals(3, records.size());
        records.forEach(this::assertNoDefects);
        assertSource(1, 22, "EC5111111 Corp|-1|9|XXEC5111111|111111|1111111|XXX111111XX1|XS0111111111|XXXX V0 09/25/21 XXXX|N.A.|20010220|3.600000|",
                Arrays.asList(
                        rValue("SECURITY", "EC5111111 Corp"),
                        rValue("ERROR_COUNT", "-1"),
                        rValue("FIELD_COUNT", "9"),
                        rValue("ID_BB_UNIQUE", "XXEC5111111"),
                        rValue("ID_BB_COMPANY", "111111"),
                        rValue("ID_BB_SECURITY", "1111111"),
                        rValue("ID_BB_GLOBAL", "XXX111111XX1"),
                        rValue("ID_ISIN", "XS0111111111"),
                        rValue("ID_BB_SEC_NUM_DES", "XXXX V0 09/25/21 XXXX"),
                        rValue("FEED_SOURCE", "N.A."),
                        rValue("FLT_CPN_HIST_DT", "20010220"),
                        rValue("FLT_CPN_HIST_RT", "3.600000")
                ), records.get(0));
        Assert.assertEquals("EC5111111 Corp", records.get(0).getValue().security);
        Assert.assertEquals(-1, records.get(0).getValue().errorCount);
        Assert.assertEquals(9, records.get(0).getValue().fieldCount);
        Assert.assertEquals("XXEC5111111", records.get(0).getValue().idBbUnique);
        Assert.assertEquals(111111, records.get(0).getValue().idBbCompany);
        Assert.assertEquals(1111111, records.get(0).getValue().idBbSecurity);
        Assert.assertEquals("XXX111111XX1", records.get(0).getValue().idBbGlobal);
        Assert.assertEquals("XS0111111111", records.get(0).getValue().idIsin);
        Assert.assertEquals("XXXX V0 09/25/21 XXXX", records.get(0).getValue().idBbSecNumDes);
        Assert.assertNull(records.get(0).getValue().feedSource);
        Assert.assertEquals(LocalDate.parse("2001-02-20"), records.get(0).getValue().fltCpnHistDt);
        Assert.assertEquals("3.6", records.get(0).getValue().fltCpnHistRt.toPlainString());

        assertSource(3, 24, "EJ8111112 Corp|-1|9|XXEJ8111112|111112|11111112|XXX111X2X222|XS0011111112|XXXXX V0 09/25/21 0000|XXX|20130925|4.000000|",
                Arrays.asList(
                        rValue("SECURITY", "EJ8111112 Corp"),
                        rValue("ERROR_COUNT", "-1"),
                        rValue("FIELD_COUNT", "9"),
                        rValue("ID_BB_UNIQUE", "XXEJ8111112"),
                        rValue("ID_BB_COMPANY", "111112"),
                        rValue("ID_BB_SECURITY", "11111112"),
                        rValue("ID_BB_GLOBAL", "XXX111X2X222"),
                        rValue("ID_ISIN", "XS0011111112"),
                        rValue("ID_BB_SEC_NUM_DES", "XXXXX V0 09/25/21 0000"),
                        rValue("FEED_SOURCE", "XXX"),
                        rValue("FLT_CPN_HIST_DT", "20130925"),
                        rValue("FLT_CPN_HIST_RT", "4.000000")
                ), records.get(2));
        Assert.assertEquals("EJ8111112 Corp", records.get(2).getValue().security);
        Assert.assertEquals(-1, records.get(2).getValue().errorCount);
        Assert.assertEquals(9, records.get(2).getValue().fieldCount);
        Assert.assertEquals("XXEJ8111112", records.get(2).getValue().idBbUnique);
        Assert.assertEquals(111112, records.get(2).getValue().idBbCompany);
        Assert.assertEquals(11111112, records.get(2).getValue().idBbSecurity);
        Assert.assertEquals("XXX111X2X222", records.get(2).getValue().idBbGlobal);
        Assert.assertEquals("XS0011111112", records.get(2).getValue().idIsin);
        Assert.assertEquals("XXXXX V0 09/25/21 0000", records.get(2).getValue().idBbSecNumDes);
        Assert.assertEquals("XXX", records.get(2).getValue().feedSource);
        Assert.assertEquals(LocalDate.parse("2013-09-25"), records.get(2).getValue().fltCpnHistDt);
        Assert.assertEquals("4", records.get(2).getValue().fltCpnHistRt.toPlainString());
    }

    @Test
    public void canReadSingleFileDataAndMetadata()
    {
        FlatData flatData = parseFlatData(BloombergFixtures.FLT_CPN_HIST_SECTION_GRAMMAR + "\n" + BloombergFixtures.metadataGrammar("fltCpnHistMeta"));

        Deserializer<BloombergFixtures.BloombergMetadata> deserializer = new Deserializer<BloombergFixtures.BloombergMetadata>(flatData, resource("bloomberg/bbg_single_file"))
                .withSectionDetails("fltCpnHist", BloombergFixtures.FltCpnHist.class, false)
                .withSectionDetails("fltCpnHistMeta", BloombergFixtures.BloombergMetadata.class, true);
        List<IChecked<BloombergFixtures.BloombergMetadata>> returned = deserializer.deserialize();

        List<IChecked<BloombergFixtures.FltCpnHist>> fltCpnHistRecords = deserializer.recordsCreatedBy("fltCpnHist");
        Assert.assertEquals(3, fltCpnHistRecords.size());
        fltCpnHistRecords.forEach(this::assertNoDefects);

        Assert.assertEquals(1, returned.size());
        IChecked<BloombergFixtures.BloombergMetadata> metadataRecord = returned.get(0);
        assertNoDefects(metadataRecord);
        assertSource(1, 2, "DATA=FLT_CPN_HIST\nREGION=euro\nTYPE=dif\nPROGRAMNAME=getdata\nDATEFORMAT=yyyymmdd\nTIMESTARTED=Sun Sep 26 18:57:42 BST 2021\nDATARECORDS=3\nTIMEFINISHED=Sun Sep 26 18:57:43 BST 2021",
                Arrays.asList(
                        rValue("DATA", "FLT_CPN_HIST"),
                        rValue("REGION", "euro"),
                        rValue("TYPE", "dif"),
                        rValue("PROGRAMNAME", "getdata"),
                        rValue("DATEFORMAT", "yyyymmdd"),
                        rValue("TIMESTARTED", "Sun Sep 26 18:57:42 BST 2021"),
                        rValue("DATARECORDS", "3"),
                        rValue("TIMEFINISHED", "Sun Sep 26 18:57:43 BST 2021")
                ), metadataRecord);
        Assert.assertEquals("getdata", metadataRecord.getValue().programName);
        Assert.assertEquals("euro", metadataRecord.getValue().region);
        Assert.assertEquals("dif", metadataRecord.getValue().type);
        Assert.assertEquals("yyyymmdd", metadataRecord.getValue().dateFormat);
        Assert.assertEquals(3, metadataRecord.getValue().dataRecords);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:42Z"), metadataRecord.getValue().timeStarted);
        Assert.assertEquals(Instant.parse("2021-09-26T17:57:43Z"), metadataRecord.getValue().timeFinished);
    }
}
