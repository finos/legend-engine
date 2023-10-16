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
import org.finos.legend.engine.protocol.pure.v1.packageableElement.ConnectionDemo;

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
        ctx.connectionDemoElement().stream().map(this::visitElement).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private ConnectionDemo visitElement(ConnectionParserGrammar.ConnectionDemoElementContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        ConnectionParserGrammar.RawValueContext rawValueContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.rawValue(), "rawValue", sourceInformation);
        ConnectionDemo connectionDemo;
        try
        {
            StringBuilder connectionDemoText = new StringBuilder();
            for (ConnectionParserGrammar.RawValueContentContext fragment : rawValueContext.rawValueContent())
            {
                connectionDemoText.append(fragment.getText());
            }
            String rawValueText = connectionDemoText.length() > 0 ? connectionDemoText.substring(0, connectionDemoText.length() - 2) : connectionDemoText.toString();
            // prepare island grammar walker source information
            int startLine = rawValueContext.ISLAND_OPEN().getSymbol().getLine();
            int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
            // only add current walker source information column offset if this is the first line
            int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + rawValueContext.ISLAND_OPEN().getSymbol().getCharPositionInLine() + rawValueContext.ISLAND_OPEN().getText().length();
            ParseTreeWalkerSourceInformation embeddedRuntimeWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
            connectionDemo = PureProtocolObjectMapperFactory.getNewObjectMapper().readValue(rawValueText, ConnectionDemo.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        connectionDemo.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        connectionDemo._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        connectionDemo.sourceInformation = sourceInformation;

        return connectionDemo;
    }
}
