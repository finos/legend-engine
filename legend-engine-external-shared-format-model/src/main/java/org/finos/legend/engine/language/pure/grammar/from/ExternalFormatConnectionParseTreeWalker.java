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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ExternalFormatConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.externalSource.ExternalSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatConnection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalSource;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;

public class ExternalFormatConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public ExternalFormatConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public void visitExternalFormatConnectionValue(ExternalFormatConnectionParserGrammar.DefinitionContext ctx, ExternalFormatConnection connectionValue, boolean isEmbedded)
    {
        // store (optional if the store is provided by embedding context, if not provided, it is required)
        ExternalFormatConnectionParserGrammar.ConnectionStoreContext storeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.connectionStore(), "store", connectionValue.sourceInformation);
        if (storeContext != null)
        {
            connectionValue.element = PureGrammarParserUtility.fromQualifiedName(storeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : storeContext.qualifiedName().packagePath().identifier(), storeContext.qualifiedName().identifier());
            connectionValue.elementSourceInformation = this.walkerSourceInformation.getSourceInformation(storeContext.qualifiedName());
        }
        else if (!isEmbedded)
        {
            // non-embedded connection requires store
            PureGrammarParserUtility.validateAndExtractRequiredField(ctx.connectionStore(), "store", connectionValue.sourceInformation);
        }
        // source
        ExternalFormatConnectionParserGrammar.ExternalSourceContext externalSourceCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.externalSource(), "source", connectionValue.sourceInformation);
        connectionValue.externalSource = this.visitExternalFormatConnectionDatasourceSpecification(externalSourceCtx);
    }

    private ExternalSource visitExternalFormatConnectionDatasourceSpecification(ExternalFormatConnectionParserGrammar.ExternalSourceContext ctx)
    {
        ExternalFormatConnectionParserGrammar.SpecificationContext specification = ctx.specification();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        ExternalSourceSpecificationSourceCode code = new ExternalSourceSpecificationSourceCode(
                ctx.specification().getText(),
                specification.specificationType().getText(),
                sourceInformation,
                new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation)
                        .withLineOffset(sourceInformation.startLine - 1)
                        .withColumnOffset(sourceInformation.startColumn)
                        .build()
        );

        List<IExternalFormatGrammarParserExtension> extensions = IExternalFormatGrammarParserExtension.getExtensions();
        ExternalSource result = IExternalFormatGrammarParserExtension.process(code, ListIterate.flatCollect(extensions, IExternalFormatGrammarParserExtension::getExtraExternalSourceSpecificationParsers));

        if (result == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

        return result;
    }
}
