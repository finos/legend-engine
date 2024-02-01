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

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Index;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.DatasetType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.Delta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.Snapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.sink.RelationalPersistenceTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Bitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Nontemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Temporality;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Unitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.AuditingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.AuditingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchId;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchIdAndDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDimensionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.Overwrite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.FilterDuplicates;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.output.GraphFetchServiceOutput;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.output.ServiceOutputTarget;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.Path;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.PropertyPathElement;
import org.finos.legend.engine.testable.persistence.mapper.AppendOnlyMapper;
import org.finos.legend.engine.testable.persistence.mapper.BitemporalDeltaMapper;
import org.finos.legend.engine.testable.persistence.mapper.BitemporalSnapshotMapper;
import org.finos.legend.engine.testable.persistence.mapper.NontemporalDeltaMapper;
import org.finos.legend.engine.testable.persistence.mapper.NontemporalSnapshotMapper;
import org.finos.legend.engine.testable.persistence.mapper.UnitemporalDeltaMapper;
import org.finos.legend.engine.testable.persistence.mapper.UnitemporalSnapshotMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.engine.testable.persistence.mapper.v2.DatasetMapper.STAGING_SUFFIX;
import static org.finos.legend.engine.testable.persistence.mapper.v2.DatasetMapper.isFieldNamePresent;

public class IngestModeMapper
{
    public static final String DIGEST_FIELD_DEFAULT = "DIGEST";
    public static final String BATCH_ID_FIELD_DEFAULT = "BATCH_ID";

    /*
    Mapper from Persistence model to IngestMode object
     */

    public static org.finos.legend.engine.persistence.components.ingestmode.IngestMode from(ServiceOutputTarget serviceOutputTarget) throws Exception
    {
        Temporality temporality = getTemporality(serviceOutputTarget);
        DatasetType datasetType = getDatasetType(serviceOutputTarget);
        TemporalityType temporalityType = getTemporalityType(temporality);

        switch (temporalityType)
        {
            case Nontemporal:
            {
                Nontemporal nontemporal = (Nontemporal) temporality;
                if (datasetType instanceof Snapshot)
                {
                    return NontemporalSnapshotMapper.from(nontemporal, datasetType);
                }
                else
                {
                    if (nontemporal.updatesHandling instanceof Overwrite)
                    {
                        return NontemporalDeltaMapper.from(nontemporal, datasetType);
                    }
                    else
                    {
                        return AppendOnlyMapper.from(nontemporal, datasetType);
                    }
                }
            }
            case Unitemporal:
            {
                Unitemporal unitemporal = (Unitemporal) temporality;
                if (datasetType instanceof Snapshot)
                {
                    return UnitemporalSnapshotMapper.from(unitemporal, datasetType);
                }
                else
                {
                    return UnitemporalDeltaMapper.from(unitemporal, datasetType);
                }
            }
            case Bitemporal:
            {
                Bitemporal bitemporal = (Bitemporal) temporality;
                if (datasetType instanceof Snapshot)
                {
                    return BitemporalSnapshotMapper.from(bitemporal, datasetType);
                }
                else
                {
                    return BitemporalDeltaMapper.from(bitemporal, datasetType);
                }
            }
            default:
                throw new Exception("Unsupported Temporality");
        }
    }

