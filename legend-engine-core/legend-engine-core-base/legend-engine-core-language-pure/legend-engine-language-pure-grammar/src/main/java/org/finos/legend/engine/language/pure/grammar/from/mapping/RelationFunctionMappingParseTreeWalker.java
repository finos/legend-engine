// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.mapping;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.relationFunctionMapping.RelationFunctionMappingParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.BindingTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.LocalMappingPropertyInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionEmbeddedPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping;

import java.util.Collections;
import java.util.stream.Collectors;

public class RelationFunctionMappingParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext parserContext;
    private final MappingElementSourceCode mappingElementSourceCode;

    public RelationFunctionMappingParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserContext parserContext, MappingElementSourceCode mappingElementSourceCode)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.parserContext = parserContext;
        this.mappingElementSourceCode = mappingElementSourceCode;
    }

    public void visitRelationFunctionClassMapping(RelationFunctionMappingParserGrammar.RelationFunctionMappingContext ctx, RelationFunctionClassMapping relationFunctionClassMapping)
    {
        relationFunctionClassMapping.relationFunction = new PackageableElementPointer(PackageableElementType.FUNCTION, ctx.functionIdentifier().getText(), walkerSourceInformation.getSourceInformation(ctx.functionIdentifier()));
        if (ctx.primaryKey() != null)
        {
            relationFunctionClassMapping.primaryKey = ctx.primaryKey().identifier()
                    .stream()
                    .map(PureGrammarParserUtility::fromIdentifier)
                    .collect(Collectors.toList());
        }
        else
        {
            relationFunctionClassMapping.primaryKey = Collections.emptyList();
        }
        relationFunctionClassMapping.propertyMappings = ctx.singlePropertyMapping()
                .stream()
                .map(c -> this.visitPropertyMapping(c, relationFunctionClassMapping._class, relationFunctionClassMapping.id, relationFunctionClassMapping))
                .collect(Collectors.toList());
    }

    private PropertyMapping visitPropertyMapping(RelationFunctionMappingParserGrammar.SinglePropertyMappingContext ctx, String ownerClass, String sourceId, RelationFunctionClassMapping relationFunctionClassMapping)
    {
        RelationFunctionPropertyMapping propertyMapping = new RelationFunctionPropertyMapping();
        PropertyPointer propertyPointer = new PropertyPointer();
        propertyPointer._class = ownerClass;
        propertyMapping.property = propertyPointer;
        
        if (ctx.singleLocalPropertyMapping()  != null)
        {
            RelationFunctionMappingParserGrammar.SingleLocalPropertyMappingContext localPropertyMappingCtx = ctx.singleLocalPropertyMapping();
            LocalMappingPropertyInfo localMappingPropertyInfo = new LocalMappingPropertyInfo();
            localMappingPropertyInfo.type = localPropertyMappingCtx.type().getText();
            RelationFunctionMappingParserGrammar.MultiplicityArgumentContext mulCtx = localPropertyMappingCtx.multiplicity().multiplicityArgument();
            localMappingPropertyInfo.multiplicity = PureGrammarParserUtility.buildMultiplicity(mulCtx.fromMultiplicity(), mulCtx.toMultiplicity());
            localMappingPropertyInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(localPropertyMappingCtx.qualifiedName());
            propertyMapping.localMappingProperty = localMappingPropertyInfo;
            visitRelationFunctionPropertyMapping(localPropertyMappingCtx.relationFunctionPropertyMapping(), propertyMapping);
            propertyPointer.property = PureGrammarParserUtility.fromQualifiedName(localPropertyMappingCtx.qualifiedName().packagePath() == null ? Collections.emptyList() : localPropertyMappingCtx.qualifiedName().packagePath().identifier(), localPropertyMappingCtx.qualifiedName().identifier());
            propertyPointer.sourceInformation = this.walkerSourceInformation.getSourceInformation(localPropertyMappingCtx.qualifiedName());
            propertyMapping.source = sourceId;
            propertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
            return propertyMapping;
        }
        else if (ctx.singleNonLocalPropertyMapping() != null)
        {
            RelationFunctionMappingParserGrammar.SingleNonLocalPropertyMappingContext propertyMappingCtx = ctx.singleNonLocalPropertyMapping();

            if (propertyMappingCtx.relationFunctionEmbeddedPropertyMapping() != null)
            {
                return visitRelationFunctionEmbeddedPropertyMapping(propertyMappingCtx, ownerClass, sourceId, relationFunctionClassMapping);
            }
            else if (propertyMappingCtx.inlineRelationFunctionEmbeddedPropertyMapping() != null)
            {
                return visitInlineRelationFunctionEmbeddedPropertyMapping(propertyMappingCtx, ownerClass, sourceId);
            }
            else
            {
                visitRelationFunctionPropertyMapping(propertyMappingCtx.relationFunctionPropertyMapping(), propertyMapping);
                propertyPointer.property = PureGrammarParserUtility.fromQualifiedName(propertyMappingCtx.qualifiedName().packagePath() == null ? Collections.emptyList() : propertyMappingCtx.qualifiedName().packagePath().identifier(), propertyMappingCtx.qualifiedName().identifier());
                propertyPointer.sourceInformation = this.walkerSourceInformation.getSourceInformation(propertyMappingCtx.qualifiedName());

                propertyMapping.source = sourceId;
                propertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
                return propertyMapping;
            }
        }

        throw new UnsupportedOperationException("Unsupported property mapping type");
    }

    private void visitRelationFunctionPropertyMapping(RelationFunctionMappingParserGrammar.RelationFunctionPropertyMappingContext ctx, RelationFunctionPropertyMapping propertyMapping)
    {
        propertyMapping.column = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        if (ctx.transformer() != null)
        {
            if (ctx.transformer().bindingTransformer() != null)
            {
                RelationFunctionMappingParserGrammar.QualifiedNameContext bindingNameCtx = ctx.transformer().bindingTransformer().qualifiedName();
                BindingTransformer bindingTransformer = new BindingTransformer();
                bindingTransformer.binding = PureGrammarParserUtility.fromQualifiedName(bindingNameCtx.packagePath() == null ? Collections.emptyList() : bindingNameCtx.packagePath().identifier(), bindingNameCtx.identifier());
                bindingTransformer.sourceInformation = this.walkerSourceInformation.getSourceInformation(bindingNameCtx);
                propertyMapping.bindingTransformer = bindingTransformer;
            }
            else if (ctx.transformer().enumTransformer() != null)
            {
                propertyMapping.enumMappingId = PureGrammarParserUtility.fromIdentifier(ctx.transformer().enumTransformer().identifier());
            }
        }
    }

    private PropertyMapping visitRelationFunctionEmbeddedPropertyMapping(RelationFunctionMappingParserGrammar.SingleNonLocalPropertyMappingContext propertyMappingCtx, String ownerClass, String sourceId, RelationFunctionClassMapping relationFunctionClassMapping)
    {
        RelationFunctionEmbeddedPropertyMapping embeddedMapping = new RelationFunctionEmbeddedPropertyMapping();
        PropertyPointer propertyPointer = new PropertyPointer();
        propertyPointer._class = ownerClass;
        propertyPointer.property = PureGrammarParserUtility.fromQualifiedName(propertyMappingCtx.qualifiedName().packagePath() == null ? Collections.emptyList() : propertyMappingCtx.qualifiedName().packagePath().identifier(), propertyMappingCtx.qualifiedName().identifier());
        propertyPointer.sourceInformation = this.walkerSourceInformation.getSourceInformation(propertyMappingCtx.qualifiedName());
        embeddedMapping.property = propertyPointer;
        embeddedMapping.source = sourceId;
        embeddedMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(propertyMappingCtx);

        RelationFunctionMappingParserGrammar.RelationFunctionEmbeddedPropertyMappingContext embeddedCtx = propertyMappingCtx.relationFunctionEmbeddedPropertyMapping();
        embeddedMapping.propertyMappings = embeddedCtx.singlePropertyMapping()
                .stream()
                .map(c -> this.visitPropertyMapping(c, null, null, relationFunctionClassMapping))
                .collect(Collectors.toList());


        return embeddedMapping;
    }

    private PropertyMapping visitInlineRelationFunctionEmbeddedPropertyMapping(RelationFunctionMappingParserGrammar.SingleNonLocalPropertyMappingContext propertyMappingCtx, String ownerClass, String sourceId)
    {
        RelationFunctionEmbeddedPropertyMapping embeddedMapping = new RelationFunctionEmbeddedPropertyMapping();
        PropertyPointer propertyPointer = new PropertyPointer();
        propertyPointer._class = ownerClass;
        propertyPointer.property = PureGrammarParserUtility.fromQualifiedName(propertyMappingCtx.qualifiedName().packagePath() == null ? Collections.emptyList() : propertyMappingCtx.qualifiedName().packagePath().identifier(), propertyMappingCtx.qualifiedName().identifier());
        propertyPointer.sourceInformation = this.walkerSourceInformation.getSourceInformation(propertyMappingCtx.qualifiedName());
        embeddedMapping.property = propertyPointer;
        embeddedMapping.source = sourceId;
        embeddedMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(propertyMappingCtx);

        RelationFunctionMappingParserGrammar.InlineRelationFunctionEmbeddedPropertyMappingContext inlineCtx = propertyMappingCtx.inlineRelationFunctionEmbeddedPropertyMapping();
        embeddedMapping.id = PureGrammarParserUtility.fromIdentifier(inlineCtx.identifier());
        embeddedMapping.propertyMappings = Collections.emptyList();

        return embeddedMapping;
    }

}
