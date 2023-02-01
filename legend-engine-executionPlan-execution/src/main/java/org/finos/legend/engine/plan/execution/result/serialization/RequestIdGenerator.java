package org.finos.legend.engine.plan.execution.result.serialization;

import java.security.SecureRandom;
import java.util.Random;

public class RequestIdGenerator
{
    private static Random random = new SecureRandom();

    public static String generateId()
    {
       return randomString(8);
    }

    private static String randomString(int len)
    {
        final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
        {
            sb.append(AB.charAt(random.nextInt(AB.length())));
        }
        return sb.toString();
    }
}