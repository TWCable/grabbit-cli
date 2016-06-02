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
package com.twcable.grabbit.tools.monitor

import com.twcable.grabbit.tools.monitor.event.EndMonitoringEvent
import com.twcable.grabbit.tools.monitor.event.JobStatusMonitoringEvent
import com.twcable.grabbit.tools.monitor.event.MonitoringEvent
import com.twcable.grabbit.tools.monitor.event.StartMonitoringEvent
import groovy.transform.CompileStatic
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.WorkQueueProcessor
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Collectors

import static com.twcable.grabbit.tools.monitor.JobState.COMPLETED
import static com.twcable.grabbit.tools.monitor.JobState.FAILED
import static com.twcable.grabbit.tools.monitor.JobState.RUNNING

@SuppressWarnings("GroovyAssignabilityCheck")
class PollingJobMonitorSpec extends Specification {

    @AutoCleanup("shutdown")
    ExecutorService executor

    @Subject
    PollingJobMonitor monitor


    def setup() {
//        executor = Executors.newCachedThreadPool()
        executor = Executors.newSingleThreadExecutor()
        monitor = createPollingJobMonitor()
    }


    def "monitor"() {
        when:
        def publisher = monitor.monitor()
        def events = consumeToEventList(publisher)

        then:
        events.first().class == StartMonitoringEvent
        events.last().class == EndMonitoringEvent
    }

    // **********************************************************************
    //
    // HELPER METHODS
    //
    // **********************************************************************

    List<MonitoringEvent> consumeToEventList(Publisher<MonitoringEvent> publisher) {
        return Flux.from(publisher).collect(Collectors.toList()).block(Duration.ofSeconds(2))
    }


    @CompileStatic
    PollingJobMonitor createPollingJobMonitor() {
        def jobStatusCache = createJobStatusCache()
        def startTime = OffsetDateTime.now().minusHours(1)

        def thePollingStatusProcessor = WorkQueueProcessor.create(executor)
        def poller = new JobStatusPoller() {
            int counter = 0


            @Override
            JobStatus pollJobStatus(URI location, long jobId) {
                def jobStatus = createJobStatus(location, jobId, startTime,
                    (counter < 4) ? RUNNING : (counter < 7) ? COMPLETED : FAILED)
                counter++

                thePollingStatusProcessor.onNext(new JobStatusMonitoringEvent(jobStatus))

                return jobStatus
            }
        }

        return PollingJobMonitor.builder().jobStatusCache(jobStatusCache).executor(executor).poller(poller).sleep(1L).build()
    }


    SimpleMapJobStatusCache createJobStatusCache() {
        def host = URI.create("http://test.com")
        def jobStatusCache = new SimpleMapJobStatusCache()
        jobStatusCache.put(host, 234234234L, RUNNING)
        jobStatusCache.put(host, 234234235L, RUNNING)
        jobStatusCache.put(host, 234234236L, RUNNING)
        jobStatusCache.put(host, 234234237L, RUNNING)
        return jobStatusCache
    }


    @CompileStatic
    protected static JobStatus createJobStatus(URI host, long jobId, OffsetDateTime startTime, JobState jobState) {
        def endTime = OffsetDateTime.now().minusMinutes(1)
        def between = Duration.between(startTime, endTime)

        switch (jobState) {
            case COMPLETED:
                return new JobStatus(host, 1234L, jobId,
                    startTime,
                    endTime,
                    "/content",
                    between.getSeconds(),
                    10234L,
                    "It's all cool",
                    "COMPLETED",
                    false
                )

            case FAILED:
                return new JobStatus(host, 1234L, jobId,
                    startTime,
                    endTime,
                    "/content",
                    between.getSeconds(),
                    234L,
                    "AAAck!!!!",
                    "FAILED",
                    false
                )

            default: return new JobStatus(host, 1234L, jobId,
                startTime,
                null,
                "/content",
                -1,
                234L,
                "",
                "RUNNING",
                true)
        }
    }

}
