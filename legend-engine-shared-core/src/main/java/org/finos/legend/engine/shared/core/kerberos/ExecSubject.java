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

import org.eclipse.collections.impl.block.function.checked.ThrowingFunction0;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

public class ExecSubject
{
    public static <T> T exec(Subject subject, ThrowingFunction0<T> proc)
    {
        try
        {
            Subject currentSubject = Subject.getSubject(AccessController.getContext());
            return subject == null || currentSubject != null ? proc.safeValue() : Subject.doAs(subject, (PrivilegedExceptionAction<T>) proc::safeValue);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
