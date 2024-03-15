// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.language.pure.modelManager.sdlc;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import java.util.List;
import java.util.function.Function;
import javax.security.auth.Subject;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.modelManager.sdlc.alloy.AlloySDLCLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.pure.PureServerLoader;
import org.finos.legend.engine.language.pure.modelManager.sdlc.workspace.WorkspaceSDLCLoader;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureSDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLCVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.context.WorkspaceSDLC;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

final class SDLCFetcher implements SDLCVisitor<PureModelContextData>
{
    private final Span parentSpan;
    private final String clientVersion;
    private final Function<Identity, CloseableHttpClient> httpClientProvider;
    private final Identity identity;
    private final PureServerLoader pureLoader;
    private final AlloySDLCLoader alloyLoader;
    private final WorkspaceSDLCLoader workspaceLoader;

    public SDLCFetcher(Span parentSpan, String clientVersion, Function<Identity, CloseableHttpClient> httpClientProvider, Identity identity, PureServerLoader pureLoader, AlloySDLCLoader alloyLoader, WorkspaceSDLCLoader workspaceLoader)
    {
        this.parentSpan = parentSpan;
        this.clientVersion = clientVersion;
        this.httpClientProvider = httpClientProvider;
        this.identity = identity;
        this.pureLoader = pureLoader;
        this.alloyLoader = alloyLoader;
        this.workspaceLoader = workspaceLoader;
    }

    @Override
    public PureModelContextData visit(AlloySDLC sdlc)
    {
        parentSpan.setTag("sdlc", "alloy");
        try (Scope ignore = GlobalTracer.get().buildSpan("Request Alloy Metadata").startActive(true))
        {
            PureModelContextData loadedProject = this.alloyLoader.loadAlloyProject(identity, sdlc, clientVersion, this.httpClientProvider);
            loadedProject.origin.sdlcInfo.packageableElementPointers = sdlc.packageableElementPointers;
            List<String> missingPaths = this.alloyLoader.checkAllPathsExist(loadedProject, sdlc);
            if (missingPaths.isEmpty())
            {
                return loadedProject;
            }
            else
            {
                throw new EngineException("The following entities:" + missingPaths + " do not exist in the project data loaded from the metadata server. " +
                        "Please make sure the corresponding Gitlab pipeline for version " + (this.alloyLoader.isLatestRevision(sdlc) ? "latest" : sdlc.version) + " has completed and also metadata server has updated with corresponding entities " +
                        "by confirming the data returned from <a href=\"" + this.alloyLoader.getMetaDataApiUrl(identity, sdlc, clientVersion) + "\"/> this API </a>.");
            }
        }
    }

    @Override
    public PureModelContextData visit(PureSDLC pureSDLC)
    {
        parentSpan.setTag("sdlc", "pure");
        try (Scope ignore = GlobalTracer.get().buildSpan("Request Pure Metadata").startActive(true))
        {
            Subject subject = SubjectTools.getCurrentSubject();

            return ListIterate.injectInto(
                    new PureModelContextData.Builder(),
                    pureSDLC.packageableElementPointers,
                    (builder, pointers) -> builder.withPureModelContextData(this.pureLoader.loadPurePackageableElementPointer(identity, pointers, clientVersion, subject == null ? "" : "?auth=kerberos", pureSDLC.overrideUrl))
            ).distinct().sorted().build();
        }
    }

    @Override
    public PureModelContextData visit(WorkspaceSDLC sdlc)
    {
        parentSpan.setTag("sdlc", "workspace");
        parentSpan.setTag("project", sdlc.project);
        parentSpan.setTag("workspace", sdlc.getWorkspace());
        parentSpan.setTag("isGroupWorkspace", sdlc.isGroupWorkspace);

        try (Scope scope = GlobalTracer.get().buildSpan("Request Workspace Metadata").startActive(true))
        {
            PureModelContextData loadedProject = this.workspaceLoader.loadWorkspace(identity, sdlc, this.httpClientProvider);
            PureModelContextData sdlcDependenciesPMCD = this.workspaceLoader.getSDLCDependenciesPMCD(identity, this.clientVersion, sdlc, this.httpClientProvider);
            return loadedProject.combine(sdlcDependenciesPMCD);
        }
    }
}
