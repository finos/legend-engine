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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

public class SQLResultColumn
{
    public String label;
    public String dataType;

    public SQLResultColumn()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public SQLResultColumn(SQLResultColumn that)
    {
        this.label = that.label;
        this.dataType = that.dataType;
    }

    public SQLResultColumn(String label, String dataType)
    {
        this.label = label;
        this.dataType = dataType;
    }

    @JsonIgnore
    public String getNonQuotedLabel()
    {
        return this.label.startsWith("\"") && this.label.endsWith("\"") ?
                this.label.substring(1, this.label.length() - 1) :
                this.label;
    }

    @JsonIgnore
    public String getQuotedLabelIfContainSpace()
    {
        String nonQuotedLabel = this.getNonQuotedLabel();
        return nonQuotedLabel.contains(" ") ? "\"" + nonQuotedLabel + "\"" : nonQuotedLabel;
    }

    @JsonIgnore
    public Pair<String, String> labelTypePair()
    {
        if (this.dataType == null)
        {
            return Tuples.pair(this.label, "String"); //TODO: This should not be null. Change after all relational types are available
        }
        String type = this.dataType.toUpperCase();
        if (type.startsWith("VARCHAR") || type.startsWith("CHAR"))
        {
            return Tuples.pair(this.label, "String");
        }
        else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE") || type.startsWith("DECIMAL") || type.startsWith("NUMERIC") || type.startsWith("REAL"))
        {
            return Tuples.pair(this.label, "Float");
        }
        else if (type.startsWith("INTEGER") || type.startsWith("BIGINT") || type.startsWith("SMALLINT") || type.startsWith("TINYINT"))
        {
            return Tuples.pair(this.label, "Integer");
        }
        else if (type.startsWith("BIT"))
        {
            return Tuples.pair(this.label, "Boolean");
        }
        else if (type.startsWith("TIMESTAMP"))
        {
            return Tuples.pair(this.label, "DateTime");
        }
        else if (type.startsWith("DATE"))
        {
            return Tuples.pair(this.label, "StrictDate");
        }
        return Tuples.pair(this.label, "String"); // Default is String. But shouldn't go here
    }
}
