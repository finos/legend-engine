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

public class AnyType extends PGType<Object>
{
    static final int OID = 2276;
    private static final int TYPE_LEN = 4;
    private static final int TYPE_MOD = -1;

    public static final AnyType INSTANCE = new AnyType();


    private AnyType()
    {
        super(OID, TYPE_LEN, TYPE_MOD, "any");
    }

    @Override
    public String type()
    {
        return Type.BASE.code();
    }

    @Override
    public int typArray()
    {
        return PGArray.ANY_ARRAY.oid();
    }

    @Override
    public String typeCategory()
    {
        return TypeCategory.UNKNOWN.code();
    }

    @Override
    public int writeAsBinary(ByteBuf buffer, Object value)
    {
        int writerIndex = buffer.writerIndex();
        buffer.writeInt(0);
        int bytesWritten = buffer.writeCharSequence(value.toString(), StandardCharsets.UTF_8);
        buffer.setInt(writerIndex, bytesWritten);
        return INT32_BYTE_SIZE + bytesWritten;
    }

    @Override
    public int writeAsText(ByteBuf buffer, Object value)
    {
        return writeAsBinary(buffer, value);
    }

    @Override
    protected byte[] encodeAsUTF8Text(Object value)
    {
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String readTextValue(ByteBuf buffer, int valueLength)
    {
        return readBinaryValue(buffer, valueLength);
    }

    @Override
    public String readBinaryValue(ByteBuf buffer, int valueLength)
    {
        int readerIndex = buffer.readerIndex();
        buffer.readerIndex(readerIndex + valueLength);
        return buffer.toString(readerIndex, valueLength, StandardCharsets.UTF_8);
    }

    @Override
    String decodeUTF8Text(byte[] bytes)
    {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

