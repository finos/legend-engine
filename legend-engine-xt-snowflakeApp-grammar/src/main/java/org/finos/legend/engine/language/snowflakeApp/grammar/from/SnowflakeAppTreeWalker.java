//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.snowflakeApp.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.SnowflakeAppParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.snowflakeApp.metamodel.SnowflakeApp;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class SnowflakeAppTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public SnowflakeAppTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(SnowflakeAppParserGrammar.DefinitionContext ctx)
    {
        ctx.snowflakeApp().stream().map(this::visitSnowflakeApp).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private SnowflakeApp visitSnowflakeApp(SnowflakeAppParserGrammar.SnowflakeAppContext ctx)
    {
        SnowflakeApp snowflakeApp = new SnowflakeApp();
        snowflakeApp.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        snowflakeApp._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        snowflakeApp.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        snowflakeApp.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        snowflakeApp.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        SnowflakeAppParserGrammar.ApplicationNameContext applicationNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.applicationName(), "applicationName", snowflakeApp.sourceInformation);
        snowflakeApp.applicationName = PureGrammarParserUtility.fromGrammarString(applicationNameContext.STRING().getText(), true);
        SnowflakeAppParserGrammar.FunctionContext functionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.function(), "function", snowflakeApp.sourceInformation);
        snowflakeApp.function = functionContext.functionIdentifier().getText();
        SnowflakeAppParserGrammar.OwnerContext ownerContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.owner(), "owner", snowflakeApp.sourceInformation);
        if (ownerContext != null)
        {
            snowflakeApp.owner = PureGrammarParserUtility.fromGrammarString(ownerContext.STRING().getText(), true);
        }
        SnowflakeAppParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", snowflakeApp.sourceInformation);
        if (descriptionContext != null)
        {
            snowflakeApp.description = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);
        }
        return snowflakeApp;
    }

    private List<TaggedValue> visitTaggedValues(SnowflakeAppParserGrammar.TaggedValuesContext ctx)
    {
        return ListIterate.collect(ctx.taggedValue(), taggedValueContext ->
        {
            TaggedValue taggedValue = new TaggedValue();
            TagPtr tagPtr = new TagPtr();
            taggedValue.tag = tagPtr;
            tagPtr.profile = PureGrammarParserUtility.fromQualifiedName(taggedValueContext.qualifiedName().packagePath() == null ? Collections.emptyList() : taggedValueContext.qualifiedName().packagePath().identifier(), taggedValueContext.qualifiedName().identifier());
            tagPtr.value = PureGrammarParserUtility.fromIdentifier(taggedValueContext.identifier());
            taggedValue.value = PureGrammarParserUtility.fromGrammarString(taggedValueContext.STRING().getText(), true);
            taggedValue.tag.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext.qualifiedName());
            taggedValue.tag.sourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext.identifier());
            taggedValue.sourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext);
            return taggedValue;
        });
    }

    private List<StereotypePtr> visitStereotypes(SnowflakeAppParserGrammar.StereotypesContext ctx)
    {
        return ListIterate.collect(ctx.stereotype(), stereotypeContext ->
        {
            StereotypePtr stereotypePtr = new StereotypePtr();
            stereotypePtr.profile = PureGrammarParserUtility.fromQualifiedName(stereotypeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : stereotypeContext.qualifiedName().packagePath().identifier(), stereotypeContext.qualifiedName().identifier());
            stereotypePtr.value = PureGrammarParserUtility.fromIdentifier(stereotypeContext.identifier());
            stereotypePtr.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.qualifiedName());
            stereotypePtr.sourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext);
            return stereotypePtr;
        });
    }
}
