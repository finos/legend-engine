// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api.execute;

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
        if (this.cookie.getMaxAge() >= 0)
        {
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
