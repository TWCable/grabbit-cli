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

import com.twcable.grabbit.tools.util.Utils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.val;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.twcable.grabbit.tools.util.Utils.softened;
import static java.nio.charset.Charset.defaultCharset;

/**
 * A simple file-based persistent implementation of {@link JobStatusCache}.
 * <p>
 * It explicitly is NOT designed for performance, thread-safety, or multiple processes.
 */
@SuppressWarnings({"WeakerAccess", "RedundantTypeArguments", "checkstyle:MultipleStringLiterals"})
public final class SimpleFileJobStatusCache extends SimpleMapJobStatusCache {
    private final File file;


    private SimpleFileJobStatusCache(File file) {
        this.file = file;
    }


    public static SimpleFileJobStatusCache open(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("\"" + file.getCanonicalPath() + "\" does not exist");
        }
        return new SimpleFileJobStatusCache(file);
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public static SimpleFileJobStatusCache createEmpty(File file) throws IOException {
        Files.deleteIfExists(file.toPath());
        file.createNewFile();
        return new SimpleFileJobStatusCache(file);
    }


    @Override
    @EnsuresNonNull("hostJobStates")
    public Collection<HostJobState> entries() {
        if (hostJobStates == null) {
            hostJobStates = readFile();
        }
        return hostJobStates;
    }


    /**
     * Puts the given entry in the cache, returning the value it's replacing.
     *
     * @return null if it's a new location/jobId combination
     */
    @Override
    @SuppressWarnings("contracts")
    public @Nullable HostJobState put(HostJobState entry) {
        val oldEntry = super.put(entry);

        // only pay the price of writing if the entry was added/changed
        if (oldEntry == null || (oldEntry.state() != entry.state())) {
            writeFile();
        }

        return oldEntry;
    }


    protected List<HostJobState> readFile() {
        try {
            val inputStream = new FileInputStream(file);
            val inputStreamReader = new InputStreamReader(inputStream, defaultCharset());
            val fileReader = new BufferedReader(inputStreamReader);
            try {
                return fileReader.lines().map(line -> {
                    final String[] data = line.split(",");
                    final String location = data[0].trim();
                    final long jobId = Long.parseLong(data[1].trim());
                    final String state = data[2].trim();
                    return new HostJobState(URI.create(location), jobId, JobState.valueOf(state));
                }).collect(Collectors.<@NonNull HostJobState>toList());
            }
            finally {
                Utils.close(fileReader);
            }
        }
        catch (FileNotFoundException e) {
            throw softened(e);
        }
    }


    @RequiresNonNull("hostJobStates")
    protected void writeFile() {
        Writer fileWriter = null;
        try {
            fileWriter = new OutputStreamWriter(new FileOutputStream(file), defaultCharset());
            val lineWriter = Utils.lineWriter(fileWriter);
            hostJobStates.stream().map(e -> e.location() + "," + e.jobId() + "," + e.state()).forEach(lineWriter);
        }
        catch (IOException e) {
            throw softened(e);
        }
        finally {
            Utils.flushAndClose(fileWriter);
        }
    }

}
