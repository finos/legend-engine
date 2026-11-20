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

package org.finos.legend.engine.language.pure.dsl.generation.project;

import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;

import java.util.ArrayList;
import java.util.List;

/**
 * What {@link ProjectGenerationRunner} produced, one entry per step of the build: compiling the
 * project, its model generations, its file generations and its artifact generations. Each step
 * has a {@link Status}, and returns what it finished before it failed.
 */
public class ProjectGenerationResult
{
    /**
     * {@link Status#FAILED} when any step failed, {@link Status#SUCCEEDED} otherwise.
     */
    public Status status;
    public StepResult compilation;
    public ModelGenerationStepResult modelGeneration;
    public FileGenerationStepResult fileGeneration;
    public ArtifactGenerationStepResult artifactGeneration;

    public enum Status
    {
        SUCCEEDED,
        FAILED,
        /**
         * The request turned the step off.
         */
        DISABLED,
        /**
         * An earlier step failed, so this one did not run, as the build stops at its first failure.
         */
        SKIPPED
    }

    public static class StepResult
    {
        public Status status;
        /**
         * How long the step took; absent when it did not run.
         */
        public Long durationMs;
        public List<Warning> warnings = new ArrayList<>();
        public Failure failure;
    }

    public static class ModelGenerationStepResult extends StepResult
    {
        /**
         * Every element generated, once.
         */
        public PureModelContextData generatedModel;
        /**
         * Every generation node of the generation specification, in order.
         */
        public List<Stage> stages = new ArrayList<>();
    }

    public static class Stage
    {
        public String id;
        public Status status;
        public List<Generation> generations = new ArrayList<>();
    }

    /**
     * The paths one generation element contributed to the generated model. A path an earlier
     * stage had already generated is a warning rather than listed again.
     */
    public static class Generation
    {
        public String element;
        public List<String> generated = new ArrayList<>();
    }

    public static class FileGenerationStepResult extends StepResult
    {
        public List<FileGenerationResult> results = new ArrayList<>();
    }

    public static class FileGenerationResult
    {
        public String fileGeneration;
        public String type;
        /**
         * The folder the build writes this file generation's files under.
         */
        public String rootFolder;
        public List<GeneratedFile> files = new ArrayList<>();
    }

    public static class ArtifactGenerationStepResult extends StepResult
    {
        public List<ArtifactResult> results = new ArrayList<>();
    }

    public static class ArtifactResult
    {
        public String element;
        public String extension;
        /**
         * The folder the build writes these files under: the element's path as folders, then the
         * extension's key.
         */
        public String folder;
        public List<GeneratedFile> files = new ArrayList<>();
    }

    /**
     * A file as the build writes it: the name with whitespace and colons replaced, and the content
     * formatted as the build formats it.
     */
    public static class GeneratedFile
    {
        public String fileName;
        public String format;
        public String content;
    }

    /**
     * What failed and where. Only the fields that apply to the step are set.
     */
    public static class Failure
    {
        public String message;
        public SourceInformation sourceInformation;
        /**
         * Model generation: the id of the generation node it failed in.
         */
        public String stage;
        /**
         * Model generation: the generation element. Artifact generation: the element.
         */
        public String element;
        /**
         * File generation: the file generation.
         */
        public String fileGeneration;
        /**
         * Artifact generation: the extension's key.
         */
        public String extension;
    }

    /**
     * What the build only logs. Only the fields that apply to the step are set.
     */
    public static class Warning
    {
        public String message;
        public String stage;
        public String element;
        /**
         * The generated element or file the warning is about.
         */
        public String path;
    }
}
