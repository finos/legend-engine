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

package org.finos.legend.pure.rust.parser;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Smoke test of the JNI contract: the Java declarations bind to the native library's symbols and the JSON comes
 * back. Skipped when the native library is not available, unless {@code -Dlegend.pure.rust.parser.required=true}
 * (set in CI), which turns a missing library into a failure.
 */
public class TestNativeGrammarParser
{
    @BeforeClass
    public static void requireNativeLibrary()
    {
        String message = "native library not available: " + NativeGrammarParser.getLoadFailure();
        if (Boolean.getBoolean("legend.pure.rust.parser.required"))
        {
            Assert.assertTrue(message, NativeGrammarParser.isAvailable());
        }
        else
        {
            Assume.assumeTrue(message, NativeGrammarParser.isAvailable());
        }
    }

    @Test
    public void testCapabilitiesListPure()
    {
        String capabilities = NativeGrammarParser.capabilities();
        Assert.assertTrue(capabilities, capabilities.contains("\"sections\":[\"Pure\""));
    }

    @Test
    public void testParseSection()
    {
        // A section body as the engine hands it over: after the header, line offset -1.
        String json = NativeGrammarParser.parseSection("Pure", "\nClass a::A {}\n", "test.pure", -1, 0);
        Assert.assertTrue(json, json.contains("\"_type\":\"class\""));
        Assert.assertTrue(json, json.contains("\"startLine\":1"));
        Assert.assertFalse(json, json.contains("\"error\""));
    }

    @Test
    public void testSyntaxErrorIsReportedAsJson()
    {
        String json = NativeGrammarParser.parseSection("Pure", "\nClass a::A { p: }\n", "test.pure", -1, 0);
        Assert.assertTrue(json, json.contains("\"kind\":\"parser\""));
    }

    @Test
    public void testParseExpression()
    {
        String json = NativeGrammarParser.parseExpression("lambda", "x|$x + 1", "query", 0, 0);
        Assert.assertTrue(json, json.startsWith("{\"value\":{"));
        Assert.assertTrue(json, json.contains("\"_type\":\"lambda\""));
        String unknown = NativeGrammarParser.parseExpression("sql", "1", "query", 0, 0);
        Assert.assertTrue(unknown, unknown.contains("\"kind\":\"unsupported\""));
    }

    @Test
    public void testNullArgumentIsAnInternalError()
    {
        String json = NativeGrammarParser.parseSection("Pure", null, "test.pure", -1, 0);
        Assert.assertTrue(json, json.contains("\"kind\":\"internal\""));
    }
}
