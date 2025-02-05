// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers.core;

import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;

import java.util.Collections;
import java.util.List;

public class SQLSource
{
    private final String type;
    private final Lambda func;
    private final String mapping;
    private final Runtime runtime;
    private final List<ExecutionOption> executionOptions;
    private final ExecutionContext executionContext;
    private final List<SQLSourceArgument> key;

    public SQLSource(String type, Lambda func, String mapping, Runtime runtime, List<ExecutionOption> executionOptions, ExecutionContext executionContext, List<SQLSourceArgument> key)
    {
        this.type = type;
        this.func = func;
        this.mapping = mapping;
        this.runtime = runtime;
        this.executionOptions = executionOptions;
        this.executionContext = executionContext;
        this.key = key == null ? Collections.emptyList() : key;
    }

    public String getType()
    {
        return type;
    }

    public Lambda getFunc()
    {
        return func;
    }

    public String getMapping()
    {
        return mapping;
    }

    public Runtime getRuntime()
    {
        return runtime;
    }

    public List<ExecutionOption> getExecutionOptions()
    {
        return executionOptions;
    }

    public ExecutionContext getExecutionContext()
    {
        return executionContext;
    }

    public List<SQLSourceArgument> getKey()
    {
        return key;
    }
}
