// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.shared.core.operational.errorManagement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler.MatchingPriority;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class to hold data corresponding to a particular category of exceptions
 * This data is to be used in categorising an exception occurring during execution
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionCategoryData
{
    /**
     * User-friendly string for the exception category
     */
    private final ExceptionCategory exceptionCategory;

    /**
     * List of exception outlines associated with this category
     */
    private final HashMap<MatchingPriority, List<ExceptionOutline>> exceptions;

    /**
     * Constructor to create an exception category data object containing data to be used in categorising occurring exceptions
     * @param exceptionCategory is the name of the category meant to be end user understandable.
     * @param exceptions is the list of exception outlines to be used for matching.
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ExceptionCategoryData(@JsonProperty("CategoryName") ExceptionCategory exceptionCategory, @JsonProperty("Exceptions") ArrayList<ExceptionOutline> exceptions)
    {
        this.exceptionCategory = exceptionCategory;
        this.exceptions = new HashMap<>();
        for (MatchingPriority priority : MatchingPriority.values())
        {
            this.exceptions.put(priority, exceptions.stream().filter(exception -> exception.getPriority() == priority).collect(Collectors.toList()));
        }
    }

    /**
     * Method to check if an exception category and an occurred exception are a match under a specified matching priority
     * @param exception is the exception that occurred during execution
     * @param priority is the priority of exception matching we would like to execute
     * @return true if the exception and category are a match under the given priority and false otherwise.
     */
    public boolean matches(Throwable exception, MatchingPriority priority)
    {
        assert (priority != null);
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        String name = exception.getClass().getSimpleName();
        return this.exceptions.get(priority).stream().anyMatch(e -> e.matches(name, message));
    }

    /**
     * @return user-friendly string corresponding to this exception category
     */
    public ExceptionCategory getExceptionCategory()
    {
        return exceptionCategory;
    }

    /**
     * Class to hold an exception's class name regex and message regex to match upcoming exceptions against
     * Holds the matching priority for the given name-message pair.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class ExceptionOutline
    {
        /**
         * Exception class name regex
         */
        private final Pattern exceptionName;

        /**
         * Regex pattern to match with the exception message
         */
        private final Pattern exceptionMessage;

        /**
         * Matching priority to define the order of importance of outlines.
         */
        private final MatchingPriority priority;

        /**
         * Constructor to create an exception outline object
         * @param exceptionName is the simple name regex of the exception
         * @param exceptionMessage is the regex corresponding to the message in the exception
         * @param priority is the priority associated with the outline.
         */
        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public ExceptionOutline(@JsonProperty("ExceptionName") String exceptionName, @JsonProperty("MessageRegex") String exceptionMessage, @JsonProperty("Priority") MatchingPriority priority)
        {
            String nameRegex = exceptionName == null ? ".*" : String.format("^%s$", exceptionName);
            this.exceptionName = Pattern.compile(nameRegex, Pattern.CASE_INSENSITIVE);
            this.exceptionMessage = exceptionMessage == null ? Pattern.compile(".*", Pattern.CASE_INSENSITIVE) : Pattern.compile(exceptionMessage, Pattern.CASE_INSENSITIVE);
            this.priority = priority;
        }

        /**
         * Method to check if an exception name and message match a predefined name and message regex pair
         * @param name is the simple name of the exception
         * @param message is the message included in the exception
         * @return true if the name and message match the predefined pair false otherwise
         */
        public boolean matches(String name, String message)
        {
            Matcher nameMatcher = this.exceptionName.matcher(name);
            Matcher messageMatcher = this.exceptionMessage.matcher(message);
            return nameMatcher.find() && messageMatcher.find();
        }

        /**
         * Method to get the priority of an exception outline.
         * @return primary or secondary.
         */
        public MatchingPriority getPriority()
        {
            return priority;
        }

    }
}
