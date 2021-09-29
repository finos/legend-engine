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

package org.finos.legend.engine.language.pure.grammar.from.extension;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.LazyIterate;

import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;

public class PureGrammarParserExtensions
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private final ImmutableList<PureGrammarParserExtension> extensions;
    private final MapIterable<String, SectionParser> sectionParsers;
    private final MapIterable<String, ConnectionValueParser> connectionParsers;
    private final MapIterable<String, AuthorizerValueParser> authorizerParsers;
    private final MapIterable<String, MappingElementParser> mappingElementParsers;
    private final MapIterable<String, MappingTestInputDataParser> mappingTestInputDataParsers;

    private PureGrammarParserExtensions(Iterable<? extends PureGrammarParserExtension> extensions)
    {
        this.extensions = Lists.immutable.withAll(extensions);
        this.sectionParsers = indexExtraSectionParsers(this.extensions);
        this.connectionParsers = indexExtraConnectionValueParsers(this.extensions);
        this.mappingElementParsers = indexExtraMappingElementParsers(this.extensions);
        this.mappingTestInputDataParsers = indexExtraMappingTestInputDataParsers(this.extensions);
        this.authorizerParsers = indexExtraAuthorizerValueParsers(this.extensions);
    }

    public List<PureGrammarParserExtension> getExtensions()
    {
        return this.extensions.castToList();
    }

    public SectionParser getExtraSectionParser(String type)
    {
        return this.sectionParsers.get(type);
    }

    public ConnectionValueParser getConnectionValueParser(String type)
    {
        return this.connectionParsers.get(type);
    }

    public AuthorizerValueParser getAuthorizerValueParser(String type)
    {
        return this.authorizerParsers.get(type);
    }

    public MappingElementParser getExtraMappingElementParser(String type)
    {
        return this.mappingElementParsers.get(type);
    }

    public MappingTestInputDataParser getExtraMappingTestInputDataParser(String type)
    {
        return this.mappingTestInputDataParsers.get(type);
    }

    public static PureGrammarParserExtensions fromExtensions(Iterable<? extends PureGrammarParserExtension> extensions)
    {
        return new PureGrammarParserExtensions(extensions);
    }

    public static PureGrammarParserExtensions fromExtensions(PureGrammarParserExtension... extensions)
    {
        return fromExtensions(Lists.immutable.with(extensions));
    }

    public static PureGrammarParserExtensions fromAvailableExtensions()
    {
        return fromExtensions(ServiceLoader.load(PureGrammarParserExtension.class));
    }

    public static void logExtensionList()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(LazyIterate.collect(ServiceLoader.load(PureGrammarParserExtension.class), extension -> "- " + extension.getClass().getSimpleName()).makeString("Pure grammar parser extension(s) loaded:\n", "\n", ""));
        }
    }

    private static MapIterable<String, SectionParser> indexExtraSectionParsers(Iterable<? extends PureGrammarParserExtension> extensions)
    {
        return indexByKey(LazyIterate.flatCollect(extensions, PureGrammarParserExtension::getExtraSectionParsers),
                SectionParser::getSectionTypeName,
                "Conflicting parsers for section type");
    }

    private static MapIterable<String, ConnectionValueParser> indexExtraConnectionValueParsers(Iterable<? extends PureGrammarParserExtension> extensions)
    {
        return indexByKey(LazyIterate.flatCollect(extensions, PureGrammarParserExtension::getExtraConnectionParsers),
                ConnectionValueParser::getConnectionTypeName,
                "Conflicting parsers for connection type");
    }

    private static MapIterable<String, AuthorizerValueParser> indexExtraAuthorizerValueParsers(Iterable<? extends PureGrammarParserExtension> extensions)
    {
        return indexByKey(LazyIterate.flatCollect(extensions, PureGrammarParserExtension::getExtraAuthorizerParsers),
                AuthorizerValueParser::getAuthorizerTypeName,
                "Conflicting parsers for authorizer type");
    }

    private static MapIterable<String, MappingElementParser> indexExtraMappingElementParsers(Iterable<? extends PureGrammarParserExtension> extensions)
    {
        return indexByKey(LazyIterate.flatCollect(extensions, PureGrammarParserExtension::getExtraMappingElementParsers),
                MappingElementParser::getElementTypeName,
                "Conflicting parsers for mapping element type");
    }

    private static MapIterable<String, MappingTestInputDataParser> indexExtraMappingTestInputDataParsers(Iterable<? extends PureGrammarParserExtension> extensions)
    {
        return indexByKey(LazyIterate.flatCollect(extensions, PureGrammarParserExtension::getExtraMappingTestInputDataParsers),
                MappingTestInputDataParser::getInputDataTypeName,
                "Conflicting parsers for mapping test input data type");
    }

    private static <T> MapIterable<String, T> indexByKey(Iterable<? extends T> elements, Function<? super T, String> keyFn, String conflictMessagePrefix)
    {
        MutableMap<String, T> index = Maps.mutable.empty();
        elements.forEach(e ->
        {
            String key = keyFn.apply(e);
            if (index.put(key, e) != null)
            {
                throw new IllegalArgumentException(conflictMessagePrefix + ": " + key);
            }
        });
        return index;
    }
}
