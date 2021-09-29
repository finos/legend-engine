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

package org.finos.legend.engine.language.pure.grammar.from.authorizer;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authorizer.AuthorizerParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.AuthorizerValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.Authorizer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.PackageableAuthorizer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.function.Consumer;

public class AuthorizerParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> pureModelContextData;
    private final ImportAwareCodeSection section;
    private final PureGrammarParserExtensions extensions;

    public AuthorizerParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserExtensions extensions, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.extensions = extensions;
        this.pureModelContextData = elementConsumer;
        this.section = section;
    }

    public void visit(AuthorizerParserGrammar.DefinitionContext ctx)
    {
        ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()), this.section.imports);
        ctx.authorizer().stream().map(this::visitAuthorizer).peek(e -> this.section.elements.add(e.getPath())).forEach(this.pureModelContextData);
    }

    private PackageableAuthorizer visitAuthorizer(AuthorizerParserGrammar.AuthorizerContext ctx)
    {
        PackageableAuthorizer authorizer = new PackageableAuthorizer();
        authorizer.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        authorizer._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        authorizer.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        Authorizer authorizerValue = this.visitAuthorizerValue(ctx.authorizerValue(), ctx.authorizerType().getText(), authorizer.sourceInformation);
        if (authorizerValue == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
        authorizer.authorizerValue = authorizerValue;
        return authorizer;
    }

    private Authorizer visitAuthorizerValue(AuthorizerParserGrammar.AuthorizerValueContext ctx, String authorizerType, SourceInformation sourceInformation)
    {
        StringBuilder authorizerValueText = new StringBuilder();
        for (AuthorizerParserGrammar.AuthorizerValueContentContext fragment : ctx.authorizerValueContent())
        {
            authorizerValueText.append(fragment.getText());
        }
        String authorizerValueCode = authorizerValueText.length() > 0 ? authorizerValueText.substring(0, authorizerValueText.length() - 1) : authorizerValueText.toString();
        // prepare island grammar walker source information
        int startLine = ctx.BRACE_OPEN().getSymbol().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.BRACE_OPEN().getSymbol().getCharPositionInLine() + ctx.BRACE_OPEN().getSymbol().getText().length();
        ParseTreeWalkerSourceInformation authorizerValueWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        AuthorizerValueSourceCode authorizerValueSourceCode = new AuthorizerValueSourceCode(authorizerValueCode, authorizerType, sourceInformation, authorizerValueWalkerSourceInformation);
        AuthorizerValueParser authorizerValueParser = this.extensions.getAuthorizerValueParser(authorizerType);
        if (authorizerValueParser == null)
        {
            throw new EngineException("Unsupported authorizer value type: " + authorizerType, sourceInformation, EngineErrorType.PARSER);
        }
        return authorizerValueParser.parse(authorizerValueSourceCode);
    }
}
