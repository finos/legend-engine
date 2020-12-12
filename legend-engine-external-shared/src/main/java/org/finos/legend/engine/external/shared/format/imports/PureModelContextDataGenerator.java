// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.external.shared.format.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Domain;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.core_pure_extensions_extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import java.lang.reflect.Method;
import java.util.List;

public class PureModelContextDataGenerator
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final MutableMap<String, Method> classPlanTransforms = Maps.mutable.empty();
    private static final MutableMap<String, Method> enumPlanTransforms = Maps.mutable.empty();
    private static final MutableMap<String, Method> functionPlanTransforms = Maps.mutable.empty();
    private static final MutableMap<String, Method> classAppliedFunctionTransforms = Maps.mutable.empty();

    public static java.lang.Class<?> loadTransClass(String pureVersion) throws ClassNotFoundException
    {
        try
        {
            if ("vX_X_X".equals(pureVersion))
            {
                throw new RuntimeException("Not possible");
            }
            return java.lang.Class.forName("org.finos.legend.pure.generated.system_protocols_pure_" + (pureVersion.equals("vX_X_X") ? "vX_X_X_transfers_other" : ("versions_" + pureVersion + "_" + pureVersion + "_trans")));
        }
        catch (ClassNotFoundException e)
        {
            return java.lang.Class.forName("org.finos.legend.pure.generated.protocols_versions_" + pureVersion + "_" + pureVersion + "_trans");
        }
    }

    public static PureModelContextData generatePureModelContextDataFromClasses(RichIterable<? extends PackageableElement> pureClasses, String pureVersion, CompiledExecutionSupport compiledExecutionSupport) throws RuntimeException
    {
        Method transformMethod = classPlanTransforms.getIfAbsentPut(pureVersion, () -> {
            try
            {
                if ("vX_X_X".equals(pureVersion))
                {
                    return java.lang.Class.forName("org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_metamodel").getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformClass_Class_1__RouterExtension_MANY__Class_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class.class, RichIterable.class, ExecutionSupport.class);
                }
                else
                {
                    return loadTransClass(pureVersion).getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformClass_Class_1__Class_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class.class, ExecutionSupport.class);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> protocolClasses = Lists.mutable.empty();

        pureClasses.toList().forEach(aClass -> {
            try
            {
                protocolClasses.add(objectMapper.readValue(org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_("vX_X_X".equals(pureVersion) ? transformMethod.invoke(null, aClass, core_pure_extensions_extension.Root_meta_pure_router_extension_defaultExtensions__RouterExtension_MANY_(compiledExecutionSupport), compiledExecutionSupport) : transformMethod.invoke(null, aClass, compiledExecutionSupport), compiledExecutionSupport), Class.class));
            }
            catch (Exception e)
            {
            }
        });

        return PureModelContextData.newPureModelContextData(null, null, protocolClasses);
    }

    public static PureModelContextData generatePureModelContextDataFromClassesWithAppliedFunctions(RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<? extends Object>> pureClasses, String pureVersion, CompiledExecutionSupport compiledExecutionSupport) throws RuntimeException
    {
        Method transformMethod = classAppliedFunctionTransforms.getIfAbsentPut(pureVersion, () -> {
            try
            {
                if ("vX_X_X".equals(pureVersion))
                {
                    return java.lang.Class.forName("org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_metamodel").getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformClass_Class_1__Boolean_1__RouterExtension_MANY__Class_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class.class, boolean.class, RichIterable.class, ExecutionSupport.class);
                }
                else
                {
                    return loadTransClass(pureVersion).getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformClass_Class_1__Boolean_1__Class_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class.class, boolean.class, ExecutionSupport.class);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> result = pureClasses.collect(f -> {
            try
            {
                Object functionDef = "vX_X_X".equals(pureVersion) ? transformMethod.invoke(null, f, true, core_pure_extensions_extension.Root_meta_pure_router_extension_defaultExtensions__RouterExtension_MANY_(compiledExecutionSupport), compiledExecutionSupport) : transformMethod.invoke(null, f, true, compiledExecutionSupport);
                return (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement) objectMapper.readValue(org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(functionDef, compiledExecutionSupport), Class.class);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }).toList();
        return PureModelContextData.newPureModelContextData(null, null, result);
    }

    public static PureModelContextData generatePureModelContextDataFromEnumerations(RichIterable<? extends PackageableElement> pureEnums, String pureVersion, CompiledExecutionSupport compiledExecutionSupport) throws RuntimeException
    {
        Method transformMethod = enumPlanTransforms.getIfAbsentPut(pureVersion, () -> {
            try
            {
                if ("vX_X_X".equals(pureVersion))
                {
                    return java.lang.Class.forName("org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_metamodel").getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformEnum_Enumeration_1__Enumeration_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration.class, ExecutionSupport.class);
                }
                else
                {
                    return loadTransClass(pureVersion).getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformEnum_Enumeration_1__Enumeration_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration.class, ExecutionSupport.class);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> protocolEnums = Lists.mutable.empty();
        pureEnums.toList().forEach(aEnum -> {
            try
            {
                protocolEnums.add(objectMapper.readValue(org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(transformMethod.invoke(null, aEnum, compiledExecutionSupport), compiledExecutionSupport), Enumeration.class));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        return PureModelContextData.newPureModelContextData(null, null, protocolEnums);
    }

    public static PureModelContextData generatePureModelContextDataFromFunctions(RichIterable<FunctionDefinition<? extends Object>> pureFunctions, String pureVersion, CompiledExecutionSupport compiledExecutionSupport) throws RuntimeException
    {
        Method transformMethod = functionPlanTransforms.getIfAbsentPut(pureVersion, () -> {
            try
            {
                if ("vX_X_X".equals(pureVersion))
                {
                    return java.lang.Class.forName("org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_metamodel").getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_transformFunction_FunctionDefinition_1__RouterExtension_MANY__Function_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition.class, RichIterable.class, ExecutionSupport.class);
                }
                else
                {
                    return loadTransClass(pureVersion).getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_transformFunction_FunctionDefinition_1__Function_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition.class, ExecutionSupport.class);
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement> result = pureFunctions.collect(f -> {
            try
            {
                Object functionDef = "vX_X_X".equals(pureVersion)?transformMethod.invoke(null, f, core_pure_extensions_extension.Root_meta_pure_router_extension_defaultExtensions__RouterExtension_MANY_(compiledExecutionSupport), compiledExecutionSupport):transformMethod.invoke(null, f, compiledExecutionSupport);
                return (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement) objectMapper.readValue(org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(functionDef, compiledExecutionSupport), Function.class);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }).toList();
        return PureModelContextData.newPureModelContextData(null, null, result);
    }
}
