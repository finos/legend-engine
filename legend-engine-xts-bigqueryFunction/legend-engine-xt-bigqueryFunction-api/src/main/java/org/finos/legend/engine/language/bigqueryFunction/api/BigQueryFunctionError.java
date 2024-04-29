// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.bigqueryFunction.api;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;

public class BigQueryFunctionError extends FunctionActivatorError
{
    public ImmutableList<String> foundSQLs;

    public BigQueryFunctionError(String message, Iterable<String> foundSQLs)
    {
        super(message);
        this.foundSQLs = Lists.immutable.withAll(foundSQLs);
    }
}
