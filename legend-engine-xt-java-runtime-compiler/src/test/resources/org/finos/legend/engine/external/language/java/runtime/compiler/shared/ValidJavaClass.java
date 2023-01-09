package org.finos.legend.engine.external.language.java.runtime.compiler.shared;

public class ValidJavaClass
{
    private final String source;

    private ValidJavaClass(String source)
    {
        this.source = source;
    }

    public String replaceNewlines()
    {
        return this.source.replaceAll("\\R", " ");
    }

    public static String succeed()
    {
        String source = "the quick\r\nbrown fox\njumps over\rthe lazy dog";
        return new ValidJavaClass(source).replaceNewlines();
    }

    public static String fail()
    {
        throw new RuntimeException("Oh no! A failure!");
    }
}