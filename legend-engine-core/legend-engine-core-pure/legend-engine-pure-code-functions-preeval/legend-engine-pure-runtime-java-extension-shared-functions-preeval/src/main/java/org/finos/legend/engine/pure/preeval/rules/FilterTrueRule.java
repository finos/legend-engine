// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.pure.preeval.rules;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.pure.preeval.ExpressionRule;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.Prologue;

public final class FilterTrueRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        return RuleSupport.isFilterReturningConstant(prologue, services, true);
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        services.trace(prologue.state(), () -> "Handling filter which returns true: " + services.runtime().typeDescription(prologue.rewritten()._func()));
        // parity: Pure reports no open variables even when the kept collection has some
        return new PrevalResult(prologue.rewrittenParameters().getFirst(), true, Lists.immutable.empty(), true);
    }
}
