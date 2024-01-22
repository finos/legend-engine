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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.h2.tools.SimpleResultSet;
import org.h2.value.Value;
import org.h2.value.ValueArray;
import org.h2.value.ValueBigint;
import org.h2.value.ValueBoolean;
import org.h2.value.ValueDouble;
import org.h2.value.ValueInteger;
import org.h2.value.ValueNull;
import org.h2.value.ValueReal;
import org.h2.value.ValueVarchar;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class LegendH2Extensions
{
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapper();

    public static Value legend_h2_extension_json_navigate(Value json, Value property, Value arrayIndex) throws Exception
    {
        if (json == ValueNull.INSTANCE)
        {
            return ValueNull.INSTANCE;
        }

        Object res;
        if (arrayIndex == ValueNull.INSTANCE)
        {
            res = OBJECT_MAPPER.readValue(json.getString(), HashMap.class).get(property.getString());
        }
        else
        {
            ArrayList<?> list = OBJECT_MAPPER.readValue(json.getString(), ArrayList.class);
            res = arrayIndex.getInt() < list.size() ? list.get(arrayIndex.getInt()) : null;
        }

        if (res == null)
        {
            return ValueNull.INSTANCE;
        }
        else if (res instanceof Map || res instanceof List)
        {
            return ValueVarchar.get(OBJECT_MAPPER.writeValueAsString(res));
        }
        else if (res instanceof String)
        {
            return ValueVarchar.get((String) res);
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
            return ValueReal.get((float) res);
        }
        else if (res instanceof Integer)
        {
            return ValueInteger.get((int) res);
        }
        else if (res instanceof Long)
        {
            return ValueBigint.get((long) res);
        }

        throw new RuntimeException("Unsupported value in H2 extension function");

    }

    public static Value legend_h2_extension_json_parse(Value json) throws Exception
    {
        if (json == ValueNull.INSTANCE)
        {
            return ValueNull.INSTANCE;
        }

        // Ensure validity of JSON
        Object res;
        try
        {
            res = OBJECT_MAPPER.readValue(json.getString(), HashMap.class);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException("Unable to parse json as a Map. Content: '" + json.getString() + "'. Error: '" + e.getMessage() + "'");
        }

        return ValueVarchar.get(OBJECT_MAPPER.writeValueAsString(res));
    }

    public static String legend_h2_extension_base64_decode(String string)
    {
        return string == null ? null : new String(Base64.decodeBase64(string));
    }

    public static String legend_h2_extension_base64_encode(String string)
    {
        return string == null ? null : Base64.encodeBase64URLSafeString(string.getBytes(StandardCharsets.UTF_8));
    }

    public static String legend_h2_extension_reverse_string(String string)
    {
        return string == null ? null : new StringBuilder(string).reverse().toString();
    }

    public static String legend_h2_extension_split_part(String string, String token, Integer part)
    {
        if (part < 1)
        {
            throw new IllegalArgumentException("Split part must be greater than zero");
        }

        if (string == null)
        {
            return null;
        }

        String[] parts = StringUtils.split(string, token);
        int readjustedPart = part - 1;

        return parts.length > readjustedPart ? parts[readjustedPart] : null;
    }

    private static HashSet<Object> extractProperty(HashSet<Object> resultSet, Object pathToExtract)
    {
        HashSet<Object> res = new HashSet<>();
        if (pathToExtract instanceof String)
        {
            String property = (String) pathToExtract;
            if (property.equals("*"))
            {
                for (Object r: resultSet)
                {
                    if (!(r instanceof Iterable))
                    {
                        continue;
                    }
                    for (Object o : (Iterable<?>) r)
                    {
                        res.add(o);
                    }
                }
            }
            else
            {
                for (Object r: resultSet)
                {
                    try
                    {
                        Object o = ((HashMap)(r)).get(property);
                        if (o != null)
                        {
                            res.add(o);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace(); // don't stop execution
                    }
                }
            }
        }
        else
        {
            int index = (int) pathToExtract;
            for (Object r: resultSet)
            {
                try
                {
                    res.add(((ArrayList)(r)).get(index));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        }
        return res;
    }

    public static ResultSet legend_h2_extension_flatten_array(Connection conn, String tableName, String toFlattenColumnName, ValueArray jsonPaths)
    {
        try
        {
            String sql = String.format("select distinct %s as flat from %s", toFlattenColumnName, tableName);
            ResultSet resultSet = conn.createStatement().executeQuery(sql);
            HashSet<Object> toFlatten = new HashSet<>();
            while (resultSet.next())
            {
                String json = resultSet.getString("flat");
                toFlatten.add(OBJECT_MAPPER.readValue(json, HashMap.class));
            }

            ArrayList<Object> pathsToExtract = OBJECT_MAPPER.readValue(jsonPaths.getString(), ArrayList.class);

            for (Object path: pathsToExtract)
            {
                toFlatten = extractProperty(toFlatten, path);
            }

            SimpleResultSet flattenedResultSet = new SimpleResultSet();

            flattenedResultSet.addColumn("__INPUT__", Types.VARCHAR, 1000, 0); // using the original array as joinKey

            // use first non-null object to infer the type of value
            boolean resolvedFlattenedType = false;

            for (Object o: toFlatten)
            {
                if (!(o instanceof Iterable))
                {
                    continue;
                }
                for (Object value : (Iterable<?>) o)
                {
                    if (value instanceof Map || value instanceof List || value instanceof String)
                    {
                        flattenedResultSet.addColumn("VALUE", Types.VARCHAR, 1000, 0);
                    }
                    else if (value instanceof Boolean)
                    {
                        flattenedResultSet.addColumn("VALUE", Types.BOOLEAN, 0, 0);
                    }
                    else if (value instanceof Double || value instanceof Float || value instanceof BigDecimal)
                    {
                        flattenedResultSet.addColumn("VALUE", Types.DOUBLE, 20, 20);
                    }
                    else if (value instanceof Integer || value instanceof Long)
                    {
                        flattenedResultSet.addColumn("VALUE", Types.BIGINT, 0, 0);
                    }
                    else
                    {
                        throw new RuntimeException("unsupported data type in h2 extension");
                    }
                    resolvedFlattenedType = true;
                    break;
                }
                if (resolvedFlattenedType)
                {
                    break;
                }
            }

            if (!resolvedFlattenedType)
            {
                flattenedResultSet.addColumn("VALUE", Types.VARCHAR, 1000, 0);
                return flattenedResultSet;
            }

            for (Object o: toFlatten)
            {
                if (!(o instanceof Iterable))
                {
                    continue;
                }
                for (Object value : (Iterable<?>) o)
                {
                    if (value instanceof Map || value instanceof List)
                    {
                        flattenedResultSet.addRow(Arrays.asList(OBJECT_MAPPER.writeValueAsString(o), OBJECT_MAPPER.writeValueAsString(value)).toArray());
                    }
                    else
                    {
                        flattenedResultSet.addRow(Arrays.asList(OBJECT_MAPPER.writeValueAsString(o), value).toArray());
                    }
                }
            }
            return flattenedResultSet;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
