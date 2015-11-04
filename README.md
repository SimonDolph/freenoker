# Freenoker - Freemarker with Node.js

用于前端和后端无缝衔接的一个学习项目。

使用socket通信，使freemarker的template的模板通过node.js的server输出。

## 使用方法
1. clone本项目
2. 进入server文件夹，执行mvn package,在target目录下得到freenoker-server-xxx.jar
3. 执行如下命令：`java -jar target/freenoker-server-0.0.1-SNAPSHOT.jar path=${path} enableLayout=${true|false} layout=${layout_path}`  注意把${}里的内容替换掉，支持的参数见下方
4. client文件夹里有demo，包括了basic, grunt, gulp，可根据自己需要选择。

## 参数
1. path, 必填, ftl文件路径，用于freemarker读取模版
2. enableLayout, 可选, 是否启用layout，默认不启用
3. layout, 可选, layout文件，用于freemarker读取layout模板，默认_layout.ftl
4. layoutKey, 可选, 设置layout时的key，默认layout
5. screenContentKey, 可选, 内容在layout文件里的变量名，默认screen_content

## 项目进度

### release－0.0.1
实现了通过socket长连接进行模板渲染传递的功能。客户端使用Grunt作为自动化构建工具，在connect插件中使用middleware包裹了node.js的net模块；服务端使用Java的AIO，AsynchronousServerSocketChannel作为server, 并采取了callback模式。

### release-0.0.2
模板layout支持
gulp支持

## 项目计划：
- 目前版本还没有支持前台mock data。后续需要支持。
