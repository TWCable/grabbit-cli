/*
 * Copyright 2014-2016 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twcable.grabbit.tools.monitor;

import lombok.Value;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static com.twcable.grabbit.tools.monitor.JobState.COMPLETED;
import static com.twcable.grabbit.tools.monitor.JobState.FAILED;
import static com.twcable.grabbit.tools.monitor.JobState.RUNNING;
import static com.twcable.grabbit.tools.monitor.JobState.UNKNOWN;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyMap;

/**
 * An immutable value representing the status of a job, as returned by Grabbit's REST API.
 */
@Value
@SuppressWarnings({"WeakerAccess", "checkstyle:VisibilityModifier",
    "PMD.CommentDefaultAccessModifier", "RedundantCast"})
public class JobStatus {

    @SuppressWarnings("WeakerAccess")
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxx");

    URI uri;
    long transactionID;
    long jobExecutionId;
    OffsetDateTime startTime;
    @Nullable OffsetDateTime endTime;
    String path;
    long timeTaken;
    long jcrNodesWritten;
    String exitDescription;
    String exitCode;
    boolean running;


    @SuppressWarnings("unchecked")
    public static JobStatus fromJson(URI uri, @Nullable String jsonStr) {
        val json = (jsonStr == null || jsonStr.trim().isEmpty()) ? "{}" : jsonStr;

        val map = (Map<String, Object>)new Yaml().loadAs(json, Map.class);
        val transactionId = (@NonNull Long)map.getOrDefault("transactionID", -1L);
        val jobExecutionId = (@NonNull Long)map.getOrDefault("jobExecutionId", -1L);
        val startTimeStr = (String)map.getOrDefault("startTime", DATE_TIME_FORMATTER.format(now()));
        val startTime = OffsetDateTime.parse(startTimeStr, DATE_TIME_FORMATTER);
        val endTimeStr = (String)map.get("endTime");
        val endTime = (endTimeStr != null) ?
            OffsetDateTime.parse(endTimeStr, DATE_TIME_FORMATTER) : (OffsetDateTime)null;
        val path = (@NonNull String)map.getOrDefault("path", "/MISSING_PATH");
        val existStatusMap = (@NonNull Map<String, Object>)map.getOrDefault("exitStatus", emptyMap());
        val exitDescription = (@NonNull String)existStatusMap.getOrDefault("exitDescription", "");
        val exitCode = (@NonNull String)existStatusMap.getOrDefault("exitCode", "UNKNOWN");
        val running = (@NonNull Boolean)existStatusMap.getOrDefault("running", Boolean.FALSE);
        val timeTaken = Long.valueOf(map.getOrDefault("timeTaken", -1L).toString());
        val jcrNodesWritten = Long.parseLong(map.getOrDefault("jcrNodesWritten", -1L).toString());

        return new JobStatus(uri, transactionId, jobExecutionId, startTime, endTime, path, timeTaken, jcrNodesWritten,
            exitDescription, exitCode, running);
    }


    @SuppressWarnings("StringBufferReplaceableByString")
    public String asJson() {
        return new StringBuilder("{\"transactionID\": ").
            append(transactionID).
            append(", \"jobExecutionId\": ").
            append(jobExecutionId).
            append(", \"jcrNodesWritten\": ").
            append(jcrNodesWritten).
            append(", \"exitStatus\": ").
            append("{ \"exitDescription\": \"").
            append(exitDescription).
            append("\", \"exitCode\":\"").
            append(exitCode).
            append("\", \"running\":").
            append(running).
            append("}, ").
            append((endTime != null) ? "\"endTime\": \"" + endTime.format(DATE_TIME_FORMATTER) + "\", " : "").
            append("\"timeTaken\": ").
            append(timeTaken).
            append(", \"path\": \"").
            append(path).
            append("\", \"startTime\": \"").
            append(startTime.format(DATE_TIME_FORMATTER)).
            append("\"}").toString();
    }


    @SuppressWarnings({"checkstyle:NeedBraces", "checkstyle:EqualsAvoidNull"})
    public JobState state() {
        if (running) return RUNNING;
        if ("COMPLETED".equalsIgnoreCase(exitCode)) return COMPLETED;
        if ("FAILED".equalsIgnoreCase(exitCode)) return FAILED;
        return UNKNOWN;
    }

}
