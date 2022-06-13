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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TestCorporateActionsFile extends AbstractDriverTest
{
    @Test
    public void canReadCorporateActionsFile()
    {
        FlatData flatData = parseFlatData(grammar(false));

        Deserializer<Object> deserializer = new Deserializer<>(flatData, resource("bloomberg/bbg_actions_file"))
                .withSectionDetails("actions", Action.class, true)
                .withSectionDetails("deletes", Delete.class, true)
                .withSectionDetails("dvdCash", DvdCash.class, true)
                .withSectionDetails("stockBuy", StockBuy.class, true)
                .withSectionDetails("acquis", Acquis.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, false);
        List<IChecked<Object>> returned = deserializer.deserialize();

        Assert.assertEquals(28, returned.size());
        returned.forEach(this::assertNoDefects);


        List<IChecked<Delete>> actions = deserializer.recordsCreatedBy("actions");
        List<IChecked<Delete>> deletes = deserializer.recordsCreatedBy("deletes");
        List<IChecked<DvdCash>> dvdCashes = deserializer.recordsCreatedBy("dvdCash");
        List<IChecked<StockBuy>> stockBuys = deserializer.recordsCreatedBy("stockBuy");
        List<IChecked<Acquis>> acquises = deserializer.recordsCreatedBy("acquis");
        Assert.assertEquals(6, actions.size());
        Assert.assertEquals(2, deletes.size());
        Assert.assertEquals(5, dvdCashes.size());
        Assert.assertEquals(11, stockBuys.size());
        Assert.assertEquals(4, acquises.size());

        Action action = actions.get(0).getValue();
        Assert.assertEquals("AA9     GR Equity", action.security);
        Assert.assertEquals(111119L, action.idBbCompany);
        Assert.assertEquals(0L, action.idBbSecurity);
        Assert.assertEquals(0L, action.rCode);
        Assert.assertEquals(111111119, action.idAction.longValue());
        Assert.assertEquals("DIVEST", action.mnemonic);
        Assert.assertEquals("U", action.actionFlag);
        Assert.assertEquals("Afffff FF", action.idBbGlobalCompanyName);
        Assert.assertNull(action.securityIdTyp);
        Assert.assertNull(action.securityId);
        Assert.assertNull(action.crncy);
        Assert.assertNull(action.marketSectorDes);
        Assert.assertNull(action.idBbUnique);
        Assert.assertEquals(LocalDate.parse("2020-01-10"), action.announceDt);
        Assert.assertNull(action.effDt);
        Assert.assertEquals(LocalDate.parse("2020-01-17"), action.amendedDt);
        Assert.assertNull(action.idBbGlobal);
        Assert.assertEquals("XXX119XXX1X1", action.idBbGlobalCompany);
        Assert.assertNull(action.idBbSecNumDes);
        Assert.assertNull(action.feedSource);
        Assert.assertEquals(32L, action.nFields.longValue());
        Assert.assertEquals("Note 5870", action.cpNotes);

        Delete delete = deletes.get(0).getValue();
        Assert.assertEquals("AA6     GR Equity", delete.security);
        Assert.assertEquals(111116L, delete.idBbCompany);
        Assert.assertEquals(0L, delete.idBbSecurity);
        Assert.assertEquals(0L, delete.rCode);
        Assert.assertEquals(111111116L, delete.idAction.longValue());
        Assert.assertEquals("DIVEST", delete.mnemonic);
        Assert.assertEquals("D", delete.actionFlag);
        Assert.assertEquals("AccAc CC", delete.idBbGlobalCompanyName);
        Assert.assertNull(delete.securityIdTyp);
        Assert.assertNull(delete.securityId);
        Assert.assertNull(delete.crncy);
        Assert.assertNull(delete.marketSectorDes);
        Assert.assertNull(delete.idBbUnique);
        Assert.assertEquals(LocalDate.parse("2020-01-09"), delete.announceDt);
        Assert.assertNull(delete.effDt);
        Assert.assertEquals(LocalDate.parse("2020-01-17"), delete.amendedDt);
        Assert.assertNull(delete.idBbGlobal);
        Assert.assertEquals("XXX116XX1XX1", delete.idBbGlobalCompany);
        Assert.assertNull(delete.idBbSecNumDes);
        Assert.assertNull(delete.feedSource);
        Assert.assertEquals(1L, delete.nFields.longValue());
        Assert.assertNull(delete.cpNotes);
        Assert.assertEquals("2", delete.cpDeleteReason);

        DvdCash dvdCash = dvdCashes.get(0).getValue();
        Assert.assertEquals("AA3     GR Equity", dvdCash.security);
        Assert.assertEquals(111113, dvdCash.idBbCompany);
        Assert.assertEquals(1113L, dvdCash.idBbSecurity);
        Assert.assertEquals(0L, dvdCash.rCode);
        Assert.assertEquals(111111113, dvdCash.idAction.longValue());
        Assert.assertEquals("DVD_CASH", dvdCash.mnemonic);
        Assert.assertEquals("N", dvdCash.actionFlag);
        Assert.assertEquals("Aaaaaaaa Xx/Yyy", dvdCash.idBbGlobalCompanyName);
        Assert.assertEquals("CUSIP", dvdCash.securityIdTyp);
        Assert.assertEquals("111111113", dvdCash.securityId);
        Assert.assertEquals("EUR", dvdCash.crncy);
        Assert.assertEquals("Equity", dvdCash.marketSectorDes);
        Assert.assertEquals("EQ0011111300001113", dvdCash.idBbUnique);
        Assert.assertEquals(LocalDate.parse("2020-01-17"), dvdCash.announceDt);
        Assert.assertEquals(LocalDate.parse("2020-02-14"), dvdCash.effDt);
        Assert.assertEquals(LocalDate.parse("2020-01-17"), dvdCash.amendedDt);
        Assert.assertEquals("XXX113XXXX11", dvdCash.idBbGlobal);
        Assert.assertEquals("XXX113XXXXX1", dvdCash.idBbGlobalCompany);
        Assert.assertEquals("AA3", dvdCash.idBbSecNumDes);
        Assert.assertEquals("AA", dvdCash.feedSource);
        Assert.assertEquals(24L, dvdCash.nFields.longValue());
        Assert.assertNull(dvdCash.cpNotes);
        Assert.assertEquals(LocalDate.parse("2020-02-18"), dvdCash.cpRecordDt);
        Assert.assertEquals(LocalDate.parse("2020-03-06"), dvdCash.cpPayDt);
        Assert.assertEquals(4L, dvdCash.cpFreq.longValue());
        Assert.assertNull(dvdCash.cpNetAmt);
        Assert.assertNull(dvdCash.cpTaxAmt);
        Assert.assertEquals("0.62", dvdCash.cpGrossAmt.toPlainString());
        Assert.assertNull(dvdCash.cpFrankedAmt);
        Assert.assertEquals("USD", dvdCash.cpDvdCrncy);
        Assert.assertEquals("1000", dvdCash.cpDvdTyp);
        Assert.assertNull(dvdCash.cpSpplAmt);
        Assert.assertNull(dvdCash.cpForeignAmt);
        Assert.assertNull(dvdCash.cpParPct);
        Assert.assertEquals("D", dvdCash.cpStockOpt);
        Assert.assertNull(dvdCash.cpReinvestRatio);
        Assert.assertNull(dvdCash.cpPx);
        Assert.assertNull(dvdCash.cpTaxRt);
        Assert.assertEquals("1", dvdCash.cpAdj.toPlainString());
        Assert.assertEquals(LocalDate.parse("2020-02-14"), dvdCash.cpAdjDt);
        Assert.assertEquals("N", dvdCash.cpIndicator);
        Assert.assertNull(dvdCash.cpDvdDrpDiscount);
        Assert.assertNull(dvdCash.cpEusdTid);
        Assert.assertNull(dvdCash.cpEusdTidSw);
        Assert.assertEquals("F", dvdCash.cpDistAmtStatus);

        StockBuy stockBuy = stockBuys.get(0).getValue();
        Assert.assertEquals("AA5     GR Equity", stockBuy.security);
        Assert.assertEquals(111115, stockBuy.idBbCompany);
        Assert.assertEquals(0L, stockBuy.idBbSecurity);
        Assert.assertEquals(0L, stockBuy.rCode);
        Assert.assertEquals(111111115, stockBuy.idAction.longValue());
        Assert.assertEquals("STOCK_BUY", stockBuy.mnemonic);
        Assert.assertEquals("N", stockBuy.actionFlag);
        Assert.assertEquals("AB BBB", stockBuy.idBbGlobalCompanyName);
        Assert.assertNull(stockBuy.securityIdTyp);
        Assert.assertNull(stockBuy.securityId);
        Assert.assertNull(stockBuy.crncy);
        Assert.assertNull(stockBuy.marketSectorDes);
        Assert.assertNull(stockBuy.idBbUnique);
        Assert.assertEquals(LocalDate.parse("2020-01-17"), stockBuy.announceDt);
        Assert.assertEquals(LocalDate.parse("2020-01-17"), stockBuy.effDt);
        Assert.assertEquals(LocalDate.parse("2020-01-17"), stockBuy.amendedDt);
        Assert.assertNull(stockBuy.idBbGlobal);
        Assert.assertEquals("XXX115XXX111", stockBuy.idBbGlobalCompany);
        Assert.assertNull(stockBuy.idBbSecNumDes);
        Assert.assertNull(stockBuy.feedSource);
        Assert.assertEquals(10L, stockBuy.nFields.longValue());
        Assert.assertEquals("Note 02374", stockBuy.cpNotes);
        Assert.assertEquals("8100865.078125", stockBuy.cpAmt.toPlainString());
        Assert.assertEquals("1", stockBuy.cpStockBuyTyp);
        Assert.assertEquals(LocalDate.parse("2020-01-17"), stockBuy.cpCompletedDt);
        Assert.assertEquals(false, stockBuy.cpTermFlag);
        Assert.assertEquals("1", stockBuy.cpSecTyp);
        Assert.assertEquals("497.728699", stockBuy.cpPx.toPlainString());
        Assert.assertEquals("8.100864", stockBuy.cpSh.toPlainString());
        Assert.assertEquals("4032.032471", stockBuy.cpValPurchased.toPlainString());
        Assert.assertEquals("GBp", stockBuy.cpCrncy);

        Acquis acquis = acquises.get(0).getValue();
        Assert.assertEquals("AA7     GR Equity", acquis.security);
        Assert.assertEquals(111117, acquis.idBbCompany);
        Assert.assertEquals(0L, acquis.idBbSecurity);
        Assert.assertEquals(0L, acquis.rCode);
        Assert.assertEquals(111111117, acquis.idAction.longValue());
        Assert.assertEquals("ACQUIS", acquis.mnemonic);
        Assert.assertEquals("U", acquis.actionFlag);
        Assert.assertEquals("AdddddddddAddddd DD", acquis.idBbGlobalCompanyName);
        Assert.assertNull(acquis.securityIdTyp);
        Assert.assertNull(acquis.securityId);
        Assert.assertNull(acquis.crncy);
        Assert.assertNull(acquis.marketSectorDes);
        Assert.assertNull(acquis.idBbUnique);
        Assert.assertEquals(LocalDate.parse("2019-12-17"), acquis.announceDt);
        Assert.assertNull(acquis.effDt);
        Assert.assertEquals(LocalDate.parse("2020-01-17"), acquis.amendedDt);
        Assert.assertNull(acquis.idBbGlobal);
        Assert.assertEquals("XXX117XXXX11", acquis.idBbGlobalCompany);
        Assert.assertNull(acquis.idBbSecNumDes);
        Assert.assertNull(acquis.feedSource);
        Assert.assertEquals(39L, acquis.nFields.longValue());
        Assert.assertNull(null, acquis.cpNotes);
        Assert.assertEquals("3", acquis.cpFlag);
        Assert.assertEquals("111117X1 XX", acquis.cpTkr);
        Assert.assertEquals("Adddddddddd Addddd Adddddd DD", acquis.cpName);
        Assert.assertEquals(11117001L, acquis.cpIdBbComp.longValue());
        Assert.assertEquals("Adddddddddd Addddd DD", acquis.cpUnit);
        Assert.assertEquals("0", acquis.cpTotAmt.toPlainString());
        Assert.assertEquals("9", acquis.cpAcquisTyp);
        Assert.assertEquals("0", acquis.cpCash.toPlainString());
        Assert.assertNull(acquis.cpCashFlag);
        Assert.assertEquals("0", acquis.cpSh.toPlainString());
        Assert.assertNull(acquis.cpShFlag);
        Assert.assertEquals("0", acquis.cpDebt.toPlainString());
        Assert.assertNull(acquis.cpDebtFlag);
        Assert.assertEquals("1", acquis.cpStat);
        Assert.assertNull(acquis.cpDtFlag);
        Assert.assertNull(acquis.cpInitOfferPremium);
        Assert.assertNull(acquis.cpCurPremium);
        Assert.assertEquals("0", acquis.cpPctOwned.toPlainString());
        Assert.assertEquals("100", acquis.cpPctSought.toPlainString());
        Assert.assertNull(acquis.cpUnsolicited);
        Assert.assertEquals("0", acquis.cpDetailFlag);
        Assert.assertEquals("NOK", acquis.cpCrncy);
        Assert.assertNull(acquis.cpCashVal);
        Assert.assertNull(acquis.cpArbitrageProf);
        Assert.assertNull(acquis.cpCurTotVal);
        Assert.assertEquals(1L, acquis.cpAdvisorsNum.longValue());
        Assert.assertNull(acquis.cpShFractional);
        Assert.assertEquals(";2;1;3;1;Addddd Adddddddd Adddddd(s);1;AdA;13;100.000000;", acquis.cpAdvisors);
        Assert.assertEquals("AA7 AA", acquis.cpAcqTkr);
        Assert.assertEquals("XXX117XX1XX0", acquis.cpAcqIdBbGlobal);
        Assert.assertEquals("XXX117XXXX11", acquis.cpAcqIdBbGlobalCompany);
        Assert.assertEquals("AA7", acquis.cpAcqIdBbSecNumDes);
        Assert.assertEquals("AA", acquis.cpAcqFeedSource);
        Assert.assertEquals("111117X1 XX", acquis.cpTargetTkr);
        Assert.assertEquals("XXX17XXXXXX1", acquis.cpTargetIdBbGlobal);
        Assert.assertEquals("XXX117XXX1X1", acquis.cpTargetIdBbGlobalCompany);
        Assert.assertEquals("1111117D", acquis.cpTargetIdBbSecNumDes);
        Assert.assertEquals("AB", acquis.cpTargetFeedSource);
    }

    @Test
    public void canIncludeNoActions()
    {
        FlatData flatData = parseFlatData(grammar(true));

        Deserializer<Object> deserializer = new Deserializer<>(flatData, resource("bloomberg/bbg_actions_file"))
                .withSectionDetails("actions", Action.class, true)
                .withSectionDetails("deletes", Delete.class, true)
                .withSectionDetails("dvdCash", DvdCash.class, true)
                .withSectionDetails("stockBuy", StockBuy.class, true)
                .withSectionDetails("acquis", Acquis.class, true)
                .withSectionDetails("metadata", BloombergFixtures.BloombergMetadata.class, false);
        List<IChecked<Object>> returned = deserializer.deserialize();

        Assert.assertEquals(31, returned.size());
        returned.forEach(this::assertNoDefects);

        Assert.assertEquals(9, deserializer.recordsCreatedBy("actions").size());

        Assert.assertEquals(2, deserializer.<IChecked<Delete>>recordsCreatedBy("deletes").size());
        Assert.assertEquals(5, deserializer.<IChecked<DvdCash>>recordsCreatedBy("dvdCash").size());
        Assert.assertEquals(11, deserializer.<IChecked<StockBuy>>recordsCreatedBy("stockBuy").size());
        Assert.assertEquals(4, deserializer.<IChecked<Acquis>>recordsCreatedBy("acquis").size());
    }

    private String grammar(boolean includeNoAction)
    {
        return "section actions: BloombergActions\n" +
                "{\n" +
                (includeNoAction ? "        includeNoActionRecords;\n" : "") +
                "  Record\n" +
                "  {\n" +
                "    SECURITY                  : STRING;\n" +
                "    ID_BB_COMPANY             : INTEGER;\n" +
                "    ID_BB_SECURITY            : INTEGER;\n" +
                "    R_CODE                    : INTEGER;\n" +
                "    ID_ACTION                 : INTEGER(optional);\n" +
                "    MNEMONIC                  : STRING(optional);\n" +
                "    ACTION_FLAG               : STRING(optional);\n" +
                "    ID_BB_GLOBAL_COMPANY_NAME : STRING(optional);\n" +
                "    SECURITY_ID_TYP           : STRING(optional);\n" +
                "    SECURITY_ID               : STRING(optional);\n" +
                "    CRNCY                     : STRING(optional);\n" +
                "    MARKET_SECTOR_DES         : STRING(optional);\n" +
                "    ID_BB_UNIQUE              : STRING(optional);\n" +
                "    ANNOUNCE_DT               : DATE(optional);\n" +
                "    EFF_DT                    : DATE(optional);\n" +
                "    AMENDED_DT                : DATE(optional);\n" +
                "    ID_BB_GLOBAL              : STRING(optional);\n" +
                "    ID_BB_GLOBAL_COMPANY      : STRING(optional);\n" +
                "    ID_BB_SEC_NUM_DES         : STRING(optional);\n" +
                "    FEED_SOURCE               : STRING(optional);\n" +
                "    N_FIELDS                  : INTEGER(optional);\n" +
                "    CP_NOTES                  : STRING(optional);\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "section deletes: BloombergActionDetails\n" +
                "{\n" +
                "  actionFlags: 'D';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    CP_DELETE_REASON : STRING(optional);\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "section dvdCash: BloombergActionDetails\n" +
                "{\n" +
                "  actionFlags: ['N', 'U'];\n" +
                "  mnemonics: 'DVD_CASH';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    CP_RECORD_DT              : DATE(optional);\n" +
                "    CP_PAY_DT                 : DATE(optional);\n" +
                "    CP_FREQ                   : INTEGER(optional);\n" +
                "    CP_NET_AMT                : DECIMAL(optional);\n" +
                "    CP_TAX_AMT                : DECIMAL(optional);\n" +
                "    CP_GROSS_AMT              : DECIMAL(optional);\n" +
                "    CP_FRANKED_AMT            : DECIMAL(optional);\n" +
                "    CP_DVD_CRNCY              : STRING(optional);\n" +
                "    CP_DVD_TYP                : STRING(optional);\n" +
                "    CP_SPPL_AMT               : DECIMAL(optional);\n" +
                "    CP_FOREIGN_AMT            : DECIMAL(optional);\n" +
                "    CP_PAR_PCT                : DECIMAL(optional);\n" +
                "    CP_STOCK_OPT              : STRING(optional);\n" +
                "    CP_REINVEST_RATIO         : DECIMAL(optional);\n" +
                "    CP_PX                     : DECIMAL(optional);\n" +
                "    CP_TAX_RT                 : DECIMAL(optional);\n" +
                "    CP_ADJ                    : DECIMAL(optional);\n" +
                "    CP_ADJ_DT                 : DATE(optional);\n" +
                "    CP_INDICATOR              : STRING(optional);\n" +
                "    CP_DVD_DRP_DISCOUNT       : DECIMAL(optional);\n" +
                "    CP_EUSD_TID               : DECIMAL(optional);\n" +
                "    CP_EUSD_TID_SW            : DECIMAL(optional);\n" +
                "    CP_DIST_AMT_STATUS        : STRING(optional);\n" +
                "    CP_NOTES                  : STRING(optional);\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "section stockBuy: BloombergActionDetails\n" +
                "{\n" +
                "  actionFlags: ['N', 'U'];\n" +
                "  mnemonics: 'STOCK_BUY';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    CP_AMT                    : DECIMAL(optional);\n" +
                "    CP_STOCK_BUY_TYP          : STRING(optional);\n" +
                "    CP_COMPLETED_DT           : DATE(optional);\n" +
                "    CP_TERM_FLAG              : BOOLEAN(optional);\n" +
                "    CP_SEC_TYP                : STRING(optional);\n" +
                "    CP_PX                     : DECIMAL(optional);\n" +
                "    CP_SH                     : DECIMAL(optional);\n" +
                "    CP_VAL_PURCHASED          : DECIMAL(optional);\n" +
                "    CP_CRNCY                  : STRING(optional);\n" +
                "    CP_NOTES                  : STRING(optional);\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "section acquis: BloombergActionDetails\n" +
                "{\n" +
                "  actionFlags: ['N', 'U'];\n" +
                "  mnemonics: 'ACQUIS';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    CP_FLAG                        : STRING(optional);\n" +
                "    CP_TKR                         : STRING(optional);\n" +
                "    CP_NAME                        : STRING(optional);\n" +
                "    CP_ID_BB_COMP                  : INTEGER(optional);\n" +
                "    CP_UNIT                        : STRING(optional);\n" +
                "    CP_TOT_AMT                     : DECIMAL(optional);\n" +
                "    CP_ACQUIS_TYP                  : STRING(optional);\n" +
                "    CP_CASH                        : DECIMAL(optional);\n" +
                "    CP_CASH_FLAG                   : STRING(optional);\n" +
                "    CP_SH                          : DECIMAL(optional);\n" +
                "    CP_SH_FLAG                     : STRING(optional);\n" +
                "    CP_DEBT                        : DECIMAL(optional);\n" +
                "    CP_DEBT_FLAG                   : STRING(optional);\n" +
                "    CP_STAT                        : STRING(optional);\n" +
                "    CP_DT_FLAG                     : STRING(optional);\n" +
                "    CP_INIT_OFFER_PREMIUM          : STRING(optional);\n" +
                "    CP_CUR_PREMIUM                 : STRING(optional);\n" +
                "    CP_PCT_OWNED                   : DECIMAL(optional);\n" +
                "    CP_PCT_SOUGHT                  : DECIMAL(optional);\n" +
                "    CP_UNSOLICITED                 : STRING(optional);\n" +
                "    CP_DETAIL_FLAG                 : STRING(optional);\n" +
                "    CP_CRNCY                       : STRING(optional);\n" +
                "    CP_CASH_VAL                    : DECIMAL(optional);\n" +
                "    CP_ARBITRAGE_PROF              : DECIMAL(optional);\n" +
                "    CP_CUR_TOT_VAL                 : DECIMAL(optional);\n" +
                "    CP_ADVISORS_NUM                : INTEGER(optional);\n" +
                "    CP_SH_FRACTIONAL               : STRING(optional);\n" +
                "    CP_ADVISORS                    : STRING(optional);\n" +
                "    CP_ACQ_TKR                     : STRING(optional);\n" +
                "    CP_ACQ_ID_BB_GLOBAL            : STRING(optional);\n" +
                "    CP_ACQ_ID_BB_GLOBAL_COMPANY    : STRING(optional);\n" +
                "    CP_ACQ_ID_BB_SEC_NUM_DES       : STRING(optional);\n" +
                "    CP_ACQ_FEED_SOURCE             : STRING(optional);\n" +
                "    CP_TARGET_TKR                  : STRING(optional);\n" +
                "    CP_TARGET_ID_BB_GLOBAL         : STRING(optional);\n" +
                "    CP_TARGET_ID_BB_GLOBAL_COMPANY : STRING(optional);\n" +
                "    CP_TARGET_ID_BB_SEC_NUM_DES    : STRING(optional);\n" +
                "    CP_TARGET_FEED_SOURCE          : STRING(optional);\n" +
                "    CP_NOTES                       : STRING(optional);\n" +
                "  }\n" +
                "}\n" +
                "section metadata: BloombergMetadata\n" +
                "{\n" +
                "  Record\n" +
                "  {\n" +
                "    PROGRAMNAME  : STRING;\n" +
                "    DATEFORMAT   : STRING;\n" +
                "    DATARECORDS  : INTEGER;\n" +
                "    TIMESTARTED  : DATETIME(format='EEE MMM d HH:mm:ss z yyyy');\n" +
                "    TIMEFINISHED : DATETIME(format='EEE MMM d HH:mm:ss z yyyy');\n" +
                "  }\n" +
                "}\n";
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class Action
    {
        public String security;
        public long idBbCompany;
        public long idBbSecurity;
        public long rCode;
        public Long idAction;
        public String mnemonic;
        public String actionFlag;
        public String idBbGlobalCompanyName;
        public String securityIdTyp;
        public String securityId;
        public String crncy;
        public String marketSectorDes;
        public String idBbUnique;
        public LocalDate announceDt;
        public LocalDate effDt;
        public LocalDate amendedDt;
        public String idBbGlobal;
        public String idBbGlobalCompany;
        public String idBbSecNumDes;
        public String feedSource;
        public Long nFields;
        public String cpNotes;
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class Delete extends Action
    {
        public String cpDeleteReason;
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class DvdCash extends Action
    {
        public LocalDate cpRecordDt;
        public LocalDate cpPayDt;
        public Long cpFreq;
        public BigDecimal cpNetAmt;
        public BigDecimal cpTaxAmt;
        public BigDecimal cpGrossAmt;
        public BigDecimal cpFrankedAmt;
        public String cpDvdCrncy;
        public String cpDvdTyp;
        public BigDecimal cpSpplAmt;
        public BigDecimal cpForeignAmt;
        public BigDecimal cpParPct;
        public String cpStockOpt;
        public BigDecimal cpReinvestRatio;
        public BigDecimal cpPx;
        public BigDecimal cpTaxRt;
        public BigDecimal cpAdj;
        public LocalDate cpAdjDt;
        public String cpIndicator;
        public BigDecimal cpDvdDrpDiscount;
        public BigDecimal cpEusdTid;
        public BigDecimal cpEusdTidSw;
        public String cpDistAmtStatus;
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class StockBuy extends Action
    {
        public BigDecimal cpAmt;
        public String cpStockBuyTyp;
        public LocalDate cpCompletedDt;
        public Boolean cpTermFlag;
        public String cpSecTyp;
        public BigDecimal cpPx;
        public BigDecimal cpSh;
        public BigDecimal cpValPurchased;
        public String cpCrncy;
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class Acquis extends Action
    {
        public String cpFlag;
        public String cpTkr;
        public String cpName;
        public Long cpIdBbComp;
        public String cpUnit;
        public BigDecimal cpTotAmt;
        public String cpAcquisTyp;
        public BigDecimal cpCash;
        public String cpCashFlag;
        public BigDecimal cpSh;
        public String cpShFlag;
        public BigDecimal cpDebt;
        public String cpDebtFlag;
        public String cpStat;
        public String cpDtFlag;
        public String cpInitOfferPremium;
        public String cpCurPremium;
        public BigDecimal cpPctOwned;
        public BigDecimal cpPctSought;
        public String cpUnsolicited;
        public String cpDetailFlag;
        public String cpCrncy;
        public BigDecimal cpCashVal;
        public BigDecimal cpArbitrageProf;
        public BigDecimal cpCurTotVal;
        public Long cpAdvisorsNum;
        public String cpShFractional;
        public String cpAdvisors;
        public String cpAcqTkr;
        public String cpAcqIdBbGlobal;
        public String cpAcqIdBbGlobalCompany;
        public String cpAcqIdBbSecNumDes;
        public String cpAcqFeedSource;
        public String cpTargetTkr;
        public String cpTargetIdBbGlobal;
        public String cpTargetIdBbGlobalCompany;
        public String cpTargetIdBbSecNumDes;
        public String cpTargetFeedSource;
    }
}
