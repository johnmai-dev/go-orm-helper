English | [简体中文](./README-zh_CN.md) | [日本語](./README-ja_JP.md) | [한국어](./README-ko_KR.md)

<div align="center">
    <img src="https://blog.johnmai.top/go-orm-helper/src/main/resources/icons/icon64x64.svg" alt="Go ORM Helper"/>
    <h1 align="center">Go ORM Helper</h1>
</div>

<p align="center">A GoLand plugin that automatically provides database field autocompletion, tags, and generates Structs
for writing ORM code. Supports: Gorm、Xorm、Beego、GoFrame, etc.<br/>⭐️ Star to support our work!</p>

> Inspired by [Laravel Idea](https://plugins.jetbrains.com/plugin/13441-laravel-idea) & 
> [PhpStorm metadata](https://www.jetbrains.com/help/phpstorm/ide-advanced-metadata.html). When using ORM packages in Golang,
> I noticed that some ORM function parameters are strings and the IDE does not support code completion. It becomes inconvenient
> to write code when dealing with numerous fields. Previously, when I was writing PHP, I used similar plugins and found them
> extremely useful. Hence, this plugin was developed.

## Features
- [x] ORM Code Completion.
  - [x] Assisted code completion with @Model annotation.
  - [x] Assisted code completion with @Table annotation.
  - [ ] Assisted code completion for custom SQL. 🚧[WIP]
- [x] SQL to Struct conversion. [Supported](./SUPPORTED.md#supported-sql-to-struct-conversion)
- [x] Go ORM Tags Live Template. [Supported](./SUPPORTED.md#supported-orm-tags-live-template)
- More features waiting to be discovered and improved...

## Supported ORM Code Completion.
- [x] [Gorm](https://github.com/go-gorm/gorm)
- [x] [Xorm](https://gitea.com/xorm/xorm)
- [x] [GoFrame](https://github.com/gogf/gf)
- [ ] [Beego](https://github.com/beego/beego) 🚧[WIP]
- [ ] [sqlx](https://github.com/jmoiron/sqlx) 🚧[WIP]
- [More Supported](./SUPPORTED.md)

## Usage

### Code Completion
https://www.jetbrains.com/help/go/auto-completing-code.html#code-completion-for-functions

![guide.gif](assets%2Fguide.gif)

### Annotation
If the plugin is not compatible with your syntax, you can use the @Model or @Table helper.
![annotation.gif](assets%2Fannotation.gif)

The plugin will scan the Structs in your project. It is recommended to set the scanning scope.
![setting.png](assets%2Fsetting.png)

### Live Template
![live-template.gif](assets%2Flive-template.gif)

### SQL to Struct
#### Paste
![paste.gif](assets%2Fpaste.gif)

#### Menu Action
Selected SQL -> Editor Popup Menu -> Go ORM Helper Tool -> SQL Convert Struct
![manual-sql-to-struct.png](assets%2Fmanual-sql-to-struct.png)

<!-- Plugin description end -->

## Installation

> Compatibility Range:
> - GoLand — 2022.2+
> - IntelliJ IDEA Ultimate — 2022.2+

### Install Plugin from Jetbrains Marketplace

<a href="https://plugins.jetbrains.com/plugin/22173-go-orm-helper" target="_blank">
    <img src="https://blog.johnmai.top/go-orm-helper/assets/installation_button.svg" height="52" alt="Get from Marketplace" title="Get from Marketplace">
</a>

### Install Plugin from Disk

- Download Go ORM Helper Plugin [Releases](https://github.com/maiqingqiang/go-orm-helper/releases)
- Install plugins guide: https://www.jetbrains.com/help/idea/managing-plugins.html