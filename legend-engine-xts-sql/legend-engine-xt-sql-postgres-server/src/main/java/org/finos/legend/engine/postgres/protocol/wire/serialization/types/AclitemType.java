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

import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class AclitemType extends PGType<Object>
{
    public static final AclitemType INSTANCE = new AclitemType();

    AclitemType()
    {
        super(1033, 12, -1, "aclitem");
    }

    @Override
    public int typArray()
    {
        return PGArray.ACLTYPE_ARRAY.oid();
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
    public int writeAsBinary(ByteBuf buffer, Object value)
    {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public Array readBinaryValue(ByteBuf buffer, int valueLength)
    {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    byte[] encodeAsUTF8Text(Object array)
    {
        try
        {
            List<Byte> encodedValues = new ArrayList<>();
            encodedValues.add((byte) '{');
            addAll(encodedValues, array.toString().getBytes(StandardCharsets.UTF_8));
            encodedValues.add((byte) '}');
            return Bytes.toArray(encodedValues);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void addAll(List<Byte> encodedValues, byte[] bytes)
    {
        for (byte aByte : bytes)
        {
            encodedValues.add(aByte);
        }
    }

    @Override
    Array decodeUTF8Text(byte[] bytes)
    {
        throw new RuntimeException("Not implemented yet");
    }
}

