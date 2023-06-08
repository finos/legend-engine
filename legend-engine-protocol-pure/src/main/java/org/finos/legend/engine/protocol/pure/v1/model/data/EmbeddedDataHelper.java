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

package org.finos.legend.engine.protocol.pure.v1.model.data;

import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;

import java.util.List;

public class EmbeddedDataHelper
{

    public static DataElement resolveDataElement(PureModelContextData pureModelContextData, String fullPath)
    {
        DataElement dataElement = Iterate.detect(pureModelContextData.getElementsOfType(DataElement.class), e -> (fullPath.equals(e.getPath())));
        if (dataElement == null)
        {
            throw new RuntimeException("Data Element '" + fullPath + "' not found.");
        }
        return dataElement;
    }

    public static DataElement findDataElement(List<DataElement> dataElementList, String fullPath)
    {
        DataElement dataElement = Iterate.detect(dataElementList, e -> (fullPath.equals(e.getPath())));
        if (dataElement == null)
        {
            throw new RuntimeException("Data Element '" + fullPath + "' not found.");
        }
        return dataElement;
    }

    public static EmbeddedData resolveEmbeddedData(PureModelContextData pureModelContextData, EmbeddedData embeddedData)
    {
        if (embeddedData instanceof DataElementReference)
        {
            DataElement dataElement = resolveDataElement(pureModelContextData, ((DataElementReference)embeddedData).dataElement);
            return dataElement.data;
        }
        return embeddedData;
    }

    public static EmbeddedData resolveDataElementWithList(List<DataElement> dataElementList, EmbeddedData embeddedData)
    {
        if (embeddedData instanceof DataElementReference)
        {
            DataElement dataElement = findDataElement(dataElementList, ((DataElementReference)embeddedData).dataElement);
            return dataElement.data;
        }
        return embeddedData;
    }

}
