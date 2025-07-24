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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpressionAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window;
import org.finos.legend.pure.runtime.java.shared.variant.VariantInstanceImpl;

public abstract class TestTDS
{
    public static final long LONG_NULL_SENTINEL = 9_223_372_036_854_775_783L; //largest prime for 64 signed numbers
    public static final double DOUBLE_NULL_SENTINEL = Double.NEGATIVE_INFINITY;
    public static final byte BOOLEAN_AS_BYTE_SENTINEL = Byte.MIN_VALUE;
    public static final long DATE_TIME_AS_LONG_SENTINEL = Long.MIN_VALUE;

    protected MutableMap<String, Object> dataByColumnName = Maps.mutable.empty();
    protected MutableMap<String, boolean[]> isNullByColumn = Maps.mutable.empty();
    protected MutableMap<String, DataType> columnType = Maps.mutable.empty();
    protected MutableList<String> columnsOrdered = Lists.mutable.empty();
    protected long rowCount;

    public TestTDS()
    {
    }

    public abstract Object getValueAsCoreInstance(String columnName, int rowNum);

    public Object getValue(String columnName, int rowNum)
    {
        Object dataAsObject = dataByColumnName.get(columnName);
        boolean[] isNull = isNullByColumn.get(columnName);
        Object result;
        switch (columnType.get(columnName))
        {
            case LONG:
            {
                long[] data = (long[]) dataAsObject;
                result = !isNull[rowNum] ? data[rowNum] : null;
                break;
            }
            case BOOLEAN_AS_BYTE:
            {
                boolean[] data = (boolean[]) dataAsObject;
                result = !isNull[rowNum] ? data[rowNum] : null;
                break;
            }
            case DOUBLE:
            {
                double[] data = (double[]) dataAsObject;
                result = !isNull[rowNum] ? data[rowNum] : null;
                break;
            }
            case STRING:
            case CUSTOM:
            case DATETIME_AS_LONG:
                Object[] data = (Object[]) dataAsObject;
                result = data[rowNum];
                break;
            default:
                throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported in getValue");
        }
        return result;
    }

    public abstract TestTDS newTDS();

    public TestTDS newEmptyTDS()
    {
        return newTDS(columnsOrdered, columnType, 0);
    }

    public TestTDS newNullTDS()
    {
        TestTDS testTDS = newTDS(columnsOrdered, columnType, 1);
        for (String col : columnsOrdered)
        {
            testTDS.isNullByColumn.put(col, new boolean[] {true});
            switch (columnType.get(col))
            {
                case LONG:
                {
                    testTDS.dataByColumnName.put(col, new long[(int) testTDS.rowCount]);
                    break;
                }
                case BOOLEAN_AS_BYTE:
                {
                    testTDS.dataByColumnName.put(col, new boolean[(int) testTDS.rowCount]);
                    break;
                }
                case STRING:
                {
                    testTDS.dataByColumnName.put(col, new String[(int) testTDS.rowCount]);
                    break;
                }
                case DOUBLE:
                {
                    testTDS.dataByColumnName.put(col, new double[(int) testTDS.rowCount]);
                    break;
                }
                case DATETIME_AS_LONG:
                {
                    testTDS.dataByColumnName.put(col, new PureDate[(int) testTDS.rowCount]);
                    break;
                }
                case CUSTOM:
                {
                    testTDS.dataByColumnName.put(col, new Variant[(int) this.rowCount]);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + columnType.get(col) + " not supported yet!");
            }
        }
        return testTDS;
    }

    public abstract TestTDS newTDS(MutableList<String> columnOrdered, MutableMap<String, DataType> columnType, int rows);

    public TestTDS(String csv, ProcessorSupport processorSupport)
    {
        this(readCsv(csv), processorSupport);
    }

