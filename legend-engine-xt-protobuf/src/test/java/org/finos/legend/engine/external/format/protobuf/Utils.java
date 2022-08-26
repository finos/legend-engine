// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.protobuf;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.util.Scanner;
import org.finos.legend.engine.external.format.protobuf.tests.TestProtobufFileGeneration;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

public class Utils
{
    public static PureModelContextData getProtocol(String fileName) throws JsonProcessingException
    {
        String jsonString = getResourceAsString(fileName);
        return ObjectMapperFactory
            .getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(jsonString, PureModelContextData.class);
    }

    private static String getResourceAsString(String fileName)
    {
        InputStream inputStream = TestProtobufFileGeneration.class.getResourceAsStream(fileName);
        Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
