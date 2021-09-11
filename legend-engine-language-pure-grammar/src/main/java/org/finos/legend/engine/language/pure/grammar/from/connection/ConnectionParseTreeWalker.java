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

package org.finos.legend.engine.language.pure.grammar.from.connection;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.function.Consumer;

public class ConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> pureModelContextData;
    private final ImportAwareCodeSection section;
    private final PureGrammarParserExtensions extensions;

    public ConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserExtensions extensions, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.extensions = extensions;
        this.pureModelContextData = elementConsumer;
        this.section = section;
    }

    public void visit(ConnectionParserGrammar.DefinitionContext ctx)
    {
        ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()), this.section.imports);
        ctx.connection().stream().map(this::visitConnection).peek(e -> this.section.elements.add(e.getPath())).forEach(this.pureModelContextData);
    }

    public Connection visitEmbeddedRuntimeConnection(ConnectionParserGrammar.EmbeddedRuntimeConnectionContext ctx, SourceInformation embeddedConnectionSourceValue)
    {
        if (ctx == null || ctx.children == null)
        {
            throw new EngineException("Embedded connection must not be empty", embeddedConnectionSourceValue, EngineErrorType.PARSER);
        }
        Connection connectionValue = this.visitConnectionValue(ctx.connectionValue(), ctx.connectionType().getText(), this.walkerSourceInformation.getSourceInformation(ctx), true);
        if (connectionValue == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
        return connectionValue;
    }

    private PackageableConnection visitConnection(ConnectionParserGrammar.ConnectionContext ctx)
    {
        PackageableConnection connection = new PackageableConnection();
        connection.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        connection._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        connection.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        Connection connectionValue = this.visitConnectionValue(ctx.connectionValue(), ctx.connectionType().getText(), connection.sourceInformation, false);
        if (connectionValue == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
        connection.connectionValue = connectionValue;
        return connection;
    }

    private Connection visitConnectionValue(ConnectionParserGrammar.ConnectionValueContext ctx, String connectionType, SourceInformation sourceInformation, boolean isProcessingEmbeddedConnection)
    {
        StringBuilder connectionValueText = new StringBuilder();
        for (ConnectionParserGrammar.ConnectionValueContentContext fragment : ctx.connectionValueContent())
        {
            connectionValueText.append(fragment.getText());
        }
        String connectionValueCode = connectionValueText.length() > 0 ? connectionValueText.substring(0, connectionValueText.length() - 1) : connectionValueText.toString();
        // prepare island grammar walker source information
        int startLine = ctx.BRACE_OPEN().getSymbol().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.BRACE_OPEN().getSymbol().getCharPositionInLine() + ctx.BRACE_OPEN().getSymbol().getText().length();
        ParseTreeWalkerSourceInformation connectionValueWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        ConnectionValueSourceCode connectionValueSourceCode = new ConnectionValueSourceCode(connectionValueCode, connectionType, sourceInformation, connectionValueWalkerSourceInformation, isProcessingEmbeddedConnection);
        ConnectionValueParser connectionValueParser = this.extensions.getConnectionValueParser(connectionType);
        if (connectionValueParser == null)
        {
            throw new EngineException("Unsupported connection value type: " + connectionType, sourceInformation, EngineErrorType.PARSER);
        }
        return connectionValueParser.parse(connectionValueSourceCode);
    }
}
