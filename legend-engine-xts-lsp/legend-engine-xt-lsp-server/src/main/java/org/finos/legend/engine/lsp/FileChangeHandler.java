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

package org.finos.legend.engine.lsp;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts LSP file events into FileChange objects for LegendPureSession.
 * Does NOT touch PureRuntime directly -- all mutation goes through the session.
 */
public class FileChangeHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileChangeHandler.class);

    private final UriMapper uriMapper;

    FileChangeHandler(UriMapper uriMapper)
    {
        this.uriMapper = uriMapper;
    }

    /**
     * Convert LSP file events into session-level FileChange objects.
     * Reads file content from disk for created/changed files.
     */
    public List<LegendPureSession.FileChange> toFileChanges(List<FileEvent> events)
    {
        List<LegendPureSession.FileChange> changes = new ArrayList<>();

        for (FileEvent event : events)
        {
            if (!event.getUri().endsWith(".pure"))
            {
                continue;
            }

            String sourceId = this.uriMapper.toSourceId(event.getUri());

            if (event.getType() == FileChangeType.Deleted)
            {
                changes.add(new LegendPureSession.FileChange(
                        sourceId, null, LegendPureSession.FileChangeType.DELETE));
            }
            else
            {
                String content = readFile(event.getUri());
                if (content != null)
                {
                    changes.add(new LegendPureSession.FileChange(
                            sourceId, content, LegendPureSession.FileChangeType.CREATE_OR_MODIFY));
                }
            }
        }
        return changes;
    }

    private static String readFile(String uri)
    {
        try
        {
            Path path = Paths.get(URI.create(uri));
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            LOGGER.warn("Failed to read file {}: {}", uri, e.getMessage());
            return null;
        }
    }
}
