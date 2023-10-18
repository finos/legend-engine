// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.serialization;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

class ValueTransformer
{
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatterWithZ = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnZ");
    private final ZoneOffset offset = ZoneOffset.UTC;

    protected ValueTransformer()
    {

    }

    protected String transformWrappedRelationalValueForJSON(Object relationalValue, Function<Object, String> transformer)
    {
        if (relationalValue instanceof Timestamp)
        {
            return "\"" + timeFormatterWithZ.format(((Timestamp) relationalValue).toInstant().atOffset(offset)) + "\"";

        }
        else if (relationalValue instanceof java.sql.Date)
        {
            return "\"" + ((java.sql.Date) relationalValue).toLocalDate().format(dateFormat) + "\"";
        }
        else
        {
            return transformer.apply(relationalValue);
        }

    }

    protected Object transformRelationalValue(Object relationalValue, Function<Object, ?> transformer)
    {
        if (relationalValue instanceof Timestamp)
        {
            return timeFormatterWithZ.format(((Timestamp) relationalValue).toInstant().atOffset(offset));

        }
        else if (relationalValue instanceof java.sql.Date)
        {
            return ((java.sql.Date) relationalValue).toLocalDate().format(dateFormat);
        }
        else
        {
            return transformer.apply(relationalValue);
        }
    }

}
