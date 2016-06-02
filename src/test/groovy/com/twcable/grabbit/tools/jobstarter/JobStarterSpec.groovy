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
package com.twcable.grabbit.tools.jobstarter

import com.twcable.grabbit.tools.environment.HostInfo
import com.twcable.grabbit.tools.environment.UsernameAndPassword
import reactor.core.publisher.Flux
import spock.lang.Specification
import spock.lang.Subject

import java.util.stream.Collectors

import static com.twcable.grabbit.tools.environment.HostInfo.NodeType.PUBLISHER

@Subject(JobStarter)
class JobStarterSpec extends Specification {

    def "StartJobs"() {
        def host = URI.create("http://test.test")
        def file = File.createTempFile("jobstarter", "spec")
        def jobStarter = new JobStarter(new JobsConfigFileReader(file), [new HostInfo(PUBLISHER, host, new UsernameAndPassword("test", "testpw"))]) {
            protected BufferedInputStream startJobOnClient(URL url, UsernameAndPassword credentials) throws IOException {
                return new BufferedInputStream(new ByteArrayInputStream("[123,456]".bytes))
            }
        }

        when:
        def publisher = jobStarter.startJobs()
        def hosts = Flux.from(publisher).map({ it.uri }).collect(Collectors.toList()).block()
        def jobIds = Flux.from(publisher).flatMap({ it.jobIds }).collect(Collectors.toList()).block()

        then:
        hosts == [host]
        jobIds == [123, 456]

        cleanup:
        file.delete()
    }

}
