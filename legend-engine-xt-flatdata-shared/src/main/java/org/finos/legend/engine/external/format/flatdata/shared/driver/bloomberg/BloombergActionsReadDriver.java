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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.HeadedFlatDataFactory;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandlerRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.ObjectVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.VariableType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.*;

public class BloombergActionsReadDriver<T> extends AbstractBloombergReadDriver<T>
{
    static final String INCLUDE_NO_ACTION_RECORDS = "includeNoActionRecords";
    static final FlatDataVariable VARIABLE_ACTIONS_RECORD = new FlatDataVariable("bloombergActionRecord", VariableType.Object);

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

    static final String[] UNIVERSAL_FIELDS = new String[]{
            FIELD_SECURITY,
            FIELD_ID_BB_COMPANY,
            FIELD_ID_BB_SECURITY,
            FIELD_R_CODE
    };
    private static final String[] CA_FIELDS = new String[]{
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
    static final String[] COMMON_FIELDS;

    static
    {
        COMMON_FIELDS = new String[UNIVERSAL_FIELDS.length + CA_FIELDS.length];
        System.arraycopy(UNIVERSAL_FIELDS, 0, COMMON_FIELDS, 0, UNIVERSAL_FIELDS.length);
        System.arraycopy(CA_FIELDS, 0, COMMON_FIELDS, UNIVERSAL_FIELDS.length, CA_FIELDS.length);
    }

    static final List<String> NULL_STRINGS = Arrays.asList(NULL_STRING, " ");

    private static final int MNEMONIC_INDEX = Arrays.asList(COMMON_FIELDS).indexOf(FIELD_MNEMONIC);
    private static final int ACTION_FLAG_INDEX = Arrays.asList(COMMON_FIELDS).indexOf(FIELD_ACTION_FLAG);
    static final int N_FIELDS_INDEX = Arrays.asList(COMMON_FIELDS).indexOf(FIELD_N_FIELDS);
    private final boolean includeNoActionRecords;

    private final FlatDataProcessingContext context;
    private final ObjectVariable<FlatDataRecordType> actionsRecordType;
    private long recordNumber = 0;
    private ExtendedDataFactories<T> dataFactories;
    private List<BloombergExtendActionReadDriver<T>> extendingDrivers = new ArrayList<>();

    BloombergActionsReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context, "getactions");
        this.includeNoActionRecords = FlatDataUtils.getBoolean(section.getSectionProperties(), INCLUDE_NO_ACTION_RECORDS);
        this.actionsRecordType = ObjectVariable.reference(context, VARIABLE_ACTIONS_RECORD);
        this.context = context;
    }

    @Override
    public String getId()
    {
        return BloombergActionsDriverDescription.ID;
    }

    @Override
    public void start()
    {
        super.start();
        findStartOfFile();
        parseMetadataUntil(START_OF_DATA);
        setupParsing();

        List<FieldHandler> fieldHandlers = this.commonDataHandler.computeFieldHandlers(HeadedFlatDataFactory::getDynamicRawDataAccessor);
        ParsedFlatDataToObject<? extends T> objectFactory = context.createToObjectFactory(new FieldHandlerRecordType(section.getRecordType(), fieldHandlers));
        this.dataFactories = new ExtendedDataFactories<>(context.getDefiningPath(), objectFactory, fieldHandlers);

        actionsRecordType.set(section.getRecordType());
        extendingDrivers.forEach(BloombergExtendActionReadDriver::start);
    }

    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        LineReader.Line line = nextLine();
        if (line.getText().equals(END_OF_DATA))
        {
            return finish();
        }

        String[] values = line.getText().split(FIELD_SEPARATOR_REGEXP);
        if (values.length == UNIVERSAL_FIELDS.length && !includeNoActionRecords)
        {
            return Collections.emptyList();
        }

        BloombergExtendActionReadDriver<T> extender = null;
        Optional<IChecked<T>> parsed;
        if (values.length > UNIVERSAL_FIELDS.length)
        {
            String actionFlag = values[ACTION_FLAG_INDEX];
            String mnemonic = values[MNEMONIC_INDEX];
            for (BloombergExtendActionReadDriver<T> driver : extendingDrivers)
            {
                if (extender == null && driver.matches(actionFlag, mnemonic))
                {
                    extender = driver;
                }
            }
        }

        parsed = (extender == null)
                ? dataFactories.createParsed(values, line, ++recordNumber)
                : extender.createParsed(values, line, ++recordNumber);
        return parsed.map(Collections::singletonList).orElseGet(Collections::emptyList);
    }


    void addActionDriver(BloombergExtendActionReadDriver<T> actionDriver)
    {
        this.extendingDrivers.add(actionDriver);
    }
}

