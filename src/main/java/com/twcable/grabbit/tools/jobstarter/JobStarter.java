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
package com.twcable.grabbit.tools.jobstarter;

import com.twcable.grabbit.tools.environment.HostInfo;
import com.twcable.grabbit.tools.environment.UsernameAndPassword;
import com.twcable.grabbit.tools.util.Utils;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.regex.qual.Regex;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.WorkQueueProcessor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Interface to start jobs on a Grabbit client.
 */
@SuppressWarnings({"WeakerAccess", "RedundantTypeArguments"})
public class JobStarter {
    private static final Pattern JOB_IDS_PATTERN =
        Pattern.compile((@Regex(1) String)"^\\s*\\[(?<jobIds>[\\d,\\s]*)\\]\\s*$", Pattern.MULTILINE);

    private final JobsConfigFileReader jobsConfigFileReader;
    private final Iterable<HostInfo> hosts;
    private final ExecutorService executorService;


    public JobStarter(JobsConfigFileReader jobsConfigFileReader, Iterable<@NonNull HostInfo> hosts) {
        this.jobsConfigFileReader = jobsConfigFileReader;
        this.hosts = hosts;

        this.executorService = Executors.newSingleThreadExecutor();
    }


    /**
     * Start the jobs on the Grabbit clients, returning the hosts and the jobs ids.
     *
     * @throws IOException if it can't create the cache file
     */
    public Publisher<HostAndJobIds> startJobs() throws IOException {
        return Flux.fromIterable(hosts).
            map(hostInfo ->
                new HostAndJobIds(hostInfo.baseUri(), startJobs(hostInfo.baseUri(), hostInfo.credentials())));
    }


    /**
     * Start the jobs on the Grabbit clients, returning the hosts and the jobs ids.
     *
     * @throws IOException if it can't create the cache file
     */
    public static Publisher<HostAndJobIds> startJobs(JobsConfigFileReader jobsConfigFileReader,
                                                     Iterable<@NonNull HostInfo> hosts) throws IOException {
        return new JobStarter(jobsConfigFileReader, hosts).startJobs();
    }


    /**
     * Start a process on {@link #executorService} that connects to the Grabbit client at `baseUri` and publishes the
     * job ids.
     *
     * @param baseUri     the URI of the Grabbit client host to connect to
     * @param credentials the credentials to use to create the jobs
     */
    private Publisher<Long> startJobs(final URI baseUri, UsernameAndPassword credentials) {
        final Processor<Long, Long> processor = WorkQueueProcessor.share(executorService);

        executorService.execute(() -> {
            try {
                final URL clientUrl = grabbitClientUrl(baseUri);
                final InputStream inputStream = startJobOnClient(clientUrl, credentials);
                final String output = Utils.toString(inputStream).trim();

                parseStartJobsOutput(output, processor);
            }
            catch (IOException e) {
                processor.onError(e);
            }
        });

        return processor;
    }


    protected static URL grabbitClientUrl(URI baseUri) throws MalformedURLException {
        return new URL(baseUri.toURL(), "/grabbit/job");
    }


    @SuppressWarnings({"RedundantCast", "Convert2MethodRef"})
    private void parseStartJobsOutput(String startJobsOutput, Subscriber<Long> jobIdSub) {
        // the output from starting a job looks like "[123,125]"
        val matcher = JOB_IDS_PATTERN.matcher(startJobsOutput);
        if (matcher.matches()) {
            final String jobIdsStr = (@NonNull String)matcher.group("jobIds");
            Arrays.stream(jobIdsStr.split(",")).
                map(String::trim).
                map(Long::valueOf).
                forEach(jobId -> jobIdSub.onNext(jobId));
            jobIdSub.onComplete();
        }
        else {
            jobIdSub.onError(new IllegalStateException("Could not parse job ids from: " + startJobsOutput));
        }
    }


    @SuppressWarnings("PMD.PreserveStackTrace")
    protected BufferedInputStream startJobOnClient(URL url, UsernameAndPassword credentials) throws IOException {
        val httpCon = (HttpURLConnection)url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");
        httpCon.setRequestProperty("Content-Type", "application/json");
        httpCon.setRequestProperty("Authorization", "Basic " + credentials.basicAuthEncode());

        try {
            Utils.copy(jobsConfigFileReader.inputStream(), httpCon.getOutputStream());
        }
        catch (ConnectException e) {
            val newExp = new ConnectException(e.getMessage() + " when trying to connect to " + url);
            newExp.setStackTrace(e.getStackTrace());
            throw newExp;
        }
        return new BufferedInputStream(httpCon.getInputStream());
    }

}
