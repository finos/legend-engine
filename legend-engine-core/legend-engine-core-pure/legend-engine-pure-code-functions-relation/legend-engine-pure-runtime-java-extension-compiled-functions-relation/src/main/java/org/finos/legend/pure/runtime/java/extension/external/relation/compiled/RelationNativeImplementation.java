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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled;

import io.deephaven.csv.parsers.DataType;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.finos.legend.pure.generated.CoreGen;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.*;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.ColumnValue;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Frame;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Range;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.RangeInterval;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window;

import java.util.Objects;

import static org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS.readCsv;

public class RelationNativeImplementation
{
    public static TestTDSCompiled getTDS(Object value, ExecutionSupport es)
    {
        if (value instanceof TDSRelationAccessor)
        {
            return getTDS(((TDSRelationAccessor<?>) value)._sourceElement(), es);
        }
        return value instanceof TDSContainer ?
                ((TDSContainer) value).tds :
                new TestTDSCompiled(readCsv((((CoreInstance) value).getValueForMetaPropertyToOne("csv")).getName()), ((CoreInstance) value).getValueForMetaPropertyToOne(M3Properties.classifierGenericType), ((CompiledExecutionSupport) es).getProcessorSupport());
    }


    public static <T> RichIterable<Column<?, ?>> columns(Relation<? extends T> t)
    {
        return ((RelationType) t._classifierGenericType()._typeArguments().getFirst()._rawType())._columns();
    }

