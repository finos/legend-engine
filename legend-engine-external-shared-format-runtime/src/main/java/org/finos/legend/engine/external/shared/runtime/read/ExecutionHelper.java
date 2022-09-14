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

package org.finos.legend.engine.external.shared.runtime.read;

import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.InputStreamResult;
import org.finos.legend.engine.plan.execution.result.Result;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ExecutionHelper
{
    public static InputStream inputStreamFromResult(Result result)
    {
        if (result instanceof InputStreamResult)
        {
            return ((InputStreamResult) result).getInputStream();
        }
        else if (result instanceof ConstantResult)
        {
            Object value = ((ConstantResult) result).getValue();
            InputStream stream;

            if (value instanceof InputStream)
            {
                stream = (InputStream) value;
            }
            else if (value instanceof String)
            {
                stream = new ByteArrayInputStream(((String) value).getBytes(StandardCharsets.UTF_8));
            }
            else
            {
                throw new RuntimeException("Expected input stream or string, found : " + value.getClass().getSimpleName());
            }
            return stream;
        }
        else
        {
            throw new IllegalStateException("Unsupported result type for external formats: " + result.getClass().getSimpleName());
        }
    }
}
