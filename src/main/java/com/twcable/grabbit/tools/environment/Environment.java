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

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The collection of host information for a particular environment. (e.g., development, UAT, production, etc.)
 */
@SuppressWarnings("WeakerAccess")
public interface Environment extends Iterable<HostInfo> {

    /**
     * All of the hosts of a particular type in the environment.
     *
     * @param nodeType AUTHOR or PUBLISHER
     * @throws IllegalStateException if the nodeType is unknown
     */
    default Stream<HostInfo> hostsOfType(HostInfo.NodeType nodeType) throws IllegalStateException {
        switch (nodeType) {
            case AUTHOR:
                return authors();
            case PUBLISHER:
                return publishers();
            default:
                throw new IllegalStateException("Don't know what to do with " + nodeType);
        }
    }

    /**
     * All of the hosts in the environment.
     */
    default Stream<HostInfo> allHosts() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    default Spliterator<HostInfo> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.IMMUTABLE | Spliterator.NONNULL);
    }

    /**
     * All the authors to create copy jobs on.
     *
     * By default, this returns every defined author.
     *
     * You may want to override this to return just cluster masters or the like.
     */
    default Stream<HostInfo> authors() {
        return allHosts().
            filter(hostInfo -> hostInfo.nodeType() == HostInfo.NodeType.AUTHOR);
    }


    /**
     * All the publishers to create copy jobs on.
     *
     * By default, this returns every defined publisher.
     *
     * You may want to override this to return just cluster masters or the like.
     */
    default Stream<HostInfo> publishers() {
        return allHosts().
            filter(hostInfo -> hostInfo.nodeType() == HostInfo.NodeType.PUBLISHER);
    }

}
