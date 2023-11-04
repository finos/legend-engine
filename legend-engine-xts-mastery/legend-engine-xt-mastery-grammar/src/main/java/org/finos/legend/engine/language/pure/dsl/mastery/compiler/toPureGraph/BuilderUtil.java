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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_MasterRecordDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import static java.lang.String.format;

public class BuilderUtil
{

    public static Root_meta_legend_service_metamodel_Service buildService(String service, CompileContext context, SourceInformation sourceInformation)
    {
        if (service == null)
        {
            return null;
        }

        String servicePath = service.substring(0, service.lastIndexOf("::"));
        String serviceName = service.substring(service.lastIndexOf("::") + 2);

        PackageableElement packageableElement = context.pureModel.getOrCreatePackage(servicePath)._children().detect(c -> serviceName.equals(c._name()));
        if (packageableElement instanceof Root_meta_legend_service_metamodel_Service)
        {
            return (Root_meta_legend_service_metamodel_Service) packageableElement;
        }
        throw new EngineException(format("Service '%s' is not defined", service), sourceInformation, EngineErrorType.COMPILATION);
    }

    public static Root_meta_pure_mastery_metamodel_MasterRecordDefinition buildMasterRecordDefinition(String masterRecordDefinition, CompileContext context, SourceInformation sourceInformation)
    {
        if (masterRecordDefinition == null)
        {
            return null;
        }

        String masterRecordDefinitionPath = masterRecordDefinition.substring(0, masterRecordDefinition.lastIndexOf("::"));
        String masterRecordDefinitionName = masterRecordDefinition.substring(masterRecordDefinition.lastIndexOf("::") + 2);

        PackageableElement packageableElement = context.pureModel.getOrCreatePackage(masterRecordDefinitionPath)._children().detect(c -> masterRecordDefinitionName.equals(c._name()));
        if (packageableElement instanceof Root_meta_pure_mastery_metamodel_MasterRecordDefinition)
        {
            return (Root_meta_pure_mastery_metamodel_MasterRecordDefinition) packageableElement;
        }
        throw new EngineException(format("MasterRecord definition '%s' is not defined", masterRecordDefinition), sourceInformation, EngineErrorType.COMPILATION);
    }
}
