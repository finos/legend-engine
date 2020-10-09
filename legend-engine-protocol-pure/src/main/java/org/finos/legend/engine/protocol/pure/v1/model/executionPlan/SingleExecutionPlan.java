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

package org.finos.legend.engine.protocol.pure.v1.model.executionPlan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.PlatformImplementation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SingleExecutionPlan extends ExecutionPlan
{
    public boolean authDependent;
    public String kerberos;
    public Protocol serializer;
    public List<String> templateFunctions = Collections.emptyList();
    public ExecutionNode rootExecutionNode;
    public PlatformImplementation globalImplementationSupport;

    @Override
    public SingleExecutionPlan getSingleExecutionPlan(Map<String, ?> params)
    {
        return this;
    }

    @Override
    public SingleExecutionPlan getSingleExecutionPlan(Function<? super String, ?> parameterValueAccessor)
    {
        return this;
    }

    @JsonIgnore
    @BsonIgnore
    public MutableMap<String, Object> getExecutionStateParams(MutableMap<String, Object> inputState)
    {
        if (this.kerberos != null)
        {
            inputState.put("userId", this.kerberos);
        }
        return this.rootExecutionNode.getExecutionStateParams(inputState);
    }
}