    public static <T, V> RichIterable<V> map(Relation<? extends T> rel, Function2<RowContainer, ExecutionSupport, RichIterable<V>> pureFunction, ExecutionSupport es)
    {
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);
        MutableList list = Lists.mutable.empty();
        for (int i = 0; i < tds.getRowCount(); i++)
        {
            list.add(pureFunction.value(new RowContainer(tds, i), es));
        }
        return list;
    }

    public static <T> Relation<? extends T> distinct(Relation<? extends T> rel, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel, es).distinct(RelationNativeImplementation.getTDS(rel, es).getColumnNames()), ps);
    }

    public static <T> Relation<? extends T> distinct(Relation<? extends T> rel, ColSpecArray<?> columns, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel, es).distinct((MutableList) columns._names().toList()), ps);
    }

    public static <T> Long size(Relation<? extends T> res, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(res, es).getRowCount();
    }

    public static <T> Relation<? extends T> limit(Relation<? extends T> rel, long size, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel, es).slice(0, (int) size), ps);
    }

    public static <T> Relation<? extends T> slice(Relation<? extends T> rel, long start, long stop, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel, es).slice((int) start, (int) stop), ps);
    }

    public static <T> Relation<? extends T> drop(Relation<? extends T> relation, Long aLong, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(relation, es);
        return new TDSContainer((TestTDSCompiled) tds.slice(aLong.intValue(), (int) tds.getRowCount()), ps);
    }

    public static <T> Relation<? extends Object> rename(Relation<? extends T> r, ColSpec<?> old, ColSpec<?> aNew, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(r, es).rename(old._name(), aNew._name()), ps);
    }

    public static <T> Relation<? extends Object> select(Relation<? extends T> r, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(r, es).select(Lists.mutable.withAll(RelationNativeImplementation.getTDS(r, es).getColumnNames())), ps);
    }

    public static <T> Relation<? extends Object> select(Relation<? extends T> r, ColSpec<?> col, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(r, es).select(Lists.mutable.with(col._name())), ps);
    }

    public static <T> Relation<? extends Object> select(Relation<? extends T> r, ColSpecArray<?> cols, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(r, es).select(Lists.mutable.withAll(cols._names())), ps);
    }

    public static <T> Relation<? extends T> concatenate(Relation<? extends T> rel1, Relation<? extends T> rel2, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel1, es).concatenate(RelationNativeImplementation.getTDS(rel2, es)), ps);
    }

    public static <T> Relation<? extends T> filter(Relation<? extends T> rel, Function2 pureFunction, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);
        MutableIntSet list = new IntHashSet();
        for (int i = 0; i < tds.getRowCount(); i++)
        {
            if (!(boolean) pureFunction.value(new RowContainer(tds, i), es))
            {
                list.add(i);
            }
        }
        return new TDSContainer((TestTDSCompiled) tds.drop(list), ps);
    }

    public static <T> T offset(Relation<? extends T> w, T r, long offset, ExecutionSupport es)
    {
        int actualOffset = ((RowContainer) r).getRow() + (int) offset;
        if (actualOffset < 0 || actualOffset >= ((TDSContainer) w).tds.getRowCount())
        {
            return (T) new NullRowContainer();
        }
        return (T) new RowContainer(RelationNativeImplementation.getTDS(w, es), actualOffset);
    }

    public static <T, V> Relation<? extends Object> asOfJoin(Relation<? extends T> rel1, Relation<? extends V> rel2, Function3 pureFunction, LambdaFunction<?> _func, ExecutionSupport es)
    {
        return asOfJoin(rel1, rel2, pureFunction, _func, null, es);
    }

    public static <T, V> Relation<? extends Object> asOfJoin(Relation<? extends T> rel1, Relation<? extends V> rel2, Function3 matchFunction, LambdaFunction<?> _func, Function3 onFunction, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();

        TestTDS tds1 = RelationNativeImplementation.getTDS(rel1, es).sortForOuterJoin(true, _func, ps);
        TestTDS tds2 = RelationNativeImplementation.getTDS(rel2, es).sortForOuterJoin(false, _func, ps);

        TestTDS result = tds1.join(tds2).newEmptyTDS();
        for (int i = 0; i < tds1.getRowCount(); i++)
        {
            TestTDS oneRow = tds1.slice(i, i + 1);
            TestTDS exploded = oneRow.join(tds2);
            TestTDS res = filterTwoParam(exploded, matchFunction, es);
            res = onFunction == null ? res : filterTwoParam(res, onFunction, es);
            if (res.getRowCount() == 0)
            {
                result = result.concatenate(oneRow.join(tds2.newNullTDS()));
            }
            else
            {
                result = result.concatenate(res.slice(0, 1));
            }
        }
        return new TDSContainer((TestTDSCompiled) result, ps);
    }

    private static TestTDS filterTwoParam(TestTDS tds, Function3 matchFunction, ExecutionSupport es)
    {
        MutableIntSet list = new IntHashSet();
        for (int i = 0; i < tds.getRowCount(); i++)
        {
            RowContainer rc = new RowContainer((TestTDSCompiled) tds, i);
            if (!(boolean) matchFunction.value(rc, rc, es))
            {
                list.add(i);
            }
        }
        return tds.drop(list);
    }


    public abstract static class ColFuncSpecTrans
    {
        public String newColName;
        public String columnType;

        public ColFuncSpecTrans(String newColName, String columnType)
        {
            this.newColName = newColName;
            this.columnType = columnType;
        }

        public abstract Object eval(Object partition, Object frame, Object row, ExecutionSupport es);
    }

    public static class ColFuncSpecTrans1 extends ColFuncSpecTrans
    {
        public SharedPureFunction func;

        public ColFuncSpecTrans1(String newColName, SharedPureFunction func, String columnType)
        {
            super(newColName, columnType);
            this.func = func;
        }

        @Override
        public Object eval(Object partition, Object frame, Object row, ExecutionSupport es)
        {
            return func.execute(Lists.mutable.with(row), es);
        }
    }

    public static class ColFuncSpecTrans2 extends ColFuncSpecTrans
    {
        public SharedPureFunction func;

        public ColFuncSpecTrans2(String newColName, SharedPureFunction func, String columnType)
        {
            super(newColName, columnType);
            this.func = func;
        }

        @Override
        public Object eval(Object partition, Object frame, Object row, ExecutionSupport es)
        {
            return func.execute(Lists.mutable.with(partition, frame, row), es);
        }
    }

    public static <T> Relation<? extends Object> projectExtend(Relation<? extends T> rel, MutableList<ColFuncSpecTrans1> colFuncSpecTrans, boolean includeExistingColumns, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);
        TestTDSCompiled target = includeExistingColumns ? tds : (TestTDSCompiled) tds.removeColumns(tds.getColumnNames().toSet());

        return new TDSContainer((TestTDSCompiled) colFuncSpecTrans.injectInto((TestTDS) target, (accTDS, colFuncSpec) -> accTDS.addColumn(performExtend(new Window(), tds.wrapFullTDS(), colFuncSpec, es))), ps);
    }

    public static <T> Relation<?> extendAgg(Relation<? extends T> rel, MutableList<AggColSpecTrans1> aggColSpecTrans, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);
        return new TDSContainer((TestTDSCompiled) aggregateTDS(null, Lists.fixedSize.empty(), tds.wrapFullTDS(), aggColSpecTrans, false, es).injectInto((TestTDS) tds, TestTDS::addColumn), ps);
    }

    public static <T> Relation<? extends T> extendWinFunc(Relation<? extends T> rel, Window window, MutableList<ColFuncSpecTrans2> colFunc, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes = tds.sort(window.getPartition().collect(part -> new SortInfo(part, SortDirection.ASC)).toList());
        final Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedPartitions = TestTDS.sortPartitions(window.getSorts(), sortRes);

        return new TDSContainer((TestTDSCompiled) colFunc.injectInto(sortedPartitions.getOne(), (a, b) -> a.addColumn(performExtend(window, sortedPartitions, b, es))), ps);
    }

    public static <T> Relation<? extends T> extendWinAgg(Relation<? extends T> rel, Window window, MutableList<AggColSpecTrans2> aggColSpecTrans, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes = window.getPartition().isEmpty() ? tds.wrapFullTDS() : tds.sort(window.getPartition().collect(part -> new SortInfo(part, SortDirection.ASC)).toList());
        MutableList<SortInfo> sortInfos = window.getSorts();
        final Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedPartitions = TestTDS.sortPartitions(sortInfos, sortRes);

        return new TDSContainer((TestTDSCompiled) aggregateTDS(window, sortInfos, sortedPartitions, aggColSpecTrans, false, es).injectInto(sortedPartitions.getOne(), TestTDS::addColumn), ps);
    }


    private static ColumnValue performExtend(Window window, Pair<TestTDS, MutableList<Pair<Integer, Integer>>> tds, ColFuncSpecTrans colFuncSpecTrans, ExecutionSupport es)
    {
        long size = tds.getOne().getRowCount();
        boolean[] nulls = new boolean[(int) size];
        switch (colFuncSpecTrans.columnType)
        {
            case M3Paths.String:
                MutableList<String> res = Lists.mutable.empty();
                extracted(tds, window, colFuncSpecTrans, es, (i, val) -> res.add((String) val));
                return new ColumnValue(colFuncSpecTrans.newColName, DataType.STRING, res.toArray(new String[0]));
            case M3Paths.Integer:
                long[] resultLong = new long[(int) size];
                extracted(tds, window, colFuncSpecTrans, es, (i, val) -> processWithNull(i, val, nulls, () -> resultLong[i] = (long) val));
                return new ColumnValue(colFuncSpecTrans.newColName, DataType.LONG, resultLong, nulls);
            case M3Paths.Float:
                double[] resultDouble = new double[(int) size];
                extracted(tds, window, colFuncSpecTrans, es, (i, val) -> processWithNull(i, val, nulls, () -> resultDouble[i] = (double) val));
                return new ColumnValue(colFuncSpecTrans.newColName, DataType.DOUBLE, resultDouble, nulls);
            case M3Paths.Variant:
            case "Variant":
                MutableList<Variant> variantRes = Lists.mutable.empty();
                extracted(tds, window, colFuncSpecTrans, es, (i, val) -> variantRes.add((Variant) val));
                return new ColumnValue(colFuncSpecTrans.newColName, DataType.CUSTOM, variantRes.toArray(new Variant[0]));
            default:
                throw new RuntimeException(colFuncSpecTrans.columnType + " not supported yet");
        }
    }

    private static void processWithNull(Integer j, Object val, boolean[] nulls, Proc p)
    {
        {
            if (val == null)
            {
                nulls[j] = true;
            }
            else
            {
                p.invoke();
            }
        }
    }

    private interface Proc
    {
        void invoke();
    }

    private static void extracted(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> tds, Window window, ColFuncSpecTrans colFuncSpecTrans, ExecutionSupport es, Procedure2<Integer, Object> func)
    {
        int size = tds.getTwo().size();
        int k = 0;
        Object frame = window.convert(((CompiledExecutionSupport) es).getProcessorSupport(), new CompiledPrimitiveHandler());
        for (int j = 0; j < size; j++)
        {
            Pair<Integer, Integer> r = tds.getTwo().get(j);
            TDSContainer winTDS = new TDSContainer((TestTDSCompiled) tds.getOne().slice(r.getOne(), r.getTwo()), ((CompiledExecutionSupport) es).getProcessorSupport());

            for (int i = 0; i < r.getTwo() - r.getOne(); i++)
            {
                func.value(k++, colFuncSpecTrans.eval(winTDS, frame, new RowContainer((TestTDSCompiled) tds.getOne(), i), es));
            }
        }
    }


    public static <T, V> Relation<? extends Object> join(Relation<? extends T> rel1, Relation<? extends V> rel2, Enum joinKind, Function3 pureFunction, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();

        TestTDSCompiled tds1 = RelationNativeImplementation.getTDS(rel1, es);
        TestTDSCompiled tds2 = RelationNativeImplementation.getTDS(rel2, es);
        TestTDSCompiled tds = (TestTDSCompiled) tds1.join(tds2);

        MutableIntSet list = new IntHashSet();
        for (int i = 0; i < tds.getRowCount(); i++)
        {
            if (!(boolean) pureFunction.value(new RowContainer(tds, i), new RowContainer(tds, i), es))
            {
                list.add(i);
            }
        }
        TestTDSCompiled filtered = (TestTDSCompiled) tds.drop(list);
        if (joinKind.getName().equals("LEFT"))
        {
            filtered = (TestTDSCompiled) tds1.compensateLeft(filtered);
        }

        return new TDSContainer(filtered, ps);
    }

    public static <T> Relation<? extends T> sort(Relation<? extends T> rel, RichIterable<Pair<Enum, String>> collect, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds1 = RelationNativeImplementation.getTDS(rel, es);
        return new TDSContainer((TestTDSCompiled) tds1.sort(collect.collect(c -> new SortInfo(c.getTwo(), SortDirection.valueOf(c.getOne()._name()))).toList()).getOne(), ps);
    }

    public abstract static class AggColSpecTrans
    {
        public String newColName;
        public Function2 reduce;
        public String reduceType;

        public AggColSpecTrans(String newColName, Function2 reduce, String reduceType)
        {
            this.newColName = newColName;
            this.reduce = reduce;
            this.reduceType = reduceType;
        }

        public abstract Object eval(Object partition, Object frame, Object row, ExecutionSupport es);
    }

    public static class AggColSpecTrans1 extends AggColSpecTrans
    {
        public SharedPureFunction map;
        public AggColSpec<?, ?, ?> aggColSpec = null;

        public AggColSpecTrans1(String newColName, SharedPureFunction map, Function2 reduce, String reduceType)
        {
            super(newColName, reduce, reduceType);
            this.map = map;
        }

        public AggColSpecTrans1(String newColName, SharedPureFunction map, Function2 reduce, String reduceType, AggColSpec<?, ?, ?> aggColSpec)
        {
            this(newColName, map, reduce, reduceType);
            this.aggColSpec = aggColSpec;
        }

        @Override
        public Object eval(Object partition, Object frame, Object row, ExecutionSupport es)
        {
            return map.execute(Lists.mutable.with(row), es);
        }
    }

    public static class AggColSpecTrans2 extends AggColSpecTrans
    {
        public SharedPureFunction map;

        public AggColSpecTrans2(String newColName, SharedPureFunction map, Function2 reduce, String reduceType)
        {
            super(newColName, reduce, reduceType);
            this.map = map;
        }

        @Override
        public Object eval(Object partition, Object frame, Object row, ExecutionSupport es)
        {
            return map.execute(Lists.mutable.with(partition, frame, row), es);
        }
    }

    public static <T> Relation<? extends Object> groupBy(Relation<? extends T> rel, MutableList<AggColSpecTrans1> aggColSpecTrans, ExecutionSupport es)
    {
        return groupBy(rel, Lists.mutable.empty(), aggColSpecTrans, es);
    }

    public static <T> Relation<? extends Object> groupBy(Relation<? extends T> rel, ColSpec<?> cols, MutableList<AggColSpecTrans1> aggColSpecTrans, ExecutionSupport es)
    {
        return groupBy(rel, Lists.mutable.with(cols._name()), aggColSpecTrans, es);
    }

    public static <T> Relation<? extends Object> groupBy(Relation<? extends T> rel, ColSpecArray<?> cols, MutableList<AggColSpecTrans1> aggColSpecTransAll, ExecutionSupport es)
    {
        return groupBy(rel, Lists.mutable.withAll(cols._names()), aggColSpecTransAll, es);
    }

    private static <T> Relation<? extends Object> groupBy(Relation<? extends T> rel, MutableList<String> cols, MutableList<AggColSpecTrans1> aggColSpecTransAll, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes = cols.isEmpty() ? tds.wrapFullTDS() : tds.sort(cols.collect(name -> new SortInfo(name, SortDirection.ASC)).toList());

        MutableSet<String> columnsToRemove = tds.getColumnNames().clone().toSet();
        columnsToRemove.removeAll(cols.toSet());
        TestTDS distinctTDS = sortRes.getOne()._distinct(sortRes.getTwo()).removeColumns(columnsToRemove);

        return new TDSContainer((TestTDSCompiled) aggregateTDS(null, Lists.fixedSize.empty(), sortRes, aggColSpecTransAll, true, es).injectInto(distinctTDS, TestTDS::addColumn), ps);
    }

    public static <T> Relation<? extends Object> pivot(Relation<? extends T> rel, ColSpec<?> pivotCols, MutableList<AggColSpecTrans1> aggColSpecTrans, ExecutionSupport es)
    {
        return pivot(rel, Lists.mutable.with(pivotCols._name()), aggColSpecTrans, es);
    }

    public static <T> Relation<? extends Object> pivot(Relation<? extends T> rel, ColSpecArray<?> pivotCols, MutableList<AggColSpecTrans1> aggColSpecTransAll, ExecutionSupport es)
    {
        return pivot(rel, Lists.mutable.withAll(pivotCols._names()), aggColSpecTransAll, es);
    }

    private static <T> Relation<? extends Object> pivot(Relation<? extends T> rel, MutableList<String> pivotCols, MutableList<AggColSpecTrans1> aggColSpecTransAll, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);

        ListIterable<String> columnsUsedInAggregation = aggColSpecTransAll.flatCollect(col ->
        {
            LambdaFunction<?> lambdaFunction = (LambdaFunction<?>) col.aggColSpec._map();
            return getColumnsUsedInAggregation(lambdaFunction._expressionSequence());
        }).select(Objects::nonNull);

        // these are the columns not being aggregated on, which will be used for groupBy calculation before transposing
        ListIterable<String> groupByColumns = tds.getColumnNames().reject(c -> columnsUsedInAggregation.anySatisfy(a -> a.equals(c)) || pivotCols.anySatisfy(a -> a.equals(c))).withAll(pivotCols);

        // create the big group-by table by processing all aggregations
        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sorted = tds.sort(groupByColumns.collect(c -> new SortInfo(c, SortDirection.ASC)));
        TestTDSCompiled temp = (TestTDSCompiled) sorted.getOne()._distinct(sorted.getTwo());
        temp = aggColSpecTransAll.injectInto(temp, (existing, aggColSpecTrans) ->
        {
            int size = sorted.getTwo().size();
            switch (aggColSpecTrans.reduceType)
            {
                case M3Paths.String:
                {
                    String[] finalRes = new String[size];
                    performMapReduce(null, Lists.fixedSize.empty(), aggColSpecTrans, aggColSpecTrans.reduce, es, sorted, (j, o) -> finalRes[j] = (String) o, true);
                    existing.addColumn(aggColSpecTrans.newColName, DataType.STRING, finalRes);
                    break;
                }
                case M3Paths.Integer:
                {
                    long[] finalResLong = new long[size];
                    performMapReduce(null, Lists.fixedSize.empty(), aggColSpecTrans, aggColSpecTrans.reduce, es, sorted, (j, o) -> finalResLong[j] = (long) o, true);
                    existing.addColumn(aggColSpecTrans.newColName, DataType.LONG, finalResLong);
                    break;
                }
                case M3Paths.Float:
                {
                    double[] finalResDouble = new double[size];
                    performMapReduce(null, Lists.fixedSize.empty(), aggColSpecTrans, aggColSpecTrans.reduce, es, sorted, (j, o) -> finalResDouble[j] = (double) o, true);
                    existing.addColumn(aggColSpecTrans.newColName, DataType.DOUBLE, finalResDouble);
                    break;
                }
            }

            return existing;
        });

        // transposing the table to complete pivoting
        TestTDSCompiled result = (TestTDSCompiled) temp.applyPivot(groupByColumns.reject(pivotCols::contains), pivotCols, aggColSpecTransAll.collect(col -> col.newColName));

        return new TDSContainer(result, ps);
    }

    private static MutableList<String> getColumnsUsedInAggregation(RichIterable<? extends ValueSpecification> lambdas)
    {
        MutableList<String> columnsUsedInAggregation = org.eclipse.collections.impl.factory.Lists.mutable.empty();
        recursivelyCollectColumnsUsedInAggregation(lambdas, columnsUsedInAggregation);
        return columnsUsedInAggregation;
    }

    private static void recursivelyCollectColumnsUsedInAggregation(RichIterable<? extends ValueSpecification> lambdas, MutableList<String> columnsUsedInAggregation)
    {
        for (ValueSpecification lambda : lambdas)
        {
            if (lambda instanceof SimpleFunctionExpression)
            {
                SimpleFunctionExpression fe = (SimpleFunctionExpression) lambda;
                if (fe._func() instanceof Column)
                {
                    Column column = (Column) fe._func();
                    columnsUsedInAggregation.add(column._name());
                }

                else
                {
                    recursivelyCollectColumnsUsedInAggregation(fe._parametersValues(), columnsUsedInAggregation);
                }
            }

            else if (lambda instanceof InstanceValue)
            {
                InstanceValue instanceValue = (InstanceValue) lambda;
                RichIterable<? extends ValueSpecification> values = instanceValue._values().selectInstancesOf(ValueSpecification.class);
                recursivelyCollectColumnsUsedInAggregation(values, columnsUsedInAggregation);
            }
        }
    }

    public static <T> Long write(Relation<? extends T> rel, RelationElementAccessor<? extends T> relationElementAccessor, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled sourceTds = RelationNativeImplementation.getTDS(rel, es);
        if (!(relationElementAccessor instanceof TDSRelationAccessor))
        {
            throw new RuntimeException("Only source element of type meta::pure::metamodel::relation::TDSRelationAccessor is supported");
        }
        TestTDSCompiled targetTds = RelationNativeImplementation.getTDS(relationElementAccessor, es);
        TestTDSCompiled resultTds = (TestTDSCompiled) targetTds.concatenate(sourceTds);
        relationElementAccessor._sourceElement(new TDSContainer(resultTds, ps));
        return sourceTds.getRowCount();
    }

    private static MutableList<ColumnValue> aggregateTDS(Window window, MutableList<SortInfo> sortInfos, Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes, MutableList<? extends AggColSpecTrans> aggColSpecTransAll, boolean compress, ExecutionSupport es)
    {
        int size = compress ? sortRes.getTwo().size() : (int) sortRes.getOne().getRowCount();
        boolean[] nulls = new boolean[(int) size];
        MutableList<ColumnValue> columnValues = Lists.mutable.empty();
        for (AggColSpecTrans aggColSpecTrans : aggColSpecTransAll)
        {
            switch (aggColSpecTrans.reduceType)
            {
                case M3Paths.String:
                    String[] finalRes = new String[size];
                    performMapReduce(window, sortInfos, aggColSpecTrans, aggColSpecTrans.reduce, es, sortRes, (j, o) -> finalRes[j] = (String) o, compress);
                    columnValues.add(new ColumnValue(aggColSpecTrans.newColName, DataType.STRING, finalRes));
                    break;
                case M3Paths.Integer:
                    long[] finalResLong = new long[size];
                    performMapReduce(window, sortInfos, aggColSpecTrans, aggColSpecTrans.reduce, es, sortRes, (j, o) -> processWithNull(j, o, nulls, () -> finalResLong[j] = (long) o), compress);
                    columnValues.add(new ColumnValue(aggColSpecTrans.newColName, DataType.LONG, finalResLong, nulls));
                    break;
                case M3Paths.Float:
                case M3Paths.Number:
                    double[] finalResDouble = new double[size];
                    performMapReduce(window, sortInfos, aggColSpecTrans, aggColSpecTrans.reduce, es, sortRes, (j, o) -> processWithNull(j, o, nulls, () -> finalResDouble[j] = (double) o), compress);
                    columnValues.add(new ColumnValue(aggColSpecTrans.newColName, DataType.DOUBLE, finalResDouble, nulls));
                    break;
                default:
                    throw new RuntimeException(aggColSpecTrans.reduceType + " is not supported yet!");
            }
        }
        return columnValues;
    }

    private static void performMapReduce(Window window, MutableList<SortInfo> sortInfos, AggColSpecTrans map, Function2 reduce, ExecutionSupport es, Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes, Procedure2<Integer, Object> setter, boolean compress)
    {
        int cursor = 0;
        int size = sortRes.getTwo().size();
        CompiledPrimitiveHandler compiledPrimitiveHandler = new CompiledPrimitiveHandler();
        for (int j = 0; j < size; j++)
        {
            Pair<Integer, Integer> r = sortRes.getTwo().get(j);
            MutableList<Object> subList = Lists.mutable.empty();
            Integer partitionStartIndex = r.getOne();
            Integer partitionEndIndex = r.getTwo();
            int partitionSize = partitionEndIndex - partitionStartIndex;
            TestTDSCompiled sourceTDS = (TestTDSCompiled) sortRes.getOne().slice(partitionStartIndex, partitionEndIndex);
            TDSContainer winTDS = new TDSContainer(sourceTDS, ((CompiledExecutionSupport) es).getProcessorSupport());
            Object convertedFrame = window == null ? null : window.convert(((CompiledExecutionSupport) es).getProcessorSupport(), compiledPrimitiveHandler);
            Frame frame = window == null ? null : window.getFrame();
            if (window != null && (frame instanceof Range || frame instanceof RangeInterval))
            {
                if (sortInfos.size() != 1)
                {
                    throw new RuntimeException("There must be exactly one sort info for range frame, but found " + sortInfos.size());
                }
                SortInfo sortInfo = sortInfos.get(0);
                String orderByColumnName = sortInfo.getColumnName();
                SortDirection sortDirection = sortInfo.getDirection();
                Number offsetFrom = frame.getOffsetFrom();
                Number offsetTo = frame.getOffsetTo(0);
                Enum offsetFromDurationUnit = null;
                Enum offsetToDurationUnit = null;
                if (frame instanceof RangeInterval)
                {
                    RangeInterval rangeInterval = (RangeInterval) frame;
                    offsetFromDurationUnit = rangeInterval.getOffsetFromDurationUnit();
                    offsetToDurationUnit = rangeInterval.getOffsetToDurationUnit();
                }
                MutableList<Object> orderByValues = Lists.mutable.empty();
                for (int i = 0; i < partitionSize; i++)
                {
                    Object orderByRowValue = sourceTDS.getValueAsCoreInstance(orderByColumnName, i);
                    orderByValues.add(orderByRowValue);
                    Object res = map.eval(winTDS, convertedFrame, new RowContainer(winTDS.tds, i), es);
                    subList.add(res);
                }

                for (int i = 0; i < partitionSize; i++)
                {
                    MutableList<Object> aggregationValues = Lists.mutable.empty();
                    Object orderByCurrentRowValue = orderByValues.get(i);
                    for (int k = 0; k < partitionSize; k++)
                    {
                        Object currentPartitionValueAsObject = orderByValues.get(k);
                        Object aggregateValue = subList.get(k);
                        if (orderByCurrentRowValue == null)
                        {
                            if (currentPartitionValueAsObject == null) // Rows with NULL in the ORDER BY column are included in frame boundary only when the ORDER BY value of the current row is NULL.
                            {
                                aggregationValues.add(aggregateValue);
                            }
                        }
                        else if (orderByCurrentRowValue instanceof Number)
                        {
                            Number currentRowValue = (Number) orderByCurrentRowValue;
                            if (offsetFrom == null && offsetTo == null) // RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
                            {
                                aggregationValues.add(aggregateValue);
                            }
                            else if (offsetFrom == null) // RANGE BETWEEN UNBOUNDED PRECEDING AND N PRECEDING/FOLLOWING
                            {
                                if (sortDirection == SortDirection.ASC)
                                {
                                    Number upperBound = compiledPrimitiveHandler.plus(currentRowValue, offsetTo);
                                    if (currentPartitionValueAsObject != null)
                                    {
                                        Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                        if (compiledPrimitiveHandler.lessThanEqual(currentPartitionValue, upperBound))
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                                else
                                {
                                    Number lowerBound = compiledPrimitiveHandler.minus(currentRowValue, offsetTo);
                                    if (currentPartitionValueAsObject == null) // When the ORDER BY clause specifies NULLS FIRST, which is default when sort order is DESC, rows with NULL in the ORDER BY column are included in UNBOUNDED PRECEDING frames.
                                    {
                                        aggregationValues.add(aggregateValue);
                                    }
                                    else
                                    {
                                        Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                        if (compiledPrimitiveHandler.lessThanEqual(lowerBound, currentPartitionValue))
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                            }
                            else if (offsetTo == null) // RANGE BETWEEN N PRECEDING/FOLLOWING AND UNBOUNDED FOLLOWING
                            {
                                if (sortDirection == SortDirection.ASC)
                                {
                                    Number lowerBound = compiledPrimitiveHandler.plus(currentRowValue, offsetFrom);
                                    if (currentPartitionValueAsObject == null) // When the ORDER BY clause specifies NULLS LAST, which is default when sort order is ASC, rows with NULL in the ORDER BY column are included in UNBOUNDED FOLLOWING frames.
                                    {
                                        aggregationValues.add(aggregateValue);
                                    }
                                    else
                                    {
                                        Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                        if (compiledPrimitiveHandler.lessThanEqual(lowerBound, currentPartitionValue))
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                                else
                                {
                                    Number upperBound = compiledPrimitiveHandler.minus(currentRowValue, offsetFrom);
                                    if (currentPartitionValueAsObject != null)
                                    {
                                        Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                        if (compiledPrimitiveHandler.lessThanEqual(currentPartitionValue, upperBound))
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                            }
                            else // RANGE BETWEEN N PRECEDING/FOLLOWING AND N PRECEDING/FOLLOWING
                            {
                                Number lowerBound = sortDirection == SortDirection.ASC ? compiledPrimitiveHandler.plus(currentRowValue, offsetFrom) : compiledPrimitiveHandler.minus(currentRowValue, offsetTo);
                                Number upperBound = sortDirection == SortDirection.ASC ? compiledPrimitiveHandler.plus(currentRowValue, offsetTo) : compiledPrimitiveHandler.minus(currentRowValue, offsetFrom);
                                if (currentPartitionValueAsObject != null)
                                {
                                    Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                    if (compiledPrimitiveHandler.lessThanEqual(lowerBound, currentPartitionValue) && compiledPrimitiveHandler.lessThanEqual(currentPartitionValue, upperBound))
                                    {
                                        aggregationValues.add(aggregateValue);
                                    }
                                }
                            }
                        }
                        else if (frame instanceof RangeInterval && orderByCurrentRowValue instanceof PureDate)
                        {
                            PureDate currentRowValue = (PureDate) orderByCurrentRowValue;
                            if (offsetFrom == null && offsetTo == null) // RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
                            {
                                aggregationValues.add(aggregateValue);
                            }
                            else if (offsetFrom == null) // RANGE BETWEEN UNBOUNDED PRECEDING AND INTERVAL N PRECEDING/FOLLOWING
                            {
                                if (sortDirection == SortDirection.ASC)
                                {
                                    PureDate upperBound = CoreGen.adjustDate(currentRowValue, offsetTo.longValue(), offsetToDurationUnit);
                                    if (currentPartitionValueAsObject != null)
                                    {
                                        PureDate currentPartitionValue = (PureDate) currentPartitionValueAsObject;
                                        if (currentPartitionValue.compareTo(upperBound) <= 0)
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                                else
                                {
                                    PureDate lowerBound = CoreGen.adjustDate(currentRowValue, compiledPrimitiveHandler.minus(0, offsetTo).longValue(), offsetToDurationUnit);
                                    if (currentPartitionValueAsObject == null) // When the ORDER BY clause specifies NULLS FIRST, which is default when sort order is DESC, rows with NULL in the ORDER BY column are included in UNBOUNDED PRECEDING frames.
                                    {
                                        aggregationValues.add(aggregateValue);
                                    }
                                    else
                                    {
                                        PureDate currentPartitionValue = (PureDate) currentPartitionValueAsObject;
                                        if (lowerBound.compareTo(currentPartitionValue) <= 0)
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                            }
                            else if (offsetTo == null) // RANGE BETWEEN INTERVAL N PRECEDING/FOLLOWING AND UNBOUNDED FOLLOWING
                            {
                                if (sortDirection == SortDirection.ASC)
                                {
                                    PureDate lowerBound = CoreGen.adjustDate(currentRowValue, offsetFrom.longValue(), offsetFromDurationUnit);
                                    if (currentPartitionValueAsObject == null) // When the ORDER BY clause specifies NULLS LAST, which is default when sort order is ASC, rows with NULL in the ORDER BY column are included in UNBOUNDED FOLLOWING frames.
                                    {
                                        aggregationValues.add(aggregateValue);
                                    }
                                    else
                                    {
                                        PureDate currentPartitionValue = (PureDate) currentPartitionValueAsObject;
                                        if (lowerBound.compareTo(currentPartitionValue) <= 0)
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                                else
                                {
                                    PureDate upperBound = CoreGen.adjustDate(currentRowValue, compiledPrimitiveHandler.minus(0, offsetFrom).longValue(), offsetFromDurationUnit);
                                    if (currentPartitionValueAsObject != null)
                                    {
                                        PureDate currentPartitionValue = (PureDate) currentPartitionValueAsObject;
                                        if (currentPartitionValue.compareTo(upperBound) <= 0)
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                            }
                            else // RANGE BETWEEN INTERVAL N PRECEDING/FOLLOWING AND INTERVAL N PRECEDING/FOLLOWING
                            {
                                PureDate lowerBound = sortDirection == SortDirection.ASC ? CoreGen.adjustDate(currentRowValue, offsetFrom.longValue(), offsetFromDurationUnit)
                                        : CoreGen.adjustDate(currentRowValue, compiledPrimitiveHandler.minus(0, offsetTo).longValue(), offsetToDurationUnit);
                                PureDate upperBound = sortDirection == SortDirection.ASC ? CoreGen.adjustDate(currentRowValue, offsetTo.longValue(), offsetToDurationUnit)
                                        : CoreGen.adjustDate(currentRowValue, compiledPrimitiveHandler.minus(0, offsetFrom).longValue(), offsetFromDurationUnit);
                                if (currentPartitionValueAsObject != null)
                                {
                                    PureDate currentPartitionValue = (PureDate) currentPartitionValueAsObject;
                                    if (lowerBound.compareTo(currentPartitionValue) <= 0 && currentPartitionValue.compareTo(upperBound) <= 0)
                                    {
                                        aggregationValues.add(aggregateValue);
                                    }
                                }
                            }
                        }
                        else
                        {
                            throw new RuntimeException("Non-numeric values for order by column are not currently supported for range frame, but found: " + orderByCurrentRowValue.getClass());
                        }
                    }

                    aggregationValues.removeIf(Objects::isNull);
                    if (aggregationValues.isEmpty())
                    {
                        setter.value(cursor++, null);
                    }
                    else
                    {
                        Object result = reduce.value(aggregationValues, es);
                        setter.value(cursor++, result);
                    }
                }
            }
            else
            {
                for (int i = 0; i < partitionSize; i++)
                {
                    Object res = map.eval(winTDS, convertedFrame, new RowContainer(winTDS.tds, i), es);
                    subList.add(res);
                }

                // Write result
                if (compress)
                {
                    subList.removeIf(Objects::isNull);
                    Object result = reduce.value(subList, es);
                    setter.value(j, result);
                }
                else
                {
                    if (window == null)
                    {
                        subList.removeIf(Objects::isNull);
                        Object result = reduce.value(subList, es);
                        for (int i = 0; i < partitionSize; i++)
                        {
                            setter.value(cursor++, result);
                        }
                    }
                    else
                    {
                        for (int i = 0; i < partitionSize; i++)
                        {
                            Frame windowFrame = window.getFrame();
                            if (i + (Integer) windowFrame.getOffsetTo(subList.size()) < 0 || i + (Integer) windowFrame.getOffsetFrom() >= subList.size())
                            {
                                setter.value(cursor++, null);
                            }
                            else
                            {
                                MutableList<Object> framed = framedList(subList, windowFrame, i);
                                framed.removeIf(Objects::isNull);
                                Object result = reduce.value(framed, es);
                                setter.value(cursor++, result);
                            }
                        }
                    }
                }
            }
        }
    }

    protected static MutableList<Object> framedList(MutableList<Object> src, Frame frame, int currentRow)
    {
        MutableList<Object> copy = Lists.mutable.withAll(src);
        return copy.subList(frame.getLow(currentRow), frame.getHigh(currentRow, src.size()) + 1);
    }

    public static Relation<?> project(RichIterable<?> objects, RichIterable<? extends ColFuncSpecTrans1> colFuncs, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();

        MutableList<? extends String> typesL = colFuncs.collect(c -> c.columnType).toList();
        MutableList<? extends String> namesL = colFuncs.collect(c -> c.newColName).toList();
        ListIterable<TestTDSCompiled> pre = objects.collect(o ->
        {
            int i = 0;
            MutableList<TestTDSCompiled> subList = Lists.mutable.empty();
            for (SharedPureFunction f : colFuncs.collect(c -> c.func))
            {
                TestTDSCompiled one = new TestTDSCompiled();
                RichIterable<?> li = CompiledSupport.toPureCollection(f.execute(Lists.mutable.with(o), es));
                switch ((String) typesL.get(i))
                {
                    case M3Paths.String:
                        one.addColumn(namesL.get(i), DataType.STRING, li.toArray(new String[0]));
                        break;
                    case M3Paths.Integer:
                        one.addColumn(namesL.get(i), DataType.LONG, toLong(li));
                        break;
                    case M3Paths.Float:
                    case M3Paths.Number:
                        one.addColumn(namesL.get(i), DataType.DOUBLE, toDouble(li));
                        break;
                    default:
                        throw new RuntimeException(typesL.get(i) + " is not supported yet!");
                }
                if (li.isEmpty())
                {
                    one = (TestTDSCompiled) one.setNull();
                }
                subList.add(one);
                i++;
            }
            return subList.drop(1).injectInto(subList.get(0), (a, b) -> (TestTDSCompiled) a.join(b));
        }).toList();
        return new TDSContainer(pre.drop(1).injectInto(pre.get(0), (a, b) -> (TestTDSCompiled) a.concatenate(b)), ps);
    }

    public static Object toLong(RichIterable<?> li)
    {
        long[] result = new long[li.size()];
        int i = 0;
        for (Object o : li)
        {
            result[i++] = (long) o;
        }
        return result;
    }

    public static Object toDouble(RichIterable<?> li)
    {
        double[] result = new double[li.size()];
        int i = 0;
        for (Object o : li)
        {
            result[i++] = (double) o;
        }
        return result;
    }

    public static long rowNumber(Relation<?> rel, Object rc, ExecutionSupport es)
    {
        return ((RowContainer) rc).getRow() + 1;
    }

    public static long rank(Relation<?> rel, Window w, Object rc, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(rel, es).rank(w.getSorts(), ((RowContainer) rc).getRow());
    }

    public static long denseRank(Relation<?> rel, Window w, Object rc, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(rel, es).denseRank(w.getSorts(), ((RowContainer) rc).getRow());
    }

    public static double percentRank(Relation<?> rel, Window w, Object rc, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(rel, es).percentRank(w.getSorts(), ((RowContainer) rc).getRow());
    }

    public static long ntile(Relation<?> rel, Object rc, long tiles, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(rel, es).ntile(((RowContainer) rc).getRow(), tiles);
    }

    public static double cumulativeDistribution(Relation<?> rel, Window w, Object rc, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(rel, es).cumulativeDistribution(w.getSorts(), ((RowContainer) rc).getRow());
    }

    public static RowContainer first(Relation<?> rel, Window w, Object _r, ExecutionSupport es)
    {
        return new RowContainer(RelationNativeImplementation.getTDS(rel, es), w.getFrame().getLow(((RowContainer) _r).getRow()));
    }

    public static RowContainer last(Relation<?> rel, Window w, Object _r, ExecutionSupport es)
    {
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);
        return new RowContainer(tds, w.getFrame().getHigh(((RowContainer) _r).getRow(), (int) tds.getRowCount()));
    }

    public static RowContainer nth(Relation<?> rel, Window w, Object rc, long l, ExecutionSupport es)
    {
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel, es);
        int offset = tds.nth(((RowContainer) rc).getRow(), w, l);
        if (offset == -1)
        {
            return new NullRowContainer();
        }
        else
        {
            return new RowContainer(tds, offset);
        }
    }
}
