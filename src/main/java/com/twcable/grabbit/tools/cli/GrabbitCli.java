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
package com.twcable.grabbit.tools.cli;

import com.twcable.grabbit.tools.environment.Environment;
import com.twcable.grabbit.tools.environment.HostInfo;
import com.twcable.grabbit.tools.environment.MapSourcedEnvironment;
import com.twcable.grabbit.tools.jobstarter.HostAndJobIds;
import com.twcable.grabbit.tools.jobstarter.JobStarter;
import com.twcable.grabbit.tools.jobstarter.JobsConfigFileReader;
import com.twcable.grabbit.tools.monitor.JobStatusCache;
import com.twcable.grabbit.tools.monitor.PollingJobMonitor;
import com.twcable.grabbit.tools.monitor.SimpleFileJobStatusCache;
import com.twcable.grabbit.tools.monitor.event.MonitoringEvent;
import com.twcable.grabbit.tools.util.Utils;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.util.Exceptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import static com.twcable.grabbit.tools.monitor.JobState.RUNNING;

/**
 * The command line entry point.
 * include::doc/cli-interaction.adoc[]
 *
 * @see JobStarter
 * @see PollingJobMonitor
 */
@SuppressWarnings({"WeakerAccess", "RedundantTypeArguments", "RedundantCast",
    "PMD.SystemPrintln", "PMD.AvoidPrefixingMethodParameters", "PMD.CommentDefaultAccessModifier",
    "PMD.DefaultPackage"})
public final class GrabbitCli {
    /**
     * The name of the job status cache file.
     */
    public static final String DEFAULT_JOB_STATUS_CACHE_FILENAME = "grabbitIds.out";


    private GrabbitCli() {
    }


    /**
     * Entry point for the command line arguments.
     * <p>
     * [plantuml]
     * ....
     * start
     * :CliOptions.create(argv);
     * if (Either(error message, CliOptions)) then (error message)
     * :print error message to STDERR;
     * end
     * else (CliOptions)
     * :run(cliOptions);
     * stop
     * endif
     * ....
     *
     * @param args CLI arguments
     */
    @SuppressWarnings("PMD.UseVarargs")
    public static void main(String[] args) {
        val cliOptions = CliOptions.create(args);

        if (cliOptions.isRight()) {
            try {
                run(cliOptions.get());
            }
            catch (Exceptions.ReactiveException e) {
                // unwrap exception
                ((@NonNull Throwable)e.getCause()).printStackTrace(System.err);
                System.exit(1);
            }
            catch (Throwable e) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            System.exit(0);
        }
        else {
            System.err.println(cliOptions.getLeft());
            System.exit(1);
        }
    }


    /**
     * Runs the appropriate process(es) for the command line options.
     * <p>
     * Output from the processes are sent to STDOUT.
     * <p>
     * [plantuml]
     * ....
     * :CliOptions.create(argv);
     * if (start) then (true)
     * if (monitor) then (true)
     * :startWithMonitor(..);
     * else (false)
     * :startWithNoMonitor(..);
     * endif
     * else (false)
     * :monitorJobs(..);
     * endif
     * ....
     *
     * @see #startWithMonitor(String, String, String, PrintStream)
     * @see #startWithNoMonitor(String, String, String, PrintStream)
     * @see #monitorJobs(String, String, String, PrintStream)
     */
    public static void run(CliOptions options) throws IOException {
        val envConfFile = options.envConfFile();
        val environmentName = options.environmentName();
        val monitor = options.monitor();
        val start = options.start();
        val printStream = System.out;

        if (start) {
            val jobsConfFile = (@NonNull String)options.jobsConfFile();
            if (monitor) {
                startWithMonitor(jobsConfFile, envConfFile, environmentName, printStream);
            }
            else {
                startWithNoMonitor(jobsConfFile, envConfFile, environmentName, printStream);
            }
        }
        else {
            val jobIdsFile = (@NonNull String)options.idsFile();
            monitorJobs(envConfFile, environmentName, jobIdsFile, printStream);
        }
    }


