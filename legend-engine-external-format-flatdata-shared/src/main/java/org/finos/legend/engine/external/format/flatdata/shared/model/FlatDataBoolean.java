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

package org.finos.legend.engine.external.format.flatdata.shared.model;

public class FlatDataBoolean extends FlatDataDataType
{
    private String trueString;
    private String falseString;

    public FlatDataBoolean(boolean optional)
    {
        super(optional);
    }

    public FlatDataBoolean withTrueString(String s)
    {
        this.trueString = s;
        return this;
    }

    public FlatDataBoolean withFalseString(String s)
    {
        this.falseString = s;
        return this;
    }

    public String getTrueString()
    {
        return trueString;
    }

    public String getFalseString()
    {
        return falseString;
    }
}
