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


package org.finos.legend.engine.persistence.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.persistence.platform.PersistencePlatformActionRequest;
import org.finos.legend.engine.language.pure.dsl.persistence.platform.PersistencePlatformActionsExtension;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Supplier;


@Api(tags = "Persistence")
@Path("persistence/v1/platform")
@Produces(MediaType.APPLICATION_JSON)
public class PersistencePlatformActions
{
    private final Supplier<Identity> systemIdentitySupplier;
    private final ModelManager modelManager;
    private final List<PersistencePlatformActionsExtension> extensions = Lists.mutable.empty();

    public PersistencePlatformActions(Supplier<Identity> systemIdentitySupplier, ModelManager modelManager)
    {
        this.systemIdentitySupplier = systemIdentitySupplier;
        this.modelManager = modelManager;
        ServiceLoader.load(PersistencePlatformActionsExtension.class).forEach(this.extensions::add);
    }

    @POST
    @Path("validate")
    @ApiOperation(value = "Checks that the given persistence context contains a valid platform")
    public void validate(PersistencePlatformActionPayload payload, @Pac4JProfileManager @ApiParam(hidden = true) ProfileManager<CommonProfile> pm)
    {
        this.action(payload, pm, PersistencePlatformActionsExtension::validate);
    }

    @POST
    @Path("install")
    @ApiOperation(value = "Install the given persistence context using its defined platform")
    public void install(PersistencePlatformActionPayload payload, @Pac4JProfileManager @ApiParam(hidden = true) ProfileManager<CommonProfile> pm)
    {
        this.validate(payload, pm);
        this.action(payload, pm, PersistencePlatformActionsExtension::install);
    }

    @POST
    @Path("uninstall")
    @ApiOperation(value = "Uninstall the given persistence context using its defined platform")
    public void uninstall(PersistencePlatformActionPayload payload, @Pac4JProfileManager @ApiParam(hidden = true) ProfileManager<CommonProfile> pm)
    {
        this.action(payload, pm, PersistencePlatformActionsExtension::uninstall);
    }

    private void action(PersistencePlatformActionPayload payload, ProfileManager<CommonProfile> pm, Procedure2<PersistencePlatformActionsExtension, PersistencePlatformActionRequest> action)
    {
        FixedSizeList<CommonProfile> profiles = pm.get(true).map(Lists.fixedSize::of).orElse(null);
        Pair<PureModelContextData, PureModel> pureModelContextDataPureModelPair = this.modelManager.loadModelAndData(payload.model, payload.clientVersion, profiles, null);
        PureModel model = pureModelContextDataPureModelPair.getTwo();
        PackageableElement packageableElement = model.getPackageableElement(payload.persistenceContextPath);
        Assert.assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_PersistenceContext, () -> "Provided element is not of type PersistenceContext: " + payload.persistenceContextPath + " with type: " + packageableElement.getFullSystemPath());
        Root_meta_pure_persistence_metamodel_PersistenceContext persistenceContext = (Root_meta_pure_persistence_metamodel_PersistenceContext) packageableElement;
        PersistencePlatformActionsExtension platformActionsExtension = this.extensions.stream().filter(x -> x.platformType().isInstance(persistenceContext._platform())).findFirst().orElse(null);
        Assert.assertTrue(platformActionsExtension != null, () -> "No PlatformActionsExtension found for context platform type: " + persistenceContext._platform().getClass().getName());

        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        action.value(platformActionsExtension, new PersistencePlatformActionRequest(persistenceContext, pureModelContextDataPureModelPair.getOne(), model, payload.model, identity, this.systemIdentitySupplier.get()));
    }
}
