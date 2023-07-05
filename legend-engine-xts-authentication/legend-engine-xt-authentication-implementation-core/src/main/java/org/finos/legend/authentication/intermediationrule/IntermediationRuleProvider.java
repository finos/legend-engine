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

package org.finos.legend.authentication.intermediationrule;

import org.eclipse.collections.impl.list.mutable.FastList;

public class IntermediationRuleProvider
{
    private FastList<IntermediationRule> rules = FastList.newList();

    public IntermediationRuleProvider()
    {
        // TODO - load rules from classpath
        throw new UnsupportedOperationException("load rules from classpath");
    }

    public IntermediationRuleProvider(FastList<IntermediationRule> rules)
    {
        this.rules.addAll(rules);
    }

    public FastList<IntermediationRule> getRules()
    {
        return rules;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private FastList<IntermediationRule> rules = FastList.newList();

        public Builder with(IntermediationRule rule)
        {
            this.rules.add(rule);
            return this;
        }

        public IntermediationRuleProvider build()
        {
            return new IntermediationRuleProvider(this.rules);
        }
    }

}

