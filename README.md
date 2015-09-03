# Freenoker - Freemarker with Node.js

用于前端和后端无缝衔接的一个学习项目。

使用socket通信，使freemarker的template的模板通过node.js的server输出。

## 项目进度

### release－0.0.1
实现了通过socket长连接进行模板渲染传递的功能。客户端使用Grunt作为自动化构建工具，在connect插件中使用middleware包裹了node.js的net模块；服务端使用Java的AIO，AsynchronousServerSocketChannel作为server, 并采取了callback模式。

## 项目计划：

- <del>freemarker作为模版框架，并未提供layout的功能。接下来将实现前后端都可用的layout功能。</del>
- 目前版本还没有支持前台mock data。后续需要支持。
- <del>gulp貌似在社区更受好评。将学习glup，并尝试支持。</del>
