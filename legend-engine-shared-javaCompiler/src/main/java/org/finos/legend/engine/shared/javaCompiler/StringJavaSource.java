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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.tools.SimpleJavaFileObject;

public abstract class StringJavaSource extends SimpleJavaFileObject
{
    private static final Pattern PACKAGE_DECLARATION_PATTERN = Pattern.compile("^\\h*package\\h+[^;\\s]+\\h*;", Pattern.MULTILINE);

    private StringJavaSource(String packageName, String name)
    {
        super(URI.create("string:///" + packageName.replace('.', '/') + (packageName.endsWith(".") ? "" : "/") + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors)
    {
        return getCode();
    }

    public abstract String getCode();

    public abstract int size();

    public static StringJavaSource newStringJavaSource(String packageName, String name, String code)
    {
        return newStringJavaSource(packageName, name, code, true);
    }

    public static StringJavaSource newStringJavaSource(String packageName, String name, String code, boolean possiblyCompress)
    {
        String codeString = possiblyAddPackage(code, packageName);
        if (!possiblyCompress)
        {
            return new SimpleStringJavaSource(packageName, name, codeString);
        }

        byte[] codeBytes = codeString.getBytes(StandardCharsets.UTF_8);
        byte[] compressedCodeBytes = possiblyCompressCode(codeBytes);
        return (compressedCodeBytes == null) ? new SimpleStringJavaSource(packageName, name, codeString) : new CompressedStringJavaSource(packageName, name, codeBytes.length, compressedCodeBytes);
    }

    // package private for testing
    static boolean hasPackageDeclaration(String code)
    {
        return PACKAGE_DECLARATION_PATTERN.matcher(code).find();
    }

    private static String possiblyAddPackage(String code, String packageName)
    {
        return hasPackageDeclaration(code) ? code : ("package " + packageName + ";\n" + code);
    }

    private static byte[] possiblyCompressCode(byte[] codeBytes)
    {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try
        {
            byte[] outBytes = new byte[codeBytes.length];
            deflater.setInput(codeBytes);
            int compressedSize = deflater.deflate(outBytes, 0, outBytes.length, Deflater.FULL_FLUSH);
            if (compressedSize == 0)
            {
                throw new RuntimeException("Error compressing string: " + new String(codeBytes, StandardCharsets.UTF_8));
            }
            if (compressedSize >= codeBytes.length)
            {
                // if compression doesn't make it smaller, don't compress
                return null;
            }
            return Arrays.copyOfRange(outBytes, 0, compressedSize);
        }
        finally
        {
            deflater.end();
        }
    }

    private static String decompressToString(int originalSize, byte[] compressedCode)
    {
        return new String(decompressToBytes(originalSize, compressedCode), StandardCharsets.UTF_8);
    }

    private static byte[] decompressToBytes(int originalSize, byte[] compressedCode)
    {
        Inflater inflater = new Inflater(false);
        try
        {
            inflater.setInput(compressedCode);
            byte[] codeBytes = new byte[originalSize];
            int totalUncompressed = 0;
            int uncompressed;
            while ((uncompressed = inflater.inflate(codeBytes, totalUncompressed, originalSize - totalUncompressed)) != 0)
            {
                totalUncompressed += uncompressed;
            }
            if (totalUncompressed < originalSize)
            {
                throw new RuntimeException("Error decompressing code: expected " + originalSize + " bytes, got " + totalUncompressed + " bytes");
            }
            return codeBytes;
        }
        catch (DataFormatException e)
        {
            throw new RuntimeException("Error decompressing code", e);
        }
        finally
        {
            inflater.end();
        }
    }

    private static class SimpleStringJavaSource extends StringJavaSource
    {
        private final String code;

        private SimpleStringJavaSource(String packageName, String name, String code)
        {
            super(packageName, name);
            this.code = code;
        }

        @Override
        public String getCode()
        {
            return this.code;
        }

        @Override
        public int size()
        {
            return this.code.length();
        }

        @Override
        public InputStream openInputStream()
        {
            return new ByteArrayInputStream(this.code.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static class CompressedStringJavaSource extends StringJavaSource
    {
        private final int originalLength;
        private final byte[] compressedCode;

        private CompressedStringJavaSource(String packageName, String name, int originalLength, byte[] compressedCode)
        {
            super(packageName, name);
            this.originalLength = originalLength;
            this.compressedCode = compressedCode;
        }

        @Override
        public String getCode()
        {
            return decompressToString(this.originalLength, this.compressedCode);
        }

        @Override
        public int size()
        {
            return this.compressedCode.length;
        }

        @Override
        public InputStream openInputStream()
        {
            return (this.originalLength < 1024) ?
                    new ByteArrayInputStream(decompressToBytes(this.originalLength, this.compressedCode)) :
                    new InflaterInputStream(new ByteArrayInputStream(this.compressedCode));
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors)
        {
            return (this.originalLength < 1024) ?
                    new StringReader(getCode()) :
                    new InputStreamReader(new InflaterInputStream(new ByteArrayInputStream(this.compressedCode)), StandardCharsets.UTF_8);
        }
    }
}
