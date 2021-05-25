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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.Mapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.MapperPostProcessor;
import org.finos.legend.pure.generated.*;

import java.util.List;

public class HelperRelationalDatabaseConnectionBuilder
{
    public static void addTestDataSetUp(org.finos.legend.pure.m3.coreinstance.meta.relational.runtime.TestDatabaseConnection test, String testDataSetupCsv, java.util.List<String> testDataSetupSqls)
    {
        if (testDataSetupCsv != null)
        {
            test._testDataSetupCsv(testDataSetupCsv);
        }

        if (testDataSetupSqls != null)
        {
            test._testDataSetupSqls(FastList.newList(testDataSetupSqls));
        }
    }

    public static void addDatabaseConnectionProperties(org.finos.legend.pure.m3.coreinstance.meta.relational.runtime.DatabaseConnection pureConnection, String element, String connectionType, String timeZone, Boolean quoteIdentifiers, CompileContext context)
    {
        addDatabaseConnectionProperties(pureConnection, element, null, connectionType, timeZone, quoteIdentifiers, context);
    }

    public static void addDatabaseConnectionProperties(org.finos.legend.pure.m3.coreinstance.meta.relational.runtime.DatabaseConnection pureConnection, String element, SourceInformation elementSourceInformation, String connectionType, String timeZone, Boolean quoteIdentifiers, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.relational.runtime.DatabaseConnection connection = pureConnection._type(context.pureModel.getEnumValue("meta::relational::runtime::DatabaseType", connectionType));
        connection._timeZone(timeZone);
        connection._quoteIdentifiers(quoteIdentifiers);

        try
        {
            connection._element(HelperRelationalBuilder.resolveDatabase(element, elementSourceInformation, context));
        }
        catch (RuntimeException e)
        {
            connection._element(new Root_meta_relational_metamodel_Database_Impl(element)._name(element));
        }
    }

    public static Root_meta_pure_alloy_connections_MapperPostProcessor createMapperPostProcessor(MapperPostProcessor mapper)
    {
        return createMapperPostProcessor(mapper.mappers);
    }

    public static Root_meta_pure_alloy_connections_MapperPostProcessor createMapperPostProcessor(List<Mapper> mappers)
    {
        Root_meta_pure_alloy_connections_MapperPostProcessor p = new Root_meta_pure_alloy_connections_MapperPostProcessor_Impl("");
        p._mappers(createMappers(mappers));
        return p;
    }

    public static MutableList<Root_meta_pure_alloy_connections_Mapper> createMappers(List<Mapper> mappers) {
        return ListIterate.collect(mappers, m -> {
            if (m instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.TableNameMapper) {
                Root_meta_pure_alloy_connections_TableNameMapper nameMapper = new Root_meta_pure_alloy_connections_TableNameMapper_Impl("");
                Root_meta_pure_alloy_connections_SchemaNameMapper schemaNameMapper = new Root_meta_pure_alloy_connections_SchemaNameMapper_Impl("");

                schemaNameMapper._from(((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.TableNameMapper) m).schema.from);
                schemaNameMapper._to(((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.TableNameMapper) m).schema.to);

                nameMapper._from(m.from);
                nameMapper._to(m.to);
                nameMapper._schema(schemaNameMapper);

                return nameMapper;
            } else if (m instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.SchemaNameMapper) {
                Root_meta_pure_alloy_connections_SchemaNameMapper schemaNameMapper = new Root_meta_pure_alloy_connections_SchemaNameMapper_Impl("");
                schemaNameMapper._from(m.from);
                schemaNameMapper._to(m.to);

                return schemaNameMapper;
            }
            throw new IllegalArgumentException("Unknown mapper " + m.getClass().getSimpleName());
        });
    }
}
