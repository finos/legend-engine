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

package org.finos.legend.engine.application.query.model;

import java.util.List;

public class DataSpace
// NOTE: extends PackageableElement
{
    // public String label; // should we include this as a nicely-formatted name for the data space?
    public String description;
    public String groupId;
    public String artifactId;
    public String versionId;
    public String mapping;
    public String runtime;
    public List<String> diagrams;
    public String supportEmail;
}