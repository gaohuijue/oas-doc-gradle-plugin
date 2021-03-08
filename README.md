此插件由 [springdoc-openapi-gradle-plugin](https://github.com/springdoc/springdoc-openapi-gradle-plugin) 改写。

原插件仅支持OAS文件输出，若需要上传到OpenApi文档平台还需增加配置和写脚本， 为简化配置，扩展此插件。

#### build.gradle
```groovy
openApi {
    // OpenApi描述文件输出路径不影响上传到yapi，可以根据需要配置。
    outputDir.set(file("$buildDir/docs"))
    outputFileName.set("swagger.json")
    // 如果项目较大，启动慢，适当加大超时时间，若超时仍没有启动成功，OpenApi描述文件将生成失败。
    waitTimeInSeconds.set(10 * 60)
    // 上传到yapi的配置
    yapiOrigin.set("https://yapi.baidu.com")
    // yapi 项目 token，从yapi的项目的设置页面查看
    yapiProjectToken.set("64a4******************************cea4906c890c98fbfc24cca573ddf")
}
```
