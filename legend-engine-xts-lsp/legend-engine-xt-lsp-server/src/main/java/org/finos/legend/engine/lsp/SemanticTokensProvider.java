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

package org.finos.legend.engine.lsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Provides semantic token classification for Pure source files.
 * Walks the compiled model to classify tokens as class names, properties,
 * function names, enum values, etc. — information that TextMate grammars
 * cannot derive from regex patterns alone.
 *
 * Token types (indices into LEGEND):
 *   0: namespace    (package paths)
 *   1: class        (Class definitions)
 *   2: enum         (Enumeration definitions)
 *   3: function     (function definitions)
 *   4: property     (class properties)
 *   5: enumMember   (enum values)
 *   6: type         (type references)
 *   7: parameter    (function parameters)
 *   8: interface    (Profile definitions)
 *   9: struct       (Association definitions)
 */
public class SemanticTokensProvider
{
    public static final List<String> TOKEN_TYPES = Collections.unmodifiableList(Arrays.asList(
            "namespace", "class", "enum", "function", "property",
            "enumMember", "type", "parameter", "interface", "struct"
    ));

    public static final List<String> TOKEN_MODIFIERS = Collections.unmodifiableList(Arrays.asList(
            "definition", "declaration"
    ));

    private static final int TYPE_NAMESPACE = 0;
    private static final int TYPE_CLASS = 1;
    private static final int TYPE_ENUM = 2;
    private static final int TYPE_FUNCTION = 3;
    private static final int TYPE_PROPERTY = 4;
    private static final int TYPE_ENUM_MEMBER = 5;
    private static final int TYPE_TYPE = 6;
    private static final int TYPE_PARAMETER = 7;
    private static final int TYPE_INTERFACE = 8;
    private static final int TYPE_STRUCT = 9;

    private static final int MOD_DEFINITION = 1;

