// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers.shared.utils;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.IdentifiedConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.StoreConnections;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.PackageableElementPtr;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class SQLProviderUtils
{

    public static <T extends PackageableElement> T extractElement(String argumentName, Class<T> type, PureModelContextData pmcd, Predicate<T> predicate)
    {
        return extractElement(argumentName, pmcd.getElementsOfType(type), predicate);
    }

    public static <T> T extractElement(String argumentName, List<T> list, Predicate<T> predicate)
    {
        MutableList<T> elements = ListIterate.select(list,
                element -> predicate.test(element));

        if (elements.isEmpty())
        {
            throw new IllegalArgumentException("No element found for '" + argumentName + "'");
        }

        if (elements.size() > 1)
        {
            throw new IllegalArgumentException("Multiple elements found for '" + argumentName + "'");
        }

        return elements.getOnly();
    }

    public static Lambda tableToTDS(String databasePath, String schemaName, String tableName)
    {
        PackageableElementPtr databasePtr = new PackageableElementPtr();
        databasePtr.fullPath = databasePath;

        AppliedFunction tableReferenceFunc = new AppliedFunction();
        tableReferenceFunc.function = "tableReference";
        tableReferenceFunc.fControl = "tableReference_Database_1__String_1__String_1__Table_1_";
        tableReferenceFunc.parameters = FastList.newListWith(databasePtr, new CString(schemaName), new CString(tableName));

        AppliedFunction tableToTdsFunc = new AppliedFunction();
        tableToTdsFunc.function = "tableToTDS";
        tableToTdsFunc.fControl = "tableToTDS_Table_1__TableTDS_1_";
        tableToTdsFunc.parameters = Collections.singletonList(tableReferenceFunc);

        Lambda lambda = new Lambda();
        lambda.body = Collections.singletonList(tableToTdsFunc);

        return lambda;
    }

    public static boolean equalsEscaped(String value, String toMatch)
    {
        return value.equals(toMatch) || value.equals("\"" + toMatch + "\"");
    }

    public static EngineRuntime createRuntime(String connection, String store)
    {
        ConnectionPointer connectionPtr = new ConnectionPointer();
        connectionPtr.connection = connection;

        PackageableElementPointer storePointer = new PackageableElementPointer();
        storePointer.path = store;
        storePointer.type = PackageableElementType.STORE;

        IdentifiedConnection identifiedConnection = new IdentifiedConnection();
        identifiedConnection.id = "connection1";
        identifiedConnection.connection = connectionPtr;

        StoreConnections storeConnection = new StoreConnections();
        storeConnection.store = storePointer;
        storeConnection.storeConnections = FastList.newListWith(identifiedConnection);

        EngineRuntime runtime = new EngineRuntime();
        runtime.connections = FastList.newListWith(storeConnection);

        return runtime;
    }
}