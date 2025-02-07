// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.deployment.model;

public class DeploymentResponse
{

    public String key;
    public String element;
    public DeploymentStatus status;
    public String message;


    public DeploymentResponse(String key,String element, DeploymentStatus status, String message)
    {
        this.key = key;
        this.element = element;
        this.status = status;
        this.message = message;
    }

    public DeploymentResponse(String key, DeploymentStatus status, String message)
    {
        this(key, null, status, message);
    }

    public DeploymentResponse(String key)
    {
        this(key, null, null, null);
    }
}
