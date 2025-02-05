//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.language.java.runtime.compiler.compiled.natives;

import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;

public class CompileJava extends AbstractNativeFunctionGeneric
{
    public CompileJava()
    {
        super("org.finos.legend.engine.external.language.java.runtime.compiler.shared.PureCompileAndExecuteJava.compilePure",
                new Object[]{"Iterable<? extends org.finos.legend.pure.m4.coreinstance.CoreInstance>", "org.finos.legend.pure.m4.coreinstance.CoreInstance", "ExecutionSupport"},
                false, true, true,
                "compileJava_JavaSource_MANY__CompilationConfiguration_$0_1$__CompilationResult_1_");
    }
}
