// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.dependencies.domain.flatdata;

import org.finos.legend.engine.plan.dependencies.domain.dataQuality.Constrained;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;

import java.math.BigDecimal;
import java.util.List;

public interface ParsedFlatData extends Constrained<ParsedFlatData>
{
    String getTypeName();

    List<ParsedFlatDataValue> getValues();

    String getOneString(Object label);

    Number getOneNumber(Object label);

    long getOneInteger(Object label);

    double getOneFloat(Object label);

    BigDecimal getOneDecimal(Object label);

    boolean getOneBoolean(Object label);

    PureDate getOneDate(Object label);

    PureDate getOneStrictDate(Object label);

    PureDate getOneDateTime(Object label);

    ParsedFlatData getOneRecordType(Object label);

    String getOptionalString(Object label);

    Number getOptionalNumber(Object label);

    Long getOptionalInteger(Object label);

    Double getOptionalFloat(Object label);

    BigDecimal getOptionalDecimal(Object label);

    Boolean getOptionalBoolean(Object label);

    PureDate getOptionalDate(Object label);

    PureDate getOptionalStrictDate(Object label);

    PureDate getOptionalDateTime(Object label);

    ParsedFlatData getOptionalRecordType(Object label);
}
