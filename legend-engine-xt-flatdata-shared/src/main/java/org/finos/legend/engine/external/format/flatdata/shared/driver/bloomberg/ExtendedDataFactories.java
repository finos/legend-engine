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
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.NoValuesRawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class ExtendedDataFactories<T>
{
    private final LRUFactories<T> factories = new LRUFactories<>();
    private final String definingPath;
    private final ParsedFlatDataToObject<? extends T> objectFactory;
    private final List<FieldHandler> fieldHandlers;
    private final HeadedFlatDataFactory<T> universalDataFactory;

    ExtendedDataFactories(String definingPath, ParsedFlatDataToObject<? extends T> objectFactory, List<FieldHandler> fieldHandlers)
    {
        this.definingPath = definingPath;
        this.objectFactory = objectFactory;
        this.fieldHandlers = fieldHandlers;
        this.universalDataFactory = new HeadedFlatDataFactory<>(BloombergActionsReadDriver.UNIVERSAL_FIELDS, definingPath, BloombergActionsReadDriver.NULL_STRINGS);
    }

    Optional<IChecked<T>> createParsed(String[] valuesIn, LineReader.Line line, long recordNumber)
    {
        if (valuesIn.length == BloombergActionsReadDriver.UNIVERSAL_FIELDS.length)
        {
            RawFlatData raw = universalDataFactory.createRawFlatData(recordNumber, line, valuesIn);
            IChecked<RawFlatData> checkedRaw = BasicChecked.newChecked(raw, null);
            return universalDataFactory.createParsed(checkedRaw, fieldHandlers, objectFactory);
        }
        else
        {
            int nFields = Integer.parseInt(valuesIn[BloombergActionsReadDriver.N_FIELDS_INDEX]);
            int expectedValuesCount = BloombergActionsReadDriver.COMMON_FIELDS.length + nFields * 2;
            if (valuesIn.length == expectedValuesCount)
            {
                int size = BloombergActionsReadDriver.COMMON_FIELDS.length + nFields;
                String[] heads = new String[size];
                String[] values = new String[size];

                System.arraycopy(BloombergActionsReadDriver.COMMON_FIELDS, 0, heads, 0, BloombergActionsReadDriver.COMMON_FIELDS.length);
                System.arraycopy(valuesIn, 0, values, 0, BloombergActionsReadDriver.COMMON_FIELDS.length);

                StringBuilder keyBuilder = new StringBuilder();
                for (int i = BloombergActionsReadDriver.COMMON_FIELDS.length, j = BloombergActionsReadDriver.COMMON_FIELDS.length; i < valuesIn.length; i += 2, j++)
                {
                    heads[j] = valuesIn[i];
                    values[j] = valuesIn[i + 1];
                    keyBuilder.append(valuesIn[i]).append(";");
                }

                String key = keyBuilder.toString();
                HeadedFlatDataFactory<T> dataFactory = factories.computeIfAbsent(key, k -> new HeadedFlatDataFactory<>(heads, definingPath, BloombergActionsReadDriver.NULL_STRINGS));

                RawFlatData raw = dataFactory.createRawFlatData(recordNumber, line, values);
                IChecked<RawFlatData> checkedRaw = BasicChecked.newChecked(raw, null);
                return dataFactory.createParsed(checkedRaw, fieldHandlers, objectFactory);
            }
            else
            {
                List<IDefect> defects = new ArrayList<>();
                RawFlatData raw = new NoValuesRawFlatData(recordNumber, line.getLineNumber(), line.getText());
                defects.add(BasicDefect.newInvalidInputCriticalDefect("Badly formed line, expected " + expectedValuesCount + " but got " + valuesIn.length, definingPath));
                return Optional.of(BasicChecked.newChecked(null, raw, defects));
            }
        }
    }

    private static class LRUFactories<T> extends LinkedHashMap<String, HeadedFlatDataFactory<T>>
    {
        LRUFactories()
        {
            super(137, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, HeadedFlatDataFactory<T>> eldest)
        {
            return size() > 100;
        }
    }
}
