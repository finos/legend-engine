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

package org.finos.legend.engine.language.pure.grammar.from.mapping;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.enumerationMapping.EnumerationMappingParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingEnumSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingIntegerSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingStringSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumerationMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.Collections;

public class EnumerationMappingParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public EnumerationMappingParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public void visitEnumerationMapping(EnumerationMappingParserGrammar.EnumerationMappingContext ctx, EnumerationMapping enumerationMapping)
    {
        enumerationMapping.enumValueMappings = ListIterate.collect(ctx.enumSingleEntryMapping(), this::visitEnumValueMapping);
    }

    public EnumValueMapping visitEnumValueMapping(EnumerationMappingParserGrammar.EnumSingleEntryMappingContext enumerationMappingContext)
    {
        EnumValueMapping enumValueMapping = new EnumValueMapping();
        enumValueMapping.enumValue = enumerationMappingContext.enumName().getText();
        enumValueMapping.sourceValues = new ArrayList<>();
        if (enumerationMappingContext.enumMultipleSourceValue() != null && enumerationMappingContext.enumMultipleSourceValue().enumSourceValue() != null)
        {
            enumValueMapping.sourceValues = ListIterate.collect(enumerationMappingContext.enumMultipleSourceValue().enumSourceValue(), this::visitSourceValue);
        }
        else if (enumerationMappingContext.enumSourceValue() != null)
        {
            enumValueMapping.sourceValues.add(this.visitSourceValue(enumerationMappingContext.enumSourceValue()));
        }
        return enumValueMapping;
    }

    Object visitSourceValue(EnumerationMappingParserGrammar.EnumSourceValueContext ctx)
    {
        if (ctx.STRING() != null)
        {
            EnumValueMappingStringSourceValue sourceValue = new EnumValueMappingStringSourceValue();
            sourceValue.value = PureGrammarParserUtility.fromGrammarString(ctx.STRING().getText(), true);
            return sourceValue;
        }
        if (ctx.INTEGER() != null)
        {
            EnumValueMappingIntegerSourceValue sourceValue = new EnumValueMappingIntegerSourceValue();
            sourceValue.value = Integer.parseInt(ctx.INTEGER().getText());
            return sourceValue;
        }
        if (ctx.enumReference() != null)
        {
            EnumValueMappingEnumSourceValue sourceValue = new EnumValueMappingEnumSourceValue();
            sourceValue.enumeration = PureGrammarParserUtility.fromQualifiedName(ctx.enumReference().qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.enumReference().qualifiedName().packagePath().identifier(), ctx.enumReference().qualifiedName().identifier());
            sourceValue.value = PureGrammarParserUtility.fromIdentifier(ctx.enumReference().identifier());
            return sourceValue;
        }
        throw new EngineException("Source value must be either a string, an integer, or an enum", walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }
}
