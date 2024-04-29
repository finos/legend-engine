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

package org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TDSColumn
{
    public String name;
    public String type;
    public String doc;
    public String relationalType;
    public Map<String, List<String>> enumMapping = Collections.emptyMap();

    public TDSColumn()
    {

    }

    public TDSColumn(String name, String type)
    {
        this.name = name;
        this.type = type;
    }

    public TDSColumn copyWithoutEnumMapping()
    {
        TDSColumn c = new TDSColumn();
        c.name = name;
        c.type = type;
        c.doc = doc;
        c.relationalType = relationalType;
        c.enumMapping = null;
        return c;
    }
}