    public static Datasets deriveDatasets(ServiceOutputTarget serviceOutputTarget, Dataset mainDataset, String testData) throws Exception
    {
        Temporality temporality = getTemporality(serviceOutputTarget);
        DatasetType datasetType = getDatasetType(serviceOutputTarget);
        TemporalityType temporalityType = getTemporalityType(temporality);

        SchemaDefinition baseSchema = mainDataset.schema();

        DatasetDefinition.Builder mainDatasetDefinitionBuilder = DatasetDefinition.builder()
                .name(mainDataset.datasetReference().name().get())
                .database(mainDataset.datasetReference().database())
                .group(mainDataset.datasetReference().group())
                .alias(mainDataset.datasetReference().alias().orElse(null));

        SchemaDefinition.Builder mainSchemaDefinitionBuilder = SchemaDefinition.builder()
                .addAllFields(baseSchema.fields())
                .addAllIndexes(baseSchema.indexes())
                .shardSpecification(baseSchema.shardSpecification())
                .columnStoreSpecification(baseSchema.columnStoreSpecification());

        JsonExternalDatasetReference.Builder stagingDatasetBuilder = JsonExternalDatasetReference.builder()
                .name(mainDataset.datasetReference().name().get() + STAGING_SUFFIX)
                .database(mainDataset.datasetReference().database())
                .group(mainDataset.datasetReference().group())
                .alias(mainDataset.datasetReference().alias().isPresent() ? mainDataset.datasetReference().alias().get() + STAGING_SUFFIX : null)
                .data(testData);

        SchemaDefinition.Builder stagingSchemaDefinitionBuilder = SchemaDefinition.builder()
                .addAllIndexes(baseSchema.indexes())
                .shardSpecification(baseSchema.shardSpecification())
                .columnStoreSpecification(baseSchema.columnStoreSpecification());

        Set<String> fieldsToIgnore = new HashSet<>();
        Set<Field> fieldsToAdd = new HashSet<>();
        List<Field> filteredStagingFields = new ArrayList<>();
        List<Index> filteredStagingIndices = new ArrayList<>();

        switch (temporalityType)
        {
            case Nontemporal:
            {
                Nontemporal nontemporal = (Nontemporal) temporality;
                if (datasetType instanceof Snapshot)
                {
                    ignoreAuditField(fieldsToIgnore, nontemporal);
                    ignoreBatchIdField(fieldsToIgnore);
                        //todo: fix index logic
                    //    filteredStagingIndices = baseSchema.indexes().stream().filter(index -> !fieldsToIgnore.contains(index.indexName())).collect(Collectors.toList());
                    Dataset stagingDataset = getStagingDataset(baseSchema, stagingDatasetBuilder, fieldsToIgnore, fieldsToAdd);

                    return Datasets.of(mainDataset, stagingDataset);
                }
                else
                {
                    if (nontemporal.updatesHandling instanceof Overwrite)
                    {
                        ((Delta)datasetType).actionIndicator.accept(new MappingVisitors.DeriveStagingSchemaWithActionIndicatorStrategy(baseSchema, fieldsToAdd));
                        ignoreAuditField(fieldsToIgnore, nontemporal);
                        ignoreBatchIdField(fieldsToIgnore);
                        enrichMainSchemaWithDigest(baseSchema, mainSchemaDefinitionBuilder);
                        Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();
                        Dataset stagingDataset = getStagingDataset(baseSchema, stagingDatasetBuilder, fieldsToIgnore, fieldsToAdd);
                        return Datasets.of(enrichedMainDataset, stagingDataset);
                    }
                    else
                    {
                        ignoreAuditField(fieldsToIgnore, nontemporal);
                        ignoreBatchIdField(fieldsToIgnore);
                        enrichMainSchemaWithDigest(baseSchema, mainSchemaDefinitionBuilder);
                        Dataset stagingDataset = getStagingDataset(baseSchema, stagingDatasetBuilder, fieldsToIgnore, fieldsToAdd);
                        Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();
                        return Datasets.of(enrichedMainDataset, stagingDataset);
                    }
                }
            }
            case Unitemporal:
            {
                Unitemporal unitemporal = (Unitemporal) temporality;
                if (datasetType instanceof Delta)
                {
                    ((Delta)datasetType).actionIndicator.accept(new MappingVisitors.DeriveStagingSchemaWithActionIndicatorStrategy(baseSchema, fieldsToAdd));
                }
                unitemporal.processingDimension.accept(new MappingVisitors.DeriveStagingSchemaWithProcessingDimension(fieldsToIgnore));
                enrichMainSchemaWithDigest(baseSchema, mainSchemaDefinitionBuilder);
                Dataset stagingDataset = getStagingDataset(baseSchema, stagingDatasetBuilder, fieldsToIgnore, fieldsToAdd);
                Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();
                return Datasets.of(enrichedMainDataset, stagingDataset);
            }
            case Bitemporal:
            {
                Bitemporal bitemporal = (Bitemporal) temporality;
                if (datasetType instanceof Delta)
                {
                    ((Delta)datasetType).actionIndicator.accept(new MappingVisitors.DeriveStagingSchemaWithActionIndicatorStrategy(baseSchema, fieldsToAdd));
                }
                bitemporal.processingDimension.accept(new MappingVisitors.DeriveStagingSchemaWithProcessingDimension(fieldsToIgnore));
                bitemporal.sourceDerivedDimension.accept(new MappingVisitors.DeriveStagingSchemaWithSourceDimension(fieldsToIgnore, fieldsToAdd, baseSchema));
                enrichMainSchemaWithDigest(baseSchema, mainSchemaDefinitionBuilder);
                Dataset stagingDataset = getStagingDataset(baseSchema, stagingDatasetBuilder, fieldsToIgnore, fieldsToAdd);
                Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();
                return Datasets.of(enrichedMainDataset, stagingDataset);
            }
            default:
                throw new Exception("Unsupported Temporality");
        }
    }

