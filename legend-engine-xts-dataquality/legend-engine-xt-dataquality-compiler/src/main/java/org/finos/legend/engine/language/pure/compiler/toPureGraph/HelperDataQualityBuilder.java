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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;

import java.util.Objects;

public class HelperDataQualityBuilder
{
    private static DataQualityCompilerExtension getDataQualityCompilerExtension(CompileContext context)
    {
        return Objects.requireNonNull(ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), DataQualityCompilerExtension.class).getAny(), "DataQuality extension is not in scope");
    }

    public static Root_meta_external_dataquality_DataQuality<Object> getDataQuality(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        Root_meta_external_dataquality_DataQuality<Object> dataQuality = (Root_meta_external_dataquality_DataQuality<Object>) context.pureModel.getPackageableElement_safe(fullPath);
        Assert.assertTrue(dataQuality != null, () -> "Can't find dataquality '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return dataQuality;
    }

    public static Root_meta_external_dataquality_DataQuality<Object> resolveDataQualityValidation(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        return context.resolve(fullPath, sourceInformation, path -> getDataQuality(path, sourceInformation, context));
    }

}

