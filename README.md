# Description #

This script starts and monitors a Grabbit content sync.

# Prerequisites #

You must first install the Grabbit AEM package on any instance you wish to sync content to (i.e. a Grabbit client).
 You can download the latest package from bintray:
 https://bintray.com/twcable/aem/Grabbit/view#files

# How to run the script #

From the tools/grabbit-start-monitor root:
`run -h` to view list of options:

```shell
usage: run -[h|s|sm|m] [grabbit-json-file] [env-json-file] [env] [grabbit-ids-file]
 -h,--help                                                                 Show usage information
 -m,--monitor <[env-json-file] [environment] [grabbit-ids-file]>           Monitor grabbit
 -s,--start <[grabbit-json-file] [env-json-file] [environment]>            Start grabbit
 -sm,--start-monitor <[grabbit-json-file] [env-json-file] [environment]>   Start and monitor grabbit
 ```

Example for syncing to your local author:

```shell
run -sm grabbit-author.json localhost.json localhost
```

Example for syncing to your local publish:

```shell
run -sm cgrabbit-publish.json localhost.json localhost
```

# Releasing #

```shell
./gradlew release -Prelease.scope=patch -Prelease.stage=final
```

See [the release plugin documentation](https://github.com/ajoberstar/gradle-git/wiki/Release%20Plugins%201.x)

# API #

https://twcable.github.io/grabbit-cli/docs/groovydoc/

# LICENSE

Copyright 2015 Time Warner Cable, Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
the specific language governing permissions and limitations under the License.
