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

package org.finos.legend.engine.language.pure.dsl.authentication.grammar.to;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public interface IAuthenticationGrammarComposerExtension extends PureGrammarComposerExtension
{
    static Stream<IAuthenticationGrammarComposerExtension> getExtensions(PureGrammarComposerContext context)
    {
        return context.extensions.stream()
                .filter(IAuthenticationGrammarComposerExtension.class::isInstance)
                .map(IAuthenticationGrammarComposerExtension.class::cast);
    }

    default List<Function3<AuthenticationSpecification, Integer, PureGrammarComposerContext, String>> getExtraAuthenticationSpecificationComposers()
    {
        return Collections.emptyList();
    }

    default List<Function3<CredentialVaultSecret, Integer, PureGrammarComposerContext, String>> getExtraCredentialVaultSecretComposers()
    {
        return Collections.emptyList();
    }

    static String renderAuthentication(AuthenticationSpecification authenticationSpecification, int indentLevel, PureGrammarComposerContext context)
    {
        return IAuthenticationGrammarComposerExtension.getExtensions(context)
                .map(IAuthenticationGrammarComposerExtension::getExtraAuthenticationSpecificationComposers)
                .flatMap(List::stream)
                .map(x -> x.value(authenticationSpecification, indentLevel, context))
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> new EngineException("No renderer found for " + authenticationSpecification.getClass()));
    }
}