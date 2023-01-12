package org.finos.legend.engine.external.language.java.runtime.compiler.shared;

public class InvalidJavaClass
{
    private final String name;

    public InvalidJavaClass(String name)
    {
        this.name = name;
    }

    public void setName(Integer newName)
    {
        this.name = newName;
    }

    public String getName()
    {
        return this.name;
    }

    public static void main(String... args) throws Exception
    {
        for (String arg : args)
        {
            System.out.println(new InvalidJavaClass(arg).getName());
        }
    }
}