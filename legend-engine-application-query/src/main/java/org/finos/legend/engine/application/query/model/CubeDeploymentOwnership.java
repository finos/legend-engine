// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.application.query.model;

/**
 * A cube owned by a deployment: its members may change it, as a Service's
 * deployment ownership
 */
public class CubeDeploymentOwnership extends CubeOwnership
{
    public String identifier;

    public CubeDeploymentOwnership()
    {
        // jackson
    }

    public CubeDeploymentOwnership(String identifier)
    {
        this.identifier = identifier;
    }
}
