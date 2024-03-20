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

package org.finos.legend.engine.plan.dependencies.domain.dataQuality;

public class BasicRelativePathNode implements RelativePathNode
{
    private String propertyName = null;

    private Long index = null;

    private BasicRelativePathNode()
    {
        // For privacy
    }

    public String getPropertyName()
    {
        return this.propertyName;
    }

    public Long getIndex()
    {
        return this.index;
    }

    public static RelativePathNode newRelativePathNode(String propertyName, long index)
    {
        BasicRelativePathNode result = new BasicRelativePathNode();
        result.propertyName = propertyName;
        result.index = index;
        return result;
    }

    public static RelativePathNode newRelativePathNode(String propertyName)
    {
        BasicRelativePathNode result = new BasicRelativePathNode();
        result.propertyName = propertyName;
        result.index = null;
        return result;
    }
}
