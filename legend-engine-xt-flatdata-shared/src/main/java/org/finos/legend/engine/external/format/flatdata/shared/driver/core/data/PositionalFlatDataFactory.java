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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PositionalFlatDataFactory<T> extends AbstractDataFactory<T>
{
    public PositionalFlatDataFactory(String definingPath, List<String> nullStrings)
    {
        super(definingPath, nullStrings);
    }

    public Function<RawFlatData, String> getRawDataAccessor(FlatDataRecordField field)
    {
        int index = Integer.parseInt(field.getAddress()) - 1;
        return (RawFlatData raw) -> ((PositionalRawFlatData) raw).getRawValue(index);
    }

    public RawFlatData createRawFlatData(long recordNumber, LineReader.Line line, List<String> values)
    {
        return createRawFlatData(recordNumber, line, Objects.requireNonNull(values).toArray(new String[values.size()]));
    }

    public RawFlatData createRawFlatData(long recordNumber, LineReader.Line line, String[] values)
    {
        return new PositionalRawFlatData(nullStrings, recordNumber, line.getLineNumber(), line.getText(), values);
    }

    private static class PositionalRawFlatData extends AbstractRawFlatData
    {
        private final List<String> nullStrings;
        private final String[] values;

        PositionalRawFlatData(List<String> nullStrings, long number, long lineNumber, String record, String[] values)
        {
            super(number, lineNumber, record);
            this.nullStrings = nullStrings;
            this.values = values;
        }

        @Override
        protected List<RawFlatDataValue> createValues()
        {
            return IntStream.range(0, values.length).mapToObj(PositionalValue::new).collect(Collectors.toList());
        }

        String getRawValue(int index)
        {
            String value = index >= values.length ? null : values[index];
            return value != null && nullStrings.contains(value) ? null : value;
        }

        private class PositionalValue implements RawFlatDataValue
        {
            private final int index;

            PositionalValue(int index)
            {
                this.index = index;
            }

            @Override
            public Object getAddress()
            {
                return (long) index + 1;
            }

            @Override
            public String getRawValue()
            {
                return values[index];
            }

            @Override
            public String toString()
            {
                return "PositionalValue{label=" + getAddress() + ", value=" + getRawValue() + '}';
            }
        }
    }
}
