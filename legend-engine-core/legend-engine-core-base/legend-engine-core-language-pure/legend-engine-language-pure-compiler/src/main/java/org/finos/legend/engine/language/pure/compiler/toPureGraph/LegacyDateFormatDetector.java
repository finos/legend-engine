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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

/**
 * Warns that a date format uses a run of {@code S}, the deprecated form of the sub-second field.
 *
 * <p>The Java platform binding reads such a run as a fixed width, writing zeros for a date with no fraction,
 * where Pure reads it as at most that many digits, and the binding keeps that reading for any format written
 * entirely in the syntax it has always accepted. A format using any syntax Pure has added since is read the same
 * way everywhere, so a run of {@code S} in one draws no warning.
 *
 * <p>Only a literal can be checked: the first argument of {@code format}, and the {@code dateTimeFormat} of a
 * serialization or externalize configuration. A format assembled at run time is not seen here.
 */
final class LegacyDateFormatDetector
{
    private static final String FORMAT_FUNCTION = "meta::pure::functions::string::format_String_1__Any_MANY__String_1_";
    private static final String NEW_FUNCTION = "meta::pure::functions::lang::new_Class_1__String_1__KeyExpression_MANY__T_1_";
    private static final ImmutableSet<String> NAMES = Sets.immutable.with("format_String_1__Any_MANY__String_1_", "new_Class_1__String_1__KeyExpression_MANY__T_1_");
    private static final String DATE_TIME_FORMAT = "dateTimeFormat";
    private static final ImmutableSet<String> CONFIGURATIONS = Sets.immutable.with(
            "meta::pure::graphFetch::execution::AlloySerializationConfig",
            "meta::external::format::json::metamodel::externalize::JsonSchemaExternalizeConfig");

    private LegacyDateFormatDetector()
    {
    }

    static void check(SimpleFunctionExpression expression, PureModel pureModel)
    {
        if (!(expression._func() instanceof PackageableElement) || !NAMES.contains(((PackageableElement) expression._func())._name()))
        {
            return;
        }
        switch (HelperModelBuilder.getElementFullPath((PackageableElement) expression._func(), pureModel.getExecutionSupport()))
        {
            case FORMAT_FUNCTION:
            {
                RichIterable<? extends ValueSpecification> parametersValues = expression._parametersValues();
                if (parametersValues.notEmpty())
                {
                    ValueSpecification firstParam = parametersValues.getFirst();
                    String literal = stringLiteral(firstParam);
                    if (literal != null)
                    {
                        checkFormatString(literal, sourceInformation(firstParam, expression), pureModel);
                    }
                }
                break;
            }
            case NEW_FUNCTION:
            {
                if (isConfiguration(expression._genericType()._rawType(), pureModel))
                {
                    expression._parametersValues().asLazy().drop(2).forEach(v -> checkKeyExpressions(v, expression, pureModel));
                }
            }
        }
    }

    static void checkDateTimeFormat(String dateTimeFormat, SourceInformation sourceInformation, PureModel pureModel)
    {
        if (dateTimeFormat != null)
        {
            checkDatePattern(dateTimeFormat, sourceInformation, pureModel);
        }
    }

    private static void checkKeyExpressions(Object value, SimpleFunctionExpression expression, PureModel pureModel)
    {
        if (value instanceof KeyExpression)
        {
            KeyExpression keyExpression = (KeyExpression) value;
            if (DATE_TIME_FORMAT.equals(stringLiteral(keyExpression._key())))
            {
                String literal = stringLiteral(keyExpression._expression());
                if (literal != null)
                {
                    checkDatePattern(literal, sourceInformation(keyExpression._expression(), expression), pureModel);
                }
            }
        }
        else if (value instanceof InstanceValue)
        {
            ((InstanceValue) value)._values().forEach(v -> checkKeyExpressions(v, expression, pureModel));
        }
    }

    private static void checkFormatString(String formatString, SourceInformation sourceInformation, PureModel pureModel)
    {
        int length = formatString.length();
        int index = formatString.indexOf('%');
        while ((index != -1) && (index + 1 < length))
        {
            int next = index + 2;
            if ((formatString.charAt(index + 1) == 't') && (next < length) && (formatString.charAt(next) == '{'))
            {
                int end = findEndOfDatePattern(formatString, next + 1);
                if (end == -1)
                {
                    return;
                }
                checkDatePattern(formatString.substring(next + 1, end), sourceInformation, pureModel);
                next = end + 1;
            }
            index = formatString.indexOf('%', next);
        }
    }

    private static void checkDatePattern(String pattern, SourceInformation sourceInformation, PureModel pureModel)
    {
        IntSet widths = findLegacySubsecondWidths(pattern);
        if (widths.notEmpty())
        {
            pureModel.addDefects(widths.toSortedList().collect(width -> new Warning(sourceInformation, message(pattern, width))));
        }
    }

