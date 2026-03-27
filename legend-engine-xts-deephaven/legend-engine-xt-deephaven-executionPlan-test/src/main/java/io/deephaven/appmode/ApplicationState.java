// Copyright 2026 Goldman Sachs
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

// This is a stub for Deephaven's ApplicationState class (appmode)
// At runtime, inside the Deephaven server, the real class is on the classpath.

package io.deephaven.appmode;

public class ApplicationState
{
    public interface Factory
    {
        ApplicationState create(Listener listener);
    }

    public interface Listener
    {
    }

    public ApplicationState(Listener listener, String id, String name)
    {
        throw new UnsupportedOperationException("Stub only");
    }

    public <T> void setField(String name, T value)
    {
        throw new UnsupportedOperationException("Stub only");
    }
}

