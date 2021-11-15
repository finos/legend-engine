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

package org.finos.legend.engine.language.pure.dsl.service.execution;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.security.auth.Subject;

import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;

/* Work in progress, do not use */

public class ServiceRunnerInput
{
    private List<Object> args = Collections.emptyList();
    private Identity identity = null;
    private OperationalContext operationalContext = OperationalContext.newInstance();
    private SerializationFormat serializationFormat = SerializationFormat.DEFAULT;

    public static ServiceRunnerInput newInstance()
    {
        return new ServiceRunnerInput();
    }

    public ServiceRunnerInput withArgs(List<Object> args)
    {
        this.args = Objects.requireNonNull(args, "args must not be null");
        return this;
    }

    public List<Object> getArgs()
    {
        return this.args;
    }

    public ServiceRunnerInput withIdentity(Subject subject)
    {
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(subject);
        return this.withIdentity(identity);
    }

    public ServiceRunnerInput withIdentity(Identity identity)
    {
        this.identity = Objects.requireNonNull(identity, "identity must not be null");
        return this;
    }

    public Identity getIdentity()
    {
        return this.identity;
    }

    public ServiceRunnerInput withOperationalContext(OperationalContext operationalContext)
    {
        this.operationalContext = Objects.requireNonNull(operationalContext, "operationalContext must not be null");
        return this;
    }

    public OperationalContext getOperationalContext()
    {
        return this.operationalContext;
    }

    public ServiceRunnerInput withSerializationFormat(SerializationFormat serializationFormat)
    {
        this.serializationFormat = Objects.requireNonNull(serializationFormat, "serializationFormat must not be null");
        return this;
    }

    public SerializationFormat getSerializationFormat()
    {
        return this.serializationFormat;
    }
}