    /**
     * Start the jobs, them monitor their progress.
     *
     * @param jobsConfFile    the file name for the jobs configuration
     * @param envConfFile     file name for AEM environment configuration
     * @param environmentName the name of the environment to use from the environment config file
     * @param out             where to send output while monitoring the jobs
     * @throws IOException
     * @see #startJobs(String, String, String)
     * @see #monitor(JobStatusCache, String, String)
     */
    public static void startWithMonitor(String jobsConfFile,
                                        String envConfFile, String environmentName,
                                        PrintStream out) throws IOException {
        val startedJobs = startJobs(envConfFile, environmentName, jobsConfFile);

        val jobStatusCache = startedJobsToCache(startedJobs);

        val monitorEvents = monitor(jobStatusCache, envConfFile, environmentName);

        printMonitoringEvents(monitorEvents, out);
    }


    /**
     * Start the jobs and output their hosts and ids.
     *
     * @param jobsConfFile    the file name for the jobs configuration
     * @param envConfFile     file name for AEM environment configuration
     * @param environmentName the name of the environment to use from the environment config file
     * @param out             where to send output of the started jobs
     * @throws IOException
     * @see #startJobs(String, String, String)
     */
    public static void startWithNoMonitor(String jobsConfFile,
                                          String envConfFile, String environmentName,
                                          PrintStream out) throws IOException {
        val startedJobs = startJobs(envConfFile, environmentName, jobsConfFile);

        printStartedJobs(startedJobs, out);
    }


    /**
     * Create a {@link PollingJobMonitor} and send a textual representation of what it publishes to the
     * {@link PrintStream}.
     *
     * @param envConfFile     the environment configuration file name
     * @param environmentName the name of the environment to monitor
     * @param jobIdsFile      the name of the cache file for the {@link JobStatusCache}
     * @param out             where to send the string representation on what happens while monitoring
     * @see #monitor(JobStatusCache, String, String)
     */
    public static void monitorJobs(String envConfFile, String environmentName,
                                   String jobIdsFile, PrintStream out) throws IOException {
        val jobStatusCache = openJobStatusCache(jobIdsFile);

        val monitorEvents = monitor(jobStatusCache, envConfFile, environmentName);

        printMonitoringEvents(monitorEvents, out);
    }


    /**
     * Print the started jobs to the PrintStream, waiting up to 30 minutes for the jobs
     * to finish starting across all the hosts.
     */
    static void printStartedJobs(Publisher<HostAndJobIds> startedJobs, PrintStream out) {
        Flux.from(startedJobs).
            flatMap(GrabbitCli::hostAndJobIdsToStrings).
            doOnNext(out::println).
            then().block(Duration.ofMinutes(30));
    }


    /**
     * Using the provided configuration files, start jobs and publish the results.
     *
     * @see JobStarter#startJobs(JobsConfigFileReader, Iterable)
     */
    public static Publisher<HostAndJobIds> startJobs(String envConfFile, String environmentName,
                                                     String jobsConfFile) throws IOException {
        val env = environment(envConfFile, environmentName);

        val jobsConfigFileReader = jobsConfigFileReader(jobsConfFile);
        val hosts = hosts(env, jobsConfigFileReader);

        return JobStarter.startJobs(jobsConfigFileReader, hosts);
    }


    /**
     * "Denormalize" the host and its job ids.
     * <p>
     * For example, if it receives { http://test.com, [123, 678] } this will publish
     * [ "http://test.com, 123", "http://test.com, 678" ]
     */
    static Publisher<String> hostAndJobIdsToStrings(HostAndJobIds hostAndJobIds) {
        return Flux.from(hostAndJobIds.jobIds()).map(jobId -> hostAndJobIds.uri() + ", " + jobId);
    }


    private static JobStatusCache openJobStatusCache(String jobIdsFile) throws IOException {
        val jobStatusCacheFile = new File(jobIdsFile);
        return SimpleFileJobStatusCache.open(jobStatusCacheFile);
    }


