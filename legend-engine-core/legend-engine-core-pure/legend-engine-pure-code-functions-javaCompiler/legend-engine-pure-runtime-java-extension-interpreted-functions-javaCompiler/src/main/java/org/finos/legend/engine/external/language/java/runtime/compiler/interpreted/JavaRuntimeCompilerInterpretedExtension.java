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

package org.finos.legend.engine.external.language.java.runtime.compiler.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.external.language.java.runtime.compiler.interpreted.natives.CompileAndExecuteJava;
import org.finos.legend.engine.external.language.java.runtime.compiler.interpreted.natives.CompileJava;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;

public class JavaRuntimeCompilerInterpretedExtension extends BaseInterpretedExtension
{
    public JavaRuntimeCompilerInterpretedExtension()
    {
        super(Lists.fixedSize.with(
                Tuples.pair("compileJava_JavaSource_MANY__CompilationConfiguration_$0_1$__CompilationResult_1_", CompileJava::new),
                Tuples.pair("compileAndExecuteJava_JavaSource_MANY__CompilationConfiguration_$0_1$__ExecutionConfiguration_1__CompileAndExecuteResult_1_", CompileAndExecuteJava::new)
        ));
    }

    public static InterpretedExtension extension()
    {
        return new JavaRuntimeCompilerInterpretedExtension();
    }
}
