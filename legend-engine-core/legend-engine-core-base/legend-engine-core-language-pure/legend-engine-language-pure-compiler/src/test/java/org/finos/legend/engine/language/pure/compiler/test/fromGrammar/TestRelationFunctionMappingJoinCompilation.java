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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Compilation coverage for relation mapping joins: modelJoin, navigationJoin and filterJoin.
 */
public class TestRelationFunctionMappingJoinCompilation extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
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
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::class'";
    }

    // ------------------------------------------------------------------
    // Shared fixture — Person / Firm / Address + relation-returning functions.
    // ------------------------------------------------------------------

    private static final String CLASSES_SOURCE = "###Pure\n" +
            "Class my::Person\n" +
            "{\n" +
            "  firstName: String[1];\n" +
            "  firmId: Integer[1];\n" +
            "  addressId: Integer[1];\n" +
            "  firm: my::Firm[1];\n" +
            "}\n" +
            "Class my::Firm\n" +
            "{\n" +
            "  id: Integer[1];\n" +
            "  legalName: String[1];\n" +
            "  addressId: Integer[1];\n" +
            "}\n" +
            "Class my::Address\n" +
            "{\n" +
            "  city: String[1];\n" +
            "}\n";

    private static final String FUNCTIONS_SOURCE = "###Pure\n" +
            "function my::personRel(): meta::pure::metamodel::relation::Relation<(FIRSTNAME:String[1], FIRMID:Integer[1], ADDRESSID:Integer[1])>[1]\n" +
            "{\n" +
            "  1->cast(@meta::pure::metamodel::relation::Relation<(FIRSTNAME:String[1], FIRMID:Integer[1], ADDRESSID:Integer[1])>);\n" +
            "}\n" +
            "function my::firmRel(): meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1], ADDRESSID:Integer[1])>[1]\n" +
            "{\n" +
            "  1->cast(@meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1], ADDRESSID:Integer[1])>);\n" +
            "}\n" +
            "function my::addressRel(): meta::pure::metamodel::relation::Relation<(ID:Integer[1], CITY:String[1])>[1]\n" +
            "{\n" +
            "  1->cast(@meta::pure::metamodel::relation::Relation<(ID:Integer[1], CITY:String[1])>);\n" +
            "}\n";

    private static final String FIXTURE = CLASSES_SOURCE + FUNCTIONS_SOURCE;

    // ------------------------------------------------------------------
    // Helpers.
    // ------------------------------------------------------------------

    private static PureModel compileOk(String fullSource)
    {
        return test(fullSource).getTwo();
    }

    private static void compileErrorContains(String fullSource, String expectedFragment)
    {
        try
        {
            test(fullSource);
            fail("Expected compilation/parser failure containing: " + expectedFragment);
        }
        catch (EngineException e)
        {
            MatcherAssert.assertThat(
                    "Unexpected exception message. Got: " + e.getMessage(),
                    e.getMessage(),
                    CoreMatchers.containsString(expectedFragment));
        }
    }

    private static RelationFunctionInstanceSetImplementation relSet(PureModel pureModel, String mappingPath, String setId)
    {
        Mapping mapping = pureModel.getMapping(mappingPath);
        SetImplementation set = mapping._classMappings().detect(c -> setId.equals(c._id()));
        assertNotNull("No set implementation with id '" + setId + "' in mapping " + mappingPath, set);
        assertTrue("Expected RelationFunctionInstanceSetImplementation, got " + set.getClass().getName(),
                set instanceof RelationFunctionInstanceSetImplementation);
        return (RelationFunctionInstanceSetImplementation) set;
    }

    private static PropertyMapping pmByProperty(RelationFunctionInstanceSetImplementation set, String propertyName)
    {
        MutableList<? extends PropertyMapping> pms = Lists.mutable.withAll(set._propertyMappings());
        PropertyMapping pm = pms.detect(p -> propertyName.equals(p._property()._name()));
        assertNotNull("No property mapping for '" + propertyName + "' in set '" + set._id() + "'", pm);
        return pm;
    }

    // ==================================================================
    // modelJoin: class navigation → ModelJoinPropertyMapping graph shape.
    // ==================================================================

    @Test
    public void testModelJoinLowersToModelJoinPropertyMapping()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    firm: modelJoin({p, f | $p.firmId == $f.id})\n" +
                "  }\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation personSet = relSet(pureModel, "my::testMapping", "person");
        PropertyMapping firmPm = pmByProperty(personSet, "firm");
        assertTrue("firm property must lower to a ModelJoinPropertyMapping, got " + firmPm.getClass().getName(),
                firmPm instanceof ModelJoinPropertyMapping);
        ModelJoinPropertyMapping mjpm = (ModelJoinPropertyMapping) firmPm;

        assertEquals("person", mjpm._sourceSetImplementationId());
        assertEquals("firm", mjpm._targetSetImplementationId());

        LambdaFunction<?> cond = mjpm._joinCondition();
        assertNotNull("modelJoin condition must be compiled to a typed LambdaFunction", cond);
        assertEquals("Boolean",
                cond._expressionSequence().getLast()._genericType()._rawType()._name());
    }

    @Test
    public void testModelJoinToManyProperty()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Pure\n" +
                "Class my::FirmWithEmployees\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  legalName: String[1];\n" +
                "  employees: my::Person[*];\n" +
                "}\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                "  *my::FirmWithEmployees[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME,\n" +
                "    employees: modelJoin({f, p | $f.id == $p.firmId})\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation firmSet = relSet(pureModel, "my::testMapping", "firm");
        PropertyMapping employeesPm = pmByProperty(firmSet, "employees");
        assertTrue("employees property must lower to a ModelJoinPropertyMapping",
                employeesPm instanceof ModelJoinPropertyMapping);
        assertEquals("person", ((ModelJoinPropertyMapping) employeesPm)._targetSetImplementationId());
    }

    // ==================================================================
    // navigationJoin / filterJoin: typed valueFn RFPMs.
    // ==================================================================

    @Test
    public void testNavigationJoinTypesValueFnToTargetColumn()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmName: String[1]: $src->navigationJoin(my::firmRel(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation personSet = relSet(pureModel, "my::testMapping", "person");
        PropertyMapping firmNamePm = pmByProperty(personSet, "firmName");
        assertTrue("firmName must remain a RelationFunctionPropertyMapping (valueFn form), got " + firmNamePm.getClass().getName(),
                firmNamePm instanceof RelationFunctionPropertyMapping);
        RelationFunctionPropertyMapping rfpm = (RelationFunctionPropertyMapping) firmNamePm;
        assertNotNull("navigationJoin RFPM must carry a valueFn", rfpm._valueFn());
        assertEquals("String",
                rfpm._valueFn()._expressionSequence().getLast()._genericType()._rawType()._name());
    }

    @Test
    public void testNavigationJoinWithExplicitKindCompiles()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmName: String[1]: $src->navigationJoin(my::firmRel(), meta::pure::functions::relation::JoinKind.INNER, {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation personSet = relSet(pureModel, "my::testMapping", "person");
        PropertyMapping firmNamePm = pmByProperty(personSet, "firmName");
        assertTrue(firmNamePm instanceof RelationFunctionPropertyMapping);
        assertEquals("String",
                ((RelationFunctionPropertyMapping) firmNamePm)._valueFn()._expressionSequence().getLast()._genericType()._rawType()._name());
    }

    @Test
    public void testFilterJoinWithExplicitKindCompiles()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [FIRSTNAME]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +gatedName: String[1]: $src->filterJoin(my::firmRel(), meta::pure::functions::relation::JoinKind.INNER, {p, f | $p.FIRMID == $f.ID}).FIRSTNAME\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation personSet = relSet(pureModel, "my::testMapping", "person");
        assertTrue(pmByProperty(personSet, "gatedName") instanceof RelationFunctionPropertyMapping);
    }

    @Test
    public void testNavigationJoinWithWrongArityRejected()
    {
        // 5 args matches neither the 3-arg (no kind) nor the 4-arg (explicit kind) overload, so the
        // custom ParametersInference declines and the call falls through to ordinary (unhelpful,
        // pre-existing) function-resolution failure rather than a clear 'navigationJoin'-specific error.
        compileErrorContains(FIXTURE +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    firstName: FIRSTNAME,\n" +
                        "    +firmName: String[1]: $src->navigationJoin(my::firmRel(), meta::pure::functions::relation::JoinKind.INNER, false, {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                        "  }\n" +
                        ")\n",
                "Can't find variable class for variable 'p'");
    }

    @Test
    public void testFilterJoinTypesValueFnToSourceColumn()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [FIRSTNAME]\n" +
                "    +firstName: String[1]: $src->filterJoin(my::firmRel(), {p, f | $p.FIRMID == $f.ID}).FIRSTNAME\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation personSet = relSet(pureModel, "my::testMapping", "person");
        PropertyMapping firstNamePm = pmByProperty(personSet, "firstName");
        assertTrue(firstNamePm instanceof RelationFunctionPropertyMapping);
        RelationFunctionPropertyMapping rfpm = (RelationFunctionPropertyMapping) firstNamePm;
        assertEquals("String",
                rfpm._valueFn()._expressionSequence().getLast()._genericType()._rawType()._name());
    }

    @Test
    public void testChainedNavigationJoinTypes()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [FIRSTNAME]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmCity: String[1]: $src->navigationJoin(my::firmRel(), {p, f | $p.FIRMID == $f.ID})" +
                "->navigationJoin(my::addressRel(), {f, a | $f.ADDRESSID == $a.ID}).CITY\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation personSet = relSet(pureModel, "my::testMapping", "person");
        RelationFunctionPropertyMapping rfpm = (RelationFunctionPropertyMapping) pmByProperty(personSet, "firmCity");
        assertEquals("String",
                rfpm._valueFn()._expressionSequence().getLast()._genericType()._rawType()._name());
    }

    @Test
    public void testChainedFilterThenNavigationJoinTypes()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [FIRSTNAME]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmCity: String[1]: $src->filterJoin(my::firmRel(), {p, f | $p.FIRMID == $f.ID})" +
                "->navigationJoin(my::addressRel(), {p, a | $p.ADDRESSID == $a.ID}).CITY\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation personSet = relSet(pureModel, "my::testMapping", "person");
        RelationFunctionPropertyMapping rfpm = (RelationFunctionPropertyMapping) pmByProperty(personSet, "firmCity");
        assertEquals("String",
                rfpm._valueFn()._expressionSequence().getLast()._genericType()._rawType()._name());
    }

    @Test
    public void testSelfJoinNavigationJoinTypes()
    {
        PureModel pureModel = compileOk(
                "###Pure\n" +
                "Class my::Employee\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  managerId: Integer[1];\n" +
                "}\n" +
                "function my::employeeRel(): meta::pure::metamodel::relation::Relation<(ID:Integer[1], FIRSTNAME:String[1], MANAGERID:Integer[1])>[1]\n" +
                "{\n" +
                "  1->cast(@meta::pure::metamodel::relation::Relation<(ID:Integer[1], FIRSTNAME:String[1], MANAGERID:Integer[1])>);\n" +
                "}\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Employee[emp]: Relation\n" +
                "  {\n" +
                "    ~func my::employeeRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +managerName: String[1]: $src->navigationJoin(my::employeeRel(), {e, m | $e.MANAGERID == $m.ID}).FIRSTNAME\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation empSet = relSet(pureModel, "my::testMapping", "emp");
        RelationFunctionPropertyMapping rfpm = (RelationFunctionPropertyMapping) pmByProperty(empSet, "managerName");
        assertEquals("String",
                rfpm._valueFn()._expressionSequence().getLast()._genericType()._rawType()._name());
    }

    @Test
    public void testNavigationJoinToManyPrimitiveCollection()
    {
        compileOk(
                "###Pure\n" +
                "Class my::Firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  clientNames: String[*];\n" +
                "}\n" +
                "function my::firmRel2(): meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1])>[1]\n" +
                "{\n" +
                "  1->cast(@meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1])>);\n" +
                "}\n" +
                "function my::clientRel(): meta::pure::metamodel::relation::Relation<(FIRMID:Integer[1], NAME:String[1])>[1]\n" +
                "{\n" +
                "  1->cast(@meta::pure::metamodel::relation::Relation<(FIRMID:Integer[1], NAME:String[1])>);\n" +
                "}\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel2():Relation<Any>[1]\n" +
                "    legalName: LEGALNAME,\n" +
                "    clientNames: $src->navigationJoin(my::clientRel(), {f, c | $f.ID == $c.FIRMID}).NAME\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testNavigateThroughInlineModelJoin()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    firm: modelJoin({p, f | $p.firmId == $f.id})\n" +
                "  }\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [ID]\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation personSet = relSet(pureModel, "my::testMapping", "person");
        ModelJoinPropertyMapping mjpm = (ModelJoinPropertyMapping) pmByProperty(personSet, "firm");
        assertEquals("firm", mjpm._targetSetImplementationId());
        RelationFunctionInstanceSetImplementation firmSet = relSet(pureModel, "my::testMapping", "firm");
        PropertyMapping legalNamePm = pmByProperty(firmSet, "legalName");
        assertTrue("legalName on the target Firm set must be an ordinary RelationFunctionPropertyMapping",
                legalNamePm instanceof RelationFunctionPropertyMapping);
    }

    // ==================================================================
    // Negative cases.
    // ==================================================================

    // ==================================================================
    // modelJoin: target class mapping is always resolved from the property's declared type —
    // there is no explicit target reference (the construct's old, now-removed '[setId]' form).
    // ==================================================================

    @Test
    public void testModelJoinResolvesTargetFromPropertyType()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    firm: modelJoin({p, f | $p.firmId == $f.id})\n" +
                "  }\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation personSet = relSet(pureModel, "my::testMapping", "person");
        ModelJoinPropertyMapping mjpm = (ModelJoinPropertyMapping) pmByProperty(personSet, "firm");
        assertEquals("firm", mjpm._targetSetImplementationId());
    }

    @Test
    public void testModelJoinTargetResolutionFailsWithNoRootMappingForType()
    {
        compileErrorContains(FIXTURE +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    firm: modelJoin({p, f | $p.firmId == $f.id})\n" +
                        "  }\n" +
                        ")\n",
                "modelJoin: expected exactly one root class mapping for type 'my::Firm' in this mapping or its includes, found 0.");
    }

    @Test
    public void testModelJoinTargetResolutionFailsForSubtypeOnlyTarget()
    {
        // Exact-class resolution deliberately does not extend to subtypes — the mapping has no root
        // class mapping whose class is exactly 'my::Firm', so resolution fails even though a subtype
        // mapping for the property's type exists, and there is no explicit-target escape hatch.
        compileErrorContains(FIXTURE +
                        "###Pure\n" +
                        "Class my::PrivateFirm extends my::Firm\n" +
                        "{\n" +
                        "}\n" +
                        "function my::privateFirmRel(): meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1], ADDRESSID:Integer[1])>[1]\n" +
                        "{\n" +
                        "  1->cast(@meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1], ADDRESSID:Integer[1])>);\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    firm: modelJoin({p, f | $p.firmId == $f.id})\n" +
                        "  }\n" +
                        "  *my::PrivateFirm[privateFirm]: Relation\n" +
                        "  {\n" +
                        "    ~func my::privateFirmRel():Relation<Any>[1]\n" +
                        "    id: ID\n" +
                        "  }\n" +
                        ")\n",
                "modelJoin: expected exactly one root class mapping for type 'my::Firm' in this mapping or its includes, found 0.");
    }

    @Test
    public void testModelJoinConditionWrongArityRejected()
    {
        compileErrorContains(FIXTURE +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    firm: modelJoin({p | true})\n" +
                        "  }\n" +
                        "  *my::Firm[firm]: Relation\n" +
                        "  {\n" +
                        "    ~func my::firmRel():Relation<Any>[1]\n" +
                        "    id: ID\n" +
                        "  }\n" +
                        ")\n",
                "ModelJoin requires a lambda with exactly 2 parameters");
    }

    @Test
    public void testModelJoinConditionNonBooleanRejected()
    {
        compileErrorContains(FIXTURE +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    firm: modelJoin({p, f | $p.firmId})\n" +
                        "  }\n" +
                        "  *my::Firm[firm]: Relation\n" +
                        "  {\n" +
                        "    ~func my::firmRel():Relation<Any>[1]\n" +
                        "    id: ID\n" +
                        "  }\n" +
                        ")\n",
                "ModelJoin property mapping function should return 'Boolean[1]'");
    }

    @Test
    public void testNavigationJoinUnknownTargetColumnRejected()
    {
        compileErrorContains(FIXTURE +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    +firmName: String[1]: $src->navigationJoin(my::firmRel(), {p, f | $p.FIRMID == $f.ID}).NOSUCHCOL\n" +
                        "  }\n" +
                        ")\n",
                "NOSUCHCOL");
    }

    @Test
    public void testNavigationJoinUntypedTargetHasNoColumns()
    {
        compileErrorContains(FIXTURE +
                        "###Pure\n" +
                        "function my::untypedFirmRel(): meta::pure::metamodel::relation::Relation<Any>[1]\n" +
                        "{\n" +
                        "  my::firmRel()\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    +firmName: String[1]: $src->navigationJoin(my::untypedFirmRel(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                        "  }\n" +
                        ")\n",
                "Can't find property 'ID' in class 'meta::pure::metamodel::type::Any'");
    }

    @Test
    public void testPlatformNavigationJoinCompilesInOrdinaryPureCode()
    {
        compileOk(FIXTURE +
                "###Pure\n" +
                "function my::joinInOrdinaryCode(): meta::pure::metamodel::relation::Relation<Any>[1]\n" +
                "{\n" +
                "  my::personRel()->meta::pure::functions::relation::navigationJoin(my::firmRel(), {p, f | $p.FIRMID == $f.ID})\n" +
                "}\n");
    }

    @Ignore("ToFix: a transformed $src (here a filter) before a join operator is not supported yet; the source must be $src or another join")
    @Test
    public void testFilteredSourceBeforeJoinCompiles()
    {
        compileOk(FIXTURE +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    ~primaryKey: [FIRSTNAME]\n" +
                        "    +firmName: String[1]: $src->filter(r | $r.FIRMID > 0)->navigationJoin(my::firmRel(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                        "  }\n" +
                        ")\n");
    }

    @Test
    public void testJoinTargetWithOperatorsAllowed()
    {
        // The target can be any expression that evaluates to a typed Relation, not just a bare named
        // function call — here, a function call piped into a further operator.
        compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [FIRSTNAME]\n" +
                "    +firmName: String[1]: $src->navigationJoin(my::firmRel()->filter(f | $f.ID > 0), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testJoinTargetFunctionWithParametersAllowed()
    {
        compileOk(FIXTURE +
                "###Pure\n" +
                "function my::firmsFrom(minId: Integer[1]): meta::pure::metamodel::relation::Relation<(ID:Integer[1], LEGALNAME:String[1], ADDRESSID:Integer[1])>[1]\n" +
                "{\n" +
                "  my::firmRel()\n" +
                "}\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [FIRSTNAME]\n" +
                "    +firmName: String[1]: $src->navigationJoin(my::firmsFrom(1), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testLocalPropertyModelJoinLowersToModelJoinPropertyMapping()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +employer: my::Firm[0..1]: modelJoin({p, f | $p.firmId == $f.id})\n" +
                "  }\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [ID]\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n");
        PropertyMapping employer = pmByProperty(relSet(pureModel, "my::testMapping", "person"), "employer");
        assertTrue(employer instanceof ModelJoinPropertyMapping);
        assertEquals("firm", employer._targetSetImplementationId());
        assertTrue(employer._localMappingProperty());
        assertEquals("Firm", employer._localMappingPropertyType()._name());
        assertEquals("[0..1]", org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(employer._localMappingPropertyMultiplicity()));
    }

    @Test
    public void testModelJoinConditionWithNewInstanceCompiles()
    {
        compileOk(personMappingWith("    firm: modelJoin({p, f | $p.firmId == $f.id && ^my::Address(city = $p.firstName).city == $f.legalName})\n", FIRM_RELATION_MAPPING));
    }

    @Test
    public void testNavigationJoinConditionWithColSpecCompiles()
    {
        compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    +firmName: String[*]: $src->navigationJoin(my::firmRel(), {p, f | ($p.FIRMID == $f.ID) && (my::addressRel()->select(~CITY)->size() > 0)}).LEGALNAME\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testNavigationJoinConditionCanBeAFunctionReference()
    {
        compileOk(FIXTURE +
                "###Pure\n" +
                "function my::sameFirm(p: Any[1], f: Any[1]): Boolean[1]\n" +
                "{\n" +
                "  true\n" +
                "}\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    +firmName: String[*]: $src->navigationJoin(my::firmRel(), my::sameFirm_Any_1__Any_1__Boolean_1_).LEGALNAME\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testColOnOrdinaryRelationRejectedOutsideMapping()
    {
        compileErrorContains(FIXTURE +
                        "###Pure\n" +
                        "function my::illegalColUse(): Any[1]\n" +
                        "{\n" +
                        "  my::firmRel()->filter(r | $r.LEGALNAME == 'x').LEGALNAME\n" +
                        "}\n",
                "LEGALNAME");
    }

    @Test
    public void testAssociationMappingForAnotherPropertyBetweenSameClassesAllowsModelJoin()
    {
        compileOk(FIXTURE +
                "###Pure\n" +
                "Association my::Person_Employer\n" +
                "{\n" +
                "  staff: my::Person[*];\n" +
                "  employer: my::Firm[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmId: Integer[1]: FIRMID,\n" +
                "    firm: modelJoin({p, f | $p.firmId == $f.id})\n" +
                "  }\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [ID]\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                "  my::Person_Employer: ModelJoin\n" +
                "  {\n" +
                "    {employer: my::Firm[1], staff: my::Person[1] | $employer.id == $staff.firmId}\n" +
                "  }\n" +
                ")\n");
    }

    // ==================================================================
    // modelJoin to-one navigation does not require the join condition to be keyed on the target's
    // primary key: equating a PK column doesn't actually guarantee the join is to-one, so the
    // compiler does not try to prove it and leaves correctness to the mapper (matching
    // ###Relational's @Join|Col, which allows the same shape for a to-one property mapping).
    // ==================================================================

    @Test
    public void testModelJoinToOneKeyedOnTargetPrimaryKeyCompiles()
    {
        compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    firm: modelJoin({p, f | $p.firmId == $f.id})\n" +
                "  }\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [ID]\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testModelJoinToOneNotKeyedOnPrimaryKeyCompiles()
    {
        compileOk(FIXTURE +
                        "###Pure\n" +
                        "Class my::PersonByName\n" +
                        "{\n" +
                        "  firstName: String[1];\n" +
                        "  firmName: String[1];\n" +
                        "  firm: my::Firm[1];\n" +
                        "}\n" +
                        "function my::personByNameRel(): meta::pure::metamodel::relation::Relation<(FIRSTNAME:String[1], FIRMNAME:String[1])>[1]\n" +
                        "{\n" +
                        "  1->cast(@meta::pure::metamodel::relation::Relation<(FIRSTNAME:String[1], FIRMNAME:String[1])>);\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::PersonByName[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personByNameRel():Relation<Any>[1]\n" +
                        "    firstName: FIRSTNAME,\n" +
                        "    firm: modelJoin({p, f | $p.firmName == $f.legalName})\n" +
                        "  }\n" +
                        "  *my::Firm[firm]: Relation\n" +
                        "  {\n" +
                        "    ~func my::firmRel():Relation<Any>[1]\n" +
                        "    ~primaryKey: [ID]\n" +
                        "    id: ID,\n" +
                        "    legalName: LEGALNAME\n" +
                        "  }\n" +
                        ")\n");
    }

    @Test
    public void testModelJoinToOneEquatingTargetPrimaryKeyToItselfCompiles()
    {
        compileOk(FIXTURE +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    firstName: FIRSTNAME,\n" +
                        "    firm: modelJoin({p, f | $f.id == $f.id})\n" +
                        "  }\n" +
                        "  *my::Firm[firm]: Relation\n" +
                        "  {\n" +
                        "    ~func my::firmRel():Relation<Any>[1]\n" +
                        "    ~primaryKey: [ID]\n" +
                        "    id: ID,\n" +
                        "    legalName: LEGALNAME\n" +
                        "  }\n" +
                        ")\n");
    }

    @Test
    public void testModelJoinToOneKeyedOnTargetPrimaryKeyEqualToConstantCompiles()
    {
        compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    firm: modelJoin({p, f | $p.firmId > 0 && $f.id == 1})\n" +
                "  }\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [ID]\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testModelJoinToManyOverNonPrimaryKeyCompiles()
    {
        compileOk(FIXTURE +
                "###Pure\n" +
                "Class my::FirmWithEmployees\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  employees: my::Person[*];\n" +
                "}\n" +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [FIRSTNAME]\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                "  *my::FirmWithEmployees[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    id: ID,\n" +
                "    employees: modelJoin({f, p | $f.id == $p.firmId})\n" +
                "  }\n" +
                ")\n");
    }

    // ==================================================================
    // modelJoin is model-level: its condition is over the two classes' properties (like an association
    // ModelJoin's), its params may declare their types, and its target is any store's root class mapping.
    // ==================================================================

    private static final String FIRM_RELATION_MAPPING =
            "  *my::Firm[firm]: Relation\n" +
            "  {\n" +
            "    ~func my::firmRel():Relation<Any>[1]\n" +
            "    id: ID,\n" +
            "    legalName: LEGALNAME\n" +
            "  }\n";

    private static String personMappingWith(String firmPropertyMapping, String otherClassMappings)
    {
        return FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    firmId: FIRMID,\n" +
                firmPropertyMapping +
                "  }\n" +
                otherClassMappings +
                ")\n";
    }

    @Test
    public void testModelJoinConditionIsTypedToTheClasses()
    {
        PureModel pureModel = compileOk(personMappingWith("    firm: modelJoin({p, f | $p.firmId == $f.id})\n", FIRM_RELATION_MAPPING));
        LambdaFunction<?> cond = ((ModelJoinPropertyMapping) pmByProperty(relSet(pureModel, "my::testMapping", "person"), "firm"))._joinCondition();
        MutableList<String> paramTypes = Lists.mutable.withAll(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType) cond._classifierGenericType()._typeArguments().getOnly()._rawType())._parameters())
                .collect(v -> v._name() + ":" + v._genericType()._rawType()._name());
        assertEquals(Lists.mutable.with("_mj_src:Person", "_mj_tgt:Firm"), paramTypes);
    }

    @Test
    public void testModelJoinWithDeclaredParameterTypesCompiles()
    {
        compileOk(personMappingWith("    firm: modelJoin({p: my::Person[1], f: my::Firm[1] | $p.firmId == $f.id})\n", FIRM_RELATION_MAPPING));
    }

    @Test
    public void testModelJoinUsesLocalMappingPropertiesOfTheTarget()
    {
        compileOk(personMappingWith("    firm: modelJoin({p, f | $p.firmId == $f.firmKey})\n",
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmRel():Relation<Any>[1]\n" +
                "    legalName: LEGALNAME,\n" +
                "    +firmKey: Integer[1]: ID\n" +
                "  }\n"));
    }

    @Test
    public void testModelJoinRejectsRelationColumns()
    {
        compileErrorContains(personMappingWith("    firm: modelJoin({p, f | $p.FIRMID == $f.ID})\n", FIRM_RELATION_MAPPING), "Can't find property 'FIRMID'");
    }

    @Test
    public void testModelJoinRejectsAmbiguousTargetAcrossIncludes()
    {
        compileErrorContains(FIXTURE +
                        "###Mapping\n" +
                        "import my::*;\n" +
                        "Mapping my::firmMapping\n" +
                        "(\n" +
                        FIRM_RELATION_MAPPING +
                        ")\n" +
                        "Mapping my::testMapping\n" +
                        "(\n" +
                        "  include my::firmMapping\n" +
                        "  *my::Person[person]: Relation\n" +
                        "  {\n" +
                        "    ~func my::personRel():Relation<Any>[1]\n" +
                        "    firmId: FIRMID,\n" +
                        "    firm: modelJoin({p, f | $p.firmId == $f.id})\n" +
                        "  }\n" +
                        "  *my::Firm[localFirm]: Relation\n" +
                        "  {\n" +
                        "    ~func my::firmRel():Relation<Any>[1]\n" +
                        "    id: ID\n" +
                        "  }\n" +
                        ")\n",
                "modelJoin: expected exactly one root class mapping for type 'my::Firm' in this mapping or its includes, found 2: [firm, localFirm].");
    }

    @Test
    public void testModelJoinResolvesTargetFromIncludedMapping()
    {
        PureModel pureModel = compileOk(FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::firmMapping\n" +
                "(\n" +
                FIRM_RELATION_MAPPING +
                ")\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  include my::firmMapping\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firmId: FIRMID,\n" +
                "    firm: modelJoin({p, f | $p.firmId == $f.id})\n" +
                "  }\n" +
                ")\n");
        assertEquals("firm", pmByProperty(relSet(pureModel, "my::testMapping", "person"), "firm")._targetSetImplementationId());
    }

    @Test
    public void testModelJoinTargetCanBeANonRelationClassMapping()
    {
        PureModel pureModel = compileOk(personMappingWith("    firm: modelJoin({p, f | $p.firmId == $f.id})\n",
                "  *my::Firm[firm]: Pure\n" +
                "  {\n" +
                "    ~src my::Firm\n" +
                "    id: $src.id,\n" +
                "    legalName: $src.legalName\n" +
                "  }\n"));
        assertEquals("firm", pmByProperty(relSet(pureModel, "my::testMapping", "person"), "firm")._targetSetImplementationId());
    }

    // ==================================================================
    // Parity with legend-pure's Relation-mapping join checks.
    // ==================================================================

    private static String personMappingWithProperties(String propertyMappings)
    {
        return FIXTURE +
                "###Mapping\n" +
                "import my::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    ~primaryKey: [FIRSTNAME]\n" +
                "    firstName: FIRSTNAME,\n" +
                propertyMappings +
                "  }\n" +
                ")\n";
    }

    @Test
    public void testNavigationJoinValueFnDoesNotCaptureSrc()
    {
        PureModel pureModel = compileOk(personMappingWithProperties("    +firmName: String[1]: $src->navigationJoin(my::firmRel(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n"));
        RelationFunctionPropertyMapping firmName = (RelationFunctionPropertyMapping) pmByProperty(relSet(pureModel, "my::testMapping", "person"), "firmName");
        assertFalse(firmName._valueFn()._openVariables().contains("src"));
    }

    @Test
    public void testQualifiedModelJoinCompiles()
    {
        PureModel pureModel = compileOk(personMappingWith("    firm: meta::pure::functions::relation::modelJoin({p, f | $p.firmId == $f.id})\n", FIRM_RELATION_MAPPING));
        ModelJoinPropertyMapping firm = (ModelJoinPropertyMapping) pmByProperty(relSet(pureModel, "my::testMapping", "person"), "firm");
        assertEquals("firm", firm._targetSetImplementationId());
    }

    private static String personModelJoinMappingWithIncludes(String includes, String targetMappings)
    {
        return "Mapping my::testMapping\n(\n" + includes +
                "  *my::Person[person]: Relation\n  {\n" +
                "    ~func my::personRel():Relation<Any>[1]\n" +
                "    firmId: FIRMID,\n" +
                "    firm: modelJoin({p, f | $p.firmId == $f.id})\n  }\n" +
                targetMappings + ")\n";
    }

    @Test
    public void testModelJoinTargetIsCountedOnceThroughAnIncludeDiamond()
    {
        PureModel model = compileOk(FIXTURE + "###Mapping\nimport my::*;\n" +
                "Mapping my::base\n(\n" + FIRM_RELATION_MAPPING + ")\n" +
                "Mapping my::left ( include my::base )\n" +
                "Mapping my::right ( include my::base )\n" +
                personModelJoinMappingWithIncludes("  include my::left\n  include my::right\n", ""));
        assertEquals("firm", pmByProperty(relSet(model, "my::testMapping", "person"), "firm")._targetSetImplementationId());
    }

}
