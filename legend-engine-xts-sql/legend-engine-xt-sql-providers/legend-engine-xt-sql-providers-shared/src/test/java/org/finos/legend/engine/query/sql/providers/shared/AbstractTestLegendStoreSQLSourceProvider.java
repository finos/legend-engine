// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers.shared;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceProvider;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.core.TableSourceArgument;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateLoader;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectResolvedContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.query.sql.providers.shared.SQLSourceProviderTestUtils.loadPureModelContextFromResource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractTestLegendStoreSQLSourceProvider
{
    @Test
    public void testNoProjectOrCoordindates()
    {
        TableSource tableSource = new TableSource("store", FastList.newListWith(
                new TableSourceArgument("store", null, "notfound"))
        );

        testError(tableSource, "coordinates or project/workspace must be supplied");
    }

    @Test
    public void testMissingWorkspace()
    {
        TableSource tableSource = new TableSource("store", FastList.newListWith(
                new TableSourceArgument("store", null, "notfound"),
                new TableSourceArgument("project", null, "proj1"))
        );

        testError(tableSource, "workspace/group workspace must be supplied if loading from project");
    }

    @Test
    public void testMixedCoordinatesWorkspace()
    {
        TableSource tableSource = new TableSource("store", FastList.newListWith(
                new TableSourceArgument("store", null, "notfound"),
                new TableSourceArgument("project", null, "proj1"),
                new TableSourceArgument("coordinates", null, "group:artifact:version"))
        );

        testError(tableSource, "cannot mix coordinates with project/workspace");
    }

    @Test
    public void testMissingStoreParams()
    {
        String connectionName = "simple::store::DB::H2Connection";

        PureModelContextData pmcd = loadPureModelContextFromResource("pmcd.pure", this.getClass());
        when(getProjectCoordinateLoader().resolve(any(), any())).thenReturn(new ProjectResolvedContext(pmcd, pmcd));

        TableSource table = new TableSource("relationalStore", FastList.newListWith(
                new TableSourceArgument("coordinates", null, "group:artifact:version"),
                new TableSourceArgument("connection", null, connectionName)));

        IllegalArgumentException exception = Assert.assertThrows("Should throw given no store found", IllegalArgumentException.class, () -> getProvider().resolve(FastList.newListWith(table), null, Identity.getAnonymousIdentity()));
        Assert.assertEquals("'store' parameter is required", exception.getMessage());
    }

    @Test
    public void testStoreNotFound()
    {
        when(getProjectCoordinateLoader().resolve(any(), any())).thenReturn(new ProjectResolvedContext(mock(PureModelContextData.class), mock(PureModelContextData.class)));
        String connectionName = "simple::store::DB::H2Connection";

        TableSource table = new TableSource("store", FastList.newListWith(
                new TableSourceArgument("store", null, "simple::store::DBForSQL"),
                new TableSourceArgument("connection", null, connectionName),
                new TableSourceArgument("coordinates", null, "group:artifact:version")));

        IllegalArgumentException exception = Assert.assertThrows("Should throw given no store found", IllegalArgumentException.class, () -> getProvider().resolve(FastList.newListWith(table), null, Identity.getAnonymousIdentity()));
        Assert.assertEquals("No element found for 'store'", exception.getMessage());
    }

    protected void testError(TableSource tableSource, String error)
    {
        IllegalArgumentException exception = Assert.assertThrows("Should throw error", IllegalArgumentException.class, () -> getProvider().resolve(FastList.newListWith(tableSource), null, Identity.getAnonymousIdentity()));
        Assert.assertEquals(error, exception.getMessage());
    }

    protected abstract SQLSourceProvider getProvider();

    protected abstract ProjectCoordinateLoader getProjectCoordinateLoader();

}
