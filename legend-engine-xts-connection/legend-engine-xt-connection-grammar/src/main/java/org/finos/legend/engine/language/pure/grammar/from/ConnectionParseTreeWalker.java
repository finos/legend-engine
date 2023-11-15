// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ConnectionParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.Connection;

import java.util.function.Consumer;

public class ConnectionParseTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public ConnectionParseTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(ConnectionParserGrammar.DefinitionContext ctx)
    {
        ctx.databaseConnectionElement().stream().map(this::visitElement).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private Connection visitElement(ConnectionParserGrammar.DatabaseConnectionElementContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        ConnectionParserGrammar.RawValueContext rawValueContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.rawValue(), "rawValue", sourceInformation);
        Connection connection;
        try
        {
            StringBuilder text = new StringBuilder();
            for (ConnectionParserGrammar.RawValueContentContext fragment : rawValueContext.rawValueContent())
            {
                text.append(fragment.getText());
            }
            String rawValueText = text.length() > 0 ? text.substring(0, text.length() - 2) : text.toString();
            connection = PureProtocolObjectMapperFactory.getNewObjectMapper().readValue(rawValueText, Connection.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        connection.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        connection._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        connection.sourceInformation = sourceInformation;

        return connection;
    }
}
