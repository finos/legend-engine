package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.BasicRawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.NoValuesRawFlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BloombergCorporateActionsFileReadDriver<T> extends AbstractBloombergReadDriver<T>
{
    public static final String ID = "BloombergCorporateActionsFile";
    public static final String INCLUDE_NO_ACTION_RECORDS = "includeNoActionRecords";

    private static final String FIELD_SECURITY = "SECURITY";
    private static final String FIELD_ID_BB_COMPANY = "ID_BB_COMPANY";
    private static final String FIELD_ID_BB_SECURITY = "ID_BB_SECURITY";
    private static final String FIELD_R_CODE = "R_CODE";
    private static final String FIELD_ID_ACTION = "ID_ACTION";
    private static final String FIELD_MNEMONIC = "MNEMONIC";
    private static final String FIELD_ACTION_FLAG = "ACTION_FLAG";
    private static final String FIELD_ID_BB_GLOBAL_COMPANY_NAME = "ID_BB_GLOBAL_COMPANY_NAME";
    private static final String FIELD_SECURITY_ID_TYP = "SECURITY_ID_TYP";
    private static final String FIELD_SECURITY_ID = "SECURITY_ID";
    private static final String FIELD_CRNCY = "CRNCY";
    private static final String FIELD_MARKET_SECTOR_DES = "MARKET_SECTOR_DES";
    private static final String FIELD_ID_BB_UNIQUE = "ID_BB_UNIQUE";
    private static final String FIELD_ANNOUNCE_DT = "ANNOUNCE_DT";
    private static final String FIELD_EFF_DT = "EFF_DT";
    private static final String FIELD_AMENDED_DT = "AMENDED_DT";
    private static final String FIELD_ID_BB_GLOBAL = "ID_BB_GLOBAL";
    private static final String FIELD_ID_BB_GLOBAL_COMPANY = "ID_BB_GLOBAL_COMPANY";
    private static final String FIELD_ID_BB_SEC_NUM_DES = "ID_BB_SEC_NUM_DES";
    private static final String FIELD_FEED_SOURCE = "FEED_SOURCE";
    private static final String FIELD_N_FIELDS = "N_FIELDS";

    private static final String[] UNIVERSAL_FIELDS = new String[]{
            FIELD_SECURITY,
            FIELD_ID_BB_COMPANY,
            FIELD_ID_BB_SECURITY,
            FIELD_R_CODE
    };
    private static final String[] COMMON_CA_FIELDS = new String[]{
            FIELD_ID_ACTION,
            FIELD_MNEMONIC,
            FIELD_ACTION_FLAG,
            FIELD_ID_BB_GLOBAL_COMPANY_NAME,
            FIELD_SECURITY_ID_TYP,
            FIELD_SECURITY_ID,
            FIELD_CRNCY,
            FIELD_MARKET_SECTOR_DES,
            FIELD_ID_BB_UNIQUE,
            FIELD_ANNOUNCE_DT,
            FIELD_EFF_DT,
            FIELD_AMENDED_DT,
            FIELD_ID_BB_GLOBAL,
            FIELD_ID_BB_GLOBAL_COMPANY,
            FIELD_ID_BB_SEC_NUM_DES,
            FIELD_FEED_SOURCE,
            FIELD_N_FIELDS
    };
    private static final int N_FIELDS_INDEX = Arrays.asList(COMMON_CA_FIELDS).indexOf(FIELD_N_FIELDS) + UNIVERSAL_FIELDS.length;
    private static final int FIXED_CA_LENGTH = UNIVERSAL_FIELDS.length + COMMON_CA_FIELDS.length;
    private final boolean includeNoActionRecords;

    private final FlatDataProcessingContext context;
    //private final CheckedParsedDataFactory factory;

    private long recordNumber;

    BloombergCorporateActionsFileReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context);
        this.includeNoActionRecords = FlatDataUtils.getBoolean(section.getSectionProperties(), INCLUDE_NO_ACTION_RECORDS);
        this.context = context;
        //this.factory = factory();
    }

    @Override
    public String getId()
    {
        return BloombergFileReadDriver.ID;
    }

    @Override
    public void start()
    {
        super.start();
        findStartOfFile();
        parseMetadataUntil(START_OF_DATA);
        setupParsing();
    }

    //@Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        LineReader.Line line = nextLine();
        if (line.getText().equals(END_OF_DATA))
        {
            return finish();
        }

        String[] values = line.getText().split(FIELD_SEPARATOR_REGEXP);
        List<IDefect> defects = new ArrayList<>();
        RawFlatData unparsed;
        if (values.length == UNIVERSAL_FIELDS.length)
        {
            if (!includeNoActionRecords)
            {
                return Collections.emptyList();
            }
            else
            {
                unparsed = BasicRawFlatData.newRecord(++recordNumber, line.getLineNumber(), line.getText(), Arrays.asList(UNIVERSAL_FIELDS), Arrays.asList(values));
                Optional<IChecked<T>> parsed = null; //factory.make(unparsed, defects);
                return parsed.map(Collections::singletonList).orElseGet(Collections::emptyList);
            }
        }
        else if (values.length >= FIXED_CA_LENGTH)
        {
            try
            {
                int nFields = Integer.parseInt(values[N_FIELDS_INDEX]);
                int expectedValuesCount = UNIVERSAL_FIELDS.length + COMMON_CA_FIELDS.length + nFields * 2;
                if (values.length == expectedValuesCount)
                {
                    int size = FIXED_CA_LENGTH + nFields;
                    String[] caHeads = new String[size];
                    String[] caValues = new String[size];

                    System.arraycopy(UNIVERSAL_FIELDS, 0, caHeads, 0, UNIVERSAL_FIELDS.length);
                    System.arraycopy(COMMON_CA_FIELDS, 0, caHeads, UNIVERSAL_FIELDS.length, COMMON_CA_FIELDS.length);
                    System.arraycopy(values, 0, caValues, 0, FIXED_CA_LENGTH);

                    for (int i = FIXED_CA_LENGTH, j = FIXED_CA_LENGTH; i < values.length; i += 2, j++)
                    {
                        caHeads[j] = values[i];
                        caValues[j] = values[i + 1];
                    }
                    unparsed = BasicRawFlatData.newRecord(++recordNumber, line.getLineNumber(), line.getText(), Arrays.asList(caHeads), Arrays.asList(caValues));
                }
                else
                {
                    unparsed = new NoValuesRawFlatData(++recordNumber, line.getLineNumber(), line.getText());
                    defects.add(BasicDefect.newInvalidInputCriticalDefect("Badly formed line, expected " + expectedValuesCount + " but got " + values.length, context.getDefiningPath()));
                }
            }
            catch (Exception e)
            {
                unparsed = new NoValuesRawFlatData(++recordNumber, line.getLineNumber(), line.getText());
                defects.add(BasicDefect.newInvalidInputCriticalDefect("Unable to interpret line: " + e.getMessage(), context.getDefiningPath()));
            }

            Optional<IChecked<T>> parsed = null; //factory.make(unparsed, defects);
            return parsed.map(Collections::singletonList).orElseGet(Collections::emptyList);
        }
        else
        {
            unparsed = new NoValuesRawFlatData(++recordNumber, line.getLineNumber(), line.getText());
            defects.add(BasicDefect.newInvalidInputCriticalDefect("Badly formed line, meaningless number of fields", context.getDefiningPath()));
            Optional<IChecked<T>> parsed = null; //factory.make(unparsed, defects);
            return parsed.map(Collections::singletonList).orElseGet(Collections::emptyList);
        }
    }
}