    /**
     * Creates an {@link Environment} instance from the given configuration file for the named environment.
     *
     * @param envConfFile     the environment configuration file name
     * @param environmentName the name of the environment to monitor
     */
    @SuppressWarnings("unchecked")
    public static Environment environment(String envConfFile, String environmentName) throws IOException {
        val envConfigFile = new File(envConfFile);
        ensureFileExists(envConfigFile);

        val json = Utils.configAsMap(envConfigFile);
        val env = (Map<String, ?>)json.get(environmentName);
        if (env == null) {
            throw new IllegalArgumentException("Can not find \"" + environmentName + "\" in \"" +
                envConfigFile.getAbsolutePath() + "\"");
        }

        return MapSourcedEnvironment.createFromMap(env);
    }


    /**
     * Get all the {@link HostInfo} for the type of nodes specified in {@link JobsConfigFileReader#configNodeType()}
     */
    public static Iterable<HostInfo> hosts(Environment env, JobsConfigFileReader jobsConfigFileReader) {
        val nodeType = jobsConfigFileReader.configNodeType();
        return env.hostsOfType(nodeType).collect(Collectors.<@NonNull HostInfo>toList());
    }


    private static JobsConfigFileReader jobsConfigFileReader(String grabbitConfFile) throws IOException {
        val jobConfigFile = new File(grabbitConfFile);
        ensureFileExists(jobConfigFile);
        return new JobsConfigFileReader(jobConfigFile);
    }


    /**
     * Creates a {@link SimpleFileJobStatusCache} with the default filename, waiting up to 30 minutes for the jobs
     * to finish starting across all the hosts.
     *
     * @param startedJobs the job information
     * @see GrabbitCli#DEFAULT_JOB_STATUS_CACHE_FILENAME
     * @see GrabbitCli#putInJobCache(JobStatusCache, HostAndJobIds)
     */
    static JobStatusCache startedJobsToCache(Publisher<HostAndJobIds> startedJobs) throws IOException {
        val file = new File(DEFAULT_JOB_STATUS_CACHE_FILENAME);
        val jobStatusCache = SimpleFileJobStatusCache.createEmpty(file);

        Flux.from(startedJobs).
            doOnNext(hj -> putInJobCache(jobStatusCache, hj)).
            then().block(Duration.ofMinutes(30));

        return jobStatusCache;
    }


    /**
     * Copy the host and its job ids into the monitoring cache, waiting up to 5 minutes for the jobs to finish
     * starting for the host.
     *
     * @param jobStatusCache the cache to use for monitoring
     * @param hostAndJobIds  the job information
     */
    public static void putInJobCache(JobStatusCache jobStatusCache, HostAndJobIds hostAndJobIds) {
        Flux.from(hostAndJobIds.jobIds()).
            doOnNext(jobId -> jobStatusCache.put(hostAndJobIds.uri(), jobId, RUNNING)).
            then().block(Duration.ofMinutes(5));
    }


    private static void ensureFileExists(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getCanonicalPath() + " can not be found");
        }
    }


    /**
     * Creates a {@link PollingJobMonitor} and publishes the monitoring events.
     *
     * @param jobStatusCache  the cache to use for knowing what jobs to monitor
     * @param envConfFile     the environment configuration file name
     * @param environmentName the name of the environment to monitor
     */
    public static Publisher<MonitoringEvent> monitor(JobStatusCache jobStatusCache,
                                                     String envConfFile, String environmentName) throws IOException {
        val env = environment(envConfFile, environmentName);

        val jobMonitor = PollingJobMonitor.builder().
            jobStatusCache(jobStatusCache).
            environment(env).
            build();

        return jobMonitor.monitor();
    }


    /**
     * Send the monitoring events to the PrintStream, waiting up to 30 days for it to complete.
     *
     * @param monitorEvents the events to print
     * @param out           where to print the events
     * @see MonitoringEvent#toString()
     */
    public static void printMonitoringEvents(Publisher<MonitoringEvent> monitorEvents, PrintStream out) {
        Flux.from(monitorEvents).
            map(MonitoringEvent::toString).
            doOnNext(out::println).
            then().block(Duration.ofDays(30));
    }

}
