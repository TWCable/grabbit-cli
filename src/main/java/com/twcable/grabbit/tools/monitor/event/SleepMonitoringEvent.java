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

/**
 * An event indicating that the poller is about to sleep.
 */
@SuppressWarnings({"WeakerAccess", "checkstyle:VisibilityModifier"})
public class SleepMonitoringEvent implements MonitoringEvent {
    /**
     * The number of milliseconds this is going to sleep.
     */
    public final long sleepMs;


    public SleepMonitoringEvent(long sleepMs) {
        this.sleepMs = sleepMs;
    }


    @Override
    public String toString() {
        return "\n====================== Sleeping for " + sleepMs + " ms =======================";
    }
}
