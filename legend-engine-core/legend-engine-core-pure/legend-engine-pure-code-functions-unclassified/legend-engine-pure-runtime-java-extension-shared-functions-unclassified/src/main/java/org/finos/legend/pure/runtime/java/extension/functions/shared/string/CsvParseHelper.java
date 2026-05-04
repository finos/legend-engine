// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.functions.shared.string;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvParseHelper
{
    public static List<List<String>> parseCSV(String csv)
    {
        try (CSVParser parser = CSVParser.parse(csv, CSVFormat.RFC4180.withEscape('\\')))
        {
            List<List<String>> result = new ArrayList<>();
            for (CSVRecord record : parser)
            {
                List<String> row = new ArrayList<>();
                for (String field : record)
                {
                    row.add(field);
                }
                result.add(row);
            }
            return result;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to parse CSV: " + e.getMessage(), e);
        }
    }
}

