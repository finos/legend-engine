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

import org.eclipse.collections.impl.factory.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlatDataSection
{
    private String driverId;
    private String name;
    private List<FlatDataProperty> sectionProperties = Lists.mutable.empty();
    private FlatDataRecordType recordType;

    public FlatDataSection(String name, String driverId)
    {
        this.name = name;
        this.driverId = driverId;
    }

    public FlatDataSection withProperties(Iterable<FlatDataProperty> properties)
    {
        properties.forEach(this::withProperty);
        return this;
    }

    public FlatDataSection withProperty(String name, Object value)
    {
        return withProperty(new FlatDataProperty(name, value));
    }

    public FlatDataSection withProperty(FlatDataProperty property)
    {
        sectionProperties.add(property);
        return this;
    }

    public FlatDataSection setSectionProperties(ArrayList<FlatDataProperty> properties)
    {
        sectionProperties.clear();
        sectionProperties.addAll(properties);
        return this;
    }

    public FlatDataSection withRecordType(FlatDataRecordType recordType)
    {
        this.recordType = recordType;
        return this;
    }

    public String getDriverId()
    {
        return driverId;
    }

    public String getName()
    {
        return name;
    }

    public List<FlatDataProperty> getSectionProperties()
    {
        return Collections.unmodifiableList(sectionProperties);
    }

    public FlatDataRecordType getRecordType()
    {
        return recordType;
    }
}
