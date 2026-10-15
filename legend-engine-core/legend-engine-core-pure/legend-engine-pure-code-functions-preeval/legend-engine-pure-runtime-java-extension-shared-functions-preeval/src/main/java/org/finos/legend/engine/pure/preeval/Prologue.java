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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

public final class Prologue
{
    private final FunctionExpression original;
    private final FunctionExpression rewritten;
    private final ImmutableList<PrevalResult> parameters;
    private final boolean modified;
    private final ImmutableList<String> openVars;
    private final PrevalState state;
    private final String notPrevalReason;

    public Prologue(FunctionExpression original, FunctionExpression rewritten, ImmutableList<PrevalResult> parameters, boolean modified, ImmutableList<String> openVars, PrevalState state, String notPrevalReason)
    {
        this.original = original;
        this.rewritten = rewritten;
        this.parameters = parameters;
        this.modified = modified;
        this.openVars = openVars;
        this.state = state;
        this.notPrevalReason = notPrevalReason;
    }

    public FunctionExpression original()
    {
        return this.original;
    }

    public FunctionExpression rewritten()
    {
        return this.rewritten;
    }

    public ImmutableList<PrevalResult> parameters()
    {
        return this.parameters;
    }

    public boolean modified()
    {
        return this.modified;
    }

    public ImmutableList<String> openVars()
    {
        return this.openVars;
    }

    public PrevalState state()
    {
        return this.state;
    }

    public String notPrevalReason()
    {
        return this.notPrevalReason;
    }

    public ImmutableList<ValueSpecification> rewrittenParameters()
    {
        return Lists.immutable.withAll(this.rewritten._parametersValues());
    }

    public PrevalResult notPrevalled()
    {
        return new PrevalResult(this.rewritten, PrevalResult.allCanPreval(this.parameters), this.openVars, this.modified);
    }

    public Prologue withNotPrevalReason(String reason)
    {
        return new Prologue(this.original, this.rewritten, this.parameters, this.modified, this.openVars, this.state, reason);
    }
}
