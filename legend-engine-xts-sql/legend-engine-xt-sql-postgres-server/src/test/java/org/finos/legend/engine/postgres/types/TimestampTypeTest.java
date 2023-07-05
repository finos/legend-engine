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

import java.nio.charset.StandardCharsets;
import junit.framework.TestCase;
import org.junit.Test;

public class TimestampTypeTest extends TestCase
{
    @Test
    public void testEncodeAsUTF8Text()
    {
        assertEquals("2023-02-20 00:00:00.000+00",
                new String(TimestampType.INSTANCE.encodeAsUTF8Text(1676851200000L), StandardCharsets.UTF_8));
        assertEquals("0667-01-01 00:00:00.000+00 BC",
                new String(TimestampType.INSTANCE.encodeAsUTF8Text(-83184105600000L), StandardCharsets.UTF_8));
    }



}