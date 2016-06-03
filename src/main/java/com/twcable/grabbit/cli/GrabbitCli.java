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
package com.twcable.grabbit.cli;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.regex.qual.Regex;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.twcable.grabbit.cli.Utils.throwSoft;

@SuppressWarnings({"WeakerAccess", "RedundantTypeArguments"})
public class GrabbitCli {
    private static final int POLLING_TIME = Integer.parseInt(System.getProperty("pollTime", "15000"));
    private static final Pattern JOB_IDS_PATTERN = Pattern.compile((@Regex(1) String)"^\\s*\\[(?<jobIds>[\\d,\\s]*)\\]\\s*$", Pattern.MULTILINE);


    public static void main(String[] args) throws IOException {
        final CliOptions cliOptions = CliOptions.create(args);

        if (cliOptions == null) return;

        run(cliOptions);
    }


    @SuppressWarnings("RedundantCast")
    public static void run(CliOptions options) throws IOException {
        if (options.start) {
            startGrabbit((@NonNull String)options.grabbitConfFile, options.envConfFile, options.environmentName, options.monitor);
        }
        else {
            monitorGrabbit(options.envConfFile, options.environmentName, new File((@NonNull String)options.idsFile));
        }
    }


    @SuppressWarnings("unchecked")
    private static void startGrabbit(final String grabbitJson, String envJsonFile, final String environment,
                                     final boolean monitor) throws IOException {
        final Map<String, ?> env = getEnv(envJsonFile, environment);
        final Map<String, String> instances = getInstances(env, grabbitJson);

        final File file = new File("grabbitIds.out");
        Files.deleteIfExists(file.toPath());

        final String domainName = domainName(env);

        try (FileWriter jobIdsWriter = new FileWriter(file)) {
            instances.forEach((hostname, portStr) -> startJob(grabbitJson, env, jobIdsWriter, domainName, hostname, portStr, monitor));
        }

        if (monitor) {
            monitorGrabbit(envJsonFile, environment, file);
        }
    }


    private static @Nullable String domainName(Map<String, ?> env) {
        return (env.get("domainName") != null && !env.get("domainName").toString().trim().isEmpty()) ?
            (String)env.get("domainName") : null;
    }


    private static URL baseUrl(String host, String portStr, String protocol) throws MalformedURLException {
        return new URL(protocol, host, Integer.parseInt(portStr), "");
    }


    private static void startJob(String grabbitJson, Map<String, ?> env, FileWriter jobIdsWriter,
                                 @Nullable String domainName, String hostname, String portStr, boolean monitor) {
        final String host = (domainName != null) ? (hostname + "." + domainName) : hostname;

        final String output;
        final URL clientUrl, baseUrl;
        try {
            final String protocol = (@NonNull String)env.get("protocol");
            baseUrl = baseUrl(host, portStr, protocol);
            clientUrl = clientUrl(baseUrl);
            final InputStream inputStream = startJobOnClient(grabbitJson, env, clientUrl);
            output = toString(inputStream).trim();
        }
        catch (IOException e) {
            throwSoft(e);
            return; // impossible
        }

        writeOutJobsIds(jobIdsWriter, output, baseUrl, monitor);
    }


    @SuppressWarnings("RedundantCast")
    private static void writeOutJobsIds(FileWriter jobIdsWriter, String output, URL baseUrl, boolean monitor) {
        // the output from starting a job looks like "[123,125]"
        final Matcher matcher = JOB_IDS_PATTERN.matcher(output);
        if (matcher.matches()) {
            final String jobIdsStr = (@NonNull String)matcher.group("jobIds");
            final List<String> jobIds = Arrays.stream(jobIdsStr.split(",")).
                map(String::trim).
                collect(Collectors.<@NonNull String>toList());

            jobIds.forEach(jobId -> writeOutJobId(jobIdsWriter, baseUrl, jobId));
            if (!monitor) {
                jobIds.forEach(jobId -> System.out.println(baseUrl + ", " + jobId));
            }
        }
        else {
            throw new IllegalStateException("Could not parse job ids from: " + output);
        }
    }


    private static Map<String, ?> getEnv(String envJsonFile, String environment) throws FileNotFoundException {
        final Map<String, ?> json = getJsonConfig(envJsonFile);
        @SuppressWarnings("unchecked")
        final Map<String, ?> env = (Map<String, ?>)json.get(environment);
        if (env == null)
            throw new IllegalArgumentException("Can not find \"" + environment + "\" in \"" + envJsonFile + "\"");
        return env;
    }


    private static void writeOutJobId(FileWriter jobIdsWriter, URL clientUrl, String jobId) {
        try {
            jobIdsWriter.write(clientUrl + "," + jobId + ",RUNNING\n");
        }
        catch (IOException e) {
            throwSoft(e);
        }
    }


    private static String toString(InputStream inputStream) throws IOException {
        final ByteArrayOutputStream sink = new ByteArrayOutputStream();
        copy(inputStream, sink);
        return sink.toString("UTF-8");
    }