    private static Dataset getStagingDataset(SchemaDefinition baseSchema, JsonExternalDatasetReference.Builder stagingDatasetBuilder, Set<String> fieldsToIgnore, Set<Field> fieldsToAdd)
    {
        SchemaDefinition.Builder stagingSchemaDefinitionBuilder;
        List<Field> filteredStagingFields;
        filteredStagingFields = getFilteredStagingFields(baseSchema, fieldsToIgnore, fieldsToAdd);
        stagingSchemaDefinitionBuilder = SchemaDefinition.builder().addAllFields(filteredStagingFields);
        Dataset stagingDataset = stagingDatasetBuilder.schema(stagingSchemaDefinitionBuilder.build()).build();
        return stagingDataset;
    }

    private static List<Field> getFilteredStagingFields(SchemaDefinition baseSchema, Set<String> fieldsToIgnore, Set<Field> fieldsToAdd)
    {
        List<Field> filteredStagingFields;
        filteredStagingFields =  baseSchema.fields().stream().filter(field -> !fieldsToIgnore.contains(field.name())).collect(Collectors.toList());
        filteredStagingFields.addAll(fieldsToAdd);
        return filteredStagingFields;
    }

    private static void enrichMainSchemaWithDigest(SchemaDefinition baseSchema, SchemaDefinition.Builder mainSchemaDefinitionBuilder)
    {
        // DIGEST field addition
        if (!isFieldNamePresent(baseSchema, DIGEST_FIELD_DEFAULT))
        {
            Field digest = Field.builder()
                    .name(DIGEST_FIELD_DEFAULT)
                    .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
                    .build();
            mainSchemaDefinitionBuilder.addFields(digest);
        }
    }

    public static Set<String> getFieldsToIgnoreForComparison(ServiceOutputTarget serviceOutputTarget) throws Exception
    {
        return extractFieldsToExclude(serviceOutputTarget.serviceOutput.datasetType, getTemporality(serviceOutputTarget));
    }

    private static Set<String> extractFieldsToExclude(DatasetType datasetType, Temporality temporality) throws Exception
    {
        TemporalityType temporalityType = getTemporalityType(temporality);
        Set<String> fieldsToIgnore = new HashSet<>();
        switch (temporalityType)
        {
            case Nontemporal:
            {
                Nontemporal nontemporal = (Nontemporal) temporality;
                if (datasetType instanceof Snapshot)
                {
                    ignoreAuditField(fieldsToIgnore, nontemporal);
                    return fieldsToIgnore;
                }
                else
                {
                    ignoreAuditField(fieldsToIgnore, nontemporal);
                    fieldsToIgnore.add(DIGEST_FIELD_DEFAULT);
                    return fieldsToIgnore;
                }
            }
            case Unitemporal:
            {
                Unitemporal unitemporal = (Unitemporal) temporality;
                fieldsToIgnore = unitemporal.processingDimension.accept(EXTRACT_TX_DATE_TIME_FIELDS);
                fieldsToIgnore.add(DIGEST_FIELD_DEFAULT);
                return fieldsToIgnore;
            }
            case Bitemporal:
            {
                Bitemporal bitemporal = (Bitemporal) temporality;
                fieldsToIgnore = bitemporal.processingDimension.accept(EXTRACT_TX_DATE_TIME_FIELDS);
                fieldsToIgnore.add(DIGEST_FIELD_DEFAULT);
                return fieldsToIgnore;
            }
            default:
                throw new Exception("Unsupported temporality");
        }
    }

    private static void ignoreAuditField(Set<String> fieldsToIgnore, Nontemporal nontemporal)
    {
        Optional<String> auditField = nontemporal.auditing != null ? nontemporal.auditing.accept(EXTRACT_AUDIT_FIELD) : Optional.empty();
        if (auditField.isPresent())
        {
            fieldsToIgnore.add(auditField.get());
        }
    }

    private static void ignoreBatchIdField(Set<String> fieldsToIgnore)
    {
        // Shall be changed to using the user-defined batch id column name once it is added to the spec
        fieldsToIgnore.add(BATCH_ID_FIELD_DEFAULT);
    }

    public static boolean isTransactionMilestoningTimeBased(ServiceOutputTarget serviceOutputTarget) throws Exception
    {
        return isTransactionMilestoningTimeBased(getTemporality(serviceOutputTarget));
    }

