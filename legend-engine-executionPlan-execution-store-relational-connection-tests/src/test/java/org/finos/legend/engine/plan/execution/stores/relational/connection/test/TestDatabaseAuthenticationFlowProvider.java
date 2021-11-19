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

    public TestDatabaseAuthenticationFlowProvider() {
        super.registerFlow(testFlow);
    }

    public static class RecordingH2StaticWithTestUserPasswordFlow extends H2StaticWithTestUserPasswordFlow
    {
        List<Throwable> invocations = new ArrayList<>();

        @Override
        public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, TestDatabaseAuthenticationStrategy authenticationStrategy) throws Exception
        {
            Credential credential = super.makeCredential(identity, datasourceSpecification, authenticationStrategy);
            synchronized (RecordingH2StaticWithTestUserPasswordFlow.class) {
                // record the current thread's stack trace
                Throwable throwable = new Exception().fillInStackTrace();
                invocations.add(throwable);
            }
            return credential;
        }
    }
}
