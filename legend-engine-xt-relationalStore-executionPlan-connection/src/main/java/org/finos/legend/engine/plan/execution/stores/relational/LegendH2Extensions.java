//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.h2.value.Value;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueDouble;
import org.h2.value.ValueFloat;
import org.h2.value.ValueInt;
import org.h2.value.ValueLong;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class LegendH2Extensions
{
    public static Value legend_h2_extension_json_navigate(Value json, Value property, Value arrayIndex) throws Exception
    {
        if (json == ValueNull.INSTANCE)
        {
            return ValueNull.INSTANCE;
        }

        ObjectMapper mapper = new ObjectMapper();

        Object res;
        if (arrayIndex == ValueNull.INSTANCE)
        {
            res = mapper.readValue(json.getString(), HashMap.class).get(property.getString());
        }
        else
        {
            ArrayList<?> list = mapper.readValue(json.getString(), ArrayList.class);
            res = arrayIndex.getInt() < list.size() ? list.get(arrayIndex.getInt()) : null;
        }

        if (res == null)
        {
            return ValueNull.INSTANCE;
        }
        else if (res instanceof Map || res instanceof List)
        {
            return ValueString.get(mapper.writeValueAsString(res));
        }
        else if (res instanceof String)
        {
            return ValueString.get((String) res);
        }
        else if (res instanceof Boolean)
        {
            return ValueBoolean.get((boolean) res);
        }
        else if (res instanceof Double)
        {
            return ValueDouble.get((double) res);
        }
        else if (res instanceof Float)
        {
            return ValueFloat.get((float) res);
        }
        else if (res instanceof Integer)
        {
            return ValueInt.get((int) res);
        }
        else if (res instanceof Long)
        {
            return ValueLong.get((long) res);
        }

        throw new RuntimeException("Unsupported value in H2 extension function");

    }

    public static String legend_h2_extension_base64_decode(String string)
    {
        return string == null ? null : new String(Base64.decodeBase64(string));
    }

    public static String legend_h2_extension_base64_encode(String string)
    {
        return string == null ? null : Base64.encodeBase64URLSafeString(string.getBytes(StandardCharsets.UTF_8));
    }

    public static String legend_h2_extension_split_part(String string, String separator, Integer position)
    {
        if (position <= 0 || separator.equals(""))
        {
            return null;
        }
        else
        {
            String[] parts = string.split(separator, -1);
            if (position > parts.length)
            {
                return "";
            }
            else
            {
                return parts[position - 1];
            }
        }
    }
}
