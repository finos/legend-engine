// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.persistence.mapper;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.testable.persistence.exception.PersistenceException;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_sink_Sink;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_BatchPersister;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_sink_RelationalSink;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.SchemaAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_Persister;
import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.getIngestMode;
import static org.finos.legend.pure.generated.platform_store_relational_functions.Root_meta_relational_metamodel_datatype_dataTypeToSqlText_DataType_1__String_1_;

public class DatasetMapper
{
    public static String STAGING_SUFFIX = "_staging";
    private static String DEFAULT_SCHEMA = "default";
    private static String H2_PUBLIC_SCHEMA = "PUBLIC";

    public static Dataset getTargetDataset(Root_meta_pure_persistence_metamodel_Persistence persistence) throws Exception
    {
        Root_meta_pure_persistence_metamodel_persister_BatchPersister batchPersister = getBatchPersister(persistence);
        Database database = getDatabase(batchPersister);
        Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget flatTarget = getTarget(batchPersister);

        // Find the table
        RichIterable<? extends Table> tables = database._schemas().flatCollect(SchemaAccessor::_tables);
        Table table = tables.detect(t -> t._name().equals(flatTarget._targetName()));
        if (table == null)
        {
            throw new PersistenceException(String.format("Target table [%s] not found in Persistence Spec", flatTarget._targetName()));
        }
        String tableName = table._name();
        String schemaName = table._schema()._name();
        if (schemaName.equals(DEFAULT_SCHEMA))
        {
            schemaName = H2_PUBLIC_SCHEMA;
        }
        SchemaDefinition schemaDefinition = getSchemaDefinition(table);

        DatasetDefinition datasetDefinition = DatasetDefinition.builder()
                .name(tableName)
                .group(schemaName)
                .alias(tableName)
                .schema(schemaDefinition)
                .build();

        return datasetDefinition;
    }

    public static Datasets enrichAndDeriveDatasets(Persistence persistence, Dataset mainDataset, String testData) throws Exception
    {
        IngestMode ingestMode = getIngestMode(persistence);
        return ingestMode.accept(new DeriveDatasets(mainDataset, testData));
    }

    private static Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget getTarget(Root_meta_pure_persistence_metamodel_persister_BatchPersister batchPersister)
    {
        Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape targetShape = batchPersister._targetShape();
        if (!(targetShape instanceof Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget))
        {
            throw new UnsupportedOperationException("write-component-test only supports FlatTarget");
        }
        Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget flatTarget = (Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget) targetShape;
        return flatTarget;
    }

    private static Database getDatabase(Root_meta_pure_persistence_metamodel_persister_BatchPersister batchPersister)
    {
        Root_meta_pure_persistence_metamodel_persister_sink_Sink sink = batchPersister._sink();
        if (!(sink instanceof Root_meta_pure_persistence_metamodel_persister_sink_RelationalSink))
        {
            throw new UnsupportedOperationException("write-component-test only supports RelationalSink");
        }
        Root_meta_pure_persistence_metamodel_persister_sink_RelationalSink relationalSink = (Root_meta_pure_persistence_metamodel_persister_sink_RelationalSink) sink;
        Database database = relationalSink._database();
        return database;
    }

    private static SchemaDefinition getSchemaDefinition(Table table) throws Exception
    {
        SchemaDefinition.Builder builder = SchemaDefinition.builder();
        RichIterable<Column> columns = table._columns().select(c -> c instanceof Column).collect(c -> (Column) c);
        RichIterable<String> pks = table._primaryKey().select(c -> c instanceof Column).collect(c -> c._name());
        for (Column column : columns)
        {
            String dataType = Root_meta_relational_metamodel_datatype_dataTypeToSqlText_DataType_1__String_1_(column._type(), null);
            FieldType fieldType = FieldTypeMapper.from(dataType);
            Field field = Field.builder()
                    .name(column._name())
                    .primaryKey(pks.contains(column._name()))
                    .type(fieldType)
                    .build();
            builder = builder.addFields(field);
        }
        return builder.build();
    }

    private static Root_meta_pure_persistence_metamodel_persister_BatchPersister getBatchPersister(Root_meta_pure_persistence_metamodel_Persistence persistence)
    {
        Root_meta_pure_persistence_metamodel_persister_Persister persister = persistence._persister();
        if (!(persister instanceof Root_meta_pure_persistence_metamodel_persister_BatchPersister))
        {
            throw new UnsupportedOperationException("write-component-test only supports BatchPersister");
        }
        return (Root_meta_pure_persistence_metamodel_persister_BatchPersister) persister;
    }

    static boolean isFieldNamePresent(SchemaDefinition schema, String fieldName)
    {
        return schema.fields().stream().anyMatch(field -> field.name().equals(fieldName));
    }
}