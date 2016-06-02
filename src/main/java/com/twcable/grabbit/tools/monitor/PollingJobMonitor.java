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
import com.twcable.grabbit.tools.monitor.event.CompletedJobsMonitoringEvent;
import com.twcable.grabbit.tools.monitor.event.EndMonitoringEvent;
import com.twcable.grabbit.tools.monitor.event.FailedJobsMonitoringEvent;
import com.twcable.grabbit.tools.monitor.event.MonitoringEvent;
import com.twcable.grabbit.tools.monitor.event.PollingMonitoringEvent;
import com.twcable.grabbit.tools.monitor.event.SleepMonitoringEvent;
import com.twcable.grabbit.tools.monitor.event.StartMonitoringEvent;
import lombok.val;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.WorkQueueProcessor;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.twcable.grabbit.tools.monitor.JobState.COMPLETED;
import static com.twcable.grabbit.tools.monitor.JobState.FAILED;
import static com.twcable.grabbit.tools.monitor.JobState.RUNNING;
import static com.twcable.grabbit.tools.util.Utils.softened;

/**
 * A job monitor that continually polls hosts to get their current status.
 *
 * @see PollingJobMonitor#builder()
 * @see #monitor()
 */
@SuppressWarnings({"WeakerAccess", "RedundantTypeArguments"})
public final class PollingJobMonitor {
    /**
     * The default period in milliseconds between status requests. Can change by setting the "pollTime" system
     * property, otherwise defaults to 15,000ms.
     *
     * @see B.Build#sleep(long)
     */
    public static final int POLLING_TIME = Integer.parseInt(System.getProperty("pollTime", "15000"));

    private final JobStatusCache jobStatusCache;
    private final ExecutorService executor;
    private final JobStatusPoller jobStatusPoller;
    private final long sleepMs;


    /**
     * Create a monitoring instance that uses polling to get the status of jobs.
     */
    private PollingJobMonitor(JobStatusCache jobStatusCache, JobStatusPoller jobStatusPoller,
                              long sleepMs, ExecutorService executor) {
        this.jobStatusCache = jobStatusCache;
        this.sleepMs = sleepMs;
        this.executor = executor;
        this.jobStatusPoller = jobStatusPoller;
    }


    /**
     * Creates a builder to construct a {@link PollingJobMonitor}, enforcing a valid state.
     */
    public static B.WithJobStatusCache builder() {
        return new B.Builder();
    }


    /**
     * As long as there are running jobs (tracked in the {@link JobStatusCache}) repeatedly poll them at regular
     * intervals, publishing the monitoring status.
     *
     * [plantuml]
     * ....
     * hide footbox
     *
     * PollingJobMonitor <-[ : monitor
     * PollingJobMonitor -->[ : StartMonitoringEvent
     * loop there are RUNNING jobs <size:18><&timer></size>
     * PollingJobMonitor -->[ : PollingMonitoringEvent
     * PollingJobMonitor -->[ : CompletedJobsMonitoringEvent
     * PollingJobMonitor -->[ : FailedJobsMonitoringEvent
     * PollingJobMonitor -->[ : SleepMonitoringEvent
     * end
     * PollingJobMonitor -->[ : EndMonitoringEvent
     * ....
     *
     * @return the {@link Publisher} to send status updates out on
     * @see StartMonitoringEvent
     * @see PollingMonitoringEvent
     * @see CompletedJobsMonitoringEvent
     * @see FailedJobsMonitoringEvent
     * @see SleepMonitoringEvent
     * @see EndMonitoringEvent
     */
    public Publisher<MonitoringEvent> monitor() {
        final Processor<MonitoringEvent, MonitoringEvent> processor = WorkQueueProcessor.share(executor);

        executor.execute(new MonitoringRunnable(processor));

        return processor;
    }


    // **********************************************************************
    //
    // HELPER CLASSES
    //
    // **********************************************************************

    /**
     * The process that continuously runs to monitor the jobs, publishing results to the provided {@link Subscriber}.
     *
     * @see #run()
     */
    public class MonitoringRunnable implements Runnable {
        private final Subscriber<MonitoringEvent> monitoringEventSubscriber;


        /**
         * @param monitoringEventSubscriber where to send monitoring results
         */
        public MonitoringRunnable(Subscriber<MonitoringEvent> monitoringEventSubscriber) {
            this.monitoringEventSubscriber = monitoringEventSubscriber;
        }


        /**
         * At the start of each poll, will send a {@link StartMonitoringEvent}, followed by
         * {@link CompletedJobsMonitoringEvent} and {@link FailedJobsMonitoringEvent} with jobs populated as
         * appropriate. As long as there are still running jobs, it will sleep and then poll again.
         *
         * When there are no more running jobs, an {@link EndMonitoringEvent} is sent and
         * {@link Subscriber#onComplete()} is called.
         */
        @Override
        @SuppressWarnings("checkstyle:EmptyForIteratorPad")
        public void run() {
            val startTime = Instant.now();

            for (boolean hasRunningJobs = true; hasRunningJobs; ) {
                hasRunningJobs = poll();
            }

            monitoringEventSubscriber.onNext(new EndMonitoringEvent(startTime, Instant.now()));
            monitoringEventSubscriber.onComplete();
        }


