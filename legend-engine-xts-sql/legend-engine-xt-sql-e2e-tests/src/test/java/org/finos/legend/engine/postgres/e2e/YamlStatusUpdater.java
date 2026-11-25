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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates YAML test files in-place with the current test status (expected_tds_status / expected_rel_status / expected_grammar_status).
 * Activated by running with -Dparity.updateStatus=true.
 *
 * This performs line-level manipulation to preserve formatting and comments.
 */
public class YamlStatusUpdater
{
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlStatusUpdater.class);

    private final Map<ParityPath, Map<String, String>> results = new EnumMap<>(ParityPath.class);

    public YamlStatusUpdater()
    {
        for (ParityPath path : ParityPath.values())
        {
            this.results.put(path, new ConcurrentHashMap<>());
        }
    }

    public void recordResult(String testId, String path, String status)
    {
        this.results.get(ParityPath.fromLabel(path)).put(testId, status);
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

        Pattern idPattern = Pattern.compile("^(\\s*)-\\s*id:\\s*(.+)$");
        Map<ParityPath, Pattern> statusPatterns = new EnumMap<>(ParityPath.class);
        for (ParityPath path : ParityPath.values())
        {
            statusPatterns.put(path, Pattern.compile("^(\\s*)" + path.statusField() + ":\\s*(.+)$"));
        }

        boolean modified = false;
        int i = 0;
        while (i < lines.size())
        {
            String line = lines.get(i);
            Matcher idMatcher = idPattern.matcher(line);
            if (idMatcher.matches())
            {
                String currentId = idMatcher.group(2).trim().replaceAll("^[\"']|[\"']$", "");
                result.append(line).append('\n');
                i++;

                String indent = idMatcher.group(1) + "  ";
                Set<ParityPath> presentStatuses = EnumSet.noneOf(ParityPath.class);

                int blockEnd = i;
                while (blockEnd < lines.size())
                {
                    String nextLine = lines.get(blockEnd);
                    if (nextLine.matches("\\s*-\\s*id:.*") || (nextLine.trim().isEmpty() && blockEnd + 1 < lines.size() && lines.get(blockEnd + 1).matches("\\s*-\\s*id:.*")))
                    {
                        break;
                    }
                    for (ParityPath path : ParityPath.values())
                    {
                        if (statusPatterns.get(path).matcher(nextLine).matches())
                        {
                            presentStatuses.add(path);
                        }
                    }
                    blockEnd++;
                }

                // trailing comments before the next '- id:' describe the next test, so missing fields go above them
                int insertAt = blockEnd;
                while (insertAt > i && (lines.get(insertAt - 1).trim().isEmpty() || lines.get(insertAt - 1).trim().startsWith("#")))
                {
                    insertAt--;
                }

                while (i < blockEnd)
                {
                    if (i == insertAt)
                    {
                        modified |= appendMissingStatuses(result, indent, currentId, presentStatuses);
                    }
                    String blockLine = lines.get(i);
                    String replacement = null;
                    for (ParityPath path : ParityPath.values())
                    {
                        Matcher matcher = statusPatterns.get(path).matcher(blockLine);
                        if (matcher.matches() && this.results.get(path).containsKey(currentId))
                        {
                            replacement = matcher.group(1) + path.statusField() + ": " + this.results.get(path).get(currentId);
                            modified = true;
                        }
                    }
                    result.append(replacement != null ? replacement : blockLine).append('\n');
                    i++;
                }
                if (insertAt == blockEnd)
                {
                    modified |= appendMissingStatuses(result, indent, currentId, presentStatuses);
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

    private boolean appendMissingStatuses(StringBuilder result, String indent, String testId, Set<ParityPath> presentStatuses)
    {
        boolean appended = false;
        for (ParityPath path : ParityPath.values())
        {
            if (!presentStatuses.contains(path) && this.results.get(path).containsKey(testId))
            {
                result.append(indent).append(path.statusField()).append(": ").append(this.results.get(path).get(testId)).append('\n');
                appended = true;
            }
        }
        return appended;
    }
}




