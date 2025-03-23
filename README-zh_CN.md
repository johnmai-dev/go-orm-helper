[English](./README.md) | 简体中文 | [日本語](./README-ja_JP.md) | [한국어](./README-ko_KR.md)

<div align="center">
    <img src="https://blog.johnmai.top/go-orm-helper/src/main/resources/icons/icon64x64.svg" alt="Go ORM Helper"/>
    <h1 align="center">Go ORM 助手</h1>
</div>

<p align="center">一个为了让你在 GoLand 上写 ORM 能自动补全数据库字段、Tag、生成Struct 的插件。支持：Gorm、Xorm、Beego、GoFrame...）
<br>给个 ⭐️Star 支持我们的工作吧！</p>

> 灵感来源 [Laravel Idea](https://plugins.jetbrains.com/plugin/13441-laravel-idea) &
> [PhpStorm metadata](https://www.jetbrains.com/help/phpstorm/ide-advanced-metadata.html)。 本人使用 Go 的 ORM 包时，
> 一些 ORM 函数的参数是字符串，并且ide不支持代码补全，在字段贼多的情况下，记不清楚，写起来就会很不方便。以前本人写php的时候，就用到前面的插件，
> 感觉非常爽，所以就有了这个插件~~

## 特性

- [x] ORM 代码补全
    - [x] @Model 注解辅助补全
    - [x] @Table 注解辅助补全
    - [ ] 自定义 SQL 辅助不全 🚧[WIP]
- [x] SQL 转 Struct [支持明细](./SUPPORTED.md#supported-sql-to-struct-conversion)
- [x] Go ORM Tag 实时模版 [支持明细](./SUPPORTED.md#supported-orm-tags-live-template)
- 更多等你去发现与改进...

## 支持的 ORM

- [x] [Gorm](https://github.com/go-gorm/gorm)
- [x] [Xorm](https://gitea.com/xorm/xorm)
- [x] [GoFrame](https://github.com/gogf/gf)
- [ ] [Beego](https://github.com/beego/beego) 🚧[WIP]
- [ ] [sqlx](https://github.com/jmoiron/sqlx) 🚧[WIP]
- [支持明细](./SUPPORTED.md)

## 使用

### 代码补全
https://www.jetbrains.com/help/go/auto-completing-code.html#code-completion-for-functions

![guide.gif](assets%2Fguide.gif)

### 注解辅助
有些写法此插件可能无法兼容，导致无法补全。你可以使用 @Model 或者 @Table 解决此问题。
![annotation.gif](assets%2Fannotation.gif)

插件会扫描项目中所有Struct，建议设置扫描范围。
![setting.png](assets%2Fsetting.png)

### 实时模版
![live-template.gif](assets%2Flive-template.gif)

### SQL 转 Struct
#### 粘贴方式
![paste.gif](assets%2Fpaste.gif)

#### 菜单操作
选中 SQL -> 点击编辑器右键菜单 -> Go ORM 助手工具箱 -> SQL 转 Struct

![manual-sql-to-struct.png](assets%2Fmanual-sql-to-struct.png)

## 安装

> 兼容范围：
> - GoLand — 2022.2+
> - IntelliJ IDEA Ultimate — 2022.2+

### 通过 Jetbrains Marketplace 安装

<a href="https://plugins.jetbrains.com/plugin/22173-go-orm-helper" target="_blank">
    <img src="https://blog.johnmai.top/go-orm-helper/assets/installation_button.svg" height="52" alt="Get from Marketplace" title="Get from Marketplace">
</a>

### 本地安装

- 下载【Go ORM 助手】插件包 [Releases](https://github.com/maiqingqiang/go-orm-helper/releases)
- 安装插件教程: https://www.jetbrains.com/help/idea/managing-plugins.html