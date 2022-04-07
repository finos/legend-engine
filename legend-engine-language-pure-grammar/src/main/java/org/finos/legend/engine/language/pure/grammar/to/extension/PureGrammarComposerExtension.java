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

package org.finos.legend.engine.language.pure.grammar.to.extension;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;

import java.util.ArrayList;
import java.util.List;

public interface PureGrammarComposerExtension
{
    /**
     * This method takes a list of elements which are supposed to be within a section (specified by the parser name)
     */
    default List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return new ArrayList<>();
    }

    class PureFreeSectionGrammarComposerResult
    {
        public final String value;
        public final List<? extends PackageableElement> composedElements;

        public PureFreeSectionGrammarComposerResult(String value, List<? extends PackageableElement> composedElements)
        {
            this.value = value;
            this.composedElements = composedElements;
        }
    }

    /**
     * This method takes in a set of elements and remove ones supported by the element plugins then compose them, hence `free` section
     */
    default List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return new ArrayList<>();
    }

    default List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return new ArrayList<>();
    }

    default List<Function2<ClassMapping, PureGrammarComposerContext, String>> getExtraClassMappingComposers()
    {
        return new ArrayList<>();
    }

    default List<Function2<AssociationMapping, PureGrammarComposerContext, String>> getExtraAssociationMappingComposers()
    {
        return new ArrayList<>();
    }

    default List<Function2<InputData, PureGrammarComposerContext, String>> getExtraMappingTestInputDataComposers()
    {
        return new ArrayList<>();
    }

    default List<Function2<EmbeddedData, PureGrammarComposerContext, ContentWithType>> getExtraEmbeddedDataComposers()
    {
        return new ArrayList<>();
    }

    default List<Function2<TestAssertion, PureGrammarComposerContext, ContentWithType>> getExtraTestAssertionComposers()
    {
        return new ArrayList<>();
    }
}
