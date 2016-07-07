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

import spock.lang.Specification
import spock.lang.Unroll

class CliOptionsSpec extends Specification {

    @Unroll
    def "create with #args"() {
        expect:
        (CliOptions.create(args as String[]).isRight()) == isRight

        where:
        args                                                    | isRight
        []                                                      | false
        [""]                                                    | false
        ["-h"]                                                  | false
        ["-k"]                                                  | false
        ["-s", "gconf.json", "envconf.json", "localhost"]       | true
        ["-h", "-s", "gconf.json", "envconf.json", "localhost"] | false
        ["-s", "gconf.json", "envconf.json"]                    | false
        ["-s", "gconf.json"]                                    | false
        ["-s"]                                                  | false
        ["-m", "gconf.json", "envconf.json", "localhost"]       | true
        ["-sm", "gconf.json", "envconf.json", "localhost"]      | true
        ["-m", "-s", "gconf.json", "envconf.json", "localhost"] | true
    }

}
