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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication;

public class MiddleTierKeytabAuthenticationStrategy extends AuthenticationStrategy
{
    public String principal;
    public String keytabVaultReference;
    public String keytabMetadataVaultReference;

    public MiddleTierKeytabAuthenticationStrategy()
    {
        // jackson
    }

    public MiddleTierKeytabAuthenticationStrategy(String principal, String keytabVaultReference, String keytabMetadataVaultReference)
    {
        this.principal = principal;
        this.keytabVaultReference = keytabVaultReference;
        this.keytabMetadataVaultReference = keytabMetadataVaultReference;
    }

    @Override
    public <T> T accept(AuthenticationStrategyVisitor<T> authenticationStrategyVisitor)
    {
        return authenticationStrategyVisitor.visit(this);
    }
}