        private boolean poll() {
            monitoringEventSubscriber.onNext(new StartMonitoringEvent());

            val jobResults = jobStatusesForRunningJobs(monitoringEventSubscriber);

            publishCompletionReports(monitoringEventSubscriber, jobResults);

            val hasRunningJobs = jobResults.stream().anyMatch(this::isRunning);

            if (hasRunningJobs) {
                sleep(monitoringEventSubscriber);
            }
            return hasRunningJobs;
        }


        private boolean isRunning(JobStatus jobStatus) {
            return jobStatus.state() == RUNNING;
        }


        private void publishCompletionReports(Subscriber<MonitoringEvent> subscriber,
                                              Collection<JobStatus> jobResults) {
            subscriber.onNext(new CompletedJobsMonitoringEvent(jobsOfState(jobResults, COMPLETED)));
            subscriber.onNext(new FailedJobsMonitoringEvent(jobsOfState(jobResults, FAILED)));
        }


        private Collection<@NonNull JobStatus> jobsOfState(Collection<JobStatus> jobResults, JobState jobState) {
            return jobResults.stream().
                filter(jobStatus -> jobStatus.state() == jobState).
                collect(Collectors.<@NonNull JobStatus>toList());
        }


        private Collection<JobStatus> jobStatusesForRunningJobs(Subscriber<MonitoringEvent> subscriber) {
            return jobStatusCache.entries().stream().
                filter(entry -> entry.state() == RUNNING).
                map(entry -> {
                    subscriber.onNext(new PollingMonitoringEvent(entry.location(), entry.jobId()));
                    final JobStatus jobStatus = jobStatusPoller.pollJobStatus(entry.location(), entry.jobId());
                    jobStatusCache.put(entry.location(), jobStatus.jobExecutionId(), jobStatus.state());
                    return jobStatus;
                }).
                collect(Collectors.<@NonNull JobStatus>toList());
        }


        private void sleep(Subscriber<MonitoringEvent> subscriber) {
            subscriber.onNext(new SleepMonitoringEvent(sleepMs));
            try {
                Thread.sleep(sleepMs);
            }
            catch (InterruptedException e) {
                throw softened(e);
            }
        }
    }


    /**
     * Simple namespace for builder classes/interfaces.
     *
     * The builder uses a number of trivial interfaces to ensure that the type system enforces that it is impossible
     * to call {@link Build#build()} in an invalid state.
     */
    @SuppressWarnings({"unused", "checkstyle:JavadocType", "PMD.AccessorClassGeneration"})
    public interface B {
        class Builder implements Build, ExecutorOrEnvironment, WithJobStatusCache {
            private @MonotonicNonNull JobStatusCache jobStatusCache;
            private @MonotonicNonNull ExecutorService executorService;
            private @MonotonicNonNull JobStatusPoller jobStatusPoller;
            private @MonotonicNonNull Environment environment;
            private long sleepMs = -1;


            @SuppressWarnings({"RedundantCast", "PMD.AvoidLiteralsInIfCondition"})
            public PollingJobMonitor build() {
                if (executorService == null) {
                    val executorService = Executors.newCachedThreadPool();
                    this.executorService = executorService;

                    // make sure the Executor shuts down cleanly
                    val shutdownThread = new Thread(executorService::shutdown, "PollingJobMonitor shutdown");
                    Runtime.getRuntime().addShutdownHook(shutdownThread);
                }

                if (jobStatusPoller == null) {
                    if (environment == null) {
                        // should be impossible
                        throw new IllegalStateException("Need to provide either a jobStatusPoller or environment");
                    }
                    jobStatusPoller = new RemoteJobStatusPoller(environment);
                }

                if (sleepMs < 1L) {
                    sleepMs = POLLING_TIME;
                }

                return new PollingJobMonitor((@NonNull JobStatusCache)jobStatusCache, jobStatusPoller,
                    sleepMs, (@NonNull ExecutorService)executorService);
            }


            public ExecutorOrEnvironment jobStatusCache(JobStatusCache jobStatusCache) {
                this.jobStatusCache = jobStatusCache;
                return this;
            }


            public Build executor(ExecutorService executorService) {
                this.executorService = executorService;
                return this;
            }


            @Override
            public Build environment(Environment environment) {
                this.environment = environment;
                return this;
            }


            public Build poller(JobStatusPoller jobStatusPoller) {
                this.jobStatusPoller = jobStatusPoller;
                return this;
            }


            public Build sleep(long sleepMs) {
                this.sleepMs = sleepMs;
                return this;
            }
        }

        interface Build {
            PollingJobMonitor build();

            /**
             * The strategy to use for polling.
             */
            Build poller(JobStatusPoller jobStatusPoller);


            /**
             * The number of milliseconds to sleep between polling requests.
             */
            Build sleep(long sleepMs);
        }

        interface WithExecutor {
            /**
             * The {@link ExecutorService} to use for publishing results.
             */
            Build executor(ExecutorService executorService);
        }


        interface WithEnvironment {
            /**
             * The {@link Environment} to use for creating a {@link RemoteJobStatusPoller}.
             */
            Build environment(Environment environment);
        }

        interface ExecutorOrEnvironment extends WithExecutor, WithEnvironment {
        }

        interface WithJobStatusCache {
            /**
             * The cache to use to know what jobs to monitor.
             */
            ExecutorOrEnvironment jobStatusCache(JobStatusCache jobStatusCache);
        }
    }

}
