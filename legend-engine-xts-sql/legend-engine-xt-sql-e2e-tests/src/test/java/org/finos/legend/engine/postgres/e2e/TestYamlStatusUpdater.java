// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.postgres.e2e;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestYamlStatusUpdater
{
    @Test
    public void insertsAMissingStatusAboveTheCommentThatDescribesTheNextTest(@TempDir Path dir) throws Exception
    {
        String yaml = String.join("\n",
                "tests:",
                "  - id: first",
                "    sql: \"SELECT 1\"",
                "    expected_tds_status: PASS",
                "    expected_rel_status: PASS",
                "",
                "  # about the second test",
                "  - id: second",
                "    sql: \"SELECT 2\"",
                "    expected_tds_status: ERROR",
                "    expected_rel_status: ERROR",
                "");
        Files.write(dir.resolve("cases.yaml"), yaml.getBytes(StandardCharsets.UTF_8));

        YamlStatusUpdater updater = new YamlStatusUpdater();
        updater.recordResult("first", "Grammar", "PASS");
        updater.recordResult("second", "Grammar", "FAIL");
        updater.recordResult("second", "TDS", "PASS");
        updater.updateFiles(dir.toFile(), new String[] {"cases.yaml"});

        String expected = String.join("\n",
                "tests:",
                "  - id: first",
                "    sql: \"SELECT 1\"",
                "    expected_tds_status: PASS",
                "    expected_rel_status: PASS",
                "    expected_grammar_status: PASS",
                "",
                "  # about the second test",
                "  - id: second",
                "    sql: \"SELECT 2\"",
                "    expected_tds_status: PASS",
                "    expected_rel_status: ERROR",
                "    expected_grammar_status: FAIL",
                "");
        Assertions.assertEquals(expected, new String(Files.readAllBytes(new File(dir.toFile(), "cases.yaml").toPath()), StandardCharsets.UTF_8));
    }
}
