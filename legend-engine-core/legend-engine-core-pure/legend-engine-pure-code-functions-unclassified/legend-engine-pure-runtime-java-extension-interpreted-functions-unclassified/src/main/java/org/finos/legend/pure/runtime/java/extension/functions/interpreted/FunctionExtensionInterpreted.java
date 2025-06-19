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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted;

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.AlloyTest;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.LegendTest;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.Profile;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.cipher.Cipher;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.cipher.Decipher;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Get;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.collection.Repeat;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.DayOfWeekNumber;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.DayOfYear;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.Now;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.Today;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date.WeekOfYear;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.hash.Hash;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.io.ReadFile;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.io.http.Http;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.lang.MutateAdd;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.CompileValueSpecification;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.FunctionDescriptorToId;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.IsSourceReadOnly;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.IsValidFunctionDescriptor;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewAssociation;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewClass;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewEnumeration;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewLambdaFunction;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewProperty;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta.NewQualifiedProperty;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.runtime.CurrentUserId;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.runtime.IsOptionSet;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.ASCII;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.Char;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.Chunk;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.DecodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.DecodeUrl;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.EncodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.EncodeUrl;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.JaroWinklerSimilarity;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.LevenshteinDistance;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string.Matches;
import org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.tracing.TraceSpan;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;

public class FunctionExtensionInterpreted extends BaseInterpretedExtension
{
    public FunctionExtensionInterpreted()
    {
        super(//Cipher
                Tuples.pair("encrypt_String_1__String_1__String_1_", Cipher::new),
                Tuples.pair("encrypt_Number_1__String_1__String_1_", Cipher::new),
                Tuples.pair("encrypt_Boolean_1__String_1__String_1_", Cipher::new),
                Tuples.pair("decrypt_String_1__String_1__String_1_", Decipher::new),

                //Collection
                Tuples.pair("get_T_MANY__String_1__T_$0_1$_", Get::new),
                Tuples.pair("repeat_T_1__Integer_1__T_MANY_", Repeat::new),

                //Date
                Tuples.pair("dayOfWeekNumber_Date_1__Integer_1_", DayOfWeekNumber::new),
                Tuples.pair("dayOfYear_Date_1__Integer_1_", DayOfYear::new),
                Tuples.pair("now__DateTime_1_", Now::new),
                Tuples.pair("today__StrictDate_1_", Today::new),
                Tuples.pair("weekOfYear_Date_1__Integer_1_", WeekOfYear::new),

                //Hash
                Tuples.pair("hash_String_1__HashType_1__String_1_", Hash::new),

                //IO
                Tuples.pair("executeHTTPRaw_URL_1__HTTPMethod_1__String_$0_1$__String_$0_1$__HTTPResponse_1_", Http::new),
                Tuples.pair("readFile_String_1__String_$0_1$__String_$0_1$_", ReadFile::new),

                //Lang
                Tuples.pair("mutateAdd_T_1__String_1__Any_MANY__T_1_", MutateAdd::new),

                //Meta
                Tuples.pair("compileValueSpecification_String_m__CompilationResult_m_", CompileValueSpecification::new),
                Tuples.pair("functionDescriptorToId_String_1__String_1_", FunctionDescriptorToId::new),
                Tuples.pair("isSourceReadOnly_String_1__Boolean_1_", IsSourceReadOnly::new),
                Tuples.pair("isValidFunctionDescriptor_String_1__Boolean_1_", IsValidFunctionDescriptor::new),
                Tuples.pair("newAssociation_String_1__Property_1__Property_1__Association_1_", NewAssociation::new),
                Tuples.pair("newClass_String_1__Class_1_", NewClass::new),
                Tuples.pair("newEnumeration_String_1__String_MANY__Enumeration_1_", NewEnumeration::new),
                Tuples.pair("newLambdaFunction_FunctionType_1__LambdaFunction_1_", NewLambdaFunction::new),
                Tuples.pair("newProperty_String_1__GenericType_1__GenericType_1__Multiplicity_1__Property_1_", NewProperty::new),
                Tuples.pair("newQualifiedProperty_String_1__GenericType_1__GenericType_1__Multiplicity_1__VariableExpression_MANY__QualifiedProperty_1_", NewQualifiedProperty::new),


                //Runtime
                Tuples.pair("currentUserId__String_1_", CurrentUserId::new),
                Tuples.pair("isOptionSet_String_1__Boolean_1_", IsOptionSet::new),


                //String
                Tuples.pair("ascii_String_1__Integer_1_", ASCII::new),
                Tuples.pair("char_Integer_1__String_1_", Char::new),
                Tuples.pair("chunk_String_1__Integer_1__String_MANY_", Chunk::new),
                Tuples.pair("encodeBase64_String_1__String_1_", EncodeBase64::new),
                Tuples.pair("decodeBase64_String_1__String_1_", DecodeBase64::new),
                Tuples.pair("encodeUrl_String_1__String_1__String_1_", EncodeUrl::new),
                Tuples.pair("decodeUrl_String_1__String_1__String_1_", DecodeUrl::new),
                Tuples.pair("matches_String_1__String_1__Boolean_1_", Matches::new),
                Tuples.pair("jaroWinklerSimilarity_String_1__String_1__Float_1_", JaroWinklerSimilarity::new),
                Tuples.pair("levenshteinDistance_String_1__String_1__Integer_1_", LevenshteinDistance::new),

                //Tracing
                Tuples.pair("traceSpan_Function_1__String_1__V_m_", TraceSpan::new),
                Tuples.pair("traceSpan_Function_1__String_1__Function_1__V_m_", TraceSpan::new),
                Tuples.pair("traceSpan_Function_1__String_1__Function_1__Boolean_1__V_m_", TraceSpan::new),

                //LegendTests
                Tuples.pair("mayExecuteLegendTest_Function_1__Function_1__X_k_", LegendTest::new),
                Tuples.pair("mayExecuteAlloyTest_Function_1__Function_1__X_k_", AlloyTest::new),

                //Tools
                Tuples.pair("profile_T_m__Boolean_1__ProfileResult_1_", Profile::new)

        );
    }

    public static FunctionExtensionInterpreted extension()
    {
        return new FunctionExtensionInterpreted();
    }
}
