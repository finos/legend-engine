/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package org.finos.legend.engine.postgres.protocol.wire.serialization.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class JsonType extends PGType<Object>
{

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final JsonType INSTANCE = new JsonType();
    static final int OID = 114;

    private static final int TYPE_LEN = -1;
    private static final int TYPE_MOD = -1;

    private JsonType()
    {
        super(OID, TYPE_LEN, TYPE_MOD, "json");
    }

    @Override
    public int typArray()
    {
        return PGArray.JSON_ARRAY.oid();
    }

    @Override
    public String typeCategory()
    {
        return TypeCategory.USER_DEFINED_TYPES.code();
    }

    @Override
    public String type()
    {
        return Type.BASE.code();
    }

    @Override
    public int writeAsBinary(ByteBuf buffer, Object value)
    {
        byte[] bytes = encodeAsUTF8Text(value);
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
        return INT32_BYTE_SIZE + bytes.length;
    }

    @Override
    protected byte[] encodeAsUTF8Text(Object value)
    {
        try
        {
            if (value instanceof String)
            {
                String str = (String) value;
                // If the string is already valid JSON (object or array), send as-is
                if (isJsonObjectOrArray(str))
                {
                    // Re-serialize to normalize (compact) the JSON
                    Object parsed = OBJECT_MAPPER.readValue(str, Object.class);
                    return OBJECT_MAPPER.writeValueAsBytes(parsed);
                }
            }
            return OBJECT_MAPPER.writeValueAsBytes(value);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to encode value as JSON", e);
        }
    }

    private static boolean isJsonObjectOrArray(String str)
    {
        if (str.isEmpty())
        {
            return false;
        }
        char first = str.charAt(0);
        return first == '{' || first == '[';
    }

    @Override
    public Object readBinaryValue(ByteBuf buffer, int valueLength)
    {
        byte[] bytes = new byte[valueLength];
        buffer.readBytes(bytes);
        return decodeUTF8Text(bytes);
    }

    @Override
    Object decodeUTF8Text(byte[] bytes)
    {
        try
        {
            return OBJECT_MAPPER.readValue(bytes, Object.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to decode JSON value", e);
        }
    }
}
