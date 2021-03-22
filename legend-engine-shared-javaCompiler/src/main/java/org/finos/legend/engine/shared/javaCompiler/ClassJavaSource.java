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

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

class ClassJavaSource extends SimpleJavaFileObject
{
    private byte[] bytes = new byte[0];

    private ClassJavaSource(String classFilePath)
    {
        super(URI.create("memo:///" + classFilePath), Kind.CLASS);
    }

    @Override
    public InputStream openInputStream() throws IOException
    {
        return new ByteArrayInputStream(this.bytes);
    }

    @Override
    public OutputStream openOutputStream()
    {
        return new ClassJavaSource.ClassJavaSourceOutputStream(1024);
    }

    byte[] getBytes()
    {
        return this.bytes;
    }

    void setBytes(byte[] bytes)
    {
        setBytes(bytes, 0, bytes.length);
    }

    private void setBytes(byte[] bytes, int offset, int length)
    {
        byte[] newBytes = new byte[length];
        System.arraycopy(bytes, offset, newBytes, 0, length);
        this.bytes = newBytes;
    }

    String inferBinaryName()
    {
        String fileName = getName();
        int lastDot = fileName.lastIndexOf('.');
        String nameWithoutExtension = (lastDot == -1) ? fileName.substring(1) : fileName.substring(1, lastDot);
        return nameWithoutExtension.replace('/', '.');
    }

    private class ClassJavaSourceOutputStream extends ByteArrayOutputStream
    {
        private boolean closed = false;

        private ClassJavaSourceOutputStream(int size)
        {
            super(size);
        }

        @Override
        public synchronized void write(int b)
        {
            checkOpen();
            super.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len)
        {
            checkOpen();
            super.write(b, off, len);
        }

        @Override
        public synchronized void close()
        {
            if (!this.closed)
            {
                this.closed = true;

                // Write contents to byte array
                setBytes(this.buf, 0, this.count);

                // Clear data
                reset();
                this.buf = new byte[0];
            }
        }

        private void checkOpen()
        {
            if (this.closed)
            {
                throw new IllegalStateException("Stream has been closed");
            }
        }
    }

    public static ClassJavaSource fromClassName(String className)
    {
        String classFilePath = className.replace('.', '/') + Kind.CLASS.extension;
        return new ClassJavaSource(classFilePath);
    }
}
