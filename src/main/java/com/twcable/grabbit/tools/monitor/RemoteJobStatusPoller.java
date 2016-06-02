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

import com.twcable.grabbit.tools.environment.Environment;
import com.twcable.grabbit.tools.environment.HostInfo;
import com.twcable.grabbit.tools.environment.UsernameAndPassword;
import com.twcable.grabbit.tools.util.Utils;
import lombok.Value;
import lombok.val;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * A {@link JobStatusPoller} that connects to a remote instance for job information.
 */
@Value
@SuppressWarnings({"WeakerAccess", "checkstyle:VisibilityModifier", "PMD.CommentDefaultAccessModifier"})
public class RemoteJobStatusPoller implements JobStatusPoller {
    Environment environment;


    @Override
    public JobStatus pollJobStatus(URI location, long jobId) {
        try {
            val jobStatusStream = jobStatusOnClient(location, jobId);
            val jobStatusStr = Utils.toString(jobStatusStream);

            // may be needed for psuedo-jobs ids like "all", but that's not used here...
            // jobStatusStr = jobStatusStr.replaceFirst("\\[", "").replaceFirst("\\]", ""));

            return JobStatus.fromJson(location, jobStatusStr);
        }
        catch (IOException e) {
            throw Utils.softened(e);
        }
    }


    @SuppressWarnings("PMD.PreserveStackTrace")
    private BufferedInputStream jobStatusOnClient(URI baseUri, long jobId) throws IOException {
        val url = new URL(baseUri.toURL(), "/grabbit/job/" + jobId + ".json");
        try {
            val httpCon = (HttpURLConnection)url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("GET");
            // httpCon.setRequestProperty("Content-Type", "application/json");
            val credentials = credentialsFor(baseUri);
            httpCon.setRequestProperty("Authorization", "Basic " + credentials.basicAuthEncode());
            return new BufferedInputStream(httpCon.getInputStream());
        }
        catch (ConnectException e) {
            val newExp = new ConnectException(e.getMessage() + " when trying to connect to " + url);
            newExp.setStackTrace(e.getStackTrace());
            throw newExp;
        }
    }


    private UsernameAndPassword credentialsFor(URI baseUri) {
        return environment.allHosts().
            filter(hostInfo -> hostInfo.baseUri().equals(baseUri)).
            map(HostInfo::credentials).
            findFirst().
            orElseThrow(() -> new IllegalStateException("Could not find a match for " + baseUri));
    }

}
