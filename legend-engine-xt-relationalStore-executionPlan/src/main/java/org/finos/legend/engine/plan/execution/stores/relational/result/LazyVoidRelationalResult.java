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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.pac4j.core.profile.CommonProfile;

import java.sql.Connection;

public class LazyVoidRelationalResult extends VoidRelationalResult
{
    private String sql;
    private MutableList<CommonProfile> profiles;

    public LazyVoidRelationalResult(MutableList<ExecutionActivity> activities, Connection connection, MutableList<CommonProfile> profiles)
    {
        this.connection = connection;
        this.profiles = profiles;
        this.sql = ((RelationalExecutionActivity) activities.getLast()).sql;
    }

    @Override
    public void close()
    {
        this.execute(this.sql, this.connection, this.profiles);
    }
}