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

package org.finos.legend.engine.persistence.components.planner;

import java.util.stream.Collectors;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditingAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditingAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Drop;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.immutables.value.Value.Default;
import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Style;

public abstract class Planner
{
    @Immutable
    @Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
    )
    public interface PlannerOptionsAbstract
    {
        @Default
        default boolean cleanupStagingData()
        {
            return true;
        }

        @Default
        default boolean collectStatistics()
        {
            return false;
        }

        @Default
        default boolean enableSchemaEvolution()
        {
            return false;
        }
    }

    private final Datasets datasets;
    private final IngestMode ingestMode;
    private final PlannerOptions plannerOptions;
    protected final List<String> primaryKeys;

    Planner(Datasets datasets, IngestMode ingestMode, PlannerOptions plannerOptions)
    {
        this.datasets = datasets;
        this.ingestMode = ingestMode;
        this.plannerOptions = plannerOptions == null ? PlannerOptions.builder().build() : plannerOptions;
        this.primaryKeys = findCommonPrimaryKeysBetweenMainAndStaging();
    }

    private List<String> findCommonPrimaryKeysBetweenMainAndStaging()
    {
        Set<String> primaryKeysFromMain = mainDataset().schema().fields().stream().filter(Field::primaryKey).map(Field::name).collect(Collectors.toSet());
        return stagingDataset().schema().fields().stream().filter(field -> field.primaryKey() && primaryKeysFromMain.contains(field.name())).map(Field::name).collect(Collectors.toList());
    }

    protected Dataset mainDataset()
    {
        return datasets.mainDataset();
    }

    protected Dataset stagingDataset()
    {
        return datasets.stagingDataset();
    }

    protected Optional<MetadataDataset> metadataDataset()
    {
        return datasets.metadataDataset();
    }

    protected IngestMode ingestMode()
    {
        return ingestMode;
    }

    protected PlannerOptions options()
    {
        return plannerOptions;
    }

    public abstract LogicalPlan buildLogicalPlanForIngest(Resources resources, Set<Capability> capabilities);

    public LogicalPlan buildLogicalPlanForMetadataIngest(Resources resources)
    {
        return null;
    }

    public abstract LogicalPlan buildLogicalPlanForPreActions(Resources resources);

    public LogicalPlan buildLogicalPlanForPostActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        // Drop table or clean table based on flags
        if (resources.externalDatasetImported())
        {
            operations.add(Drop.of(true, stagingDataset()));
        }
        else if (plannerOptions.cleanupStagingData())
        {
            operations.add(Delete.builder().dataset(stagingDataset()).build());
        }
        return LogicalPlan.of(operations);
    }

    public abstract Map<StatisticName, LogicalPlan> buildLogicalPlanForPreRunStatistics(Resources resources);

    public abstract Map<StatisticName, LogicalPlan> buildLogicalPlanForPostRunStatistics(Resources resources);

    protected void validatePrimaryKeysNotEmpty(List<String> primaryKeys)
    {
        if (primaryKeys.isEmpty())
        {
            throw new IllegalStateException("Primary key list must not be empty");
        }
    }

    protected void validatePrimaryKeysIsEmpty(List<String> primaryKeys)
    {
        if (!primaryKeys.isEmpty())
        {
            throw new IllegalStateException("Primary key list must be empty");
        }
    }

    // auditing visitor

    protected static final AuditEnabled AUDIT_ENABLED = new AuditEnabled();

    static class AuditEnabled implements AuditingVisitor<Boolean>
    {
        private AuditEnabled()
        {
        }

        @Override
        public Boolean visitNoAuditing(NoAuditingAbstract noAuditing)
        {
            return false;
        }

        @Override
        public Boolean visitDateTimeAuditing(DateTimeAuditingAbstract dateTimeAuditing)
        {
            return true;
        }
    }
}
