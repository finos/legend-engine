package org.finos.legend.engine.query.graphQL.api;

import org.apache.http.cookie.Cookie;

import java.util.Date;

public class MyCookie implements Cookie
{
    private javax.servlet.http.Cookie cookie;

    public MyCookie(javax.servlet.http.Cookie cookie)
    {
        this.cookie = cookie;
    }

    @Override
    public String getName()
    {
        return this.cookie.getName();
    }

    @Override
    public String getValue()
    {
        return this.cookie.getValue();
    }

    @Override
    public String getComment()
    {
        return this.cookie.getComment();
    }

    @Override
    public String getCommentURL()
    {
        return "";
    }

    @Override
    public Date getExpiryDate()
    {
        if (this.cookie.getMaxAge() >= 0) {
            return new Date(System.currentTimeMillis() + this.cookie.getMaxAge() * 1000L);
        }
        throw new RuntimeException("");
    }

    @Override
    public boolean isPersistent()
    {
        return true;
    }

    @Override
    public String getDomain()
    {
        return "localhost";
    }

    @Override
    public String getPath()
    {
        return "/";
    }

    @Override
    public int[] getPorts()
    {
        return new int[]{};
    }

    @Override
    public boolean isSecure()
    {
        return false;
    }

    @Override
    public int getVersion()
    {
        return this.cookie.getVersion();
    }

    @Override
    public boolean isExpired(Date date)
    {
        return false;
    }
}
