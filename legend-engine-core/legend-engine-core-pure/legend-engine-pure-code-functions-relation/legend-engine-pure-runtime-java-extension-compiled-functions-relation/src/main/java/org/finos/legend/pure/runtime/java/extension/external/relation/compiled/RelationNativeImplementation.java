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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.CompiledPrimitiveBuilder;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.NullRowContainer;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.RowContainer;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.TDSContainer;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.TestTDSCompiled;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.ColumnValue;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Frame;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;

import java.util.Objects;

import static org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS.readCsv;

public class RelationNativeImplementation
{

    public static TestTDSCompiled getTDS(Object value)
    {
        return value instanceof TDSContainer ?
                ((TDSContainer) value).tds :
                new TestTDSCompiled(readCsv((((CoreInstance) value).getValueForMetaPropertyToOne("csv")).getName()), ((CoreInstance) value).getValueForMetaPropertyToOne(M3Properties.classifierGenericType));
    }


    public static <T> RichIterable<Column<?, ?>> columns(Relation<? extends T> t)
    {
        return ((RelationType) t._classifierGenericType()._typeArguments().getFirst()._rawType())._columns();
    }

    public static <T, V> RichIterable<V> map(Relation<? extends T> rel, Function2<RowContainer, ExecutionSupport, RichIterable<V>> pureFunction, ExecutionSupport es)
    {
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);
        MutableList list = Lists.mutable.empty();
        for (int i = 0; i < tds.getRowCount(); i++)
        {
            list.add(pureFunction.value(new RowContainer(tds, i), es));
        }
        return list;
    }

    public static <T> Relation<? extends T> distinct(Relation<? extends T> rel, ColSpecArray<?> columns, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel).distinct((MutableList) columns._names().toList()), ps);
    }

    public static <T> Long size(Relation<? extends T> res)
    {
        return RelationNativeImplementation.getTDS(res).getRowCount();
    }

    public static <T> Relation<? extends T> limit(Relation<? extends T> rel, long size, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel).slice(0, (int) size), ps);
    }

    public static <T> Relation<? extends T> slice(Relation<? extends T> rel, long start, long stop, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel).slice((int) start, (int) stop), ps);
    }

    public static <T> Relation<? extends T> drop(Relation<? extends T> relation, Long aLong, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(relation);
        return new TDSContainer((TestTDSCompiled) tds.slice(aLong.intValue(), (int) tds.getRowCount()), ps);
    }

    public static <T> Relation<? extends Object> rename(Relation<? extends T> r, ColSpec<?> old, ColSpec<?> aNew, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(r).rename(old._name(), aNew._name()), ps);
    }

    public static <T> Relation<? extends Object> select(Relation<? extends T> r, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(r).select(Sets.mutable.withAll(RelationNativeImplementation.getTDS(r).getColumnNames())), ps);
    }

    public static <T> Relation<? extends Object> select(Relation<? extends T> r, ColSpec<?> col, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(r).select(Sets.mutable.with(col._name())), ps);
    }

    public static <T> Relation<? extends Object> select(Relation<? extends T> r, ColSpecArray<?> cols, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(r).select(Sets.mutable.withAll(cols._names())), ps);
    }

    public static <T> Relation<? extends T> concatenate(Relation<? extends T> rel1, Relation<? extends T> rel2, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel1).concatenate(RelationNativeImplementation.getTDS(rel2)), ps);
    }

    public static <T> Relation<? extends T> filter(Relation<? extends T> rel, Function2 pureFunction, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);
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
        return (T) new RowContainer(RelationNativeImplementation.getTDS(w), actualOffset);
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

    public static <T> Relation<? extends Object> extend(Relation<? extends T> rel, MutableList<ColFuncSpecTrans1> colFuncSpecTrans, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);
        return new TDSContainer((TestTDSCompiled) colFuncSpecTrans.injectInto((TestTDS) tds, (accTDS, colFuncSpec) -> accTDS.addColumn(performExtend(new Window(), tds.wrapFullTDS(), colFuncSpec, es))), ps);
    }

    public static <T> Relation<?> extendAgg(Relation<? extends T> rel, MutableList<AggColSpecTrans1> aggColSpecTrans, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);
        return new TDSContainer((TestTDSCompiled) aggregateTDS(null, tds.wrapFullTDS(), aggColSpecTrans, false, es).injectInto((TestTDS) tds, TestTDS::addColumn), ps);
    }

    public static <T> Relation<? extends T> extendWinFunc(Relation<? extends T> rel, Window window, MutableList<ColFuncSpecTrans2> colFunc, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes = tds.sort(window.getPartition().collect(part -> new SortInfo(part, SortDirection.ASC)).toList());
        final Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedPartitions = TestTDS.sortPartitions(window.getSorts(), sortRes);

        return new TDSContainer((TestTDSCompiled) colFunc.injectInto(sortedPartitions.getOne(), (a, b) -> a.addColumn(performExtend(window, sortedPartitions, b, es))), ps);
    }

    public static <T> Relation<? extends T> extendWinAgg(Relation<? extends T> rel, Window window, MutableList<AggColSpecTrans2> aggColSpecTrans, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes = window.getPartition().isEmpty() ? tds.wrapFullTDS() : tds.sort(window.getPartition().collect(part -> new SortInfo(part, SortDirection.ASC)).toList());
        final Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedPartitions = TestTDS.sortPartitions(window.getSorts(), sortRes);

        return new TDSContainer((TestTDSCompiled) aggregateTDS(window, sortedPartitions, aggColSpecTrans, false, es).injectInto(sortedPartitions.getOne(), TestTDS::addColumn), ps);
    }


    private static ColumnValue performExtend(Window window, Pair<TestTDS, MutableList<Pair<Integer, Integer>>> tds, ColFuncSpecTrans colFuncSpecTrans, ExecutionSupport es)
    {
        long size = tds.getOne().getRowCount();
        boolean[] nulls = new boolean[(int) size];
        switch (colFuncSpecTrans.columnType)
        {
            case "String":
                MutableList<String> res = Lists.mutable.empty();
                extracted(tds, window, colFuncSpecTrans, es, (i, val) -> res.add((String) val));
                return new ColumnValue(colFuncSpecTrans.newColName, DataType.STRING, res.toArray(new String[0]));
            case "Integer":
                int[] resultInt = new int[(int) size];
                extracted(tds, window, colFuncSpecTrans, es, (i, val) -> processWithNull(i, val, nulls, () -> resultInt[i] = (int) (long) val));
                return new ColumnValue(colFuncSpecTrans.newColName, DataType.INT, resultInt, nulls);
            case "Double":
            case "Float":
                double[] resultDouble = new double[(int) size];
                extracted(tds, window, colFuncSpecTrans, es, (i, val) -> processWithNull(i, val, nulls, () -> resultDouble[i] = (double) val));
                return new ColumnValue(colFuncSpecTrans.newColName, DataType.DOUBLE, resultDouble, nulls);
        }
        throw new RuntimeException(colFuncSpecTrans.columnType + " not supported yet");
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
        Object frame = window.convert(((CompiledExecutionSupport) es).getProcessorSupport(), new CompiledPrimitiveBuilder());
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

        TestTDSCompiled tds1 = RelationNativeImplementation.getTDS(rel1);
        TestTDSCompiled tds2 = RelationNativeImplementation.getTDS(rel2);
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
        TestTDSCompiled tds1 = RelationNativeImplementation.getTDS(rel);
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

        public AggColSpecTrans1(String newColName, SharedPureFunction map, Function2 reduce, String reduceType)
        {
            super(newColName, reduce, reduceType);
            this.map = map;
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
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes = tds.sort(cols.collect(name -> new SortInfo(name, SortDirection.ASC)).toList());

        MutableSet<String> columnsToRemove = tds.getColumnNames().clone().toSet();
        columnsToRemove.removeAll(cols.toSet());
        TestTDS distinctTDS = sortRes.getOne()._distinct(sortRes.getTwo()).removeColumns(columnsToRemove);

        return new TDSContainer((TestTDSCompiled) aggregateTDS(null, sortRes, aggColSpecTransAll, true, es).injectInto(distinctTDS, TestTDS::addColumn), ps);
    }

    private static MutableList<ColumnValue> aggregateTDS(Window window, Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes, MutableList<? extends AggColSpecTrans> aggColSpecTransAll, boolean compress, ExecutionSupport es)
    {
        int size = compress ? sortRes.getTwo().size() : (int) sortRes.getOne().getRowCount();
        MutableList<ColumnValue> columnValues = Lists.mutable.empty();
        for (AggColSpecTrans aggColSpecTrans : aggColSpecTransAll)
        {
            switch (aggColSpecTrans.reduceType)
            {
                case "String":
                    String[] finalRes = new String[size];
                    performMapReduce(window, aggColSpecTrans, aggColSpecTrans.reduce, es, sortRes, (o, j) -> finalRes[j] = (String) o, compress);
                    columnValues.add(new ColumnValue(aggColSpecTrans.newColName, DataType.STRING, finalRes));
                    break;
                case "Integer":
                    int[] finalResInt = new int[size];
                    performMapReduce(window, aggColSpecTrans, aggColSpecTrans.reduce, es, sortRes, (o, j) -> finalResInt[j] = (int) (long) o, compress);
                    columnValues.add(new ColumnValue(aggColSpecTrans.newColName, DataType.INT, finalResInt));
                    break;
                case "Double":
                case "Float":
                case "Number":
                    double[] finalResDouble = new double[size];
                    performMapReduce(window, aggColSpecTrans, aggColSpecTrans.reduce, es, sortRes, (o, j) -> finalResDouble[j] = (double) o, compress);
                    columnValues.add(new ColumnValue(aggColSpecTrans.newColName, DataType.FLOAT, finalResDouble));
                    break;
                default:
                    throw new RuntimeException(aggColSpecTrans.reduceType + " is not supported yet!");
            }
        }
        return columnValues;
    }

    private static void performMapReduce(Window window, AggColSpecTrans map, Function2 reduce, ExecutionSupport es, Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes, Function2<Object, Integer, Object> val, boolean compress)
    {
        int cursor = 0;
        int size = sortRes.getTwo().size();
        for (int j = 0; j < size; j++)
        {
            Pair<Integer, Integer> r = sortRes.getTwo().get(j);
            MutableList<Object> subList = org.eclipse.collections.impl.factory.Lists.mutable.empty();
            TDSContainer winTDS = new TDSContainer((TestTDSCompiled) sortRes.getOne().slice(r.getOne(), r.getTwo()), ((CompiledExecutionSupport) es).getProcessorSupport());
            Object frame = window == null ? null : window.convert(((CompiledExecutionSupport) es).getProcessorSupport(), new CompiledPrimitiveBuilder());
            for (int i = 0; i < r.getTwo() - r.getOne(); i++)
            {
                Object res = map.eval(winTDS, frame, new RowContainer(winTDS.tds, i), es);
                subList.add(res);
            }

            // Write result
            if (compress)
            {
                Object result = reduce.value(subList, es);
                subList.removeIf(Objects::isNull);
                val.apply(result, j);
            }
            else
            {
                if (window == null)
                {
                    subList.removeIf(Objects::isNull);
                    Object result = reduce.value(subList, es);
                    for (int i = 0; i < r.getTwo() - r.getOne(); i++)
                    {
                        val.apply(result, cursor++);
                    }
                }
                else
                {
                    for (int i = 0; i < r.getTwo() - r.getOne(); i++)
                    {
                        MutableList<Object> framed = framedList(subList, window.getFrame(), i);
                        framed.removeIf(Objects::isNull);
                        Object result = reduce.value(framed, es);
                        val.apply(result, cursor++);
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
                    case "String":
                        one.addColumn(namesL.get(i), DataType.STRING, li.toArray(new String[0]));
                        break;
                    case "Integer":
                        one.addColumn(namesL.get(i), DataType.INT, toInt(li));
                        break;
                    case "Double":
                    case "Float":
                    case "Number":
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

    public static Object toInt(RichIterable<?> li)
    {
        int[] result = new int[li.size()];
        int i = 0;
        for (Object o : li)
        {
            result[i++] = (int) (long) o;
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
        return RelationNativeImplementation.getTDS(rel).rank(w.getSorts(), ((RowContainer) rc).getRow());
    }

    public static long denseRank(Relation<?> rel, Window w, Object rc, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(rel).denseRank(w.getSorts(), ((RowContainer) rc).getRow());
    }

    public static double percentRank(Relation<?> rel, Window w, Object rc, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(rel).percentRank(w.getSorts(), ((RowContainer) rc).getRow());
    }

    public static long ntile(Relation<?> rel, Object rc, long tiles, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(rel).ntile(((RowContainer) rc).getRow(), tiles);
    }

    public static double cumulativeDistribution(Relation<?> rel, Window w, Object rc, ExecutionSupport es)
    {
        return RelationNativeImplementation.getTDS(rel).cumulativeDistribution(w.getSorts(), ((RowContainer) rc).getRow());
    }

    public static RowContainer first(Relation<?> rel, Window w, Object _r, ExecutionSupport es)
    {
        return new RowContainer(RelationNativeImplementation.getTDS(rel), w.getFrame().getLow(((RowContainer) _r).getRow()));
    }

    public static RowContainer last(Relation<?> rel, Window w, Object _r, ExecutionSupport es)
    {
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);
        return new RowContainer(tds, w.getFrame().getHigh(((RowContainer) _r).getRow(), (int) tds.getRowCount()));
    }

    public static RowContainer nth(Relation<?> rel, Window w, Object rc, long l, ExecutionSupport es)
    {
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);
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
