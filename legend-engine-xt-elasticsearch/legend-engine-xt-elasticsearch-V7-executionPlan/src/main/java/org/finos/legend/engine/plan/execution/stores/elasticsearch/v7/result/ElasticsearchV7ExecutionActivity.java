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
//

package org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import org.finos.legend.engine.plan.execution.result.ExecutionActivity;

public class ElasticsearchV7ExecutionActivity extends ExecutionActivity
{
    public final String esRequest;
    public final URI uri;

    public ElasticsearchV7ExecutionActivity(@JsonProperty("uri") URI uri, @JsonProperty("esRequest") String esRequest)
    {
        this._type = "es7_activity";
        this.uri = uri;
        this.esRequest = esRequest;
    }
}
