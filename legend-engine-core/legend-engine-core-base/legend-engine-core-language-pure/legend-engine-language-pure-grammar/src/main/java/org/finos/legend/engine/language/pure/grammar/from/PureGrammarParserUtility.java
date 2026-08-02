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
import org.antlr.v4.runtime.Token;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.extension.TagPtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PureGrammarParserUtility
{
    private static final String PACKAGE_SEPARATOR = "::";
    private static final String DOC_PROFILE_PATH = "meta::pure::profiles::doc";
    private static final String DOC_TAG = "doc";
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
        // Multi-line strings (Java text-block style) are delimited by triple single quotes.
        if (isTextBlock(val))
        {
            return processTextBlock(val, unescape);
        }
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

    /**
     * Build a string literal from a raw grammar STRING token, recording whether it was authored as a multi-line
     * (triple-quoted) block. The flag is the whole decision - nothing inspects the value.
     */
    public static CString toCString(String rawToken)
    {
        CString cString = new CString(fromGrammarString(rawToken, true));
        cString.multiLine = isTextBlock(rawToken);
        return cString;
    }

    /**
     * A text block opens with `'''` followed - after optional spaces or tabs - by a line terminator, so the first
     * line of content is the line after the delimiter. This mirrors legend-pure's lexer fragment; `'''abc'''` and
     * `''''''` are deliberately not text blocks.
     */
    public static boolean isTextBlock(String val)
    {
        if (val.length() < 7 || !val.startsWith("'''") || !val.endsWith("'''"))
        {
            return false;
        }
        int i = 3;
        while (i < val.length() && (val.charAt(i) == ' ' || val.charAt(i) == '\t'))
        {
            i++;
        }
        if (i < val.length() && val.charAt(i) == '\r')
        {
            i++;
        }
        return i < val.length() && val.charAt(i) == '\n';
    }

    /**
     * Convert the raw text of a multi-line string literal (`'''...'''`) into its value, following the Java text-block
     * algorithm: normalize line terminators, drop the opening-delimiter line and the closing `'''`, strip the common
     * (incidental) leading-whitespace indentation, strip trailing whitespace from each line, then process escape
     * sequences. The `\s` escape and `\`-line-continuation are not supported.
     * <p>
     * Kept behaviourally identical to legend-pure's {@code processMultilineString} so the two grammars accept the
     * same source and produce the same value - see docs/engineering/architecture/type-system.md.
     */
    public static String processTextBlock(String val, boolean unescape)
    {
        // normalize line terminators, then drop the opening delimiter line (through its terminator) and the closing '''
        String normalized = val.replace("\r\n", "\n").replace('\r', '\n');
        String body = normalized.substring(normalized.indexOf('\n') + 1, normalized.length() - 3);
        // -1 keeps a trailing empty line (from a closing delimiter on its own line)
        String[] lines = body.split("\n", -1);
        // the minimum indentation spans every non-blank line plus the last (closing-delimiter) line even when blank -
        // that last line sets a floor which prevents over-stripping the value's own indentation
        int minIndent = Integer.MAX_VALUE;
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i];
            int leading = leadingWhitespaceCount(line);
            if (leading < line.length() || i == lines.length - 1)
            {
                minIndent = Math.min(minIndent, leading);
            }
        }
        if (minIndent == Integer.MAX_VALUE)
        {
            minIndent = 0;
        }
        StringBuilder builder = new StringBuilder(body.length());
        for (int i = 0; i < lines.length; i++)
        {
            if (i > 0)
            {
                builder.append('\n');
            }
            String line = lines[i];
            builder.append(stripTrailingWhitespace(line.substring(Math.min(minIndent, line.length()))));
        }
        String result = builder.toString();
        if (unescape)
        {
            result = StringEscapeUtils.unescapeJava(result);
        }
        return result;
    }

    /**
     * Convert the raw text of a documentation literal (`'''...'''`) into the string stored as the
     * `meta::pure::profiles::doc` `doc` tagged value: the same layout as a multi-line string literal, minus the
     * escape processing, plus the removal of leading and trailing blank lines.
     * <p>
     * Content is literal on purpose - documentation is prose, and unescaping prose silently rewrites a regex
     * (`\d+` -> `d+`), a Markdown escape (`\*` -> `*`) and a Windows path (`C:\temp` -> `C:` followed by a tab).
     * Dropping the surrounding blank lines is what makes a documentation literal and the equivalent explicit
     * `doc.doc` tagged value hold the same string.
     * <p>
     * Kept behaviourally identical to legend-pure's {@code DocumentationCanonicalizer}.
     */
    public static String canonicalizeDocumentation(String rawToken)
    {
        String[] lines = processTextBlock(rawToken, false).split("\n", -1);
        // the layout has already stripped trailing whitespace, so a blank line is exactly empty
        int start = 0;
        int end = lines.length;
        while (start < end && lines[start].isEmpty())
        {
            start++;
        }
        while (end > start && lines[end - 1].isEmpty())
        {
            end--;
        }
        return String.join("\n", Arrays.asList(lines).subList(start, end));
    }

    /**
     * Build an element's tagged values, folding in its documentation as a `meta::pure::profiles::doc` `doc` value
     * when it declares any. Documentation is sugar for that tagged value - there is no new profile and no new
     * protocol - so nothing downstream of the parser needs to know it was written as a `'''...'''` block.
     * <p>
     * Shared by every grammar that accepts documentation. ANTLR generates a distinct `DocumentationContext` class
     * per grammar even for a rule inherited from `M3ParserGrammar`, so this takes the literal's token rather than
     * the context: a caller passes `ctx.documentation() == null ? null : ctx.documentation().STRING().getSymbol()`.
     *
     * @param documentation the documentation literal's token, or null when the element declares none
     * @param taggedValues  the explicit tagged values, already built by the caller's own walker
     */
    public static List<TaggedValue> taggedValuesWithDocumentation(Token documentation, List<TaggedValue> taggedValues, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        if (documentation == null)
        {
            return taggedValues;
        }
        // `documentation` matches the single STRING token, which covers both literal shapes, so the block shape is
        // checked here rather than in the grammar
        String rawToken = documentation.getText();
        if (!isTextBlock(rawToken))
        {
            throw new EngineException("Documentation must be written as a multi-line ('''...''') literal", walkerSourceInformation.getSourceInformation(documentation), EngineErrorType.PARSER);
        }
        TaggedValue conflict = ListIterate.detect(taggedValues, PureGrammarParserUtility::isDocTaggedValue);
        if (conflict != null)
        {
            throw new EngineException("Element has both documentation and an explicit doc.doc tagged value. Use one.", conflict.sourceInformation, EngineErrorType.PARSER);
        }
        return Lists.mutable.<TaggedValue>with(buildDocumentationTaggedValue(documentation, rawToken, walkerSourceInformation)).withAll(taggedValues);
    }

    private static TaggedValue buildDocumentationTaggedValue(Token token, String rawToken, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        SourceInformation sourceInformation = documentationSourceInformation(token, rawToken, walkerSourceInformation);
        TaggedValue taggedValue = new TaggedValue();
        taggedValue.tag = new TagPtr();
        taggedValue.tag.profile = DOC_PROFILE_PATH;
        taggedValue.tag.value = DOC_TAG;
        taggedValue.tag.profileSourceInformation = sourceInformation;
        taggedValue.tag.sourceInformation = sourceInformation;
        taggedValue.value = new CString(canonicalizeDocumentation(rawToken));
        taggedValue.value.multiLine = true;
        taggedValue.sourceInformation = sourceInformation;
        return taggedValue;
    }

    /**
     * A token reports only the line it *starts* on, and
     * {@link ParseTreeWalkerSourceInformation#getSourceInformation(Token)} derives the end column from the start
     * column plus the whole token length - which names a column past the end of the first line for a token whose
     * text spans lines. A documentation literal always spans lines (the opening delimiter must be followed by a
     * line terminator) and always ends on `'''`, so the end is computed here from the text instead.
     * <p>
     * This is deliberately not fixed in the shared helper: the island and section content tokens run through it
     * too, they end *with* a newline rather than after one, and several suites pin their current coordinates.
     */
    private static SourceInformation documentationSourceInformation(Token token, String rawToken, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        if (!walkerSourceInformation.getReturnSourceInfo())
        {
            return null;
        }
        // the column offset applies only to the first line, which the end never sits on here
        int startLine = token.getLine() + walkerSourceInformation.getLineOffset();
        int startColumn = token.getCharPositionInLine() + 1 + (token.getLine() == 1 ? walkerSourceInformation.getColumnOffset() : 0);
        int newLines = 0;
        for (int i = rawToken.indexOf('\n'); i >= 0; i = rawToken.indexOf('\n', i + 1))
        {
            newLines++;
        }
        int endColumn = rawToken.length() - rawToken.lastIndexOf('\n') - 1;
        return new SourceInformation(walkerSourceInformation.getSourceId(), startLine, startColumn, startLine + newLines, endColumn);
    }

    /**
     * Tag references are not resolved at parse time, so this matches the profile as written: bare `doc` resolved
     * through an import, or the fully qualified `meta::pure::profiles::doc`. A user profile that merely ends in
     * `doc`, such as `my::pkg::doc`, is a different profile and is not a conflict.
     */
    private static boolean isDocTaggedValue(TaggedValue taggedValue)
    {
        return taggedValue.tag != null && DOC_TAG.equals(taggedValue.tag.value)
                && (DOC_TAG.equals(taggedValue.tag.profile) || DOC_PROFILE_PATH.equals(taggedValue.tag.profile));
    }

    private static String stripTrailingWhitespace(String line)
    {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1)))
        {
            end--;
        }
        return end == line.length() ? line : line.substring(0, end);
    }

    private static int leadingWhitespaceCount(String line)
    {
        int count = 0;
        while (count < line.length() && Character.isWhitespace(line.charAt(count)))
        {
            count++;
        }
        return count;
    }

    public static <T extends RuleContext> List<T> validateRequiredListField(List<T> contexts, String fieldName, SourceInformation sourceInformation)
    {
        if (contexts == null || contexts.isEmpty())
        {
            throw new EngineException("Field '" + fieldName + "' is required", sourceInformation, EngineErrorType.PARSER);
        }
        return contexts;
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
        for (String identifier : path.split(PACKAGE_SEPARATOR))
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

    public static Multiplicity buildMultiplicity(ParserRuleContext fromMultiplicity, ParserRuleContext toMultiplicity)
    {
        String star = "*";
        Multiplicity m = new Multiplicity();
        m.lowerBound = Integer.parseInt(fromMultiplicity != null ? fromMultiplicity.getText() : star.equals(toMultiplicity.getText()) ? "0" : toMultiplicity.getText());
        m.setUpperBound(star.equals(toMultiplicity.getText()) ? null : Integer.parseInt(toMultiplicity.getText()));
        return m;
    }
    
}
