// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.shared.format.model.test;

import org.apache.commons.io.IOUtils;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ExternalSchemaSetGrammarBuilder
{
    private String path;
    private String format;
    private List<SchemaValues> schemas = Lists.mutable.empty();

    public ExternalSchemaSetGrammarBuilder(String path, String format)
    {
        this.path = path;
        this.format = format;
    }

    public ExternalSchemaSetGrammarBuilder withSchemaText(String content)
    {
        return withSchemaText(null, null, content);
    }

    public ExternalSchemaSetGrammarBuilder withSchemaText(String id, String content)
    {
        return withSchemaText(id, null, content);
    }

    public ExternalSchemaSetGrammarBuilder withSchemaText(String id, String location, String content)
    {
        this.schemas.add(new SchemaValues(id, location, content));
        return this;
    }

    public ExternalSchemaSetGrammarBuilder withSchemaResource(String name)
    {
        return withSchemaResource(null, null, name);
    }

    public ExternalSchemaSetGrammarBuilder withSchemaResource(String id, String name)
    {
        return withSchemaResource(id, null, name);
    }

    public ExternalSchemaSetGrammarBuilder withSchemaResource(String id, String location, String name)
    {
        try
        {
            return withSchemaText(id, location, IOUtils.toString(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(name), "Missing resource " + name)));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String build()
    {
        StringBuilder grammar = new StringBuilder("###ExternalFormat\n");
        grammar.append("SchemaSet ").append(path).append("\n");
        grammar.append("{\n");
        grammar.append("  format:").append(format).append(";\n");
        grammar.append("  schemas: [\n");
        for (int i = 0; i < schemas.size(); i++)
        {
            SchemaValues values = schemas.get(i);
            grammar.append("    {\n");
            if (values.id != null)
            {
                grammar.append("      id:").append(values.id).append(";\n");
            }
            if (values.location != null)
            {
                grammar.append("      location:").append(PureGrammarComposerUtility.convertString(values.location, true)).append(";\n");
            }
            grammar.append("      content:").append(PureGrammarComposerUtility.convertString(values.content, true)).append(";\n");
            grammar.append("    }").append(i < schemas.size() - 1 ? "," : "").append("\n");
        }
        grammar.append("  ];\n");
        grammar.append("}\n");
        return grammar.toString();
    }

    private static class SchemaValues
    {
        final String id;
        final String location;
        final String content;

        SchemaValues(String id, String location, String content)
        {
            this.id = id;
            this.location = location;
            this.content = content;
        }
    }
}