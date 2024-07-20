// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.language.pure.code.completer.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.code.completer.Completer;
import org.finos.legend.engine.language.pure.code.completer.CompleterExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Code Completion")
@Path("pure/v1/codeCompletion")
@Produces(MediaType.APPLICATION_JSON)
public class CompleteCode
{
    private final ModelManager modelManager;
    private final List<CompleterExtension> completerExtensions;

    public CompleteCode(ModelManager modelManager)
    {
        this(modelManager, StreamSupport.stream(ServiceLoader.load(CompleterExtension.class).spliterator(), false).collect(Collectors.toList()));
    }

    public CompleteCode(ModelManager modelManager, List<CompleterExtension> completerExtensions)
    {
        this.modelManager = modelManager;
        this.completerExtensions = completerExtensions;
    }

    @POST
    @Path("completeCode")
    @ApiOperation(value = "Returns code completion result for the given code block")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response completeCode(@Context HttpServletRequest request, CompleteCodeInput completeCodeInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm, @Context UriInfo uriInfo)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            if (completeCodeInput.offset != null && completeCodeInput.offset != -1)
            {
                throw new UnsupportedOperationException("Code completion with offset not yet supported (offset should be either -1 or null)");
            }
            Completer codeCompleter = new Completer(getModelText(completeCodeInput.model, identity), this.completerExtensions);
            return Response.ok(new CodeCompletionResult(codeCompleter.complete(completeCodeInput.codeBlock))).build();
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CODE_COMPLETION_ERROR, identity.getName());
        }
    }

    private String getModelText(PureModelContext model, Identity identity)
    {
        if (model instanceof PureModelContextText)
        {
            return ((PureModelContextText) model).code;
        }
        else
        {
            PureModelContextData pureModelContextData = modelManager.loadData(model, model instanceof PureModelContextPointer ? ((PureModelContextPointer) model).serializer.version : null, identity);
            return PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build()).renderPureModelContextData(pureModelContextData);
        }
    }
}
