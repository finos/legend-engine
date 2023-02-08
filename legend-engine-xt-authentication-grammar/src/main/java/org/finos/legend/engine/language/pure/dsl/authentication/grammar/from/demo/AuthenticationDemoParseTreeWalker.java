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

package org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.demo;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.AuthenticationParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.demo.AuthenticationDemo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;

import java.util.function.Consumer;

public class AuthenticationDemoParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;
    private final PureGrammarParserContext context;
    private final AuthenticationParseTreeWalker authenticationParseTreeWalker;

    public AuthenticationDemoParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section, PureGrammarParserContext context)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.context = context;
        this.authenticationParseTreeWalker = new AuthenticationParseTreeWalker(walkerSourceInformation, context);
    }

    public void visit(AuthenticationParserGrammar.DefinitionContext ctx)
    {
        ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()), this.section.imports);
        ctx.elementDefinition().stream().map(this::visitAuthenticationDemo).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private AuthenticationDemo visitAuthenticationDemo(AuthenticationParserGrammar.ElementDefinitionContext elementDefinitionContext)
    {
        AuthenticationDemo authenticationDemo = new AuthenticationDemo();

        AuthenticationParserGrammar.AuthenticationDemoContext ctx = elementDefinitionContext.authenticationDemo();
        authenticationDemo.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        authenticationDemo._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());

        // authentication
        AuthenticationParserGrammar.AuthenticationContext authenticationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.authentication(), "authentication", authenticationDemo.sourceInformation);
        authenticationDemo.authenticationSpecification = authenticationParseTreeWalker.visitAuthenticationSpecification(authenticationContext);

        return authenticationDemo;
    }
}
