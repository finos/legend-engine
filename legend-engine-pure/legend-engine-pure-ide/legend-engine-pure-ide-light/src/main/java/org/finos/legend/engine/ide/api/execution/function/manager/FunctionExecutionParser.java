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

package org.finos.legend.engine.ide.api.execution.function.manager;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation.enumeration.Enumeration;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonParser;

class FunctionExecutionParser
{
    private static final char QUOTE = '\'';
    private static final char DATE_START = '%';
    private static final char COLLECTION_START = '[';
    private static final char COLLECTION_END = ']';
    private static final char COLLECTION_SEPARATOR = ',';

    private final ProcessorSupport processorSupport;

    FunctionExecutionParser(ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
    }

    CoreInstance parseParametersAsList(String[] parameterStrings)
    {
        MutableList<CoreInstance> parameters = FastList.newList(parameterStrings.length);
        for (int i = 0; i < parameterStrings.length; i++)
        {
            parameters.add(parseParameter(parameterStrings[i], true));
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(parameters, true, this.processorSupport);
    }

    CoreInstance parseJsonParameter(String parameter)
    {
        CoreInstance jsonElementParameter = new JsonParser(this.processorSupport).toPureJson(parameter);
        return ValueSpecificationBootstrap.wrapValueSpecification(wrapAsList(Lists.fixedSize.of(jsonElementParameter)), true, this.processorSupport);
    }

    MutableList<CoreInstance> parseParameters(Iterable<String> parameterStrings, boolean wrapAsList)
    {
        MutableList<CoreInstance> parameters = Lists.mutable.with();
        for (String parameterString : parameterStrings)
        {
            parameters.add(parseParameter(parameterString, wrapAsList));
        }
        return parameters;
    }

    CoreInstance parseParameter(String parameterString, boolean wrapAsList)
    {
        int length = parameterString.length();
        return parseParameter(parameterString, findNextNonWhitespace(parameterString, 0, length), findLastNonWhitespace(parameterString, 0, length) + 1, wrapAsList);
    }

    private CoreInstance parseParameter(String paramString, int start, int end, boolean wrapAsList)
    {
        try
        {
            if (paramString.charAt(start) == COLLECTION_START)
            {
                ListIterable<CoreInstance> coreInstances = parseCollectionParameter(paramString, start, end);

                if (wrapAsList)
                {
                    return wrapAsList(coreInstances);
                }
                else
                {
                    return ValueSpecificationBootstrap.wrapValueSpecification(coreInstances, true, this.processorSupport);
                }
            }
            else
            {
                CoreInstance value = parseValue(paramString, start, end);
                if (wrapAsList)
                {
                    return wrapAsList(Lists.fixedSize.of(value));
                }
                else
                {
                    return ValueSpecificationBootstrap.wrapValueSpecification(value, true, this.processorSupport);
                }
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Invalid parameter specification: " + paramString, e);
        }
    }

    private CoreInstance wrapAsList(ListIterable<CoreInstance> values)
    {
        CoreInstance inst = this.processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.List);
        Instance.addValueToProperty(inst, M3Properties.values, values, this.processorSupport);

        CoreInstance classifierGenericType = this.processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.GenericType);
        Instance.setValueForProperty(classifierGenericType, M3Properties.rawType, this.processorSupport.package_getByUserPath(M3Paths.List), this.processorSupport);
        Instance.setValueForProperty(classifierGenericType, M3Properties.typeArguments, Type.wrapGenericType(this.processorSupport.package_getByUserPath(M3Paths.Any), this.processorSupport), this.processorSupport);

        Instance.setValueForProperty(inst, M3Properties.classifierGenericType, classifierGenericType, this.processorSupport);
        return inst;
    }

