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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.List;

public abstract class Result implements IResult
{
    public String status;
    private boolean isClosed;
    public GenerationInfo generationInfo = null;
    public List<ExecutionActivity> activities;
    private PropertyChangeSupport propertyChangeSupport;

    public Result(String status)
    {
        this.status = status;
        this.activities = Lists.mutable.empty();
        isClosed = false;
    }

    public Result(String status, List<ExecutionActivity> activities)
    {
        this.status = status;
        this.activities = activities;
    }

    public void setPropertyChangeListeners(PropertyChangeListener... listeners)
    {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        Arrays.stream(listeners).forEach(l -> propertyChangeSupport.addPropertyChangeListener(l));
    }

    public abstract <T> T accept(ResultVisitor<T> resultVisitor);

    public Result realizeInMemory()
    {
        return this;
    }

    public void close()
    {
        if (!this.isClosed)
        {
            this.isClosed = true;
            if (propertyChangeSupport != null)
            {
                propertyChangeSupport.firePropertyChange("isClosed", false, isClosed);
            }
        }
    }
}
