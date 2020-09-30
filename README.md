# Android lints
Custom set of android lints rules.

[![](https://jitpack.io/v/kozaxinan/android-lints.svg)](https://jitpack.io/#kozaxinan/android-lints)

### ImmutableDataClassRule - Error
Kotlin data classes should be immutable by design. Having var in a data class is code smell. Use copy() method when instance needs to be modified.

### NetworkLayerImmutableClassRule - Warning
Classes used in network layer should be immutable by design. This lint checks Retrofit interface methods' return type for immutability. Java classes need to have final fields and kotlin data classes needs to have only var fields.

If you want to use this rule, please configure you lint.xml.
```
<issue id="NetworkLayerImmutableClassRule" severity="error" />
```

### NetworkLayerClassSerializedNameRule - Information
Classes used in network layer with Gson should use SerializedName annotation. This lint checks Retrofit interface methods' return type for SerializedName annotation. Adding annotation rule to prevent obfuscation errors. If you don't use Gson for Retrofit, this check is useless. That is why it is `infromation` level. 

If you want to use this rule, please configure you lint.xml. 
```
<issue id="NetworkLayerClassSerializedNameRule" severity="error" />
```

### NetworkLayerClassJsonRule - Information
Classes used in network layer with Moshi should use Json annotation. This lint checks Retrofit interface methods' return type for Json annotation. Adding annotation rule to prevent obfuscation errors. If you don't use Moshi for Retrofit, this check is useless. That is why it is `infromation` level. 

If you want to use this rule, please configure you lint.xml. 
```
<issue id="NetworkLayerClassJsonRule" severity="error" />
```

### NetworkLayerClassJsonClassRule - Information
Classes used in network layer with Moshi should use JsonClass annotation to generate the adaptor class. This lint checks Retrofit interface methods' return type for JsonClass annotation. If you don't use Moshi for Retrofit, this check is useless. That is why it is `infromation` level. 

If you want to use this rule, please configure you lint.xml. 
```
<issue id="NetworkLayerClassJsonRule" severity="error" />
```

# Usage
Library is published to jitpack.io. After adding implementation to your module, lint will include new rules. 

[![](https://jitpack.io/v/kozaxinan/android-lints.svg)](https://jitpack.io/#kozaxinan/android-lints)

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.kozaxinan:android-lints:<UseLatestVersion>'
}
```

# License

Copyright 2020 Kozaxinan - Sinan Kozak

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