    private ListIterable<CoreInstance> parseCollectionParameter(String string, int start, int end)
    {
        int index = findNextNonWhitespace(string, start + 1, end);

        // Check for empty collection
        if (string.charAt(index) == COLLECTION_END)
        {
            // Check if there is non-whitespace after the collection end character
            if (index + 1 < end)
            {
                throw new IllegalArgumentException("Invalid collection specification: " + string);
            }
            return Lists.immutable.with();
        }

        // Parse values in the collection
        MutableList<CoreInstance> values = Lists.mutable.with();
        while ((index != -1) && (index < end))
        {
            int valueEnd = findValueEnd(string, index, end);
            if (valueEnd == -1)
            {
                throw new IllegalArgumentException("Error parsing value at index " + index + " of collection specification: " + string);
            }
            values.add(parseValue(string, index, valueEnd));

            index = findNextNonWhitespace(string, valueEnd, end);
            switch (string.charAt(index))
            {
                case COLLECTION_SEPARATOR:
                {
                    index = findNextNonWhitespace(string, index + 1, end);
                    break;
                }
                case COLLECTION_END:
                {
                    index = findNextNonWhitespace(string, index + 1, end);
                    if (index != -1)
                    {
                        throw new IllegalArgumentException("Invalid collection specification: " + string.substring(start, end));
                    }
                    return values;
                }
                default:
                {
                    throw new IllegalArgumentException("Invalid collection specification: " + string.substring(start, end));
                }
            }
        }
        throw new IllegalArgumentException("Invalid collection specification: " + string.substring(start, end));
    }

