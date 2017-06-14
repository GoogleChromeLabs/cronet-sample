Cronet Sample
===================================

[Cronet](https://chromium.googlesource.com/chromium/src/+/master/components/cronet?autodive=0%2F%2F)
is Chromium's Networking stack packaged as a library. This sample app shows how to use the library.

### Getting Started
---------------
It's a manual procedure to get the Cronet library into a project. To add the libraries to the
sample, please follow the following steps:

1. Go to [Cronet releases website]
          (https://console.cloud.google.com/storage/browser/chromium-cronet/android)
and choose the latest release
2. Navigate to chromium-cronet/android/releasenumber/Release/cronet
3. Get .jar files
4. Download cronet_api.jar, cronet_impl_common_java.jar and cronet_impl_native_java.jar
5. Directly under the "app" directory of your project, create a "libs" directory. Use a shell
command if you like, or use "File|New|Directory" from the menu. But note that you only get
"Directory" as an option if you are in "Project" view, not "Android" view.
"Project" models the local machine's filesystem, but Android is a virtual layout of files
corresponding to the deployed hierarchy.
6. Copy cronet_api.jar cronet_impl_common_java.jar and cronet_impl_native_java.jar
under the libs directory
7. Select the files at once
8. Bring up the context menu and choose "Add as Library"
9. Confirm "OK" at the "Add to module" dialog
10. Update the app/build.gradle file matching the names of the libraries
11. Get .so files
12. Download .so files, currently located
[here](https://console.cloud.google.com/storage/browser/chromium-cronet/android/57.0.2926.0/Release/cronet/libs/)
13. Under "app/src/main" create a directory named "jniLibs"
14. Copy armeabi and armeabi-v7a into jniLibs, which should contain only subdirectories,
not directly a '.so' file. If you typically use an emulator,
copy x86_64 folder into jniLibs folder as well

### License
---------------

```
Copyright 2015 Google, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements. See the NOTICE file distributed with this work for
additional information regarding copyright ownership. The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
