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
package org.finos.legend.engine.plan.execution.stores.service.auth;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.finos.legend.engine.connection.ConnectionSpecification;

import java.net.URI;
import java.util.List;

public class ServiceStoreConnectionSpecification extends ConnectionSpecification
{

    public URI uri;
    public String httpMethod;
    public List<Header> headers;
    public StringEntity requestBodyDescription;
    public String mimeType;

    public ServiceStoreConnectionSpecification(URI uri, String httpMethod, List<Header> headers, StringEntity requestBodyDescription, String mimeType)
    {
        this.uri = uri;
        this.httpMethod = httpMethod;
        this.headers = headers;
        this.requestBodyDescription = requestBodyDescription;
        this.mimeType = mimeType;
    }
}
