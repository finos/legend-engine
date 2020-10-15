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

package org.finos.engine.shared.javaCompiler;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public abstract class StringJavaSource extends SimpleJavaFileObject
{
    private static final Pattern PACKAGE_DECLARATION_PATTERN = Pattern.compile("^\\h*package\\h+[^;\\s]+\\h*;\\h*", Pattern.MULTILINE);

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
            return new StringJavaSource.SimpleStringJavaSource(packageName, name, codeString);
        }

        byte[] codeBytes = codeString.getBytes(StandardCharsets.UTF_8);
        byte[] compressedCodeBytes = possiblyCompressCode(codeBytes);
        return (compressedCodeBytes == null) ? new SimpleStringJavaSource(packageName, name, codeString) : new StringJavaSource.CompressedStringJavaSource(packageName, name, codeBytes.length, compressedCodeBytes);
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
        deflater.setInput(codeBytes);
        byte[] outBytes = new byte[codeBytes.length];
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

    private static String decompressCode(int originalSize, byte[] compressedCode)
    {
        Inflater inflater = new Inflater(false);
        inflater.setInput(compressedCode);
        byte[] codeBytes = new byte[originalSize];
        int totalUncompressed = 0;
        try
        {
            for (int uncompressed = inflater.inflate(codeBytes, 0, originalSize); uncompressed != 0; uncompressed = inflater.inflate(codeBytes, totalUncompressed, originalSize - totalUncompressed))
            {
                totalUncompressed += uncompressed;
            }
            if (totalUncompressed < originalSize)
            {
                throw new RuntimeException("Error decompressing code: expected " + originalSize + " bytes, got " + totalUncompressed + " bytes");
            }
        }
        catch (DataFormatException e)
        {
            throw new RuntimeException("Error decompressing code", e);
        }
        return new String(codeBytes, StandardCharsets.UTF_8);
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
            return decompressCode(this.originalLength, this.compressedCode);
        }

        @Override
        public int size()
        {
            return this.compressedCode.length;
        }

        @Override
        public InputStream openInputStream()
        {
            return new ByteArrayInputStream(this.getCode().getBytes(StandardCharsets.UTF_8));
        }
    }
}

