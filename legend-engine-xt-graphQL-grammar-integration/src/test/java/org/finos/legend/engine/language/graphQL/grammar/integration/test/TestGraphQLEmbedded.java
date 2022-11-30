// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.graphQL.grammar.integration.test;

import org.finos.legend.engine.pure.runtime.compiler.Tools;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.junit.Test;

public abstract class TestGraphQLEmbedded
{
    protected static PureRuntime runtime;
    protected static FunctionExecution functionExecution;

    @Test
    public void testCompileGraphQL()
    {
        test("assertEquals('ok', meta::legend::compileVS('#GQL{type ok{id : String}}#')->cast(@meta::external::query::graphQL::metamodel::sdl::ExecutableDocument).definitions->cast(@meta::external::query::graphQL::metamodel::sdl::typeSystem::ObjectTypeDefinition).name);");
    }

    @Test
    public void testCompileEnumGraphQL()
    {
        test("assertEquals(meta::external::query::graphQL::metamodel::sdl::executable::OperationType.mutation, meta::legend::compileVS('#GQL{mutation { likeStory(storyID: 12345) { story { likeCount } } }}#')->cast(@meta::external::query::graphQL::metamodel::sdl::Document).definitions->at(0)->cast(@meta::external::query::graphQL::metamodel::sdl::executable::OperationDefinition).type)");
    }

    private void test(String code)
    {
        Tools.test(code, functionExecution, runtime);
    }
}