    private static BufferedInputStream startJobOnClient(String grabbitJson, Map<String, ?> env, URL url) throws IOException {
        System.out.println("Connecting to " + url);
        HttpURLConnection httpCon = (HttpURLConnection)url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");
        httpCon.setRequestProperty("Content-Type", "application/json");
        setBasicAuth(httpCon, (@NonNull String)env.get("username"), (@NonNull String)env.get("password"));

        final FileInputStream fileInputStream = new FileInputStream(grabbitJson);
        copy(fileInputStream, httpCon.getOutputStream());
        return new BufferedInputStream(httpCon.getInputStream());
    }


    private static void setBasicAuth(HttpURLConnection httpCon, String username, String password) {
        String encoding = Base64.getEncoder().encodeToString((username + ':' + password).getBytes());
        httpCon.setRequestProperty("Authorization", "Basic " + encoding);
    }


    private static BufferedInputStream jobStatusOnClient(URL baseUrl, String jobId, Map<String, ?> env) throws IOException {
        final URL url = new URL(baseUrl, "/grabbit/job/" + jobId + ".json");
        System.out.println("Connecting to " + url);
        HttpURLConnection httpCon = (HttpURLConnection)url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("GET");
        // httpCon.setRequestProperty("Content-Type", "application/json");
        setBasicAuth(httpCon, (@NonNull String)env.get("username"), (@NonNull String)env.get("password"));
        return new BufferedInputStream(httpCon.getInputStream());
    }


    private static URL clientUrl(URL baseUrl) throws MalformedURLException {
        return new URL(baseUrl, "/grabbit/job");
    }


