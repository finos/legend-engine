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

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;

public class ExecSubject
{
    public static <T> T exec(Subject subject, ThrowingFunction0<T> proc)
    {
        Subject currentSubject = Subject.getSubject(AccessController.getContext());
        if ((subject == null) || (currentSubject != null))
        {
            try
            {
                return proc.safeValue();
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        try
        {
            return Subject.doAs(subject, (PrivilegedExceptionAction<T>) proc::safeValue);
        }
        catch (PrivilegedActionException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error)
            {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }
}
