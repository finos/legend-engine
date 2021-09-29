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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.AuthorizerPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.AuthorizerVisitor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.service.Authorizer;

import java.util.Objects;

public class AuthorizerFirstPassBuilder implements AuthorizerVisitor<Authorizer>
{
    private final CompileContext context;

    public AuthorizerFirstPassBuilder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public Authorizer visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.Authorizer authorizer)
    {
        return this.context.getCompilerExtensions().getExtraAuthorizerValueProcessors().stream()
                .map(processor -> processor.value(authorizer, this.context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported authorizer value type '" + authorizer.getClass() + "'"));
    }

    @Override
    public Authorizer visit(AuthorizerPointer authorizerPointer)
    {
        return this.context.resolveAuthorizer(authorizerPointer.authorizer, authorizerPointer.sourceInformation);
    }
}
