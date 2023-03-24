//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.protocol.store.elasticsearch.specification.utils;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import java.io.IOException;

public class ExternalTaggedUnionKeyDeserializer extends KeyDeserializer
{
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException
    {
        int hashPos = key.indexOf('#');
        if (hashPos == -1)
        {
            throw ctxt.weirdKeyException(String.class, key, "Property name for externally tagged '" + key + "' is not in the 'type#name' format. Make sure the request has 'typed_keys' set.");
        }
        return key.substring(hashPos + 1);
    }
}
