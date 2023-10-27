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

package org.finos.legend.engine.query.sql.api.sources;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;

import java.util.List;

/**
 * @deprecated
 * Use {@link org.finos.legend.engine.query.sql.providers.core.SQLSource}
 */
@Deprecated
public class SQLSource extends org.finos.legend.engine.query.sql.providers.core.SQLSource
{
    public SQLSource(String type, Lambda func, String mapping, Runtime runtime, List<ExecutionOption> executionOptions, List<SQLSourceArgument> key)
    {
        this(type, func, mapping, runtime, executionOptions, null, key);
    }

    public SQLSource(String type, Lambda func, String mapping, Runtime runtime, List<ExecutionOption> executionOptions, ExecutionContext executionContext, List<SQLSourceArgument> key)
    {
        super(type, func, mapping, runtime, executionOptions, executionContext, ListIterate.collect(key, k -> new org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument(k.getName(), k.getIndex(), k.getValue())));
    }
}