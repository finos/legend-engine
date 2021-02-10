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

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;

import java.util.Map;

public class PureGrammarParserContext
{
    private final PureGrammarParserExtensions extensions;

    /**
     * flatDataRecordTypeFieldFuncMap holds a map of flat data record types to
     * another map of its corresponding field names' mapped to property function names.
     * This is used when building the flat data mapping transform function
     * <p>
     * TODO: this is very hacky and should be removed, potentially by using generic for flat data Column type
     */
    public final Map<String, Map<String, String>> flatDataRecordTypeFieldFuncMap;

    public PureGrammarParserContext(PureGrammarParserExtensions extensions)
    {
        this(extensions, Maps.mutable.empty());
    }

    public PureGrammarParserContext(PureGrammarParserExtensions extensions, Map<String, Map<String, String>> flatDataRecordTypeFieldFuncMap)
    {
        this.extensions = extensions;
        this.flatDataRecordTypeFieldFuncMap = flatDataRecordTypeFieldFuncMap;
    }

    public PureGrammarParserExtensions getPureGrammarParserExtensions()
    {
        return this.extensions;
    }
}
