// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.protocol.wire.serialization.types;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public class NameType extends PGType<String>
{
    public static final NameType INSTANCE = new NameType();

    static final int OID = 19;

    private static final int TYPE_LEN = 64;
    private static final int TYPE_MOD = -1;

    NameType()
    {
        super(OID, TYPE_LEN, TYPE_MOD, "name");
    }

    @Override
    public int typArray()
    {
        return PGArray.NAME_ARRAY.oid();
    }

    @Override
    public String typeCategory()
    {
        return TypeCategory.STRING.code();
    }

    @Override
    public String type()
    {
        return Type.BASE.code();
    }

    @Override
    public int writeAsBinary(ByteBuf buffer, String value)
    {
        byte[] bytes = new byte[TYPE_LEN];
        buffer.writeBytes(value.getBytes());
        bytes[value.length()] = 0;
        return TYPE_LEN;
    }

    @Override
    public String readBinaryValue(ByteBuf buffer, int valueLength)
    {
        int length = buffer.bytesBefore((byte) 0x00);

        if (length < 0)
        {
            return null;
        }

        String str = buffer.readCharSequence(length, StandardCharsets.UTF_8).toString();

        buffer.skipBytes(64 - length);

        return str;
    }

    @Override
    byte[] encodeAsUTF8Text(String value)
    {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    String decodeUTF8Text(byte[] bytes)
    {
        return new String(bytes, StandardCharsets.UTF_8);
    }

}

