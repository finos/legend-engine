// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.data;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandlerRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class LazyParsedFlatData implements ParsedFlatData
{
    private enum FieldState
    {
        INVALID,
        VERIFIED,
        MISSING,
        PARSED_STRING,
        PARSED_BOOLEAN,
        PARSED_LONG,
        PARSED_DOUBLE,
        PARSED_BIG_DECIMAL,
        PARSED_LOCAL_DATE,
        PARSED_INSTANT
    }

    final RawFlatData rawFlatData;
    final List<FieldHandler> fieldHandlers;
    final String definingPath;

    final List<IDefect> defects;
    final FieldState[] states;
    boolean[] booleanValues;
    long[] longValues;
    double[] doubleValues;
    Object[] objectValues;

    public LazyParsedFlatData(RawFlatData rawFlatData, List<IDefect> priorDefects, List<FieldHandler> fieldHandlers, String definingPath)
    {
        this.rawFlatData = rawFlatData;
        this.fieldHandlers = fieldHandlers;
        this.defects = Lists.mutable.ofAll(priorDefects);
        this.states = new FieldState[fieldHandlers.size()];
        this.definingPath = definingPath;
    }

    public List<IDefect> getDefects()
    {
        return defects;
    }

    public void addInvalidInputDefect(FieldHandler handler, String errorMessage)
    {
        String suffix = "'" + handler.getField().getLabel() + "' with value: " + handler.rawValue(rawFlatData) + ", error: ParseException " + errorMessage;
        IDefect defect = handler.getField().isOptional()
                ? BasicDefect.newInvalidInputErrorDefect("Failed to read " + suffix, definingPath)
                : BasicDefect.newInvalidInputCriticalDefect("Failed to read mandatory " + suffix, definingPath);
        defects.add(defect);
        states[handler.getFieldIndex()] = FieldState.INVALID;
    }

    public void addMissingValueDefect(FieldHandler handler)
    {
        IDefect defect = BasicDefect.newInvalidInputCriticalDefect("Failed to read '" + handler.getField().getLabel() + "' not present in the source", definingPath);
        defects.add(defect);
        states[handler.getFieldIndex()] = FieldState.INVALID;
    }

    public void setVerified(FieldHandler handler)
    {
        states[handler.getFieldIndex()] = FieldState.VERIFIED;
    }

    public boolean isVerified(FieldHandler handler)
    {
        return states[handler.getFieldIndex()] != FieldState.INVALID && states[handler.getFieldIndex()] != FieldState.MISSING;
    }

    public void setMissing(FieldHandler handler)
    {
        states[handler.getFieldIndex()] = FieldState.MISSING;
    }

    @Override
    public boolean hasStringValue(FlatDataRecordField field)
    {
        FieldHandler handler = fieldHandler(field);
        if (isVerified(handler) && states[handler.getFieldIndex()] != FieldState.PARSED_STRING)
        {
            try
            {
                String s = handler.getString(rawFlatData);
                objectValues = objectValues == null ? new Object[states.length] : objectValues;
                objectValues[handler.getFieldIndex()] = s;
                states[handler.getFieldIndex()] = FieldState.PARSED_STRING;
            }
            catch (ParseException e)
            {
                addInvalidInputDefect(handler, e.getMessage());
            }
        }

        return states[handler.getFieldIndex()] == FieldState.PARSED_STRING;
    }

    @Override
    public boolean hasBooleanValue(FlatDataRecordField field)
    {
        FieldHandler handler = fieldHandler(field);
        if (isVerified(handler) && states[handler.getFieldIndex()] != FieldState.PARSED_BOOLEAN)
        {
            try
            {
                boolean b = handler.getBoolean(rawFlatData);
                booleanValues = booleanValues == null ? new boolean[states.length] : booleanValues;
                booleanValues[handler.getFieldIndex()] = b;
                states[handler.getFieldIndex()] = FieldState.PARSED_BOOLEAN;
            }
            catch (ParseException e)
            {
                addInvalidInputDefect(handler, e.getMessage());
            }
        }

        return states[handler.getFieldIndex()] == FieldState.PARSED_BOOLEAN;
    }

    @Override
    public boolean hasLongValue(FlatDataRecordField field)
    {
        FieldHandler handler = fieldHandler(field);
        if (isVerified(handler) && states[handler.getFieldIndex()] != FieldState.PARSED_LONG)
        {
            try
            {
                long l = handler.getLong(rawFlatData);
                longValues = longValues == null ? new long[states.length] : longValues;
                longValues[handler.getFieldIndex()] = l;
                states[handler.getFieldIndex()] = FieldState.PARSED_LONG;
            }
            catch (ParseException e)
            {
                addInvalidInputDefect(handler, e.getMessage());
            }
        }

        return states[handler.getFieldIndex()] == FieldState.PARSED_LONG;
    }

    @Override
    public boolean hasDoubleValue(FlatDataRecordField field)
    {
        FieldHandler handler = fieldHandler(field);
        if (isVerified(handler) && states[handler.getFieldIndex()] != FieldState.PARSED_DOUBLE)
        {
            try
            {
                double d = handler.getDouble(rawFlatData);
                doubleValues = doubleValues == null ? new double[states.length] : doubleValues;
                doubleValues[handler.getFieldIndex()] = d;
                states[handler.getFieldIndex()] = FieldState.PARSED_DOUBLE;
            }
            catch (ParseException e)
            {
                addInvalidInputDefect(handler, e.getMessage());
            }
        }

        return states[handler.getFieldIndex()] == FieldState.PARSED_DOUBLE;
    }

    @Override
    public boolean hasBigDecimalValue(FlatDataRecordField field)
    {
        FieldHandler handler = fieldHandler(field);
        if (isVerified(handler) && states[handler.getFieldIndex()] != FieldState.PARSED_BIG_DECIMAL)
        {
            try
            {
                BigDecimal bd = handler.getBigDecimal(rawFlatData);
                objectValues = objectValues == null ? new Object[states.length] : objectValues;
                objectValues[handler.getFieldIndex()] = bd;
                states[handler.getFieldIndex()] = FieldState.PARSED_BIG_DECIMAL;
            }
            catch (ParseException e)
            {
                addInvalidInputDefect(handler, e.getMessage());
            }
        }

        return states[handler.getFieldIndex()] == FieldState.PARSED_BIG_DECIMAL;
    }

    @Override
    public boolean hasLocalDateValue(FlatDataRecordField field)
    {
        FieldHandler handler = fieldHandler(field);
        if (isVerified(handler) && states[handler.getFieldIndex()] != FieldState.PARSED_LOCAL_DATE)
        {
            try
            {
                LocalDate ld = handler.getLocalDate(rawFlatData);
                objectValues = objectValues == null ? new Object[states.length] : objectValues;
                objectValues[handler.getFieldIndex()] = ld;
                states[handler.getFieldIndex()] = FieldState.PARSED_LOCAL_DATE;
            }
            catch (ParseException e)
            {
                addInvalidInputDefect(handler, e.getMessage());
            }
        }

        return states[handler.getFieldIndex()] == FieldState.PARSED_LOCAL_DATE;
    }

    @Override
    public boolean hasInstantValue(FlatDataRecordField field)
    {
        FieldHandler handler = fieldHandler(field);
        if (isVerified(handler) && states[handler.getFieldIndex()] != FieldState.PARSED_INSTANT)
        {
            try
            {
                Instant i = handler.getInstant(rawFlatData);
                objectValues = objectValues == null ? new Object[states.length] : objectValues;
                objectValues[handler.getFieldIndex()] = i;
                states[handler.getFieldIndex()] = FieldState.PARSED_INSTANT;
            }
            catch (ParseException e)
            {
                addInvalidInputDefect(handler, e.getMessage());
            }
        }

        return states[handler.getFieldIndex()] == FieldState.PARSED_INSTANT;
    }

    @Override
    public String getString(FlatDataRecordField field)
    {
        if (!hasStringValue(field))
        {
            throw new IllegalStateException("No such value");
        }
        return (String) objectValues[fieldHandler(field).getFieldIndex()];
    }

    @Override
    public boolean getBoolean(FlatDataRecordField field)
    {
        if (!hasBooleanValue(field))
        {
            throw new IllegalStateException("No such value");
        }
        return booleanValues[fieldHandler(field).getFieldIndex()];
    }

    @Override
    public long getLong(FlatDataRecordField field)
    {
        if (!hasLongValue(field))
        {
            throw new IllegalStateException("No such value");
        }
        return longValues[fieldHandler(field).getFieldIndex()];
    }

    @Override
    public double getDouble(FlatDataRecordField field)
    {
        if (!hasDoubleValue(field))
        {
            throw new IllegalStateException("No such value");
        }
        return doubleValues[fieldHandler(field).getFieldIndex()];
    }

    @Override
    public BigDecimal getBigDecimal(FlatDataRecordField field)
    {
        if (!hasBigDecimalValue(field))
        {
            throw new IllegalStateException("No such value");
        }
        return (BigDecimal) objectValues[fieldHandler(field).getFieldIndex()];
    }

    @Override
    public LocalDate getLocalDate(FlatDataRecordField field)
    {
        if (!hasLocalDateValue(field))
        {
            throw new IllegalStateException("No such value");
        }
        return (LocalDate) objectValues[fieldHandler(field).getFieldIndex()];
    }

    @Override
    public Instant getInstant(FlatDataRecordField field)
    {
        if (!hasInstantValue(field))
        {
            throw new IllegalStateException("No such value");
        }
        return (Instant) objectValues[fieldHandler(field).getFieldIndex()];
    }

    private FieldHandler fieldHandler(FlatDataRecordField field)
    {
        return (field instanceof FieldHandlerRecordField)
                ? ((FieldHandlerRecordField) field).getFieldHandler()
                : fieldHandlers.stream().filter(f -> f.getField().getLabel().equals(field.getLabel())).findFirst().orElseThrow(() -> new IllegalArgumentException("No field handler for " + field.getLabel()));
    }
}
