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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.data;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HeadedFlatDataFactory<T>
{
    private final String[] headings;
    private String definingPath;
    private final List<String> nullStrings;

    public HeadedFlatDataFactory(List<String> headings, String definingPath, List<String> nullStrings)
    {
        this(headings.toArray(new String[0]), definingPath, nullStrings);
    }

    public HeadedFlatDataFactory(String[] headings, String definingPath, List<String> nullStrings)
    {
        this.headings = headings;
        this.definingPath = definingPath;
        this.nullStrings = nullStrings;
    }

    public List<String> headings()
    {
        return Arrays.asList(headings);
    }

    public int headingsSize()
    {
        return headings.length;
    }

    public boolean containsHeading(String heading)
    {
        return headings().contains(heading);
    }

    public Function<RawFlatData, String> getRawDataAccessor(FlatDataRecordField field)
    {
        for (int i=0; i<headings.length; i++)
        {
            if (headings[i].equals(field.getLabel()))
            {
                int index = i;
                return (RawFlatData raw) -> ((HeadedRawFlatData) raw).getRawValue(index);
            }
        }
        return (RawFlatData raw) -> null;
    }

    public RawFlatData createRawFlatData(long recordNumber, LineReader.Line line, List<String> values)
    {
        return createRawFlatData(recordNumber, line, Objects.requireNonNull(values).toArray(new String[values.size()]));
    }

    public RawFlatData createRawFlatData(long recordNumber, LineReader.Line line, String[] values)
    {
        return new HeadedRawFlatData(nullStrings, headings, recordNumber, line.getLineNumber(), line.getText(), values);
    }

    public Optional<IChecked<T>> createParsed(IChecked<RawFlatData> unparsed, List<FieldHandler> fieldHandlers, ParsedFlatDataToObject<? extends T> objectFactory)
    {
        if (unparsed.getDefects().stream().anyMatch(d -> d.getEnforcementLevel() == EnforcementLevel.Critical))
        {
            return Optional.of(BasicChecked.newChecked(null, unparsed.getValue(), unparsed.getDefects()));
        }
        else if (unparsed.getValue() == null)
        {
            return Optional.empty();
        }
        else
        {
            RawFlatData rawData = unparsed.getValue();
            LazyParsedFlatData parseData = new LazyParsedFlatData(rawData, unparsed.getDefects(), fieldHandlers, definingPath);

            for (FieldHandler handler : fieldHandlers)
            {
                if (handler.hasRawValue(rawData))
                {
                    String errorMessage = handler.validate(rawData);
                    if (errorMessage == null)
                    {
                        parseData.setVerified(handler);
                    }
                    else
                    {
                        parseData.addInvalidInputDefect(handler, errorMessage);
                    }
                }
                else if (!handler.getField().isOptional())
                {
                    parseData.addMissingValueDefect(handler);
                }
                else
                {
                    parseData.setMissing(handler);
                }
            }

            List<IDefect> defects = parseData.getDefects();
            IChecked<? extends T> checkedValue = objectFactory.makeChecked(parseData);
            defects.addAll(checkedValue.getDefects());
            T value = checkedValue.getValue();
            if (objectFactory.isReturnable())
            {
                if (defects.stream().anyMatch(d -> d.getEnforcementLevel() == EnforcementLevel.Critical))
                {
                    value = null;
                }
                return Optional.of(BasicChecked.newChecked(value, unparsed.getValue(), defects));
            }
            else if(defects.isEmpty())
            {
                return Optional.empty();
            }
            else
            {
                defects.add(BasicDefect.newNonReturnableDefect(definingPath));
                return Optional.of(BasicChecked.newChecked(null, new NonRecordRawFlatData(unparsed.getValue()), defects));
            }
        }
    }

    private static class HeadedRawFlatData extends AbstractRawFlatData
    {
        private final List<String> nullStrings;
        private final String[] headings;
        private final String[] values;

        HeadedRawFlatData(List<String> nullStrings, String[] headings, long number, long lineNumber, String record, String[] values)
        {
            super(number, lineNumber, record);
            this.nullStrings = nullStrings;
            this.headings = headings;
            this.values = values;
        }

        @Override
        protected List<RawFlatDataValue> createValues()
        {
            int limit = Math.min(headings.length, values.length);
            return IntStream.range(0, limit).mapToObj(HeadedValue::new).collect(Collectors.toList());
        }

        String getRawValue(int index)
        {
            String value =  index >= values.length ? null : values[index];
            return value != null && nullStrings.contains(value) ? null : value;
        }

        private class HeadedValue implements RawFlatDataValue
        {
            private final int index;

            HeadedValue(int index)
            {
                this.index = index;
            }

            @Override
            public Object getAddress()
            {
                return headings[index];
            }

            @Override
            public String getRawValue()
            {
                return values[index];
            }

            @Override
            public String toString()
            {
                return "HeadedValue{label=" + getAddress() + ", value=" + getRawValue() + '}';
            }
        }
    }

    public static Function<RawFlatData, String> getDynamicRawDataAccessor(FlatDataRecordField field)
    {
        final String label = field.getLabel();
        return (RawFlatData raw) ->
        {
            HeadedRawFlatData headedRaw = (HeadedRawFlatData) raw;
            for (int i=0; i<headedRaw.headings.length; i++)
            {
                if (headedRaw.headings[i].equals(label))
                {
                    return headedRaw.getRawValue(i);
                }
            }
            return null;
        };
    }
}
