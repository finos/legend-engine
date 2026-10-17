// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.postgres.protocol.wire.serialization.types;

import java.nio.charset.StandardCharsets;
import junit.framework.TestCase;
import org.junit.Test;

public class JsonTypeTest extends TestCase
{
    @Test
    public void testScalarJsonTextIsNotDoubleEncoded()
    {
        assertEquals("1", encode("1"));
        assertEquals("42", encode("42"));
        assertEquals("true", encode("true"));
        assertEquals("null", encode("null"));
        assertEquals("\"deep\"", encode("\"deep\""));
    }

    @Test
    public void testPlainStringIsEncodedAsJsonStringLiteral()
    {
        assertEquals("\"hello\"", encode("hello"));
    }

    @Test
    public void testObjectAndArrayTextIsPassedThroughVerbatim()
    {
        // Postgres's jsonb canonical form includes a space after ':' and ','...
        assertEquals("{\"c\": 42}", encode("{\"c\": 42}"));
        assertEquals("{\"a\": 1, \"b\": 2}", encode("{\"a\": 1, \"b\": 2}"));
        assertEquals("[1, 2, 3]", encode("[1, 2, 3]"));
        // ...but Postgres's `json` type preserves the original text exactly, spacing and all --
        // re-serializing through a generic Java Map/List would destroy whichever convention
        // actually applied, so both must come back completely unchanged.
        assertEquals("{\"c\":42}", encode("{\"c\":42}"));
        assertEquals("{\"a\":{\"b\":\"deep\"}}", encode("{\"a\":{\"b\":\"deep\"}}"));
    }

    private static String encode(String rawJsonbText)
    {
        return new String(JsonType.INSTANCE.encodeAsUTF8Text(rawJsonbText), StandardCharsets.UTF_8);
    }
}
