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
import org.finos.legend.pure.ide.light.PureIDECodeRepository;
import org.finos.legend.pure.ide.light.PureIDEServer;
import org.finos.legend.pure.ide.light.SourceLocationConfiguration;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

import java.nio.file.Paths;
import java.util.Optional;

public class PureIDELight extends PureIDEServer
{
    public static void main(String[] args) throws Exception
    {
        new PureIDELight().run(args.length == 0 ? new String[]{"server", "legend-engine-pure-ide-light/src/main/resources/ideLightConfig.json"} : args);
    }

    @Override
    protected MutableList<RepositoryCodeStorage> buildRepositories(SourceLocationConfiguration sourceLocationConfiguration)
    {
        String ideFilesLocation = Optional.ofNullable(sourceLocationConfiguration).map(s -> s.ideFilesLocation).orElse("legend-engine-pure-ide-light/src/main/resources/pure_ide");
        return Lists.mutable.<RepositoryCodeStorage>empty()
                .with(this.buildCore("legend-engine-xt-persistence-pure", "persistence"))
                .with(this.buildCore("legend-engine-xt-mastery-pure", "mastery"))
                .with(this.buildCore("legend-engine-xt-relationalStore-pure", "relational"))
                .with(this.buildCore("legend-engine-xt-relationalStore-sqlserver-pure", "relational_sqlserver"))
                .with(this.buildCore("legend-engine-xt-relationalStore-bigquery-pure", "relational_bigquery"))
                .with(this.buildCore("legend-engine-xt-relationalStore-spanner-pure", "relational_spanner"))
                .with(this.buildCore("legend-engine-xt-serviceStore-pure", "servicestore"))
                .with(this.buildCore("legend-engine-xt-text-pure", "text"))
                .with(this.buildCore("legend-engine-xt-data-space-pure", "data-space"))
                .with(this.buildCore("legend-engine-xt-diagram-pure", "diagram"))
                .with(this.buildCore("legend-engine-xt-flatdata-pure", "external-format-flatdata"))
                .with(this.buildCore("legend-engine-xt-json-pure", "external-format-json"))
                .with(this.buildCore("legend-engine-xt-xml-pure", "external-format-xml"))
                .with(this.buildCore("legend-engine-xt-graphQL-pure", "external-query-graphql"))
                .with(this.buildCore("legend-engine-xt-graphQL-pure-metamodel", "external-query-graphql-metamodel"))
                .with(this.buildCore("legend-engine-xt-protobuf-pure", "external-format-protobuf"))
                .with(this.buildCore("legend-engine-xt-avro-pure", "external-format-avro"))
                .with(this.buildCore("legend-engine-xt-rosetta-pure", "external-format-rosetta"))
                .with(this.buildCore("legend-engine-xt-morphir-pure", "external-language-morphir"))
                .with(this.buildCore("legend-engine-xt-haskell-pure", "external-language-haskell"))
                .with(this.buildCore("legend-engine-xt-daml-pure", "external-language-daml"))
                .with(this.buildCore("legend-engine-pure-ide-light-metadata-pure", "ide_metadata"))
                .with(this.buildCore("legend-engine-pure-code-compiled-core", ""))
                .with(this.buildCore("legend-engine-xt-analytics-mapping-pure", "analytics-mapping"))
                .with(this.buildCore("legend-engine-xt-analytics-lineage-pure", "analytics-lineage"))
                .with(this.buildCore("legend-engine-xt-javaGeneration-pure", "external-language-java"))
                .with(this.buildCore("legend-engine-xt-javaPlatformBinding-pure", "java-platform-binding"))
                .with(this.buildCore("legend-engine-xt-flatdata-javaPlatformBinding-pure", "external-format-flatdata-java-platform-binding"))
                .with(this.buildCore("legend-engine-xt-json-javaPlatformBinding-pure", "external-format-json-java-platform-binding"))
                .with(this.buildCore("legend-engine-xt-xml-javaPlatformBinding-pure", "external-format-xml-java-platform-binding"))
                .with(this.buildCore("legend-engine-xt-relationalStore-javaPlatformBinding-pure", "relational-java-platform-binding"))
                .with(this.buildCore("legend-engine-xt-serviceStore-javaPlatformBinding-pure", "servicestore-java-platform-binding"))
                .with(this.buildCore("legend-engine-pure-code-compiled-core-configuration", "configuration"))
                .with(this.buildCore("legend-engine-xt-sql-pure-metamodel", "external-query-sql-metamodel"))
                .with(this.buildCore("legend-engine-xt-sql-pure", "external-query-sql"))
                .with(new MutableFSCodeStorage(new PureIDECodeRepository(), Paths.get(ideFilesLocation)));
    }

    @Override
    protected void postInit()
    {
        FunctionExecutionInterpreted fe = (FunctionExecutionInterpreted) this.getPureSession().getFunctionExecution();
        fe.setProcessorSupport(new LegendCompileMixedProcessorSupport(fe.getRuntime().getContext(), fe.getRuntime().getModelRepository(), fe.getProcessorSupport()));
    }

    protected MutableFSCodeStorage buildCore(String path, String module)
    {
        String resourceDir = path + "/src/main/resources/";
        String moduleName = "".equals(module) ? "core" : ("core_" + module.replace("-", "_"));
        return new MutableFSCodeStorage(
                GenericCodeRepository.build(Paths.get(resourceDir + moduleName + ".definition.json")),
                Paths.get(resourceDir + moduleName)
        );
    }
}
