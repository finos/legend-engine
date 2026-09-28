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

package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.pure.preeval.rules.AndOrRule;
import org.finos.legend.engine.pure.preeval.rules.IfRule;
import org.finos.legend.engine.pure.preeval.rules.ReactivateRule;

public final class Rules
{
    public static final ImmutableList<PreParameterRule> PRE_PARAMETER = Lists.immutable.with(new IfRule(), new AndOrRule());
    public static final ImmutableList<ExpressionRule> EXPANSION = Lists.immutable.empty();
    public static final ImmutableList<ExpressionRule> NOT_PREVALLED = Lists.immutable.empty();
    public static final ExpressionRule REACTIVATE = new ReactivateRule();

    private Rules()
    {
    }
}
