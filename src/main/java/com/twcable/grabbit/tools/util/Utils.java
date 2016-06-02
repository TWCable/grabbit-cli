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
package com.twcable.grabbit.tools.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.System.lineSeparator;
import static java.nio.charset.Charset.defaultCharset;

/**
 * Useful utility functions.
 */
@SuppressWarnings("WeakerAccess")
public final class Utils {

    private Utils() {
        // utility functions, so there shouldn't be an instance
    }


    /**
     * Remove checked-ness from the exception. The same exception is still thrown (checked or unchecked), but this
     * removes the compiler's checks.
     */
    @SuppressWarnings("RedundantTypeArguments")
    public static <T extends Throwable> void throwSoft(T exp) {
        throw softened(exp);
    }


    /**
     * Remove checked-ness from the exception. The same exception is returned (checked or unchecked), but this
     * removes the compiler's checks.
     */
    @SuppressWarnings("RedundantTypeArguments")
    public static <T extends Throwable> RuntimeException softened(T exp) {
        return Utils.<RuntimeException>uncheck(exp);
    }


    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T uncheck(Throwable throwable) throws T {
        throw (T)throwable;
    }


    /**
     * Write the given string to the Writer, followed by the system newline.
     * If there's an IOException, it's still thrown, but it's no longer "checked."
     */
    public static void writeLine(Writer writer, String line) {
        try {
            writer.write(line);
            writer.write(lineSeparator());
        }
        catch (IOException e) {
            throwSoft(e);
        }
    }


    /**
     * "Quietly" flush and close the writer. If there's an IOException, it's still thrown, but it's no longer "checked."
     */
    public static void flushAndClose(@Nullable Writer writer) {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            }
            catch (IOException e) {
                throwSoft(e);
            }
        }
    }


    /**
     * "Quietly" close the instance. If there's an IOException, it's still thrown, but it's no longer "checked."
     */
    public static void close(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException e) {
                throwSoft(e);
            }
        }
    }


    /**
     * Returns a {@link Consumer} that simply invokes {@link Utils#writeLine(Writer, String)}.
     *
     * The currying is useful to get around "Writer is not final" compiler errors for lambdas in those situations
     * where you know it's safe to do so.
     */
    public static Consumer<String> lineWriter(Writer writer) {
        return line -> writeLine(writer, line);
    }


    /**
     * Copies the {@link InputStream} to a String. The stream is closed upon completion.
     */
    public static String toString(InputStream inputStream) throws IOException {
        val sink = new ByteArrayOutputStream();
        copy(inputStream, sink);
        return sink.toString("UTF-8");
    }


    /**
     * Copies the {@link InputStream} into the {@link OutputStream}, closing both when complete.
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    public static void copy(InputStream source, OutputStream sink) throws IOException {
        final byte[] buf = new byte[8 * 1024];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
        }
        source.close();
        sink.flush();
        sink.close();
    }


    /**
     * Read in a configuration file -- either YAML or JSON -- and return its results as a Map.
     */
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
    public static Map<String, ?> configAsMap(File configFile) {
        val yaml = new Yaml();
        try {
            val in = new FileInputStream(configFile);
            val reader = new InputStreamReader(in, defaultCharset());
            return yaml.loadAs(reader, Map.class);
        }
        catch (FileNotFoundException e) {
            throw softened(e);
        }
    }
}
