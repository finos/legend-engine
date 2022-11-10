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

import java.util.Optional;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;
import static org.finos.legend.engine.testable.persistence.mapper.DatasetMapper.STAGING_SUFFIX;
import static org.finos.legend.engine.testable.persistence.mapper.DatasetMapper.isFieldNamePresent;
import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.DIGEST_FIELD_DEFAULT;

public class DeriveDatasets implements IngestModeVisitor<Datasets>
{
    private Persistence persistence;
    private Dataset mainDataset;
    private SchemaDefinition baseSchema;
    private String testData;

    private DatasetDefinition.Builder mainDatasetDefinitionBuilder;
    private SchemaDefinition.Builder mainSchemaDefinitionBuilder;
    private SchemaDefinition.Builder stagingSchemaDefinitionBuilder;
    private JsonExternalDatasetReference.Builder stagingDatasetBuilder;

    public DeriveDatasets(Persistence persistence, Dataset mainDataset, String testData)
    {
        this.persistence = persistence;
        this.mainDataset = mainDataset;
        this.baseSchema = mainDataset.schema();
        this.testData = testData;

        this.mainDatasetDefinitionBuilder = DatasetDefinition.builder()
                .name(mainDataset.datasetReference().name().get())
                .database(mainDataset.datasetReference().database())
                .group(mainDataset.datasetReference().group())
                .alias(mainDataset.datasetReference().alias().orElse(null));

        this.mainSchemaDefinitionBuilder = SchemaDefinition.builder()
                .addAllFields(mainDataset.schema().fields())
                .addAllIndexes(mainDataset.schema().indexes())
                .shardSpecification(mainDataset.schema().shardSpecification())
                .columnStoreSpecification(mainDataset.schema().columnStoreSpecification());

        this.stagingSchemaDefinitionBuilder = SchemaDefinition.builder()
                .addAllFields(mainDataset.schema().fields())
                .addAllIndexes(mainDataset.schema().indexes())
                .shardSpecification(mainDataset.schema().shardSpecification())
                .columnStoreSpecification(mainDataset.schema().columnStoreSpecification());

        this.stagingDatasetBuilder = JsonExternalDatasetReference.builder()
                .name(mainDataset.datasetReference().name().get() + STAGING_SUFFIX)
                .database(mainDataset.datasetReference().database())
                .group(mainDataset.datasetReference().group())
                .alias(mainDataset.datasetReference().alias().isPresent() ? mainDataset.datasetReference().alias().get() + STAGING_SUFFIX : null)
                .data(testData);
    }

    @Override
    public Datasets visit(AppendOnly appendOnly)
    {
        Dataset stagingDataset = stagingDatasetBuilder.schema(stagingSchemaDefinitionBuilder.build()).build();
        if (appendOnly.filterDuplicates)
        {
            mainSchemaDefinitionBuilder = enrichMainSchemaWithDigest();
        }
        mainSchemaDefinitionBuilder = appendOnly.auditing.accept(new MappingVisitors.EnrichSchemaWithAuditing(mainSchemaDefinitionBuilder, mainDataset));
        Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();

        return Datasets.of(enrichedMainDataset, stagingDataset);
    }

    @Override
    public Datasets visit(BitemporalDelta bitemporalDelta)
    {
        ValidityDerivation validityDerivation = bitemporalDelta.validityMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_VALIDITY_MILESTONING).validityDerivation();

        stagingSchemaDefinitionBuilder = validityDerivation.accept(new MappingVisitors.EnrichSchemaWithValidityMilestoningDerivation(stagingSchemaDefinitionBuilder, mainDataset));
        stagingSchemaDefinitionBuilder = bitemporalDelta.mergeStrategy.accept(new MappingVisitors.EnrichSchemaWithMergyStrategy(stagingSchemaDefinitionBuilder, mainDataset));
        Dataset stagingDataset = stagingDatasetBuilder.schema(stagingSchemaDefinitionBuilder.build()).build();

        mainSchemaDefinitionBuilder = enrichMainSchemaWithDigest();
        mainSchemaDefinitionBuilder = validityDerivation.accept(new MappingVisitors.EnrichSchemaWithValidityMilestoningDerivation(mainSchemaDefinitionBuilder, mainDataset));
        mainSchemaDefinitionBuilder = bitemporalDelta.transactionMilestoning.accept(new MappingVisitors.EnrichSchemaWithTransactionMilestoning(mainSchemaDefinitionBuilder, mainDataset));
        mainSchemaDefinitionBuilder = bitemporalDelta.validityMilestoning.accept(new MappingVisitors.EnrichSchemaWithValidityMilestoning(mainSchemaDefinitionBuilder, mainDataset));
        Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();

