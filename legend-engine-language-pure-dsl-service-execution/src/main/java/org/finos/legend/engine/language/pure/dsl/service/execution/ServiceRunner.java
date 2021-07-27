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

package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.finos.legend.engine.plan.execution.PlanExecutorInfo;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCrossAssociationKeys;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/* Work in progress, do not use */

public interface ServiceRunner
{
    /**
     * Get the canonical class name of ServiceRunner instance
     *
     * @return canonical class name
     */
    default String getCanonicalClassName()
    {
        return this.getClass().getCanonicalName();
    }

    /**
     * Get the fully qualified service path (e.g. pack1::pack2::MyService)
     *
     * @return fully qualified service path
     */
    String getServicePath();

    /**
     * Get plan executor information (connection pool details etc.)
     *
     * @return plan executor information
     */
    PlanExecutorInfo getPlanExecutorInfo();

    /**
     * Run the service with passed in arguments
     *
     * @param args parameter list (can be empty)
     * @return serialized service execution result
     */
    default String run(List<Object> args)
    {
        return this.run(args, null);
    }

    /**
     * Run the service with passed in arguments and identity to be used for privileged actions
     *
     * @param args parameter list (can be empty)
     * @param identity identity to be used when performing any privileged actions
     * @return serialized service execution result
     */
    default String run(List<Object> args, Identity identity)
    {
        return this.run(args, identity, OperationalContext.newInstance());
    }

    /**
     * Run the service with passed in arguments, identity to be used for privileged actions and operation context
     *
     * @param args parameter list (can be empty)
     * @param identity identity to be used when performing any privileged actions
     * @param operationalContext operational context holding client side caches etc.
     * @return serialized service execution result
     */
    default String run(List<Object> args, Identity identity, OperationalContext operationalContext)
    {
        return this.run(args, identity, operationalContext, SerializationFormat.PURE);
    }

    /**
     * Run the service with passed in arguments, identity to be used for privileged actions, operation context and
     * serialize the result in the provided format
     *
     * @param args parameter list (can be empty)
     * @param identity identity to be used when performing any privileged actions
     * @param operationalContext operational context holding client side caches etc.
     * @param serializationFormat format in which result needs to be serialized
     * @return serialized service execution result
     */
    default String run(List<Object> args, Identity identity, OperationalContext operationalContext, SerializationFormat serializationFormat)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.run(args, identity, operationalContext, serializationFormat, outputStream);
        return outputStream.toString();
    }

    /**
     * Run the service with passed in arguments, identity to be used for privileged actions, operation context
     * and write the serialized result to the passed output stream
     *
     * @param args parameter list (can be empty)
     * @param identity identity to be used when performing any privileged actions
     * @param operationalContext operational context holding client side caches etc.
     * @param serializationFormat format in which result needs to be serialized
     * @param outputStream output stream to which serialized result needs to be written
     */
    void run(List<Object> args, Identity identity, OperationalContext operationalContext, SerializationFormat serializationFormat, OutputStream outputStream);

    /**
     * Get the list of valid graph fetch cross association keys for the service
     *
     * @return valid graph fetch cross association keys list
     */
    default List<GraphFetchCrossAssociationKeys> getGraphFetchCrossAssociationKeys()
    {
        return Collections.emptyList();
    }
}
