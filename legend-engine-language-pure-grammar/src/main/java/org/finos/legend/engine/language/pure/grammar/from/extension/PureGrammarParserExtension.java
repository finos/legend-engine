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

package org.finos.legend.engine.language.pure.grammar.from.extension;

import java.util.Collections;

public interface PureGrammarParserExtension
{
    default Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Collections.emptyList();
    }

    default Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return Collections.emptyList();
    }

    default Iterable<? extends MappingElementParser> getExtraMappingElementParsers()
    {
        return Collections.emptyList();
    }

    default Iterable<? extends MappingTestInputDataParser> getExtraMappingTestInputDataParsers()
    {
        return Collections.emptyList();
    }

    default Iterable<? extends AuthorizerValueParser> getExtraAuthorizerParsers()
    {
        return Collections.emptyList();
    }
}
