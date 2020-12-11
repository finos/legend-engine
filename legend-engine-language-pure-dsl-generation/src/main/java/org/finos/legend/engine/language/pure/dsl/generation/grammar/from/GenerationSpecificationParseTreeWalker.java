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

package org.finos.legend.engine.language.pure.dsl.generation.grammar.from;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.GenerationSpecificationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationTreeNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;

import java.util.Collections;
import java.util.function.Consumer;

public class GenerationSpecificationParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;

    public GenerationSpecificationParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(GenerationSpecificationParserGrammar.DefinitionContext ctx)
    {
        this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
        ctx.generationSpecification().stream().map(this::visitGenerationSpecification).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private GenerationSpecification visitGenerationSpecification(GenerationSpecificationParserGrammar.GenerationSpecificationContext ctx)
    {
        GenerationSpecification generationSpecification = new GenerationSpecification();
        generationSpecification.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        generationSpecification._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        generationSpecification.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        GenerationSpecificationParserGrammar.GenerationNodesContext generationNodesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.generationNodes(), "generationNodes", generationSpecification.sourceInformation);
        if (generationNodesContext != null)
        {
            generationSpecification.generationNodes = ListIterate.collect(generationNodesContext.generationNodesValues().generationNode(), this::visitGenerationTreeNode);
        }
        GenerationSpecificationParserGrammar.FileGenerationsContext fileGenerationsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.fileGenerations(), "fileGenerations", generationSpecification.sourceInformation);
        if (fileGenerationsContext != null)
        {
            generationSpecification.fileGenerations = ListIterate.collect(fileGenerationsContext.fileGenerationsValues().qualifiedName(), this::visitFileGeneration);
        }
        return generationSpecification;
    }

    private GenerationTreeNode visitGenerationTreeNode(GenerationSpecificationParserGrammar.GenerationNodeContext ctx)
    {
        GenerationTreeNode generationTreeNode = new GenerationTreeNode();
        generationTreeNode.generationElement = PureGrammarParserUtility.fromQualifiedName(ctx.generationElement().qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.generationElement().qualifiedName().packagePath().identifier(), ctx.generationElement().qualifiedName().identifier());
        generationTreeNode.id = ctx.nodeId() != null ? PureGrammarParserUtility.fromGrammarString(ctx.nodeId().STRING().getText(), true) : generationTreeNode.generationElement;
        generationTreeNode.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return generationTreeNode;
    }

    private PackageableElementPointer visitFileGeneration(GenerationSpecificationParserGrammar.QualifiedNameContext ctx)
    {
        PackageableElementPointer packageableElementPointer = new PackageableElementPointer();
        packageableElementPointer.type = PackageableElementType.FILE_GENERATION;
        packageableElementPointer.path = PureGrammarParserUtility.fromQualifiedName(ctx.packagePath() == null ? Collections.emptyList() : ctx.packagePath().identifier(), ctx.identifier());
        packageableElementPointer.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return packageableElementPointer;
    }
}
