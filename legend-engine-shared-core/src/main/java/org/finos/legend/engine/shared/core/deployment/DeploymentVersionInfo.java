package org.finos.legend.engine.shared.core.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentVersionInfo {
    @JsonProperty(value="git.branch", required = true)
    public String branch;

    @JsonProperty(value="git.build.time", required = true)
    public String buildTime;

    @JsonProperty(value="git.build.version", required = true)
    public String buildVersion;

    @JsonProperty(value="git.closest.tag.name", required = true)
    public String buildTag;

    @JsonProperty(value="git.commit.id", required = true)
    public String commitId;

    @JsonProperty(value="git.commit.id.abbrev")
    public String commitIdAbbreviated;

    @JsonProperty(value="git.commit.time")
    public String commitTime;

    @JsonProperty(value="git.total.commit.count")
    public String commitCount;

    public String infoToJson()
    {
        try{
            return new ObjectMapper().writeValueAsString(this);
        }
        catch (Exception e)
        {
            return "{}" ;
        }
    }
}
