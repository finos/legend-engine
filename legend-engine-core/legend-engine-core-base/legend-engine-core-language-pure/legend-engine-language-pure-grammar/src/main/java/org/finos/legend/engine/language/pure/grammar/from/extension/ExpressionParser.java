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

import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;

/**
 * Parses the standalone expressions {@code PureGrammarParser} parses: lambdas, value specifications and graph fetch
 * trees, each positioned with the given offsets (the line offset added to every line, the column offset to the first
 * line's columns).
 */
public interface ExpressionParser
{
    LambdaFunction parseLambda(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo);

    ValueSpecification parseValueSpecification(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo);

    RootGraphFetchTree parseGraphFetch(String code, String sourceId, int lineOffset, int columnOffset, boolean returnSourceInfo);
}
