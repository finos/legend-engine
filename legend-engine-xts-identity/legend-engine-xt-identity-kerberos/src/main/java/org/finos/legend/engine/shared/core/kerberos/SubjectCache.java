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

package org.finos.legend.engine.shared.core.kerberos;

import javax.security.auth.Subject;

public class SubjectCache
{
    private final long creationTime;
    private final Subject subject;
    private static final long validityPeriod = 1000L * 60 * 30; // 30mn

    public SubjectCache(Subject subject)
    {
        this.creationTime = System.currentTimeMillis();
        this.subject = subject;
    }

    public Subject getSubject()
    {
        return subject;
    }

    public boolean isValid()
    {
        return subject != null && System.currentTimeMillis() - creationTime <= validityPeriod;
    }

    public long getCreationTime()
    {
        return this.creationTime;
    }
}

