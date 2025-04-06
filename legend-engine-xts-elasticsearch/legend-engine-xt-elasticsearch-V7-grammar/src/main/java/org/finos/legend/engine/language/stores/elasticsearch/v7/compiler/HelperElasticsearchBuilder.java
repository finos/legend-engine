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

import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.SourceInformationHelper;
import org.finos.legend.engine.language.pure.dsl.authentication.compiler.toPureGraph.HelperAuthenticationBuilder;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.context.Elasticsearch7ExecutionContext;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreURLSourceSpecification;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7Store;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7StoreIndex;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7StoreIndexProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.PropertyBase;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;

public final class HelperElasticsearchBuilder
{
    private HelperElasticsearchBuilder()
    {

    }

    public static Root_meta_external_store_elasticsearch_v7_metamodel_store_Elasticsearch7Store buildStoreFirstPass(Elasticsearch7Store srcStore, CompileContext context)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = SourceInformationHelper.toM3SourceInformation(srcStore.sourceInformation);

        MutableList<Root_meta_external_store_elasticsearch_v7_metamodel_store_Elasticsearch7StoreIndex> indices = srcStore.indices.stream().map(x -> buildIndex(x, sourceInformation, context)).collect(Collectors.toCollection(Lists.mutable::empty));
        Class<?> classifier = context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::store::Elasticsearch7Store");
        Root_meta_external_store_elasticsearch_v7_metamodel_store_Elasticsearch7Store store = new Root_meta_external_store_elasticsearch_v7_metamodel_store_Elasticsearch7Store_Impl(srcStore.name, sourceInformation, classifier)
                ._classifierGenericType(context.pureModel.getGenericType(classifier))
                ._indices(indices);

        return store._validate(true, sourceInformation, context.getExecutionSupport());
    }

    private static Root_meta_external_store_elasticsearch_v7_metamodel_store_Elasticsearch7StoreIndex buildIndex(Elasticsearch7StoreIndex srcIndex, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CompileContext context)
    {
        MutableList<Root_meta_external_store_elasticsearch_v7_metamodel_store_Elasticsearch7StoreIndexProperty> properties = srcIndex.properties.stream().map(x -> buildIndexProperty(x, sourceInformation, context)).collect(Collectors.toCollection(Lists.mutable::empty));
        return new Root_meta_external_store_elasticsearch_v7_metamodel_store_Elasticsearch7StoreIndex_Impl(srcIndex.indexName, sourceInformation, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::store::Elasticsearch7StoreIndex"))
                ._indexName(srcIndex.indexName)
                ._properties(properties);
    }

    private static Root_meta_external_store_elasticsearch_v7_metamodel_store_Elasticsearch7StoreIndexProperty buildIndexProperty(Elasticsearch7StoreIndexProperty srcProperty, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation, CompileContext context)
    {
        HelperElasticsearchPropertyBuilder visitor = new HelperElasticsearchPropertyBuilder(context);
        Root_meta_external_store_elasticsearch_v7_metamodel_specification_types_mapping_Property property;
        PropertyBase propertyBase = (PropertyBase) srcProperty.property.unionValue();
        if (propertyBase != null)
        {
            property = propertyBase.accept(visitor);
        }
        else
        {
            property = visitor.getBuiltProperty();
        }

        return new Root_meta_external_store_elasticsearch_v7_metamodel_store_Elasticsearch7StoreIndexProperty_Impl(srcProperty.propertyName, sourceInformation, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::store::Elasticsearch7StoreIndexProperty"))
                ._propertyName(srcProperty.propertyName)
                ._property(property);
    }

    public static Root_meta_external_store_elasticsearch_v7_metamodel_runtime_Elasticsearch7StoreConnection buildConnection(Elasticsearch7StoreConnection srcConn, CompileContext context)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = SourceInformationHelper.toM3SourceInformation(srcConn.sourceInformation);

        Root_meta_pure_runtime_connection_authentication_AuthenticationSpecification authSpec = HelperAuthenticationBuilder.buildAuthenticationSpecification(srcConn.authSpec, context);
        Root_meta_external_store_elasticsearch_v7_metamodel_runtime_Elasticsearch7StoreURLSourceSpecification sourceSpec = buildSourceSpecification(srcConn.sourceSpec, context, srcConn.sourceInformation);

        Root_meta_external_store_elasticsearch_v7_metamodel_runtime_Elasticsearch7StoreConnection conn = new Root_meta_external_store_elasticsearch_v7_metamodel_runtime_Elasticsearch7StoreConnection_Impl("", sourceInformation, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::runtime::Elasticsearch7StoreConnection"))
                ._authSpec(authSpec)
                ._sourceSpec(sourceSpec);

        return conn._validate(true, sourceInformation, context.getExecutionSupport());
    }

    private static Root_meta_external_store_elasticsearch_v7_metamodel_runtime_Elasticsearch7StoreURLSourceSpecification buildSourceSpecification(Elasticsearch7StoreURLSourceSpecification srcSourceSpec, CompileContext context, SourceInformation sourceInformation)
    {
        Assert.assertTrue(srcSourceSpec.url.getPort() > 0, () -> "Elasticseaerch URL is missing port: " + srcSourceSpec.url, sourceInformation, EngineErrorType.COMPILATION);

        Enum scheme = context.pureModel.getEnumValue("meta::pure::functions::io::http::URLScheme", srcSourceSpec.url.getScheme(), SourceInformation.getUnknownSourceInformation(), sourceInformation);

        Root_meta_pure_functions_io_http_URL url = new Root_meta_pure_functions_io_http_URL_Impl(srcSourceSpec.url.toString(), null, context.pureModel.getClass("meta::pure::functions::io::http::URL"))
                ._scheme(scheme)
                ._host(srcSourceSpec.url.getHost())
                ._port(srcSourceSpec.url.getPort())
                ._path(StringUtils.isEmpty(srcSourceSpec.url.getPath()) ? "/" : srcSourceSpec.url.getPath());

        return new Root_meta_external_store_elasticsearch_v7_metamodel_runtime_Elasticsearch7StoreURLSourceSpecification_Impl(srcSourceSpec.url.toString() + "_spec", null, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::runtime::Elasticsearch7StoreURLSourceSpecification"))
                ._url(url);
    }

    public static Root_meta_external_store_elasticsearch_v7_metamodel_executionPlan_context_Elasticsearch7ExecutionContext buildExecutionContext(Elasticsearch7ExecutionContext srcExecContext, CompileContext context)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = SourceInformationHelper.toM3SourceInformation(null);
        return new Root_meta_external_store_elasticsearch_v7_metamodel_executionPlan_context_Elasticsearch7ExecutionContext_Impl("exexCtx", sourceInformation, context.pureModel.getClass("meta::external::store::elasticsearch::v7::metamodel::executionPlan::context::Elasticsearch7ExecutionContext"))
                ._validate(true, sourceInformation, context.getExecutionSupport());
    }
}
