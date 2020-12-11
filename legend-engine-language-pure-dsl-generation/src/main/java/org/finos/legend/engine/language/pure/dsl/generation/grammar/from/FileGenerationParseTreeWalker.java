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

import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.FileGenerationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.ConfigurationProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FileGenerationParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;

    public FileGenerationParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(FileGenerationParserGrammar.DefinitionContext ctx)
    {
        this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
        ctx.fileGeneration().stream().map(this::visitFileGeneration).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private FileGenerationSpecification visitFileGeneration(FileGenerationParserGrammar.FileGenerationContext ctx)
    {
        FileGenerationSpecification fileGeneration = new FileGenerationSpecification();
        fileGeneration.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        fileGeneration._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        fileGeneration.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        String typeString = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        try
        {
            fileGeneration.type = typeString.substring(0, 1).toLowerCase() + typeString.substring(1);
            fileGeneration.typeSourceInformation = walkerSourceInformation.getSourceInformation(ctx.identifier());
        }
        catch (IllegalArgumentException e)
        {
            String supportedTypes = LazyIterate.collect(Arrays.asList(FileGenerationType.class.getEnumConstants()), Enum::name).makeString(",");
            throw new EngineException("Generation type '" + typeString + "' is not supported. Supported types are: " + supportedTypes + ".", walkerSourceInformation.getSourceInformation(ctx.identifier()), EngineErrorType.PARSER);
        }
        // scopes (optional)
        FileGenerationParserGrammar.ScopeElementsContext scopeElementsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.scopeElements(), "scopeElements", fileGeneration.sourceInformation);
        fileGeneration.scopeElements = scopeElementsContext != null ? ListIterate.collect(scopeElementsContext.elementsArray().qualifiedName(), qn -> PureGrammarParserUtility.fromQualifiedName(qn.packagePath() == null ? Collections.emptyList() : qn.packagePath().identifier(), qn.identifier())) : new ArrayList<>();
        // outputPath
        FileGenerationParserGrammar.GenerationOutputPathContext generationOutputPathContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.generationOutputPath(), "generationOutputPath", fileGeneration.sourceInformation);
        if (generationOutputPathContext != null)
        {
            fileGeneration.generationOutputPath = PureGrammarParserUtility.fromGrammarString(generationOutputPathContext.STRING().getText(), true);
        }
        // config properties
        fileGeneration.configurationProperties = ListIterate.collect(ctx.configProperty(), configPropertyContext -> this.visitConfigurationProperty(configPropertyContext, fileGeneration));
        return fileGeneration;
    }

    private ConfigurationProperty visitConfigurationProperty(FileGenerationParserGrammar.ConfigPropertyContext configPropertyContext, FileGenerationSpecification fileGeneration)
    {
        ConfigurationProperty configurationProperty = new ConfigurationProperty();
        configurationProperty.sourceInformation = walkerSourceInformation.getSourceInformation(configPropertyContext);
        if (configPropertyContext.configPropertyValue() != null)
        {
            configurationProperty.name = configPropertyContext.configPropertyName().getText();
            if ("generationOutputPath".equals(configurationProperty.name) || "scopeElements".equals(configurationProperty.name))
            {
                throw new EngineException("Can't have config property with reserved name '" + configurationProperty.name + "'", fileGeneration.sourceInformation, EngineErrorType.PARSER);
            }
            configurationProperty.value = this.visitConfigPropertyValue(configPropertyContext.configPropertyValue());
        }
        return configurationProperty;
    }

    private Object visitConfigPropertyValue(FileGenerationParserGrammar.ConfigPropertyValueContext configPropertyValueContext)
    {
        if (configPropertyValueContext.INTEGER() != null)
        {
            return this.longFromTerminalNode(configPropertyValueContext.INTEGER());
        }
        else if (configPropertyValueContext.STRING() != null)
        {
            return this.stringFromTerminalNode(configPropertyValueContext.STRING());
        }
        else if (configPropertyValueContext.BOOLEAN() != null)
        {
            return configPropertyValueContext.BOOLEAN().getText().equals("true");
        }
        else if (configPropertyValueContext.integerArray() != null)
        {
            return ListIterate.collect(configPropertyValueContext.integerArray().INTEGER(), this::longFromTerminalNode);
        }
        else if (configPropertyValueContext.stringArray() != null)
        {
            return ListIterate.collect(configPropertyValueContext.stringArray().STRING(), this::stringFromTerminalNode);
        }
        else if (configPropertyValueContext.configMap() != null)
        {
            FileGenerationParserGrammar.ConfigMapContext configMapContext = configPropertyValueContext.configMap();
            Map<String, Object> configMap = new HashMap<>();
            for (FileGenerationParserGrammar.ConfigPropertyContext configPropertyContext : configMapContext.configProperty())
            {
                String name = configPropertyContext.configPropertyName().getText();
                Object val = this.visitConfigPropertyValue(configPropertyContext.configPropertyValue());
                configMap.put(name, val);
            }
            return configMap;
        }
        throw new EngineException("Unsupported config value '" + configPropertyValueContext.getText() + "'", walkerSourceInformation.getSourceInformation(configPropertyValueContext), EngineErrorType.PARSER);
    }

    private String stringFromTerminalNode(TerminalNode terminalNode)
    {
        return PureGrammarParserUtility.fromGrammarString(terminalNode.getText(), true);
    }

    private Long longFromTerminalNode(TerminalNode terminalNode)
    {
        return Long.parseLong(terminalNode.getText());
    }
}
