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

package org.finos.legend.engine.test.emit.junit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.test.emit.EMITSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestEMITSnapshot
{
    private static final ObjectMapper JSON = new ObjectMapper();

    @AfterEach
    void clearProperties()
    {
        System.clearProperty(EMITSnapshot.DIRECTORY_PROPERTY);
        System.clearProperty(EMITSnapshot.CROSS_READ_PROPERTY);
    }

    @Test
    void noSnapshotGroupWithoutTheProperty()
    {
        Assertions.assertFalse(childNames(EMITTestSuiteBuilder.testContainer("emit-models/", "basic/m2m-passing")).contains("Snapshot"));
    }

    @Test
    void snapshotRecordsProtocolAndTestPlans(@TempDir Path dir) throws Throwable
    {
        System.setProperty(EMITSnapshot.DIRECTORY_PROPERTY, dir.toString());
        executeAll(EMITTestSuiteBuilder.testContainer("emit-models/", "basic/m2m-passing"));

        Path model = dir.resolve("emit-models").resolve("m2m-passing");
        JsonNode outcome = JSON.readTree(model.resolve("model.json").toFile());
        Assertions.assertEquals("ok", outcome.path("parse").path("status").asText());
        Assertions.assertEquals("ok", outcome.path("compile").path("status").asText());
        Assertions.assertEquals("ok", outcome.path("roundTrip").path("status").asText());
        Assertions.assertTrue(Files.exists(model.resolve("pmcd.json")));
        Assertions.assertTrue(Files.exists(model.resolve("pmcd.roundtrip.json")));
        Assertions.assertTrue(JSON.readTree(dir.resolve("emit-models").resolve("_subtypes.json").toFile()).size() > 0);

        List<Path> plans = jsonFilesUnder(model.resolve("plans").resolve("test"));
        Assertions.assertFalse(plans.isEmpty(), "expected a debug plan per mapping test");
        for (Path plan : plans)
        {
            Assertions.assertEquals("ok", JSON.readTree(plan.toFile()).path("status").asText(), plan::toString);
        }
    }

    @Test
    void crossReadLoadsAndCompilesTheOtherSnapshot(@TempDir Path dir) throws Throwable
    {
        Path first = dir.resolve("first");
        System.setProperty(EMITSnapshot.DIRECTORY_PROPERTY, first.toString());
        executeAll(EMITTestSuiteBuilder.testContainer("emit-models/", "basic/m2m-passing"));

        Path second = dir.resolve("second");
        System.setProperty(EMITSnapshot.DIRECTORY_PROPERTY, second.toString());
        System.setProperty(EMITSnapshot.CROSS_READ_PROPERTY, first.toString());
        DynamicContainer container = EMITTestSuiteBuilder.testContainer("emit-models/", "basic/m2m-passing");
        executeAll(container);

        Path model = second.resolve("emit-models").resolve("m2m-passing");
        JsonNode crossRead = JSON.readTree(model.resolve("crossread.json").toFile());
        Assertions.assertEquals("ok", crossRead.path("deserialize").path("status").asText());
        Assertions.assertEquals("ok", crossRead.path("compile").path("status").asText());
        Assertions.assertEquals(JSON.readTree(model.resolve("pmcd.roundtrip.json").toFile()), JSON.readTree(model.resolve("pmcd.crossread.json").toFile()));
    }

    private static List<String> childNames(DynamicContainer container)
    {
        return container.getChildren().map(DynamicNode::getDisplayName).collect(Collectors.toList());
    }

    private static List<Path> jsonFilesUnder(Path root) throws IOException
    {
        try (Stream<Path> files = Files.walk(root))
        {
            return files.filter(p -> p.toString().endsWith(".json")).collect(Collectors.toList());
        }
    }

    private static void executeAll(DynamicNode node) throws Throwable
    {
        if (node instanceof DynamicContainer)
        {
            List<DynamicNode> children = ((DynamicContainer) node).getChildren().collect(Collectors.toList());
            for (DynamicNode child : children)
            {
                executeAll(child);
            }
        }
        else
        {
            ((DynamicTest) node).getExecutable().execute();
        }
    }
}
