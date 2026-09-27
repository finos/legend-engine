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
 * Takes over a section type from the parser that would otherwise parse it: another extension's (see
 * {@link PureGrammarParserExtension#getExtraSectionParsers()}) or one of the engine's built-in ones. The replacing
 * parser receives the replaced one, and may delegate to it.
 */
public interface SectionParserOverride
{
    /**
     * @return the section type taken over, e.g. {@code Pure}
     */
    String getSectionTypeName();

    /**
     * The parser to use for the section type.
     *
     * @param replaced the parser the section type would otherwise use
     * @return the parser to use instead, which may delegate to {@code replaced}
     */
    SectionParser override(SectionParser replaced);
}
