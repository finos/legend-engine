// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.deephaven.compiler;

import io.deephaven.api.ColumnName;
import io.deephaven.api.JoinAddition;
import io.deephaven.api.JoinMatch;
import io.deephaven.api.Pair;
import io.deephaven.api.Selectable;
import io.deephaven.api.SortColumn;
import io.deephaven.api.TableOperations;
import io.deephaven.api.TableOperationsDefaults;
import io.deephaven.api.expression.Expression;
import io.deephaven.api.filter.Filter;
import io.deephaven.api.filter.FilterAnd;
import io.deephaven.api.filter.FilterBase;
import io.deephaven.api.filter.FilterComparison;
import io.deephaven.api.filter.FilterIn;
import io.deephaven.api.filter.FilterNot;
import io.deephaven.api.filter.FilterOr;
import io.deephaven.api.literal.Literal;
import io.deephaven.api.literal.LiteralFilter;
import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.client.impl.BarrageSubscription;
import io.deephaven.client.impl.BarrageSnapshot;
import io.deephaven.client.impl.FlightSession;
import io.deephaven.client.impl.Session;
import io.deephaven.client.impl.ApplicationService;
import io.deephaven.client.impl.ConsoleService;
import io.deephaven.client.impl.InputTableService;
import io.deephaven.client.impl.ObjectService;
import io.deephaven.client.impl.ConfigService;
import io.deephaven.client.impl.TableService;
import io.deephaven.client.impl.TableHandleManager;
import io.deephaven.qst.TableCreator;
import io.deephaven.qst.table.TableSpec;
import io.deephaven.qst.table.TicketTable;
import io.deephaven.qst.table.TableBase;
import io.deephaven.qst.table.TableSchema;
import io.deephaven.qst.table.LabeledTables;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.plan.execution.stores.deephaven.specifics.IDeephavenExecutionNodeSpecifics;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilters;

import java.util.LinkedHashMap;
import java.util.Map;


public class DeephavenJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    static final Map<String, Class<?>> DEPENDENCIES = new LinkedHashMap<>();
    private static final String PURE_PACKAGE = "meta::external::store::deephaven::executionPlan::platformBinding::legendJava::";

    static
    {
        DEPENDENCIES.put("io.deephaven.api.ColumnName", ColumnName.class);
        DEPENDENCIES.put("io.deephaven.api.expression.Expression", Expression.class);
        DEPENDENCIES.put("io.deephaven.api.filter.Filter", Filter.class);
        DEPENDENCIES.put("io.deephaven.api.filter.FilterAnd", FilterAnd.class);
        DEPENDENCIES.put("io.deephaven.api.filter.FilterBase", FilterBase.class);
        DEPENDENCIES.put("io.deephaven.api.filter.FilterComparison", FilterComparison.class);
        DEPENDENCIES.put("io.deephaven.api.filter.FilterOr", FilterOr.class);
        DEPENDENCIES.put("io.deephaven.api.filter.FilterIn", FilterIn.class);
        DEPENDENCIES.put("io.deephaven.api.filter.FilterNot", FilterNot.class);
        DEPENDENCIES.put("io.deephaven.api.JoinAddition", JoinAddition.class);
        DEPENDENCIES.put("io.deephaven.api.JoinMatch", JoinMatch.class);
        DEPENDENCIES.put("io.deephaven.api.literal.Literal", Literal.class);
        DEPENDENCIES.put("io.deephaven.api.literal.LiteralFilter", LiteralFilter.class);
        DEPENDENCIES.put("io.deephaven.api.Pair", Pair.class);
        DEPENDENCIES.put("io.deephaven.api.Selectable", Selectable.class);
        DEPENDENCIES.put("io.deephaven.api.SortColumn", SortColumn.class);
        DEPENDENCIES.put("io.deephaven.api.TableOperations", TableOperations.class);
        DEPENDENCIES.put("io.deephaven.api.TableOperationsDefaults", TableOperationsDefaults.class);

        DEPENDENCIES.put("io.deephaven.client.impl.ApplicationService", ApplicationService.class);
        DEPENDENCIES.put("io.deephaven.client.impl.BarrageSession", BarrageSession.class);
        DEPENDENCIES.put("io.deephaven.client.impl.BarrageSubscription", BarrageSubscription.class);
        DEPENDENCIES.put("io.deephaven.client.impl.BarrageSnapshot", BarrageSnapshot.class);
        DEPENDENCIES.put("io.deephaven.client.impl.ConfigService", ConfigService.class);
        DEPENDENCIES.put("io.deephaven.client.impl.ConsoleService", ConsoleService.class);
        DEPENDENCIES.put("io.deephaven.client.impl.FlightSession", FlightSession.class);
        DEPENDENCIES.put("io.deephaven.client.impl.InputTableService", InputTableService.class);
        DEPENDENCIES.put("io.deephaven.client.impl.ObjectService", ObjectService.class);
        DEPENDENCIES.put("io.deephaven.client.impl.Session", Session.class);
        DEPENDENCIES.put("io.deephaven.client.impl.TableHandle", TableHandle.class);
        DEPENDENCIES.put("io.deephaven.client.impl.TableHandleManager", TableHandleManager.class);
        DEPENDENCIES.put("io.deephaven.client.impl.TableService", TableService.class);

        DEPENDENCIES.put("io.deephaven.qst.TableCreator", TableCreator.class);
        DEPENDENCIES.put("io.deephaven.qst.table.LabeledTables", LabeledTables.class);
        DEPENDENCIES.put("io.deephaven.qst.table.TableBase", TableBase.class);
        DEPENDENCIES.put("io.deephaven.qst.table.TableSchema", TableSchema.class);
        DEPENDENCIES.put("io.deephaven.qst.table.TableSpec", TableSpec.class);
        DEPENDENCIES.put("io.deephaven.qst.table.TicketTable", TicketTable.class);

        DEPENDENCIES.put(PURE_PACKAGE + "IDeephavenExecutionNodeSpecifics", IDeephavenExecutionNodeSpecifics.class);
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Deephaven");
    }

    @Override
    public ClassPathFilter getExtraClassPathFilter()
    {
        return ClassPathFilters.fromClasses(DEPENDENCIES.values());
    }
}
