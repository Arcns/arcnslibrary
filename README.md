## 基于MVVM的公共库

### 地址
```
    implementation 'com.github.Arcns.arcnslibrary:arcns-core:0.1.12-3'
    implementation 'com.github.Arcns.arcnslibrary:arcns-map-gaode:0.1.12-3'
    implementation 'com.github.Arcns.arcnslibrary:arcns-map-baidu:0.1.12-3'
```

#### Navigation相关的工具在本library为仅编译（compileOnly），所以如果需要用到Navigation相关的工具，需要在主项目的build.gradle中添加：
```
    // Navigation Kotlin
    // 开启navigation需要配置三个地方：module.gradle页面顶部apply、module.gradle页面dependencies、project.gradle页面的classpath
    def nav_version = "2.2.2"
    implementation "androidx.navigation:navigation-fragment-ktx:$nav_version"
    implementation "androidx.navigation:navigation-ui-ktx:$nav_version"
```

####  弹出框（afollestad）相关的工具在本library为仅编译（compileOnly），所以如果需要用到弹出框相关的工具，需要在主项目的build.gradle中添加：
```
    // 弹出框
    implementation 'com.afollestad.material-dialogs:core:3.3.0'
    implementation 'com.afollestad.material-dialogs:lifecycle:3.3.0'
    implementation 'com.afollestad.material-dialogs:datetime:3.3.0'
    implementation 'com.afollestad.material-dialogs:bottomsheets:3.3.0'
```

####  录音和播放相关的工具在本library为仅编译（compileOnly），所以如果需要用到录音和播放相关的工具，需要在主项目的build.gradle中添加：
```
    // 录音和播放
    implementation 'com.github.CarGuo:GSYRecordWave:2.0.1'
```