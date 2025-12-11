// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class SingleValueTDS extends TestTDSInterpreted
{
    GenericType genericType;
    CoreInstance value;

    public SingleValueTDS(CoreInstance value, GenericType genericType, ModelRepository repository, ProcessorSupport processorSupport)
    {
        super(repository, processorSupport);
        this.genericType = genericType;
        this.value = value;
    }

    @Override
    public CoreInstance getValueAsCoreInstance(String columnName, int rowNum)
    {
        return ValueSpecificationBootstrap.wrapValueSpecification_ResultGenericTypeIsKnown(Lists.mutable.with(value), genericType, true, processorSupport);
    }

    public Object getValue(String columnName, int rowNum)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getRowCount()
    {
        return 1;
    }
}
