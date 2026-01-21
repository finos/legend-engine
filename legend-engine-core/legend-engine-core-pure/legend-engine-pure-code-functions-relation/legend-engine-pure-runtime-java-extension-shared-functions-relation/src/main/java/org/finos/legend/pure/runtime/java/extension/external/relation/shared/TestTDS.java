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

package org.finos.legend.pure.runtime.java.extension.external.relation.shared;

import io.deephaven.csv.CsvSpecs;
import io.deephaven.csv.parsers.DataType;
import io.deephaven.csv.parsers.Parsers;
import io.deephaven.csv.reading.CsvReader;
import io.deephaven.csv.sinks.SinkFactory;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpressionAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window;
import org.finos.legend.pure.runtime.java.extension.external.variant.VariantInstanceImpl;

public abstract class TestTDS
{
    public static final long LONG_NULL_SENTINEL = 9_223_372_036_854_775_783L; //largest prime for 64 signed numbers
    public static final double DOUBLE_NULL_SENTINEL = Double.NEGATIVE_INFINITY;
    public static final byte BOOLEAN_AS_BYTE_SENTINEL = Byte.MIN_VALUE;
    public static final long DATE_TIME_AS_LONG_SENTINEL = Long.MIN_VALUE;

    protected MutableMap<String, GenericType> pureTypesByColumnName = Maps.mutable.empty();
    protected MutableMap<String, Object> dataByColumnName = Maps.mutable.empty();
    protected MutableList<String> columnsOrdered = Lists.mutable.empty();
    protected long rowCount;

    protected ProcessorSupport processorSupport;

    public TestTDS(ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
    }

    public TestTDS(String csv, ProcessorSupport processorSupport)
    {
        this(readCsv(csv), Lists.mutable.empty(), processorSupport);
    }

    public TestTDS(CsvReader.Result result, MutableList<GenericType> types, ProcessorSupport processorSupport)
    {
        build(result, types, processorSupport);
    }

    protected TestTDS(MutableList<String> columnOrdered, MutableMap<String, GenericType> pureTypesByColumnName, int rows, ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
        this.columnsOrdered = columnOrdered;
        this.pureTypesByColumnName = pureTypesByColumnName;
        this.rowCount = rows;
        this.columnsOrdered.forEach(c -> this.dataByColumnName.put(c, new Object[(int) this.rowCount]));
    }

    protected void build(CsvReader.Result result, MutableList<GenericType> types, ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;

        this.rowCount = result.numRows();

        if (types.isEmpty())
        {
            throw new RuntimeException("Not supported");
        }

        ListIterate.zip(Lists.mutable.with(result.columns()), types).forEach(c ->
        {
            CsvReader.ResultColumn column = c.getOne();
            GenericType pureType = c.getTwo();
            String name = column.name().trim();
            columnsOrdered.add(name);
            pureTypesByColumnName.put(name, c.getTwo());
            dataByColumnName.put(name, getDataAsType(column, pureType, (int) rowCount, processorSupport));
        });
    }

    public TestTDS newEmptyTDS()
    {
        return newTDS(this.columnsOrdered, this.pureTypesByColumnName, 0);
    }

    public TestTDS newNullTDS()
    {
        TestTDS testTDS = newTDS(this.columnsOrdered, this.pureTypesByColumnName, 1);

        for (String col : this.columnsOrdered)
        {
            testTDS.dataByColumnName.put(col, new Object[(int) testTDS.rowCount]);
        }
        return testTDS;
    }

    public abstract TestTDS newTDS(MutableList<String> columnOrdered, MutableMap<String, GenericType> columnTyp, int rows);

    public abstract Object getValueAsCoreInstance(String columnName, int rowNum);

    public Object getValue(String columnName, int rowNum)
    {
        return ((Object[]) dataByColumnName.get(columnName))[rowNum];
    }

