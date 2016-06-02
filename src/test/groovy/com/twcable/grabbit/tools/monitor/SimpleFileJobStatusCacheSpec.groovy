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

import spock.lang.Specification

class SimpleFileJobStatusCacheSpec extends Specification {

    def "put and entries work as expected"() {
        def file = File.createTempFile("SimpleFileJobStatusCacheSpec", "test")
        SimpleFileJobStatusCache cache = SimpleFileJobStatusCache.createEmpty(file)

        expect:
        cache.entries().isEmpty()

        when:
        def existing = cache.put(URI.create("http://test.com"), 2345345, JobState.RUNNING)

        then:
        existing == null
        cache.entries().first().state == JobState.RUNNING

        when:
        existing = cache.put(URI.create("http://test.com"), 2345345, JobState.COMPLETED)

        then:
        existing.state == JobState.RUNNING
        cache.entries().first().state == JobState.COMPLETED

        cleanup:
        file.delete()
    }

}
