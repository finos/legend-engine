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
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.AbstractPropertyBaseVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.BooleanProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.ByteNumberProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.CorePropertyBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.DateProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.DocValuesPropertyBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.DoubleNumberProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.FloatNumberProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.HalfFloatNumberProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.IntegerNumberProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.KeywordProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.LongNumberProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.NestedProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.NumberPropertyBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.ObjectProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.PropertyBase;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.ShortNumberProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.StandardNumberProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.TextProperty;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_BooleanProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_ByteNumberProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_CorePropertyBase;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_DateProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_DocValuesPropertyBase;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_DoubleNumberProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_FloatNumberProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_HalfFloatNumberProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_IntegerNumberProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_KeywordProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_LongNumberProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_NestedProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_NumberPropertyBase;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_ObjectProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_PropertyBase;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_ShortNumberProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_StandardNumberProperty;
import org.finos.legend.pure.generated.Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_TextProperty_Impl;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

public class HelperElasticsearchPropertyBuilder extends AbstractPropertyBaseVisitor<Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property>
{
    private final CompileContext context;
    private final LiteralOrExpressionCompiler literalOrExpressionCompiler;
    private Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property property;
    private String field;

    public HelperElasticsearchPropertyBuilder(CompileContext context)
    {
        this.context = context;
        this.literalOrExpressionCompiler = new LiteralOrExpressionCompiler(context);
    }

    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property getBuiltProperty()
    {
        return property;
    }

