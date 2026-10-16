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

package org.finos.legend.engine.language.pure.grammar.from.extension;

/**
 * Takes over the parsing of whole models (see {@link ModelParser}) from the engine's own parser, which the replacing
 * parser receives, and may delegate to: the engine's parser splits the code into sections and parses each with its
 * section type's parser, section parser overrides included (see {@link SectionParserOverride}).
 */
public interface ModelParserOverride
{
    /**
     * The parser to use for whole models.
     *
     * @param replaced the parser that would otherwise be used
     * @return the parser to use instead, which may delegate to {@code replaced}
     */
    ModelParser override(ModelParser replaced);
}
