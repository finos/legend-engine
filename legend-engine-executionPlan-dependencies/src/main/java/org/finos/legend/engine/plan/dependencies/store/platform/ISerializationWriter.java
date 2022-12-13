// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.dependencies.store.platform;

import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

public interface ISerializationWriter
{
    void startObject(String typePath);

    void startObject(String typePath, String objectRef);

    void writeBooleanProperty(String name, boolean value);

    void writeBooleanProperty(String name, Boolean value);

    void writeBooleanProperty(String name, List<Boolean> values);

    void writeIntegerProperty(String name, long value);

    void writeIntegerProperty(String name, Long value);

    void writeIntegerProperty(String name, List<Long> values);

    void writeFloatProperty(String name, double value);

    void writeFloatProperty(String name, Double value);

    void writeFloatProperty(String name, List<Double> values);

    void writeDecimalProperty(String name, BigDecimal value);

    void writeDecimalProperty(String name, List<BigDecimal> values);

    void writeNumberProperty(String name, Number value);

    void writeNumberProperty(String name, List<Number> values);

    void writeStringProperty(String name, String value);

    void writeStringProperty(String name, List<String> values);

    void writeStrictDateProperty(String name, PureDate value);

    void writeStrictDateProperty(String name, List<PureDate> values);

    void writeDateTimeProperty(String name, PureDate value);

    void writeDateTimeProperty(String name, List<PureDate> values);

    void writeDateProperty(String name, PureDate value);

    void writeDateProperty(String name, List<PureDate> values);

    void writeEnumProperty(String name, String path, String value);

    void writeEnumProperty(String name, String path, List<String> values);

    void writeUnitProperty(String name, String path, Number value);

    void writeUnitProperty(String name, String path, List<Number> values);

    <T> void writeComplexProperty(String name, T value, Consumer<T> writeOne);

    <T> void writeComplexProperty(String name, List<T> values, Consumer<T> writeOne);

    void endObject();
}
