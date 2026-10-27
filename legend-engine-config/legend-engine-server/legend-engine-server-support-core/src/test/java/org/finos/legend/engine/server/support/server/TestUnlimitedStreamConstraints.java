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

package org.finos.legend.engine.server.support.server;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class TestUnlimitedStreamConstraints
{
    private static final int BEYOND_DEFAULT_MAX_DEPTH = 3 * StreamReadConstraints.DEFAULT_MAX_DEPTH;

    @AfterEach
    public void restoreJacksonDefaults()
    {
        StreamReadConstraints.overrideDefaultStreamReadConstraints(null);
        StreamWriteConstraints.overrideDefaultStreamWriteConstraints(null);
    }

    @Test
    public void installedMapperReadsAndWritesJsonNestedBeyondJacksonDefaultDepth() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        UnlimitedStreamConstraints.install(mapper);
        String json = nestedArrays(BEYOND_DEFAULT_MAX_DEPTH);

        JsonNode node = mapper.readTree(json);

        Assertions.assertEquals(json, mapper.writeValueAsString(node));
    }

    @Test
    public void installedMapperReadsStringLongerThanJacksonDefaultMaxStringLength() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        UnlimitedStreamConstraints.install(mapper);
        String value = String.join("", Collections.nCopies(StreamReadConstraints.DEFAULT_MAX_STRING_LEN + 1, "a"));

        String parsed = mapper.readValue("\"" + value + "\"", String.class);

        Assertions.assertEquals(value.length(), parsed.length());
    }

    @Test
    public void installedMapperReadsNumberLongerThanJacksonDefaultMaxNumberLength() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        UnlimitedStreamConstraints.install(mapper);
        String digits = String.join("", Collections.nCopies(StreamReadConstraints.DEFAULT_MAX_NUM_LEN + 1, "7"));

        JsonNode node = mapper.readTree("{\"n\":" + digits + "}");

        Assertions.assertEquals(digits, node.get("n").bigIntegerValue().toString());
    }

    @Test
    public void installedMapperReadsPropertyNameLongerThanJacksonDefaultMaxNameLength() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        UnlimitedStreamConstraints.install(mapper);
        String name = String.join("", Collections.nCopies(StreamReadConstraints.DEFAULT_MAX_NAME_LEN + 1, "p"));

        JsonNode node = mapper.readTree("{\"" + name + "\":1}");

        Assertions.assertEquals(1, node.get(name).intValue());
    }

    @Test
    public void mappersBuiltAfterInstallAreUnlimited() throws Exception
    {
        UnlimitedStreamConstraints.install(new ObjectMapper());
        ObjectMapper builtAfter = new ObjectMapper();
        String json = nestedArrays(BEYOND_DEFAULT_MAX_DEPTH);

        Assertions.assertEquals(json, builtAfter.writeValueAsString(builtAfter.readTree(json)));
    }

    @Test
    public void aMapperBuiltBeforeInstallAndNotPassedInKeepsJacksonDefaults()
    {
        ObjectMapper builtBefore = new ObjectMapper();
        UnlimitedStreamConstraints.install(new ObjectMapper());

        Assertions.assertThrows(StreamConstraintsException.class, () -> builtBefore.readTree(nestedArrays(BEYOND_DEFAULT_MAX_DEPTH)));
    }

    private static String nestedArrays(int depth)
    {
        return String.join("", Collections.nCopies(depth, "[")) + String.join("", Collections.nCopies(depth, "]"));
    }
}
