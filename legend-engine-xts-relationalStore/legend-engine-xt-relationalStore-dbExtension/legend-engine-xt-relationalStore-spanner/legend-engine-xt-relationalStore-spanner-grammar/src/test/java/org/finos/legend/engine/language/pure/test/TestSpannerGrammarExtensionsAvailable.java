// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.pure.test;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SpannerCompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.grammar.from.IRelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.SpannerGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.SpannerGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.junit.Assert;
import org.junit.Test;

import java.util.ServiceLoader;

public class TestSpannerGrammarExtensionsAvailable
{
    @Test
    public void testCompilerExtensionAvailable()
    {
        MutableList<Class<?>> compilerExtensions =
                Lists.mutable.withAll(ServiceLoader.load(CompilerExtension.class))
                        .collect(Object::getClass);
        Assert.assertTrue(compilerExtensions.contains(SpannerCompilerExtension.class));
    }

    @Test
    public void testGrammarParserExtensionAvailable()
    {
        MutableList<Class<?>> relationalGrammarParserExtensions =
                Lists.mutable.withAll(ServiceLoader.load(IRelationalGrammarParserExtension.class))
                        .collect(Object::getClass);
        Assert.assertTrue(relationalGrammarParserExtensions.contains(SpannerGrammarParserExtension.class));
    }

    @Test
    public void testGrammarComposerExtensionAvailable()
    {
        MutableList<Class<?>> pureGrammarComposerExtensions =
                Lists.mutable.withAll(ServiceLoader.load(PureGrammarComposerExtension.class))
                        .collect(Object::getClass);
        Assert.assertTrue(pureGrammarComposerExtensions.contains(SpannerGrammarComposerExtension.class));
    }
}
