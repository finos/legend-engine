// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.to;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.language.stores.elasticsearch.v7.from.ElasticsearchEmbeddedDataParser;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.data.ElasticsearchV7EmbeddedData;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.data.ElasticsearchV7IndexEmbeddedData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

public class ElasticsearchEmbeddedDataComposer
{
    public static ContentWithType composeEmbeddedData(EmbeddedData embeddedData, PureGrammarComposerContext context)
    {
        if (embeddedData instanceof ElasticsearchV7EmbeddedData)
        {
            ElasticsearchV7EmbeddedData elasticsearchV7EmbeddedData = (ElasticsearchV7EmbeddedData) embeddedData;

            String indentationString = context.getIndentationString();
            String content = elasticsearchV7EmbeddedData.indexData
                    .stream()
                    .map(x -> ElasticsearchEmbeddedDataComposer.compose(x, context))
                    .collect(Collectors.joining(";\n" + indentationString, indentationString, ";"));

            return new ContentWithType(ElasticsearchEmbeddedDataParser.TYPE, content);
        }
        else
        {
            return null;
        }
    }

    private static String compose(ElasticsearchV7IndexEmbeddedData embeddedData, PureGrammarComposerContext context)
    {
        return PureGrammarComposerUtility.convertIdentifier(embeddedData.index) + ':' + "\n" + renderJson(embeddedData.documentsAsJson, context);
    }

    // TODO move to core together with g4 files to allow re-use
    private static String renderJson(String json, PureGrammarComposerContext context)
    {
        try
        {
            String defaultIndentation = context.getIndentationString() + PureGrammarComposerUtility.getTabString();

            DefaultPrettyPrinter.Indenter indenter = new JsonIndenter(defaultIndentation);

            DefaultPrettyPrinter defaultPrettyPrinter = new DefaultPrettyPrinter()
                    .withArrayIndenter(indenter)
                    .withObjectIndenter(indenter);

            ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapper();

            JsonNode jsonNode = mapper.readTree(json);

            return defaultIndentation + mapper
                    .setDefaultPrettyPrinter(defaultPrettyPrinter)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(jsonNode);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static class JsonIndenter implements DefaultPrettyPrinter.Indenter
    {
        private final String defaultIndentation;

        public JsonIndenter(String defaultIndentation)
        {
            this.defaultIndentation = defaultIndentation;
        }

        @Override
        public void writeIndentation(JsonGenerator jg, int level) throws IOException
        {
            jg.writeRaw('\n');
            jg.writeRaw(defaultIndentation);
            if (level > 0)
            {
                while (level > 0)
                {
                    level--;
                    jg.writeRaw(PureGrammarComposerUtility.getTabString());
                }
            }
        }

        @Override
        public boolean isInline()
        {
            return false;
        }
    }
}
