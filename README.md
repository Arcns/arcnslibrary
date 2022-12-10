## 基于MVVM的公共库 [![](https://www.jitpack.io/v/Arcns/arcnslibrary.svg)](https://www.jitpack.io/#Arcns/arcnslibrary)


### 地址
```
    implementation 'com.github.Arcns.arcnslibrary:arcns-core:0.3.7-2'
    implementation 'com.github.Arcns.arcnslibrary:arcns-map-gaode:0.3.7-2'
    implementation 'com.github.Arcns.arcnslibrary:arcns-map-baidu:0.3.7-2'
    implementation 'com.github.Arcns.arcnslibrary:arcns-media-audio:0.3.7-2'
    implementation 'com.github.Arcns.arcnslibrary:arcns-media-selector:0.3.7-2'
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
####  悬浮窗相关的方法（goDrawOverlaysSettings、canDrawOverlays）在本library为仅编译（compileOnly），所以如果需要用到悬浮窗相关的方法，需要在主项目的build.gradle中添加：
```
    // 悬浮窗
    implementation 'com.github.czy1121:settingscompat:1.1.4'
```