    private Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(PropertyBase val)
    {
        this.getPropertyBase()
                ._meta(new PureMap(val.meta))
                ._properties(new PureMap(MapIterate.collectValues(val.properties, (k, v) -> ((PropertyBase) v.unionValue()).accept(new HelperElasticsearchPropertyBuilder(this.context)))))
                ._ignore_above(literalOrExpressionCompiler.compileLong(val.ignore_above))
                ._dynamic(Optional.ofNullable(val.dynamic).map(x -> this.context.pureModel.getEnumValue("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::DynamicMapping", x.name())).orElse(null))
                ._fields(new PureMap(MapIterate.collectValues(val.fields, (k, v) -> ((PropertyBase) v.unionValue()).accept(new HelperElasticsearchPropertyBuilder(this.context)))));

        return this.property;
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(CorePropertyBase val)
    {
        this.<Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_CorePropertyBase>getPropertyBase()
                ._copy_to(literalOrExpressionCompiler.compileString(val.copy_to))
                ._similarity(literalOrExpressionCompiler.compileString(val.similarity))
                ._store(literalOrExpressionCompiler.compileBoolean(val.store));
        return this.visit((PropertyBase) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(NumberPropertyBase val)
    {
        this.<Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_NumberPropertyBase>getPropertyBase()
                ._index(literalOrExpressionCompiler.compileBoolean(val.index))
                ._ignore_malformed(literalOrExpressionCompiler.compileBoolean(val.ignore_malformed));
        return this.visit((DocValuesPropertyBase) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(StandardNumberProperty val)
    {
        this.<Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_StandardNumberProperty>getPropertyBase()
                ._coerce(literalOrExpressionCompiler.compileBoolean(val.coerce))
        // ._on_script_error(val.on_script_error)
        // ._script(val.script)
        ;
        return this.visit((NumberPropertyBase) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(DocValuesPropertyBase val)
    {
        this.<Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_DocValuesPropertyBase>getPropertyBase()._doc_values(literalOrExpressionCompiler.compileBoolean(val.doc_values));
        return this.visit((CorePropertyBase) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property defaultValue(PropertyBase val)
    {
        throw new UnsupportedOperationException("not supported at the moment: " + val.getClass().getCanonicalName());
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(KeywordProperty val)
    {
        this.setPropertyBase("keyword", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_KeywordProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::KeywordProperty"))
                .__pure_protocol_type("keywordProperty")
                ._boost(literalOrExpressionCompiler.compileNumber(val.boost))
                ._eager_global_ordinals(literalOrExpressionCompiler.compileBoolean(val.eager_global_ordinals))
                ._index(literalOrExpressionCompiler.compileBoolean(val.index))
                ._normalizer(literalOrExpressionCompiler.compileString(val.normalizer))
                ._norms(literalOrExpressionCompiler.compileBoolean(val.norms))
                ._null_value(literalOrExpressionCompiler.compileString(val.null_value))
                ._split_queries_on_whitespace(literalOrExpressionCompiler.compileBoolean(val.split_queries_on_whitespace))
                ._index_options(Optional.ofNullable(val.index_options).map(x -> this.context.pureModel.getEnumValue("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::IndexOptions", x.name())).orElse(null))
        );

        return this.visit((DocValuesPropertyBase) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(BooleanProperty val)
    {
        setPropertyBase("_boolean",
                new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_BooleanProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::BooleanProperty"))
                        .__pure_protocol_type("booleanProperty")
                        ._boost(literalOrExpressionCompiler.compileNumber(val.boost))
                        ._index(literalOrExpressionCompiler.compileBoolean(val.index))
                        ._null_value(literalOrExpressionCompiler.compileBoolean(val.null_value))
                // ._fielddata(val.fielddata)
        );

        return this.visit((DocValuesPropertyBase) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(DateProperty val)
    {
        setPropertyBase("date", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_DateProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::DateProperty"))
                        .__pure_protocol_type("dateProperty")
                        ._null_value(literalOrExpressionCompiler.compileString(val.null_value))
                        ._boost(literalOrExpressionCompiler.compileNumber(val.boost))
                        ._format(literalOrExpressionCompiler.compileString(val.format))
                        ._ignore_malformed(literalOrExpressionCompiler.compileBoolean(val.ignore_malformed))
                        ._index(literalOrExpressionCompiler.compileBoolean(val.index))
                        ._precision_step(literalOrExpressionCompiler.compileLong(val.precision_step))
                        ._locale(literalOrExpressionCompiler.compileString(val.locale))
                // ._fielddata(val.fielddata)
        );

        return this.visit((DocValuesPropertyBase) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(ByteNumberProperty val)
    {
        setPropertyBase("_byte", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_ByteNumberProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::ByteNumberProperty"))
                .__pure_protocol_type("byteNumberProperty")
                ._null_value(literalOrExpressionCompiler.compileNumber(val.null_value)));

        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(FloatNumberProperty val)
    {
        setPropertyBase("_float", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_FloatNumberProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::FloatNumberProperty"))
                .__pure_protocol_type("floatNumberProperty")
                ._null_value(literalOrExpressionCompiler.compileNumber(val.null_value)));

        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(HalfFloatNumberProperty val)
    {
        setPropertyBase("half_float", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_HalfFloatNumberProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::HalfFloatNumberProperty"))
                .__pure_protocol_type("halfFloatNumberProperty")
                ._null_value(literalOrExpressionCompiler.compileNumber(val.null_value)));

        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(IntegerNumberProperty val)
    {
        setPropertyBase("integer", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_IntegerNumberProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::IntegerNumberProperty"))
                .__pure_protocol_type("integerNumberProperty")
                ._null_value(literalOrExpressionCompiler.compileLong(val.null_value)));

        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(LongNumberProperty val)
    {
        setPropertyBase("_long", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_LongNumberProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::LongNumberProperty"))
                .__pure_protocol_type("longNumberProperty")
                ._null_value(literalOrExpressionCompiler.compileLong(val.null_value)));

        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(ShortNumberProperty val)
    {
        setPropertyBase("_short", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_ShortNumberProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::ShortNumberProperty"))
                .__pure_protocol_type("shortNumberProperty")
                ._null_value(literalOrExpressionCompiler.compileNumber(val.null_value)));

        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(DoubleNumberProperty val)
    {
        setPropertyBase("_double", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_DoubleNumberProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::DoubleNumberProperty"))
                .__pure_protocol_type("doubleNumberProperty")
                ._null_value(literalOrExpressionCompiler.compileNumber(val.null_value)));

        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(TextProperty val)
    {
        setPropertyBase("text", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_TextProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::TextProperty"))
                        .__pure_protocol_type("textProperty")
                        ._analyzer(literalOrExpressionCompiler.compileString(val.analyzer))
                        ._boost(literalOrExpressionCompiler.compileNumber(val.boost))
                        ._eager_global_ordinals(literalOrExpressionCompiler.compileBoolean(val.eager_global_ordinals))
//                        ._fielddata_frequency_filter(val.fielddata_frequency_filter)
                        ._index(literalOrExpressionCompiler.compileBoolean(val.index))
                        ._index_options(Optional.ofNullable(val.index_options).map(x -> this.context.pureModel.getEnumValue("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::IndexOptions", x.name())).orElse(null))
                        ._index_phrases(literalOrExpressionCompiler.compileBoolean(val.index_phrases))
//                        ._index_prefixes(val.index_prefixes)
                        ._norms(literalOrExpressionCompiler.compileBoolean(val.norms))
                        ._position_increment_gap(literalOrExpressionCompiler.compileLong(val.position_increment_gap))
                        ._search_analyzer(literalOrExpressionCompiler.compileString(val.search_analyzer))
                        ._search_quote_analyzer(literalOrExpressionCompiler.compileString(val.search_quote_analyzer))
                        ._term_vector(Optional.ofNullable(val.term_vector).map(x -> this.context.pureModel.getEnumValue("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::TermVectorOption", x.name())).orElse(null))
        );

        return this.visit((CorePropertyBase) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(ObjectProperty val)
    {
        setPropertyBase("object", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_ObjectProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::ObjectProperty"))
                .__pure_protocol_type("objectProperty"));

        return this.visit((CorePropertyBase) val);
    }

    @Override
    public Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property visit(NestedProperty val)
    {
        setPropertyBase("nested", new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_NestedProperty_Impl("", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::NestedProperty"))
                .__pure_protocol_type("nestedProperty")
                ._include_in_parent(literalOrExpressionCompiler.compileBoolean(val.include_in_parent))
                ._include_in_root(literalOrExpressionCompiler.compileBoolean(val.include_in_root)));

        return this.visit((CorePropertyBase) val);
    }

    private void setPropertyBase(String field, Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_PropertyBase propertyBase)
    {
        this.property = new Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property_Impl("property", null, this.context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::specification::types::mapping::Property"));
        this.field = field;
        Instance.setValueForProperty(this.property, field, propertyBase, this.context.pureModel.getExecutionSupport().getProcessorSupport());
    }

    private <T extends Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_PropertyBase> T getPropertyBase()
    {
        return (T) Instance.getValueForMetaPropertyToOneResolved(this.property, field, this.context.pureModel.getExecutionSupport().getProcessorSupport());
    }
}
