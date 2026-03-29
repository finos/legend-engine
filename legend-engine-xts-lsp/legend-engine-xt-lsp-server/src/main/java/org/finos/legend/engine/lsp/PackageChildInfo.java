// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.lsp;

/**
 * DTO for a child of a Pure package, used by the tree view.
 * Must have public getters/setters for JSON-RPC serialization.
 */
public class PackageChildInfo
{
    private String name;
    private String qualifiedPath;
    private String kind;
    private boolean isPackage;
    private int childCount;
    private String uri;
    private Integer line;

    public PackageChildInfo()
    {
    }

    PackageChildInfo(String name, String qualifiedPath, String kind,
                     boolean isPackage, int childCount, String uri, Integer line)
    {
        this.name = name;
        this.qualifiedPath = qualifiedPath;
        this.kind = kind;
        this.isPackage = isPackage;
        this.childCount = childCount;
        this.uri = uri;
        this.line = line;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getQualifiedPath()
    {
        return qualifiedPath;
    }

    public void setQualifiedPath(String qualifiedPath)
    {
        this.qualifiedPath = qualifiedPath;
    }

    public String getKind()
    {
        return kind;
    }

    public void setKind(String kind)
    {
        this.kind = kind;
    }

    public boolean getIsPackage()
    {
        return isPackage;
    }

    public void setIsPackage(boolean isPackage)
    {
        this.isPackage = isPackage;
    }

    public int getChildCount()
    {
        return childCount;
    }

    public void setChildCount(int childCount)
    {
        this.childCount = childCount;
    }

    public String getUri()
    {
        return uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    public Integer getLine()
    {
        return line;
    }

    public void setLine(Integer line)
    {
        this.line = line;
    }
}
