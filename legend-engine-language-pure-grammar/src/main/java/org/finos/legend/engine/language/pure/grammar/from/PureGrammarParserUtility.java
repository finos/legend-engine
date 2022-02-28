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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.apache.commons.text.StringEscapeUtils;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PureGrammarParserUtility
{
    private static final String PACKAGE_SEPARATOR = "::";
    private static final Pattern VALID_STRING_PATTERN = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_$]*");

    /**
     * Convert string in grammar to string to be used in the graph
     *
     * @param val      the string to be converted
     * @param unescape whether to unescape the string or not, usually we should unescape, but if the string is a JSON
     *                 for example, we must not unescape because JSON string itself may account for some escaping, such
     *                 as '{"record":"{\"oneName\":\"oneName 99\"}"}', if we unescape this it would become
     *                 '{"record":"{"oneName":"oneName 99"}"}', hence is a malformed JSON string, as such, we only should
     *                 unescape if the string is simply a string, not represent any structure that might contain
     *                 other form of escape themselves. This will count for not only JSON, but also Java code and so on.
     *                 On the other hand, we should escape for certain places such as a string because then '\n' in grammar
     *                 will be treated as a new line character or 'it\'s' will be treated as `it's`.
     */
    public static String fromGrammarString(String val, boolean unescape)
    {
        if (unescape)
        {
            // since PURE is syntactically close to Java, we use `unescapeJava`
            val = StringEscapeUtils.unescapeJava(val);
        }
        return PureGrammarParserUtility.removeQuotes(val);
    }

    public static String removeQuotes(String val)
    {
        return val.substring(1, val.length() - 1);
    }

    public static <T extends RuleContext> T validateAndExtractRequiredField(List<T> contexts, String fieldName, SourceInformation sourceInformation)
    {
        if (contexts == null || contexts.isEmpty())
        {
            throw new EngineException("Field '" + fieldName + "' is required", sourceInformation, EngineErrorType.PARSER);
        }
        else if (contexts.size() != 1)
        {
            throw new EngineException("Field '" + fieldName + "' should be specified only once", sourceInformation, EngineErrorType.PARSER);
        }
        return contexts.get(0);
    }

    public static <T extends RuleContext> T validateAndExtractOptionalField(List<T> contexts, String fieldName, SourceInformation sourceInformation)
    {
        if (contexts == null || contexts.isEmpty())
        {
            return null;
        }
        else if (contexts.size() != 1)
        {
            throw new EngineException("Field '" + fieldName + "' should be specified only once", sourceInformation, EngineErrorType.PARSER);
        }
        return contexts.get(0);
    }

    public static String fromQualifiedName(List<? extends ParserRuleContext> packagePath, ParserRuleContext identifier)
    {
        String path = packagePath.stream().map(PureGrammarParserUtility::fromIdentifier).collect(Collectors.joining(PACKAGE_SEPARATOR));
        return path + (path.isEmpty() ? "" : PACKAGE_SEPARATOR) + PureGrammarParserUtility.fromIdentifier(identifier);
    }

    public static String fromPath(List<? extends ParserRuleContext> identifiers)
    {
        return identifiers.stream().map(PureGrammarParserUtility::fromIdentifier).collect(Collectors.joining(PACKAGE_SEPARATOR));
    }

    public static String fromIdentifier(ParserRuleContext identifier)
    {
        String text = identifier.getText();
        return text.startsWith("'") ? PureGrammarParserUtility.fromGrammarString(text, true) : text;
    }

    public static String validatePath(String path, SourceInformation sourceInformation)
    {
        List<String> parts = new ArrayList<>();
        for (String identifier: path.split(PACKAGE_SEPARATOR))
        {
            if (identifier.startsWith("'"))
            {
                if (!identifier.endsWith("'"))
                {
                    throw new EngineException("Invalid identifier: " + identifier, sourceInformation, EngineErrorType.PARSER);
                }
                parts.add(fromGrammarString(identifier, true));
            }
            else
            {
                if (!VALID_STRING_PATTERN.matcher(identifier).matches())
                {
                    throw new EngineException("Invalid identifier: " + identifier, sourceInformation, EngineErrorType.PARSER);
                }
                parts.add(identifier);
            }
        }
        return String.join(PACKAGE_SEPARATOR, parts);
    }
}
