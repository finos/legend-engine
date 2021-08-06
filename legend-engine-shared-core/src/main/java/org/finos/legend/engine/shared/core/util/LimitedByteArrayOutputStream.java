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

package org.finos.legend.engine.shared.core.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

public class LimitedByteArrayOutputStream extends ByteArrayOutputStream
{
    private final int maxCapacity;
    private int received = 0;

    public LimitedByteArrayOutputStream(int maxCapacity)
    {
        super(maxCapacity);
        this.maxCapacity = maxCapacity;
    }

    @Override
    public synchronized void write(int b)
    {
        if (received < maxCapacity)
        {
            super.write(b);
        }
        received++;
    }

    @Override
    public synchronized void write(byte[] b, int off, int len)
    {
        if (received < maxCapacity)
        {
            int l = Math.min(len, maxCapacity - received);
            super.write(b, off, l);
        }
        received += len;
    }

    @Override
    public synchronized String toString()
    {
        return (received <= maxCapacity)
               ? super.toString()
               : super.toString() + "...";
    }

    @Override
    public synchronized String toString(String charsetName) throws UnsupportedEncodingException
    {
        return (received <= maxCapacity)
               ? super.toString(charsetName)
               : super.toString(charsetName) + "...";
    }
}
