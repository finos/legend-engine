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

package org.finos.legend.engine.external.shared.utils;

import org.finos.legend.engine.plan.dependencies.domain.dataQuality.EnforcementLevel;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.shared.core.url.UrlFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExternalFormatRuntime
{
    public static InputStream openUrl(String url)
    {
        try
        {
            return UrlFactory.create(url).openStream();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Stream<?> unwrapCheckedStream(Stream<IChecked<?>> checkedStream)
    {
        return checkedStream.map(ExternalFormatRuntime::unwrapCheckedValue);
    }

    public static Object unwrapCheckedValue(IChecked<?> checked)
    {
        if (checked.getDefects().stream().anyMatch(d -> d.getEnforcementLevel() != EnforcementLevel.Warn))
        {
            throw new IllegalStateException(checked.getDefects().stream().map(IDefect::getMessage).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        }
        else if (checked.getValue() == null)
        {
            throw new IllegalStateException("Unexpected error: no object and no explanatory defects");
        }
        return checked.getValue();
    }
}
