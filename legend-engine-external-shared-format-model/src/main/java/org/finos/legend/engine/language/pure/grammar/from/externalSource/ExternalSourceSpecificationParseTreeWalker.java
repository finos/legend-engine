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

package org.finos.legend.engine.language.pure.grammar.from.externalSource;

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.externalSource.ExternalSourceSpecificationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalSource;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ParameterExternalSource;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.UrlStreamExternalSource;

public class ExternalSourceSpecificationParseTreeWalker
{
    public ExternalSource visitUrlStreamExternalSourceSpecification(ExternalSourceSpecificationSourceCode code, ExternalSourceSpecificationParserGrammar.UrlStreamExternalSourceSpecificationContext ctx)
    {
        UrlStreamExternalSource extSource = new UrlStreamExternalSource();
        extSource.sourceInformation = code.getSourceInformation();
        ExternalSourceSpecificationParserGrammar.UrlStreamUrlContext urlCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.urlStreamUrl(), "url", code.getSourceInformation());
        extSource.url = PureGrammarParserUtility.fromGrammarString(urlCtx.STRING().getText(), true);
        return extSource;
    }

    public ExternalSource visitParameterExternalSourceSpecification(ExternalSourceSpecificationSourceCode code, ExternalSourceSpecificationParserGrammar.ParameterExternalSourceSpecificationContext ctx)
    {
        ParameterExternalSource extSource = new ParameterExternalSource();
        extSource.sourceInformation = code.getSourceInformation();
        ExternalSourceSpecificationParserGrammar.ParameterNameContext parameterNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.parameterName(), "name", code.getSourceInformation());
        extSource.name = PureGrammarParserUtility.fromGrammarString(parameterNameContext.STRING().getText(), true);
        return extSource;
    }
}
