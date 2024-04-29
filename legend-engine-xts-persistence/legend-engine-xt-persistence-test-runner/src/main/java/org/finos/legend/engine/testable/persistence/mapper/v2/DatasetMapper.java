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

package org.finos.legend.engine.testable.persistence.mapper.v2;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.output.ServiceOutputTarget;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.Path;
import org.finos.legend.engine.testable.persistence.mapper.FieldTypeMapper;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_service_GraphFetchServiceOutput;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_target_PersistenceTarget;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_RelationalPersistenceTarget;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.pure.generated.platform_store_relational_functions.Root_meta_relational_metamodel_datatype_dataTypeToSqlText_DataType_1__String_1_;

public class DatasetMapper
{
    public static String STAGING_SUFFIX = "_staging";
    private static String DEFAULT_SCHEMA = "default";
    private static String H2_PUBLIC_SCHEMA = "PUBLIC";

    public static Dataset getTargetDatasetV2(Root_meta_pure_persistence_metamodel_Persistence persistence, Path graphFetchPath) throws Exception
    {
        Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget serviceOutputTarget;
        if (persistence._serviceOutputTargets().size() > 1 && graphFetchPath != null)
        {
            serviceOutputTarget = getMultiFlatServiceTargetOutput(persistence, graphFetchPath);
        }
        else
        {
            serviceOutputTarget = getFlatServiceTargetOutput(persistence);
        }
        Table table = getTargetTable(serviceOutputTarget);
        String schemaName = table._schema()._name();
        if (schemaName.equals(DEFAULT_SCHEMA))
        {
            schemaName = H2_PUBLIC_SCHEMA;
        }
        SchemaDefinition schemaDefinition = getSchemaDefinition(table);

        DatasetDefinition datasetDefinition = DatasetDefinition.builder()
                .name(table._name())
                .group(schemaName)
                .alias(table._name())
                .schema(schemaDefinition)
                .build();

        return datasetDefinition;
    }

    private static Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget getMultiFlatServiceTargetOutput(Root_meta_pure_persistence_metamodel_Persistence persistence, Path graphFetchPath) throws Exception
    {
        RichIterable<? extends Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget> serviceOutputTargets = persistence._serviceOutputTargets();
        if (serviceOutputTargets.size() == 1)
        {
            return serviceOutputTargets.getAny();
        }
        else
        {
            if (graphFetchPath == null)
            {
                throw new Exception("Graph fetch service-outputs require path parameter within tests");
            }
            List<String> testPropertyList = IngestModeMapper.getPropertyList(graphFetchPath);
            for (Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget serviceOutputTarget: serviceOutputTargets)
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.Path path = ((Root_meta_pure_persistence_metamodel_service_GraphFetchServiceOutput)serviceOutputTarget._serviceOutput())._path();
                if (path._name() == graphFetchPath.name && getPurePropertyList(path).containsAll(testPropertyList))
                {
                    return serviceOutputTarget;
                }
            }
            throw new Exception("Exception : Cannot find a serviceOutputTarget with the matching path");
        }
    }

    private static List<String> getPurePropertyList(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.Path path)
    {
        List<String> propertyList = new ArrayList<>();
        path._path().forEach(ppe ->
        {
            if (ppe instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement)
            {
                String property = String.valueOf(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.path.PropertyPathElement) ppe)._property());
                propertyList.add(property);
            }
        });
        return propertyList;
    }

    public static Datasets enrichAndDeriveDatasets(ServiceOutputTarget serviceOutputTarget, Dataset mainDataset, String testData) throws Exception
    {
        return IngestModeMapper.deriveDatasets(serviceOutputTarget, mainDataset, testData);
    }

    private static Table getTargetTable(Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget serviceOutputTarget)
    {
        Root_meta_pure_persistence_metamodel_target_PersistenceTarget target = serviceOutputTarget._target();
        if (!(target instanceof Root_meta_pure_persistence_relational_metamodel_RelationalPersistenceTarget))
        {
            throw new UnsupportedOperationException("write-component-test only supports RelationalSink");
        }
        Root_meta_pure_persistence_relational_metamodel_RelationalPersistenceTarget relationalSink = (Root_meta_pure_persistence_relational_metamodel_RelationalPersistenceTarget) target;
        return relationalSink._table();
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

    public static Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget getFlatServiceTargetOutput(Root_meta_pure_persistence_metamodel_Persistence persistence)
    {
        RichIterable<? extends Root_meta_pure_persistence_metamodel_service_ServiceOutputTarget> serviceOutputTargets = persistence._serviceOutputTargets();
        return serviceOutputTargets.getAny();
    }

    static boolean isFieldNamePresent(SchemaDefinition schema, String fieldName)
    {
        return schema.fields().stream().anyMatch(field -> field.name().equals(fieldName));
    }
}