    public TestTDS(CsvReader.Result result, ProcessorSupport processorSupport)
    {
        this.rowCount = result.numRows();

        ArrayIterate.forEach(result.columns(), c ->
        {
            String name = c.name();
            DataType type = c.dataType();
            int typeIndex = name.indexOf(':');
            if (typeIndex != -1)
            {
                String specifiedType = name.substring(typeIndex + 1);
                switch (specifiedType)
                {
                    case M3Paths.Boolean:
                        type = DataType.BOOLEAN_AS_BYTE;
                        break;
                    case M3Paths.Integer:
                        type = DataType.LONG;
                        break;
                    case M3Paths.DateTime:
                        type = DataType.DATETIME_AS_LONG;
                        break;
                    case M3Paths.Float:
                        type = DataType.DOUBLE;
                        break;
                    case M3Paths.String:
                        type = DataType.STRING;
                        break;
                    case M3Paths.Variant:
                        type = DataType.CUSTOM;
                        break;
                    default:
                }

                name = name.substring(0, typeIndex);
            }

            columnsOrdered.add(name);
            columnType.put(name, type);
            Object data = getDataAsType(c, type, rowCount);
            dataByColumnName.put(name, data);
            boolean[] isNullFlag = new boolean[(int) this.rowCount];
            isNullByColumn.put(name, isNullFlag);
            switch (type)
            {
                case LONG:
                    for (int i = 0; i < this.rowCount; i++)
                    {
                        isNullFlag[i] = ((long[]) data)[i] == LONG_NULL_SENTINEL;
                    }
                    break;
                case BOOLEAN_AS_BYTE:
                    boolean[] booleans = new boolean[(int) this.rowCount];
                    dataByColumnName.put(name, booleans);
                    for (int i = 0; i < this.rowCount; i++)
                    {
                        byte booleanAsByte = ((byte[]) data)[i];
                        booleans[i] = booleanAsByte == 1;
                        isNullFlag[i] = booleanAsByte == BOOLEAN_AS_BYTE_SENTINEL;
                    }
                    break;
                case DOUBLE:
                    for (int i = 0; i < this.rowCount; i++)
                    {
                        isNullFlag[i] = ((double[]) data)[i] == DOUBLE_NULL_SENTINEL;
                    }
                    break;
                case CUSTOM:
                    Variant[] variants = new Variant[(int) this.rowCount];
                    dataByColumnName.put(name, variants);
                    for (int i = 0; i < this.rowCount; i++)
                    {
                        Object origData = c.data();
                        String value;
                        switch (c.dataType()) // check original type
                        {
                            case LONG:
                                long lVal = ((long[]) origData)[i];
                                value = lVal == LONG_NULL_SENTINEL ? null : Long.toString(lVal);
                                break;
                            case DOUBLE:
                                double dVal = ((double[]) origData)[i];
                                value = dVal == DOUBLE_NULL_SENTINEL ? null : Double.toString(dVal);
                                break;
                            case BOOLEAN_AS_BYTE:
                                byte bVal = ((byte[]) origData)[i];
                                value = bVal == BOOLEAN_AS_BYTE_SENTINEL ? null : Boolean.toString(bVal == 1);
                                break;
                            case STRING:
                                value = ((String[]) origData)[i];
                                break;
                            default:
                                throw new RuntimeException("ERROR " + c.dataType() + " not supported yet on variant!");
                        }
                        variants[i] = value == null ? null : VariantInstanceImpl.newVariant(value, null, processorSupport);
                    }
                    break;
                case STRING:
                    // nothing... csv parser manage how to handle null sentinels
                    break;
                case DATETIME_AS_LONG:
                    PureDate[] dates = new PureDate[(int) this.rowCount];
                    dataByColumnName.put(name, dates);
                    for (int i = 0; i < this.rowCount; i++)
                    {
                        long value = ((long[]) data)[i];
                        dates[i] = value == DATE_TIME_AS_LONG_SENTINEL ? null : DateFunctions.fromDate(new Date(value / 1000000));
                    }
                    break;
                default:
                    throw new RuntimeException(c.dataType() + " not supported yet!");
            }
        });
    }

