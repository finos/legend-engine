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
import java.nio.charset.StandardCharsets;

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
                // A json/jsonb column's extracted value (e.g. the result of ->, #>) arrives here
                // as the exact text Postgres itself already produced for it: Postgres's own `json`
                // type preserves the original input text verbatim (including whitespace, key
                // order, duplicate keys), while `jsonb` returns its own canonical form (e.g. a
                // space after every ':' and ','). Either way, it's already correct -- pass it
                // through unchanged rather than re-serializing it (re-serializing through a
                // generic Java Map/List loses whichever of those two conventions applied). Only a
                // genuine non-JSON string -- never valid json/jsonb text on its own -- needs
                // encoding as a JSON string literal here.
                String str = (String) value;
                if (isValidJson(str))
                {
                    return str.getBytes(StandardCharsets.UTF_8);
                }
                return OBJECT_MAPPER.writeValueAsBytes(str);
            }
            return OBJECT_MAPPER.writeValueAsBytes(value);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to encode value as JSON", e);
        }
    }

    private static boolean isValidJson(String str)
    {
        try
        {
            OBJECT_MAPPER.readTree(str);
            return true;
        }
        catch (IOException notJson)
        {
            return false;
        }
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
