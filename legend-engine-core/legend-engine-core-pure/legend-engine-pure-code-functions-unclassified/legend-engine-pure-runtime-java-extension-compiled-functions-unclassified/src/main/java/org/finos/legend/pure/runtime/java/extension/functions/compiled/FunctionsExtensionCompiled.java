// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License",
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

package org.finos.legend.pure.runtime.java.extension.functions.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.AbstractCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.MayExecuteAlloyTest;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.MayExecuteLegendTest;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.Profile;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.cipher.Decrypt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.cipher.Encrypt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection.Get;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection.Repeat;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.DayOfWeekNumber;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.DayOfYear;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Now;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Today;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.WeekOfYear;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.hash.Hash;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.io.ReadFile;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.io.http.Http;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.MutateAdd;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.CompileValueSpecification;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.FunctionDescriptorToId;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.IsSourceReadOnly;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.IsValidFunctionDescriptor;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewAssociation;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewClass;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewEnumeration;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewLambdaFunction;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewProperty;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewQualifiedProperty;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.CurrentUserId;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.IsOptionSet;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ASCII;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Char;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Chunk;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.DecodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.DecodeUrl;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.EncodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.EncodeUrl;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.JaroWinklerSimilarity;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.LevenshteinDistance;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Matches;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.tracing.TraceSpan;

import java.util.List;

public class FunctionsExtensionCompiled extends AbstractCompiledExtension
{
    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.with(loadExtraJavaSource("org.finos.legend.pure.generated", "FunctionsGen", "org/finos/legend/pure/runtime/java/extension/functions/compiled/FunctionsGen.java"));
    }

    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(
                // Cipher
                new Decrypt(),
                new Encrypt(),

                // Collection
                new Get(),
                new Repeat(),

                //Date
                new DayOfWeekNumber(),
                new DayOfYear(),
                new Now(),
                new Today(),
                new WeekOfYear(),

                //Hash
                new Hash(),

                //IO
                new Http(),
                new ReadFile(),

                //Lang
                new MutateAdd(),

                // Meta
                new CompileValueSpecification(),
                new FunctionDescriptorToId(),
                new IsSourceReadOnly(),
                new IsValidFunctionDescriptor(),
                new NewAssociation(),
                new NewClass(),
                new NewEnumeration(),
                new NewLambdaFunction(),
                new NewProperty(),
                new NewQualifiedProperty(),

                //Runtime
                new CurrentUserId(),
                new IsOptionSet(),

                //String
                new ASCII(),
                new Char(),
                new Chunk(),
                new DecodeBase64(),
                new EncodeBase64(),
                new DecodeUrl(),
                new EncodeUrl(),
                new Matches(),
                new JaroWinklerSimilarity(),
                new LevenshteinDistance(),

                //Tracing
                new TraceSpan(),

                // LegendTests
                new MayExecuteAlloyTest(),
                new MayExecuteLegendTest(),

                //Tools
                new Profile()
        );
    }

    @Override
    public String getRelatedRepository()
    {
        return "core_functions_unclassified";
    }

    public static CompiledExtension extension()
    {
        return new FunctionsExtensionCompiled();
    }
}
