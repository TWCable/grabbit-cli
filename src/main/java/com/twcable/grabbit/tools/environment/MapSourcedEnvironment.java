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

import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.twcable.grabbit.tools.environment.HostInfo.NodeType.AUTHOR;
import static com.twcable.grabbit.tools.environment.HostInfo.NodeType.PUBLISHER;
import static com.twcable.grabbit.tools.util.Utils.softened;

/**
 * Collection of information about an AEM environment (authors & publishers).
 */
@SuppressWarnings({"WeakerAccess", "RedundantTypeArguments"})
public class MapSourcedEnvironment extends AbstractEnvironment {

    public MapSourcedEnvironment(Iterable<HostInfo> hosts) {
        super(hosts);
    }


    // TODO: Abstract this out to be able to be created from any source
    public static MapSourcedEnvironment createFromMap(Map<String, ?> env) {
        val credentials =
            new UsernameAndPassword((@NonNull String)env.get("username"), (@NonNull String)env.get("password"));
        val protocol = (@NonNull String)env.get("protocol");

        val allHosts = allHosts(env, protocol, credentials);

        return new MapSourcedEnvironment(allHosts);
    }


    @SuppressWarnings("unchecked")
    private static Collection<HostInfo> allHosts(Map<String, ?> env, String protocol, UsernameAndPassword credentials) {
        val domainName = domainName(env);

        final Function<HostInfo.NodeType, Function<Map.Entry<String, String>, HostInfo>> f =
            nodeType ->
                mapEntry -> hostInfo(nodeType, credentials, protocol, domainName, mapEntry);

        val allHosts = hostInfoFromMap((Map<String, String>)env.get("authors"), f.apply(AUTHOR));
        allHosts.addAll(hostInfoFromMap((Map<String, String>)env.get("publishers"), f.apply(PUBLISHER)));
        return allHosts;
    }


    @SuppressWarnings("argument.type.incompatible")
    private static URI baseUri(String protocol, @Nullable String domainName, String hostname, int port) {
        val host = (domainName != null) ? (hostname + "." + domainName) : hostname;

        try {
            return new URI(protocol, null, host, port, null, null, null);
        }
        catch (URISyntaxException e) {
            throw softened(e);
        }
    }


    /**
     * Assumes the authors are clustered, so returns the first author it has, at random.
     */
    @Override
    public Stream<HostInfo> authors() {
        return super.authors().limit(1);
    }


    @SuppressWarnings("checkstyle:MultipleStringLiterals")
    private static @Nullable String domainName(Map<String, ?> env) {
        return (env.get("domainName") != null && !env.get("domainName").toString().trim().isEmpty()) ?
            (String)env.get("domainName") : null;
    }


    @SuppressWarnings("checkstyle:NeedBraces")
    private static Collection<HostInfo> hostInfoFromMap(@Nullable Map<String, String> map,
                                                        Function<Map.Entry<String, String>,
                                                            HostInfo> entryHostInfoFunc) {
        if (map == null) return Collections.emptyList();
        return map.entrySet().stream().
            map(entryHostInfoFunc).
            collect(Collectors.<@NonNull HostInfo>toList());
    }


    private static HostInfo hostInfo(HostInfo.NodeType nodeType, UsernameAndPassword credentials, String protocol,
                                     @Nullable String domainName, Map.Entry<String, String> mapEntry) {
        val baseUri = baseUri(protocol, domainName, mapEntry.getKey(), Integer.parseInt(mapEntry.getValue()));
        return new HostInfo(nodeType, baseUri, credentials);
    }

}
