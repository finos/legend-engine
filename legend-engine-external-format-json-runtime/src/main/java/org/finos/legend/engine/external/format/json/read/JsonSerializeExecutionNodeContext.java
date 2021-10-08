package org.finos.legend.engine.external.format.json.read;

import org.finos.legend.engine.plan.dependencies.store.inMemory.IStoreStreamReader;
import org.finos.legend.engine.plan.dependencies.store.inMemory.IStoreStreamReadingExecutionNodeContext;
import org.finos.legend.engine.plan.execution.nodes.helpers.freemarker.FreeMarkerExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.DefaultExecutionNodeContext;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.shared.core.url.UrlFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class JsonSerializeExecutionNodeContext extends DefaultExecutionNodeContext implements IStoreStreamReadingExecutionNodeContext
{
    private final ExecutionState state;

    public static ExecutionNodeJavaPlatformHelper.ExecutionNodeContextFactory factory()
    {
        return (ExecutionState state, Result childResult) -> new JsonSerializeExecutionNodeContext(state, childResult);
    }

    private JsonSerializeExecutionNodeContext(ExecutionState state, Result childResult)
    {
        super(state, childResult);
        this.state = super.state;
    }

    @Override
    public IStoreStreamReader createReader(String s)
    {
        return null;
    }

    @Override
    public URL createUrl(String url) throws MalformedURLException
    {
        return UrlFactory.create(FreeMarkerExecutor.process(url, this.state));
    }
}
