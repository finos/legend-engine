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
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;

import java.util.List;

public class AuthenticationGrammarComposerExtension implements IAuthenticationGrammarComposerExtension
{
    @Override
    public List<Function2<Pair<String, AuthenticationSpecification>, Integer, String>> getExtraAuthenticationSpecificationComposers()
    {
        return Lists.mutable.with((authenticationSpecPair, baseIndentation) ->
        {
            String securitySchemeId = authenticationSpecPair.getOne();
            AuthenticationSpecification spec = authenticationSpecPair.getTwo();

            if (spec instanceof UserPasswordAuthenticationSpecification || spec instanceof ApiKeyAuthenticationSpecification)
            {
                return spec.accept(new AuthenticationSpecificationComposer(baseIndentation, null));
            }

            return null;
        });
    }
}