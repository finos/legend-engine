// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result;

import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;

public interface ResultVisitor<T>
{
    T visit(ErrorResult errorResult);

    T visit(StreamingObjectResult tStreamingObjectResult);

    T visit(JsonStreamingResult jsonStreamingResult);

    T visit(ConstantResult constantResult);

    T visit(MultiResult multiResult);

    T visit(UpdateNodeResult updateNodeResult);

    default T visit(StreamingResult multiResult)
    {
        throw new UnsupportedOperationException("Not supported");
    }
}
