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

import java.nio.charset.Charset;
import java.util.Base64;

/**
 * The tuple of user name and password for authentication.
 *
 * @see #basicAuthEncode()
 */
@Value
@SuppressWarnings({"WeakerAccess", "checkstyle:VisibilityModifier", "PMD.CommentDefaultAccessModifier"})
public class UsernameAndPassword {
    String username;
    String password;


    /**
     * Base-64 encode the username and password, suitable for HTTP Basic Authentication.
     */
    public String basicAuthEncode() {
        return Base64.getEncoder().encodeToString((username + ':' + password).getBytes(Charset.defaultCharset()));
    }
}
