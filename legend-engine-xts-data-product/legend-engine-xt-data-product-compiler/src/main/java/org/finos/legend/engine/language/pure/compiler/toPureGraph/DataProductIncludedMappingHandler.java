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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.IncludedMappingHandler;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MappingInclude_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataProduct_DataProduct;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

public class DataProductIncludedMappingHandler implements IncludedMappingHandler
{
    @Override
    public Mapping resolveMapping(MappingInclude mappingInclude, CompileContext context)
    {
        Root_meta_pure_metamodel_dataProduct_DataProduct dataProduct = (Root_meta_pure_metamodel_dataProduct_DataProduct) context.pureModel.getPackageableElement(mappingInclude.getFullName());
        return dataProduct._defaultExecutionContext()._mapping();
    }

    @Override
    public org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude processMappingInclude(MappingInclude mappingInclude, CompileContext context, Mapping parentMapping, Mapping includedMapping)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude include = new Root_meta_pure_mapping_MappingInclude_Impl("", null, context.pureModel.getClass("meta::pure::mapping::MappingInclude"));
        include._owner(parentMapping);
        include._included(includedMapping);
        return include;
    }
}
