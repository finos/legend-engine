// Copyright 2022 Goldman Sachs
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


package org.finos.legend.engine.analytics.mapping.model.coverage.api;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.platform_pure_corefunctions_multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.finos.legend.pure.generated.core_analytics_mapping_modelCoverageAnalytics;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_MappedEntity;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_MappedProperty;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_EntityMappedProperty;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_EnumMappedProperty;

import java.util.List;


@Api(tags = "Analytics - Model")
@Path("pure/v1/analytics/mapping")
public class ModelCoverageAnalytics
{
    private final ModelManager modelManager;

    public ModelCoverageAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("modelCoverage")
    @ApiOperation(value = "Analyze the input pure mapping to generate information about mapped classes and mapped properties of each class")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response doModelCoverageAnalytics(ModelCoverageAnalyticsInput input,
                                             @QueryParam("returnMappedEntityInfo") @DefaultValue("true") boolean returnMappedEntityInfo,
                                             @QueryParam("returnMappedPropertyInfo") @DefaultValue("true") boolean returnMappedPropertyInfo,
                                             @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        PureModel pureModel = this.modelManager.loadModel(input.model, input.clientVersion, profiles, null);
        Mapping mapping = input.mapping == null ? null : pureModel.getMapping(input.mapping);
        try (Scope scope = GlobalTracer.get().buildSpan("Mapping: analysis").startActive(true))
        {
            try
            {
                RichIterable<? extends Root_meta_analytics_mapping_modelCoverage_MappedEntity> mappedEntities = core_analytics_mapping_modelCoverageAnalytics.Root_meta_analytics_mapping_modelCoverage_doModelCoverageAnalytics_Mapping_1__MappedEntity_MANY_(mapping, pureModel.getExecutionSupport());
                return ManageConstantResult.manageResult(profiles, new ModelCoverageAnalyticsResult(mappedEntities.collect(e -> buildMappedEntity(e, returnMappedEntityInfo, returnMappedPropertyInfo, pureModel.getExecutionSupport())).toList()));
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_MAPPING_MODEL_COVERAGE_ERROR, Response.Status.BAD_REQUEST, profiles);
            }
        }
    }

    private MappedEntity buildMappedEntity(Root_meta_analytics_mapping_modelCoverage_MappedEntity mappedEntity, boolean returnMappedEntityInfo, boolean returnMappedPropertyInfo, CompiledExecutionSupport es)
    {
        List<MappedProperty> mappedProperties = mappedEntity._properties().collect(p -> buildMappedProperty(p, returnMappedPropertyInfo, es)).toList();
        MappedEntity result = new MappedEntity(mappedEntity._path(), mappedProperties);
        if (returnMappedEntityInfo)
        {
            if (mappedEntity._info() != null)
            {
                if (mappedEntity._info()._subClasses() != null)
                {
                    List<String> subClasses = mappedEntity._info()._subClasses().collect((String p) -> p).toList();
                    if (mappedEntity._info()._isRootEntity() != null)
                    {
                        result.info = new MappedEntityInfo(mappedEntity._info()._isRootEntity(), subClasses);
                    }
                    else
                    {
                        result.info = new MappedEntityInfo(subClasses);
                    }
                }
                else if (mappedEntity._info()._isRootEntity() != null)
                {
                    result.info = new MappedEntityInfo(mappedEntity._info()._isRootEntity());
                }
            }
        }
        return result;
    }

    private MappedProperty buildMappedProperty(Root_meta_analytics_mapping_modelCoverage_MappedProperty mappedProperty, boolean returnMappedPropertyInfo, CompiledExecutionSupport es)
    {
        MappedProperty result;
        if (mappedProperty instanceof Root_meta_analytics_mapping_modelCoverage_EntityMappedProperty)
        {
            result = new EntityMappedProperty(mappedProperty._name(), ((Root_meta_analytics_mapping_modelCoverage_EntityMappedProperty) mappedProperty)._entityPath(), ((Root_meta_analytics_mapping_modelCoverage_EntityMappedProperty) mappedProperty)._subType());
        }
        else if (mappedProperty instanceof Root_meta_analytics_mapping_modelCoverage_EnumMappedProperty)
        {
            result = new EnumMappedProperty(mappedProperty._name(), ((Root_meta_analytics_mapping_modelCoverage_EnumMappedProperty) mappedProperty)._enumPath());
        }
        else
        {
           result = new MappedProperty(mappedProperty._name());
        }
        if (returnMappedPropertyInfo && mappedProperty._info() != null)
        {
            if (mappedProperty._info()._multiplicity() != null)
            {
                Multiplicity multiplicity = new Multiplicity(
                        (int) platform_pure_corefunctions_multiplicity.Root_meta_pure_functions_multiplicity_getLowerBound_Multiplicity_1__Integer_1_(
                                mappedProperty._info()._multiplicity().getFirst(), es),
                        platform_pure_corefunctions_multiplicity.Root_meta_pure_functions_multiplicity_hasUpperBound_Multiplicity_1__Boolean_1_(
                                mappedProperty._info()._multiplicity().getFirst(), es) ?
                                (int) platform_pure_corefunctions_multiplicity.Root_meta_pure_functions_multiplicity_getUpperBound_Multiplicity_1__Integer_1_(
                                        mappedProperty._info()._multiplicity().getFirst(), es) : Integer.MAX_VALUE);
                if (mappedProperty._info()._type() != null)
                {
                    result.mappedPropertyInfo = new MappedPropertyInfo(multiplicity, MappedPropertyType.valueOf(mappedProperty._info()._type().getFirst()._name()));
                }
                else
                {
                    result.mappedPropertyInfo = new MappedPropertyInfo(multiplicity);
                }
            }
            else if (mappedProperty._info()._type() != null)
            {
                result.mappedPropertyInfo = new MappedPropertyInfo(MappedPropertyType.valueOf(mappedProperty._info()._type().getFirst()._name()));
            }
        }
        return result;
    }
}
