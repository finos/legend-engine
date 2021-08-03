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

import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.IdentityFactory;
import org.finos.legend.engine.shared.core.url.StreamProvider;

import javax.security.auth.Subject;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/* Work in progress, do not use */

public class ServiceRunnerInput
{
    private List<Object> args = Collections.emptyList();
    private StreamProvider connectionInput = null;
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

    List<Object> getArgs()
    {
        return this.args;
    }

    public ServiceRunnerInput withConnectionInput(String stringInput)
    {
        return this.withConnectionInput(AbstractServicePlanExecutor.newStreamProvider(Objects.requireNonNull(stringInput, "stringInput must not be null")));
    }

    public ServiceRunnerInput withConnectionInput(byte[] byteArrayInput)
    {
        return this.withConnectionInput(AbstractServicePlanExecutor.newStreamProvider(Objects.requireNonNull(byteArrayInput, "byteArrayInput must not be null")));
    }

    public ServiceRunnerInput withConnectionInput(InputStream streamInput)
    {
        return this.withConnectionInput(AbstractServicePlanExecutor.newStreamProvider(Objects.requireNonNull(streamInput, "streamInput must not be null")));
    }

    public ServiceRunnerInput withConnectionInput(Map<String, ? extends InputStream> multiStreamInput)
    {
        return this.withConnectionInput(AbstractServicePlanExecutor.newStreamProvider(Objects.requireNonNull(multiStreamInput, "multiStreamInput must not be null")));
    }

    public ServiceRunnerInput withConnectionInput(StreamProvider connectionInput)
    {
        this.connectionInput = Objects.requireNonNull(connectionInput, "connectionInput must not be null");
        return this;
    }

    StreamProvider getConnectionInput()
    {
        return this.connectionInput;
    }

    public ServiceRunnerInput withIdentity(Subject subject)
    {
        return this.withIdentity(IdentityFactory.newSubjectIdentity(subject));
    }

    public ServiceRunnerInput withIdentity(Identity identity)
    {
        this.identity = Objects.requireNonNull(identity, "identity must not be null");
        return this;
    }

    Identity getIdentity()
    {
        return this.identity;
    }

    public ServiceRunnerInput withOperationalContext(OperationalContext operationalContext)
    {
        this.operationalContext = Objects.requireNonNull(operationalContext, "operationalContext must not be null");
        return this;
    }

    OperationalContext getOperationalContext()
    {
        return this.operationalContext;
    }

    public ServiceRunnerInput withSerializationFormat(SerializationFormat serializationFormat)
    {
        this.serializationFormat = Objects.requireNonNull(serializationFormat, "serializationFormat must not be null");
        return this;
    }

    SerializationFormat getSerializationFormat()
    {
        return this.serializationFormat;
    }
}