    private Object getDataAsType(CsvReader.ResultColumn c, DataType type, long rowCount)
    {
        if (rowCount == 0)
        {
            switch (type)
            {
                case LONG:
                    return new long[0];
                case BOOLEAN_AS_BYTE:
                    return new byte[0];
                case DOUBLE:
                    return new double[0];
                case STRING:
                    return new String[0];
                case CUSTOM:
                    return new Variant[0];
                case DATETIME_AS_LONG:
                    return new PureDate[0];
                default:
                    throw new RuntimeException("ERROR " + type + " not supported yet!");
            }
        }
        // else do proper conversion...
        return c.data();
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
                case LONG:
                {
                    this.dataByColumnName.put(p.getOne(), new long[(int) this.rowCount]);
                    boolean[] array = new boolean[(int) this.rowCount];
                    Arrays.fill(array, Boolean.TRUE);
                    this.isNullByColumn.put(p.getOne(), array);
                    break;
                }
                case BOOLEAN_AS_BYTE:
                {
                    this.dataByColumnName.put(p.getOne(), new boolean[(int) this.rowCount]);
                    boolean[] array = new boolean[(int) this.rowCount];
                    Arrays.fill(array, Boolean.TRUE);
                    this.isNullByColumn.put(p.getOne(), array);
                    break;
                }
                case STRING:
                {
                    this.dataByColumnName.put(p.getOne(), new String[(int) this.rowCount]);
                    break;
                }
                case CUSTOM:
                {
                    this.dataByColumnName.put(p.getOne(), new Variant[(int) this.rowCount]);
                    break;
                }
                case DOUBLE:
                {
                    this.dataByColumnName.put(p.getOne(), new double[(int) this.rowCount]);
                    boolean[] array = new boolean[(int) this.rowCount];
                    Arrays.fill(array, Boolean.TRUE);
                    this.isNullByColumn.put(p.getOne(), array);
                    break;
                }
                case DATETIME_AS_LONG:
                    this.dataByColumnName.put(p.getOne(), new PureDate[(int) this.rowCount]);
                    break;
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
                case LONG:
                    res.dataByColumnName.put(c.getOne(), new long[1]);
                    res.isNullByColumn.put(c.getOne(), array);
                    break;
                case BOOLEAN_AS_BYTE:
                    res.dataByColumnName.put(c.getOne(), new boolean[1]);
                    res.isNullByColumn.put(c.getOne(), array);
                    break;
                case DOUBLE:
                    res.dataByColumnName.put(c.getOne(), new double[1]);
                    res.isNullByColumn.put(c.getOne(), array);
                    break;
                case STRING:
                    res.dataByColumnName.put(c.getOne(), new String[1]);
                    break;
                case CUSTOM:
                    res.dataByColumnName.put(c.getOne(), new Variant[1]);
                    break;
                case DATETIME_AS_LONG:
                    res.dataByColumnName.put(c.getOne(), new PureDate[1]);
                    break;
                default:
                    throw new RuntimeException("ERROR " + columnType.get(c.getTwo()) + " not supported yet!");
            }
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
        MutableMap<String, DataType> columnTypes = Maps.mutable.empty();
        columnTypes.putAll(this.columnType);
        columnTypes.putAll(otherTDS.columnType);
        MutableList<String> columnOrdered = Lists.mutable.empty();
        columnOrdered.addAll(this.columnsOrdered);
        columnOrdered.addAll(otherTDS.columnsOrdered);
        columnOrdered = columnOrdered.distinct();
        TestTDS res = newTDS(columnOrdered, columnTypes, (int) (rowCount * otherTDS.rowCount));

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
        Object dataAsObject = dataByColumnName.get(columnName);
        boolean[] nullAsObject = isNullByColumn.get(columnName);
        boolean[] nullAsObjectSrc = srcTDS.isNullByColumn.get(columnName);
        switch (columnType.get(columnName))
        {
            case LONG:
            {
                ((long[]) dataAsObject)[row] = ((long[]) srcTDS.dataByColumnName.get(columnName))[srcRow];
                nullAsObject[row] = nullAsObjectSrc[srcRow];
                break;
            }
            case BOOLEAN_AS_BYTE:
            {
                ((boolean[]) dataAsObject)[row] = ((boolean[]) srcTDS.dataByColumnName.get(columnName))[srcRow];
                nullAsObject[row] = nullAsObjectSrc[srcRow];
                break;
            }
            case DOUBLE:
            {
                ((double[]) dataAsObject)[row] = ((double[]) srcTDS.dataByColumnName.get(columnName))[srcRow];
                nullAsObject[row] = nullAsObjectSrc[srcRow];
                break;
            }
            case STRING:
            case DATETIME_AS_LONG:
            case CUSTOM:
            {
                ((Object[]) dataAsObject)[row] = ((Object[]) srcTDS.dataByColumnName.get(columnName))[srcRow];
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
            boolean[] copyIsNull = null;
            switch (columnType.get(columnName))
            {
                case LONG:
                {
                    copy = Arrays.copyOf((long[]) dataAsObject, (int) rowCount);
                    copyIsNull = Arrays.copyOf(this.isNullByColumn.get(columnName), (int) rowCount);
                    break;
                }
                case BOOLEAN_AS_BYTE:
                {
                    copy = Arrays.copyOf((boolean[]) dataAsObject, (int) rowCount);
                    copyIsNull = Arrays.copyOf(isNullByColumn.get(columnName), (int) rowCount);
                    break;
                }
                case DOUBLE:
                {
                    copy = Arrays.copyOf((double[]) dataAsObject, (int) rowCount);
                    copyIsNull = Arrays.copyOf(isNullByColumn.get(columnName), (int) rowCount);
                    break;
                }
                case STRING:
                case DATETIME_AS_LONG:
                case CUSTOM:
                {
                    copy = Arrays.copyOf((Object[]) dataAsObject, (int) rowCount);
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
                case LONG:
                {
                    long[] src = (long[]) dataAsObject;
                    long[] target = new long[(int) copy.rowCount - size];
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
                case BOOLEAN_AS_BYTE:
                {
                    boolean[] src = (boolean[]) dataAsObject;
                    boolean[] target = new boolean[(int) copy.rowCount - size];
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
                case STRING:
                case DATETIME_AS_LONG:
                case CUSTOM:
                {
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
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + copy.columnType.get(columnName) + " not supported yet!");
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
                case LONG:
                {
                    long[] _copy = Arrays.copyOf((long[]) dataAsObject1, (int) result.rowCount);
                    System.arraycopy((long[]) dataAsObject2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    newIsNull = concatenate((boolean[]) isNullByColumn.get(columnName), (boolean[]) tds2.isNullByColumn.get(columnName));
                    break;
                }
                case BOOLEAN_AS_BYTE:
                {
                    boolean[] _copy = Arrays.copyOf((boolean[]) dataAsObject1, (int) result.rowCount);
                    System.arraycopy((boolean[]) dataAsObject2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    newIsNull = concatenate((boolean[]) isNullByColumn.get(columnName), (boolean[]) tds2.isNullByColumn.get(columnName));
                    break;
                }
                case DOUBLE:
                {
                    double[] _copy = Arrays.copyOf((double[]) dataAsObject1, (int) result.rowCount);
                    System.arraycopy((double[]) dataAsObject2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
                    newIsNull = concatenate((boolean[]) isNullByColumn.get(columnName), (boolean[]) tds2.isNullByColumn.get(columnName));
                    break;
                }
                case STRING:
                case DATETIME_AS_LONG:
                case CUSTOM:
                {
                    Object[] _copy = Arrays.copyOf((Object[]) dataAsObject1, (int) result.rowCount);
                    System.arraycopy((Object[]) dataAsObject2, 0, _copy, (int) rowCount, (int) tds2.rowCount);
                    copy = _copy;
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
        return Tuples.pair(this.copy(), Lists.mutable.with(Tuples.pair(0, (int) this.getRowCount())));
    }

    public TestTDS addColumn(String name, DataType dataType, Object res)
    {
        boolean[] array = new boolean[Array.getLength(res)];
        Arrays.fill(array, Boolean.FALSE);
        return addColumn(name, dataType, res, array);
    }

    public TestTDS addColumn(String name, DataType dataType, Object res, boolean[] nulls)
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
            case LONG:
            case BOOLEAN_AS_BYTE:
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
        DataType type = copy.columnType.get(oldName);
        Object data = copy.dataByColumnName.get(oldName);
        boolean[] oldIsNull = copy.isNullByColumn.get(oldName);
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
                case LONG:
                {
                    copy.dataByColumnName.put(columnName, Arrays.copyOfRange((long[]) dataAsObject, from, to));
                    copy.isNullByColumn.put(columnName, Arrays.copyOfRange(isNull, from, to));
                    break;
                }
                case BOOLEAN_AS_BYTE:
                {
                    copy.dataByColumnName.put(columnName, Arrays.copyOfRange((boolean[]) dataAsObject, from, to));
                    copy.isNullByColumn.put(columnName, Arrays.copyOfRange(isNull, from, to));
                    break;
                }
                case DOUBLE:
                {
                    copy.dataByColumnName.put(columnName, Arrays.copyOfRange((double[]) dataAsObject, from, to));
                    copy.isNullByColumn.put(columnName, Arrays.copyOfRange(isNull, from, to));
                    break;
                }
                case STRING:
                case DATETIME_AS_LONG:
                case CUSTOM:
                {
                    copy.dataByColumnName.put(columnName, Arrays.copyOfRange((Object[]) dataAsObject, from, to));
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
            switch (copy.columnType.get(columnName))
            {
                case LONG:
                {
                    long[] src = (long[]) dataAsObject;
                    long val = src[start];
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
                case BOOLEAN_AS_BYTE:
                {
                    boolean[] src = (boolean[]) dataAsObject;
                    boolean val = src[start];
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
                case STRING:
                case DATETIME_AS_LONG:
                case CUSTOM:
                {
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
            case LONG:
            {
                long[] src = (long[]) dataAsObject;
                MutableList<Pair<Integer, Long>> list = Lists.mutable.empty();
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
            case BOOLEAN_AS_BYTE:
            {
                boolean[] src = (boolean[]) dataAsObject;
                MutableList<Pair<Integer, Boolean>> list = Lists.mutable.empty();
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
            case STRING:
            case DATETIME_AS_LONG:
            case CUSTOM:
            {
                Comparable<Object>[] src = (Comparable<Object>[]) dataAsObject;
                MutableList<Pair<Integer, Comparable<Object>>> list = Lists.mutable.empty();
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
                throw new RuntimeException("ERROR " + columnType.get(columnName) + " not supported yet!");
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
                case LONG:
                {
                    long[] src = (long[]) dataAsObject;
                    boolean[] isNullResult = new boolean[(int) copy.rowCount];
                    long[] result = new long[(int) copy.rowCount];
                    for (int i = 0; i < indices.size(); i++)
                    {
                        result[i] = src[indices.get(i)];
                        isNullResult[i] = isNull[indices.get(i)];
                    }
                    System.arraycopy(result, 0, src, start, end - start);
                    System.arraycopy(isNullResult, 0, isNull, start, end - start);
                    break;
                }
                case BOOLEAN_AS_BYTE:
                {
                    boolean[] src = (boolean[]) dataAsObject;
                    boolean[] isNullResult = new boolean[(int) copy.rowCount];
                    boolean[] result = new boolean[(int) copy.rowCount];
                    for (int i = 0; i < indices.size(); i++)
                    {
                        result[i] = src[indices.get(i)];
                        isNullResult[i] = isNull[indices.get(i)];
                    }
                    System.arraycopy(result, 0, src, start, end - start);
                    System.arraycopy(isNullResult, 0, isNull, start, end - start);
                    break;
                }
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
                case STRING:
                case DATETIME_AS_LONG:
                case CUSTOM:
                {
                    Object[] src = (Object[]) dataAsObject;
                    Object[] result = (Object[]) Array.newInstance(src.getClass().getComponentType(), (int) copy.rowCount);
                    for (int i = 0; i < indices.size(); i++)
                    {
                        result[i] = src[indices.get(i)];
                    }
                    System.arraycopy(result, 0, src, start, end - start);
                    break;
                }
                default:
                    throw new RuntimeException("ERROR " + copy.columnType.get(columnName) + " not supported yet!");
            }
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
            if (resS.rowCount == 0 || !leftS.fullMatch(cols, resS, rowLeftCurs, rowResCurs))
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
                case LONG:
                {
                    valid = valid && ((long[]) firstDataAsObject)[rowFirst] == ((long[]) secondDataAsObject)[rowSecond];
                    break;
                }
                case BOOLEAN_AS_BYTE:
                {
                    valid = valid && ((boolean[]) firstDataAsObject)[rowFirst] == ((boolean[]) secondDataAsObject)[rowSecond];
                    break;
                }
                case DOUBLE:
                {
                    valid = valid && ((double[]) firstDataAsObject)[rowFirst] == ((double[]) secondDataAsObject)[rowSecond];
                    break;
                }
                case STRING:
                case DATETIME_AS_LONG:
                case CUSTOM:
                {
                    valid = valid && Objects.equals(((Object[]) firstDataAsObject)[rowFirst], (((Object[]) secondDataAsObject)[rowSecond]));
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
        private final DataType columnType;
        private final String columnName;

        public PivotColumnInfo(MutableList<Pair<String, String>> columnValues, String aggColumnName, DataType columnType)
        {
            this.columnValues = columnValues;
            this.aggColumnName = aggColumnName;
            this.columnType = columnType;
            // TODO: we might need to rethink this column naming strategy, it could break in some edge cases
            this.columnName = "'" + ListIterate.collect(columnValues, Pair::getTwo).with(aggColumnName).select(Objects::nonNull).makeString("__|__") + "'";
        }

        public String getColumnName()
        {
            return this.columnName;
        }

        public String getAggColumnName()
        {
            return this.aggColumnName;
        }

        public DataType getColumnType()
        {
            return this.columnType;
        }

        public boolean match(TestTDS tds, int row)
        {
            return columnValues.allSatisfy(col -> Objects.toString(tds.getValue(col.getOne(), row)).equals(col.getTwo()));
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
                        Tuples.pair(c, Objects.toString(sortedByPivotColumns.getOne().getValue(c, r.getOne())))).toList(), aggColumnName, this.columnType.get(aggColumnName)));
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
            switch (newColInfo.getColumnType())
            {
                case LONG:
                {
                    long[] values = new long[size];
                    for (int i = 0; i < size; i++)
                    {
                        for (int j = sortedByNonTransposeColumnsTwo.get(i).getOne(); j < sortedByNonTransposeColumnsTwo.get(i).getTwo(); j++)
                        {
                            if (newColInfo.match(sortedByNonTransposeColumnsOne, j))
                            {
                                values[i] = ((long[]) sortedByNonTransposeColumnsOne.dataByColumnName.get(newColInfo.getAggColumnName()))[j];
                                isNull[i] = sortedByNonTransposeColumnsOne.isNullByColumn.get(newColInfo.getAggColumnName())[j];
                            }
                        }
                    }
                    dataAsObject = values;
                    break;
                }
                case STRING:
                {
                    String[] values = new String[size];
                    for (int i = 0; i < size; i++)
                    {
                        for (int j = sortedByNonTransposeColumnsTwo.get(i).getOne(); j < sortedByNonTransposeColumnsTwo.get(i).getTwo(); j++)
                        {
                            if (newColInfo.match(sortedByNonTransposeColumnsOne, j))
                            {
                                values[i] = ((String[]) sortedByNonTransposeColumnsOne.dataByColumnName.get(newColInfo.getAggColumnName()))[j];
                            }
                        }
                    }
                    dataAsObject = values;
                    break;
                }
                case BOOLEAN_AS_BYTE:
                {
                    boolean[] values = new boolean[size];
                    for (int i = 0; i < size; i++)
                    {
                        for (int j = sortedByNonTransposeColumnsTwo.get(i).getOne(); j < sortedByNonTransposeColumnsTwo.get(i).getTwo(); j++)
                        {
                            if (newColInfo.match(sortedByNonTransposeColumnsOne, j))
                            {
                                values[i] = ((boolean[]) sortedByNonTransposeColumnsOne.dataByColumnName.get(newColInfo.getAggColumnName()))[j];
                                isNull[i] = sortedByNonTransposeColumnsOne.isNullByColumn.get(newColInfo.getAggColumnName())[j];
                            }
                        }
                    }
                    dataAsObject = values;
                    break;
                }
                case DOUBLE:
                {
                    double[] values = new double[size];
                    for (int i = 0; i < size; i++)
                    {
                        for (int j = sortedByNonTransposeColumnsTwo.get(i).getOne(); j < sortedByNonTransposeColumnsTwo.get(i).getTwo(); j++)
                        {
                            if (newColInfo.match(sortedByNonTransposeColumnsOne, j))
                            {
                                values[i] = ((double[]) sortedByNonTransposeColumnsOne.dataByColumnName.get(newColInfo.getAggColumnName()))[j];
                                isNull[i] = sortedByNonTransposeColumnsOne.isNullByColumn.get(newColInfo.getAggColumnName())[j];
                            }
                        }
                    }
                    dataAsObject = values;
                    break;
                }
                case DATETIME_AS_LONG:
                {
                    PureDate[] values = new PureDate[size];
                    for (int i = 0; i < size; i++)
                    {
                        for (int j = sortedByNonTransposeColumnsTwo.get(i).getOne(); j < sortedByNonTransposeColumnsTwo.get(i).getTwo(); j++)
                        {
                            if (newColInfo.match(sortedByNonTransposeColumnsOne, j))
                            {
                                values[i] = ((PureDate[]) sortedByNonTransposeColumnsOne.dataByColumnName.get(newColInfo.getAggColumnName()))[j];
                            }
                        }
                    }
                    dataAsObject = values;
                    break;
                }
                default:
                {
                    throw new RuntimeException("ERROR " + newColInfo.getColumnType() + " not supported yet!");
                }
            }
            tds.dataByColumnName.put(name, dataAsObject);
            if (!newColInfo.getColumnType().equals(DataType.STRING) && !newColInfo.getColumnType().equals(DataType.DATETIME_AS_LONG))
            {
                tds.isNullByColumn.put(name, isNull);
            }
            tds.columnsOrdered.add(name);
            tds.columnType.put(name, newColInfo.getColumnType());
            return tds;
        });

        return result;
    }

    public MutableList<Pair<String, DataType>> getColumnWithTypes()
    {
        return this.columnsOrdered.collect(col -> Tuples.pair(col, this.columnType.get(col)));
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
