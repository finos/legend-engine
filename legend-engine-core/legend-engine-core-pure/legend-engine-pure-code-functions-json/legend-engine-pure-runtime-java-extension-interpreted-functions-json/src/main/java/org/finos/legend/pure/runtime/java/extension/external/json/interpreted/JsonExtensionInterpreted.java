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

package org.finos.legend.pure.runtime.java.extension.external.json.interpreted;

import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives.Escape;
import org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives.FromJson;
import org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives.FromJsonDeprecated;
import org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives.JsonStringsEqual;
import org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives.ParseJSON;
import org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives.ToJson;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;

public class JsonExtensionInterpreted extends BaseInterpretedExtension
{
    public JsonExtensionInterpreted()
    {
        super(Tuples.pair("escape_String_1__String_1_", Escape::new),
                Tuples.pair("parseJSON_String_1__JSONElement_1_", ParseJSON::new),
                Tuples.pair("equalJsonStrings_String_1__String_1__Boolean_1_", JsonStringsEqual::new),
                Tuples.pair("fromJson_String_1__Class_1__JSONDeserializationConfig_1__T_1_", FromJson::new),
                Tuples.pair("fromJsonDeprecated_String_1__Class_1__JSONDeserializationConfig_1__T_1_", FromJsonDeprecated::new),
                Tuples.pair("toJsonBeta_Any_MANY__JSONSerializationConfig_1__String_1_", ToJson::new)
        );
    }

    public static InterpretedExtension extension()
    {
        return new JsonExtensionInterpreted();
    }
}
