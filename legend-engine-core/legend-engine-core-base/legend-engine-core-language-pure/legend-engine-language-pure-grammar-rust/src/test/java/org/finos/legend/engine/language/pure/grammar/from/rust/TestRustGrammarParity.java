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

package org.finos.legend.engine.language.pure.grammar.from.rust;

import com.fasterxml.jackson.databind.JsonNode;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How the parity harness reads the engine's JSON as the Rust parser writes it, and reads its accepted list.
 */
public class TestRustGrammarParity
{
    @Test
    public void testPositionsAreWrittenAsTheRustParserWritesThem() throws IOException
    {
        assertCompact(new SourceInformation("a.pure", 3, 5, 3, 9), "a.pure:3:5-9");
        assertCompact(new SourceInformation("a.pure", 3, 5, 7, 2), "a.pure:3:5-7:2");
        // A source id may hold ':' and '-'.
        assertCompact(new SourceInformation("x:y-z.pure", 1, 1, 1, 1), "x:y-z.pure:1:1-1");
        // Only position objects: an object with other fields keeps them all.
        JsonNode other = tree("{\"sourceId\":\"a\",\"startLine\":1,\"startColumn\":1,\"endLine\":1,\"endColumn\":2,\"name\":\"n\"}");
        Assert.assertEquals(other, RustGrammarParity.compactPositions(other));
    }

    @Test
    public void testCanonicalJsonDropsWhatTheEngineReadsAsAbsent() throws IOException
    {
        Assert.assertEquals(RustGrammarParity.canonical(tree("{\"a\":[{\"c\":1}]}")), RustGrammarParity.canonical(tree("{\"a\":[{\"b\":null,\"c\":1,\"d\":[]}],\"e\":[],\"f\":null}")));
        // Numbers by value: the same number written two ways.
        Assert.assertEquals(RustGrammarParity.canonical(tree("{\"n\":10.50,\"m\":1.0E1,\"z\":0.0}")), RustGrammarParity.canonical(tree("{\"n\":10.5,\"m\":10,\"z\":0}")));
        Assert.assertNotEquals(RustGrammarParity.canonical(tree("{\"n\":1}")), RustGrammarParity.canonical(tree("{\"n\":\"1\"}")));
    }

    @Test
    public void testTheAcceptedListHasAKeyAndAReasonPerLine() throws IOException
    {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("0123456789abcdef", "accepted: a Java-only quirk");
        expected.put("unlinked:connection:X", "pending: dsl-x");
        expected.put("model-0123456789abcdef", "");
        Assert.assertEquals(expected, RustGrammarParity.parseAccepted(new BufferedReader(new StringReader("# comment\n\n0123456789abcdef  accepted: a Java-only quirk\n  unlinked:connection:X pending: dsl-x\nmodel-0123456789abcdef\n"))));
    }

    @Test
    public void testTheModulesAcceptedListReads() throws IOException
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(TestRustGrammarParity.class.getClassLoader().getResourceAsStream(RustGrammarParity.ACCEPTED_RESOURCE), StandardCharsets.UTF_8)))
        {
            RustGrammarParity.parseAccepted(reader).forEach((key, reason) -> Assert.assertTrue(key + ": " + reason, reason.matches("(accepted|pending|java-only|test): .+")));
        }
    }

    private static void assertCompact(SourceInformation position, String compact) throws IOException
    {
        JsonNode written = RustGrammarParity.compactPositions(RustSectionResult.MAPPER.valueToTree(position));
        Assert.assertEquals(compact, written.asText());
        SourceInformation read = SourceInformation.fromString(written.asText());
        Assert.assertEquals(RustSectionResult.MAPPER.writeValueAsString(position), RustSectionResult.MAPPER.writeValueAsString(read));
    }

    private static JsonNode tree(String json) throws IOException
    {
        return RustSectionResult.MAPPER.readTree(json);
    }
}
