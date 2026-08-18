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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates YAML test files in-place with the current test status (expected_tds_status / expected_rel_status).
 * Activated by running with -Dparity.updateStatus=true.
 *
 * This performs line-level manipulation to preserve formatting and comments.
 */
public class YamlStatusUpdater
{
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlStatusUpdater.class);

    private final Map<String, String> tdsResults = new ConcurrentHashMap<>();
    private final Map<String, String> relResults = new ConcurrentHashMap<>();

    public void recordResult(String testId, String path, String status)
    {
        if ("TDS".equals(path))
        {
            tdsResults.put(testId, status);
        }
        else
        {
            relResults.put(testId, status);
        }
    }

    /**
     * Updates all YAML test files with the recorded statuses.
     * @param resourceDir the src/test/resources directory (resolved from project root)
     * @param testFiles array of relative resource paths
     */
    public void updateFiles(File resourceDir, String[] testFiles)
    {
        for (String testFile : testFiles)
        {
            File yamlFile = new File(resourceDir, testFile);
            if (!yamlFile.exists())
            {
                LOGGER.warn("YAML file not found for update: {}", yamlFile);
                continue;
            }
            try
            {
                updateFile(yamlFile);
            }
            catch (IOException e)
            {
                LOGGER.error("Failed to update YAML file: {}", yamlFile, e);
            }
        }
    }

    private void updateFile(File yamlFile) throws IOException
    {
        List<String> lines = Files.readAllLines(yamlFile.toPath());
        StringBuilder result = new StringBuilder();
        String currentId = null;

        Pattern idPattern = Pattern.compile("^(\\s*)-\\s*id:\\s*(.+)$");
        Pattern tdsStatusPattern = Pattern.compile("^(\\s*)expected_tds_status:\\s*(.+)$");
        Pattern relStatusPattern = Pattern.compile("^(\\s*)expected_rel_status:\\s*(.+)$");

        boolean modified = false;
        int i = 0;
        while (i < lines.size())
        {
            String line = lines.get(i);
            Matcher idMatcher = idPattern.matcher(line);
            if (idMatcher.matches())
            {
                currentId = idMatcher.group(2).trim().replaceAll("^[\"']|[\"']$", "");
                result.append(line).append('\n');
                i++;

                // Look ahead for the rest of this test case's fields
                // We need to insert/update expected_tds_status and expected_rel_status
                String indent = idMatcher.group(1) + "  ";
                boolean hasTdsStatus = false;
                boolean hasRelStatus = false;

                // Scan ahead to find end of this test block and check for existing status fields
                int blockEnd = i;
                while (blockEnd < lines.size())
                {
                    String nextLine = lines.get(blockEnd);
                    // New test case or end of list
                    if (nextLine.matches("\\s*-\\s*id:.*") || (nextLine.trim().isEmpty() && blockEnd + 1 < lines.size() && lines.get(blockEnd + 1).matches("\\s*-\\s*id:.*")))
                    {
                        break;
                    }
                    if (tdsStatusPattern.matcher(nextLine).matches())
                    {
                        hasTdsStatus = true;
                    }
                    if (relStatusPattern.matcher(nextLine).matches())
                    {
                        hasRelStatus = true;
                    }
                    blockEnd++;
                }

                // Process the block lines
                while (i < blockEnd)
                {
                    String blockLine = lines.get(i);
                    Matcher tdsMatcher = tdsStatusPattern.matcher(blockLine);
                    Matcher relMatcher = relStatusPattern.matcher(blockLine);

                    if (tdsMatcher.matches() && currentId != null && tdsResults.containsKey(currentId))
                    {
                        result.append(tdsMatcher.group(1)).append("expected_tds_status: ").append(tdsResults.get(currentId)).append('\n');
                        modified = true;
                    }
                    else if (relMatcher.matches() && currentId != null && relResults.containsKey(currentId))
                    {
                        result.append(relMatcher.group(1)).append("expected_rel_status: ").append(relResults.get(currentId)).append('\n');
                        modified = true;
                    }
                    else
                    {
                        result.append(blockLine).append('\n');
                    }
                    i++;
                }

                // Append missing status fields at the end of the block
                if (currentId != null && !hasTdsStatus && tdsResults.containsKey(currentId))
                {
                    result.append(indent).append("expected_tds_status: ").append(tdsResults.get(currentId)).append('\n');
                    modified = true;
                }
                if (currentId != null && !hasRelStatus && relResults.containsKey(currentId))
                {
                    result.append(indent).append("expected_rel_status: ").append(relResults.get(currentId)).append('\n');
                    modified = true;
                }
            }
            else
            {
                result.append(line).append('\n');
                i++;
            }
        }

        if (modified)
        {
            Files.write(yamlFile.toPath(), result.toString().getBytes(StandardCharsets.UTF_8));
            LOGGER.info("Updated status in: {}", yamlFile.getName());
        }
    }
}




