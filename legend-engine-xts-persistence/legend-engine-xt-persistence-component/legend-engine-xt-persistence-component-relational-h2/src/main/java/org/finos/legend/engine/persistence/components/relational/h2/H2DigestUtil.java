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

package org.finos.legend.engine.persistence.components.relational.h2;

import org.apache.commons.codec.digest.DigestUtils;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class H2DigestUtil
{
    private static final byte[] EMPTY_STRING = new byte[] { 88 };

    public static void registerMD5Udf(JdbcHelper sink, String UdfName)
    {
        sink.executeStatement("CREATE ALIAS " + UdfName + " FOR \"org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil.MD5\";");
    }

    public static String MD5(String... columnNameAndValueList)
    {
        return calculateMD5Digest(generateRowMap(columnNameAndValueList));
    }

    private static Map<String, Object> generateRowMap(String[] columnNameAndValueList)
    {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < columnNameAndValueList.length; i++)
        {
            map.put(columnNameAndValueList[i], columnNameAndValueList[++i]);
        }
        return map;
    }

    private static String calculateMD5Digest(Map<String, Object> row)
    {
        List<String> fieldNames = row.keySet().stream().sorted().collect(Collectors.toList());
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream))
        {
            fieldNames.stream().forEachOrdered(field ->
            {
                Optional<Object> value = Optional.ofNullable(row.get(field));
                value.ifPresent(v -> writeValueWithFieldName(field, v, dataOutputStream));
            });
            dataOutputStream.flush();
            return DigestUtils.md5Hex(byteArrayOutputStream.toByteArray());
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to create digest", e);
        }
    }

    private static void writeValueWithFieldName(String fieldName, Object value, DataOutputStream dataOutputStream)
    {
        try
        {
            dataOutputStream.writeInt(fieldName.hashCode());
            String stringValue = value.toString();
            if (stringValue == null || stringValue.length() == 0)
            {
                dataOutputStream.write(EMPTY_STRING);
            }
            else
            {
                dataOutputStream.writeBytes(stringValue);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(String.format("Unable to create digest for field [%s]", fieldName), e);
        }
    }
}
