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

import com.twcable.grabbit.tools.util.Either;
import lombok.Value;
import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * The command line interface options parser.
 *
 * @see GrabbitCli
 */
@Value
@SuppressWarnings({"WeakerAccess", "checkstyle:VisibilityModifier", "PMD.CommentDefaultAccessModifier"})
final class CliOptions {
    boolean start;
    boolean monitor;
    @Nullable String jobsConfFile;
    String envConfFile;
    String environmentName;
    @Nullable String idsFile;


    /**
     * Parse the arguments.
     *
     * @param args the command line arguments
     * @return Right if successfully parsed; otherwise Left with the error message/help
     */
    @SuppressWarnings("PMD.UseVarargs")
    public static Either<String, CliOptions> create(String[] args) {
        val help = new Option("h", "help", false, "Show usage information");
        val start = new Option("s", "start", false, "Start Grabbit");
        val monitor = new Option("m", "monitor", false, "Monitor Grabbit");

        val options = new Options();
        options.addOption(help);
        options.addOption(start);
        options.addOption(monitor);

        val parser = new DefaultParser();
        try {
            // parse the command line arguments
            val line = parser.parse(options, args);

            if (line.hasOption('h') || !hasValidOptions(line)) {
                val formatter = new HelpFormatter();
                val stringWriter = new StringWriter();
                formatter.printHelp(new PrintWriter(stringWriter), formatter.getWidth(),
                    "grabbit-cli -[h|s|sm|m] [grabbit-job-config-file] [env-config-file] [env] [job-ids-cache-file]",
                    "Starts and/or monitors jobs on the Grabbit client", options,
                    formatter.getLeftPadding(), formatter.getDescPadding(), "", false);
                return Either.left(stringWriter.toString());
            }

            val argList = line.getArgList();

            if (line.hasOption('s')) {
                return Either.right(new CliOptions(true, line.hasOption('m'), argList.get(0),
                    argList.get(1), argList.get(2), null));
            }
            else {
                return Either.right(new CliOptions(false, true, null, argList.get(0), argList.get(1), argList.get(2)));
            }
        }
        catch (ParseException exp) {
            return Either.left("Parsing failed.  Reason: " + exp.getMessage());
        }
    }


    private static boolean hasValidOptions(CommandLine line) {
        val argList = line.getArgList();
        return (line.hasOption('s') || line.hasOption('m')) && argList.size() == 3;
    }
}
