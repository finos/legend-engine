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

package org.finos.legend.engine.protocol.pure.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Iterator;

public class ProcessHelper
{
    public static <X> X processOne(JsonNode node, String name, java.lang.Class<X> _class, ObjectCodec codec) throws JsonProcessingException
    {
        JsonNode sourceInformation = node.get(name);
        return sourceInformation == null ? null : codec.treeToValue(sourceInformation, _class);
    }

    public static <X> MutableList<X> processMany(JsonNode node, String name, java.lang.Class<X> _class, ObjectCodec codec) throws JsonProcessingException
    {
        JsonNode stereotypes = node.get(name);
        if (stereotypes != null)
        {
            MutableList<X> res = Lists.mutable.empty();
            Iterator<JsonNode> it = stereotypes.elements();
            while (it.hasNext())
            {
                res.add(codec.treeToValue(it.next(), _class));
            }
            return res;
        }
        return Lists.mutable.empty();
    }
}