    /**
     * The widths of the runs of {@code S} in a date pattern written entirely in the syntax the Java platform
     * binding has always accepted.
     *
     * @param pattern date pattern
     * @return the widths, or none if the pattern uses any syntax outside that
     */
    private static IntSet findLegacySubsecondWidths(String pattern)
    {
        MutableIntSet widths = IntSets.mutable.empty();
        int length = pattern.length();
        int index = 0;
        while (index < length)
        {
            char character = pattern.charAt(index);
            switch (character)
            {
                case '[':
                {
                    index = findEndOfTimeZone(pattern, index + 1);
                    if (index == -1)
                    {
                        return IntSets.immutable.empty();
                    }
                    break;
                }
                case '"':
                {
                    index = findEndOfQuotedText(pattern, index + 1);
                    if (index == -1)
                    {
                        return IntSets.immutable.empty();
                    }
                    break;
                }
                case 'S':
                {
                    int end = index + 1;
                    while ((end < length) && (pattern.charAt(end) == 'S'))
                    {
                        end++;
                    }
                    widths.add(end - index);
                    index = end;
                    break;
                }
                case 'y':
                case 'M':
                case 'd':
                case 'h':
                case 'H':
                case 'a':
                case 'm':
                case 's':
                case 'z':
                case 'Z':
                case 'X':
                case '-':
                case '/':
                case ':':
                case '.':
                case ' ':
                case '\t':
                {
                    index++;
                    break;
                }
                default:
                {
                    return IntSets.immutable.empty();
                }
            }
        }
        return widths;
    }

    private static String message(String pattern, int width)
    {
        String digits = width + ((width == 1) ? " digit" : " digits");
        return "Date format '" + pattern + "' uses " + repeat('S', width) + ", a deprecated form of the sub-second field. Replace it with S" + width
                + " for exactly " + digits + ", ?[S" + width + "|\"" + repeat('0', width) + "\"] for exactly " + digits + " and "
                + ((width == 1) ? "a zero" : "zeros") + " when the date has no sub-second, or "
                + ((width <= 3) ? ("S<" + width + " for at most " + digits) : "S* for however many digits the date has") + ".";
    }

    private static boolean isConfiguration(Type type, PureModel pureModel)
    {
        return (type instanceof PackageableElement) && CONFIGURATIONS.contains(HelperModelBuilder.getElementFullPath((PackageableElement) type, pureModel.getExecutionSupport()));
    }

    private static String stringLiteral(ValueSpecification valueSpecification)
    {
        if (valueSpecification instanceof InstanceValue)
        {
            Object value = ((InstanceValue) valueSpecification)._values().getAny();
            if ((value instanceof String) && (((InstanceValue) valueSpecification)._values().size() == 1))
            {
                return (String) value;
            }
        }
        return null;
    }

    private static SourceInformation sourceInformation(ValueSpecification valueSpecification, SimpleFunctionExpression expression)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = valueSpecification.getSourceInformation();
        return SourceInformationHelper.fromM3SourceInformation((sourceInformation == null) ? expression.getSourceInformation() : sourceInformation);
    }

    private static int findEndOfDatePattern(String formatString, int start)
    {
        boolean escaped = false;
        boolean inQuotes = false;
        for (int i = start; i < formatString.length(); i++)
        {
            char next = formatString.charAt(i);
            if (escaped)
            {
                escaped = false;
            }
            else if (inQuotes)
            {
                if (next == '"')
                {
                    inQuotes = false;
                }
                else if (next == '\\')
                {
                    escaped = true;
                }
            }
            else if (next == '"')
            {
                inQuotes = true;
            }
            else if (next == '}')
            {
                return i;
            }
        }
        return -1;
    }

    private static int findEndOfTimeZone(String pattern, int start)
    {
        boolean escaped = false;
        boolean inQuotes = false;
        for (int i = start; i < pattern.length(); i++)
        {
            char next = pattern.charAt(i);
            if (escaped)
            {
                escaped = false;
            }
            else if (next == '"')
            {
                inQuotes = !inQuotes;
            }
            else if ((next == ']') && !inQuotes)
            {
                return i + 1;
            }
            else if (next == '\\')
            {
                escaped = true;
            }
        }
        return -1;
    }

    private static int findEndOfQuotedText(String pattern, int start)
    {
        boolean escaped = false;
        for (int i = start; i < pattern.length(); i++)
        {
            char next = pattern.charAt(i);
            if (escaped)
            {
                escaped = false;
            }
            else if (next == '"')
            {
                return i + 1;
            }
            else if (next == '\\')
            {
                escaped = true;
            }
        }
        return -1;
    }

    private static String repeat(char character, int count)
    {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++)
        {
            builder.append(character);
        }
        return builder.toString();
    }
}
