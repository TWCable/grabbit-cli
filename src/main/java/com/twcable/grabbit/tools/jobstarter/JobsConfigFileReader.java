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
package com.twcable.grabbit.tools.jobstarter;

import com.twcable.grabbit.tools.environment.HostInfo;
import com.twcable.grabbit.tools.util.Utils;
import lombok.val;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static com.twcable.grabbit.tools.environment.HostInfo.NodeType.AUTHOR;
import static com.twcable.grabbit.tools.environment.HostInfo.NodeType.PUBLISHER;

/**
 * Reads and parses the Grabbit Client configuration file.
 *
 * For the most part, this is used "raw" to be passed to the Grabbit Client. However this also parses the file
 * to determine what {@link HostInfo.NodeType} this is for.
 */
@SuppressWarnings({"WeakerAccess", "checkstyle:MultipleStringLiterals"})
public class JobsConfigFileReader {
    private final File grabbitJobConfigFile;


    public JobsConfigFileReader(File grabbitJobConfigFile) throws FileNotFoundException {
        if (!grabbitJobConfigFile.exists()) {
            throw new FileNotFoundException("\"" + grabbitJobConfigFile.getAbsolutePath() + "\" could not be found");
        }
        this.grabbitJobConfigFile = grabbitJobConfigFile;
    }


    /**
     * Returns the {@link HostInfo.NodeType} that corresponds to the "clientNodeType" key at the top level
     * of the configuration.
     */
    public HostInfo.NodeType configNodeType() {
        val clientNodeType = (String)Utils.configAsMap(grabbitJobConfigFile).get("clientNodeType");
        if (clientNodeType == null) {
            throw new IllegalStateException("Could not find key \"clientNodeType\" at the top level of \"" +
                grabbitJobConfigFile.getAbsolutePath() + "\"");
        }

        switch (clientNodeType.toLowerCase()) {
            case "author":
                return AUTHOR;
            case "publish":
            case "publisher":
                return PUBLISHER;
            default:
                throw new IllegalStateException("Could not map \"" + clientNodeType + "\" to a NodeType");
        }
    }


    /**
     * Returns an {@link InputStream} for the configuration file. If there is a {@link FileNotFoundException} it is
     * "softened."
     *
     * @see Utils#softened(Throwable)
     */
    public InputStream inputStream() {
        try {
            return new FileInputStream(grabbitJobConfigFile);
        }
        catch (FileNotFoundException e) {
            // only possible if the file disappears while this is running
            throw Utils.softened(e);
        }
    }

}
