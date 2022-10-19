package org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation;

import org.eclipse.collections.impl.utility.Iterate;

import java.util.Collections;
import java.util.List;

public class ValidationRuleSet<T>
{
    private static final ValidationRuleSet<Object> EMPTY = new ValidationRuleSet<>("EMPTY", Collections.emptyList());

    @SuppressWarnings("unchecked")
    public static <R> ValidationRuleSet<R> empty()
    {
        return (ValidationRuleSet<R>) EMPTY;
    }

    private final String name;
    private final List<ValidationRule<T>> rules;

    public ValidationRuleSet(String name, List<ValidationRule<T>> rules)
    {
        this.name = name;
        this.rules = rules;
    }

    public String name()
    {
        return name;
    }

    public ValidationResult validate(T object)
    {
        return Iterate.injectInto(ValidationResult.success(), rules, (result, rule) -> result.combine(rule.execute(object)));
    }
}