    private static void copy(InputStream source, OutputStream sink) throws IOException {
        byte[] buf = new byte[8 * 1024];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
        }
        source.close();
        sink.flush();
        sink.close();
    }


    @SuppressWarnings("RedundantCast")
    public static void monitorGrabbit(String envJsonFile, final String environment, final File file) {
        final long startTime = System.currentTimeMillis();
        boolean hasRunningJobs = true;

        while (hasRunningJobs) {
            System.out.println("\n================= POLLING GRABBIT JOBS ================");
            System.out.println("host | jobId | status");

            final List<String> jobResultStrs = new ArrayList<>();

            try {
                final Map<String, ?> env = getEnv(envJsonFile, environment);

                final BufferedReader fileReader = new BufferedReader(new FileReader(file));
                try {
                    fileReader.lines().forEach(line -> {
                        System.out.println(line);

                        String[] data = line.split(",");
                        final String location = data[0].trim();
                        final String jobId = data[1].trim();
                        String status = data[2].trim();
                        if (status.equals("RUNNING")) {
                            try {
                                System.out.println("Polling Grabbit job " + jobId + " on " + location);
                                final BufferedInputStream jobStatusStream = jobStatusOnClient(new URL(location), jobId, env);
                                final String jobStatusStr = toString(jobStatusStream);
                                System.out.println(jobStatusStr);

                                // may be needed for psuedo-jobs ids like "all", but that's not used here...
                                // jobStatusStr = jobStatusStr.replaceFirst("\\[", "").replaceFirst("\\]", ""));

                                jobResultStrs.add(jobStatusStr);
                            }
                            catch (IOException e) {
                                throwSoft(e);
                            }
                        }
                    });
                }
                finally {
                    try {
                        fileReader.close();
                    }
                    catch (IOException e) {
                        throwSoft(e);
                    }
                }
            }
            catch (FileNotFoundException e) {
                throwSoft(e);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, ?>> jobResultMaps = jobResultStrs.stream().
                <@NonNull Map<String, ?>>map(jobResultStr -> (Map<String, Object>)new Yaml().loadAs(jobResultStr, Map.class)).
                collect(Collectors.<@NonNull Map<String, ?>>toList());

            final List<Map<String, ?>> completedJobs = jobResultMaps.stream().
                filter(GrabbitCli::jobIsCompleted).
                peek(jobMap -> updateFile(file, (@Regex String)jobMap.get("jobExecutionId") + ",RUNNING", (@Regex String)jobMap.get("jobExecutionId") + ",COMPLETE")).
                collect(Collectors.<@NonNull Map<String, ?>>toList());

            if (!completedJobs.isEmpty()) {
                System.out.println("\n====================== COMPLETED =====================");
                completedJobs.forEach(GrabbitCli::printJobStatus);
            }

            final List<Map<String, ?>> failedJobs = jobResultMaps.stream().
                filter(GrabbitCli::jobIsFailed).
                peek(jobMap -> updateFile(file, (@Regex String)jobMap.get("jobExecutionId") + ",RUNNING", (@Regex String)jobMap.get("jobExecutionId") + ",FAILED")).
                collect(Collectors.<@NonNull Map<String, ?>>toList());

            if (!failedJobs.isEmpty()) {
                System.out.println("\n======================= FAILED =======================");
                failedJobs.forEach(GrabbitCli::printJobStatus);
            }

            final List<Map<String, ?>> runningJobs = jobResultMaps.stream().
                filter(GrabbitCli::jobIsRunning).
                collect(Collectors.<@NonNull Map<String, ?>>toList());

            if (!runningJobs.isEmpty()) {
                System.out.println("\n====================== RUNNING =======================");
                runningJobs.forEach(GrabbitCli::printJobStatus);

                sleep(POLLING_TIME);
            }
            else {
                hasRunningJobs = false;
            }
        }

        long currentTime = System.currentTimeMillis();
        final double scriptTime = (currentTime - startTime) / 60 / 1000.0;
        System.out.println("\n=======================================================");
        System.out.println("=== ALL JOBS COMPLETED - Script time: " + scriptTime + " min");
        System.out.println("=======================================================");
    }


    private static void sleep(int pollingTime) {
        System.out.println("\n====================== Sleeping for " + pollingTime + " ms =======================");
        try {
            Thread.sleep(pollingTime);
        }
        catch (InterruptedException e) {
            throwSoft(e);
        }
    }


    private static boolean jobIsRunning(Map<String, ?> jobMap) {
        return jobMap.containsKey("exitStatus") &&
            Objects.equals(Boolean.TRUE, ((@NonNull Map)jobMap.get("exitStatus")).get("running"));
    }


    private static boolean jobIsCompleted(Map<String, ?> jobMap) {
        return jobMap.containsKey("exitStatus") &&
            Objects.equals(Boolean.FALSE, ((@NonNull Map)jobMap.get("exitStatus")).get("running")) &&
            Objects.equals("COMPLETED", ((@NonNull Map)jobMap.get("exitStatus")).get("exitCode"));
    }


    private static boolean jobIsFailed(Map<String, ?> jobMap) {
        return jobMap.containsKey("exitStatus") &&
            Objects.equals(Boolean.FALSE, ((@NonNull Map)jobMap.get("exitStatus")).get("running")) &&
            Objects.equals("FAILED", ((@NonNull Map)jobMap.get("exitStatus")).get("exitCode"));
    }


    public static void updateFile(File file, @Regex String patternToFind, @Regex String patternToReplace) {
        final Path path = file.toPath();
        try {
            final List<String> lines = Files.readAllLines(path);
            final BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.WRITE);
            lines.stream().
                map(line -> line.replaceAll(patternToFind, patternToReplace)).
                forEach(line -> writeLine(writer, line));
        }
        catch (IOException e) {
            throwSoft(e);
        }
    }


    private static void writeLine(BufferedWriter writer, String line) {
        try {
            writer.write(line);
            writer.newLine();
        }
        catch (IOException e) {
            throwSoft(e);
        }
    }


    public static void printJobStatus(final Map<String, ?> job) {
        System.out.println("job: " + job.get("jobExecutionId"));
        System.out.println("startTime: " + job.get("startTime"));
        System.out.println("path: " + job.get("path"));
        System.out.println("status: " + ((@NonNull Map)job.get("exitStatus")).get("exitCode"));
        System.out.println("running: " + ((@NonNull Map)job.get("exitStatus")).get("running"));
        System.out.println("timeTaken: " + job.get("timeTaken"));
        System.out.println("jcrNodesWritten: " + job.get("jcrNodesWritten"));
        System.out.println("---");
    }


    /**
     * Returns a mapping of host name to port from the environments file for the type of client specified in the Grabbit configuration file.
     *
     * @return never null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getInstances(final Map<String, ?> env, final String grabbitJsonFilename) throws FileNotFoundException {
        final String grabbitNodeType = (String)getJsonConfig(grabbitJsonFilename).get("clientNodeType");
        final Map<String, String> authors = (env == null ? null : (Map<String, String>)env.get("authors"));
        final Map<String, String> publishers = (env == null ? null : (Map<String, String>)env.get("publishers"));

        if (Objects.equals(grabbitNodeType, "author") && authors != null) {
            final String firstAuthorKey = authors.keySet().iterator().next();
            return Collections.singletonMap(firstAuthorKey, authors.get(firstAuthorKey));
        }
        else {
            if (publishers == null)
                throw new IllegalStateException("There are no authors or publishers defined in " + grabbitJsonFilename + ".");
            if (Objects.equals(grabbitNodeType, "publish"))
                return publishers;
            else
                throw new IllegalStateException("There are no valid instance of type " + grabbitNodeType + " for " + env + ".");
        }
    }


    @SuppressWarnings("unchecked")
    private static Map<String, ?> getJsonConfig(String jsonConfigFileName) throws FileNotFoundException {
        final Yaml yaml = new Yaml();
        final File jsonFile = new File(jsonConfigFileName);
        final FileReader fileReader = new FileReader(jsonFile);
        return yaml.loadAs(fileReader, Map.class);
    }

}