    private static boolean isTransactionMilestoningTimeBased(Temporality temporality) throws Exception
    {
        TemporalityType temporalityType = getTemporalityType(temporality);
        switch (temporalityType)
        {
            case Nontemporal:
            {
                return false;
            }
            case Unitemporal:
            {
                Unitemporal unitemporal = (Unitemporal) temporality;
                return unitemporal.processingDimension.accept(TRANSACTION_MILESTONING_TIME_BASED);
            }
            case Bitemporal:
            {
                Bitemporal bitemporal = (Bitemporal) temporality;
                return bitemporal.processingDimension.accept(TRANSACTION_MILESTONING_TIME_BASED);
            }
            default:
                throw new Exception("Unsupported temporality");
        }
    }

    public static DatasetType getDatasetType(ServiceOutputTarget serviceOutputTarget)
    {
        return serviceOutputTarget.serviceOutput.datasetType;
    }

    public static Temporality getTemporality(ServiceOutputTarget serviceOutputTarget) throws Exception
    {
        if (serviceOutputTarget.persistenceTarget instanceof RelationalPersistenceTarget)
        {
            RelationalPersistenceTarget target = (RelationalPersistenceTarget) serviceOutputTarget.persistenceTarget;
            return target.temporality;
        }
        throw new Exception("Test Runner only accepts Relational Target");
    }

    public static ServiceOutputTarget getServiceOutputTarget(Persistence persistence, Path graphFetchPath) throws Exception
    {
        List<ServiceOutputTarget> serviceOutputTargetList = persistence.serviceOutputTargets;
        if (serviceOutputTargetList.size() == 1)
        {
            return serviceOutputTargetList.get(0);
        }
        else
        {
            if (graphFetchPath == null)
            {
                throw new Exception("Graph fetch service-outputs require path parameter within tests");
            }
            List<String> testPropertyList = getPropertyList(graphFetchPath);

            for (ServiceOutputTarget serviceOutputTarget: serviceOutputTargetList)
            {
                Path path = ((GraphFetchServiceOutput)serviceOutputTarget.serviceOutput).path;
                List<String> propertyList = getPropertyList(path);

                if (path.name == graphFetchPath.name && path.startType.equals(graphFetchPath.startType) && propertyList.containsAll(testPropertyList))
                {
                    return serviceOutputTarget;
                }
            }
            throw new Exception("Exception : Cannot find a serviceOutputTarget with the matching path");
        }
    }

    public static List<String> getPropertyList(Path path)
    {
        List<String> propertyList = new ArrayList<>();
        path.path.forEach(ppe ->
        {
            if (ppe instanceof PropertyPathElement)
            {
                String property = ((PropertyPathElement) ppe).property;
                propertyList.add(property);
            }
        });
        return propertyList;
    }

    public static TemporalityType getTemporalityType(Temporality temporality)
    {
        String clazz = temporality.getClass().getSimpleName();
        TemporalityType temporalityType = TemporalityType.valueOf(clazz);
        return temporalityType;
    }

    public static final ProcessingDimensionVisitor<Set<String>> EXTRACT_TX_DATE_TIME_FIELDS = new ProcessingDimensionVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitBatchId(BatchId val)
        {
            return new HashSet<>();
        }

        @Override
        public Set<String> visitDateTime(ProcessingDateTime val)
        {
            return new HashSet<>(Arrays.asList(val.timeIn, val.timeOut));
        }

        @Override
        public Set<String> visitBatchIdAndDateTime(BatchIdAndDateTime val)
        {
            return new HashSet<>(Arrays.asList(val.timeIn, val.timeOut));
        }
    };

    public static final ProcessingDimensionVisitor<Boolean> TRANSACTION_MILESTONING_TIME_BASED = new ProcessingDimensionVisitor<Boolean>()
    {

        @Override
        public Boolean visitBatchId(BatchId val)
        {
            return false;
        }

        @Override
        public Boolean visitDateTime(ProcessingDateTime val)
        {
            return true;
        }

        @Override
        public Boolean visitBatchIdAndDateTime(BatchIdAndDateTime val)
        {
            return false;
        }
    };

    public static final AuditingVisitor<Optional<String>> EXTRACT_AUDIT_FIELD = new AuditingVisitor<Optional<String>>()
    {
        @Override
        public Optional<String> visitAuditingDateTime(AuditingDateTime val)
        {
            return Optional.of(val.auditingDateTimeName);
        }

        @Override
        public Optional<String> visitNoAuditing(NoAuditing val)
        {
            return Optional.empty();
        }
    };

}