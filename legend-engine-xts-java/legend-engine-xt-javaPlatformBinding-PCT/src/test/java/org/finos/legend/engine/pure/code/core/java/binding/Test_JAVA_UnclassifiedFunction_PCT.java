// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.java.binding;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.code.core.CoreJavaPlatformBindingCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_UnclassifiedFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //ascii
            one("meta::pure::functions::string::tests::ascii::testAsciiMultiCharString_Function_1__Boolean_1_", "\"meta::pure::functions::string::ascii_String_1__Integer_1_ is not supported yet!\""),
            one("meta::pure::functions::string::tests::ascii::testAsciiSpecialCharacter_Function_1__Boolean_1_", "\"meta::pure::functions::string::ascii_String_1__Integer_1_ is not supported yet!\""),
            one("meta::pure::functions::string::tests::ascii::testAscii_Function_1__Boolean_1_", "\"meta::pure::functions::string::ascii_String_1__Integer_1_ is not supported yet!\""),
            //char
            one("meta::pure::functions::string::tests::char::testChar_Function_1__Boolean_1_", "\"meta::pure::functions::string::char_Integer_1__String_1_ is not supported yet!\""),

            //repeatString
            one("meta::pure::functions::string::tests::repeatstring::testRepeatStringNoString_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 1 error compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:14: error: incompatible types: no instance(s) of type variable(s) T exist so that java.util.List<T> conforms to java.lang.String\n            return Library.repeatString(Collections.emptyList(), (int) 2L);\n                          ^\n\n/_pure/plan/root/Execute.java\n0001 package _pure.plan.root;\n0002 \n0003 import java.util.Collections;\n0004 import java.util.List;\n0005 import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;\n0006 import org.finos.legend.engine.plan.dependencies.util.Library;\n0007 \n0008 public class Execute\n0009 {\n0010     public static String execute(IExecutionNodeContext context)\n0011     {\n0012         try\n0013         {\n0014             return Library.repeatString(Collections.emptyList(), (int) 2L);\n0015         }\n0016         catch (Exception e)\n0017         {\n0018             throw new RuntimeException(\"Failed in node: root\", e);\n0019         }\n0020     }"),

            //toLowerFirstCharacter
            one("meta::pure::functions::string::tests::tolower::TestToLowerFirstCharacterAlreadyLower_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toLowerFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toLowerFirstCharacter]"),
            one("meta::pure::functions::string::tests::tolower::TestToLowerFirstCharacterEmptyString_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toLowerFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toLowerFirstCharacter]"),
            one("meta::pure::functions::string::tests::tolower::TestToLowerFirstCharacterManyStrings_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toLowerFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toLowerFirstCharacter]"),            one("meta::pure::functions::string::tests::tolower::TestToLowerFirstCharacterEmptyString_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toLowerFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toLowerFirstCharacter]"),
            one("meta::pure::functions::string::tests::tolower::TestToLowerFirstCharacterNumber_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toLowerFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toLowerFirstCharacter]"),
            one("meta::pure::functions::string::tests::tolower::TestToLowerFirstCharacter_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toLowerFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toLowerFirstCharacter]"),

            //toUpperFirstCharacter
            one("meta::pure::functions::string::tests::toupper::TestToUpperFirstCharacterAlreadyLower_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toUpperFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toUpperFirstCharacter]"),
            one("meta::pure::functions::string::tests::toupper::TestToUpperFirstCharacterEmptyString_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toUpperFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toUpperFirstCharacter]"),
            one("meta::pure::functions::string::tests::toupper::TestToUpperFirstCharacterManyStrings_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toUpperFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toUpperFirstCharacter]"),
            one("meta::pure::functions::string::tests::toupper::TestToUpperFirstCharacterNumber_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toUpperFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toUpperFirstCharacter]"),
            one("meta::pure::functions::string::tests::toupper::TestToUpperFirstCharacter_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't resolve the builder for function 'toUpperFirstCharacter' - stack:[Function 'test::testFunction__String_MANY_' Third Pass, Applying toUpperFirstCharacter]")
            );

    public static Test suite()
    {
        return PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter);
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
