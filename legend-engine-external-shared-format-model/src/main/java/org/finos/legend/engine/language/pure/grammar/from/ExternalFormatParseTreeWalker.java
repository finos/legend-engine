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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ExternalFormatParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.Binding;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchema;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchemaSet;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class ExternalFormatParseTreeWalker
{
    private final Map<String, ExternalFormatExtension> schemaExtensions;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;

    ExternalFormatParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext parserContext, ImportAwareCodeSection section)
    {
        this.schemaExtensions = ExternalFormatExtensionLoader.extensions();
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    void visit(ExternalFormatParserGrammar.DefinitionContext ctx)
    {
        this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
        ctx.schemaSet().stream().map(this::visitSchemaSet).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        ctx.binding().stream().map(this::visitBinding).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private ExternalFormatSchemaSet visitSchemaSet(ExternalFormatParserGrammar.SchemaSetContext ctx)
    {
        SourceInformation sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        ExternalFormatSchemaSet schemaSet = new ExternalFormatSchemaSet();
        schemaSet.sourceInformation = sourceInformation;
        schemaSet.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        schemaSet._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());

        // format
        ExternalFormatParserGrammar.FormatContext formatContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.format(), "format", sourceInformation);
        String format = formatContext.VALID_STRING().getText();
        SourceInformation formatSourceInformation = this.walkerSourceInformation.getSourceInformation(formatContext);
        ExternalFormatExtension schemaExtension = schemaExtensions.get(format);
        if (schemaExtension == null)
        {
            throw new EngineException("Unknown schema format: " + format, formatSourceInformation, EngineErrorType.PARSER);
        }
        schemaSet.format = format;

        // schemas
        ExternalFormatParserGrammar.SchemasContext schemasContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.schemas(), "schemas", sourceInformation);
        schemaSet.schemas = ListIterate.collect(schemasContext.schema(), this::visitSchema);
        return schemaSet;
    }

    private ExternalFormatSchema visitSchema(ExternalFormatParserGrammar.SchemaContext ctx)
    {
        SourceInformation sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        ExternalFormatSchema schema = new ExternalFormatSchema();
        schema.sourceInformation = sourceInformation;
        // id
        ExternalFormatParserGrammar.SchemaIdContext idContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.schemaId(), "id", sourceInformation);
        schema.id = idContext == null ? null : PureGrammarParserUtility.fromIdentifier(idContext.identifier());
        // location
        ExternalFormatParserGrammar.SchemaLocationContext locationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.schemaLocation(), "location", sourceInformation);
        schema.location = locationContext == null ? null : PureGrammarParserUtility.fromGrammarString(locationContext.STRING().getText(), true);
        // content
        ExternalFormatParserGrammar.SchemaContentContext contentContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.schemaContent(), "content", sourceInformation);
        schema.content = PureGrammarParserUtility.fromGrammarString(contentContext.STRING().getText(), true);
        schema.contentSourceInformation = this.walkerSourceInformation.getSourceInformation(contentContext);
        return schema;
    }

    private Binding visitBinding(ExternalFormatParserGrammar.BindingContext ctx)
    {
        SourceInformation sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        Binding binding = new Binding();
        binding.sourceInformation = sourceInformation;
        binding.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        binding._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        // schemaSet
        ExternalFormatParserGrammar.SchemaSetReferenceContext schemaSetReferenceContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.schemaSetReference(), "schemaSet", sourceInformation);
        binding.schemaSet = schemaSetReferenceContext == null ? null : processQualifiedName(schemaSetReferenceContext.qualifiedName());
        // schemaId
        ExternalFormatParserGrammar.SchemaIdReferenceContext schemaIdReferenceContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.schemaIdReference(), "schemaId", sourceInformation);
        binding.schemaId = schemaIdReferenceContext == null ? null : PureGrammarParserUtility.fromIdentifier(schemaIdReferenceContext.identifier());
        // contentType
        ExternalFormatParserGrammar.ContentTypeContext contentTypeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.contentType(), "contentType", sourceInformation);
        binding.contentType = PureGrammarParserUtility.fromGrammarString(contentTypeContext.STRING().getText(), true);
        // modelIncludes & modelExcludes
        ExternalFormatParserGrammar.ModelIncludesContext modelIncludesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.modelIncludes(), "modelIncludes", sourceInformation);
        ExternalFormatParserGrammar.ModelExcludesContext modelExcludesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.modelExcludes(), "modelExcludes", sourceInformation);
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = modelIncludesContext.qualifiedName().stream().map(this::processQualifiedName).collect(Collectors.toList());
        if (modelExcludesContext != null)
        {
            modelUnit.packageableElementExcludes = modelExcludesContext.qualifiedName().stream().map(this::processQualifiedName).collect(Collectors.toList());
        }
        binding.modelUnit = modelUnit;
        return binding;
    }

    private String processQualifiedName(ExternalFormatParserGrammar.QualifiedNameContext ctx)
    {
        List<? extends ParserRuleContext> packagePath = ctx.packagePath() == null ? Collections.emptyList() : ctx.packagePath().identifier();
        return PureGrammarParserUtility.fromQualifiedName(packagePath, ctx.identifier());
    }
}
