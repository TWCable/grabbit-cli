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

class Utils {

    /**
     * Remove checked-ness from the exception. The same exception is still thrown (checked or unchecked), but this
     * removes the compiler's checks.
     */
    @SuppressWarnings("RedundantTypeArguments")
    public static <T extends Throwable> void throwSoft(T exp) {
        throw Utils.<RuntimeException>uncheck(exp);
    }


    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T uncheck(Throwable throwable) throws T {
        throw (T)throwable;
    }

}