        return Datasets.of(enrichedMainDataset, stagingDataset);
    }

    @Override
    public Datasets visit(BitemporalSnapshot bitemporalSnapshot)
    {
        ValidityDerivation validityDerivation = bitemporalSnapshot.validityMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_VALIDITY_MILESTONING).validityDerivation();

        stagingSchemaDefinitionBuilder = validityDerivation.accept(new MappingVisitors.EnrichSchemaWithValidityMilestoningDerivation(stagingSchemaDefinitionBuilder, mainDataset));
        Dataset stagingDataset = stagingDatasetBuilder.schema(stagingSchemaDefinitionBuilder.build()).build();

        mainSchemaDefinitionBuilder = enrichMainSchemaWithDigest();
        mainSchemaDefinitionBuilder = bitemporalSnapshot.transactionMilestoning.accept(new MappingVisitors.EnrichSchemaWithTransactionMilestoning(mainSchemaDefinitionBuilder, mainDataset));
        mainSchemaDefinitionBuilder = bitemporalSnapshot.validityMilestoning.accept(new MappingVisitors.EnrichSchemaWithValidityMilestoning(mainSchemaDefinitionBuilder, mainDataset));
        Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();

        return Datasets.of(enrichedMainDataset, stagingDataset);
    }

    @Override
    public Datasets visit(NontemporalDelta nontemporalDelta)
    {
        stagingSchemaDefinitionBuilder = nontemporalDelta.mergeStrategy.accept(new MappingVisitors.EnrichSchemaWithMergyStrategy(stagingSchemaDefinitionBuilder, mainDataset));
        Dataset stagingDataset = stagingDatasetBuilder.schema(stagingSchemaDefinitionBuilder.build()).build();

        mainSchemaDefinitionBuilder = enrichMainSchemaWithDigest();
        mainSchemaDefinitionBuilder = nontemporalDelta.auditing.accept(new MappingVisitors.EnrichSchemaWithAuditing(mainSchemaDefinitionBuilder, mainDataset));
        Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();

        return Datasets.of(enrichedMainDataset, stagingDataset);
    }

    @Override
    public Datasets visit(NontemporalSnapshot nontemporalSnapshot)
    {
        Dataset stagingDataset = stagingDatasetBuilder.schema(stagingSchemaDefinitionBuilder.build()).build();

        mainSchemaDefinitionBuilder = nontemporalSnapshot.auditing.accept(new MappingVisitors.EnrichSchemaWithAuditing(mainSchemaDefinitionBuilder, mainDataset));
        Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();

        return Datasets.of(enrichedMainDataset, stagingDataset);
    }

    @Override
    public Datasets visit(UnitemporalDelta unitemporalDelta)
    {
        stagingSchemaDefinitionBuilder = unitemporalDelta.mergeStrategy.accept(new MappingVisitors.EnrichSchemaWithMergyStrategy(stagingSchemaDefinitionBuilder, mainDataset));
        Dataset stagingDataset = stagingDatasetBuilder.schema(stagingSchemaDefinitionBuilder.build()).build();

        mainSchemaDefinitionBuilder = enrichMainSchemaWithDigest();
        mainSchemaDefinitionBuilder = unitemporalDelta.transactionMilestoning.accept(new MappingVisitors.EnrichSchemaWithTransactionMilestoning(mainSchemaDefinitionBuilder, mainDataset));
        Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();

        return Datasets.of(enrichedMainDataset, stagingDataset);
    }

    @Override
    public Datasets visit(UnitemporalSnapshot unitemporalSnapshot)
    {
        Dataset stagingDataset = stagingDatasetBuilder.schema(stagingSchemaDefinitionBuilder.build()).build();

        mainSchemaDefinitionBuilder = enrichMainSchemaWithDigest();
        mainSchemaDefinitionBuilder = unitemporalSnapshot.transactionMilestoning.accept(new MappingVisitors.EnrichSchemaWithTransactionMilestoning(mainSchemaDefinitionBuilder, mainDataset));
        Dataset enrichedMainDataset = mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.build()).build();

        return Datasets.of(enrichedMainDataset, stagingDataset);
    }

    private SchemaDefinition.Builder enrichMainSchemaWithDigest()
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
        return mainSchemaDefinitionBuilder;
    }
}
