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

import java.util.Collection;

import static java.lang.System.lineSeparator;

/**
 * A {@link MonitoringEvent} that contains any jobs that have completed since the last poll.
 */
@SuppressWarnings({"WeakerAccess", "checkstyle:VisibilityModifier"})
public class CompletedJobsMonitoringEvent implements MonitoringEvent {
    /**
     * The jobs that have completed as part of the last poll. May be empty.
     */
    public final Collection<JobStatus> completedJobs;


    public CompletedJobsMonitoringEvent(Collection<JobStatus> completedJobs) {
        this.completedJobs = completedJobs;
    }


    @Override
    public String toString() {
        final StringBuilder stringBuilder =
            new StringBuilder("\n====================== COMPLETED =====================\n");

        completedJobs.forEach(jobStatus ->
            stringBuilder.append(new JobStatusMonitoringEvent(jobStatus).toString()).append(lineSeparator())
        );

        return stringBuilder.toString();
    }
}
