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

package org.finos.legend.engine.language.pure.dsl.persistence.cloud.compiler.toPureGraph;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.cloud.context.AwsGluePersistencePlatform;
import org.finos.legend.pure.generated.Root_meta_external_persistence_aws_metamodel_AwsGluePersistencePlatform;
import org.finos.legend.pure.generated.Root_meta_external_persistence_aws_metamodel_AwsGluePersistencePlatform_Impl;

public class HelperPersistenceCloudBuilder
{
    private HelperPersistenceCloudBuilder()
    {
    }

    public static Root_meta_external_persistence_aws_metamodel_AwsGluePersistencePlatform buildAwsGluePersistencePlatform(AwsGluePersistencePlatform persistencePlatform, CompileContext context)
    {
        return new Root_meta_external_persistence_aws_metamodel_AwsGluePersistencePlatform_Impl("", null, context.pureModel.getClass("meta::external::persistence::aws::metamodel::AwsGluePersistencePlatform"))
                ._dataProcessingUnits(persistencePlatform.dataProcessingUnits);
    }
}
