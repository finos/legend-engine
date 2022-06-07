// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public interface ParsedFlatData
{
    boolean hasStringValue(FlatDataRecordField field);

    boolean hasBooleanValue(FlatDataRecordField field);

    boolean hasLongValue(FlatDataRecordField field);

    boolean hasDoubleValue(FlatDataRecordField field);

    boolean hasBigDecimalValue(FlatDataRecordField field);

    boolean hasLocalDateValue(FlatDataRecordField field);

    boolean hasInstantValue(FlatDataRecordField field);

    String getString(FlatDataRecordField field);

    boolean getBoolean(FlatDataRecordField field);

    long getLong(FlatDataRecordField field);

    double getDouble(FlatDataRecordField field);

    BigDecimal getBigDecimal(FlatDataRecordField field);

    LocalDate getLocalDate(FlatDataRecordField field);

    Instant getInstant(FlatDataRecordField field);
}
