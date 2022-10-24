// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation;

import org.eclipse.collections.impl.utility.Iterate;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ValidationRuleSet<T>
{
    private static final ValidationRuleSet<Object> EMPTY = new ValidationRuleSet<>("EMPTY", x -> true, Collections.emptyList());

    @SuppressWarnings("unchecked")
    public static <R> ValidationRuleSet<R> empty()
    {
        return (ValidationRuleSet<R>) EMPTY;
    }

    private final String name;
    private final Predicate<T> filter;
    private final List<ValidationRule<T>> rules;

    public ValidationRuleSet(String name, Predicate<T> filter, List<ValidationRule<T>> rules)
    {
        this.name = name;
        this.filter = filter;
        this.rules = rules;
    }

    public String name()
    {
        return name;
    }

    public ValidationResult validate(T object)
    {
        return filter.test(object)
                ? Iterate.injectInto(ValidationResult.success(), rules, (result, rule) -> result.combine(rule.execute(object)))
                : ValidationResult.success();
    }
}
