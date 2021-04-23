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

package org.finos.legend.engine.plan.execution.result.graphFetch;

import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;

import java.util.stream.Stream;

public class GraphFetchResult extends Result
{
    private Stream<GraphObjectsBatch> graphObjectsBatchStream;
    private Result rootResult;

    public GraphFetchResult(Stream<GraphObjectsBatch> graphObjectsBatchStream, Result rootResult)
    {
        super("success");
        this.graphObjectsBatchStream = graphObjectsBatchStream;
        this.rootResult = rootResult;
    }

    public Stream<GraphObjectsBatch> getGraphObjectsBatchStream()
    {
        return this.graphObjectsBatchStream;
    }

    public Result getRootResult()
    {
        return this.rootResult;
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        throw new UnsupportedOperationException("Not supported!");
    }

    @Override
    public void close()
    {
        if (this.rootResult != null)
        {
            this.rootResult.close();
        }
    }
}
