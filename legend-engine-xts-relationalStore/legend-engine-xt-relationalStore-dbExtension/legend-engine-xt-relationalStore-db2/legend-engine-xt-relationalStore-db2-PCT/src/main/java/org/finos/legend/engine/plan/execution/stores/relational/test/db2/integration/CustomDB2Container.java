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

package org.finos.legend.engine.plan.execution.stores.relational.test.db2.integration;

import com.github.dockerjava.api.model.HostConfig;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.utility.DockerImageName;

public class CustomDB2Container extends Db2Container
{
    private static final String DEFAULT_REGISTRY = "docker.io";
    private static final String IMAGE = "/ibmcom/db2:11.5.8.0";
    private static final String CANONICAL_IMAGE_NAME = "ibmcom/db2";
    // DB2 instance creation rejects host names that are not valid identifiers, such as the hex container id Docker assigns by default
    private static final String HOST_NAME = "db2server";
    // DB2 keeps most of its working memory in /dev/shm; the 64m default stalls instance configuration
    private static final long SHM_SIZE = 2L * 1024 * 1024 * 1024;

    public CustomDB2Container()
    {
        super(DockerImageName.parse(System.getProperty("legend.engine.testcontainer.registry", DEFAULT_REGISTRY) + IMAGE).asCompatibleSubstituteFor(CANONICAL_IMAGE_NAME));
        this.acceptLicense();
        // the image remounts /database and starts the instance at boot, both of which need full privileges
        this.withPrivilegedMode(true);
        this.withCreateContainerCmdModifier(cmd ->
        {
            // a host UTS namespace, which testing defaulted to, rejects a container host name outright
            cmd.withHostConfig((cmd.getHostConfig() == null ? new HostConfig() : cmd.getHostConfig()).withUtSMode("private").withIpcMode("private").withShmSize(SHM_SIZE));
            cmd.withHostName(HOST_NAME);
        });
    }
}