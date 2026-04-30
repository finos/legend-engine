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

package org.finos.legend.engine.shared.mongo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StoredAuditInformation
{
    private Integer version;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdAt;
    private String createdBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime updatedAt;
    private String updatedBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime deletedAt;
    private String deletedBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime validUntil;

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getCreatedBy()
    {
        return createdBy;
    }

    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy()
    {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy)
    {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt)
    {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt()
    {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt)
    {
        this.deletedAt = deletedAt;
    }

    public String getDeletedBy()
    {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy)
    {
        this.deletedBy = deletedBy;
    }

    public LocalDateTime getValidUntil()
    {
        return validUntil;
    }


    public void setValidUntil(LocalDateTime validUntil)
    {
        this.validUntil = validUntil;
    }

    public static StoredAuditInformationBuilder builder()
    {
        return new StoredAuditInformationBuilder();
    }

    public static class StoredAuditInformationBuilder
    {
        private Integer version;
        private String createdBy;
        private String updatedBy;
        private String deletedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private LocalDateTime validUntil;

        public StoredAuditInformationBuilder withVersion(Integer version)
        {
            this.version = version;
            return this;
        }

        public StoredAuditInformationBuilder withCreatedBy(String createdBy)
        {
            this.createdBy = createdBy;
            return this;
        }

        public StoredAuditInformationBuilder withUpdatedBy(String updatedBy)
        {
            this.updatedBy = updatedBy;
            return this;
        }

        public StoredAuditInformationBuilder withDeletedBy(String deletedBy)
        {
            this.deletedBy = deletedBy;
            return this;
        }

        public StoredAuditInformationBuilder withCreatedAt(LocalDateTime createdAt)
        {
            this.createdAt = createdAt;
            return this;
        }

        public StoredAuditInformationBuilder withUpdatedAt(LocalDateTime updatedAt)
        {
            this.updatedAt = updatedAt;
            return this;
        }

        public StoredAuditInformationBuilder withDeletedAt(LocalDateTime deletedAt)
        {
            this.deletedAt = deletedAt;
            return this;
        }

        public StoredAuditInformationBuilder withValidUntil(LocalDateTime validUntil)
        {
            this.validUntil = validUntil;
            return this;
        }

        public StoredAuditInformationBuilder from(StoredAuditInformation source)
        {
            this.version = source.getVersion();
            this.createdAt = source.getCreatedAt();
            this.createdBy = source.getCreatedBy();
            this.updatedAt = source.getUpdatedAt();
            this.updatedBy = source.getUpdatedBy();
            this.validUntil = source.getValidUntil();
            this.deletedAt = source.getDeletedAt();
            this.deletedBy = source.getDeletedBy();
            return this;
        }

        public StoredAuditInformation build()
        {
            StoredAuditInformation audit = new StoredAuditInformation();
            audit.setVersion(this.version);
            audit.setCreatedAt(this.createdAt);
            audit.setCreatedBy(this.createdBy);
            audit.setUpdatedAt(this.updatedAt);
            audit.setUpdatedBy(this.updatedBy);
            audit.setValidUntil(this.validUntil);
            audit.setDeletedAt(this.deletedAt);
            audit.setDeletedBy(this.deletedBy);
            return audit;
        }
    }
}
