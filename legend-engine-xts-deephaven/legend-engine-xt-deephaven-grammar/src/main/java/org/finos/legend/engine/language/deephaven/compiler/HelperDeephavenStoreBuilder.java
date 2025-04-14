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
//

package org.finos.legend.engine.language.deephaven.compiler;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.language.pure.dsl.authentication.compiler.toPureGraph.HelperAuthenticationBuilder;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenSourceSpecification;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.DeephavenStore;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Table;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Column;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.*;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Column;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_DeephavenStore;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_DeephavenStore_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Table;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Table_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_store_Column_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_Type;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_BooleanType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_BooleanType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_ByteType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_ByteType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_CharType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_CharType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_CustomType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_CustomType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_DoubleType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_DoubleType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_FloatType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_FloatType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_IntType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_IntType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_LongType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_LongType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_ShortType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_ShortType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_StringType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_StringType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_DateTimeType;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_type_DateTimeType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_runtime_DeephavenSourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_runtime_DeephavenConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_runtime_DeephavenSourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification;
import org.finos.legend.pure.generated.Root_meta_external_store_deephaven_metamodel_runtime_DeephavenConnection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;

import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;


public class HelperDeephavenStoreBuilder
{
    public static Root_meta_external_store_deephaven_metamodel_store_DeephavenStore buildStoreFirstPass(DeephavenStore srcStore, CompileContext context)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = SourceInformationHelper.toM3SourceInformation(srcStore.sourceInformation);
        MutableList<Root_meta_external_store_deephaven_metamodel_store_Table> tables = srcStore.tables.stream().map(x -> buildTable(x, sourceInformation, context)).collect(Collectors.toCollection(Lists.mutable::empty));
        Class<?> classifier = context.pureModel.getClass("meta::external::store::deephaven::metamodel::store::DeephavenStore");
        Root_meta_external_store_deephaven_metamodel_store_DeephavenStore store = new Root_meta_external_store_deephaven_metamodel_store_DeephavenStore_Impl(srcStore.name, sourceInformation, classifier)
                ._classifierGenericType(context.pureModel.getGenericType(classifier))
                ._tables(tables);
        return store._validate(true, sourceInformation, context.getExecutionSupport());
    }

    public static Root_meta_external_store_deephaven_metamodel_store_Table buildTable(Table srcTable, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CompileContext context)
    {
        MutableList<Root_meta_external_store_deephaven_metamodel_store_Column> columns = srcTable.columns.stream().map(x -> buildColumn(x, sourceInformation, context)).collect(Collectors.toCollection(Lists.mutable::empty));
        Class<?> classifier = context.pureModel.getClass("meta::external::store::deephaven::metamodel::store::Table");
        // TODO - for things that aren't packageable elements we need to add sourceinfo back in future - sourceinfo is only avail for packageable element equivalents in pure
        return new Root_meta_external_store_deephaven_metamodel_store_Table_Impl(srcTable.name, sourceInformation, classifier)
                ._name(srcTable.name)
                ._columns(columns);
    }

    public static Root_meta_external_store_deephaven_metamodel_store_Column buildColumn(Column srcColumn, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CompileContext context)
    {
        Class<?> colClassifier = context.pureModel.getClass("meta::external::store::deephaven::metamodel::store::Column");
        Root_meta_external_store_deephaven_metamodel_type_Type type = srcColumn.type.accept(new DeephavenTypeVisitor(context));
        // TODO - for things that aren't packageable elements we need to add sourceinfo back in future - sourceinfo is only avail for packageable element equivalents in pure
        return new Root_meta_external_store_deephaven_metamodel_store_Column_Impl(srcColumn.name, sourceInformation, colClassifier)
                ._name(srcColumn.name)
                ._type(type);
    }

    private static class DeephavenTypeVisitor implements TypeVisitor<Root_meta_external_store_deephaven_metamodel_type_Type>
    {
        private final CompileContext context;

        private DeephavenTypeVisitor(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_BooleanType visit(BooleanType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_BooleanType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::BooleanType")).__type("booleanType");
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_ByteType visit(ByteType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_ByteType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::ByteType")).__type("byteType");
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_CharType visit(CharType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_CharType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::CharType")).__type("charType");
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_CustomType visit(CustomType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_CustomType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::CustomType"));
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_DoubleType visit(DoubleType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_DoubleType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::DoubleType")).__type("doubleType");
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_FloatType visit(FloatType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_FloatType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::FloatType")).__type("floatType");
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_IntType visit(IntType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_IntType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::IntType")).__type("intType");
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_LongType visit(LongType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_LongType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::LongType")).__type("longType");
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_ShortType visit(ShortType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_ShortType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::ShortType")).__type("shortType");
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_StringType visit(StringType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_StringType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::StringType")).__type("stringType");
        }

        @Override
        public Root_meta_external_store_deephaven_metamodel_type_DateTimeType visit(DateTimeType val)
        {
            return new Root_meta_external_store_deephaven_metamodel_type_DateTimeType_Impl(val.getClass().getName(), null, this.context.pureModel.getClass("meta::external::store::deephaven::metamodel::type::DateTimeType")).__type("dateTimeType");
        }
    }

    public static Root_meta_external_store_deephaven_metamodel_runtime_DeephavenConnection buildConnection(DeephavenConnection srcConn, CompileContext context)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = SourceInformationHelper.toM3SourceInformation(srcConn.sourceInformation);

        Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification authSpec = HelperAuthenticationBuilder.buildAuthenticationSpecification(srcConn.authSpec, context);
        Root_meta_external_store_deephaven_metamodel_runtime_DeephavenSourceSpecification sourceSpec = buildSourceSpecification(srcConn.sourceSpec, context, srcConn.sourceInformation);

        Root_meta_external_store_deephaven_metamodel_runtime_DeephavenConnection conn = new Root_meta_external_store_deephaven_metamodel_runtime_DeephavenConnection_Impl("", sourceInformation, context.pureModel.getClass("meta::external::store::deephaven::metamodel::runtime::DeephavenConnection"))
                ._authSpec(authSpec)
                ._sourceSpec(sourceSpec);

        return conn._validate(true, sourceInformation, context.getExecutionSupport());
    }

    private static Root_meta_external_store_deephaven_metamodel_runtime_DeephavenSourceSpecification buildSourceSpecification(DeephavenSourceSpecification srcSourceSpec, CompileContext context, SourceInformation sourceInformation)
    {
        Assert.assertTrue(srcSourceSpec.url.getPort() > 0, () -> "Deephaven URL is missing port: " + srcSourceSpec.url, sourceInformation, EngineErrorType.COMPILATION);

        Enum scheme = context.pureModel.getEnumValue("meta::pure::functions::io::http::URLScheme", srcSourceSpec.url.getScheme(), SourceInformation.getUnknownSourceInformation(), sourceInformation);

        Root_meta_pure_functions_io_http_URL url = new Root_meta_pure_functions_io_http_URL_Impl(srcSourceSpec.url.toString(), null, context.pureModel.getClass("meta::pure::functions::io::http::URL"))
                ._scheme(scheme)
                ._host(srcSourceSpec.url.getHost())
                ._port(srcSourceSpec.url.getPort())
                ._path(StringUtils.isEmpty(srcSourceSpec.url.getPath()) ? "/" : srcSourceSpec.url.getPath());

        return new Root_meta_external_store_deephaven_metamodel_runtime_DeephavenSourceSpecification_Impl(srcSourceSpec.url.toString() + "_spec", null, context.pureModel.getClass("meta::external::store::deephaven::metamodel::runtime::DeephavenSourceSpecification"))
                ._url(url);
    }
}
