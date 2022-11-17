package org.finos.legend.engine.persistence.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.ServiceLoader;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.persistence.platform.PlatformActionsExtension;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

@Api(tags = "Persistence")
@Path("persistence/platform/v1")
@Produces(MediaType.APPLICATION_JSON)
public class PersistencePlatformActions
{
    private final ModelManager modelManager;
    private final List<PlatformActionsExtension> extensions = Lists.mutable.empty();

    public PersistencePlatformActions(ModelManager modelManager)
    {
        this.modelManager = modelManager;
        ServiceLoader.load(PlatformActionsExtension.class).forEach(this.extensions::add);
    }

    @POST
    @Path("validate")
    @ApiOperation(value = "Checks that the given persistence context contains a valid platform")
    public void validate(PersistencePlatformActionRequest request, @Pac4JProfileManager @ApiParam(hidden = true) ProfileManager<CommonProfile> pm)
    {
        this.action(request, pm, PlatformActionsExtension::validate);
    }

    @POST
    @Path("install")
    @ApiOperation(value = "Install the given persistence context using its defined platform")
    public void install(PersistencePlatformActionRequest request, @Pac4JProfileManager @ApiParam(hidden = true) ProfileManager<CommonProfile> pm)
    {
        this.validate(request, pm);
        this.action(request, pm, PlatformActionsExtension::install);
    }

    private void action(PersistencePlatformActionRequest request, ProfileManager<CommonProfile> pm, Procedure3<PlatformActionsExtension, Root_meta_pure_persistence_metamodel_PersistenceContext, PureModel> action)
    {
        MutableList<CommonProfile> profile = pm.get(true).map(x -> Lists.mutable.of(x)).orElse(null);
        Pair<PureModelContextData, PureModel> pureModelContextDataPureModelPair = this.modelManager.loadModelAndData(request.model, request.clientVersion, profile, null);
        PureModel model = pureModelContextDataPureModelPair.getTwo();
        PackageableElement packageableElement = model.getPackageableElement(request.persistenceContext);
        Assert.assertTrue(packageableElement instanceof Root_meta_pure_persistence_metamodel_PersistenceContext, () -> "Provided element is not of type PersistenceContext: " + request.persistenceContext);
        Root_meta_pure_persistence_metamodel_PersistenceContext persistenceContext = (Root_meta_pure_persistence_metamodel_PersistenceContext) packageableElement;
        PlatformActionsExtension platformActionsExtension = this.extensions.stream().filter(x -> x.platformType().isInstance(persistenceContext._platform())).findFirst().orElse(null);
        Assert.assertTrue(platformActionsExtension != null, () -> "No PlatformActionsExtension found for context platform type: " + persistenceContext._platform().getClass().getName());

        action.value(platformActionsExtension, persistenceContext, model);
    }
}
