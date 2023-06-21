// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.protocol.test;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureSDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLC;
import org.junit.Assert;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

public class TestPureContextPointer
{

    private PureModelContextPointer context(SDLC sdlc)
    {
        PureModelContextPointer pointer = new PureModelContextPointer();
        Protocol protocol = new Protocol();
        protocol.name = "V1";
        protocol.version = "1.0.0";
        pointer.serializer = protocol;
        pointer.sdlcInfo = sdlc;

        return pointer;
    }

    private PackageableElementPointer element(String path, PackageableElementType type)
    {
        PackageableElementPointer p = new PackageableElementPointer();
        p.path = path;
        p.type = type;

        return p;
    }

    private AlloySDLC alloy(String project, String group, String artifact, String baseVersion, String version, List<PackageableElementPointer> elements)
    {
        AlloySDLC alloy = new AlloySDLC();
        alloy.baseVersion = baseVersion;
        alloy.version = version;
        alloy.project = project;
        alloy.groupId = group;
        alloy.artifactId = artifact;
        alloy.packageableElementPointers = elements;

        return alloy;
    }

    private PureSDLC pure(String baseVersion, String version, List<PackageableElementPointer> elements)
    {
        PureSDLC pure = new PureSDLC();
        pure.baseVersion = baseVersion;
        pure.version = version;
        pure.packageableElementPointers = elements;

        return pure;
    }

    private void assertPMCPEquals(PureModelContextPointer expected, PureModelContextPointer result)
    {
        //required as set is really used and converted to list which leads to flaky equality
        Comparator<? super PackageableElementPointer> comparator = Comparator.comparing(p -> p.type + ":" + p.type);
        expected.sdlcInfo.packageableElementPointers.sort(comparator);
        result.sdlcInfo.packageableElementPointers.sort(comparator);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testCombineAlloy()
    {
        PackageableElementPointer p1 = element("P1", PackageableElementType.CLASS);
        SDLC alloy = alloy("P1", "G1", "A1", "v1", "V1", FastList.newListWith(p1));

        PackageableElementPointer p2 = element("P2", PackageableElementType.PACKAGE);
        SDLC alloy2 = alloy("P1", "G1", "A1", "v1", "V1", FastList.newListWith(p2));

        PureModelContextPointer left = context(alloy);
        PureModelContextPointer right = context(alloy2);

        PureModelContextPointer result = left.combine(right);

        SDLC expectedSDLC = alloy("P1", "G1", "A1", "v1", "V1", FastList.newListWith(p1, p2));
        PureModelContextPointer expected = context(expectedSDLC);

        assertPMCPEquals(expected, result);
    }

    @Test
    public void testCombinePure()
    {
        PackageableElementPointer p1 = element("P1", PackageableElementType.CLASS);
        SDLC pure = pure("v1", "V1", FastList.newListWith(p1));

        PackageableElementPointer p2 = element("P2", PackageableElementType.PACKAGE);
        SDLC pure2 = pure("v1", "V1", FastList.newListWith(p2));

        PureModelContextPointer left = context(pure);
        PureModelContextPointer right = context(pure2);

        PureModelContextPointer result = left.combine(right);

        SDLC expectedSDLC = pure("v1", "V1", FastList.newListWith(p1, p2));
        PureModelContextPointer expected = context(expectedSDLC);

        assertPMCPEquals(expected, result);
    }

    @Test
    public void testCombineIncompatible()
    {
        PackageableElementPointer p1 = element("P1", PackageableElementType.CLASS);
        SDLC pure = pure("v1", "V1", FastList.newListWith(p1));

        PackageableElementPointer p2 = element("P2", PackageableElementType.PACKAGE);
        SDLC pure2 = pure("v2", "V1", FastList.newListWith(p2));

        PureModelContextPointer left = context(pure);
        PureModelContextPointer right = context(pure2);

        Assert.assertEquals("Can't merge two context as they come from two different environment", Assert.assertThrows(RuntimeException.class, () -> left.combine(right)).getMessage());
    }

    @Test
    public void testCombineNull()
    {
        PackageableElementPointer p1 = element("P1", PackageableElementType.CLASS);
        SDLC pure = pure("v1", "V1", FastList.newListWith(p1));
        PureModelContextPointer left = context(pure);

        PureModelContextPointer result = left.combine(null);

        Assert.assertEquals(left, result);
    }
}