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

import java.time.OffsetDateTime

class JobStatusSpec extends Specification {
    def uri = URI.create("http://test.test")


    def "JobStatus"() {
        JobStatus jobStatus = new JobStatus(
            uri,
            5700257125617303512L,
            6599420059518545859L,
            OffsetDateTime.parse("2016-06-03T13:19:57+0000", JobStatus.DATE_TIME_FORMATTER),
            OffsetDateTime.parse("2016-06-03T13:19:58+0000", JobStatus.DATE_TIME_FORMATTER),
            "/content/modals",
            1,
            2345L,
            "descr",
            "HUH?",
            true
        )

        when:
        def jobStatusJson = jobStatus.asJson()

        then: "reflexivity of asJson and fromJson"
        jobStatus == JobStatus.fromJson(uri, jobStatusJson)

        and: "check for edge-cases"
        JobStatus.fromJson(uri, null) != null
        JobStatus.fromJson(uri, "") != null
        JobStatus.fromJson(uri, "  \n  ") != null
        JobStatus.fromJson(uri, "{}") != null
        JobStatus.fromJson(uri, "{ \n }") != null
        JobStatus.fromJson(uri, "{\"exitStatus\":{\"exitCode\":\"OTHER\"}}").exitCode == "OTHER"
    }


    def "read Job Status"() {
        def jobStatusStr = '''
{
  "transactionID": 5700257125617303512,
  "jobExecutionId": 6599420059518545859,
  "jcrNodesWritten": -1,
  "exitStatus": {
    "exitDescription": "",
    "exitCode": "UNKNOWN",
    "running": true
  },
  "endTime": null,
  "timeTaken": -1,
  "path": "/content/modals",
  "startTime": "2016-06-03T13:19:57+0000"
}'''
        expect:
        JobStatus.fromJson(uri, jobStatusStr) != null
    }

}
