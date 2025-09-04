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
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.query.sql.providers.core.SQLSource;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceResolvedContext;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.core.TableSourceArgument;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateLoader;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateWrapper;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectResolvedContext;
import org.finos.legend.engine.query.sql.providers.shared.utils.SQLProviderUtils;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.finos.legend.engine.query.sql.providers.shared.SQLSourceProviderTestUtils.loadPureModelContextFromResource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestFunctionSQLSourceProvider
{
    @Mock
    private ProjectCoordinateLoader projectCoordinateLoader;

    private FunctionSQLSourceProvider provider;

    @Before
    public void setup()
    {
        this.provider = new FunctionSQLSourceProvider(projectCoordinateLoader);
    }

    @Test
    public void testType()
    {
        Assert.assertEquals("func", provider.getType());
    }

    @Test
    public void testWorkspace()
    {
        String functionName = "simple::func::simpleFunction_String_MANY__TabularDataSet_1_";

        ProjectCoordinateWrapper coordinates = ProjectCoordinateWrapper.workspace("proj1", "ws1");

        PureModelContextData pmcd = loadPureModelContextFromResource("function-pmcd.pure", this.getClass());
        Function function = SQLProviderUtils.extractElement("function", pmcd.getElementsOfType(Function.class), f -> f.getPath().equals(functionName));

        when(projectCoordinateLoader.resolve(eq(coordinates), any())).thenReturn(new ProjectResolvedContext(pmcd, pmcd));

        TableSource tableSource = createTableSource(functionName,
                new TableSourceArgument("project", null, "proj1"),
                new TableSourceArgument("workspace", null, "ws1")
        );

        LambdaFunction lambda = new LambdaFunction();
        lambda.body = function.body;
        lambda.parameters = function.parameters;

        SQLSource expected = new SQLSource("func", lambda, null, null, FastList.newList(), null, FastList.newListWith(
                new SQLSourceArgument("path", 0, functionName),
                new SQLSourceArgument("project", null, "proj1"),
                new SQLSourceArgument("workspace", null, "ws1")
        ));

        testSuccess(tableSource, pmcd, expected);
    }

    @Test
    public void testCoordinates()
    {
        testCoordinates("simple::func::simpleFunction_String_MANY__TabularDataSet_1_");
    }

    @Test
    public void testRelation()
    {
        testCoordinates("simple::func::relationFunction_String_MANY__Relation_1_");
    }

    private void testCoordinates(String functionName)
    {
        ProjectCoordinateWrapper coordinates = ProjectCoordinateWrapper.coordinates("proj1:art:1.0.0");

        PureModelContextData pmcd = loadPureModelContextFromResource("function-pmcd.pure", this.getClass());
        Function function = SQLProviderUtils.extractElement("function", pmcd.getElementsOfType(Function.class), f -> f.getPath().equals(functionName));
        PureModelContextPointer pointer = new PureModelContextPointer();

        when(projectCoordinateLoader.resolve(eq(coordinates), any())).thenReturn(new ProjectResolvedContext(pointer, pmcd));

        TableSource tableSource = createTableSource(functionName,
                new TableSourceArgument("coordinates", null, "proj1:art:1.0.0")
        );

        LambdaFunction lambda = new LambdaFunction();
        lambda.body = function.body;
        lambda.parameters = function.parameters;

        SQLSource expected = new SQLSource("func", lambda, null, null, FastList.newList(), null, FastList.newListWith(
                new SQLSourceArgument("path", 0, functionName),
                new SQLSourceArgument("coordinates", null, "proj1:art:1.0.0")
        ));

        testSuccess(tableSource, pointer, expected);
    }

    @Test
    public void testNoProjectOrCoordinates()
    {
        TableSource tableSource = createTableSource("simple::func__TabularDataSet_1_");
        testException(tableSource, IllegalArgumentException.class, "coordinates or project/workspace must be supplied");
    }

    @Test
    public void testNoWorkspaceWithProject()
    {
        TableSource tableSource = createTableSource("simple::func__TabularDataSet_1_", new TableSourceArgument("project", null, "proj1"));
        testException(tableSource, IllegalArgumentException.class, "workspace/group workspace must be supplied if loading from project");
    }

    @Test
    public void testNotTDSFunc()
    {
        String functionName = "simple::func::nonTdsFunction__String_1_";

        ProjectCoordinateWrapper coordinates = ProjectCoordinateWrapper.coordinates("proj1:art:1.0.0");

        PureModelContextData pmcd = loadPureModelContextFromResource("function-pmcd.pure", this.getClass());

        when(projectCoordinateLoader.resolve(eq(coordinates), any())).thenReturn(new ProjectResolvedContext(pmcd, pmcd));

        TableSource tableSource = createTableSource(functionName,
                new TableSourceArgument("coordinates", null, "proj1:art:1.0.0")
        );

        testException(tableSource, EngineException.class, "Function " + functionName + " does not return a supported data type. Supported types: [meta::pure::tds::TabularDataSet, meta::relational::mapping::TableTDS, meta::pure::metamodel::relation::Relation]");
    }

    private <T extends Throwable> void testException(TableSource tableSource, Class<T> throwable, String expected)
    {
        T exception = Assert.assertThrows("Should throw given no service found", throwable, () -> provider.resolve(FastList.newListWith(tableSource), null, Identity.getAnonymousIdentity()));
        Assert.assertEquals(expected, exception.getMessage());
    }

    private void testSuccess(TableSource tableSource, PureModelContext expectedContext, SQLSource expected)
    {
        SQLSourceResolvedContext result = provider.resolve(FastList.newListWith(tableSource), null, Identity.getAnonymousIdentity());

        //ASSERT
        Assert.assertEquals(FastList.newListWith(expectedContext), result.getPureModelContexts());
        Assert.assertEquals(1, result.getSources().size());

        SQLSourceProviderTestUtils.assertLogicalEquality(expected, result.getSources().get(0));
    }

    private final TableSource createTableSource(String func, TableSourceArgument... extraArguments)
    {
        return new TableSource("func", FastList.newListWith(
                new TableSourceArgument(null, 0, func)).with(extraArguments)
        );
    }
}

