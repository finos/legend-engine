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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.antlr.v4.runtime.misc.Interval;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataProductParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.HelperEmbeddedDataGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProduct;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductDiagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductExecutable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductSupportCombinedInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductSupportEmail;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductSupportInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductTemplateExecutable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataProduct.DataProductPackageableElementExecutable;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TagPtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class DataProductParseTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;
    private final PureGrammarParserContext context;

    public DataProductParseTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section, PureGrammarParserContext context)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.context = context;
    }

    public void visit(DataProductParserGrammar.DefinitionContext ctx)
    {
        ctx.dataProductElement().stream().map(this::visitDataProduct).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private DataProduct visitDataProduct(DataProductParserGrammar.DataProductElementContext ctx)
    {
        DataProduct dataProduct = new DataProduct();
        dataProduct.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        dataProduct._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        dataProduct.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        dataProduct.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        dataProduct.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        // Execution contexts
        DataProductParserGrammar.ExecutionContextsContext executionContextsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executionContexts(), "executionContexts", dataProduct.sourceInformation);
        dataProduct.executionContexts = ListIterate.collect(executionContextsContext.executionContext(), executionContext -> this.visitDataProductExecutionContext(executionContext));

        // Default execution context
        DataProductParserGrammar.DefaultExecutionContextContext defaultExecutionContextContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.defaultExecutionContext(), "defaultExecutionContext", dataProduct.sourceInformation);
        dataProduct.defaultExecutionContext = PureGrammarParserUtility.fromGrammarString(defaultExecutionContextContext.STRING().getText(), true);

        // Title (optional)
        DataProductParserGrammar.TitleContext titleContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.title(), "title", dataProduct.sourceInformation);
        dataProduct.title = titleContext != null ? PureGrammarParserUtility.fromGrammarString(titleContext.STRING().getText(), true) : null;

        // Description (optional)
        DataProductParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", dataProduct.sourceInformation);
        dataProduct.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // Elements (optional)
        DataProductParserGrammar.ElementsContext elementsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.elements(), "elements", dataProduct.sourceInformation);
        dataProduct.elements = elementsContext != null ? ListIterate.collect(elementsContext.elementScopePath(), elementScopePathContext ->
        {
            DataProductElementPointer pointer = new DataProductElementPointer();
            pointer.path = PureGrammarParserUtility.fromQualifiedName(elementScopePathContext.qualifiedName().packagePath() == null ? Collections.emptyList() : elementScopePathContext.qualifiedName().packagePath().identifier(), elementScopePathContext.qualifiedName().identifier());
            pointer.sourceInformation = walkerSourceInformation.getSourceInformation(elementScopePathContext);
            if (elementScopePathContext.MINUS() != null)
            {
                pointer.exclude = true;
            }
            return pointer;
        }) : null;

        // Executables (optional)
        DataProductParserGrammar.ExecutablesContext executablesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executables(), "executables", dataProduct.sourceInformation);
        dataProduct.executables = executablesContext != null ? ListIterate.collect(executablesContext.executable(), executableContext -> visitDataProductExecutable(executableContext)) : null;

        // Diagrams (optional)
        DataProductParserGrammar.DiagramsContext diagramsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.diagrams(), "diagrams", dataProduct.sourceInformation);
        dataProduct.diagrams = diagramsContext != null ? ListIterate.collect(diagramsContext.diagram(), this::visitDataProductDiagram) : null;

        // Featured diagrams (optional)
        // This has been deprecated in favor of diagrams
        DataProductParserGrammar.FeaturedDiagramsContext featuredDiagramsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.featuredDiagrams(), "featuredDiagrams", dataProduct.sourceInformation);
        if (featuredDiagramsContext != null)
        {
            List<DataProductDiagram> featuredDiagrams = ListIterate.collect(featuredDiagramsContext.qualifiedName(), diagramPathContext ->
            {
                DataProductDiagram diagram = new DataProductDiagram();
                diagram.sourceInformation = this.walkerSourceInformation.getSourceInformation(diagramPathContext);
                diagram.title = "";
                diagram.diagram = new PackageableElementPointer(
                        PureGrammarParserUtility.fromQualifiedName(diagramPathContext.packagePath() == null ? Collections.emptyList() : diagramPathContext.packagePath().identifier(), diagramPathContext.identifier())
                );
                diagram.diagram.sourceInformation = diagram.sourceInformation;
                return diagram;
            });
            if (dataProduct.diagrams != null)
            {
                dataProduct.diagrams.addAll(featuredDiagrams);
            }
            else
            {
                dataProduct.diagrams = featuredDiagrams;
            }
        }

        // Support info (optional)
        DataProductParserGrammar.SupportInfoContext supportInfoContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.supportInfo(), "supportInfo", dataProduct.sourceInformation);
        dataProduct.supportInfo = supportInfoContext != null ? this.visitDataProductSupportInfo(supportInfoContext) : null;

        return dataProduct;
    }

    private DataProductExecutionContext visitDataProductExecutionContext(DataProductParserGrammar.ExecutionContextContext ctx)
    {
        DataProductExecutionContext executionContext = new DataProductExecutionContext();
        executionContext.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // Name
        DataProductParserGrammar.ExecutionContextNameContext executionContextNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executionContextName(), "name", executionContext.sourceInformation);
        executionContext.name = PureGrammarParserUtility.fromGrammarString(executionContextNameContext.STRING().getText(), true);

        // Title (optional)
        DataProductParserGrammar.ExecutionContextTitleContext titleContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executionContextTitle(), "title", executionContext.sourceInformation);
        executionContext.title = titleContext != null ? PureGrammarParserUtility.fromGrammarString(titleContext.STRING().getText(), true) : null;

        // Description (optional)
        DataProductParserGrammar.ExecutionContextDescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executionContextDescription(), "description", executionContext.sourceInformation);
        executionContext.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // Mapping
        DataProductParserGrammar.ExecutionContextMappingContext mappingContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executionContextMapping(), "mapping", executionContext.sourceInformation);
        executionContext.mapping = new PackageableElementPointer(
                PackageableElementType.MAPPING,
                PureGrammarParserUtility.fromQualifiedName(mappingContext.qualifiedName().packagePath() == null ? Collections.emptyList() : mappingContext.qualifiedName().packagePath().identifier(), mappingContext.qualifiedName().identifier())
        );
        executionContext.mapping.sourceInformation = walkerSourceInformation.getSourceInformation(mappingContext);

        // Runtime
        DataProductParserGrammar.ExecutionContextDefaultRuntimeContext defaultRuntimeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executionContextDefaultRuntime(), "defaultRuntime", executionContext.sourceInformation);
        executionContext.defaultRuntime = new PackageableElementPointer(
                PackageableElementType.RUNTIME,
                PureGrammarParserUtility.fromQualifiedName(defaultRuntimeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : defaultRuntimeContext.qualifiedName().packagePath().identifier(), defaultRuntimeContext.qualifiedName().identifier())
        );
        executionContext.defaultRuntime.sourceInformation = walkerSourceInformation.getSourceInformation(defaultRuntimeContext);

        // TestData
        DataProductParserGrammar.ExecutionContextTestDataContext data = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executionContextTestData(), "testData", executionContext.sourceInformation);
        if (data == null)
        {
            executionContext.testData = null;
        }
        else
        {
            EmbeddedData embeddedData = HelperEmbeddedDataGrammarParser.parseEmbeddedData(data.embeddedData(), this.walkerSourceInformation, this.context.getPureGrammarParserExtensions());
            if (!(embeddedData instanceof DataElementReference))
            {
                throw new EngineException("Test data within a DataProduct must be created as a DataElementReference type, not an EmbeddedData type.", walkerSourceInformation.getSourceInformation(data), EngineErrorType.PARSER);
            }
            executionContext.testData = (DataElementReference) embeddedData;
        }

        return executionContext;
    }

    private DataProductExecutable visitDataProductExecutable(DataProductParserGrammar.ExecutableContext ctx)
    {
        if (ctx.executableTemplateQuery() != null && ctx.executableTemplateQuery().size() > 0)
        {
            return visitDataProductTemplateExecutable(ctx);
        }
        else if (ctx.executablePath() != null && ctx.executablePath().size() > 0)
        {
            return visitDataProductPackageableElementExecutable(ctx);
        }
        throw new UnsupportedOperationException("Can't parse unsupported dataProduct executable. please specify token 'execute' or 'query'.");
    }

    private DataProductExecutable visitDataProductPackageableElementExecutable(DataProductParserGrammar.ExecutableContext ctx)
    {
        DataProductPackageableElementExecutable executable = new DataProductPackageableElementExecutable();
        executable.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // ID
        DataProductParserGrammar.ExecutableIdContext executableIdContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executableId(), "id", executable.sourceInformation);
        if (executableIdContext != null)
        {
            executable.id = executableIdContext.VALID_STRING() != null ? executableIdContext.VALID_STRING().getText() : executableIdContext.DECIMAL() != null ? executableIdContext.DECIMAL().getText() : executableIdContext.INTEGER().getText();
        }

        // title
        DataProductParserGrammar.ExecutableTitleContext executableTitleContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executableTitle(), "title", executable.sourceInformation);
        executable.title = PureGrammarParserUtility.fromGrammarString(executableTitleContext.STRING().getText(), true);

        // Description (optional)
        DataProductParserGrammar.ExecutableDescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executableDescription(), "description", executable.sourceInformation);
        executable.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // executionContextKey
        DataProductParserGrammar.ExecutableExecutionContextKeyContext executionContextKeyContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executableExecutionContextKey(), "executionContextKey", executable.sourceInformation);
        if (executionContextKeyContext != null)
        {
            executable.executionContextKey = PureGrammarParserUtility.fromGrammarString(executionContextKeyContext.STRING().getText(), true);
        }

        // executable
        DataProductParserGrammar.ExecutablePathContext executableContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executablePath(), "executable", executable.sourceInformation);
        if (executableContext.functionIdentifier() != null)
        {
            executable.executable = new PackageableElementPointer(PackageableElementType.FUNCTION, executableContext.functionIdentifier().getText(), walkerSourceInformation.getSourceInformation(executableContext.functionIdentifier()));
        }
        else if (executableContext.qualifiedName() != null)
        {
            executable.executable = new PackageableElementPointer(
                    PureGrammarParserUtility.fromQualifiedName(executableContext.qualifiedName().packagePath() == null ? Collections.emptyList() : executableContext.qualifiedName().packagePath().identifier(), executableContext.qualifiedName().identifier())
            );
        }
        else
        {
            throw new UnsupportedOperationException("Can't parse unsupported executable in dataspace packageablement executable");
        }
        executable.executable.sourceInformation = walkerSourceInformation.getSourceInformation(executableContext);

        return executable;
    }

    private DataProductExecutable visitDataProductTemplateExecutable(DataProductParserGrammar.ExecutableContext ctx)
    {
        DataProductTemplateExecutable executable = new DataProductTemplateExecutable();
        SourceInformation sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // ID
        DataProductParserGrammar.ExecutableIdContext executableTemplateQueryIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executableId(), "id", sourceInformation);
        String id = executableTemplateQueryIdContext.VALID_STRING() != null ? executableTemplateQueryIdContext.VALID_STRING().getText() : executableTemplateQueryIdContext.DECIMAL() != null ? executableTemplateQueryIdContext.DECIMAL().getText() : executableTemplateQueryIdContext.INTEGER().getText();

        // Title
        DataProductParserGrammar.ExecutableTitleContext executableTitleContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executableTitle(), "title", sourceInformation);
        String title = PureGrammarParserUtility.fromGrammarString(executableTitleContext.STRING().getText(), true);

        // Description (optional)
        DataProductParserGrammar.ExecutableDescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executableDescription(), "description", sourceInformation);
        String description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // executionContextKey
        DataProductParserGrammar.ExecutableExecutionContextKeyContext executionContextKeyContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executableExecutionContextKey(), "executionContextKey", sourceInformation);
        String executionContextKey = executionContextKeyContext != null ? PureGrammarParserUtility.fromGrammarString(executionContextKeyContext.STRING().getText(), true) : null;

        // query
        DataProductParserGrammar.ExecutableTemplateQueryContext queryContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executableTemplateQuery(), "query", sourceInformation);
        executable.query = visitLambda(queryContext.combinedExpression());
        executable.id = id;
        executable.description = description;
        executable.title = title;
        executable.executionContextKey = executionContextKey;
        executable.sourceInformation = sourceInformation;
        return executable;
    }

    private LambdaFunction visitLambda(DataProductParserGrammar.CombinedExpressionContext ctx)
    {
        DomainParser parser = new DomainParser();
        // prepare island grammar walker source information
        int startLine = ctx.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combineExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        String lambdaString = this.input.getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        ValueSpecification valueSpecification = parser.parseCombinedExpression(lambdaString, combineExpressionSourceInformation, null);
        if (valueSpecification instanceof LambdaFunction)
        {
            return (LambdaFunction) valueSpecification;
        }
        // NOTE: If the user just provides the body of the lambda, we will wrap a lambda around it
        // we might want to reconsider this behavior and throw error if this convenience causes any trouble
        LambdaFunction lambda = new LambdaFunction();
        lambda.body = new ArrayList<>();
        lambda.body.add(valueSpecification);
        lambda.parameters = new ArrayList<>();
        return lambda;
    }

    private DataProductDiagram visitDataProductDiagram(DataProductParserGrammar.DiagramContext ctx)
    {
        DataProductDiagram diagram = new DataProductDiagram();
        diagram.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // Name
        DataProductParserGrammar.DiagramTitleContext executableTitleContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.diagramTitle(), "title", diagram.sourceInformation);
        diagram.title = PureGrammarParserUtility.fromGrammarString(executableTitleContext.STRING().getText(), true);

        // Description (optional)
        DataProductParserGrammar.DiagramDescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.diagramDescription(), "description", diagram.sourceInformation);
        diagram.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // Path
        DataProductParserGrammar.DiagramPathContext pathContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.diagramPath(), "diagram", diagram.sourceInformation);
        diagram.diagram = new PackageableElementPointer(
                PureGrammarParserUtility.fromQualifiedName(pathContext.qualifiedName().packagePath() == null ? Collections.emptyList() : pathContext.qualifiedName().packagePath().identifier(), pathContext.qualifiedName().identifier())
        );
        diagram.diagram.sourceInformation = walkerSourceInformation.getSourceInformation(pathContext);

        return diagram;
    }

    private DataProductSupportInfo visitDataProductSupportInfo(DataProductParserGrammar.SupportInfoContext ctx)
    {
        if (ctx.supportCombinedInfo() != null)
        {
            DataProductParserGrammar.SupportCombinedInfoContext combinedInfoContext = ctx.supportCombinedInfo();
            DataProductSupportCombinedInfo supportInfo = new DataProductSupportCombinedInfo();
            supportInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(combinedInfoContext);

            // Documentation URL (optional)
            DataProductParserGrammar.SupportDocumentationUrlContext documentationUrlContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.supportDocumentationUrl(), "documentationUrl", supportInfo.sourceInformation);
            supportInfo.documentationUrl = documentationUrlContext != null ? PureGrammarParserUtility.fromGrammarString(documentationUrlContext.STRING().getText(), true) : null;

            // Emails (optional)
            DataProductParserGrammar.CombinedInfoEmailsContext emailsContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.combinedInfoEmails(), "emails", supportInfo.sourceInformation);
            supportInfo.emails = emailsContext != null ? ListIterate.collect(emailsContext.STRING(), val -> PureGrammarParserUtility.fromGrammarString(val.getText(), true)) : null;

            // Website (optional)
            DataProductParserGrammar.CombinedInfoWebsiteContext websiteContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.combinedInfoWebsite(), "website", supportInfo.sourceInformation);
            supportInfo.website = websiteContext != null ? PureGrammarParserUtility.fromGrammarString(websiteContext.STRING().getText(), true) : null;

            // FAQ URL (optional)
            DataProductParserGrammar.CombinedInfoFaqUrlContext faqUrlContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.combinedInfoFaqUrl(), "faqUrl", supportInfo.sourceInformation);
            supportInfo.faqUrl = faqUrlContext != null ? PureGrammarParserUtility.fromGrammarString(faqUrlContext.STRING().getText(), true) : null;

            // Support URL (optional)
            DataProductParserGrammar.CombinedInfoSupportUrlContext supportUrlContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.combinedInfoSupportUrl(), "supportUrl", supportInfo.sourceInformation);
            supportInfo.supportUrl = supportUrlContext != null ? PureGrammarParserUtility.fromGrammarString(supportUrlContext.STRING().getText(), true) : null;

            return supportInfo;

        }
        else if (ctx.supportEmail() != null)
        {
            DataProductParserGrammar.SupportEmailContext supportEmailContext = ctx.supportEmail();
            DataProductSupportEmail supportInfo = new DataProductSupportEmail();
            supportInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(supportEmailContext);

            // Documentation URL (optional)
            DataProductParserGrammar.SupportDocumentationUrlContext documentationUrlContext = PureGrammarParserUtility.validateAndExtractOptionalField(supportEmailContext.supportDocumentationUrl(), "documentationUrl", supportInfo.sourceInformation);
            supportInfo.documentationUrl = documentationUrlContext != null ? PureGrammarParserUtility.fromGrammarString(documentationUrlContext.STRING().getText(), true) : null;

            // Address
            DataProductParserGrammar.SupportEmailAddressContext supportEmailAddressContext = PureGrammarParserUtility.validateAndExtractRequiredField(supportEmailContext.supportEmailAddress(), "address", supportInfo.sourceInformation);
            supportInfo.address = PureGrammarParserUtility.fromGrammarString(supportEmailAddressContext.STRING().getText(), true);

            return supportInfo;
        }
        throw new UnsupportedOperationException("Can't parse unsupported support info");
    }

    private List<TaggedValue> visitTaggedValues(DataProductParserGrammar.TaggedValuesContext ctx)
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

    private List<StereotypePtr> visitStereotypes(DataProductParserGrammar.StereotypesContext ctx)
    {
        return ListIterate.collect(ctx.stereotype(), stereotypeContext ->
        {
            StereotypePtr stereotypePtr = new StereotypePtr();
            stereotypePtr.profile = PureGrammarParserUtility.fromQualifiedName(stereotypeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : stereotypeContext.qualifiedName().packagePath().identifier(), stereotypeContext.qualifiedName().identifier());
            stereotypePtr.value = PureGrammarParserUtility.fromIdentifier(stereotypeContext.identifier());
            stereotypePtr.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.qualifiedName());
            stereotypePtr.sourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.identifier());
            return stereotypePtr;
        });
    }
}
