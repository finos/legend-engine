// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
public class SystemPropertiesSecret extends CredentialVaultSecret
{
    public String systemPropertyName;

    public SystemPropertiesSecret()
    {
    }

    public SystemPropertiesSecret(String systemPropertyName)
    {
        this.systemPropertyName = systemPropertyName;
    }

    @Override
    public <T> T accept(PackageableElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @Override
    public <T> T accept(CredentialVaultSecretVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}