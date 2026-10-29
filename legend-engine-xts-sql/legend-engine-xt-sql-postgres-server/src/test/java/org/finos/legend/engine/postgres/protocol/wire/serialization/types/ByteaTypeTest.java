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
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Types;

public class ByteaTypeTest
{
    private static final byte[] VALUE = new byte[]{0x0A, 0x0B, (byte) 0xCA, (byte) 0xFE};

    @Test
    public void testBinarySqlTypesMapToBytea()
    {
        Assertions.assertSame(ByteaType.INSTANCE, PGTypes.get(Types.VARBINARY, null, 0));
        Assertions.assertSame(ByteaType.INSTANCE, PGTypes.get(Types.BINARY, null, 0));
        Assertions.assertEquals(17, ByteaType.INSTANCE.oid());
        Assertions.assertEquals("bytea", ByteaType.INSTANCE.typName());
    }

    @Test
    public void testTextFormatIsPostgresHex()
    {
        Assertions.assertEquals("\\x0a0bcafe", new String(ByteaType.INSTANCE.encodeAsUTF8Text(VALUE), StandardCharsets.UTF_8));
        Assertions.assertArrayEquals(VALUE, (byte[]) ByteaType.INSTANCE.decodeUTF8Text("\\x0a0bcafe".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testTextFormatRejectsNonHexInput()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ByteaType.INSTANCE.decodeUTF8Text("\\001\\002".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testBinaryFormatIsRawBytes()
    {
        ByteBuf buffer = Unpooled.buffer();
        try
        {
            Assertions.assertEquals(4 + VALUE.length, ByteaType.INSTANCE.writeAsBinary(buffer, VALUE));
            Assertions.assertEquals(VALUE.length, buffer.readInt());
            Assertions.assertArrayEquals(VALUE, (byte[]) ByteaType.INSTANCE.readBinaryValue(buffer, VALUE.length));
        }
        finally
        {
            buffer.release();
        }
    }
}
