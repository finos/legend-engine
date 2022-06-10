//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.flatdata.shared.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FlatDataValidationResult
{
    public static FlatDataValidationResult valid()
    {
        return new FlatDataValidationResult(true, Collections.emptyList());
    }

    public static FlatDataValidationResult invalid(FlatDataDefect... defects)
    {
        return new FlatDataValidationResult(false, Arrays.asList(defects));
    }

    public static FlatDataValidationResult invalid(List<FlatDataDefect> defects)
    {
        return new FlatDataValidationResult(false, defects);
    }

    private final boolean valid;
    private final List<FlatDataDefect> defects;

    public FlatDataValidationResult(boolean valid, List<FlatDataDefect> defects)
    {
        if (!valid && defects.isEmpty())
        {
            throw new IllegalArgumentException("Invalid result must have defects");
        }
        this.valid = valid;
        this.defects = Collections.unmodifiableList(defects);
    }

    public boolean isValid()
    {
        return valid;
    }

    public List<FlatDataDefect> getDefects()
    {
        return defects;
    }

    @Override
    public String toString()
    {
        return valid ? "Valid" : "Invalid: " + defects;
    }
}
