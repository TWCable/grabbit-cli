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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class CliOptions {
    public final boolean start;
    public final boolean monitor;
    public final String grabbitConfFile;
    public final String envConfFile;
    public final String environmentName;
    public final String idsFile;


    private CliOptions(boolean start, boolean monitor, String grabbitConfFile, String envConfFile, String environmentName, String idsFile) {
        this.start = start;
        this.monitor = monitor;
        this.grabbitConfFile = grabbitConfFile;
        this.envConfFile = envConfFile;
        this.environmentName = environmentName;
        this.idsFile = idsFile;
    }


    static CliOptions create(String[] args) {
        Option help = new Option("h", "help", false, "Show usage information");
        Option start = new Option("s", "start", false, "Start Grabbit");
        Option monitor = new Option("m", "monitor", false, "Monitor Grabbit");

        Options options = new Options();
        options.addOption(help);
        options.addOption(start);
        options.addOption(monitor);

        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            final List<String> argList = line.getArgList();

            if (line.hasOption('h') || !((line.hasOption('s') || line.hasOption('m')) && argList.size() == 3)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("grabbit-cli -[h|s|sm|m] [grabbit-json-file] [env-json-file] [env] [grabbit-ids-file]",
                    "Starts and/or monitors jobs on the Grabbit client", options, "", false);
                return null;
            }

            if (line.hasOption('s')) {
                return new CliOptions(true, line.hasOption('m'), argList.get(0), argList.get(1), argList.get(2), null);
            }
            else {
                return new CliOptions(false, true, null, argList.get(0), argList.get(1), argList.get(2));
            }
        }
        catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            return null;
        }
    }
}
