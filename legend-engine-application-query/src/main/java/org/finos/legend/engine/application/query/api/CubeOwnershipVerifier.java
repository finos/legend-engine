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

package org.finos.legend.engine.application.query.api;

import org.finos.legend.engine.application.query.model.CubeDeploymentOwnership;
import org.finos.legend.engine.shared.core.identity.Identity;

/**
 * Tells the Cube store who belongs to a deployment, as Service's
 * OwnershipValidator does for services. The engine has none: a deployment of
 * it installs one with {@link java.util.ServiceLoader}. Without one, the store
 * can't check deployments, and only a cube's creator may change it.
 */
public interface CubeOwnershipVerifier
{
    /**
     * Whether the user is a member of the deployment: one who may change the
     * cubes it owns, and give a cube to it
     */
    boolean isOwner(Identity identity, CubeDeploymentOwnership ownership);
}
