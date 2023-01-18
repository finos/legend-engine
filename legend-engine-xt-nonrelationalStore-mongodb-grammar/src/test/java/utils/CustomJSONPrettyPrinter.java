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

package utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;

public class CustomJSONPrettyPrinter extends DefaultPrettyPrinter
{
    public CustomJSONPrettyPrinter()
    {
    }

    public CustomJSONPrettyPrinter(CustomJSONPrettyPrinter base)
    {
        super(base);
        _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
    }

    @Override
    public CustomJSONPrettyPrinter createInstance()
    {
        return new CustomJSONPrettyPrinter(this);
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException
    {
        jg.writeRaw(':');
        jg.writeRaw(' ');
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException
    {
        if (!_arrayIndenter.isInline())
        {
            --_nesting;
        }
        if (nrOfValues > 0)
        {
            _arrayIndenter.writeIndentation(g, _nesting);
        }
        g.writeRaw(']');
    }

}
