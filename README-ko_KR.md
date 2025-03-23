[English](./README.md) | [简体中文](./README-zh_CN.md) | [日本語](./README-ja_JP.md) | 한국어

<div align="center">
    <img src="https://blog.johnmai.top/go-orm-helper/src/main/resources/icons/icon64x64.svg" alt="Go ORM 도우미"/>
    <h1 align="center">Go ORM 도우미</h1>
</div>

<p align="center">GoLand 에 ORM을 쓸 수 있도록 데이터베이스 필드, Tag, Struct를 자동으로 완성하는 플러그인지원: Gorm, Xorm, Beego, GoFrame...)
<br>우리의 작업을 지원하기 위해 ⭐️스타를 부탁드립니다!</p>

> [Laravel Idea](https://plugins.jetbrains.com/plugin/13441-laravel-idea) &
> [PhpStorm metadata](https://www.jetbrains.com/help/phpstorm/ide-advanced-metadata.html) 에서영감을 받았습니다.본인이 Go의 ORM 
> 패키지를 사용할 때 일부 ORM 함수의 매개 변수는 문자열이고 ide는 코드 완성을 지원하지 않으며 필드가 많은 경우 잘 기억하지 못하고 쓰기가 매우 불편합니다.이전에 
> 본인이 php를 쓸 때 앞의 플러그인을 사용했는데 아주 상쾌해서 이 플러그인이 생겼어요~~

## 특징

- [x] ORM 코드 완성
    - [x] @Model 메모 보조 완성
    - [x] @Table 메모 보조 완성
    - [ ] 사용자 정의 SQL 보조 불완전 🚧[WIP]
- [x] SQL 변환 Struct [지원 내역](./SUPPORTED.md#supported-sql-to-struct-conversion)
- [x] Go ORM Tag 실시간 템플릿 [지원 내역](./SUPPORTED.md#supported-orm-tags-live-template)
- 더 많은 것을 발견하고 개선하기를 기다린다...

## 지원되는 ORM

- [x] [Gorm](https://github.com/go-gorm/gorm)
- [x] [Xorm](https://gitea.com/xorm/xorm)
- [x] [GoFrame](https://github.com/gogf/gf)
- [ ] [Beego](https://github.com/beego/beego) 🚧[WIP]
- [ ] [sqlx](https://github.com/jmoiron/sqlx) 🚧[WIP]
- [지원 내역](./SUPPORTED.md)

## 사용

![guide.gif](assets%2Fguide.gif)

## 설치

> 호환성 범위:
> - GoLand — 2022.2+
> - IntelliJ IDEA Ultimate — 2022.2+

### Jetbrains Marketplace 에서 플러그인 설치

<a href="https://plugins.jetbrains.com/plugin/22173-go-orm-helper" target="_blank">
    <img src="https://blog.johnmai.top/go-orm-helper/assets/installation_button.svg" height="52" alt="Get from Marketplace" title="Get from Marketplace">
</a>

### 로컬 설치

- [Go ORM 도우미] 플러그인 패키지 다운로드 [Releases](https://github.com/maiqingqiang/go-orm-helper/releases)
- 플러그인 설치 자습서 https://www.jetbrains.com/help/idea/managing-plugins.html