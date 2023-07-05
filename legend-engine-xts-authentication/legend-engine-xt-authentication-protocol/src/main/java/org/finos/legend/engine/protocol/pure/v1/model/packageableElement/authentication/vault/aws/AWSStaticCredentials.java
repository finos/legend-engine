// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
public class AWSStaticCredentials extends AWSCredentials
{
    public CredentialVaultSecret accessKeyId;
    public CredentialVaultSecret secretAccessKey;

    public AWSStaticCredentials()
    {
    }

    public AWSStaticCredentials(CredentialVaultSecret accessKeyId, CredentialVaultSecret secretAccessKey)
    {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    @Override
    public <T> T accept(AWSCredentialsVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
