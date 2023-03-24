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

package org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.demo;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;

import java.util.List;
import java.util.ServiceLoader;

public interface IAuthenticationDemoParserExtension extends PureGrammarParserExtension
{
    static List<IAuthenticationDemoParserExtension> getExtensions()
    {
        return Lists.mutable.ofAll(ServiceLoader.load(IAuthenticationDemoParserExtension.class));
    }
}
