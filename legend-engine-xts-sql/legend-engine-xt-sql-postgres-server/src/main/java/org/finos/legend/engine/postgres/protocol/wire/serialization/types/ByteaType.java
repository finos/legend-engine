// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.postgres.protocol.wire.serialization.types;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.finos.legend.engine.shared.core.operational.Assert;

import java.nio.charset.StandardCharsets;

public class ByteaType extends PGType<Object>
{
    static final int OID = 17;
    private static final int ARRAY_OID = 1001;
    private static final int TYPE_LEN = -1;
    private static final int TYPE_MOD = -1;
    private static final String HEX_PREFIX = "\\x";

    public static final ByteaType INSTANCE = new ByteaType();

    private ByteaType()
    {
        super(OID, TYPE_LEN, TYPE_MOD, "bytea");
    }

    @Override
    public int typArray()
    {
        return ARRAY_OID;
    }

    @Override
    public String type()
    {
        return Type.BASE.code();
    }

    @Override
    public String typeCategory()
    {
        return TypeCategory.USER_DEFINED_TYPES.code();
    }

    @Override
    public int writeAsBinary(ByteBuf buffer, Object value)
    {
        byte[] bytes = toBytes(value);
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
        return INT32_BYTE_SIZE + bytes.length;
    }

    @Override
    public Object readBinaryValue(ByteBuf buffer, int valueLength)
    {
        byte[] bytes = new byte[valueLength];
        buffer.readBytes(bytes);
        return bytes;
    }

    @Override
    byte[] encodeAsUTF8Text(Object value)
    {
        return (HEX_PREFIX + ByteBufUtil.hexDump(toBytes(value))).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    Object decodeUTF8Text(byte[] bytes)
    {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (!text.startsWith(HEX_PREFIX))
        {
            throw new IllegalArgumentException("Unsupported bytea text format (expected hex, prefixed with '" + HEX_PREFIX + "'): " + text);
        }
        return ByteBufUtil.decodeHexDump(text, HEX_PREFIX.length(), text.length() - HEX_PREFIX.length());
    }

    private static byte[] toBytes(Object value)
    {
        Assert.assertTrue(value instanceof byte[], () -> "value must be byte[], got: " + value);
        return (byte[]) value;
    }
}
