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
import io.deephaven.csv.reading.CsvReader;
import io.deephaven.csv.sinks.SinkFactory;
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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class TestTDS
{
    protected MutableMap<String, Object> dataByColumnName = Maps.mutable.empty();
    protected MutableMap<String, Object> isNullByColumn = Maps.mutable.empty();
    protected MutableMap<String, DataType> columnType = Maps.mutable.empty();
    protected MutableList<String> columnsOrdered = Lists.mutable.empty();
    protected long rowCount;

    public TestTDS()
    {
    }

    public TestTDS newTDS()
    {
        return new TestTDS();
    }

    public TestTDS newEmptyTDS()
    {
        return newTDS(columnsOrdered, columnType, 0);
    }

    public TestTDS newNullTDS()
    {
        TestTDS testTDS = newTDS(columnsOrdered, columnType, 1);
        for (String col : columnsOrdered)
        {
            testTDS.isNullByColumn.put(col, new boolean[]{true});
            switch (columnType.get(col))
            {
                case INT:
                {
                    testTDS.dataByColumnName.put(col, new int[(int) testTDS.rowCount]);
                    break;
                }
                case CHAR:
                {
                    testTDS.dataByColumnName.put(col, new char[(int) this.rowCount]);
                    break;
                }
                case STRING:
                {
                    testTDS.dataByColumnName.put(col, new String[(int) this.rowCount]);
                    break;
                }
                case FLOAT:
                case DOUBLE:
                {
                    testTDS.dataByColumnName.put(col, new double[(int) this.rowCount]);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + columnType.get(col) + " not supported yet!");
            }
        }
        return testTDS;
    }

    public TestTDS newTDS(MutableList<String> columnOrdered, MutableMap<String, DataType> columnType, int rows)
    {
        return new TestTDS(columnOrdered, columnType, rows);
    }

    public TestTDS(String csv)
    {
        this(readCsv(csv));
    }

    public TestTDS(CsvReader.Result result)
    {
        this.rowCount = result.numRows();

        ArrayIterate.forEach(result.columns(), c ->
        {
            columnsOrdered.add(c.name());
            columnType.put(c.name(), c.dataType());
            dataByColumnName.put(c.name(), c.data());
            boolean[] array = new boolean[(int) this.rowCount];
            isNullByColumn.put(c.name(), array);
            switch (c.dataType())
            {
                case INT:
                    for (int i = 0; i < this.rowCount; i++)
                    {
                        array[i] = ((int[]) c.data())[i] == Integer.MIN_VALUE;
                    }
                    break;
                case CHAR:
                    for (int i = 0; i < this.rowCount; i++)
                    {
                        array[i] = ((char[]) c.data())[i] == Character.MIN_VALUE;
                    }
                    break;
                case DOUBLE:
                    for (int i = 0; i < this.rowCount; i++)
                    {
                        array[i] = ((double[]) c.data())[i] == -Double.MAX_VALUE;
                    }
                    break;
                case STRING:
                    for (int i = 0; i < this.rowCount; i++)
                    {
                        if ("null".equals(((String[]) c.data())[i]))
                        {
                            ((String[]) c.data())[i] = null;
                        }
                    }
            }
        });
    }

    protected TestTDS(MutableList<String> columnOrdered, MutableMap<String, DataType> columnType, int rows)
    {
        this.columnsOrdered = columnOrdered;
        this.columnType = columnType;
        this.rowCount = rows;
        this.columnType.keyValuesView().forEach(p ->
        {
            switch (p.getTwo())
            {
                case INT:
                {
                    this.dataByColumnName.put(p.getOne(), new int[(int) this.rowCount]);
                    boolean[] array = new boolean[(int) this.rowCount];
                    Arrays.fill(array, Boolean.TRUE);
                    this.isNullByColumn.put(p.getOne(), array);
                    break;
                }
                case CHAR:
                {
                    this.dataByColumnName.put(p.getOne(), new char[(int) this.rowCount]);
                    boolean[] array = new boolean[(int) this.rowCount];
                    Arrays.fill(array, Boolean.TRUE);
                    this.isNullByColumn.put(p.getOne(), array);
                    break;
                }
                case STRING:
                {
                    this.dataByColumnName.put(p.getOne(), new String[(int) this.rowCount]);
                    boolean[] array = new boolean[(int) this.rowCount];
                    Arrays.fill(array, Boolean.TRUE);
                    this.isNullByColumn.put(p.getOne(), array);
                    break;
                }
                case FLOAT:
                case DOUBLE:
                {
                    this.dataByColumnName.put(p.getOne(), new double[(int) this.rowCount]);
                    boolean[] array = new boolean[(int) this.rowCount];
                    Arrays.fill(array, Boolean.TRUE);
                    this.isNullByColumn.put(p.getOne(), array);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + columnType.get(p.getOne()) + " not supported yet!");
            }
        });
    }

    public TestTDS setNull()
    {
        TestTDS res = this.copy();
        res.rowCount = 1;
        boolean[] array = new boolean[(int) res.rowCount];
        Arrays.fill(array, Boolean.TRUE);
        res.columnType.keyValuesView().forEach(c ->
        {
            switch (c.getTwo())
            {
                case INT:
                    res.dataByColumnName.put(c.getOne(), new int[1]);
                    res.isNullByColumn.put(c.getOne(), array);
                    break;
                case CHAR:
                    res.dataByColumnName.put(c.getOne(), new char[1]);
                    res.isNullByColumn.put(c.getOne(), array);
                    break;
                case FLOAT:
                case DOUBLE:
                    res.dataByColumnName.put(c.getOne(), new double[1]);
                    res.isNullByColumn.put(c.getOne(), array);
                    break;
                case STRING:
                    res.dataByColumnName.put(c.getOne(), new String[1]);
                    break;
                default:
                    throw new RuntimeException("ERROR " + columnType.get(c.getTwo()) + " not supported yet!");
            }
        });
        return res;
    }

    private static SinkFactory makeMySinkFactory()
    {
        return SinkFactory.arrays(
                Byte.MIN_VALUE,
                Short.MIN_VALUE,
                Integer.MIN_VALUE,
                Long.MIN_VALUE,
                -Float.MAX_VALUE,
                -Double.MAX_VALUE,
                Byte.MIN_VALUE,
                Character.MIN_VALUE,
                "null",
                Long.MIN_VALUE,
                Long.MIN_VALUE);
    }

    public static CsvReader.Result readCsv(String csv)
    {
        try
        {
            return CsvReader.read(CsvSpecs.csv(), new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), SinkFactory.arrays());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error parsing:\n" + csv, e);
        }
    }

    public TestTDS join(TestTDS otherTDS)
    {
        MutableMap<String, DataType> columnTypes = Maps.mutable.empty();
        columnTypes.putAll(this.columnType);
        columnTypes.putAll(otherTDS.columnType);
        MutableList<String> columnOrdered = Lists.mutable.empty();
        columnOrdered.addAll(this.columnsOrdered);
        columnOrdered.addAll(otherTDS.columnsOrdered);
        TestTDS res = newTDS(columnOrdered, columnTypes, (int) (rowCount * otherTDS.rowCount));

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
        return res;
    }

    public void setValue(String columnName, int row, TestTDS srcTDS, int srcRow)
    {
        Object dataAsObject = dataByColumnName.get(columnName);
        boolean[] nullAsObject = (boolean[]) isNullByColumn.get(columnName);
        boolean[] nullAsObjectSrc = (boolean[]) srcTDS.isNullByColumn.get(columnName);
        switch (columnType.get(columnName))
        {
            case INT:
            {
                ((int[]) dataAsObject)[row] = ((int[]) srcTDS.dataByColumnName.get(columnName))[srcRow];
                nullAsObject[row] = nullAsObjectSrc[srcRow];
                break;
            }
            case CHAR:
            {
                ((char[]) dataAsObject)[row] = ((char[]) srcTDS.dataByColumnName.get(columnName))[srcRow];
                nullAsObject[row] = nullAsObjectSrc[srcRow];
                break;
            }
            case STRING:
            {
                ((String[]) dataAsObject)[row] = ((String[]) srcTDS.dataByColumnName.get(columnName))[srcRow];
                break;
            }
            case FLOAT:
            case DOUBLE:
            {
                ((double[]) dataAsObject)[row] = ((double[]) srcTDS.dataByColumnName.get(columnName))[srcRow];
                nullAsObject[row] = nullAsObjectSrc[srcRow];
                break;
            }
            default:
                throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported yet!");
        }
    }

    public TestTDS copy()
    {
        TestTDS result = newTDS();
        result.rowCount = rowCount;
        result.columnsOrdered = Lists.mutable.withAll(columnsOrdered);
        result.columnType = Maps.mutable.withMap(columnType);
        result.dataByColumnName = Maps.mutable.empty();
        result.isNullByColumn = Maps.mutable.empty();
        dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject = dataByColumnName.get(columnName);
            Object copy;
            Object copyIsNull = null;
            switch (columnType.get(columnName))
            {
                case INT:
                {
                    copy = Arrays.copyOf((int[]) dataAsObject, (int) rowCount);
                    copyIsNull = Arrays.copyOf((boolean[]) this.isNullByColumn.get(columnName), (int) rowCount);
                    break;
                }
                case CHAR:
                {
                    copy = Arrays.copyOf((char[]) dataAsObject, (int) rowCount);
                    copyIsNull = Arrays.copyOf((boolean[]) isNullByColumn.get(columnName), (int) rowCount);
                    break;
                }
                case STRING:
                {
                    copy = Arrays.copyOf((String[]) dataAsObject, (int) rowCount);
                    break;
                }
                case FLOAT:
                case DOUBLE:
                {
                    copy = Arrays.copyOf((double[]) dataAsObject, (int) rowCount);
                    copyIsNull = Arrays.copyOf((boolean[]) isNullByColumn.get(columnName), (int) rowCount);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported yet!");
            }
            result.dataByColumnName.put(columnName, copy);
            if (copyIsNull != null)
            {
                result.isNullByColumn.put(columnName, copyIsNull);
            }
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
            boolean[] isNull = (boolean[]) copy.isNullByColumn.get(columnName);
            boolean[] isNullTarget = new boolean[(int) copy.rowCount - size];
            switch (copy.columnType.get(columnName))
            {
                case INT:
                {
                    int[] src = (int[]) dataAsObject;
                    int[] target = new int[(int) copy.rowCount - size];
                    int j = 0;
                    for (int i = 0; i < copy.rowCount; i++)
                    {
                        if (!rows.contains(i))
                        {
                            target[j] = src[i];
                            isNullTarget[j++] = isNull[i];
                        }
                    }
                    copy.dataByColumnName.put(columnName, target);
                    copy.isNullByColumn.put(columnName, isNullTarget);
                    break;
                }
                case CHAR:
                {
                    char[] src = (char[]) dataAsObject;
                    char[] target = new char[(int) copy.rowCount - size];
                    int j = 0;
                    for (int i = 0; i < copy.rowCount; i++)
                    {
                        if (!rows.contains(i))
                        {
                            target[j] = src[i];
                            isNullTarget[j++] = isNull[i];
                        }
                    }
                    copy.dataByColumnName.put(columnName, target);
                    copy.isNullByColumn.put(columnName, isNullTarget);
                    break;
                }
                case STRING:
                {
                    String[] src = (String[]) dataAsObject;
                    String[] target = new String[(int) copy.rowCount - size];
                    int j = 0;
                    for (int i = 0; i < copy.rowCount; i++)
                    {
                        if (!rows.contains(i))
                        {
                            target[j++] = src[i];
                        }
                    }
                    copy.dataByColumnName.put(columnName, target);
                    break;
                }
                case FLOAT:
                case DOUBLE:
                {
                    double[] src = (double[]) dataAsObject;
                    double[] target = new double[(int) copy.rowCount - size];
                    int j = 0;
                    for (int i = 0; i < copy.rowCount; i++)
                    {
                        if (!rows.contains(i))
                        {
                            target[j] = src[i];
                            isNullTarget[j++] = isNull[i];
                        }
                    }
                    copy.dataByColumnName.put(columnName, target);
                    copy.isNullByColumn.put(columnName, isNullTarget);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + copy.columnType.get(columnName) + " not supported in drop!");
            }
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
        TestTDS result = newTDS();
        result.rowCount = this.rowCount + tds2.rowCount;
        result.columnType = Maps.mutable.withMap(columnType);
        result.columnsOrdered = Lists.mutable.withAll(columnsOrdered);

        dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject1 = dataByColumnName.get(columnName);
            Object dataAsObject2 = tds2.dataByColumnName.get(columnName);
            Object copy;
            boolean[] newIsNull = null;
            switch (columnType.get(columnName))
            {
                case INT:
                {
                    int[] _copy = Arrays.copyOf((int[]) dataAsObject1, (int) result.rowCount);
                    System.arraycopy((int[]) dataAsObject2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    newIsNull = concatenate((boolean[]) isNullByColumn.get(columnName), (boolean[]) tds2.isNullByColumn.get(columnName));
                    break;
                }
                case CHAR:
                {
                    char[] _copy = Arrays.copyOf((char[]) dataAsObject1, (int) result.rowCount);
                    System.arraycopy((char[]) dataAsObject2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    newIsNull = concatenate((boolean[]) isNullByColumn.get(columnName), (boolean[]) tds2.isNullByColumn.get(columnName));
                    break;
                }
                case STRING:
                {
                    String[] _copy = Arrays.copyOf((String[]) dataAsObject1, (int) result.rowCount);
                    System.arraycopy((String[]) dataAsObject2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    break;
                }
                case FLOAT:
                case DOUBLE:
                {
                    double[] _copy = Arrays.copyOf((double[]) dataAsObject1, (int) result.rowCount);
                    System.arraycopy((double[]) dataAsObject2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    newIsNull = concatenate((boolean[]) isNullByColumn.get(columnName), (boolean[]) tds2.isNullByColumn.get(columnName));
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported yet!");
            }
            result.dataByColumnName.put(columnName, copy);
            if (newIsNull != null)
            {
                result.isNullByColumn.put(columnName, newIsNull);
            }
        });
        return result;
    }

    private boolean[] concatenate(boolean[] set1, boolean[] set2)
    {
        boolean[] _copy = Arrays.copyOf(set1, set1.length + set2.length);
        System.arraycopy(set2, 0, _copy, set1.length, set2.length);
        return _copy;
    }

    public TestTDS addColumn(ColumnValue columnValue)
    {
        if (columnValue.nulls == null)
        {
            return addColumn(columnValue.name, columnValue.type, columnValue.result);
        }
        else
        {
            return addColumn(columnValue.name, columnValue.type, columnValue.result, columnValue.nulls);
        }
    }

    public Pair<TestTDS, MutableList<Pair<Integer, Integer>>> wrapFullTDS()
    {
        return Tuples.pair(this.copy(), org.eclipse.collections.impl.factory.Lists.mutable.with(Tuples.pair(0, (int) this.getRowCount())));
    }

    public TestTDS addColumn(String name, DataType dataType, Object res)
    {
        boolean[] array = new boolean[Array.getLength(res)];
        Arrays.fill(array, Boolean.FALSE);
        return addColumn(name, dataType, res, array);
    }

    public TestTDS addColumn(String name, DataType dataType, Object res, Object nulls)
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
        this.columnsOrdered.add(name);
        this.columnType.put(name, dataType);
        switch (dataType)
        {
            case INT:
            case CHAR:
            case FLOAT:
            case DOUBLE:
                isNullByColumn.put(name, nulls);
        }
        return this;
    }

    public TestTDS removeColumns(MutableSet<? extends String> columns)
    {
        TestTDS copy = this.copy();
        copy.columnsOrdered.removeAll(columns);
        copy.columnType.removeAllKeys(columns);
        copy.isNullByColumn.removeAllKeys(columns);
        copy.dataByColumnName.removeAllKeys(columns);
        return copy;
    }

    public TestTDS select(MutableSet<? extends String> columns)
    {
        MutableSet<String> allColumns = Sets.mutable.withAll(this.getColumnNames());
        allColumns.removeAll(columns);
        return removeColumns(allColumns);
    }

    public TestTDS rename(String oldName, String newName)
    {
        TestTDS copy = this.copy();
        DataType type = copy.columnType.get(oldName);
        Object data = copy.dataByColumnName.get(oldName);
        Object oldIsNull = copy.isNullByColumn.get(oldName);
        copy.columnType.put(newName, type);
        copy.dataByColumnName.put(newName, data);
        copy.isNullByColumn.put(newName, oldIsNull);
        copy.columnsOrdered.add(newName);
        copy.columnType.remove(oldName);
        copy.dataByColumnName.remove(oldName);
        copy.isNullByColumn.remove(oldName);
        copy.columnsOrdered.remove(oldName);
        return copy;
    }

    public TestTDS slice(int from, int to)
    {
        TestTDS copy = this.copy();
        copy.dataByColumnName.forEachKey(columnName ->
        {
            Object dataAsObject = copy.dataByColumnName.get(columnName);
            boolean[] isNull = (boolean[]) copy.isNullByColumn.get(columnName);
            switch (copy.columnType.get(columnName))
            {
                case INT:
                {
                    copy.dataByColumnName.put(columnName, Arrays.copyOfRange((int[]) dataAsObject, from, to));
                    copy.isNullByColumn.put(columnName, Arrays.copyOfRange(isNull, from, to));
                    break;
                }
                case CHAR:
                {
                    copy.dataByColumnName.put(columnName, Arrays.copyOfRange((char[]) dataAsObject, from, to));
                    copy.isNullByColumn.put(columnName, Arrays.copyOfRange(isNull, from, to));
                    break;
                }
                case STRING:
                {
                    copy.dataByColumnName.put(columnName, Arrays.copyOfRange((String[]) dataAsObject, from, to));
                    break;
                }
                case FLOAT:
                case DOUBLE:
                {
                    copy.dataByColumnName.put(columnName, Arrays.copyOfRange((double[]) dataAsObject, from, to));
                    copy.isNullByColumn.put(columnName, Arrays.copyOfRange(isNull, from, to));
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + copy.columnType.get(columnName) + " not supported yet!");
            }
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

    public TestTDS distinct(MutableList<String> columns)
    {
        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> res = this.sort(columns.collect(c -> new SortInfo(c, SortDirection.ASC)));
        return res.getOne()._distinct(res.getTwo());
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
        SortInfo currentSort = sortInfos.getFirst();
        this.sortOneLevel(copy, currentSort, start, end);
        if (!sortInfos.isEmpty())
        {
            String columnName = currentSort.columnName;
            Object dataAsObject = copy.dataByColumnName.get(columnName);
            switch (copy.columnType.get(columnName))
            {
                case INT:
                {
                    int[] src = (int[]) dataAsObject;
                    int val = src[start];
                    int subStart = start;

                    for (int i = start; i < end; i++)
                    {
                        if (src[i] != val || (src[i] == val && i == end - 1))
                        {
                            int realEnd = (src[i] == val && i == end - 1) ? end : i;
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
                    break;
                }
                case CHAR:
                {
                    char[] src = (char[]) dataAsObject;
                    char val = src[start];
                    int subStart = start;

                    for (int i = start; i < end; i++)
                    {
                        if (src[i] != val || (src[i] == val && i == end - 1))
                        {
                            int realEnd = (src[i] == val && i == end - 1) ? end : i;
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
                    break;
                }
                case STRING:
                {
                    String[] src = (String[]) dataAsObject;
                    String val = src[start];
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
                    break;
                }
                case FLOAT:
                case DOUBLE:
                {
                    double[] src = (double[]) dataAsObject;
                    double val = src[start];
                    int subStart = start;
                    for (int i = start; i < end; i++)
                    {
                        if (src[i] != val || (src[i] == val && i == end - 1))
                        {
                            int realEnd = (src[i] == val && i == end - 1) ? end : i;
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
                    break;
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
        Object dataAsObject = copy.dataByColumnName.get(columnName);
        switch (copy.columnType.get(columnName))
        {
            case INT:
            {
                int[] src = (int[]) dataAsObject;
                MutableList<Pair<Integer, Integer>> list = Lists.mutable.empty();
                for (int i = start; i < end; i++)
                {
                    list.add(Tuples.pair(i, src[i]));
                }
                list.sortThisBy(Pair::getTwo);
                if (sortInfo.direction == SortDirection.DESC)
                {
                    list.reverseThis();
                }
                this.reorder(copy, list.collect(Pair::getOne), start, end);
                break;
            }
            case CHAR:
            {
                char[] src = (char[]) dataAsObject;
                MutableList<Pair<Integer, Character>> list = Lists.mutable.empty();
                for (int i = start; i < end; i++)
                {
                    list.add(Tuples.pair(i, src[i]));
                }
                list.sortThisBy(Pair::getTwo);
                if (sortInfo.direction == SortDirection.DESC)
                {
                    list.reverseThis();
                }
                this.reorder(copy, list.collect(Pair::getOne), start, end);
                break;
            }
            case STRING:
            {
                String[] src = (String[]) dataAsObject;
                MutableList<Pair<Integer, String>> list = Lists.mutable.empty();
                for (int i = start; i < end; i++)
                {
                    list.add(Tuples.pair(i, src[i]));
                }
                list.sortThisBy(Pair::getTwo);
                if (sortInfo.direction == SortDirection.DESC)
                {
                    list.reverseThis();
                }
                this.reorder(copy, list.collect(Pair::getOne), start, end);
                break;
            }
            case FLOAT:
            case DOUBLE:
            {
                double[] src = (double[]) dataAsObject;
                MutableList<Pair<Integer, Double>> list = Lists.mutable.empty();
                for (int i = start; i < end; i++)
                {
                    list.add(Tuples.pair(i, src[i]));
                }
                list.sortThisBy(Pair::getTwo);
                if (sortInfo.direction == SortDirection.DESC)
                {
                    list.reverseThis();
                }
                this.reorder(copy, list.collect(Pair::getOne), start, end);
                break;
            }
            default:
                throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in drop!");
        }
    }

    private void reorder(TestTDS copy, MutableList<Integer> indices, int start, int end)
    {
        for (String columnName : copy.dataByColumnName.keysView())
        {
            Object dataAsObject = copy.dataByColumnName.get(columnName);
            boolean[] isNull = (boolean[]) copy.isNullByColumn.get(columnName);
            switch (copy.columnType.get(columnName))
            {
                case INT:
                {
                    int[] src = (int[]) dataAsObject;
                    boolean[] isNullResult = new boolean[(int) copy.rowCount];
                    int[] result = new int[(int) copy.rowCount];
                    for (int i = 0; i < indices.size(); i++)
                    {
                        result[i] = src[indices.get(i)];
                        isNullResult[i] = isNull[indices.get(i)];
                    }
                    System.arraycopy(result, 0, src, start, end - start);
                    System.arraycopy(isNullResult, 0, isNull, start, end - start);
                    break;
                }
                case CHAR:
                {
                    char[] src = (char[]) dataAsObject;
                    boolean[] isNullResult = new boolean[(int) copy.rowCount];
                    char[] result = new char[(int) copy.rowCount];
                    for (int i = 0; i < indices.size(); i++)
                    {
                        result[i] = src[indices.get(i)];
                        isNullResult[i] = isNull[indices.get(i)];
                    }
                    System.arraycopy(result, 0, src, start, end - start);
                    System.arraycopy(isNullResult, 0, isNull, start, end - start);
                    break;
                }
                case STRING:
                {
                    String[] src = (String[]) dataAsObject;
                    String[] result = new String[(int) copy.rowCount];
                    for (int i = 0; i < indices.size(); i++)
                    {
                        result[i] = src[indices.get(i)];
                    }
                    System.arraycopy(result, 0, src, start, end - start);
                    break;
                }
                case FLOAT:
                case DOUBLE:
                {
                    double[] src = (double[]) dataAsObject;
                    boolean[] isNullResult = new boolean[(int) copy.rowCount];
                    double[] result = new double[(int) copy.rowCount];
                    for (int i = 0; i < indices.size(); i++)
                    {
                        result[i] = src[indices.get(i)];
                        isNullResult[i] = isNull[indices.get(i)];
                    }
                    System.arraycopy(result, 0, src, start, end - start);
                    System.arraycopy(isNullResult, 0, isNull, start, end - start);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + copy.columnType.get(columnName) + " not supported in drop!");
            }
        }
    }

    public String toString()
    {
        RichIterable<String> columns = this.dataByColumnName.keysView();
        MutableList<String> rows = Lists.mutable.empty();
        for (int i = 0; i < rowCount; i++)
        {
            int finalI = i;
            rows.add(columns.collect(columnName ->
            {
                Object dataAsObject = dataByColumnName.get(columnName);
                boolean[] isNull = (boolean[]) isNullByColumn.get(columnName);

                switch (columnType.get(columnName))
                {
                    case INT:
                    {
                        return isNull[finalI] ? "NULL" : ((int[]) dataAsObject)[finalI];
                    }
                    case CHAR:
                    {
                        return isNull[finalI] ? "NULL" : ((char[]) dataAsObject)[finalI];
                    }
                    case STRING:
                    {
                        String res = ((String[]) dataAsObject)[finalI];
                        return res == null ? "NULL" : res;
                    }
                    case FLOAT:
                    case DOUBLE:
                    {
                        return isNull[finalI] ? "NULL" : ((double[]) dataAsObject)[finalI];
                    }
                    default:
                        return "";
                }
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
            if (!leftS.fullMatch(cols, resS, rowLeftCurs, rowResCurs))
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

        TestTDS missingTDS = newTDS(res.columnsOrdered.clone(), res.columnType.clone(), missings.size());

        int cursor = 0;
        for (Integer missing : missings)
        {
            for (String col : columnType.keysView())
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
            switch (columnType.get(col))
            {
                case INT:
                {
                    valid = valid && ((int[]) firstDataAsObject)[rowFirst] == ((int[]) secondDataAsObject)[rowSecond];
                    break;
                }
                case CHAR:
                {
                    valid = valid && ((char[]) firstDataAsObject)[rowFirst] == ((char[]) secondDataAsObject)[rowSecond];
                    break;
                }
                case STRING:
                {
                    valid = valid && Objects.equals(((String[]) firstDataAsObject)[rowFirst], (((String[]) secondDataAsObject)[rowSecond]));
                    break;
                }
                case FLOAT:
                case DOUBLE:
                {
                    valid = valid && ((double[]) firstDataAsObject)[rowFirst] == ((double[]) secondDataAsObject)[rowSecond];
                    break;
                }
                default:
                    throw new RuntimeException("ERROR");
            }
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
}
