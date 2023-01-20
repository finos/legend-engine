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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;

import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public interface IAuthenticationGrammarComposerExtension extends PureGrammarComposerExtension
{
    static List<IAuthenticationGrammarComposerExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IAuthenticationGrammarComposerExtension.class));
    }

    default List<Function2<AuthenticationSpecification, Integer, String>> getExtraAuthenticationSpecificationComposers()
    {
        return Collections.emptyList();
    }

    default List<Function2<CredentialVaultSecret, Integer, String>> getExtraCredentialVaultSecretComposers()
    {
        return Collections.emptyList();
    }
}