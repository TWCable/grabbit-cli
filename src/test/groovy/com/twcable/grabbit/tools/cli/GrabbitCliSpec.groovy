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
package com.twcable.grabbit.tools.cli

import spock.lang.Ignore
import spock.lang.Specification

class GrabbitCliSpec extends Specification {

    @Ignore("Assumes a running instance of Grabbit Client")
    def "smoke"() {
        def classLoader = this.class.classLoader
        def envsFileName = classLoader.getResource("environments.json").file
        def grabbitConfFileName = classLoader.getResource("publish-content.json").file

        def options = CliOptions.create(["-s", grabbitConfFileName, envsFileName, "localhost"] as String[])

        when:
        GrabbitCli.run(options.get())

        then:
        true
    }

}
