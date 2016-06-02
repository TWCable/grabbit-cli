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

import java.net.URI;

/**
 * An event indicating the start of a polling request to the Grabbit client.
 */
@SuppressWarnings({"WeakerAccess", "checkstyle:VisibilityModifier"})
public class PollingMonitoringEvent implements MonitoringEvent {
    /**
     * The base URI of the Grabbit client being polled.
     */
    public final URI location;
    /**
     * The ID of the Job being queried.
     */
    public final long jobId;


    public PollingMonitoringEvent(URI location, long jobId) {
        this.location = location;
        this.jobId = jobId;
    }


    @Override
    public String toString() {
        return "Polling Grabbit job " + jobId + " on " + location;
    }
}
