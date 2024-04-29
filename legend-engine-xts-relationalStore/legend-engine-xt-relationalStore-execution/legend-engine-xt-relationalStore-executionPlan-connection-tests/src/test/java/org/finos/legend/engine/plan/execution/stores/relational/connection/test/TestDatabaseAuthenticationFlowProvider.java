//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.test;

import org.finos.legend.engine.authentication.flows.H2StaticWithTestUserPasswordFlow;
import org.finos.legend.engine.authentication.provider.AbstractDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.ArrayList;
import java.util.List;

public class TestDatabaseAuthenticationFlowProvider extends AbstractDatabaseAuthenticationFlowProvider
{
    public RecordingH2StaticWithTestUserPasswordFlow testFlow = new RecordingH2StaticWithTestUserPasswordFlow();

    public TestDatabaseAuthenticationFlowProvider()
    {
        super.registerFlow(testFlow);
    }

    public static class RecordingH2StaticWithTestUserPasswordFlow extends H2StaticWithTestUserPasswordFlow
    {
        List<Throwable> invocations = new ArrayList<>();

        @Override
        public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, TestDatabaseAuthenticationStrategy authenticationStrategy) throws Exception
        {
            Credential credential = super.makeCredential(identity, datasourceSpecification, authenticationStrategy);
            synchronized (RecordingH2StaticWithTestUserPasswordFlow.class)
            {
                // record the current thread's stack trace
                Throwable throwable = new Exception().fillInStackTrace();
                invocations.add(throwable);
            }
            return credential;
        }
    }
}
