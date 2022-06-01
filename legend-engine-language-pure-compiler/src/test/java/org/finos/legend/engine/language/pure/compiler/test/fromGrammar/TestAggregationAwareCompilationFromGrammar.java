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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelToModel.PureInstanceSetImplementation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Comparator;

public class TestAggregationAwareCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Test
    public void testAggregationAwareMappingGrammarSingleAggregate()
    {
        PureModel pureModel = test("###Pure\n" +
                "Class test::Sales\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  salesDate: test::FiscalCalendar[1];\n" +
                "  revenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class test::FiscalCalendar\n" +
                "{\n" +
                "  date: Date[1];\n" +
                "  fiscalYear: Integer[1];\n" +
                "  fiscalMonth: Integer[1];\n" +
                "  fiscalQtr: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Sales_By_Date\n" +
                "{\n" +
                "  salesDate: test::FiscalCalendar[1];\n" +
                "  netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::math::sum(numbers: Float[*]): Float[1]\n" +
                "{\n" +
                "   $numbers->plus()\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::map\n" +
                "(\n" +
                "  test::FiscalCalendar[b]: Pure\n" +
                "  {\n" +
                "    ~src test::FiscalCalendar\n" +
                "    date: $src.date,\n" +
                "    fiscalYear: $src.fiscalYear,\n" +
                "    fiscalMonth: $src.fiscalMonth,\n" +
                "    fiscalQtr: $src.fiscalQtr\n" +
                "  }\n" +
                "  test::Sales[a]: AggregationAware \n" +
                "  {\n" +
                "    Views: [\n" +
                "      (\n" +
                "        ~modelOperation: {\n" +
                "          ~canAggregate true,\n" +
                "          ~groupByFunctions (\n" +
                "            $this.salesDate\n" +
                "          ),\n" +
                "          ~aggregateValues (\n" +
                "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                "          )\n" +
                "        },\n" +
                "        ~aggregateMapping: Pure\n" +
                "        {\n" +
                "          ~src test::Sales_By_Date\n" +
                "          salesDate[b]: $src.salesDate,\n" +
                "          revenue: $src.netRevenue\n" +
                "        }\n" +
                "      )\n" +
                "    ],\n" +
                "    ~mainMapping: Pure\n" +
                "    {\n" +
                "      ~src test::Sales\n" +
                "      salesDate[b]: $src.salesDate,\n" +
                "      revenue: $src.revenue\n" +
                "    }\n" +
                "  }\n" +
                ")\n").getTwo();

        InstanceSetImplementation setImpl = (InstanceSetImplementation) pureModel.getMapping("test::map")._classMappings().toSortedList(new Comparator<SetImplementation>()
        {
            @Override
            public int compare(SetImplementation o1, SetImplementation o2)
            {
                return o1._id().compareTo(o2._id());
            }
        }).get(0);

        Assert.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assert.assertEquals("a", aggSetImpl._id());

        Assert.assertNotNull(aggSetImpl._mainSetImplementation());
        Assert.assertTrue(aggSetImpl._mainSetImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 1);

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingGrammarMultiAggregate()
    {
        PureModel pureModel = test("###Pure\n" +
                "Class test::Sales\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  salesDate: test::FiscalCalendar[1];\n" +
                "  revenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class test::FiscalCalendar\n" +
                "{\n" +
                "  date: Date[1];\n" +
                "  fiscalYear: Integer[1];\n" +
                "  fiscalMonth: Integer[1];\n" +
                "  fiscalQtr: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Sales_By_Date\n" +
                "{\n" +
                "  salesDate: test::FiscalCalendar[1];\n" +
                "  netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Sales_By_Qtr\n" +
                "{\n" +
                "  salesQtrFirstDate: test::FiscalCalendar[1];\n" +
                "  netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::math::sum(numbers: Float[*]): Float[1]\n" +
                "{\n" +
                "   $numbers->plus()\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::map\n" +
                "(\n" +
                "  test::FiscalCalendar[b]: Pure\n" +
                "  {\n" +
                "    ~src test::FiscalCalendar\n" +
                "    date: $src.date,\n" +
                "    fiscalYear: $src.fiscalYear,\n" +
                "    fiscalMonth: $src.fiscalMonth,\n" +
                "    fiscalQtr: $src.fiscalQtr\n" +
                "  }\n" +
                "  test::Sales[a]: AggregationAware \n" +
                "  {\n" +
                "    Views: [\n" +
                "      (\n" +
                "        ~modelOperation: {\n" +
                "          ~canAggregate false,\n" +
                "          ~groupByFunctions (\n" +
                "            $this.salesDate.fiscalYear,\n" +
                "            $this.salesDate.fiscalQtr\n" +
                "          ),\n" +
                "          ~aggregateValues (\n" +
                "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                "          )\n" +
                "        },\n" +
                "        ~aggregateMapping: Pure\n" +
                "        {\n" +
                "          ~src test::Sales_By_Qtr\n" +
                "          salesDate[b]: $src.salesQtrFirstDate,\n" +
                "          revenue: $src.netRevenue\n" +
                "        }\n" +
                "      )\n" +
                ",\n" +
                "      (\n" +
                "        ~modelOperation: {\n" +
                "          ~canAggregate true,\n" +
                "          ~groupByFunctions (\n" +
                "            $this.salesDate\n" +
                "          ),\n" +
                "          ~aggregateValues (\n" +
                "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                "          )\n" +
                "        },\n" +
                "        ~aggregateMapping: Pure\n" +
                "        {\n" +
                "          ~src test::Sales_By_Date\n" +
                "          salesDate[b]: $src.salesDate,\n" +
                "          revenue: $src.netRevenue\n" +
                "        }\n" +
                "      )\n" +
                "    ],\n" +
                "    ~mainMapping: Pure\n" +
                "    {\n" +
                "      ~src test::Sales\n" +
                "      salesDate[b]: $src.salesDate,\n" +
                "      revenue: $src.revenue\n" +
                "    }\n" +
                "  }\n" +
                ")\n").getTwo();

        InstanceSetImplementation setImpl = (InstanceSetImplementation) pureModel.getMapping("test::map")._classMappings().toSortedList(new Comparator<SetImplementation>()
        {
            @Override
            public int compare(SetImplementation o1, SetImplementation o2)
            {
                return o1._id().compareTo(o2._id());
            }
        }).get(0);

        Assert.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assert.assertEquals("a", aggSetImpl._id());

        Assert.assertNotNull(aggSetImpl._mainSetImplementation());
        Assert.assertTrue(aggSetImpl._mainSetImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 2);

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assert.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 2);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_1", aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingGrammarMultiViewsMultiAggregateValues()
    {
        PureModel pureModel = test("###Pure\n" +
                "Class test::Sales\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  salesDate: test::FiscalCalendar[1];\n" +
                "  revenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class test::FiscalCalendar\n" +
                "{\n" +
                "  date: Date[1];\n" +
                "  fiscalYear: Integer[1];\n" +
                "  fiscalMonth: Integer[1];\n" +
                "  fiscalQtr: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Sales_By_Date\n" +
                "{\n" +
                "  salesDate: test::FiscalCalendar[1];\n" +
                "  netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Sales_By_Qtr\n" +
                "{\n" +
                "  salesQtrFirstDate: test::FiscalCalendar[1];\n" +
                "  netRevenue: Float[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::math::sum(numbers: Float[*]): Float[1]\n" +
                "{\n" +
                "   $numbers->plus()\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::map\n" +
                "(\n" +
                "  test::FiscalCalendar[b]: Pure\n" +
                "  {\n" +
                "    ~src test::FiscalCalendar\n" +
                "    date: $src.date,\n" +
                "    fiscalYear: $src.fiscalYear,\n" +
                "    fiscalMonth: $src.fiscalMonth,\n" +
                "    fiscalQtr: $src.fiscalQtr\n" +
                "  }\n" +
                "  test::Sales[a]: AggregationAware \n" +
                "  {\n" +
                "    Views: [\n" +
                "      (\n" +
                "        ~modelOperation: {\n" +
                "          ~canAggregate false,\n" +
                "          ~groupByFunctions (\n" +
                "            $this.salesDate.fiscalYear,\n" +
                "            $this.salesDate.fiscalQtr\n" +
                "          ),\n" +
                "          ~aggregateValues (\n" +
                "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                "          )\n" +
                "        },\n" +
                "        ~aggregateMapping: Pure\n" +
                "        {\n" +
                "          ~src test::Sales_By_Qtr\n" +
                "          salesDate[b]: $src.salesQtrFirstDate,\n" +
                "          revenue: $src.netRevenue\n" +
                "        }\n" +
                "      )\n" +
                ",\n" +
                "      (\n" +
                "        ~modelOperation: {\n" +
                "          ~canAggregate true,\n" +
                "          ~groupByFunctions (\n" +
                "            $this.salesDate\n" +
                "          ),\n" +
                "          ~aggregateValues (\n" +
                "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() ),\n" +
                "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                "          )\n" +
                "        },\n" +
                "        ~aggregateMapping: Pure\n" +
                "        {\n" +
                "          ~src test::Sales_By_Date\n" +
                "          salesDate[b]: $src.salesDate,\n" +
                "          revenue: $src.netRevenue\n" +
                "        }\n" +
                "      )\n" +
                "    ],\n" +
                "    ~mainMapping: Pure\n" +
                "    {\n" +
                "      ~src test::Sales\n" +
                "      salesDate[b]: $src.salesDate,\n" +
                "      revenue: $src.revenue\n" +
                "    }\n" +
                "  }\n" +
                ")\n").getTwo();

        InstanceSetImplementation setImpl = (InstanceSetImplementation) pureModel.getMapping("test::map")._classMappings().toSortedList(new Comparator<SetImplementation>()
        {
            @Override
            public int compare(SetImplementation o1, SetImplementation o2)
            {
                return o1._id().compareTo(o2._id());
            }
        }).get(0);

        Assert.assertTrue(setImpl instanceof AggregationAwareSetImplementation);

        AggregationAwareSetImplementation aggSetImpl = (AggregationAwareSetImplementation) setImpl;
        Assert.assertEquals("a", aggSetImpl._id());

        Assert.assertNotNull(aggSetImpl._mainSetImplementation());
        Assert.assertTrue(aggSetImpl._mainSetImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Main", aggSetImpl._mainSetImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().size() == 2);

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification());
        Assert.assertFalse(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._groupByFunctions().size() == 2);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._aggregateSpecification()._aggregateValues().size() == 1);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_0", aggSetImpl._aggregateSetImplementations().toList().get(0)._setImplementation()._id());

        Assert.assertNotNull(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._canAggregate());
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._groupByFunctions().size() == 1);
        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._aggregateSpecification()._aggregateValues().size() == 2);

        Assert.assertTrue(aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation() instanceof PureInstanceSetImplementation);
        Assert.assertEquals("a_Aggregate_1", aggSetImpl._aggregateSetImplementations().toList().get(1)._setImplementation()._id());
    }

    @Test
    public void testAggregationAwareMappingErrorInMainSetImplementationTarget()
    {
        try
        {
            test("###Pure\n" +
                    "Class test::Sales\n" +
                    "{\n" +
                    "  id: Integer[1];\n" +
                    "  salesDate: test::FiscalCalendar[1];\n" +
                    "  revenue: Float[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class test::FiscalCalendar\n" +
                    "{\n" +
                    "  date: Date[1];\n" +
                    "  fiscalYear: Integer[1];\n" +
                    "  fiscalMonth: Integer[1];\n" +
                    "  fiscalQtr: Integer[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class test::Sales_By_Date\n" +
                    "{\n" +
                    "  salesDate: test::FiscalCalendar[1];\n" +
                    "  netRevenue: Float[1];\n" +
                    "}\n" +
                    "\n" +
                    "function meta::pure::functions::math::sum(numbers: Float[*]): Float[1]\n" +
                    "{\n" +
                    "   $numbers->plus()\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "###Mapping\n" +
                    "Mapping test::map\n" +
                    "(\n" +
                    "  test::FiscalCalendar[b]: Pure\n" +
                    "  {\n" +
                    "    ~src test::FiscalCalendar\n" +
                    "    date: $src.date,\n" +
                    "    fiscalYear: $src.fiscalYear,\n" +
                    "    fiscalMonth: $src.fiscalMonth,\n" +
                    "    fiscalQtr: $src.fiscalQtr\n" +
                    "  }\n" +
                    "  test::Sales[a]: AggregationAware \n" +
                    "  {\n" +
                    "    Views: [\n" +
                    "      (\n" +
                    "        ~modelOperation: {\n" +
                    "          ~canAggregate true,\n" +
                    "          ~groupByFunctions (\n" +
                    "            $this.salesDate\n" +
                    "          ),\n" +
                    "          ~aggregateValues (\n" +
                    "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                    "          )\n" +
                    "        },\n" +
                    "        ~aggregateMapping: Pure\n" +
                    "        {\n" +
                    "          ~src test::Sales_By_Date\n" +
                    "          salesDate[b]: $src.salesDate,\n" +
                    "          revenue: $src.netRevenue\n" +
                    "        }\n" +
                    "      )\n" +
                    "    ],\n" +
                    "    ~mainMapping: Pure\n" +
                    "    {\n" +
                    "      ~src test::Sales\n" +
                    "      salesDate[b]: $src.salesDate_NonExistent,\n" +
                    "      revenue: $src.revenue\n" +
                    "    }\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Can't find property 'salesDate_NonExistent' in class 'test::Sales'", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInMainSetImplementationProperty()
    {
        try
        {
            test("###Pure\n" +
                    "Class test::Sales\n" +
                    "{\n" +
                    "  id: Integer[1];\n" +
                    "  salesDate: test::FiscalCalendar[1];\n" +
                    "  revenue: Float[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class test::FiscalCalendar\n" +
                    "{\n" +
                    "  date: Date[1];\n" +
                    "  fiscalYear: Integer[1];\n" +
                    "  fiscalMonth: Integer[1];\n" +
                    "  fiscalQtr: Integer[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class test::Sales_By_Date\n" +
                    "{\n" +
                    "  salesDate: test::FiscalCalendar[1];\n" +
                    "  netRevenue: Float[1];\n" +
                    "}\n" +
                    "\n" +
                    "function meta::pure::functions::math::sum(numbers: Float[*]): Float[1]\n" +
                    "{\n" +
                    "   $numbers->plus()\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "###Mapping\n" +
                    "Mapping test::map\n" +
                    "(\n" +
                    "  test::FiscalCalendar[b]: Pure\n" +
                    "  {\n" +
                    "    ~src test::FiscalCalendar\n" +
                    "    date: $src.date,\n" +
                    "    fiscalYear: $src.fiscalYear,\n" +
                    "    fiscalMonth: $src.fiscalMonth,\n" +
                    "    fiscalQtr: $src.fiscalQtr\n" +
                    "  }\n" +
                    "  test::Sales[a]: AggregationAware \n" +
                    "  {\n" +
                    "    Views: [\n" +
                    "      (\n" +
                    "        ~modelOperation: {\n" +
                    "          ~canAggregate true,\n" +
                    "          ~groupByFunctions (\n" +
                    "            $this.salesDate\n" +
                    "          ),\n" +
                    "          ~aggregateValues (\n" +
                    "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                    "          )\n" +
                    "        },\n" +
                    "        ~aggregateMapping: Pure\n" +
                    "        {\n" +
                    "          ~src test::Sales_By_Date\n" +
                    "          salesDate[b]: $src.salesDate,\n" +
                    "          revenue: $src.netRevenue\n" +
                    "        }\n" +
                    "      )\n" +
                    "    ],\n" +
                    "    ~mainMapping: Pure\n" +
                    "    {\n" +
                    "      ~src test::Sales\n" +
                    "      salesDate_nonExistent[b]: $src.salesDate,\n" +
                    "      revenue: $src.revenue\n" +
                    "    }\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Can't find property 'salesDate_nonExistent' in [Sales, Any]", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewTarget()
    {
        try
        {
            test("###Pure\n" +
                    "Class test::Sales\n" +
                    "{\n" +
                    "  id: Integer[1];\n" +
                    "  salesDate: test::FiscalCalendar[1];\n" +
                    "  revenue: Float[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class test::FiscalCalendar\n" +
                    "{\n" +
                    "  date: Date[1];\n" +
                    "  fiscalYear: Integer[1];\n" +
                    "  fiscalMonth: Integer[1];\n" +
                    "  fiscalQtr: Integer[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class test::Sales_By_Date\n" +
                    "{\n" +
                    "  salesDate: test::FiscalCalendar[1];\n" +
                    "  netRevenue: Float[1];\n" +
                    "}\n" +
                    "\n" +
                    "function meta::pure::functions::math::sum(numbers: Float[*]): Float[1]\n" +
                    "{\n" +
                    "   $numbers->plus()\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "###Mapping\n" +
                    "Mapping test::map\n" +
                    "(\n" +
                    "  test::FiscalCalendar[b]: Pure\n" +
                    "  {\n" +
                    "    ~src test::FiscalCalendar\n" +
                    "    date: $src.date,\n" +
                    "    fiscalYear: $src.fiscalYear,\n" +
                    "    fiscalMonth: $src.fiscalMonth,\n" +
                    "    fiscalQtr: $src.fiscalQtr\n" +
                    "  }\n" +
                    "  test::Sales[a]: AggregationAware \n" +
                    "  {\n" +
                    "    Views: [\n" +
                    "      (\n" +
                    "        ~modelOperation: {\n" +
                    "          ~canAggregate true,\n" +
                    "          ~groupByFunctions (\n" +
                    "            $this.salesDate\n" +
                    "          ),\n" +
                    "          ~aggregateValues (\n" +
                    "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                    "          )\n" +
                    "        },\n" +
                    "        ~aggregateMapping: Pure\n" +
                    "        {\n" +
                    "          ~src test::Sales_By_Date\n" +
                    "          salesDate[b]: $src.salesDate_NonExistent,\n" +
                    "          revenue: $src.netRevenue\n" +
                    "        }\n" +
                    "      )\n" +
                    "    ],\n" +
                    "    ~mainMapping: Pure\n" +
                    "    {\n" +
                    "      ~src test::Sales\n" +
                    "      salesDate[b]: $src.salesDate,\n" +
                    "      revenue: $src.revenue\n" +
                    "    }\n" +
                    "  }\n" +
                    ")\n"
            );
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Can't find property 'salesDate_NonExistent' in class 'test::Sales_By_Date'", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewProperty()
    {
        try
        {
            test(
                    "###Pure\n" +
                            "Class test::Sales\n" +
                            "{\n" +
                            "  id: Integer[1];\n" +
                            "  salesDate: test::FiscalCalendar[1];\n" +
                            "  revenue: Float[1];\n" +
                            "}\n" +
                            "\n" +
                            "Class test::FiscalCalendar\n" +
                            "{\n" +
                            "  date: Date[1];\n" +
                            "  fiscalYear: Integer[1];\n" +
                            "  fiscalMonth: Integer[1];\n" +
                            "  fiscalQtr: Integer[1];\n" +
                            "}\n" +
                            "\n" +
                            "Class test::Sales_By_Date\n" +
                            "{\n" +
                            "  salesDate: test::FiscalCalendar[1];\n" +
                            "  netRevenue: Float[1];\n" +
                            "}\n" +
                            "\n" +
                            "function meta::pure::functions::math::sum(numbers: Float[*]): Float[1]\n" +
                            "{\n" +
                            "   $numbers->plus()\n" +
                            "}\n" +
                            "\n" +
                            "\n" +
                            "###Mapping\n" +
                            "Mapping test::map\n" +
                            "(\n" +
                            "  test::FiscalCalendar[b]: Pure\n" +
                            "  {\n" +
                            "    ~src test::FiscalCalendar\n" +
                            "    date: $src.date,\n" +
                            "    fiscalYear: $src.fiscalYear,\n" +
                            "    fiscalMonth: $src.fiscalMonth,\n" +
                            "    fiscalQtr: $src.fiscalQtr\n" +
                            "  }\n" +
                            "  test::Sales[a]: AggregationAware \n" +
                            "  {\n" +
                            "    Views: [\n" +
                            "      (\n" +
                            "        ~modelOperation: {\n" +
                            "          ~canAggregate true,\n" +
                            "          ~groupByFunctions (\n" +
                            "            $this.salesDate\n" +
                            "          ),\n" +
                            "          ~aggregateValues (\n" +
                            "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                            "          )\n" +
                            "        },\n" +
                            "        ~aggregateMapping: Pure\n" +
                            "        {\n" +
                            "          ~src test::Sales_By_Date\n" +
                            "          salesDate_NonExistent[b]: $src.salesDate,\n" +
                            "          revenue: $src.netRevenue\n" +
                            "        }\n" +
                            "      )\n" +
                            "    ],\n" +
                            "    ~mainMapping: Pure\n" +
                            "    {\n" +
                            "      ~src test::Sales\n" +
                            "      salesDate[b]: $src.salesDate,\n" +
                            "      revenue: $src.revenue\n" +
                            "    }\n" +
                            "  }\n" +
                            ")\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Can't find property 'salesDate_NonExistent' in [Sales, Any]", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewModelOperationGroupByFunction()
    {
        try
        {
            test(
                    "###Pure\n" +
                            "Class test::Sales\n" +
                            "{\n" +
                            "  id: Integer[1];\n" +
                            "  salesDate: test::FiscalCalendar[1];\n" +
                            "  revenue: Float[1];\n" +
                            "}\n" +
                            "\n" +
                            "Class test::FiscalCalendar\n" +
                            "{\n" +
                            "  date: Date[1];\n" +
                            "  fiscalYear: Integer[1];\n" +
                            "  fiscalMonth: Integer[1];\n" +
                            "  fiscalQtr: Integer[1];\n" +
                            "}\n" +
                            "\n" +
                            "Class test::Sales_By_Date\n" +
                            "{\n" +
                            "  salesDate: test::FiscalCalendar[1];\n" +
                            "  netRevenue: Float[1];\n" +
                            "}\n" +
                            "\n" +
                            "\n" +
                            "###Mapping\n" +
                            "Mapping test::map\n" +
                            "(\n" +
                            "  test::FiscalCalendar[b]: Pure\n" +
                            "  {\n" +
                            "    ~src test::FiscalCalendar\n" +
                            "    date: $src.date,\n" +
                            "    fiscalYear: $src.fiscalYear,\n" +
                            "    fiscalMonth: $src.fiscalMonth,\n" +
                            "    fiscalQtr: $src.fiscalQtr\n" +
                            "  }\n" +
                            "  test::Sales[a]: AggregationAware \n" +
                            "  {\n" +
                            "    Views: [\n" +
                            "      (\n" +
                            "        ~modelOperation: {\n" +
                            "          ~canAggregate true,\n" +
                            "          ~groupByFunctions (\n" +
                            "            $this.salesDate_NonExistent\n" +
                            "          ),\n" +
                            "          ~aggregateValues (\n" +
                            "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->sum() )\n" +
                            "          )\n" +
                            "        },\n" +
                            "        ~aggregateMapping: Pure\n" +
                            "        {\n" +
                            "          ~src test::Sales_By_Date\n" +
                            "          salesDate[b]: $src.salesDate,\n" +
                            "          revenue: $src.netRevenue\n" +
                            "        }\n" +
                            "      )\n" +
                            "    ],\n" +
                            "    ~mainMapping: Pure\n" +
                            "    {\n" +
                            "      ~src test::Sales\n" +
                            "      salesDate[b]: $src.salesDate,\n" +
                            "      revenue: $src.revenue\n" +
                            "    }\n" +
                            "  }\n" +
                            ")\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Can't find property 'salesDate_NonExistent' in class 'test::Sales'", e.getMessage());
        }
    }

    @Test
    public void testAggregationAwareMappingErrorInAggregateViewModelOperationAggregateFunction()
    {
        try
        {
            test("###Pure\n" +
                    "Class test::Sales\n" +
                    "{\n" +
                    "  id: Integer[1];\n" +
                    "  salesDate: test::FiscalCalendar[1];\n" +
                    "  revenue: Float[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class test::FiscalCalendar\n" +
                    "{\n" +
                    "  date: Date[1];\n" +
                    "  fiscalYear: Integer[1];\n" +
                    "  fiscalMonth: Integer[1];\n" +
                    "  fiscalQtr: Integer[1];\n" +
                    "}\n" +
                    "\n" +
                    "Class test::Sales_By_Date\n" +
                    "{\n" +
                    "  salesDate: test::FiscalCalendar[1];\n" +
                    "  netRevenue: Float[1];\n" +
                    "}\n" +
                    "\n" +
                    "\n" +
                    "###Mapping\n" +
                    "Mapping test::map\n" +
                    "(\n" +
                    "  test::FiscalCalendar[b]: Pure\n" +
                    "  {\n" +
                    "    ~src test::FiscalCalendar\n" +
                    "    date: $src.date,\n" +
                    "    fiscalYear: $src.fiscalYear,\n" +
                    "    fiscalMonth: $src.fiscalMonth,\n" +
                    "    fiscalQtr: $src.fiscalQtr\n" +
                    "  }\n" +
                    "  test::Sales[a]: AggregationAware \n" +
                    "  {\n" +
                    "    Views: [\n" +
                    "      (\n" +
                    "        ~modelOperation: {\n" +
                    "          ~canAggregate true,\n" +
                    "          ~groupByFunctions (\n" +
                    "            $this.salesDate\n" +
                    "          ),\n" +
                    "          ~aggregateValues (\n" +
                    "            ( ~mapFn:$this.revenue , ~aggregateFn: $mapped->summation() )\n" +
                    "          )\n" +
                    "        },\n" +
                    "        ~aggregateMapping: Pure\n" +
                    "        {\n" +
                    "          ~src test::Sales_By_Date\n" +
                    "          salesDate[b]: $src.salesDate,\n" +
                    "          revenue: $src.netRevenue\n" +
                    "        }\n" +
                    "      )\n" +
                    "    ],\n" +
                    "    ~mainMapping: Pure\n" +
                    "    {\n" +
                    "      ~src test::Sales\n" +
                    "      salesDate[b]: $src.salesDate,\n" +
                    "      revenue: $src.revenue\n" +
                    "    }\n" +
                    "  }\n" +
                    ")\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Can't resolve the builder for function 'summation' - stack:[Lambda, new lambda, Applying summation]", e.getMessage());
        }
    }

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Mapping\n" +
                "Mapping anything::class\n" +
                "(\n" +
                ")\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::class'";
    }
}
