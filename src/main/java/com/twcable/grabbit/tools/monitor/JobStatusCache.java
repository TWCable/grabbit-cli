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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.util.Collection;

/**
 * A cache of the job status from the last time "start" was invoked. While monitoring the jobs, the status of the
 * jobs are likely the change.
 */
public interface JobStatusCache {

    Collection<HostJobState> entries();


    /**
     * Puts the given entry in the cache, returning the value it's replacing.
     *
     * @return null if it's a new location/jobId combination
     */
    default @Nullable HostJobState put(URI location, long jobId, JobState state) {
        return put(new HostJobState(location, jobId, state));
    }


    /**
     * Puts the given entry in the cache, returning the value it's replacing.
     *
     * @return null if it's a new location/jobId combination
     */
    @Nullable HostJobState put(HostJobState entry);

}
