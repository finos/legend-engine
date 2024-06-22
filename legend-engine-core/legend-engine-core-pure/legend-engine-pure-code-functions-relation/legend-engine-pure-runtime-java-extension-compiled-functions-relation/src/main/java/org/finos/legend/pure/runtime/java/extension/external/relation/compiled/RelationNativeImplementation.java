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
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.RowContainer;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.TDSContainer;
import org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared.TestTDSCompiled;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;

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
        return new TDSContainer((TestTDSCompiled) RelationNativeImplementation.getTDS(rel1).concatenate((TestTDSCompiled) RelationNativeImplementation.getTDS(rel2)), ps);
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

    public static class ColFuncSpecTrans
    {
        public String newColName;
        public Function2 func;
        public String columnType;

        public ColFuncSpecTrans(String newColName, Function2 func, String columnType)
        {
            this.newColName = newColName;
            this.func = func;
            this.columnType = columnType;
        }
    }

    public static <T> Relation<? extends Object> extend(Relation<? extends T> rel, MutableList<ColFuncSpecTrans> colFuncSpecTrans, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);
        TestTDSCompiled t = colFuncSpecTrans.injectInto(tds, (a, b) -> performExtend(b, es, a, ps));
        return new TDSContainer(t, ps);
    }

    private static TestTDSCompiled performExtend(ColFuncSpecTrans colFuncSpecTrans, ExecutionSupport es, TestTDSCompiled tds, ProcessorSupport ps)
    {
        switch (colFuncSpecTrans.columnType)
        {
            case "String":
                MutableList<String> res = Lists.mutable.empty();
                for (int i = 0; i < tds.getRowCount(); i++)
                {
                    res.add((String) colFuncSpecTrans.func.value(new RowContainer(tds, i), es));
                }
                return (TestTDSCompiled) tds.addColumn(colFuncSpecTrans.newColName, DataType.STRING, res.toArray(new String[0]));
            case "Integer":
                int[] resultInt = new int[(int) tds.getRowCount()];
                for (int i = 0; i < tds.getRowCount(); i++)
                {
                    resultInt[i] = (int) (long) colFuncSpecTrans.func.value(new RowContainer(tds, i), es);
                }
                return (TestTDSCompiled) tds.addColumn(colFuncSpecTrans.newColName, DataType.INT, resultInt);
            case "Double":
            case "Float":
                double[] resultDouble = new double[(int) tds.getRowCount()];
                for (int i = 0; i < tds.getRowCount(); i++)
                {
                    resultDouble[i] = (double) colFuncSpecTrans.func.value(new RowContainer(tds, i), es);
                }
                return (TestTDSCompiled) tds.addColumn(colFuncSpecTrans.newColName, DataType.DOUBLE, resultDouble);
        }
        throw new RuntimeException(colFuncSpecTrans.columnType + " not supported yet");
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

    public static class AggColSpecTrans
    {
        public String newColName;
        public Function2 map;
        public Function2 reduce;
        public String reduceType;

        public AggColSpecTrans(String newColName, Function2 map, Function2 reduce, String reduceType)
        {
            this.newColName = newColName;
            this.map = map;
            this.reduce = reduce;
            this.reduceType = reduceType;
        }
    }

    public static <T> Relation<? extends Object> groupBy(Relation<? extends T> rel, ColSpec<?> cols, MutableList<AggColSpecTrans> aggColSpecTrans, ExecutionSupport es)
    {
        return groupBy(rel, Lists.mutable.with(cols._name()), aggColSpecTrans, es);
    }

    public static <T> Relation<? extends Object> groupBy(Relation<? extends T> rel, ColSpecArray<?> cols, MutableList<AggColSpecTrans> aggColSpecTransAll, ExecutionSupport es)
    {
        return groupBy(rel, Lists.mutable.withAll(cols._names()), aggColSpecTransAll, es);
    }

    private static <T> Relation<? extends Object> groupBy(Relation<? extends T> rel, MutableList<String> cols, MutableList<AggColSpecTrans> aggColSpecTransAll, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        TestTDSCompiled tds = RelationNativeImplementation.getTDS(rel);

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes = tds.sort(cols.collect(name -> new SortInfo(name, SortDirection.ASC)).toList());

        int size = sortRes.getTwo().size();

        MutableSet<String> columnsToRemove = tds.getColumnNames().clone().toSet();
        columnsToRemove.removeAll(cols.toSet());

        TestTDSCompiled finalTDS = (TestTDSCompiled) sortRes.getOne()._distinct(sortRes.getTwo()).removeColumns(columnsToRemove);

        for (AggColSpecTrans aggColSpecTrans : aggColSpecTransAll)
        {
            switch ((String) aggColSpecTrans.reduceType)
            {
                case "String":
                    String[] finalRes = new String[size];
                    performMapReduce(aggColSpecTrans.map, aggColSpecTrans.reduce, es, size, sortRes, (o, j) -> finalRes[j] = (String) o);
                    finalTDS.addColumn(aggColSpecTrans.newColName, DataType.STRING, finalRes);
                    break;
                case "Integer":
                    int[] finalResInt = new int[size];
                    performMapReduce(aggColSpecTrans.map, aggColSpecTrans.reduce, es, size, sortRes, (o, j) -> finalResInt[j] = (int) (long) o);
                    finalTDS.addColumn(aggColSpecTrans.newColName, DataType.INT, finalResInt);
                    break;
                case "Double":
                case "Float":
                case "Number":
                    double[] finalResDouble = new double[size];
                    performMapReduce(aggColSpecTrans.map, aggColSpecTrans.reduce, es, size, sortRes, (o, j) -> finalResDouble[j] = (double) o);
                    finalTDS.addColumn(aggColSpecTrans.newColName, DataType.FLOAT, finalResDouble);
                    break;
                default:
                    throw new RuntimeException(aggColSpecTrans.reduceType + " is not supported yet!");
            }
        }

        return new TDSContainer(finalTDS, ps);
    }

    private static void performMapReduce(Function2 map, Function2 reduce, ExecutionSupport es, int size, Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortRes, Function2<Object, Integer, Object> val)
    {
        for (int j = 0; j < size; j++)
        {
            Pair<Integer, Integer> r = sortRes.getTwo().get(j);
            MutableList<Object> subList = org.eclipse.collections.impl.factory.Lists.mutable.empty();
            for (int i = r.getOne(); i < r.getTwo(); i++)
            {
                subList.add(map.value(new RowContainer((TestTDSCompiled) sortRes.getOne(), i), es));
            }
            val.apply(reduce.value(subList, es), j);
        }
    }

    public static Relation<?> project(RichIterable<?> objects, RichIterable<? extends ColFuncSpecTrans> colFuncs, ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();

        MutableList<? extends String> typesL = colFuncs.collect(c -> c.columnType).toList();
        MutableList<? extends String> namesL = colFuncs.collect(c -> c.newColName).toList();
        ListIterable<TestTDSCompiled> pre = objects.collect(o ->
        {
            int i = 0;
            MutableList<TestTDSCompiled> subList = Lists.mutable.empty();
            for (Function2 f : colFuncs.collect(c -> c.func))
            {
                TestTDSCompiled one = new TestTDSCompiled();
                RichIterable<?> li = CompiledSupport.toPureCollection(f.apply(o, es));
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
}
