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

package org.finos.legend.pure.runtime.java.extension.external.json.shared;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.ConversionContext;
import org.finos.legend.pure.runtime.java.extension.external.shared.conversion.UnitConversion;

public class JsonUnitSerialization<T extends CoreInstance> extends UnitConversion<T, Object>
{
    public JsonUnitSerialization(CoreInstance type)
    {
        super(type);
    }

    @Override
    public Object apply(T pureObject, ConversionContext context)
    {
        CoreInstance processedPureObject = pureObject;
        Object value;
        String typeString;
        ProcessorSupport processorSupport = context.getProcessorSupport();

        if (null == pureObject.getValueForMetaPropertyToOne("genericType"))
        {
            processedPureObject = ValueSpecificationBootstrap.wrapValueSpecification(pureObject, false, context.getProcessorSupport());
            value = Long.valueOf(processedPureObject.getValueForMetaPropertyToOne("values").getValueForMetaPropertyToOne("values").getName());
            try
            {
                typeString = ((String) pureObject.getClass().getMethod("getFullSystemPath").invoke(pureObject)).substring(6);
            }
            catch (Exception e)
            {
                throw new PureExecutionException("Cannot find full path name for the designated unit type.");
            }
        }
        else
        {
            CoreInstance numericInArray = processedPureObject.getValueForMetaPropertyToOne("values");
            value = numericInArray instanceof PrimitiveCoreInstance ? ((PrimitiveCoreInstance) numericInArray).getValue() : ((PrimitiveCoreInstance) numericInArray.getValueForMetaPropertyToOne("values")).getValue();
            typeString = PackageableElement.getUserPathForPackageableElement(Instance.getValueForMetaPropertyToOneResolved(processedPureObject, M3Properties.genericType, M3Properties.rawType, processorSupport));
        }

        return Maps.mutable.with(
                this.unitKeyName, Lists.mutable.<Object>with(Maps.mutable.with("unitId", typeString, "exponentValue", 1)),
                this.valueKeyName, value);
    }
}
