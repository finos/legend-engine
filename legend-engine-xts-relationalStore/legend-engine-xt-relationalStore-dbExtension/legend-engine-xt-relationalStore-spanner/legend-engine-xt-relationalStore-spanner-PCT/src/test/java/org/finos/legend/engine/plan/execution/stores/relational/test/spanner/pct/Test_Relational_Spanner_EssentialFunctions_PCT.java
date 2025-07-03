// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.spanner.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Spanner_EssentialFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.essentialFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.spannerAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //add
            one("meta::pure::functions::collection::tests::add::testAddWithOffset_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'add_T_MANY__Integer_1__T_1__T_$1_MANY$_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::add::testAdd_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'add_T_MANY__T_1__T_$1_MANY$_'.", AdapterQualifier.unsupportedFeature),

            //at
            one("meta::pure::functions::collection::tests::at::testAtOtherScenario_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> map(x:String[1] | [$x, 'z'] -> plus();) -> at(0)\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::at::testAtWithVariable_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> at(1)\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::at::testAt_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> at(0)\"", AdapterQualifier.unsupportedFeature),

            //concatenate
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateMixedType_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateSimple_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateTypeInference_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            //contains
            one("meta::pure::functions::collection::tests::contains::testContainsNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::contains::testContainsPrimitive_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] operator does not exist: bigint = text\nHint: No operator matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select 1 in (1, 2, 5, 2, Text'a', Boolean'true', Date'2014-02-01', Text'c')'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::contains::testContainsWithFunction_Function_1__Boolean_1_", "no viable alternative at input '->meta::pure::functions::collection::contains(meta::pure::functions::collection::tests::contains::ClassWithoutEquality.all()->meta::pure::functions::multiplicity::toOne(),comparator(a:meta::pure::functions::collection::tests::contains::ClassWithoutEquality[1],'", AdapterQualifier.unsupportedFeature),

            //drop
            one("meta::pure::functions::collection::tests::drop::testDropExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::drop::testDropInList_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'new_Class_1__String_1__KeyExpression_MANY__T_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::drop::testDropManyOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnEmptyList_Function_1__Boolean_1_", "\"Invalid type for parameter inside the drop function. Expected a value, found operation/function\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::drop::testDropOneOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::drop::testDropZeroOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            //exists
            one("meta::pure::functions::collection::tests::exists::testExists_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            //find
            one("meta::pure::functions::collection::tests::find::testFindInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/essential/collection/iteration/find.pure:38cc38-42); error compiling generated Java code:\nimport org.eclipse.collections.api.LazyIterable;\nimport org.eclipse.collections.api.block.function.Function0;\nimport org.eclipse.collections.api.block.function.Function;\nimport org.eclipse.collections.api.block.function.Function2;\nimport org.eclipse.collections.api.block.predicate.Predicate;\nimport org.eclipse.collections.api.block.procedure.Procedure;\nimport org.eclipse.collections.api.map.ImmutableMap;\nimport org.eclipse.collections.api.map.MutableMap;\nimport org.eclipse.collections.api.map.MutableMapIterable;\nimport org.eclipse.collections.api.map.MapIterable;\nimport org.eclipse.collections.api.map.primitive.IntObjectMap;\nimport org.eclipse.collections.api.set.MutableSet;\nimport org.eclipse.collections.api.set.SetIterable;\nimport org.eclipse.collections.api.list.MutableList;\nimport org.eclipse.collections.api.list.ListIterable;\nimport org.eclipse.collections.api.RichIterable;\nimport org.eclipse.collections.api.tuple.Pair;\nimport org.eclipse.collections.impl.factory.Lists;\nimport org.eclipse.collections.impl.factory.Maps;\nimport org.eclipse.collections.impl.map.mutable.UnifiedMap;\nimport org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;\nimport org.eclipse.collections.impl.set.mutable.UnifiedSet;\nimport org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;\nimport org.eclipse.collections.impl.list.mutable.FastList;\nimport org.eclipse.collections.impl.factory.Sets;\nimport org.eclipse.collections.impl.block.function.checked.CheckedFunction0;\nimport org.eclipse.collections.impl.utility.Iterate;\nimport org.eclipse.collections.impl.utility.LazyIterate;\nimport org.eclipse.collections.impl.utility.StringIterate;\nimport org.finos.legend.pure.m3.navigation.generictype.GenericType;\nimport org.finos.legend.pure.m3.navigation.ProcessorSupport;\nimport org.finos.legend.pure.m3.execution.ExecutionSupport;\nimport org.finos.legend.pure.m3.exception.PureExecutionException;\nimport org.finos.legend.pure.m4.coreinstance.CoreInstance;\nimport org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;\nimport org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.*;\nimport org.finos.legend.pure.runtime.java.compiled.metadata.*;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.*;\nimport org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.ChangeType;\nimport org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.ChangedPath;\nimport org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;\nimport org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;\nimport org.finos.legend.pure.m3.tools.ListHelper;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\nimport org.finos.legend.pure.runtime.java.compiled.execution.*;\nimport org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\nimport org.finos.legend.pure.runtime.java.compiled.serialization.model.*;\nimport org.finos.legend.pure.runtime.java.compiled.metadata.*;\nimport java.lang.reflect.Method;\nimport java.math.BigInteger;\nimport java.sql.DatabaseMetaData;\nimport java.sql.PreparedStatement;\nimport java.sql.ResultSetMetaData;\nimport java.util.Iterator;\nimport java.util.Calendar;\nimport java.util.Map;\nimport java.util.ArrayDeque;\nimport java.util.Deque;\nimport org.json.simple.JSONObject;\nimport org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;\nimport org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;\n\n\npublic class DynaClass{\n   public static org.finos.legend.pure.generated.Root_meta_pure_functions_collection_tests_model_CO_Person doProcess(final MapIterable<String, Object> vars, final MutableMap<String, Object> valMap, final IntObjectMap<CoreInstance> localLambdas, final ExecutionSupport es){\n       return _smith;\n   }\n}\n", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::find::testFindLiteralFromVar_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::collection::find' - stack:[Function 'test::lambdaContainer__Any_MANY_' Third Pass, new lambda, Applying meta::pure::mapping::from, Applying meta::pure::functions::collection::find]", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::find::testFindLiteral_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::collection::find' - stack:[Function 'test::lambdaContainer__Any_MANY_' Third Pass, new lambda, Applying meta::pure::mapping::from, Applying meta::pure::functions::collection::find]", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::find::testFindUsingVarForFunction_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/essential/collection/iteration/find.pure:47cc38-42); error compiling generated Java code:\nimport org.eclipse.collections.api.LazyIterable;\nimport org.eclipse.collections.api.block.function.Function0;\nimport org.eclipse.collections.api.block.function.Function;\nimport org.eclipse.collections.api.block.function.Function2;\nimport org.eclipse.collections.api.block.predicate.Predicate;\nimport org.eclipse.collections.api.block.procedure.Procedure;\nimport org.eclipse.collections.api.map.ImmutableMap;\nimport org.eclipse.collections.api.map.MutableMap;\nimport org.eclipse.collections.api.map.MutableMapIterable;\nimport org.eclipse.collections.api.map.MapIterable;\nimport org.eclipse.collections.api.map.primitive.IntObjectMap;\nimport org.eclipse.collections.api.set.MutableSet;\nimport org.eclipse.collections.api.set.SetIterable;\nimport org.eclipse.collections.api.list.MutableList;\nimport org.eclipse.collections.api.list.ListIterable;\nimport org.eclipse.collections.api.RichIterable;\nimport org.eclipse.collections.api.tuple.Pair;\nimport org.eclipse.collections.impl.factory.Lists;\nimport org.eclipse.collections.impl.factory.Maps;\nimport org.eclipse.collections.impl.map.mutable.UnifiedMap;\nimport org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;\nimport org.eclipse.collections.impl.set.mutable.UnifiedSet;\nimport org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;\nimport org.eclipse.collections.impl.list.mutable.FastList;\nimport org.eclipse.collections.impl.factory.Sets;\nimport org.eclipse.collections.impl.block.function.checked.CheckedFunction0;\nimport org.eclipse.collections.impl.utility.Iterate;\nimport org.eclipse.collections.impl.utility.LazyIterate;\nimport org.eclipse.collections.impl.utility.StringIterate;\nimport org.finos.legend.pure.m3.navigation.generictype.GenericType;\nimport org.finos.legend.pure.m3.navigation.ProcessorSupport;\nimport org.finos.legend.pure.m3.execution.ExecutionSupport;\nimport org.finos.legend.pure.m3.exception.PureExecutionException;\nimport org.finos.legend.pure.m4.coreinstance.CoreInstance;\nimport org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;\nimport org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.*;\nimport org.finos.legend.pure.runtime.java.compiled.metadata.*;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.*;\nimport org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.ChangeType;\nimport org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.ChangedPath;\nimport org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;\nimport org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;\nimport org.finos.legend.pure.m3.tools.ListHelper;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\nimport org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\nimport org.finos.legend.pure.runtime.java.compiled.execution.*;\nimport org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\nimport org.finos.legend.pure.runtime.java.compiled.serialization.model.*;\nimport org.finos.legend.pure.runtime.java.compiled.metadata.*;\nimport java.lang.reflect.Method;\nimport java.math.BigInteger;\nimport java.sql.DatabaseMetaData;\nimport java.sql.PreparedStatement;\nimport java.sql.ResultSetMetaData;\nimport java.util.Iterator;\nimport java.util.Calendar;\nimport java.util.Map;\nimport java.util.ArrayDeque;\nimport java.util.Deque;\nimport org.json.simple.JSONObject;\nimport org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;\nimport org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;\n\n\npublic class DynaClass{\n   public static org.finos.legend.pure.generated.Root_meta_pure_functions_collection_tests_model_CO_Person doProcess(final MapIterable<String, Object> vars, final MutableMap<String, Object> valMap, final IntObjectMap<CoreInstance> localLambdas, final ExecutionSupport es){\n       return _smith;\n   }\n}\n", AdapterQualifier.unsupportedFeature),

            //fold
            one("meta::pure::functions::collection::tests::fold::testFoldCollectionAccumulator_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndEmptyIdentity_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndNonEmptyIdentity_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldFiltering_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::lang::copy'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldMixedAccumulatorTypes_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldToMany_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::lang::copy'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldWithEmptyAccumulator_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldWithSingleValue_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFold_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::lang::copy'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testIntegerSum_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testStringSum_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'.", AdapterQualifier.unsupportedFeature),

            //forall
            one("meta::pure::functions::collection::tests::forall::testforAllOnEmptySet_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsFalse_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsTrue_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),

            //head
            one("meta::pure::functions::collection::tests::head::testHeadComplex_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::head::testHeadOnEmptySet_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::head::testHeadOnOneElement_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::head::testHeadSimple_Function_1__Boolean_1_", "\"Cannot cast a collection of size 2 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            //indexOf
            one("meta::pure::functions::collection::tests::indexof::testIndexOfOneElement_Function_1__Boolean_1_", "\"\nexpected: 0\nactual:   1\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::indexof::testIndexOf_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'indexOf_T_MANY__T_1__Integer_1_'.", AdapterQualifier.unsupportedFeature),

            //init
            one("meta::pure::functions::collection::tests::init::testInitOnEmptySet_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::collection::init' - stack:[Function 'test::lambdaContainer__Any_MANY_' Third Pass, new lambda, Applying meta::pure::mapping::from, Applying meta::pure::functions::collection::init]", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::init::testInitOneElement_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::collection::init' - stack:[Function 'test::lambdaContainer__Any_MANY_' Third Pass, new lambda, Applying meta::pure::mapping::from, Applying meta::pure::functions::collection::init]", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::init::testInit_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::collection::init' - stack:[Function 'test::lambdaContainer__Any_MANY_' Third Pass, new lambda, Applying meta::pure::mapping::from, Applying meta::pure::functions::collection::init]", AdapterQualifier.unsupportedFeature),

            //last
            one("meta::pure::functions::collection::tests::last::testLastFromEmpty_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'last_T_MANY__T_$0_1$_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::last::testLastOfOneElementList_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'last_T_MANY__T_$0_1$_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::last::testLast_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'last_T_MANY__T_$0_1$_'.", AdapterQualifier.unsupportedFeature),

            //removeDuplicates
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesEmptyListExplicit_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesEmptyList_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveNonStandardFunction_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionExplicit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunction_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'.", AdapterQualifier.unsupportedFeature),

            //removeDuplicatesBy
            one("meta::pure::functions::collection::tests::removeDuplicatesBy::testRemoveDuplicatesByPrimitive_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'.", AdapterQualifier.unsupportedFeature),

            //reverse
            one("meta::pure::functions::collection::tests::reverse::testReverseEmpty_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'reverse_T_m__T_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::reverse::testReverse_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'reverse_T_m__T_m_'.", AdapterQualifier.unsupportedFeature),

            //slice
            one("meta::pure::functions::collection::tests::slice::testSliceEqualBounds_Function_1__Boolean_1_", "\"Cannot cast a collection of size 4 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::slice::testSliceInList_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'new_Class_1__String_1__KeyExpression_MANY__T_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::slice::testSliceOnBounds_Function_1__Boolean_1_", "\"Cannot cast a collection of size 4 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::slice::testSliceOutOfBounds_Function_1__Boolean_1_", "\"Cannot cast a collection of size 4 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::slice::testSlice_Function_1__Boolean_1_", "\"Cannot cast a collection of size 6 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            //sort
            one("meta::pure::functions::collection::tests::sort::testMixedSortNoComparator_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortNoComparator_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortReversed_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithFunctionVariables_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithKey_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSimpleSort_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSortEmptySet_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.unsupportedFeature),

            //tail
            one("meta::pure::functions::collection::tests::tail::testTailOnEmptySet_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::tail::testTailOneElement_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::tail::testTail_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'.", AdapterQualifier.unsupportedFeature),

            //take
            one("meta::pure::functions::collection::tests::take::testTakeExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::take::testTakeInList_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'new_Class_1__String_1__KeyExpression_MANY__T_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::take::testTakeManyOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnEmptyList_Function_1__Boolean_1_", "\"Invalid type for parameter inside the take/limit function. Expected a value, found operation/function\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::take::testTakeOneOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::take::testTakeZeroOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            //zip
            one("meta::pure::functions::collection::tests::zip::testZipBothListsAreOfPairs_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsSameLength_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListsIsOfPairs_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListsIsOfPairs_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsInvestigation),

            //date
            one("meta::pure::functions::date::tests::testAdjustByDaysBigNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByHoursBigNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByHours_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByMicrosecondsBigNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByMicroseconds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByMillisecondsBigNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByMilliseconds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByMinutesBigNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByMinutes_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByMonthsBigNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustBySecondsBigNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustBySeconds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByWeeksBigNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'adjust' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustReflectiveEvaluation_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::lang::eval(?)'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'dateDiff' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffHours_Function_1__Boolean_1_", "\"[unsupported-api] The function 'dateDiff' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffMilliseconds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'dateDiff' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffMinutes_Function_1__Boolean_1_", "\"[unsupported-api] The function 'dateDiff' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'dateDiff' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffSeconds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'dateDiff' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'dateDiff' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffWithDifferentTimeZones_Function_1__Boolean_1_", "\"[unsupported-api] The function 'dateDiff' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'dateDiff' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromDay_Function_1__Boolean_1_", "[unsupported-api] The function 'date' (state: [Select, false]) is not supported yet", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromHour_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromMinute_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromMonth_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Date_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromSecond_Function_1__Boolean_1_", "[unsupported-api] The function 'date' (state: [Select, false]) is not supported yet", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromSubSecond_Function_1__Boolean_1_", "[unsupported-api] The function 'date' (state: [Select, false]) is not supported yet", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromYear_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Date_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDatePartYearMonthOnly_Function_1__Boolean_1_", "Date has no day: 1973-11", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDatePartYearOnly_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDatePart_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: The Postgres Type is not supported: timestamp without time zone - Statement: 'select date(Timestamp'1973-11-05 13:01:25')'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDayOfMonth_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: The Postgres Type is not supported: timestamp without time zone - Statement: 'select extract(day from Timestamp'2015-04-15')'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasDay_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasDay_Date_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasHour_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasHour_Date_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasMinute_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMinute_Date_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasMonthReflect_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMonth_Date_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasMonth_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMonth_Date_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasSecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSecond_Date_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasSubsecondWithAtLeastPrecision_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasSubsecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecond_Date_1__Boolean_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHour_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: The Postgres Type is not supported: timestamp without time zone - Statement: 'select extract(hour from Timestamp'2015-04-15')'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testMinute_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: The Postgres Type is not supported: timestamp without time zone - Statement: 'select extract(minute from Timestamp'2015-04-15')'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testMonthNumber_Function_1__Boolean_1_", "Date has no day: 2015-04", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testSecond_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: The Postgres Type is not supported: timestamp without time zone - Statement: 'select extract(second from Timestamp'2015-04-15 17:09:21')'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testYear_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\"", AdapterQualifier.unsupportedFeature),

            //if
            one("meta::pure::functions::lang::tests::if::testMultiIf_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::lang::if(?)'", AdapterQualifier.needsInvestigation),

            //match
            one("meta::pure::functions::lang::tests::match::testMatchManyWithMany_Function_1__Boolean_1_", "\"Match only supports operands with multiplicity [1]..! Current operand : ['w', 'w', 'w']\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchOneWithMany_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchOneWithZeroOne_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchOneWith_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParam_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'match_Any_MANY__Function_$1_MANY$__P_o__T_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParamsAndFunctionsAsParam_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'match_Any_MANY__Function_$1_MANY$__P_o__T_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParamManyMatch_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParam_Function_1__Boolean_1_", "\"Cast exception: Literal cannot be cast to SemiStructuredPropertyAccess\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsManyMatch_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctions_Function_1__Boolean_1_", "\"Cast exception: Literal cannot be cast to SemiStructuredPropertyAccess\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithMixedReturnType_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchZeroWithMany_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchZeroWithZero_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatch_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            //abs
            one("meta::pure::functions::math::tests::abs::testDecimalAbs_Function_1__Boolean_1_", "\"\nexpected: 3.0D\nactual:   3D\"", AdapterQualifier.needsInvestigation),

            //log10
            one("meta::pure::functions::math::tests::log10::testNumberLog10_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function log10(double precision) is not supported - Statement: 'select log10(10)'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::log10::testSimpleLog10_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function log10(double precision) is not supported - Statement: 'select log10(10)'", AdapterQualifier.unsupportedFeature),

            //mod
            one("meta::pure::functions::math::tests::mod::testModInEval_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"mod(%s,%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::mod::testModWithNegativeNumbers_Function_1__Boolean_1_", "\"\nexpected: 3\nactual:   -2\"", AdapterQualifier.needsInvestigation),

            //pow
            one("meta::pure::functions::math::tests::pow::testNumberPow_Function_1__Boolean_1_", "\"\nexpected: 9.0\nactual:   27.0\"", AdapterQualifier.needsInvestigation),

            //rem
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithNegativeNumbers_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"mod(%s,%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::rem::testRemInEval_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"mod(%s,%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::rem::testRemWithDecimals_Function_1__Boolean_1_", "\"\nexpected: 0.14D\nactual:   0.14\"", AdapterQualifier.needsInvestigation),

            //round
            one("meta::pure::functions::math::tests::round::testDecimalRound_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"round(%s::float)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::round::testFloatRoundWithScale_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"round(%s::float)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::round::testNegativeFloatRoundHalfEvenUp_Function_1__Boolean_1_", "\"\nexpected: -16\nactual:   -17\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::round::testPositiveFloatRoundHalfEvenDown_Function_1__Boolean_1_", "\"\nexpected: 16\nactual:   17\"", AdapterQualifier.needsInvestigation),

            //sign
            one("meta::pure::functions::math::tests::sign::testSign_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function sign(numeric) is not supported - Statement: 'select sign(-10.5)'", AdapterQualifier.unsupportedFeature),

            //toDecimal
            one("meta::pure::functions::math::tests::toDecimal::testDecimalToDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toDecimal' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::toDecimal::testDoubleToDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toDecimal' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::toDecimal::testIntToDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toDecimal' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //toFloat
            one("meta::pure::functions::math::tests::toFloat::testDecimalToFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toFloat' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::toFloat::testDoubleToFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toFloat' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::toFloat::testIntToFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toFloat' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //trigonometry
            one("meta::pure::functions::math::tests::trigonometry::testCoTangentEval_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function cot(double precision) is not supported - Statement: 'select cot(1.5)'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::trigonometry::testCoTangent_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function cot(double precision) is not supported - Statement: 'select cot(1)'", AdapterQualifier.unsupportedFeature),

            //cubeRoot
            one("meta::pure::functions::math::tests::testCubeRootEval_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function cbrt(double precision) is not supported - Statement: 'select cbrt(27)'", AdapterQualifier.unsupportedFeature),

            //cubeRoot
            one("meta::pure::functions::math::tests::testCubeRoot_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function cbrt(double precision) is not supported - Statement: 'select cbrt(0)'", AdapterQualifier.unsupportedFeature),

            //squareRoot
            one("meta::pure::functions::math::tests::testSquareRoot_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function sqrt(numeric) is not supported - Statement: 'select sqrt(0.01)'", AdapterQualifier.unsupportedFeature),

            //format
            one("meta::pure::functions::string::tests::format::testFormatBoolean_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithRounding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithTruncation_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatFloat_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatInEval_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatInEvaluate_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatIntegerWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatInteger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatPair_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatRepr_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatString_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testSimpleFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.unsupportedFeature),

            //indexOf
            one("meta::pure::functions::string::tests::indexOf::testFromIndex_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'indexOf_String_1__String_1__Integer_1__Integer_1_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::indexOf::testSimple_Function_1__Boolean_1_", "\"\nexpected: 4\nactual:   5\"", AdapterQualifier.needsInvestigation),

            //joinStrings
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsNoStrings_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsSingleString_Function_1__Boolean_1_", "\"The database type 'Spanner' is not supported yet!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsUsingGenericArrow_Function_1__Boolean_1_", "\"\nexpected: 'a,b,c'\nactual:   ',a,b,c,'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStrings_Function_1__Boolean_1_", "\"\nexpected: 'a,b,c'\nactual:   ',a,b,c,'\"", AdapterQualifier.needsInvestigation),

            //parseBoolean
            one("meta::pure::functions::string::tests::parseBoolean::testParseFalse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseBoolean::testParseTrue_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //parseDate
            one("meta::pure::functions::string::tests::parseDate::testParseDateTypes_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithTimezone_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithZ_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseDate::testParseDate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //parseDecimal
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseDecimal::testParseZero_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //parseFloat
            one("meta::pure::functions::string::tests::parseFloat::testParseFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseFloat' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseFloat::testParseZero_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseFloat' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //parseInteger
            one("meta::pure::functions::string::tests::parseInteger::testParseInteger_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseInteger' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //split
            one("meta::pure::functions::string::tests::split::testSplitWithNoSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::split::testSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'.", AdapterQualifier.unsupportedFeature),

            //substring
            one("meta::pure::functions::string::tests::substring::testStartEnd_Function_1__Boolean_1_", "\"\nexpected: 'he quick brown fox jumps over the lazy do'\nactual:   'the quick brown fox jumps over the lazy do'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::substring::testStart_Function_1__Boolean_1_", "\"\nexpected: 'he quick brown fox jumps over the lazy dog'\nactual:   'the quick brown fox jumps over the lazy dog'\"", AdapterQualifier.needsInvestigation),

            //variant
            one("meta::pure::functions::collection::tests::fold::testFold_FromVariantAsPrimitive_Function_1__Boolean_1_", "[unsupported-api] relational lambda processeing not supported for Database Type: Spanner", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFold_FromVariant_Function_1__Boolean_1_", "[unsupported-api] relational lambda processeing not supported for Database Type: Spanner", AdapterQualifier.unsupportedFeature),

            //toString
            one("meta::pure::functions::string::tests::toString::testBooleanToString_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testClassToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testComplexClassToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testDateTimeToString_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testDateTimeWithTimezoneToString_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testDateToString_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testEnumerationToString_Function_1__Boolean_1_", "\"Match failure: ClassInstanceHolderObject instanceOf ClassInstanceHolder\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testFloatToStringWithExcessTrailingZeros_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testFloatToStringWithNegativeExponent_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testFloatToStringWithNoLeadingZero_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testFloatToStringWithPositiveExponent_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testFloatToString_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testIntegerToString_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testListToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPairCollectionToString_Function_1__Boolean_1_", "Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPairToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testSimpleDateToString_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testStringToString_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature)
            );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Spanner).getFirst())
        );
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public Adapter getAdapter()
    {
        return adapter;
    }

    @Override
    public String getPlatform()
    {
        return platform;
    }
}
