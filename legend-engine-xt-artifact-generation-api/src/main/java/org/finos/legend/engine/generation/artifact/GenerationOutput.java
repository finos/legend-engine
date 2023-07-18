// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.generation.artifact;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.Objects;
import org.apache.commons.text.StringEscapeUtils;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;

public class GenerationOutput
{
    private final String content;
    private final String fileName;
    private final String format;

    public GenerationOutput(String content, String fileName, String format)
    {
        this.content = content;
        this.fileName = removeIllegalCharactersInFileName(fileName);
        this.format = format;
    }

    private String removeIllegalCharactersInFileName(String fileName)
    {
        return fileName.replaceAll("\\s+|:", "_");
    }

    public String getContent()
    {
        return this.content;
    }

    public String getFileName()
    {
        return this.fileName;
    }

    public String getFormat()
    {
        return this.format;
    }

    public String extractFileContent() throws IOException
    {
        if ("json".equals(this.format))
        {
            return PureProtocolObjectMapperFactory.withPureProtocolExtensions(JsonMapper.builder()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .serializationInclusion(JsonInclude.Include.NON_NULL)
                    .build())
                .readTree(this.content)
                .toPrettyString();
        }
        else if ("yaml".equals(this.format))
        {
            return this.content;
        }
        return StringEscapeUtils.unescapeJava(this.content);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        GenerationOutput that = (GenerationOutput) o;
        return Objects.equals(getContent(), that.getContent()) &&
            Objects.equals(getFileName(), that.getFileName());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getContent(), getFileName());
    }
}

