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

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.dependencies.store.shared.IResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class Result implements IResult
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Result.class);

    public String status;
    public GenerationInfo generationInfo = null;
    public List<ExecutionActivity> activities;

    private final List<AutoCloseable> closeables = Lists.mutable.empty();

    public Result(String status)
    {
        this.status = status;
        this.activities = Lists.mutable.empty();
    }

    public Result(String status, List<ExecutionActivity> activities)
    {
        this.status = status;
        this.activities = activities;
    }

    public abstract <T> T accept(ResultVisitor<T> resultVisitor);

    public Result realizeInMemory()
    {
        return this;
    }

    public Result addCloseable(AutoCloseable closeable)
    {
        if (closeable != null)
        {
            this.closeables.add(closeable);
        }
        return this;
    }

    public void close()
    {
        this.closeables.forEach(closeable ->
        {
            try
            {
                closeable.close();
            }
            catch (Exception e)
            {
                LOGGER.error("error closing resource attached to result", e);
            }
        });
        this.closeables.clear();
    }
}
