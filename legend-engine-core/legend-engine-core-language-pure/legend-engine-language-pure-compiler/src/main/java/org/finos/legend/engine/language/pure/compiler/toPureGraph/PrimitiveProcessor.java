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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.apache.commons.lang3.ClassUtils;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormat;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrimitiveProcessor
{
    public static Object process(Object o)
    {
        if (o == null)
        {
            return null;
        }
        if (o instanceof Integer)
        {
            return Long.valueOf((Integer) o);
        }
        if (o instanceof Float)
        {
            return Double.valueOf((Float) o);
        }
        if (o instanceof LocalDate)
        {
            return DateFormat.parseStrictDate(o.toString());
        }
        if (o instanceof LocalDateTime)
        {
            return DateFormat.parseDateTime(o.toString());
        }
        if (ClassUtils.isPrimitiveOrWrapper(o.getClass()) || (o instanceof String) || (o instanceof PureDate))
        {
            return o;
        }
        throw new IllegalArgumentException(o.toString());
    }
}
