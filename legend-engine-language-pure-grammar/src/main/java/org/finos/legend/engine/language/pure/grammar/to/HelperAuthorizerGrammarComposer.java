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

package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.Authorizer;

import java.util.Objects;
import java.util.Optional;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class HelperAuthorizerGrammarComposer
{
    // WIP: remove this method when we remove the connection visitor
    public static String getAuthorizerValueName(Authorizer authorizer, PureGrammarComposerContext context)
    {
        Optional<org.eclipse.collections.api.tuple.Pair<String, String>> authorizerValueString = context.extraAuthorizerValueComposers.stream().map(composer -> composer.value(authorizer, context)).filter(Objects::nonNull).findFirst();
        return authorizerValueString.orElseGet(() -> Tuples.pair(unsupported(authorizer.getClass(), "authorizer type"), null)).getOne();
    }
}
