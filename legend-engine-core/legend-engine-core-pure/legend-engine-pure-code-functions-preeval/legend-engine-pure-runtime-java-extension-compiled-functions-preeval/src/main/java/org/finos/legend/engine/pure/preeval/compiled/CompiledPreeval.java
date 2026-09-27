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

package org.finos.legend.engine.pure.preeval.compiled;

import org.finos.legend.engine.pure.preeval.PreevalImplementation;
import org.finos.legend.engine.pure.preeval.Preevaluator;
import org.finos.legend.engine.pure.preeval.PrevalResults;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

public final class CompiledPreeval
{
    private CompiledPreeval()
    {
    }

    public static CoreInstance preval(Object item, PureMap inScopeVars, PureMap rollingInScopeVars, CoreInstance hooks, CoreInstance debug, ExecutionSupport executionSupport)
    {
        return PrevalResults.toPure(Preevaluator.preval((CoreInstance) item), ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
    }

    public static String preevalImplementation()
    {
        return PreevalImplementation.current().name();
    }
}
