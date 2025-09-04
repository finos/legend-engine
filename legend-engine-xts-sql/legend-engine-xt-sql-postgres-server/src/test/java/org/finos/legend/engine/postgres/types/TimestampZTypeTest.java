// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.postgres.types;

import org.junit.Test;
import junit.framework.TestCase;
import java.nio.charset.StandardCharsets;

public class TimestampZTypeTest extends TestCase
{
    @Test
    public void testEncodeAsUTF8Text_AD()
    {
        assertEquals("2023-02-20 00:00:00.000+00",
                new String(TimestampZType.INSTANCE.encodeAsUTF8Text(1676851200000L), StandardCharsets.UTF_8));
    }

    @Test
    public void testEncodeAsUTF8Text_BD()
    {
        assertEquals("0667-01-01 00:00:00.000+00 BC",
                new String(TimestampZType.INSTANCE.encodeAsUTF8Text(-83184105600000L), StandardCharsets.UTF_8));
    }

    @Test
    public void testDecodeUTF8Text_AD()
    {
        String input = "2023-02-20 00:00:00.000+00";
        long expected = 1676851200000L;
        Object result = TimestampZType.INSTANCE.decodeUTF8Text(input.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, result);
    }

    @Test
    public void testDecodeUTF8Text_BD()
    {
        String input = "0667-01-01 00:00:00.000+00 BC";
        long expected = -83184105600000L;
        Object result = TimestampZType.INSTANCE.decodeUTF8Text(input.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, result);
    }
}