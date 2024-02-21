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

package org.finos.legend.engine.query.sql.providers;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.query.sql.providers.core.SQLSource;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceResolvedContext;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.core.TableSourceArgument;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateLoader;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateWrapper;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectResolvedContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.finos.legend.engine.query.sql.providers.shared.SQLSourceProviderTestUtils.assertLogicalEquality;
import static org.finos.legend.engine.query.sql.providers.shared.SQLSourceProviderTestUtils.loadPureModelContextFromResource;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestLegendServiceSQLSourceProvider
{
    @Mock
    private ProjectCoordinateLoader projectCoordinateLoader;

    private LegendServiceSQLSourceProvider provider;

    @Before
    public void setup()
    {
        provider = new LegendServiceSQLSourceProvider(projectCoordinateLoader);
    }

    @Test
    public void testType()
    {
        Assert.assertEquals("service", provider.getType());
    }

    public void testSingleService(String pattern, TableSource table, Procedure<PureModelContextData> pmcdSupplier, Function<PureSingleExecution, SQLSource> expectedFunc)
    {
        PureModelContextData pmcd = loadPureModelContextFromResource("pmcd.pure", this.getClass());

        Service service = ListIterate.select(pmcd.getElementsOfType(Service.class), s -> s.pattern.equals(pattern)).getOnly();
        PureSingleExecution execution = (PureSingleExecution) service.execution;

        pmcdSupplier.accept(pmcd);

        SQLSource expected = expectedFunc.apply(execution);

        SQLSourceResolvedContext resolved = provider.resolve(FastList.newListWith(table), null, Identity.getAnonymousIdentity());
        Assert.assertNotNull(resolved.getPureModelContext());
        Assert.assertEquals(1, resolved.getSources().size());

        assertLogicalEquality(expected, resolved.getSources().get(0));
    }

    @Test
    public void testSingleServicePatternAndCoordinates()
    {
        String pattern = "/people";
        TableSource table = new TableSource("service", FastList.newListWith(
                new TableSourceArgument("pattern", 0, pattern),
                new TableSourceArgument("coordinates", null, "group:artifact:version")
        ));

        testSingleService(pattern, table, pmcd ->
        {
            PureModelContextPointer pointer = new PureModelContextPointer();
            when(projectCoordinateLoader.resolve(eq(ProjectCoordinateWrapper.coordinates("group:artifact:version")), any())).thenReturn(new ProjectResolvedContext(pointer, pmcd));
        }, execution -> new SQLSource("service", execution.func, execution.mapping, execution.runtime, execution.executionOptions, null, FastList.newListWith(
                new SQLSourceArgument("pattern", 0, pattern),
                new SQLSourceArgument("coordinates", null, "group:artifact:version")
        )));
    }

    @Test
    public void testMultiServicePatternAndCoordinates()
    {
        String pattern = "/people/{key}";
        PureModelContextData pmcd = loadPureModelContextFromResource("pmcd.pure", this.getClass());


        PureModelContextPointer pointer = new PureModelContextPointer();
        when(projectCoordinateLoader.resolve(eq(ProjectCoordinateWrapper.coordinates("group:artifact:version")), any())).thenReturn(new ProjectResolvedContext(pointer, pmcd));

        Service service = ListIterate.select(pmcd.getElementsOfType(Service.class), s -> s.pattern.equals(pattern)).getOnly();
        PureMultiExecution multi = (PureMultiExecution) service.execution;
        KeyedExecutionParameter execution = multi.executionParameters.get(1);


        TableSource table = new TableSource("service", FastList.newListWith(
                new TableSourceArgument("pattern", 0, pattern),
                new TableSourceArgument("key", null, "k2"),
                new TableSourceArgument("coordinates", null, "group:artifact:version")
        ));

        SQLSource expected = new SQLSource("service", multi.func, execution.mapping, execution.runtime, execution.executionOptions, null, FastList.newListWith(
                new SQLSourceArgument("pattern", 0, pattern),
                new SQLSourceArgument("coordinates", null, "group:artifact:version"),
                new SQLSourceArgument("key", null, "k2")
        ));

        SQLSourceResolvedContext resolved = provider.resolve(FastList.newListWith(table), null, Identity.getAnonymousIdentity());
        Assert.assertEquals(FastList.newListWith(pointer), resolved.getPureModelContexts());
        Assert.assertEquals(1, resolved.getSources().size());

        assertLogicalEquality(expected, resolved.getSources().get(0));
    }

    @Test
    public void testSingleServicePatternPatternAndWorkspace()
    {
        String pattern = "/people";
        TableSource table = new TableSource("service", FastList.newListWith(
                new TableSourceArgument("pattern", 0, pattern),
                new TableSourceArgument("project", null, "p1"),
                new TableSourceArgument("workspace", null, "ws")
        ));

        testSingleService(pattern, table, pmcd ->
                        when(projectCoordinateLoader.resolve(eq(ProjectCoordinateWrapper.workspace("p1", "ws")), any())).thenReturn(new ProjectResolvedContext(pmcd, pmcd)),
                execution -> new SQLSource("service", execution.func, execution.mapping, execution.runtime, execution.executionOptions, null, FastList.newListWith(
                        new SQLSourceArgument("pattern", 0, pattern),
                        new SQLSourceArgument("project", null, "p1"),
                        new SQLSourceArgument("workspace", null, "ws")
                )));

    }

    @Test
    public void testSingleServicePatternPatternAndGroupWorkspace()
    {
        String pattern = "/people";
        TableSource table = new TableSource("service", FastList.newListWith(
                new TableSourceArgument("pattern", 0, pattern),
                new TableSourceArgument("project", null, "p1"),
                new TableSourceArgument("groupWorkspace", null, "gws")
        ));

        testSingleService(pattern, table, pmcd ->
                        when(projectCoordinateLoader.resolve(eq(ProjectCoordinateWrapper.groupWorkspace("p1", "gws")), any())).thenReturn(new ProjectResolvedContext(pmcd, pmcd)),
                execution -> new SQLSource("service", execution.func, execution.mapping, execution.runtime, execution.executionOptions, null, FastList.newListWith(
                        new SQLSourceArgument("pattern", 0, pattern),
                        new SQLSourceArgument("project", null, "p1"),
                        new SQLSourceArgument("groupWorkspace", null, "gws")
                )));

    }

    @Test
    public void testNoServiceFound()
    {
        PureModelContextData pmcd = loadPureModelContextFromResource("pmcd.pure", this.getClass());
        when(projectCoordinateLoader.resolve(eq(ProjectCoordinateWrapper.workspace("p1", "ws")), any())).thenReturn(new ProjectResolvedContext(pmcd, pmcd));

        TableSource table = new TableSource("service", FastList.newListWith(
                new TableSourceArgument("pattern", 0, "notfound"),
                new TableSourceArgument("project", null, "p1"),
                new TableSourceArgument("workspace", null, "ws")
        ));
        IllegalArgumentException exception = Assert.assertThrows("Should throw given no service found", IllegalArgumentException.class, () -> provider.resolve(FastList.newListWith(table), null, Identity.getAnonymousIdentity()));
        Assert.assertEquals("No element found for 'service'", exception.getMessage());
    }
}