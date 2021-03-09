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

package org.finos.legend.engine.plan.execution.result;

import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.dependencies.store.shared.IReferencedObject;
import org.finos.legend.engine.plan.execution.result.date.EngineDate;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ResultNormalizer
{
    private static final String DEFAULT_DATABASE_TIME_ZONE = "GMT";

    public static Object normalizeToSql(Object o)
    {
        return normalizeToSql(o, DEFAULT_DATABASE_TIME_ZONE);
    }

    public static Object normalizeToSql(Object o, String databaseTimeZone)
    {
        if (o == null)
        {
            return "null";
        }

        if (o instanceof Map)
        {
            return ((Map<?, ?>) o).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> normalizeToSql(e.getValue(), databaseTimeZone)));
        }

        if (o instanceof Iterable)
        {
            return StreamSupport.stream(((Iterable<?>) o).spliterator(), false).map(v -> normalizeToSql(v, databaseTimeZone)).collect(Collectors.toList());
        }

        if (o instanceof PureDate)
        {
            PureDate pureDate = (PureDate) o;
            if (pureDate.hasSubsecond())
            {
                return pureDate.format("[" + databaseTimeZone + "]yyyy-MM-dd HH:mm:ss.SSSSSS");
            }
            if (pureDate.hasSecond())
            {
                return pureDate.format("[" + databaseTimeZone + "]yyyy-MM-dd HH:mm:ss");
            }
            return pureDate.format("[" + databaseTimeZone + "]yyyy-MM-dd");
        }

        if (o instanceof EngineDate)
        {
            return ((EngineDate) o).formatToSql();
        }

        if (o instanceof String)
        {
            return ((String) o).replace("'", "\'");
        }

        if(o instanceof IReferencedObject)
        {
            Field[] fields = o.getClass().getDeclaredFields();
            for(Field f:fields)
            {
                try
                {
                    f.setAccessible(true);
                    f.set(o,normalizeToSql(f.get(o),databaseTimeZone));
                }
                catch (IllegalAccessException e)
                {
                }
            }
            return o;
        }

        return o.toString();
    }
}