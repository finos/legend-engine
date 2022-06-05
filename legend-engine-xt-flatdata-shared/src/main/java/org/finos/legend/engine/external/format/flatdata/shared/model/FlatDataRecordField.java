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

package org.finos.legend.engine.external.format.flatdata.shared.model;

public class FlatDataRecordField
{
    private String label;
    private FlatDataDataType type;
    private String address;

    public FlatDataRecordField(String label, FlatDataDataType type)
    {
        this(label, type, null);
    }

    public FlatDataRecordField(String label, FlatDataDataType type, String address)
    {
        this.label = label;
        this.type = type;
        this.address = address;
    }

    public boolean isOptional()
    {
        return type.isOptional();
    }

    public String getLabel()
    {
        return label;
    }

    public FlatDataDataType getType()
    {
        return type;
    }

    public String getAddress()
    {
        return address;
    }
}

