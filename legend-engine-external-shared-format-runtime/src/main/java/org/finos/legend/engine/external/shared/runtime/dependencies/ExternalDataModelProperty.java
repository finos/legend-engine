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

package org.finos.legend.engine.external.shared.runtime.dependencies;

public class ExternalDataModelProperty<T>
{
    public enum Type {String, Boolean, Integer, Float, Decimal, StrictDate, DateTime;}

    private final Type type;
    private final ExternalDataAdder<T> adder;

    ExternalDataModelProperty(Type type, ExternalDataAdder<T> adder) {
        this.type = type;
        this.adder = adder;
    }

    public Type getType()
    {
        return type;
    }

    public String getName()
    {
        return adder.getPropertyName();
    }

    public ExternalDataAdder<T> getAdder()
    {
        return adder;
    }
}
