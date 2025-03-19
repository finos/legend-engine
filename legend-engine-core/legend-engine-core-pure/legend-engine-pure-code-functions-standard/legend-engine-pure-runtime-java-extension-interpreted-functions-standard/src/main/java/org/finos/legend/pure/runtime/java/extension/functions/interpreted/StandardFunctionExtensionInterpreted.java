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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted;

import org.finos.legend.pure.runtime.java.extension.functions.standard.shared.natives.StandardSharedFunctionsPlaceholder;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;

public class StandardFunctionExtensionInterpreted extends BaseInterpretedExtension
{
    public StandardFunctionExtensionInterpreted()
    {
    }

    public static StandardFunctionExtensionInterpreted extension()
    {
        return new StandardFunctionExtensionInterpreted();
    }

    // placeholder to preserve pom structure for new module structure that enables native functions in core_functions_standard
    public static void StandardFunctionExtensionInterpretedPlaceholder()
    {
        System.out.println(StandardSharedFunctionsPlaceholder.StandardSharedFunctionPlaceholder());
    }
}
