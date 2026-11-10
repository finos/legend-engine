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

package org.finos.legend.engine.language.pure.dsl.generation.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 * Ported from legend-sdlc's TestGenerationOutput and TestFileGenerationFactory.testExtractFileContent,
 * at legend-sdlc 3e2897a8: the files written must be the build's, byte for byte.
 */
public class TestGeneratedFiles
{
    @Test
    void jsonIsFormattedAsJsonNodeToPrettyString() throws Exception
    {
        String json = "{\"b\":1,\"a\":[true,null,{}],\"n\":null,\"e\":[],\"d\":{\"z\":\"x\",\"y\":1.50}}";
        String content = GeneratedFiles.extractFileContent(json, "json");
        Assertions.assertEquals(new ObjectMapper().readTree(json).toPrettyString(), content);
        Assertions.assertTrue(content.contains("\"b\" : 1"), content);
    }

    @Test
    void jsonOfEveryShapeIsFormattedAsJsonNodeToPrettyString() throws Exception
    {
        String[] inputs = {
                "[1,2,[3,[4,[]]],{}]",
                "\"text\"",
                "42",
                "true",
                "{}",
                "[]",
                "  {\"a\" : 1}  ",
                "{\"z\":1,\"a\":2,\"m\":{\"y\":null,\"b\":[null,null]}}",
                "{\"dup\":1,\"dup\":2}",
                "{\"u\":\"caf\\u00e9 \\ud83d\\ude00 \\u0000 \\t\\n\\\"\\\\/ </script>\",\"cjk\":\"漢字\"}",
                "[-0.0,0.1,1.0,1E10,1e400,3.141592653589793238462643383279,123456789012345678901234567890]",
                "{\"_type\":\"class\",\"name\":\"A\",\"package\":\"x\",\"properties\":[]}"
        };
        for (String json : inputs)
        {
            Assertions.assertEquals(new ObjectMapper().readTree(json).toPrettyString(), GeneratedFiles.extractFileContent(json, "json"), json);
        }
    }

    @Test
    void jsonDeeperThanJacksonDefaultLimitIsExtracted()
    {
        int depth = 1200;
        String json = String.join("", Collections.nCopies(depth, "{\"a\":")) + "1" + String.join("", Collections.nCopies(depth, "}"));
        String content = GeneratedFiles.extractFileContent(json, "json");
        Assertions.assertTrue(content.contains("\"a\" : 1"));
        Assertions.assertEquals(depth, content.chars().filter(c -> c == '{').count());
    }

    @Test
    void emptyOrBlankJsonIsExtractedAsEmpty()
    {
        for (String json : new String[]{"", " ", "\n", " \t\r\n "})
        {
            Assertions.assertEquals("", GeneratedFiles.extractFileContent(json, "json"), "\"" + json + "\"");
        }
        Assertions.assertEquals("null", GeneratedFiles.extractFileContent("null", "json"));
    }

    @Test
    void nestedJsonIsIndented()
    {
        String expected = "[ [ [ {%n" +
                "  \"type\" : \"myName\",%n" +
                "  \"name\" : \"testing\",%n" +
                "  \"fields\" : [ ]%n" +
                "} ] ] ]";
        Assertions.assertEquals(String.format(expected), GeneratedFiles.extractFileContent("[[[{\"type\":\"myName\",\"name\":\"testing\",\"fields\": []}]]]", "json"));
    }

    @Test
    void yamlIsKeptAndOtherFormatsAreUnescaped()
    {
        Assertions.assertEquals("a: '\\n'", GeneratedFiles.extractFileContent("a: '\\n'", "yaml"));
        Assertions.assertEquals("line\nnext", GeneratedFiles.extractFileContent("line\\nnext", "txt"));
    }

    @Test
    void fileNamesLoseWhitespaceAndColons()
    {
        Assertions.assertEquals("my_file_name.txt", GeneratedFiles.newFile("", "my file:name.txt", "txt").fileName);
    }
}
