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

package org.finos.legend.engine.shared.javaCompiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Base64;
import javax.tools.SimpleJavaFileObject;

class ClassJavaSource extends SimpleJavaFileObject
{
    private byte[] bytes = new byte[0];

    private ClassJavaSource(String classFilePath)
    {
        super(URI.create("memo:///" + classFilePath), Kind.CLASS);
    }

    @Override
    public InputStream openInputStream()
    {
        return new ByteArrayInputStream(this.bytes);
    }

    @Override
    public OutputStream openOutputStream()
    {
        return openOutputStream(1024);
    }

    OutputStream openOutputStream(int size)
    {
        return new ByteArrayOutputStream(size)
        {
            @Override
            public synchronized void close()
            {
                setBytes_internal(toByteArray());
            }
        };
    }

    byte[] getBytes()
    {
        return this.bytes;
    }

    String getEncodedBytes()
    {
        return Base64.getEncoder().encodeToString(getBytes());
    }

    void setBytes(byte[] bytes)
    {
        setBytes(bytes, 0, bytes.length);
    }

    void setEncodedBytes(String encodedBytes)
    {
        setBytes_internal(Base64.getDecoder().decode(encodedBytes));
    }

    private void setBytes(byte[] bytes, int offset, int length)
    {
        byte[] newBytes = new byte[length];
        System.arraycopy(bytes, offset, newBytes, 0, length);
        setBytes_internal(newBytes);
    }

    private void setBytes_internal(byte[] bytes)
    {
        this.bytes = bytes;
    }

    String inferBinaryName()
    {
        String fileName = getName();
        int lastDot = fileName.lastIndexOf('.');
        String nameWithoutExtension = (lastDot == -1) ? fileName.substring(1) : fileName.substring(1, lastDot);
        return nameWithoutExtension.replace('/', '.');
    }

    public static ClassJavaSource fromClassName(String className)
    {
        String classFilePath = className.replace('.', '/') + Kind.CLASS.extension;
        return new ClassJavaSource(classFilePath);
    }
}
