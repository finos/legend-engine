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

package org.finos.legend.engine.ide.api.execution.function.manager;


enum OutputFormat
{
    RAW(ContentType.text),  // Raw output - no formatting whatsoever
    PRE(ContentType.text),  // Pre-formatted output - surrounded with <pre></pre> tags
    JSON(ContentType.json), // JSON output - formatted into JSON
    CSV(ContentType.csv, "attachment;filename=result.csv"), //CSV
    XLSX(ContentType.xlsx, "attachment;filename=result.xlsx"); //XLSX

    private final ContentType defaultContentType;
    private final String contentDisposition;

    OutputFormat(ContentType type)
    {
        this(type,null);
    }

    OutputFormat(ContentType defaultContentType, String contentDisposition)
    {
        this.defaultContentType = defaultContentType;
        this.contentDisposition = contentDisposition;
    }

    public ContentType getDefaultContentType()
    {
        return this.defaultContentType;
    }

    public String getDefaultContentDisposition()
    {
        return this.contentDisposition;
    }
}