    /**
     * Get semantic tokens for a source file.
     * Returns the encoded token data as required by LSP (array of 5-tuples:
     * deltaLine, deltaStartChar, length, tokenType, tokenModifiers).
     */
    public static List<Integer> getTokens(PureRuntime runtime, String sourceId)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return Collections.emptyList();
        }

        List<RawToken> tokens = new ArrayList<>();
        collectTokensFromSource(source, runtime, tokens);

        // Sort by position (line, then column)
        tokens.sort(Comparator.comparingInt((RawToken t) -> t.line)
                .thenComparingInt(t -> t.column));

        // Encode as LSP delta format
        return encodeDelta(tokens);
    }

    private static void collectTokensFromSource(Source source, PureRuntime runtime, List<RawToken> tokens)
    {
        ListIterable<? extends CoreInstance> newInstances = source.getNewInstances();
        if (newInstances == null)
        {
            return;
        }

        for (CoreInstance instance : newInstances)
        {
            if (instance instanceof Package)
            {
                continue;
            }

            String classifierName = instance.getClassifier().getName();
            SourceInformation si = instance.getSourceInformation();
            if (si == null || !sourceId(source).equals(si.getSourceId()))
            {
                continue;
            }

            switch (classifierName)
            {
                case "Class":
                    addDefinitionToken(tokens, instance, TYPE_CLASS);
                    addClassMembers(tokens, instance, runtime, source);
                    break;
                case "Enumeration":
                    addDefinitionToken(tokens, instance, TYPE_ENUM);
                    addEnumValues(tokens, instance, source);
                    break;
                case "ConcreteFunctionDefinition":
                    addDefinitionToken(tokens, instance, TYPE_FUNCTION);
                    addFunctionParameters(tokens, instance, source);
                    break;
                case "NativeFunction":
                    addDefinitionToken(tokens, instance, TYPE_FUNCTION);
                    break;
                case "Profile":
                    addDefinitionToken(tokens, instance, TYPE_INTERFACE);
                    break;
                case "Association":
                    addDefinitionToken(tokens, instance, TYPE_STRUCT);
                    addClassMembers(tokens, instance, runtime, source);
                    break;
                default:
                    break;
            }
        }
    }

    private static void addDefinitionToken(List<RawToken> tokens, CoreInstance element, int tokenType)
    {
        SourceInformation si = element.getSourceInformation();
        if (si == null)
        {
            return;
        }

        // Extract a reasonable short name for length calculation.
        // element.getName() returns mangled names for functions (e.g., "greet_String_1__String_1_")
        // so we extract just the simple name before any underscore/parameter suffix.
        String name = element.getName();
        if (name == null || name.startsWith("@"))
        {
            return;
        }

        // For functions, getName() returns mangled signature (greet_String_1__String_1_).
        // Extract the short name (before first underscore that's followed by a type signature).
        // For classes/enums, getName() is already the short name.
        String shortName = name;
        if (tokenType == TYPE_FUNCTION)
        {
            int underscoreIdx = name.indexOf('_');
            if (underscoreIdx > 0)
            {
                shortName = name.substring(0, underscoreIdx);
            }
        }

        tokens.add(new RawToken(si.getStartLine(), si.getStartColumn(), shortName.length(), tokenType, MOD_DEFINITION));
    }

    private static void addClassMembers(List<RawToken> tokens, CoreInstance classElement, PureRuntime runtime, Source source)
    {
        try
        {
            ListIterable<? extends CoreInstance> properties = classElement.getValueForMetaPropertyToMany(M3Properties.properties);
            if (properties != null)
            {
                for (CoreInstance prop : properties)
                {
                    SourceInformation propSi = prop.getSourceInformation();
                    if (propSi == null || !sourceId(source).equals(propSi.getSourceId()))
                    {
                        continue;
                    }

                    String propName = prop.getName();
                    if (propName != null && !propName.startsWith("@"))
                    {
                        tokens.add(new RawToken(propSi.getStartLine(), propSi.getStartColumn(),
                                propName.length(), TYPE_PROPERTY, MOD_DEFINITION));
                    }

                    // Type reference in the property
                    addTypeReference(tokens, prop, runtime, source);
                }
            }
        }
        catch (Exception ignored)
        {
            // Properties not accessible
        }
    }

    private static void addTypeReference(List<RawToken> tokens, CoreInstance prop, PureRuntime runtime, Source source)
    {
        try
        {
            CoreInstance genericType = prop.getValueForMetaPropertyToOne(M3Properties.genericType);
            if (genericType == null)
            {
                return;
            }
            CoreInstance rawType = genericType.getValueForMetaPropertyToOne(M3Properties.rawType);
            if (rawType != null)
            {
                rawType = ImportStub.withImportStubByPass(rawType, runtime.getProcessorSupport());
            }
            if (rawType == null)
            {
                return;
            }
            SourceInformation typeSi = genericType.getSourceInformation();
            if (typeSi == null || !sourceId(source).equals(typeSi.getSourceId()))
            {
                return;
            }
            String typeName = rawType.getName();
            if (typeName != null && !typeName.startsWith("@"))
            {
                tokens.add(new RawToken(typeSi.getStartLine(), typeSi.getStartColumn(),
                        typeName.length(), TYPE_TYPE, 0));
            }
        }
        catch (Exception ignored)
        {
            // Type info not accessible
        }
    }

    private static void addEnumValues(List<RawToken> tokens, CoreInstance enumeration, Source source)
    {
        try
        {
            ListIterable<? extends CoreInstance> values = enumeration.getValueForMetaPropertyToMany(M3Properties.values);
            if (values != null)
            {
                for (CoreInstance val : values)
                {
                    SourceInformation valSi = val.getSourceInformation();
                    if (valSi == null || !sourceId(source).equals(valSi.getSourceId()))
                    {
                        continue;
                    }
                    String valName = val.getName();
                    if (valName != null)
                    {
                        tokens.add(new RawToken(valSi.getStartLine(), valSi.getStartColumn(),
                                valName.length(), TYPE_ENUM_MEMBER, 0));
                    }
                }
            }
        }
        catch (Exception ignored)
        {
            // Values not accessible
        }
    }

    private static void addFunctionParameters(List<RawToken> tokens, CoreInstance function, Source source)
    {
        try
        {
            ListIterable<? extends CoreInstance> params =
                    function.getValueForMetaPropertyToMany(M3Properties.classifierGenericType)
                            != null ? function.getValueForMetaPropertyToMany("parameters") : null;
            // Pure function parameters are stored differently — skip for now
            // TODO: extract parameter names from function signature
        }
        catch (Exception ignored)
        {
            // Parameters not accessible
        }
    }

    private static String sourceId(Source source)
    {
        return source.getId();
    }

    /**
     * Encode tokens as LSP delta format: [deltaLine, deltaStartChar, length, tokenType, tokenModifiers]
     */
    private static List<Integer> encodeDelta(List<RawToken> tokens)
    {
        List<Integer> data = new ArrayList<>(tokens.size() * 5);
        int prevLine = 0;
        int prevCol = 0;

        for (RawToken token : tokens)
        {
            int line = token.line - 1; // Convert to 0-based
            int col = token.column - 1;

            int deltaLine = line - prevLine;
            int deltaCol = (deltaLine == 0) ? (col - prevCol) : col;

            data.add(deltaLine);
            data.add(deltaCol);
            data.add(token.length);
            data.add(token.tokenType);
            data.add(token.tokenModifiers);

            prevLine = line;
            prevCol = col;
        }
        return data;
    }

    private static class RawToken
    {
        final int line;     // 1-based
        final int column;   // 1-based
        final int length;
        final int tokenType;
        final int tokenModifiers;

        RawToken(int line, int column, int length, int tokenType, int tokenModifiers)
        {
            this.line = line;
            this.column = column;
            this.length = Math.max(1, length);
            this.tokenType = tokenType;
            this.tokenModifiers = tokenModifiers;
        }
    }
}
