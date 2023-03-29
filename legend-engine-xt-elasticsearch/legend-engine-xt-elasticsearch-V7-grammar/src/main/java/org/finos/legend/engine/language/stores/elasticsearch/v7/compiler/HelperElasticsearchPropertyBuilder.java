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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.compiler;

import java.util.Optional;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.*;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

public class HelperElasticsearchPropertyBuilder extends AbstractPropertyBaseVisitor<Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property>
{
    private final CompileContext context;

    public HelperElasticsearchPropertyBuilder(CompileContext context)
    {
        this.context = context;
    }

    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property newProperty()
    {
        return new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property_Impl("property", null, this.context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::Property"));
    }

    private <V extends PropertyBase, T extends Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_PropertyBase> T buildPropertyBase(V val, T prop)
    {
        return (T) prop
                ._meta(new PureMap(val.meta))
                ._properties(new PureMap(MapIterate.collectValues(val.properties, (k, v) -> ((PropertyBase) v.unionValue()).accept(this))))
                ._ignore_above(val.ignore_above)
                ._dynamic(Optional.ofNullable(val.dynamic).map(x -> this.context.pureModel.getEnumValue("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::DynamicMapping", x.name())).orElse(null))
                ._fields(new PureMap(MapIterate.collectValues(val.fields, (k, v) -> ((PropertyBase) v.unionValue()).accept(this))));
    }

    private <V extends CorePropertyBase, T extends Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_CorePropertyBase> T buildCorePropertyBase(V val, T prop)
    {
        return (T) buildPropertyBase(val, prop._copy_to(Lists.adapt(val.copy_to))._similarity(val.similarity)._store(val.store));
    }

//    private <V extends NumberPropertyBase, T extends Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_NumberPropertyBase> T buildNumberPropertyBase(V val, T prop)
//    {
//        return (T) buildDocValuePropertyBase(val, prop.);
//    }

//    private <V extends StandardNumberProperty, T extends Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_StandardNumberProperty> T buildStandardNumberProperty(V val, T prop)
//    {
//        return (T) buildNumberPropertyBase(val, prop.);
//    }

    private <V extends DocValuesPropertyBase, T extends Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_DocValuesPropertyBase> T buildDocValuePropertyBase(V val, T prop)
    {
        return (T) buildCorePropertyBase(val, prop._doc_values(val.doc_values));
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property defaultValue(PropertyBase val)
    {
        throw new UnsupportedOperationException("not supported at the moment: " + val.getClass().getCanonicalName());
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(KeywordProperty val)
    {
        Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_KeywordProperty prop = buildDocValuePropertyBase(
                val,
                new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_KeywordProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::KeywordProperty"))
                        ._boost(val.boost)
                        ._eager_global_ordinals(val.eager_global_ordinals)
                        ._index(val.index)
                        ._normalizer(val.normalizer)
                        ._norms(val.norms)
                        ._null_value(val.null_value)
                        ._split_queries_on_whitespace(val.split_queries_on_whitespace)
                        ._index_options(Optional.ofNullable(val.index_options).map(x -> this.context.pureModel.getEnumValue("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::IndexOptions", x.name())).orElse(null))
        );

        return newProperty()
                ._keyword(prop);
    }
}
