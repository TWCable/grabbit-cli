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

import lombok.val;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A simple {@link java.util.Map} persistent implementation of {@link JobStatusCache}.
 *
 * It explicitly is NOT designed for thread-safety.
 */
@SuppressWarnings("WeakerAccess")
public class SimpleMapJobStatusCache implements JobStatusCache {
    @MonotonicNonNull
    protected Collection<HostJobState> hostJobStates;


    private static boolean match(HostJobState entry1, HostJobState entry2) {
        return entry1.location().equals(entry2.location()) && entry1.jobId() == entry2.jobId();
    }


    @Override
    @EnsuresNonNull("hostJobStates")
    public Collection<HostJobState> entries() {
        if (hostJobStates == null) {
            hostJobStates = new ArrayList<>();
        }
        return hostJobStates;
    }


    /**
     * Puts the given entry in the cache, returning the value it's replacing.
     *
     * @return null if it's a new location/jobId combination
     */
    @Override
    @EnsuresNonNull("hostJobStates")
    public @Nullable HostJobState put(HostJobState entry) {
        val existing = entries().stream().filter(e -> match(e, entry)).findAny().orElse(null);

        // if nothing would change, there's no point in rewriting the collection and persisting the non-change
        if (existing == null || !existing.state().equals(entry.state())) {
            if (existing == null) { // new entry
                this.hostJobStates = new ArrayList<>(entries());
                hostJobStates.add(entry);
            }
            else { // modified entry
                hostJobStates = entries().stream().map(e ->
                    match(e, entry) ? entry : e
                ).collect(Collectors.<@NonNull HostJobState>toList());
            }
        }

        return existing;
    }

}
