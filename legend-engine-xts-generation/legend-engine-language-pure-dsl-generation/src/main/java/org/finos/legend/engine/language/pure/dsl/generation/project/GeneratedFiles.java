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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Turns generated content into the files the build writes, and holds them the way the build's
 * output folder does.
 * <p>
 * Ported from legend-sdlc {@code GenerationOutput} and the serialization in
 * {@code FileGenerationMojo}, at legend-sdlc 3e2897a8. Until the build calls this class, a change
 * to those has to be made here too.
 */
final class GeneratedFiles
{
    private GeneratedFiles()
    {
    }

    static ProjectGenerationResult.GeneratedFile newFile(String content, String fileName, String format)
    {
        ProjectGenerationResult.GeneratedFile file = new ProjectGenerationResult.GeneratedFile();
        file.fileName = removeIllegalCharactersInFileName(fileName);
        file.format = format;
        file.content = extractFileContent(content, format);
        return file;
    }

    private static String removeIllegalCharactersInFileName(String fileName)
    {
        return fileName.replaceAll("\\s+|:", "_");
    }

    static String extractFileContent(String content, String format)
    {
        if ("json".equals(format))
        {
            // writes the same text as JsonNode.toPrettyString(), which isn't used because it writes through Jackson's own mapper,
            // which keeps the default write nesting limit
            JsonMapper jsonMapper = PureProtocolObjectMapperFactory.withPureProtocolExtensions(JsonMapper.builder()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .serializationInclusion(JsonInclude.Include.NON_NULL)
                    .build());
            // generated JSON has no size we can bound, so every Jackson stream limit is lifted, as the build does
            ObjectMapperFactory.withStandardConfigurations(jsonMapper);
            try
            {
                JsonNode node = jsonMapper.readTree(content);
                // empty or blank content reads as a MissingNode, which toPrettyString() wrote as "" but the mapper writes as null
                return node.isMissingNode() ? "" : jsonMapper.writeValueAsString(node);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
        if ("yaml".equals(format))
        {
            return content;
        }
        return StringEscapeUtils.unescapeJava(content);
    }

    /**
     * The build writes file generations and artifacts into one folder: a file written twice with
     * the same content is a warning, and with different content fails the build.
     */
    static final class OutputFolder
    {
        private final Map<String, String> contentByPath = new HashMap<>();

        /**
         * @return whether the file was already there with the same content
         * @throws IllegalStateException when it was already there with different content
         */
        boolean add(String path, String content, String duplicateMessage)
        {
            if (!this.contentByPath.containsKey(path))
            {
                this.contentByPath.put(path, content);
                return false;
            }
            if (!Objects.equals(this.contentByPath.get(path), content))
            {
                throw new IllegalStateException(duplicateMessage);
            }
            return true;
        }
    }
}
