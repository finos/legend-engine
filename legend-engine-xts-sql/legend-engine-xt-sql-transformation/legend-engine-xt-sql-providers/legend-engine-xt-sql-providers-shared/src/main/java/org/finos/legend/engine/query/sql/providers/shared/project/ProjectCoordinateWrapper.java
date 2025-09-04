// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.query.sql.providers.shared.project;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;
import org.finos.legend.engine.query.sql.providers.core.TableSource;

import java.util.List;
import java.util.Optional;

public class ProjectCoordinateWrapper
{

    private static final String ARG_COORDINATES = "coordinates";
    private static final String ARG_PROJECT = "project";
    private static final String ARG_WORKSPACE = "workspace";
    private static final String ARG_GROUP_WORKSPACE = "groupWorkspace";

    private final Optional<String> coordinates;
    private final Optional<String> project;
    private final Optional<String> workspace;
    private final Optional<String> groupWorkspace;

    private ProjectCoordinateWrapper(Optional<String> coordinates, Optional<String> project, Optional<String> workspace, Optional<String> groupWorkspace)
    {
        this.coordinates = coordinates;
        this.project = project;
        this.workspace = workspace;
        this.groupWorkspace = groupWorkspace;
    }

    public static ProjectCoordinateWrapper coordinates(String coordinates)
    {
        return new ProjectCoordinateWrapper(Optional.of(coordinates), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ProjectCoordinateWrapper workspace(String project, String workspace)
    {
        return new ProjectCoordinateWrapper(Optional.empty(), Optional.of(project), Optional.of(workspace), Optional.empty());
    }

    public static ProjectCoordinateWrapper groupWorkspace(String project, String groupWorkspace)
    {
        return new ProjectCoordinateWrapper(Optional.empty(), Optional.of(project), Optional.empty(), Optional.of(groupWorkspace));
    }

    public static ProjectCoordinateWrapper extractFromTableSource(TableSource source)
    {
        return extractFromTableSource(source, true);
    }

    public static ProjectCoordinateWrapper extractFromTableSource(TableSource source, boolean required)
    {
        Optional<String> coordinates = Optional.ofNullable(source.getArgumentValueAs(ARG_COORDINATES, -1, String.class, false));
        Optional<String> project = Optional.ofNullable(source.getArgumentValueAs(ARG_PROJECT, -1, String.class, false));
        Optional<String> workspace = Optional.ofNullable(source.getArgumentValueAs(ARG_WORKSPACE, -1, String.class, false));
        Optional<String> groupWorkspace = Optional.ofNullable(source.getArgumentValueAs(ARG_GROUP_WORKSPACE, -1, String.class, false));

        validateArguments(coordinates, project, workspace, groupWorkspace, required);

        return new ProjectCoordinateWrapper(coordinates, project, workspace, groupWorkspace);
    }

    public void addProjectCoordinatesAsSQLSourceArguments(List<SQLSourceArgument> keys)
    {
        coordinates.ifPresent(value -> keys.add(new SQLSourceArgument(ARG_COORDINATES, null, value)));
        project.ifPresent(value -> keys.add(new SQLSourceArgument(ARG_PROJECT, null, value)));
        workspace.ifPresent(value -> keys.add(new SQLSourceArgument(ARG_WORKSPACE, null, value)));
        groupWorkspace.ifPresent(value -> keys.add(new SQLSourceArgument(ARG_GROUP_WORKSPACE, null, value)));
    }

    private static void validateArguments(Optional<String> coordinates, Optional<String> project, Optional<String> workspace, Optional<String> groupWorkspace, boolean required)
    {
        if (coordinates.isPresent() && (project.isPresent() || workspace.isPresent() || groupWorkspace.isPresent()))
        {
            throw new IllegalArgumentException("cannot mix coordinates with project/workspace");
        }
        if (project.isPresent() && !(workspace.isPresent() || groupWorkspace.isPresent()))
        {
            throw new IllegalArgumentException("workspace/group workspace must be supplied if loading from project");
        }

        if (required && !(coordinates.isPresent() || project.isPresent()))
        {
            throw new IllegalArgumentException("coordinates or project/workspace must be supplied");
        }
    }


    public Optional<String> getCoordinates()
    {
        return coordinates;
    }

    public Optional<String> getProject()
    {
        return project;
    }

    public Optional<String> getWorkspace()
    {
        return workspace;
    }

    public Optional<String> getGroupWorkspace()
    {
        return groupWorkspace;
    }

    @Override
    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

}