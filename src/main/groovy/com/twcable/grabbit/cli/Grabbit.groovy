/*
 * Copyright 2014-2015 Time Warner Cable, Inc.
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
package com.twcable.grabbit.cli

import groovy.json.JsonSlurper

class Grabbit {
    final static int POLLING_TIME = Integer.parseInt(System.getProperty("pollTime", "15000"))
    final static long START_TIME = System.currentTimeMillis()
    private static def runningJobs
    private static def completedJobs
    private static def failedJobs


    static void main(String[] args) {

        def cli = new CliBuilder(usage: "-[h|s|sm|m] [grabbit-json-file] [env-json-file] [env] [grabbit-ids-file]", width: 200)
        cli.with {
            h longOpt: 'help', 'Show usage information'
            s longOpt: 'start', args: 3, argName: '[grabbit-json-file] [env-json-file] [environment]', 'Start grabbit'
            sm longOpt: 'start-monitor', args: 3, argName: '[grabbit-json-file] [env-json-file] [environment]', 'Start and monitor grabbit'
            m longOpt: 'monitor', args: 3, argName: '[env-json-file] [environment] [grabbit-ids-file]', 'Monitor grabbit'
        }

        def options = cli.parse(args)

        if (options.h) {
            cli.usage()
            //usage: groovy Grabbit -[h|s|sm|m] [grabbit-json-file] [env-json-file] [env] [grabbit-ids-file]
            // -h,--help                                                                 Show usage information
            // -m,--monitor <[env-json-file] [environment] [grabbit-ids-file]>           Monitor grabbit
            // -s,--start <[grabbit-json-file] [env-json-file] [environment]>            Start grabbit
            // -sm,--start-monitor <[grabbit-json-file] [env-json-file] [environment]>   Start and monitor grabbit
            return
        }

        if (options.s)
            startGrabbit(args[1], args[2], args[3], false)

        else if (options.m)
            monitorGrabbit(args[1], args[2], args[3])

        else if (options.sm)
            startGrabbit(args[1], args[2], args[3], true)
    }


    static def startGrabbit(def grabbitJson, def envJsonFile, def environment, Boolean monitor) {
        def json = getJsonConfig(envJsonFile)
        def env = json."${environment}"
        def instances = getInstances(env, grabbitJson)

        File file = new File("grabbitIds.out")
        if (file.exists())
            file.delete()

        instances.each { hostname, port ->
            def location
            if (env?.domainName)
                location = "${env.protocol}://${hostname}.${env.domainName}:${port}"
            else
                location = "${env.protocol}://${hostname}:${port}"

            def grabbitCmd = "curl -v -X PUT -H 'Content-Type: application/json' -d @${grabbitJson} -u ${env.username}:${env.password} ${location}/grabbit/job"

            String results = runCmd(grabbitCmd, "Started Grabbit jobs on ${location}")

            def jobIds = results.replaceAll("\\[", "").replaceAll("\\]", "").split(',').collect { it }

            file.withWriterAppend { out ->
                jobIds.each { id ->
                    out << "${location},${id},RUNNING\n"
                }
            }

            if (!monitor) {
                jobIds.each {
                    println "${location}, ${it}"
                }
            }
        }

        if (!monitor)
            System.exit(0)
        else
            monitorGrabbit(envJsonFile, environment, file)
    }


    static def monitorGrabbit(def envJsonFile, def environment, String file) {
        monitorGrabbit(envJsonFile, environment, new File(file))
    }


    static def monitorGrabbit(def envJsonFile, def environment, File file) {
        def resultsInfo
        String jobResults
        def json = getJsonConfig(envJsonFile)
        def env = json."${environment}"

        println "\n================= POLLING GRABBIT JOBS ================"
        println "host | jobId | status"
        file.eachLine { line ->
            println line
            def data = line.split(",")
            def location = data[0]
            def jobId = data[1]
            def status = data[2]
            def grabbitCmd = "curl -u ${env.username}:${env.password} --request GET ${location}/grabbit/job/${jobId}.json"
            String jobResult

            if (status == "RUNNING") {
                jobResult = runCmd(grabbitCmd, "Polling Grabbit jobs on ${location}")
                jobResult = jobResult.replaceFirst("\\[", "").replaceFirst("\\]", "")

                if (jobResults)
                    jobResults = "${jobResults},${jobResult}"
                else
                    jobResults = "${jobResult}"
            }
        }

        resultsInfo = new JsonSlurper().parseText("[${jobResults}]")

        def currentJobs = resultsInfo.findAll { it?.exitStatus?.running == true }
        runningJobs = currentJobs

        currentJobs = resultsInfo.findAll {
            it?.exitStatus?.running == false && it?.exitStatus?.exitCode == "COMPLETED"
        }
        completedJobs = completedJobs ? completedJobs + currentJobs : currentJobs
        completedJobs?.each {
            updateFile(file, "${it.jobExecutionId},RUNNING", "${it.jobExecutionId},COMPLETE")
        }

        currentJobs = resultsInfo.findAll { it?.exitStatus?.running == false && it?.exitStatus?.exitCode == "FAILED" }
        failedJobs = failedJobs ? failedJobs + currentJobs : currentJobs
        failedJobs?.each {
            updateFile(file, "${it.jobExecutionId},RUNNING", "${it.jobExecutionId},FAILED")
        }

        if (completedJobs) {
            println "\n====================== COMPLETED ======================"
            completedJobs.each { printJobStatus(it) }
        }

        if (failedJobs) {
            println "\n======================== FAILED ======================="
            failedJobs.each { printJobStatus(it) }
        }

        if (runningJobs) {
            println "\n======================= RUNNING ======================="
            runningJobs.each { printJobStatus(it) }

            println "\n================ Sleeping for ${POLLING_TIME} ms ================"
            sleep POLLING_TIME
            monitorGrabbit(envJsonFile, environment, file)
        }
        else {
            endScript(0)
        }
    }


    static def updateFile(File file, String patternToFind, String patternToReplace) {
        file.write(file.text.replaceAll(patternToFind, patternToReplace))
    }


    static def endScript(int exitCode) {
        def currentTime = System.currentTimeMillis()
        double scriptTime = (currentTime - START_TIME) / 60 / 1000.0;
        if (exitCode == 0) {
            println "\n======================================================="
            println "=== ALL JOBS COMPLETED - Script time: ${scriptTime} min"
            println "======================================================="
        }

        System.exit(exitCode)
    }


    static def printJobStatus(def job) {
        println "job: ${job.jobExecutionId}"
        println "startTime: ${job.startTime}"
        println "path: ${job.path}"
        println "status: ${job.exitStatus.exitCode}"
        println "running: ${job.exitStatus.running}"
        println "timeTaken: ${job.timeTaken}"
        println "jcrNodesWritten: ${job.jcrNodesWritten}"
        println "---"
    }


    static def getInstances(def env, def grabbitJson) {
        def grabbitNodeType = getJsonConfig(grabbitJson).clientNodeType
        def authors = env?.authors
        def publishers = env?.publishers
        def firstAuthor

        if (grabbitNodeType == "author" && authors) {
            def firstAuthorKey = authors.keySet().first()
            firstAuthor = [((String)firstAuthorKey): ((String)"${authors[firstAuthorKey]}")]

            return firstAuthor
        }
        else {
            if (!publishers) throw new Exception("There are no authors or publishers defined in ${grabbitJson}.")
            if (grabbitNodeType == "publish")
                return publishers
            else
                throw new Exception("There are no valid instance of type ${grabbitNodeType} for ${env}.")
        }
    }


    static def getJsonConfig(def jsonConfigFile) {
        File jsonFile = new File(jsonConfigFile)
        def fileText = jsonFile.getText()
        def json = new JsonSlurper().parseText(fileText)
        return json
    }


    static def runCmd(def cmd, def action) {
        def resultList = shellCommand(cmd, ".")

        if (resultList[0] != 0) {
            println "\n*** ERROR: There was a problem with the ${action}, aborting..."
            println "> ${cmd}"
            println resultList[2]
            endScript(1)
        }
        else
            return resultList[1]
    }


    static def shellCommand(String command, String directory) {
        File workingDir = new File(directory)
        String[] commandArray = ["bash", "-c", command]

        def process = new ProcessBuilder(commandArray).directory(workingDir).start()
        InputStream is = process.getInputStream()
        InputStream err = process.getErrorStream()
        ThreadedStreamProcessor isHandler = new ThreadedStreamProcessor(is)
        ThreadedStreamProcessor errHandler = new ThreadedStreamProcessor(err)

        isHandler.start()
        errHandler.start()

        process.waitFor();
        def exitValue = process.exitValue()
        isHandler.interrupt()
        errHandler.interrupt()
        isHandler.join()
        errHandler.join()

        def stdout = isHandler?.getOutput()
        def stderr = errHandler?.getOutput()

        return [exitValue, stdout, stderr]
    }
}
