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

import org.antlr.v4.runtime.ParserRuleContext;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.relationFunctionMapping.RelationFunctionMappingParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.LocalMappingPropertyInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping;

import java.util.Collections;
import java.util.List;
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

    public void visitRelationFunctionMapping(RelationFunctionMappingParserGrammar.RelationMappingContext ctx, RelationFunctionClassMapping relationFunctionClassMapping)
    {
        relationFunctionClassMapping.relationFunction = new PackageableElementPointer(PackageableElementType.FUNCTION, ctx.functionIdentifier().getText(), walkerSourceInformation.getSourceInformation(ctx.functionIdentifier()));
        relationFunctionClassMapping.propertyMappings = ctx.singlePropertyMapping()
                .stream()
                .map(c -> this.visitRelationPropertyMapping(c, relationFunctionClassMapping))
                .collect(Collectors.toList());
    }

    private PropertyMapping visitRelationPropertyMapping(RelationFunctionMappingParserGrammar.SinglePropertyMappingContext ctx, RelationFunctionClassMapping relationFunctionClassMapping)
    {
        RelationFunctionPropertyMapping propertyMapping = new RelationFunctionPropertyMapping();
        if (ctx.PLUS()  != null)
        {
            LocalMappingPropertyInfo localMappingPropertyInfo = new LocalMappingPropertyInfo();
            localMappingPropertyInfo.type = ctx.type().getText();
            RelationFunctionMappingParserGrammar.MultiplicityArgumentContext mulCtx = ctx.multiplicity().multiplicityArgument();
            localMappingPropertyInfo.multiplicity = PureGrammarParserUtility.buildMultiplicity(mulCtx.fromMultiplicity(), mulCtx.toMultiplicity());
            localMappingPropertyInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx.qualifiedName());
            propertyMapping.localMappingProperty = localMappingPropertyInfo;
        }

        PropertyPointer propertyPointer = new PropertyPointer();
        propertyPointer.property = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        propertyPointer._class = relationFunctionClassMapping._class;
        propertyMapping.property = propertyPointer;
        
        propertyMapping.column = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        propertyMapping.source = relationFunctionClassMapping.id;
        
        propertyPointer.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx.qualifiedName());
        propertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return propertyMapping;
    }

}