    private CoreInstance parseValue(String string, int start, int end)
    {
        try
        {
            if (isPossiblyDate(string, start, end))
            {
                return parsePrimitiveParameter(string.substring(start + 1, end), ModelRepository.DATE_TYPE_NAME);
            }
            else if (isPossiblyString(string, start, end))
            {
                // TODO evaluate whether this is the correct type of unescape
                return parsePrimitiveParameter(StringEscapeUtils.unescapeJava(string.substring(start + 1, end - 1)), ModelRepository.STRING_TYPE_NAME);
            }
            else if (isPossiblyInteger(string, start, end))
            {
                return parsePrimitiveParameter(string.substring(start, end), ModelRepository.INTEGER_TYPE_NAME);
            }
            else if (isPossiblyFloat(string, start, end))
            {
                return parsePrimitiveParameter(string.substring(start, end), ModelRepository.FLOAT_TYPE_NAME);
            }
            else if (isPossiblyBoolean(string, start, end))
            {
                return parsePrimitiveParameter(string.substring(start, end), ModelRepository.BOOLEAN_TYPE_NAME);
            }
            else if (isPossiblyPropertyOrEnumeration(string, start, end))
            {
                return parsePropertyOrEnumerationParameter(string, start, end);
            }
            else if (isPossiblyPackageableElement(string, start, end))
            {
                return parsePackageableElementParameter(string, start, end);
            }
            else
            {
                throw new RuntimeException("Cannot determine type: " + string.substring(start, end));
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Invalid value specification: " + string.substring(start, end), e);
        }
    }

    private CoreInstance parsePrimitiveParameter(String string, String primitiveTypeName)
    {
        return this.processorSupport.newCoreInstance(string, primitiveTypeName, null);
    }

    public CoreInstance wrapString(String string)
    {
        CoreInstance value = this.processorSupport.newCoreInstance(string, ModelRepository.STRING_TYPE_NAME, null);
        return ValueSpecificationBootstrap.wrapValueSpecification(value, true, this.processorSupport);
    }

    private CoreInstance parsePropertyOrEnumerationParameter(String string, int start, int end)
    {
        int splitIndex = indexOf(string, '.', start, end);
        if (splitIndex == -1)
        {
            throw new IllegalArgumentException("Invalid property or enumeration specification: " + string.substring(start, end));
        }

        String ownerString = string.substring(start, splitIndex);
        CoreInstance owner = this.processorSupport.package_getByUserPath(ownerString);
        if (owner == null)
        {
            throw new IllegalArgumentException("Unknown element: " + ownerString);
        }

        String valueString = string.substring(splitIndex + 1, end);
        if (this.processorSupport.instance_instanceOf(owner, M3Paths.Enumeration))
        {
            CoreInstance enumValue = Enumeration.findEnum(owner, valueString);
            if (enumValue == null)
            {
                throw new IllegalArgumentException("Unknown value for " + ownerString + ": " + valueString);
            }
            return enumValue;
        }
        else
        {
            CoreInstance property = this.processorSupport.class_findPropertyUsingGeneralization(owner, valueString);
            if (property == null)
            {
                throw new IllegalArgumentException("Unknown property for " + ownerString + ": " + valueString);
            }
            return property;
        }
    }

    private CoreInstance parsePackageableElementParameter(String string, int start, int end)
    {
        CoreInstance instance = this.processorSupport.package_getByUserPath(string.substring(start, end));
        if (instance == null)
        {
            throw new IllegalArgumentException("Unknown element: " + string.substring(start, end));
        }
        return instance;
    }

    private boolean isPossiblyBoolean(String string, int start, int end)
    {
        String lower = string.substring(start, end).toLowerCase();
        return ModelRepository.BOOLEAN_TRUE.equals(lower) || ModelRepository.BOOLEAN_FALSE.equals(lower);
    }

    private boolean isPossiblyDate(String string, int start, int end)
    {
        return string.charAt(start) == DATE_START;
    }

    private boolean isPossiblyString(String string, int start, int end)
    {
        return (string.charAt(start) == QUOTE) && (string.charAt(end - 1) == QUOTE);
    }

    private boolean isPossiblyInteger(String string, int start, int end)
    {
        char first = string.charAt(start);
        if ((first == '+') || (first == '-') || Character.isDigit(first))
        {
            for (int i = start + 1; i < end; i++)
            {
                if (!Character.isDigit(string.charAt(i)))
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    private boolean isPossiblyFloat(String string, int start, int end)
    {
        char first = string.charAt(start);
        if (first == '.')
        {
            return ((start + 1) < end) && Character.isDigit(string.charAt(start + 1));
        }
        else if ((first == '+') || (first == '-') || Character.isDigit(first))
        {
            return contains(string, '.', start + 1, end);
        }
        else
        {
            return false;
        }
    }

    private boolean isPossiblyPropertyOrEnumeration(String string, int start, int end)
    {
        return Character.isLetter(string.charAt(start)) && contains(string, '.', start + 1, end);
    }

    private boolean isPossiblyPackageableElement(String string, int start, int end)
    {
        char first = string.charAt(start);
        if (first == ':')
        {
            return ((end - start) == 2) && (string.charAt(start + 1) == ':');
        }
        else
        {
            return Character.isLetter(first) && !contains(string, '.', start + 1, end);
        }
    }

    // General parsing helpers

    private static int findNextNonWhitespace(String string, int start, int end)
    {
        for (int index = start; index < end; index++)
        {
            if (!Character.isWhitespace(string.charAt(index)))
            {
                return index;
            }
        }
        return -1;
    }

    private static int findLastNonWhitespace(String string, int start, int end)
    {
        for (int index = end - 1; index >= start; index--)
        {
            if (!Character.isWhitespace(string.charAt(index)))
            {
                return index;
            }
        }
        return -1;
    }

    private static int findValueEnd(String string, int start, int end)
    {
        if (string.charAt(start) == QUOTE)
        {
            int index = start + 1;
            while (index < end)
            {
                switch (string.charAt(index))
                {
                    case QUOTE:
                    {
                        return index + 1;
                    }
                    case '\\':
                    {
                        index += 2;
                        break;
                    }
                    default:
                    {
                        index += 1;
                    }
                }
            }
            return -1;
        }
        else
        {
            for (int index = start; index < end; index++)
            {
                char character = string.charAt(index);
                if ((character == COLLECTION_SEPARATOR) || (character == COLLECTION_END) || Character.isWhitespace(character))
                {
                    return index;
                }
            }
            return -1;
        }
    }

    private static boolean contains(String string, char character, int start, int end)
    {
        return indexOf(string, character, start, end) != -1;
    }

    private static int indexOf(String string, char character, int start, int end)
    {
        for (int index = start; index < end; index++)
        {
            if (character == string.charAt(index))
            {
                return index;
            }
        }
        return -1;
    }
}
