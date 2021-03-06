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
package com.twcable.grabbit.tools.environment;

import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.Iterator;

/**
 * Simple base implentation of an {@link Environment}.
 */
@Value
@NonFinal
@SuppressWarnings({"WeakerAccess", "checkstyle:VisibilityModifier", "PMD.CommentDefaultAccessModifier"})
public abstract class AbstractEnvironment implements Environment {
    Iterable<HostInfo> hosts;


    @Override
    public Iterator<HostInfo> iterator() {
        return hosts.iterator();
    }

}
