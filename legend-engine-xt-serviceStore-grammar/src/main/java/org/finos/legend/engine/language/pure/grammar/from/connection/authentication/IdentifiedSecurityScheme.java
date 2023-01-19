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

package org.finos.legend.engine.language.pure.grammar.from.connection.authentication;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecuritySchemeVisitor;

public class IdentifiedSecurityScheme extends SecurityScheme
{

    public String id;
    public SourceInformation sourceInformation;

    public IdentifiedSecurityScheme()
    {
    }

    public IdentifiedSecurityScheme(String id, SourceInformation sourceInformation)
    {
        this.id = id;
        this.sourceInformation = sourceInformation;
    }

    @Override
    public <T> T accept(SecuritySchemeVisitor<T> securitySchemeVisitor)
    {
        return null;
    }
}
