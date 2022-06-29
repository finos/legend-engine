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
import org.finos.legend.pure.ide.light.PureIDECodeRepository;
import org.finos.legend.pure.ide.light.PureIDEServer;
import org.finos.legend.pure.ide.light.SourceLocationConfiguration;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

public class PureIDELight extends PureIDEServer
{
    public static void main(String[] args) throws Exception
    {
        new PureIDELight().run(args.length == 0 ? new String[] {"server", "legend-engine-pure-ide-light/src/main/resources/ideLightConfig.json"} : args);
    }

    public MutableList<RepositoryCodeStorage> buildRepositories(SourceLocationConfiguration sourceLocationConfiguration)
    {
        try
        {
            String ideFilesLocation = Optional.ofNullable(sourceLocationConfiguration)
                    .flatMap(s -> Optional.ofNullable(s.ideFilesLocation))
                    .orElse("legend-engine-pure-ide-light/src/main/resources/pure_ide");

            return Lists.mutable
                    .<RepositoryCodeStorage>with(new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository()))
                    .with(this.buildCore("legend-engine-xt-persistence-pure", "persistence"))
                    .with(this.buildCore("legend-engine-xt-relationalStore-pure", "relational"))
                    .with(this.buildCore("legend-engine-xt-relationalStore-sqlserver-pure", "relational_sqlserver"))
                    .with(this.buildCore("legend-engine-xt-relationalStore-snowflake-pure", "relational_snowflake"))
                    .with(this.buildCore("legend-engine-xt-relationalStore-redshift-pure", "relational_redshift"))
                    .with(this.buildCore("legend-engine-xt-relationalStore-bigQuery-pure", "relational_bigquery"))
                    .with(this.buildCore("legend-engine-xt-relationalStore-databricks-pure", "relational_databricks"))
                    .with(this.buildCore("legend-engine-xt-serviceStore-pure", "servicestore"))
                    .with(this.buildCore("legend-engine-xt-text-pure", "text"))
                    .with(this.buildCore("legend-engine-xt-data-space-pure", "data-space"))
                    .with(this.buildCore("legend-engine-xt-diagram-pure", "diagram"))
                    .with(this.buildCore("legend-engine-xt-flatdata-pure", "external-format-flatdata"))
                    .with(this.buildCore("legend-engine-xt-json-pure", "external-format-json"))
                    .with(this.buildCore("legend-engine-xt-xml-pure", "external-format-xml"))
                    .with(this.buildCore("legend-engine-xt-graphQL-pure", "external-query-graphql"))
                    .with(this.buildCore("legend-engine-xt-protobuf-pure", "external-format-protobuf"))
                    .with(this.buildCore("legend-engine-xt-avro-pure", "external-format-avro"))
                    .with(this.buildCore("legend-engine-xt-rosetta-pure", "external-format-rosetta"))
                    .with(this.buildCore("legend-engine-xt-morphir-pure", "external-language-morphir"))
                    .with(this.buildCore("legend-engine-pure-ide-light-metadata-pure", "ide_metadata"))
                    .with(this.buildCore("legend-engine-pure-code-compiled-core", ""))
                    .with(this.buildCore("legend-engine-pure-code-compiled-core-external-shared", "external-shared"))
                    .with(this.buildCore("legend-engine-xt-analytics-mapping-pure", "analytics-mapping"))
                    .with(new MutableFSCodeStorage(new PureIDECodeRepository(), Paths.get(ideFilesLocation)));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected MutableFSCodeStorage buildCore(String path, String module) throws IOException
    {
        String resources = path + "/src/main/resources";
        String moduleName = "core" + (module.equals("") ? "" : "_" + module);
        return new MutableFSCodeStorage(
                GenericCodeRepository.build(Paths.get(resources + "/" + moduleName.replace("-", "_") + ".definition.json")),
                Paths.get(resources + "/" + moduleName.replace("-", "_"))
        );
    }
}
