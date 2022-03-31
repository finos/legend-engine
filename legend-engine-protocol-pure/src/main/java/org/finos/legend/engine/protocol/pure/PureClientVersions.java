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

package org.finos.legend.engine.protocol.pure;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.utility.ArrayIterate;

public class PureClientVersions
{
    public static ImmutableList<String> versions = Lists.immutable.with("v1_0_0", "v1_1_0", "v1_2_0", "v1_3_0", "v1_4_0", "v1_5_0", "v1_6_0", "v1_7_0", "v1_8_0", "v1_9_0", "v1_10_0", "v1_11_0", "v1_12_0", "v1_13_0", "v1_14_0", "v1_15_0", "v1_16_0", "v1_17_0", "v1_18_0", "v1_19_0", "v1_20_0", "v1_21_0", "v1_22_0", "v1_23_0", "vX_X_X");
    public static ImmutableList<String> versionsSameCase = versions.collect(String::toLowerCase);

    static
    {
        assert !hasRepeatedVersions(versions) : "Repeated version id :" + versions.toBag().selectByOccurrences(i -> i > 1).toSet().makeString("[", ", ", "]");
    }

    public static String production = "v1_23_0";

    static boolean hasRepeatedVersions(ImmutableList<String> versions)
    {
        return versions.distinct().size() != versions.size();
    }

    static String getVersion(ImmutableList<String> versions, int versionNumber)
    {
        return versions.select(v -> !"vX_X_X".equals(v)).toSortedListBy(v -> Integer.parseInt(ArrayIterate.makeString(v.substring(1).split("_"), ""))).get(versions.select(v -> !"vX_X_X".equals(v)).size() - versionNumber - 1);
    }

    public static boolean versionAGreaterThanOrEqualsVersionB(String version1, String version2)
    {
        return versionsSameCase.indexOf(version1.toLowerCase()) >= versionsSameCase.indexOf(version2.toLowerCase());
    }

    public static ImmutableList<String> versionsSince(String version)
    {
        return versions.select(v -> versionAGreaterThanOrEqualsVersionB(v, version));
    }
}
