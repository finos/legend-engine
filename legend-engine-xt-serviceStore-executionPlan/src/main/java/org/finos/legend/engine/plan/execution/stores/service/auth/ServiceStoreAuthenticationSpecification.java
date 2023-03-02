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

package org.finos.legend.engine.plan.execution.stores.service.auth;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AuthenticationSchemeRequirement;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SingleAuthenticationSchemeRequirement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;

import java.util.List;
import java.util.Map;

public class ServiceStoreAuthenticationSpecification extends AuthenticationSpecification
{

    public List<AuthenticationSchemeRequirement> authenticationSchemeRequirements = Lists.mutable.empty();

    public ServiceStoreAuthenticationSpecification(List<AuthenticationSchemeRequirement> authenticationSchemeRequirements, List<SecurityScheme> securitySchemes)
    {
        if (authenticationSchemeRequirements != null)
        {
            this.authenticationSchemeRequirements = authenticationSchemeRequirements;
        }
        //For already existing persisted plans
        if (securitySchemes != null)
        {
            securitySchemes.forEach(s ->
            {
                SingleAuthenticationSchemeRequirement singleAuthenticationSchemeRequirement = new SingleAuthenticationSchemeRequirement();
                singleAuthenticationSchemeRequirement.securityScheme = s;
                this.authenticationSchemeRequirements.add(singleAuthenticationSchemeRequirement);
            });
        }
    }

    @Override
    public <T> T accept(AuthenticationSpecificationVisitor<T> visitor)
    {
        throw new RuntimeException("Cannot process ServiceStoreAuthenticationSpecification\n");
    }
}