    private Object getDataAsType(CsvReader.ResultColumn c, GenericType genericType, int rowCount, ProcessorSupport processorSupport)
    {
        // CSV parser, when all values are null, cannot infer type, and ends giving String[]
        Object data = c.data();
        Type type = genericType._rawType();
        if (processorSupport.type_subTypeOf(type, processorSupport.package_getByUserPath(M3Paths.String)))
        {
            if (c.dataType() == DataType.STRING)
            {
                return data;
            }
            else
            {
                throw new RuntimeException("Not supported data type :'" + c.dataType() + "' for Pure type: '" + PackageableElement.getUserPathForPackageableElement(type) + "'");
            }
        }
        else if (processorSupport.type_subTypeOf(type, processorSupport.package_getByUserPath(M3Paths.Integer)))
        {
            Long[] result = new Long[rowCount];
            if (c.dataType() == DataType.LONG)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    long value = ((long[]) data)[i];
                    result[i] = value == LONG_NULL_SENTINEL ? null : value;
                }
            }
            else if (c.dataType() == DataType.STRING)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    String value = ((String[]) data)[i];
                    result[i] = value == null ? null : Long.valueOf(value);
                }
            }
            else
            {
                throw new RuntimeException("Not supported data type :'" + c.dataType() + "' for Pure type: '" + PackageableElement.getUserPathForPackageableElement(type) + "'");
            }
            return result;
        }
        else if (processorSupport.type_subTypeOf(type, processorSupport.package_getByUserPath(M3Paths.Date)))
        {
            if (c.dataType() == DataType.DATETIME_AS_LONG)
            {
                PureDate[] dates = new PureDate[(int) this.rowCount];
                for (int i = 0; i < this.rowCount; i++)
                {
                    long value = ((long[]) data)[i];
                    dates[i] = value == DATE_TIME_AS_LONG_SENTINEL ? null : DateFunctions.fromDate(new Date(value / 1000000));
                }
                return dates;
            }
            else
            {
                throw new RuntimeException("Not supported data type :'" + c.dataType() + "' for Pure type: '" + PackageableElement.getUserPathForPackageableElement(type) + "'");
            }
        }
        else if (processorSupport.type_subTypeOf(type, processorSupport.package_getByUserPath(M3Paths.Boolean)))
        {
            if (c.dataType() == DataType.BOOLEAN_AS_BYTE)
            {
                Boolean[] result = new Boolean[rowCount];
                for (int i = 0; i < this.rowCount; i++)
                {
                    byte bVal = ((byte[]) data)[i];
                    result[i] = bVal == BOOLEAN_AS_BYTE_SENTINEL ? null : bVal == 1;
                }
                return result;
            }
            else
            {
                throw new RuntimeException("Not supported data type :'" + c.dataType() + "' for Pure type: '" + PackageableElement.getUserPathForPackageableElement(type) + "'");
            }
        }
        else if (processorSupport.type_subTypeOf(type, processorSupport.package_getByUserPath(M3Paths.Float)))
        {
            Double[] result = new Double[rowCount];
            if (c.dataType() == DataType.DOUBLE)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    double bVal = ((double[]) data)[i];
                    result[i] = bVal == DOUBLE_NULL_SENTINEL ? null : bVal;
                }
            }
            else if (c.dataType() == DataType.LONG)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    long bVal = ((long[]) data)[i];
                    result[i] = bVal == LONG_NULL_SENTINEL ? null : (double) bVal;
                }
            }
            else
            {
                throw new RuntimeException("Not supported data type :'" + c.dataType() + "' for Pure type: '" + PackageableElement.getUserPathForPackageableElement(type) + "'");
            }
            return result;
        }
        else if (processorSupport.type_subTypeOf(type, processorSupport.package_getByUserPath(M3Paths.Decimal)))
        {
            Integer scale = null;
            if (type == processorSupport.package_getByUserPath("meta::pure::precisePrimitives::Numeric"))
            {
                scale = ((Number) ((InstanceValue) genericType._typeVariableValues().toList().get(1))._values().getFirst()).intValue();
            }
            BigDecimal[] result = new BigDecimal[rowCount];
            if (c.dataType() == DataType.DOUBLE)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    double bVal = ((double[]) data)[i];
                    result[i] = bVal == DOUBLE_NULL_SENTINEL ? null : BigDecimal.valueOf(bVal);
                    if (scale != null && result[i] != null)
                    {
                        result[i] = result[i].setScale(scale.intValue(), RoundingMode.HALF_UP);
                    }
                }
                return result;
            }
            else if (c.dataType() == DataType.LONG)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    long bVal = ((long[]) data)[i];
                    result[i] = bVal == LONG_NULL_SENTINEL ? null : BigDecimal.valueOf(bVal);
                    if (scale != null && result[i] != null)
                    {
                        result[i] = result[i].setScale(scale.intValue(), RoundingMode.HALF_UP);
                    }
                }
                return result;
            }
            else
            {
                throw new RuntimeException("Not supported data type :'" + c.dataType() + "' for Pure type: '" + PackageableElement.getUserPathForPackageableElement(type) + "'");
            }
        }
        else if (processorSupport.type_subTypeOf(type, processorSupport.package_getByUserPath(M3Paths.Variant)))
        {
            Variant[] result = new Variant[rowCount];
            if (c.dataType() == DataType.STRING)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    String value = ((String[]) data)[i];
                    result[i] = value == null ? null : VariantInstanceImpl.newVariant(value, processorSupport);
                }
            }
            else if (c.dataType() == DataType.LONG)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    long value = ((long[]) data)[i];
                    result[i] = value == LONG_NULL_SENTINEL ? null : VariantInstanceImpl.newVariant(String.valueOf(value), processorSupport);
                }
            }
            else if (c.dataType() == DataType.BOOLEAN_AS_BYTE)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    byte value = ((byte[]) data)[i];
                    result[i] = value == BOOLEAN_AS_BYTE_SENTINEL ? null : VariantInstanceImpl.newVariant(String.valueOf(value == 1), processorSupport);
                }
            }
            else
            {
                throw new RuntimeException("Not supported data type :'" + c.dataType() + "' for Pure type: '" + PackageableElement.getUserPathForPackageableElement(type) + "'");
            }
            return result;
        }
        else if (processorSupport.type_subTypeOf(type, processorSupport.package_getByUserPath(M3Paths.Number)))
        {
            Double[] result = new Double[rowCount];
            if (c.dataType() == DataType.DOUBLE)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    double bVal = ((double[]) data)[i];
                    result[i] = bVal == DOUBLE_NULL_SENTINEL ? null : bVal;
                }
            }
            else if (c.dataType() == DataType.LONG)
            {
                for (int i = 0; i < this.rowCount; i++)
                {
                    long value = ((long[]) data)[i];
                    result[i] = value == LONG_NULL_SENTINEL ? null : (double) value;
                }
            }
            else
            {
                throw new RuntimeException("Not supported data type :'" + c.dataType() + "' for Pure type: '" + PackageableElement.getUserPathForPackageableElement(type) + "'");
            }
            return result;
        }
        throw new RuntimeException("ERROR. Pure Type: '" + PackageableElement.getUserPathForPackageableElement(type) + "' is not supported yet.");
    }

    public TestTDS setNull()
    {
        TestTDS res = this.copy();
        res.rowCount = 1;
        boolean[] array = new boolean[(int) res.rowCount];
        Arrays.fill(array, Boolean.TRUE);
        res.columnsOrdered.forEach(c ->
        {
            res.dataByColumnName.put(c, new Object[1]);
        });
        return res;
    }

    public static CsvSpecs makePureCsvSpecs()
    {
        return CsvSpecs.builder()
                .nullValueLiterals(Arrays.asList("", "null", "NULL"))
                .parsers(Parsers.MINIMAL) // BOOLEAN, LONG, DOUBLE, DATETIME, STRING
                .build();
    }

    public static SinkFactory makePureSinkFactory()
    {
        return SinkFactory.arrays(
                null,
                null,
                null,
                LONG_NULL_SENTINEL,
                null,
                DOUBLE_NULL_SENTINEL,
                BOOLEAN_AS_BYTE_SENTINEL,
                null,
                null,
                DATE_TIME_AS_LONG_SENTINEL,
                null);
    }

    public static CsvReader.Result readCsv(String csv)
    {
        try
        {
            return CsvReader.read(makePureCsvSpecs(), new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), makePureSinkFactory());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error parsing:\n" + csv, e);
        }
    }

    public TestTDS join(TestTDS otherTDS)
    {
        MutableList<String> columnOrdered = Lists.mutable.empty();
        columnOrdered.addAll(this.columnsOrdered);
        columnOrdered.addAll(otherTDS.columnsOrdered);
        columnOrdered = columnOrdered.distinct();
        MutableMap<String, GenericType> pureTypesByColumnName = Maps.mutable.empty();
        pureTypesByColumnName.putAll(this.pureTypesByColumnName);
        pureTypesByColumnName.putAll(otherTDS.pureTypesByColumnName);
        columnOrdered = columnOrdered.distinct();
        TestTDS res = newTDS(columnOrdered, pureTypesByColumnName, (int) (rowCount * otherTDS.rowCount));

        if (res.rowCount != 0)
        {
            for (int i = 0; i < this.rowCount; i++)
            {
                for (int j = 0; j < otherTDS.rowCount; j++)
                {
                    for (String column : this.dataByColumnName.keysView())
                    {
                        res.setValue(column, i * (int) otherTDS.rowCount + j, this, i);
                    }
                    for (String column : otherTDS.dataByColumnName.keysView())
                    {
                        res.setValue(column, i * (int) otherTDS.rowCount + j, otherTDS, j);
                    }
                }
            }
        }
        return res;
    }

    public void setValue(String columnName, int row, TestTDS srcTDS, int srcRow)
    {
        Object[] dataAsObject = (Object[]) dataByColumnName.get(columnName);
        dataAsObject[row] = ((Object[]) srcTDS.dataByColumnName.get(columnName))[srcRow];
    }

    public TestTDS copy()
    {
        TestTDS result = newTDS(Lists.mutable.withAll(columnsOrdered), Maps.mutable.withMap(this.pureTypesByColumnName), (int) rowCount);
        result.dataByColumnName = Maps.mutable.empty();
        dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject = dataByColumnName.get(columnName);
            Object copy = Arrays.copyOf((Object[]) dataAsObject, (int) rowCount);
            result.dataByColumnName.put(columnName, copy);
        });
        return result;
    }

    public TestTDS drop(IntSet rows)
    {
        TestTDS copy = this.copy();
        int size = rows.size();
        copy.dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject = copy.dataByColumnName.get(columnName);
            Object[] src = (Object[]) dataAsObject;
            Object[] target = (Object[]) Array.newInstance(src.getClass().getComponentType(), (int) copy.rowCount - size);
            int j = 0;
            for (int i = 0; i < copy.rowCount; i++)
            {
                if (!rows.contains(i))
                {
                    target[j++] = src[i];
                }
            }
            copy.dataByColumnName.put(columnName, target);
        });
        copy.rowCount = copy.rowCount - size;
        return copy;
    }

    public long getRowCount()
    {
        return rowCount;
    }

    public TestTDS concatenate(TestTDS tds2)
    {
        TestTDS result = newTDS(Lists.mutable.withAll(columnsOrdered), Maps.mutable.withMap(pureTypesByColumnName), (int) (this.rowCount + tds2.rowCount));

        dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject1 = dataByColumnName.get(columnName);
            Object dataAsObject2 = tds2.dataByColumnName.get(columnName);
            Object copy;
            Object[] _copy = Arrays.copyOf((Object[]) dataAsObject1, (int) result.rowCount);
            System.arraycopy((Object[]) dataAsObject2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
            copy = _copy;
            result.dataByColumnName.put(columnName, copy);
        });
        return result;
    }

    public TestTDS addColumn(ColumnValue columnValue)
    {
        return addColumn(columnValue.name, columnValue.pureType, columnValue.multiplicity, columnValue.result);
    }

    public Pair<TestTDS, MutableList<Pair<Integer, Integer>>> wrapFullTDS()
    {
        return Tuples.pair(this.copy(), Lists.mutable.with(Tuples.pair(0, (int) this.getRowCount())));
    }

    public TestTDS addColumn(String name, GenericType type, Multiplicity multiplicity)
    {
        Object res;
        res = new Object[(int) this.rowCount];
        return addColumn(name, type, multiplicity, res);
    }

    public TestTDS addColumn(String name, GenericType type, Multiplicity multiplicity, Object res)
    {
        boolean[] array = new boolean[Array.getLength(res)];
        Arrays.fill(array, Boolean.FALSE);
        return addColumn(name, type, multiplicity, res, array);
    }

    public TestTDS addColumn(String name, GenericType type, Multiplicity multiplicity, Object res, boolean[] nulls)
    {
        int size = Array.getLength(res);
        if (this.rowCount == 0)
        {
            this.rowCount = size;
        }
        if (size != this.rowCount)
        {
            throw new RuntimeException("Error!");
        }
        this.dataByColumnName.put(name, res);
        this.pureTypesByColumnName.put(name, type);
        this.columnsOrdered.add(name);
        return this;
    }

    public TestTDS removeColumns(MutableSet<? extends String> columns)
    {
        TestTDS copy = this.copy();
        copy.columnsOrdered.removeAll(columns);
        copy.dataByColumnName.removeAllKeys(columns);
        return copy;
    }

    public TestTDS select(MutableList<? extends String> columns)
    {
        MutableSet<String> allColumns = Sets.mutable.withAll(this.getColumnNames());
        allColumns.removeAll(Sets.mutable.withAll(columns));
        TestTDS testTDS = removeColumns(allColumns);
        testTDS.columnsOrdered = Lists.mutable.withAll(columns);
        return testTDS;
    }

    public TestTDS rename(String oldName, String newName)
    {
        TestTDS copy = this.copy();
        GenericType pureType = copy.pureTypesByColumnName.get(oldName);
        Object data = copy.dataByColumnName.get(oldName);
        copy.pureTypesByColumnName.put(newName, pureType);
        copy.dataByColumnName.put(newName, data);
        copy.columnsOrdered.add(newName);
        copy.pureTypesByColumnName.remove(oldName);
        copy.dataByColumnName.remove(oldName);
        copy.columnsOrdered.remove(oldName);
        return copy;
    }

    public TestTDS slice(int from, int to)
    {
        TestTDS copy = this.copy();
        copy.dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject = copy.dataByColumnName.get(columnName);

            copy.dataByColumnName.put(columnName, Arrays.copyOfRange((Object[]) dataAsObject, from, to));
        });
        copy.rowCount = (long) to - from;
        return copy;
    }

    public Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sort(SortInfo sortInfos)
    {
        return this.sort(Lists.mutable.with(sortInfos));
    }

    public Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sort(ListIterable<SortInfo> sortInfos)
    {
        TestTDS copy = this.copy();
        MutableList<Pair<Integer, Integer>> ranges = Lists.mutable.empty();
        this.sort(copy, sortInfos, 0, (int) rowCount, ranges);
        return Tuples.pair(copy, ranges);
    }

    public MutableMap<String, GenericType> getPureTypesByColumnName()
    {
        return pureTypesByColumnName;
    }

    public TestTDS distinct(MutableList<String> columns)
    {
        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> res = this.sort(columns.collect(c -> new SortInfo(c, SortDirection.ASC)));
        TestTDS result = res.getOne()._distinct(res.getTwo());
        return result.select(columns.toList());
    }

    public TestTDS _distinct(MutableList<Pair<Integer, Integer>> ranges)
    {
        MutableIntSet drop = new IntHashSet();
        ranges.forEach(r ->
        {
            for (int i = r.getOne() + 1; i < r.getTwo(); i++)
            {
                drop.add(i);
            }
        });
        return this.drop(drop);
    }


    private void sort(TestTDS copy, ListIterable<SortInfo> sortInfos, int start, int end, MutableList<Pair<Integer, Integer>> ranges)
    {
        if (copy.rowCount == 0)
        {
            return;
        }
        SortInfo currentSort = sortInfos.getFirst();
        this.sortOneLevel(copy, currentSort, start, end);
        if (!sortInfos.isEmpty())
        {
            String columnName = currentSort.columnName;
            Object dataAsObject = copy.dataByColumnName.get(columnName);

            Object[] src = (Object[]) dataAsObject;
            Object val = src[start];
            int subStart = start;
            for (int i = start; i < end; i++)
            {
                if (!Objects.equals(src[i], val) || (Objects.equals(src[i], val) && i == end - 1))
                {
                    int realEnd = (Objects.equals(src[i], val) && i == end - 1) ? end : i;
                    if (sortInfos.size() > 1)
                    {
                        sort(copy, sortInfos.subList(1, sortInfos.size()), subStart, realEnd, ranges);
                    }
                    else
                    {
                        ranges.add(Tuples.pair(subStart, realEnd));
                    }
                    val = src[i];
                    subStart = i;
                }
            }
        }
        if (ranges.getLast() != null)
        {
            int lastVal = ranges.getLast().getTwo();
            if (lastVal < end)
            {
                ranges.add(Tuples.pair(lastVal, end));
            }
        }
    }

    private void sortOneLevel(TestTDS copy, SortInfo sortInfo, int start, int end)
    {
        String columnName = sortInfo.columnName;
        MutableList<Pair<Integer, Comparable<Object>>> list = Lists.mutable.empty();
        for (int i = start; i < end; i++)
        {
            list.add(Tuples.pair(i, (Comparable<Object>) copy.getValue(columnName, i)));
        }
        list.sortThis(Comparators.bySecondOfPair(Comparators.safeNullsHigh(Comparators.byFunction(p -> p))));
        if (sortInfo.direction == SortDirection.DESC)
        {
            list.reverseThis();
        }
        this.reorder(copy, list.collect(Pair::getOne), start, end);
    }

    private void reorder(TestTDS copy, MutableList<Integer> indices, int start, int end)
    {
        for (String columnName : copy.dataByColumnName.keysView())
        {
            Object dataAsObject = copy.dataByColumnName.get(columnName);
            Object[] src = (Object[]) dataAsObject;
            Object[] result = (Object[]) Array.newInstance(src.getClass().getComponentType(), (int) copy.rowCount);
            for (int i = 0; i < indices.size(); i++)
            {
                result[i] = src[indices.get(i)];
            }
            System.arraycopy(result, 0, src, start, end - start);
        }
    }

    public String toString()
    {
        RichIterable<String> columns = this.columnsOrdered;
        MutableList<String> rows = Lists.mutable.empty();
        for (int i = 0; i < rowCount; i++)
        {
            int finalI = i;
            rows.add(columns.collect(columnName ->
            {
                Object dataAsObject = this.getValue(columnName, finalI);
                return dataAsObject == null ? "NULL" : Objects.toString(dataAsObject);
            }).makeString(", "));
        }
        return columns.makeString(", ") + "\n" + rows.makeString("\n");
    }


    public TestTDS compensateLeft(TestTDS res)
    {
        MutableList<SortInfo> sortInfos = this.dataByColumnName.keysView().collect(c -> new SortInfo(c, SortDirection.ASC)).toList();
        MutableList<String> cols = this.dataByColumnName.keysView().toList();

        int rowLeftCurs = 0;
        int rowResCurs = 0;
        TestTDS leftS = this.sort(sortInfos).getOne();
        TestTDS resS = res.sort(sortInfos).getOne();

        MutableList<Integer> missings = Lists.mutable.empty();
        while (rowLeftCurs < leftS.rowCount)
        {
            if (resS.rowCount == 0 || rowResCurs >= resS.rowCount || !leftS.fullMatch(cols, resS, rowLeftCurs, rowResCurs))
            {
                missings.add(rowLeftCurs);
                rowLeftCurs++;
            }
            else
            {
                do
                {
                    rowResCurs++;
                }
                while (rowResCurs < resS.rowCount && leftS.fullMatch(cols, resS, rowLeftCurs, rowResCurs));
                rowLeftCurs++;
            }
        }

        TestTDS missingTDS = newTDS(res.columnsOrdered.clone(), pureTypesByColumnName, missings.size());

        int cursor = 0;
        for (Integer missing : missings)
        {
            for (String col : columnsOrdered)
            {
                missingTDS.setValue(col, cursor, leftS, missing);
            }
            cursor++;
        }
        return res.concatenate(missingTDS);
    }

    public boolean fullMatch(MutableList<String> cols, TestTDS second, int rowFirst, int rowSecond)
    {
        boolean valid = true;
        for (String col : cols)
        {
            Object firstDataAsObject = dataByColumnName.get(col);
            Object secondDataAsObject = second.dataByColumnName.get(col);
            valid = valid && Objects.equals(((Object[]) firstDataAsObject)[rowFirst], (((Object[]) secondDataAsObject)[rowSecond]));
        }
        return valid;
    }

    public MutableList<String> getColumnNames()
    {
        return this.columnsOrdered;
    }

    public static Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortPartitions(MutableList<SortInfo> transformedSort, Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source)
    {
        if (!transformedSort.isEmpty())
        {
            TestTDS res = source.getOne().newEmptyTDS();
            for (int j = 0; j < source.getTwo().size(); j++)
            {
                Pair<Integer, Integer> r = source.getTwo().get(j);
                res = res.concatenate(source.getOne().slice(r.getOne(), r.getTwo()).sort(transformedSort).getOne());
            }
            source = Tuples.pair(res, source.getTwo());
        }
        return source;
    }

    public long rank(MutableList<SortInfo> sorts, int row)
    {
        MutableList<String> columns = sorts.collect(SortInfo::getColumnName);
        MutableList<?> baseRow = fetch(this, columns, row);
        int rank = findFirstPrecedentDifferentRow(row, this, columns, baseRow);
        return rank + 1;

    }

    private static MutableList<?> fetch(TestTDS tds, MutableList<String> columns, int row)
    {
        return columns.collect(x -> tds.getValue(x, row));
    }

    private static int findFirstPrecedentDifferentRow(int row, TestTDS tds, MutableList<String> columns, MutableList<?> baseRow)
    {
        int rank = row;
        do
        {
            rank--;
        }
        while (rank >= 0 && fetch(tds, columns, rank).equals(baseRow));
        return rank + 1;
    }

    public long denseRank(MutableList<SortInfo> sorts, int row)
    {
        MutableList<String> columns = sorts.collect(SortInfo::getColumnName);
        MutableList<?> precedentRow = fetch(this, columns, 0);
        int rank = 1;
        for (int i = 1; i <= row; i++)
        {
            MutableList<?> currentRow = fetch(this, columns, i);
            if (!currentRow.equals(precedentRow))
            {
                rank++;
                precedentRow = currentRow;
            }
        }
        return rank;
    }

    public double percentRank(MutableList<SortInfo> sorts, int row)
    {
        MutableList<String> columns = sorts.collect(SortInfo::getColumnName);
        int size = (int) this.getRowCount();
        MutableList<?> baseRow = fetch(this, columns, row);
        int rank = findFirstPrecedentDifferentRow(row, this, columns, baseRow);
        return size == 1 ? 0 : (double) (rank) / (size - 1);
    }

    public long ntile(int row, long tiles)
    {
        int size = (int) this.getRowCount();
        return (long) ((double) row * tiles / size) + 1;
    }

    public double cumulativeDistribution(MutableList<SortInfo> sorts, int row)
    {
        MutableList<String> columns = sorts.collect(SortInfo::getColumnName);
        int size = (int) this.getRowCount();
        MutableList<?> baseRow = fetch(this, columns, row);
        int rank = findFirstPrecedentDifferentRow(row, this, columns, baseRow);
        return (double) (rank + 1) / size;
    }

    public int nth(int row, Window w, long l)
    {
        long offset = w.getFrame().getLow(row) + l - 1;
        int high = w.getFrame().getHigh(row, (int) this.getRowCount());

        if (offset <= high)
        {
            return (int) offset;
        }
        else
        {
            return -1;
        }
    }

    static class PivotColumnInfo
    {
        private final MutableList<Pair<String, String>> columnValues;
        private final String aggColumnName;
        private final GenericType pureType;
        private final String columnName;

        public PivotColumnInfo(MutableList<Pair<String, String>> columnValues, String aggColumnName, GenericType pureType)
        {
            this.columnValues = columnValues;
            this.aggColumnName = aggColumnName;
            this.pureType = pureType;
            // TODO: we might need to rethink this column naming strategy, it could break in some edge cases
            this.columnName = ListIterate.collect(columnValues, Pair::getTwo).with(aggColumnName).select(Objects::nonNull).makeString("__|__");
        }

        public String getColumnName()
        {
            return this.columnName;
        }

        public String getAggColumnName()
        {
            return this.aggColumnName;
        }

        public boolean match(TestTDS tds, int row)
        {
            return columnValues.allSatisfy(col -> Objects.toString(tds.getValue(col.getOne(), row)).equals(col.getTwo()));
        }

        public GenericType getPureType()
        {
            return this.pureType;
        }
    }

    // TODO: clean this up so this is more readable and properly leverage the fundamental methods like sort()
    public TestTDS applyPivot(ListIterable<String> nonTransposeColumns, ListIterable<String> pivotColumns, ListIterable<String> aggColumns)
    {
        // compute the different unique combinations of values
        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedByPivotColumns = this.sort(pivotColumns.collect(c -> new SortInfo(c, SortDirection.ASC)));
        MutableList<PivotColumnInfo> newColumns = Lists.mutable.empty();
        for (int i = 0; i < sortedByPivotColumns.getTwo().size(); i++)
        {
            Pair<Integer, Integer> r = sortedByPivotColumns.getTwo().get(i);
            for (String aggColumnName : aggColumns)
            {
                newColumns.add(new PivotColumnInfo(pivotColumns.collect(c ->
                        Tuples.pair(c, Objects.toString(sortedByPivotColumns.getOne().getValue(c, r.getOne())))).toList(), aggColumnName, this.pureTypesByColumnName.get(aggColumnName)));
            }
        }

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedByNonTransposeColumns = this.sort(nonTransposeColumns.collect(c -> new SortInfo(c, SortDirection.ASC)));
        TestTDS result = this._distinct(sortedByNonTransposeColumns.getTwo()).removeColumns(this.columnsOrdered.reject(nonTransposeColumns::contains).toSet());
        result = newColumns.injectInto(result, (tds, newColInfo) ->
        {
            int size = (int) tds.rowCount;
            String name = newColInfo.getColumnName();
            Object dataAsObject;
            boolean[] isNull = new boolean[size];
            Arrays.fill(isNull, Boolean.TRUE);
            TestTDS sortedByNonTransposeColumnsOne = sortedByNonTransposeColumns.getOne();
            MutableList<Pair<Integer, Integer>> sortedByNonTransposeColumnsTwo = sortedByNonTransposeColumns.getTwo();

            Object[] values = new Object[size];
            for (int i = 0; i < size; i++)
            {
                for (int j = sortedByNonTransposeColumnsTwo.get(i).getOne(); j < sortedByNonTransposeColumnsTwo.get(i).getTwo(); j++)
                {
                    if (newColInfo.match(sortedByNonTransposeColumnsOne, j))
                    {
                        values[i] = ((Object[]) sortedByNonTransposeColumnsOne.dataByColumnName.get(newColInfo.getAggColumnName()))[j];
                    }
                }
            }
            dataAsObject = values;
            tds.dataByColumnName.put(name, dataAsObject);
            tds.columnsOrdered.add(name);
            tds.pureTypesByColumnName.put(name, newColInfo.getPureType());
            return tds;
        });

        return result;
    }

    public TestTDS sortForOuterJoin(boolean isLeft, LambdaFunction<?> lambdaFunction, ProcessorSupport processorSupport)
    {
        FunctionType fType = (FunctionType) Function.computeFunctionType(lambdaFunction, processorSupport);
        ValueSpecification vs = lambdaFunction._expressionSequence().getFirst();
        if (vs instanceof SimpleFunctionExpression)
        {
            SimpleFunctionExpression fe = (SimpleFunctionExpression) vs;
            String funcName = fe._func().getName();
            String truncatedFuncName = funcName.substring(0, funcName.indexOf("_"));
            if (Sets.mutable.with("lessThan", "greaterThan", "lessThanEquals", "greaterThanEquals").contains(truncatedFuncName))
            {
                SortDirection sortDirection = Sets.mutable.with("lessThan", "lessThanEquals").contains(truncatedFuncName) ? SortDirection.ASC : SortDirection.DESC;
                ValueSpecification left = fe._parametersValues().toList().get(0);
                ValueSpecification right = fe._parametersValues().toList().get(1);
                if (left instanceof SimpleFunctionExpression && right instanceof SimpleFunctionExpression)
                {
                    SimpleFunctionExpression leftF = (SimpleFunctionExpression) left;
                    SimpleFunctionExpression rightF = (SimpleFunctionExpression) right;
                    MutableList<String> signatureParameters = fType._parameters().collect(VariableExpressionAccessor::_name).toList();
                    while (leftF._parametersValues().getFirst() instanceof SimpleFunctionExpression)
                    {
                        leftF = (SimpleFunctionExpression) leftF._parametersValues().getFirst();
                    }
                    while (rightF._parametersValues().getFirst() instanceof SimpleFunctionExpression)
                    {
                        rightF = (SimpleFunctionExpression) rightF._parametersValues().getFirst();
                    }

                    String leftName = (((VariableExpression) leftF._parametersValues().getFirst())._name());
                    if (leftName.equals(signatureParameters.get(0)))
                    {
                        return this.sort(new SortInfo(isLeft ? leftF._func()._name() : rightF._func()._name(), sortDirection)).getOne();
                    }
                    else
                    {
                        return this.sort(new SortInfo(isLeft ? rightF._func()._name() : leftF._func()._name(), sortDirection)).getOne();
                    }
                }
            }
        }
        return this;
    }
}
