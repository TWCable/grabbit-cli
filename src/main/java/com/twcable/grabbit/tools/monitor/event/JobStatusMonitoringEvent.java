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
package com.twcable.grabbit.tools.monitor.event;

import com.twcable.grabbit.tools.monitor.JobStatus;

import static java.lang.System.lineSeparator;

/**
 * The current status of a job, complete with detail.
 */
@SuppressWarnings({"WeakerAccess", "checkstyle:VisibilityModifier"})
public class JobStatusMonitoringEvent implements MonitoringEvent {
    /**
     * The full object representation of the job status as returned by the Grabbit client.
     */
    public final JobStatus jobStatus;


    public JobStatusMonitoringEvent(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }


    @Override
    public String toString() {
        return "job: " + jobStatus.jobExecutionId() + lineSeparator() +
            "startTime: " + jobStatus.startTime() + lineSeparator() +
            "path: " + jobStatus.path() + lineSeparator() +
            "status: " + jobStatus.exitCode() + lineSeparator() +
            "running: " + jobStatus.running() + lineSeparator() +
            "timeTaken: " + jobStatus.timeTaken() + lineSeparator() +
            "jcrNodesWritten: " + jobStatus.jcrNodesWritten() + lineSeparator() +
            "---";
    }
}
