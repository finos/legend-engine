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

package org.finos.legend.engine.shared.core.identity.credential;

import org.finos.legend.engine.shared.core.identity.Credential;

/*
    This is a no-op credential that is used to indicate the use of GCP credentials that are natively available in the environment.

    See the following docs for details :
    1/ https://cloud.google.com/bigquery/docs/authentication
    2/ https://cloud.google.com/docs/authentication/production
 */
public class GCPApplicationDefaultCredential implements Credential
{
}