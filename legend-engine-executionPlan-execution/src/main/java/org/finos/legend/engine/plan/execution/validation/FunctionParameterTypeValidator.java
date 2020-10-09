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

package org.finos.legend.engine.plan.execution.validation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.result.freemarker.PlanDateParameterDateFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

abstract class FunctionParameterTypeValidator
{
    private static final FunctionParameterTypeValidator strictDateValidator = new FunctionParameterTypeValidator("StrictDate")
    {
        @Override
        protected boolean isValidJavaType(Object parameterValue)
        {
            return parameterValue instanceof LocalDate;
        }

        @Override
        protected boolean canParse(String parameterValue)
        {
            return PlanDateParameterDateFormat.getPlanDateFormatters(false).anySatisfyWith(FunctionParameterTypeValidator::canParseDate, parameterValue);
        }

        @Override
        protected void appendErrorMessageDetail(StringBuilder builder)
        {
            PlanDateParameterDateFormat.getPlanDateFormatters(false).asLazy().collect(PlanDateParameterDateFormat.PlanDateParameterFormatter::getDatePattern).appendString(builder, " Expected formats: [", ",", "]");
        }
    };

    private static final FunctionParameterTypeValidator dateTimeValidator = new FunctionParameterTypeValidator("DateTime")
    {
        @Override
        protected boolean isValidJavaType(Object parameterValue)
        {
            return (parameterValue instanceof LocalDateTime) || (parameterValue instanceof ZonedDateTime) || (parameterValue instanceof Instant);
        }

        @Override
        protected boolean canParse(String parameterValue)
        {
            return PlanDateParameterDateFormat.getPlanDateFormatters(true).anySatisfyWith(FunctionParameterTypeValidator::canParseDate, parameterValue);
        }

        @Override
        protected void appendErrorMessageDetail(StringBuilder builder)
        {
            PlanDateParameterDateFormat.getPlanDateFormatters(true).asLazy().collect(PlanDateParameterDateFormat.PlanDateParameterFormatter::getDatePattern).appendString(builder, " Expected formats: [", ",", "]");
        }
    };

    private static final FunctionParameterTypeValidator dateValidator = new FunctionParameterTypeValidator("Date")
    {
        @Override
        protected boolean isValidJavaType(Object parameterValue)
        {
            return dateTimeValidator.isValid(parameterValue) || strictDateValidator.isValid(parameterValue);
        }

        @Override
        protected boolean canParse(String parameterValue)
        {
            return PlanDateParameterDateFormat.getPlanDateFormatters().anySatisfyWith(FunctionParameterTypeValidator::canParseDate, parameterValue);
        }

        @Override
        protected void appendErrorMessageDetail(StringBuilder builder)
        {
            PlanDateParameterDateFormat.getPlanDateFormatters().asLazy().collect(PlanDateParameterDateFormat.PlanDateParameterFormatter::getDatePattern).appendString(builder, " Expected formats: [", ",", "]");
        }
    };

    private static final FunctionParameterTypeValidator integerValidator = new FunctionParameterTypeValidator("Integer")
    {
        @Override
        protected boolean isValidJavaType(Object parameterValue)
        {
            return (parameterValue instanceof Long) || (parameterValue instanceof Integer);
        }

        @Override
        protected boolean canParse(String parameterValue)
        {
            try
            {
                Long.parseLong(parameterValue);
                return true;
            }
            catch (NumberFormatException ignore)
            {
                return false;
            }
        }
    };

    private static final FunctionParameterTypeValidator floatValidator = new FunctionParameterTypeValidator("Float")
    {
        @Override
        protected boolean isValidJavaType(Object parameterValue)
        {
            return (parameterValue instanceof Double) || (parameterValue instanceof Float);
        }

        @Override
        protected boolean canParse(String parameterValue)
        {
            try
            {
                Double.parseDouble(parameterValue);
                return true;
            }
            catch (NumberFormatException ignore)
            {
                return false;
            }
        }
    };

    private static final FunctionParameterTypeValidator booleanValidator = new FunctionParameterTypeValidator("Boolean")
    {
        @Override
        protected boolean isValidJavaType(Object parameterValue)
        {
            return parameterValue instanceof Boolean;
        }

        @Override
        protected boolean canParse(String parameterValue)
        {
            return "true".equalsIgnoreCase(parameterValue) || "false".equalsIgnoreCase(parameterValue);
        }
    };

    private static final FunctionParameterTypeValidator stringValidator = new FunctionParameterTypeValidator("String")
    {
        @Override
        protected boolean isValidJavaType(Object parameterValue)
        {
            return parameterValue instanceof String;
        }

        @Override
        protected boolean canParse(String parameterValue)
        {
            return true;
        }
    };

    private static final FunctionParameterTypeValidator decimalValidator = new FunctionParameterTypeValidator("Decimal")
    {
        @Override
        protected boolean isValidJavaType(Object parameterValue)
        {
            return parameterValue instanceof BigDecimal;
        }

        @Override
        protected boolean canParse(String parameterValue)
        {
            try
            {
                new BigDecimal(parameterValue);
                return true;
            }
            catch (NumberFormatException ignore)
            {
                return false;
            }
        }
    };

    private static final ImmutableMap<String, FunctionParameterTypeValidator> VALIDATORS = Lists.immutable.with(strictDateValidator, dateTimeValidator, dateValidator, integerValidator, floatValidator, decimalValidator, booleanValidator, stringValidator).groupByUniqueKey(FunctionParameterTypeValidator::getType);

    private final String type;

    private FunctionParameterTypeValidator(String type)
    {
        this.type = type;
    }

    String getType()
    {
        return this.type;
    }

    ValidationResult validate(Object parameterValue)
    {
        return isValid(parameterValue) ? ValidationResult.successValidationResult() : ValidationResult.errorValidationResult(buildErrorMessage(parameterValue));
    }

    protected abstract boolean isValidJavaType(Object parameterValue);

    protected abstract boolean canParse(String parameterValue);

    protected void appendErrorMessageDetail(StringBuilder builder)
    {
        // nothing by default
    }

    private boolean isValid(Object parameterValue)
    {
        if (parameterValue == null)
        {
            return true;
        }
        if (parameterValue instanceof Iterable)
        {
            return Iterate.allSatisfy((Iterable<?>) parameterValue, this::isSingleValueValid);
        }
        return isSingleValueValid(parameterValue);
    }

    private boolean isSingleValueValid(Object value)
    {
        return isValidJavaType(value) || ((value instanceof String) && canParse((String) value));
    }

    private String buildErrorMessage(Object parameterValue)
    {
        StringBuilder builder = new StringBuilder("Unable to process '").append(this.type).append("' parameter, value: ");
        if (parameterValue instanceof String)
        {
            builder.append("'").append(parameterValue).append("'");
            if (!"String".equals(this.type))
            {
                builder.append(" is not parsable.");
            }
        }
        else
        {
            builder.append(parameterValue).append('.');
        }
        appendErrorMessageDetail(builder);
        return builder.toString();
    }

    static FunctionParameterTypeValidator externalParameterTypeValidator(String type)
    {
        return VALIDATORS.get(type);
    }

    static RichIterable<String> getExternalParameterTypes()
    {
        return VALIDATORS.keysView();
    }

    private static boolean canParseDate(PlanDateParameterDateFormat.PlanDateParameterFormatter formatter, String parameterValue)
    {
        try
        {
            formatter.parse(parameterValue);
            return true;
        }
        catch (DateTimeParseException ignore)
        {
            return false;
        }
    }
}
