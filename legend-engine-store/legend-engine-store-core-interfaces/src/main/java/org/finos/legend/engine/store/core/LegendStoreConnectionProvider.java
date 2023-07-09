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

package org.finos.legend.engine.store.core;

import org.finos.legend.authentication.intermediationrule.IntermediationRuleProvider;

import java.util.Optional;

public interface LegendStoreConnectionProvider<T>
{
    /*
        Lifecycle method to be called when the provider is first initialized.
        Intended for initialization actions like setting up connection pools etc.
     */
    default void initialize() throws Exception
    {

    }

    /*
         Lifecycle method to be called when the provider is to be destroyed.
         Intended for tear down actions like shutting down connection pools etc.
         Note : This is deliberately not a Java AutoCloseable
    */
    default void shutdown() throws Exception
    {

    }

    /*
        Lifecycle method to acquire a connection.
        This method has to be thread safe.
     */
    T getConnection() throws Exception;

    /*
        Lifecycle method to configure a connection. E.g. configure a HttpClient as a "connection"
        This method has to be thread safe.
     */
    void configureConnection(T connection) throws Exception;

    Optional<IntermediationRuleProvider> getIntermediationRuleProvider() throws Exception;
}
