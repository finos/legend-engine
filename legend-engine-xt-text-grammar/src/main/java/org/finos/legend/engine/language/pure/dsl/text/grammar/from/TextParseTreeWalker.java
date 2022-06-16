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

package org.finos.legend.engine.language.pure.dsl.text.grammar.from;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.TextParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.text.Text;

import java.util.function.Consumer;

public class TextParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public TextParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(TextParserGrammar.DefinitionContext ctx)
    {
        ctx.textElement().stream().map(this::visitText).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private Text visitText(TextParserGrammar.TextElementContext ctx)
    {
        Text text = new Text();
        text.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        text._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        text.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // type
        TextParserGrammar.TextTypeContext textTypeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.textType(), "type", text.sourceInformation);
        text.type = textTypeContext != null ? PureGrammarParserUtility.fromIdentifier(textTypeContext.identifier()) : null;
        // content
        TextParserGrammar.TextContentContext textContentContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.textContent(), "content", text.sourceInformation);
        text.content = PureGrammarParserUtility.fromGrammarString(textContentContext.STRING().getText(), true);
        return text;
    }
}
