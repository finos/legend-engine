//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.ide;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.LegendCompileMixedProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

import java.nio.file.Paths;

public class PureIDELight extends PureIDEServer
{
    public static void main(String[] args) throws Exception
    {
        System.setProperty("legend.test.h2.port", "1975");
        System.setProperty("user.timezone", "GMT");
        new PureIDELight().run(args.length == 0 ? new String[]{"server", "legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json"} : args);
    }

    @Override
    protected MutableList<RepositoryCodeStorage> buildRepositories(SourceLocationConfiguration sourceLocationConfiguration)
    {
        return Lists.mutable.<RepositoryCodeStorage>empty()
                // --- MINIMUM -----
                .with(this.buildCore("legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-metadata-pure", "ide_metadata"))
                .with(this.build("legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-pure", "pure_ide", false))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure", "relational"))
                .with(this.buildCore("legend-engine-xts-serviceStore/legend-engine-xt-serviceStore-pure", "servicestore"))
                .with(this.buildCore("legend-engine-xts-service/legend-engine-language-pure-dsl-service-pure", "service"))
                .with(this.buildCore("legend-engine-xts-json/legend-engine-xt-json-pure", "external_format_json"))
                .with(this.buildCore("legend-engine-xts-generation/legend-engine-language-pure-dsl-generation-pure", "generation"))
                .with(this.buildCore("legend-engine-xts-authentication/legend-engine-xt-authentication-pure", "authentication"))
                .with(this.buildCore("legend-engine-xts-data-space/legend-engine-xt-data-space-pure-metamodel", "data_space_metamodel"))
                .with(this.buildCore("legend-engine-xts-diagram/legend-engine-xt-diagram-pure-metamodel", "diagram-metamodel"))
                .with(this.buildCore("legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-unclassified/legend-engine-pure-functions-unclassified-pure", "functions_unclassified"))
                .with(this.buildCore("legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-json/legend-engine-pure-functions-json-pure", "functions_json"))
                .with(this.buildCore("legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-functions-relation-pure", "functions_relation"))
                .with(this.buildCore("legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-standard/legend-engine-pure-functions-standard-pure", "functions_standard"))
                .with(this.buildCore("legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core", ""))
                // --- MINIMUM -----

                .with(this.buildCore("legend-engine-xts-java/legend-engine-xt-javaGeneration-conventions-essential-pure", "external_language_java_conventions_essential"))
                .with(this.buildCore("legend-engine-xts-java/legend-engine-xt-javaGeneration-conventions-standard-pure", "external_language_java_conventions_standard"))
                .with(this.buildCore("legend-engine-xts-protocol-java-generation/legend-engine-protocol-generation-pure", "protocol_generation"))
                .with(this.buildCore("legend-engine-xts-persistence/legend-engine-xt-persistence-pure", "persistence"))
                .with(this.buildCore("legend-engine-xts-functionActivator/legend-engine-xt-functionActivator-pure", "function_activator"))
                .with(this.buildCore("legend-engine-xts-snowflakeApp/legend-engine-xt-snowflakeApp-pure", "snowflakeapp"))
                .with(this.buildCore("legend-engine-xts-bigqueryFunction/legend-engine-xt-bigqueryFunction-pure", "bigqueryfunction"))
                .with(this.buildCore("legend-engine-xts-hostedService/legend-engine-xt-hostedService-pure", "hostedservice"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-javaPlatformBinding-pure", "relational-java-platform-binding"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-sqlserver/legend-engine-xt-relationalStore-sqlserver-pure", "relational_sqlserver"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-duckdb/legend-engine-xt-relationalStore-duckdb-pure", "relational_duckdb"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-memsql/legend-engine-xt-relationalStore-memsql-pure", "relational_memsql"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-bigquery/legend-engine-xt-relationalStore-bigquery-pure", "relational_bigquery"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-spanner/legend-engine-xt-relationalStore-spanner-pure", "relational_spanner"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-athena/legend-engine-xt-relationalStore-athena-pure", "relational_athena"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-snowflake/legend-engine-xt-relationalStore-snowflake-pure", "relational_snowflake"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-redshift/legend-engine-xt-relationalStore-redshift-pure", "relational_redshift"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-databricks/legend-engine-xt-relationalStore-databricks-pure", "relational_databricks"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-hive/legend-engine-xt-relationalStore-hive-pure", "relational_hive"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-postgres/legend-engine-xt-relationalStore-postgres-pure", "relational_postgres"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-presto/legend-engine-xt-relationalStore-presto-pure", "relational_presto"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-sybase/legend-engine-xt-relationalStore-sybase-pure", "relational_sybase"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-sybaseiq/legend-engine-xt-relationalStore-sybaseiq-pure", "relational_sybaseiq"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-sparksql/legend-engine-xt-relationalStore-sparksql-pure", "relational_sparksql"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-analytics/legend-engine-xt-relationalStore-store-entitlement-pure", "relational_store_entitlement"))
                .with(this.buildCore("legend-engine-xts-serviceStore/legend-engine-xt-serviceStore-javaPlatformBinding-pure", "servicestore-java-platform-binding"))
                .with(this.buildCore("legend-engine-xts-text/legend-engine-xt-text-pure-metamodel", "text-metamodel"))
                .with(this.buildCore("legend-engine-xts-data-space/legend-engine-xt-data-space-pure-metamodel", "data-space-metamodel"))
                .with(this.buildCore("legend-engine-xts-data-space/legend-engine-xt-data-space-pure", "data-space"))
                .with(this.buildCore("legend-engine-xts-diagram/legend-engine-xt-diagram-pure", "diagram"))
                .with(this.buildCore("legend-engine-xts-flatdata/legend-engine-xt-flatdata-pure", "external-format-flatdata"))
                .with(this.buildCore("legend-engine-xts-flatdata/legend-engine-xt-flatdata-javaPlatformBinding-pure", "external-format-flatdata-java-platform-binding"))
                .with(this.buildCore("legend-engine-xts-json/legend-engine-xt-json-pure", "external-format-json"))
                .with(this.buildCore("legend-engine-xts-json/legend-engine-xt-json-javaPlatformBinding-pure", "external-format-json-java-platform-binding"))
                .with(this.buildCore("legend-engine-xts-xml/legend-engine-xt-xml-pure", "external-format-xml"))
                .with(this.buildCore("legend-engine-xts-xml/legend-engine-xt-xml-javaPlatformBinding-pure", "external-format-xml-java-platform-binding"))
                .with(this.buildCore("legend-engine-xts-openapi/legend-engine-xt-openapi-pure", "external-format-openapi"))
                .with(this.buildCore("legend-engine-xts-graphQL/legend-engine-xt-graphQL-pure", "external-query-graphql"))
                .with(this.buildCore("legend-engine-xts-graphQL/legend-engine-xt-graphQL-pure-metamodel", "external-query-graphql-metamodel"))
                .with(this.buildCore("legend-engine-xts-protobuf/legend-engine-xt-protobuf-pure", "external-format-protobuf"))
                .with(this.buildCore("legend-engine-xts-avro/legend-engine-xt-avro-pure", "external-format-avro"))
                .with(this.buildCore("legend-engine-xts-rosetta/legend-engine-xt-rosetta-pure", "external-format-rosetta"))
                .with(this.buildCore("legend-engine-xts-morphir/legend-engine-xt-morphir-pure", "external-language-morphir"))
                .with(this.buildCore("legend-engine-xts-haskell/legend-engine-xt-haskell-pure", "external-language-haskell"))
                .with(this.buildCore("legend-engine-xts-daml/legend-engine-xt-daml-pure", "external-language-daml"))
                .with(this.buildCore("legend-engine-xts-changetoken/legend-engine-xt-changetoken-pure", "pure-changetoken"))
                .with(this.buildCore("legend-engine-xts-changetoken/legend-engine-xt-changetoken-test-pure", "pure-changetoken-test"))
                .with(this.buildCore("legend-engine-xts-analytics/legend-engine-xts-analytics-mapping/legend-engine-xt-analytics-mapping-pure", "analytics-mapping"))
                .with(this.buildCore("legend-engine-xts-analytics/legend-engine-xts-analytics-class/legend-engine-xt-analytics-class-pure", "analytics-class"))
                .with(this.buildCore("legend-engine-xts-analytics/legend-engine-xts-analytics-binding/legend-engine-xt-analytics-binding-pure", "analytics-binding"))
                .with(this.buildCore("legend-engine-xts-analytics/legend-engine-xts-analytics-function/legend-engine-xt-analytics-function-pure", "analytics-function"))
                .with(this.buildCore("legend-engine-xts-analytics/legend-engine-xts-analytics-lineage/legend-engine-xt-analytics-lineage-pure", "analytics-lineage"))
                .with(this.buildCore("legend-engine-xts-analytics/legend-engine-xts-analytics-search/legend-engine-xt-analytics-search-pure", "analytics-search"))
                .with(this.buildCore("legend-engine-xts-analytics/legend-engine-xts-analytics-test-coverage/legend-engine-xt-analytics-test-coverage-pure", "analytics-test-coverage"))
                .with(this.buildCore("legend-engine-xts-java/legend-engine-xt-javaGeneration-pure", "external-language-java"))
                .with(this.buildCore("legend-engine-xts-java/legend-engine-xt-javaGeneration-featureBased-pure", "external-language-java-feature-based-generation"))
                .with(this.buildCore("legend-engine-xts-java/legend-engine-xt-javaPlatformBinding-pure", "java-platform-binding"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-postgresSql/legend-engine-xt-relationalStore-postgresSqlModel-pure", "external-store-relational-postgres-sql-model"))
                .with(this.buildCore("legend-engine-xts-sql/legend-engine-xt-sql-pure", "external-query-sql"))
                .with(this.buildCore("legend-engine-xts-elasticsearch/legend-engine-xt-elasticsearch-pure-specification-metamodel", "elasticsearch_specification_metamodel"))
                .with(this.buildCore("legend-engine-xts-elasticsearch/legend-engine-xt-elasticsearch-executionPlan-test", "elasticsearch_execution_test"))
                .with(this.buildCore("legend-engine-xts-elasticsearch/legend-engine-xt-elasticsearch-V7-pure-metamodel", "elasticsearch_seven_metamodel"))
                .with(this.buildCore("legend-engine-xts-mongodb/legend-engine-xt-nonrelationalStore-mongodb-pure", "nonrelational-mongodb"))
                .with(this.buildCore("legend-engine-xts-mongodb/legend-engine-xt-nonrelationalStore-mongodb-javaPlatformBinding-pure", "nonrelational-mongodb-java-platform-binding"))
                .with(this.buildCore("legend-engine-xts-iceberg/legend-engine-xt-iceberg-pure", "external-tableformat-iceberg"))
                .with(this.buildCore("legend-engine-xts-arrow/legend-engine-xt-arrow-pure", "external-format-arrow"))
                .with(this.buildCore("legend-engine-xts-relationalai/legend-engine-xt-relationalai-pure", "external-query-relationalai"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-PCT/legend-engine-pure-functions-relationalStore-PCT-pure", "external_test_connection"))
                .with(this.buildCore("legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure", "dataquality"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-sqlPlanning-pure", "external-store-relational-sql-planning"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-sqlDialectTranslation-pure", "external-store-relational-sql-dialect-translation"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-SDT-pure", "external-store-relational-sdt"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-duckdb/legend-engine-xt-relationalStore-duckdb-sqlDialectTranslation-pure", "external-store-relational-sql-dialect-translation-duckdb"))
                .with(this.buildCore("legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-FCT-pure", "relational_fct"))
                ;

    }

    @Override
    protected void postInit()
    {
        FunctionExecutionInterpreted fe = (FunctionExecutionInterpreted) this.getPureSession().getFunctionExecution();
        fe.setProcessorSupport(new LegendCompileMixedProcessorSupport(fe.getRuntime().getContext(), fe.getRuntime().getModelRepository(), fe.getProcessorSupport()));
    }

    protected MutableFSCodeStorage buildCore(String path, String module)
    {
        return build(path, module, true);
    }

    protected MutableFSCodeStorage build(String path, String module, boolean core)
    {
        String resourceDir = path + "/src/main/resources/";
        String moduleName = core ? "".equals(module) ? "core" : ("core_" + module.replace("-", "_")) : module.replace("-", "_");
        GenericCodeRepository repository = GenericCodeRepository.build(Paths.get(resourceDir + moduleName + ".definition.json"));
        return new MutableFSCodeStorage(
                GenericCodeRepository.build(repository.getName(), repository.getAllowedPackagesPattern(), repository.getDependencies().toSet().with("pure_ide_debug")),
                Paths.get(resourceDir + moduleName)
        );
    }

}
