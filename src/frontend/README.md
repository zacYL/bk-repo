# bk-repo 前端项目

## 产品概述

- 嘉为蓝鲸 CPack制品库是一款自研底层引擎的企业级管理依赖包和制品包的工具，位于嘉为蓝鲸 DevOps平台应用开发域。CPack不仅支持 Generic、Docker、Maven、Helm、npm、PyPI 、Composer、RPM 包等主流制品库类型，而且支持仓库代理功能，解决不同仓库管理复杂的问题；通过版本管理及元数据来管理制品全生命周期；提供安全扫描与依赖分析进行风险把控；拥有精细化的权限管控与监控能力，保障数字资产安全；具备制品同步分发能力，实现多数据中心的数据同步。

- CPack 在实现对制品全生命周期、风险质量多维管控的同时，与嘉为蓝鲸 Cpipe 持续集成天然打通，既适用于开发人员从制品库中获取开发依赖、构建依赖并上传构建制品及数据到制品库，又适用于运维人员将测试通过满足部署条件的制品对接环境进行部署，有效支撑开发、构建、测试、部署等阶段对依赖、制品的使用，有效提高研运团队工作效率。

## 术语定义

### 制品
源码编译打包生成的二进制文件，不同的开发语言对应着不同格式的二进制文件，这些二进制文件通常用于运行在服务器上或者作为编译依赖。支持的制品类型包括文件类型、Docker、Maven、Helm、npm、PyPI、Composer等。 制品库的层级关系为：仓库 > 包 > 版本，每个层级描述如下：

- 仓库：用于管理不同类型的仓库和仓库下的包资源，可以设置仓库对外的访问权限。

- 包：构建产物对外提供访问的基础单元，用于介绍当前构建产物的用途和使用指引。

- 版本：列出某个包下的所有构建产物，详细记录了每次构建产物的版本迭代更新变化。

可利用归档构件插件将制品归档到指定的仓库中。

### custom(自定义)仓库

用于管理自定义仓库中的制品，显示自定义仓库制品信息，也可在此处进行新建文件夹、上传制品等操作。

### pipeline(流水线)仓库

用于管理流水线仓库中的制品，显示流水线仓库制品信息，也可在此处进行查看详情、计算文件大小等操作。

### report(报告)仓库

用于管理流水线仓库中归档报告中的产物，显示报告仓库制品信息，也可在此处进行查看详情等操作。

### npm仓库

用于管理npm类型的包，也可以代理远程中央仓库的包，显示上传至仓库的包信息和相关版本详情信息，也可以在此处进行包的删除等操作。

### helm仓库
用于管理helm类型的包，显示上传至仓库的包信息和版本详情信息，也可以在此处进行包的删除等操作。

### maven仓库
用于管理maven类型的包，也可以代理远程中央仓库的包，显示上传至仓库的包信息和相关版本详情信息，也可以在此处进行包的删除等操作。

### rpm仓库

用于管理rpm类型的包，显示上传至仓库的包信息和相关版本详情信息，也可以在此处进行包的删除等操作。

### pypi仓库

用于管理pypi类型的包，也可以代理远程中央仓库的包，显示上传至仓库的包信息和相关版本详情信息，也可以在此处进行包的删除等操作。

### composer仓库

用于管理composer类型的包，也可以代理远程中央仓库的包，显示上传至仓库的包信息和相关版本详情信息，也可以在此处进行包的删除等操作。

### NUGET仓库

显示用户上传或者从中央仓库代理的包信息，可通过在权限中心申请相关权限后进行nuget推包操作。

### 同步策略

当需要不同同步场景时，支持立即执行、指定时间执行、定时执行及实时自动执行等策略；

### 冲突策略

当目标节点存在相同同步对象时，支持执行多种冲突策略；

### 限速策略

当企业对分发网络流量有限定时，支持设置限速策略。


## 产品功能列表

<table>
    <tr>
        <td colspan="3" style="text-align: center;"><b> CPack 制品库</b></td>
    </tr>
    <tr>
     <th>类别</th>
     <th>功能</th>
     <th>描述</th>
    </tr>
    <tr>
        <td>custom(自定义)仓库</td>
        <td>管理自定义仓库中的制品</td>
        <td>显示自定义仓库制品信息，也可在此处进行新建文件夹、上传制品等操作。</td>
    </tr>
    <tr>
        <td>pipeline(流水线)仓库</td>
        <td>管理流水线仓库中的制品</td>
        <td>显示流水线仓库制品信息，也可在此处进行查看详情、计算文件大小等操作。</td>
    </tr>
    <tr>
        <td>NPM仓库</td>
        <td>管理NPM依赖源类型的仓库</td>
        <td>显示用户上传或者从中央仓库代理的包信息，可通过在权限中心申请相关权限后进行NPM推包操作。</td>
    </tr>
    <tr>
        <td>HELM仓库</td>
        <td>管理HELM依赖源类型的仓库</td>
        <td>显示用户上传或者从中央仓库代理的包信息，可通过在权限中心申请相关权限后进行HELM推包操作。</td>
    </tr>
    <tr>
        <td>Docker仓库</td>
        <td>管理Docker镜像仓库</td>
        <td>显示公共镜像信息，可通过在权限中心申请相关权限后进行推镜像操作。</td>
    </tr>
    <tr>
        <td>Maven仓库</td>
        <td>管理Maven依赖源类型的仓库</td>
        <td>显示用户上传或者从中央仓库代理的包信息，可通过在权限中心申请相关权限后进行Maven推包操作。</td>
    </tr>
    <tr>
        <td>PYPI仓库</td>
        <td>管理PYPI依赖源类型的仓库</td>
        <td>显示用户上传或者从中央仓库代理的包信息，可通过在权限中心申请相关权限后进行PYPI推包操作。</td>
    </tr>
    <tr>
        <td>Composer仓库</td>
        <td>管理Composer仓库镜像仓库</td>
        <td>显示用户上传或者从中央仓库代理的包信息，可通过在权限中心申请相关权限后进行Composer推包操作。</td>
    </tr>
    <tr>
        <td>RPM仓库</td>
        <td>管理RPM仓库镜像仓库</td>
        <td>显示用户上传或者从中央仓库代理的包信息，可通过在权限中心申请相关权限后进行RPM推包操作。</td>
    </tr>
    <tr>
        <td>NUGET仓库</td>
        <td>管理nuget仓库镜像仓库</td>
        <td>显示用户上传或者从中央仓库代理的包信息，可通过在权限中心申请相关权限后进行nuget推包操作。</td>
    </tr>
    <tr>
        <td>软件源</td>
        <td>查看公开的软件源仓库</td>
        <td>显示管理员设置公开的仓库、制品、依赖包，可对公开软件进行查看、下载操作。</td>
    </tr>
     <tr>
        <td>制品搜索</td>
        <td>对制品进行搜索</td>
        <td>展示全部制品，实现全局搜索目标制品，也可以根据仓库类型筛选仓库类型下的制品。</td>
    </tr>
    <tr>
        <td>分发计划</td>
        <td>分发计划管理</td>
        <td>支持仓库、制品、文件等同步类型，通过多种同步策略、冲突策略及限速策略以灵活应对企业中各种同步场景。</td>
    </tr>
     <tr>
        <td>制品分析</td>
        <td>对制品进行安全分析</td>
        <td>支持多种类型制品扫描，通过监控设置自动扫描最新上传制品，也可手动执行扫描，并解析制品扫描报告更清晰展示制品漏洞情况。</td>
    </tr>
     <tr>
        <td>安全设置</td>
        <td>管理扫描相关的安全数据库</td>
        <td>管理各扫描器的漏洞数据库、开源许可证数据库，支持离线手动更新数据库，支持设置代理白名单管理拉取的依赖。</td>
    </tr>
    <tr>
        <td>节点管理</td>
        <td>管理项目节点</td>
        <td>支持多地数据中心集群方式部署和横向多节点扩展，通过实时监控节点的运行状态以保障制品分发顺利进行。</td>
    </tr>
    <tr>
        <td>访问令牌</td>
        <td>管理项目访问令牌</td>
        <td>创建访问令牌，用于作为访问依赖源仓库的访问凭证，拉取制品。</td>
    </tr>
<table>

## 用户的权限划分

在平台中分别可以在用户管理、项目管理、仓库管理中设置用户或管理员等身份并划分权限。具体可分为系统管理员、项目管理员、仓库管理员、普通用户。

- 系统管理员：全部管理与所有仓库权限；
- 项目管理员：被授权项目的制品仓库、制品搜索、制品分发、访问令牌、项目设置的权限；
- 仓库管理员：被授权仓库的仓库信息、权限管理、修改制品、查看制品、删除制品权限；
- 普通用户：被授权项目及仓库中的部分被授权修改、查看、删除权限。

## 项目技术栈

本项目使用的前端框架为 Vue2.6, 包管理工具为 Webpack，打包工具使用 gulp，使用 lerna 管理多个项目包 (因为本项目分为 独立部署、集成CI、软件源、管理页面admin等四种模式)，其中集成CI模式接入蓝鲸底座,部分通用功能代码位于独立部署的 `devops-repository`目录下，`devops-op`目录为制品库的管理页面，基本属于独立的服务。

## 前端功能菜单

**路径: `devops-repository\src\views\index.vue`**

若是集成CI模式下不显示菜单请询问后端人员，修改部署脚本及后端项目代码。

## 依赖管理

因为本项目中的几种模式下的代码存在部分相互依赖的情况，因此将其分为不同的包放置于同一个代码库中进行管理，此时就需要使用 lerna 来管理，其中 devops-op 这个包和其他包之间没有任何关联，只是统一使用 lerna 来管理和使用 gulp 来打包。

## 相关配置及简述

### whistle代理配置

- 独立部署和集成CI模式代理配置

```bash
/\.(net|com|reponet)\/ui(.*)$/ https://127.0.0.1:8086/ui$2
# 独立部署
ws://bkrepo.center.canway.com:8086/ui/ ws://127.0.0.1:8086/ui/
ws://repo.external.canway.reponet:8086/ui/ ws://127.0.0.1:8086/ui/

# 上海银行独立部署
ws://cpack.cpbostest.com:8086/ui/ ws://127.0.0.1:8086/ui/

# 集成CI模式
ws://devops.newcc.com:8086/ui/ ws://127.0.0.1:8086/ui/
ws://devops.opsenv.com:8086/ui/ ws://127.0.0.1:8086/ui/
ws://devops.onlycteam.net/:8086/ui/ ws://127.0.0.1:8086/ui/
/^http:\/\/bkrepo(.*)\/web(.*)$/ http://bkrepo$1/web$2
```

- 软件源

```bash
/\.(net|com|reponet)\/software(.*)$/ https://127.0.0.1:8085/software$2

ws://bkrepo.center.canway.com:8085/software/ ws://127.0.0.1:8085/software/

/^http:\/\/bkrepo(.*)\/web(.*)$/ http://bkrepo$1/web$2
```

- 管理页面 op

```bash
# 前面的.net是为了匹配域名，ui必须跟在域名后面
/\.(net|com|reponet)\/op(.*)$/ https://127.0.0.1:8086
ws://bkrepo.center.canway.com:8086/op/ ws://127.0.0.1:8086

/^https:\/\/bkrepo(.*)\/web(.*)$/ http://bkrepo$1/web$2
```

### 独立部署模式配置简述

- 独立部署模式没有接入蓝鲸底座，使用的用户认证体系和权限管理等是制品库自带的；

- 因为存在某些客户只需要我们公司的制品库服务，不需要蓝鲸的其余业务；

- 用户可以通过一个已经拥有权限的账号登录后在用户管理中添加新用户并配置其相关权限；

- 可通过节点管理控制数据集群节点，支持多地数据中心集群方式部署和横向多节点扩展，管理和监控节点状态；

- 可通过安全设置配置漏洞库、许可证、漏洞白名单、代理白名单等；

- 可通过审计日志查看到最近哪些 Ip 在哪些时间点修改了哪写项目的哪些对象，对其执行了什么操作；

- 可通过网络设置限制每个分发计划的最大网络速率，从而保证网络带宽资源的合理利用。

### 集成CI模式配置简述

- 集成CI模式接入了蓝鲸底座，用户认证和权限管理体系使用的是蓝鲸Devops提供的服务；

- 集成CI模式下的制品库配置位于系统设置>制品配置，注意，若没有权限则无法看到系统设置；

- 集成CI模式下的制品库配置包括节点设置、安全设置、审计日志、网络设置，其功能和独立部署模式下的功能相似。

- 集成CI模式下的制品配置代码不在当前项目中，其存在于 `admin`这个代码仓库，若涉及修改请联系陈灿(charis@canway.net)

### 软件源简述

- 软件源不存在于单独的环境；

- 在独立部署模式中可以通过点击页面左上方的`软件源`字样切换到软件源页面；

- 在集成CI模式下可通过点击消息通知旁边的应用列表图标后选择进入软件源；

- 软件源页面中只包含制品仓库和制品搜索两个页面功能，其中制品仓库页面中在集成CI模式下不属于该租户下的项目名称不会被显示。

### 管理系统 op 简述

- op 是制品库的一个管理系统，与其他几种模式没有任何关联；

- op 是配合独立部署模式使用的，用于管理员做一些不常用的系统管理操作。

- 其内部使用的组件库为 elementUI；

## 代码分支管理及提交规范

- 若是基于最新的环境开发或修改缺陷，则基于 `dev`分支创建一个自己的分支，命名如 `dev_gjq`,`dev_king`等，具体可根据自己喜好来命名，但最好通俗易懂，可以让其他人一看知道是谁；

- 当在自己的分支上开发完成或缺陷修改完成，将代码提交到远程仓库，并发起 merge Request，指定相关有权限人员合并到 `dev`分支，前端可以找王鹏凯(pengkaiwang@canway.net),后端可以找陈灿(charis@canway.net)；

- 若是之前的版本也需要现在的某个功能，则使用 cherry-pick 将 `dev`分支的代码摘取过去，然后提交到远程仓库即可；

- 若是基于某些已有的版本修改缺陷则直接在该版本上修改缺陷，修改完成后提交代码到远程仓库，若之后的版本也存在此缺陷，则将修改缺陷的提交记录 cherry-pick 到其余版本并同步到最新的 `dev`分支；

- 公司使用的是 约定式提交规范，提交信息必须是项目工作项ID(需求、任务、优化、缺陷等在创建的时候都会有自己的id，若没有则可使用 p1_0) + 描述(一般为需求或缺陷等的描述，如果都不是可自行填写，但需要通俗易懂)，例如： git commit -m "feat: p1_20270 【CPack】浙江农信——Generic仓库分目录制定清理策略"，或 git commit -m "fix: p1_0 【CPack】 修复了xxxx"


<table><thead><tr><th style="text-align:left;">标记</th><th style="text-align:left;">描述</th></tr></thead><tbody><tr><td style="text-align:left;">feat</td><td style="text-align:left;">新增功能</td></tr><tr><td style="text-align:left;">fix</td><td style="text-align:left;">修复Bug</td></tr><tr><td style="text-align:left;">docs</td><td style="text-align:left;">文档变更</td></tr><tr><td style="text-align:left;">style</td><td style="text-align:left;">代码格式（不影响功能，例如空格、分号等格式修正）</td></tr><tr><td style="text-align:left;">refactor</td><td style="text-align:left;">代码重构</td></tr><tr><td style="text-align:left;">perf</td><td style="text-align:left;">改善性能</td></tr><tr><td style="text-align:left;">test</td><td style="text-align:left;">测试</td></tr><tr><td style="text-align:left;">chore</td><td style="text-align:left;">变更构建流程或辅助工具</td></tr><tr><td style="text-align:left;">style</td><td style="text-align:left;">代码样式调整，不涉及业务变更</td></tr><tr><td style="text-align:left;">format</td><td style="text-align:left;">不修改业务逻辑下，仅做代码规范的格式化</td></tr><tr><td style="text-align:left;">merge</td><td style="text-align:left;">仅做分⽀合并同步</td></tr><tr><td style="text-align:left;">depend</td><td style="text-align:left;">对⼯程的依赖进⾏增删改</td></tr><tr><td style="text-align:left;">del</td><td style="text-align:left;">删除可能仍然有⼈⽤到的功能、API等破坏性动作</td></tr></tbody></table>


## 开发环境访问地址

**因为后端开发同学远在深圳，所以本项目的开发联调阶段需要使用线上环境。**

- 独立部署： [http://bkrepo.center.canway.com/](http://bkrepo.center.canway.com/)

- 集成CI：根据产品和测试要求，可使用本地代理CI模式访问，具体操作细节见下文。

- 管理页面OP：[http://bkrepo.center.canway.com/op/](http://bkrepo.center.canway.com/op/)

## 注意事项

- 若需要修改的功能在独立部署模式和集成CI模式下均存在，此时可能需要修改 `devops-repository`以及`devops-repository-ci`两个目录下的代码，具体的请根据实际需要查找。

- 本项目使用了国际化，若是`locale\repository\en-US.json`文件中存在的中文名，请优先使用其中的中文名所对应的key。

- 本项目所使用的组件库为静态引入的 bk-magic-vue，路径为 `devops-repository\static\libs\bk-magic-vue`。

- 本项目使用了静态引入的 cherry-markdown依赖库，路径为 `devops-repository\static\libs\cherry-markdown`。

- 本项目使用的 svg图片统一存放于 `devops-repository\src\images`目录下，菜单logo与菜单的路由名一致。

- 本项目使用二次封装的axios来发送http请求，路径为`devops-repository\src\utils\request.js`。

- 本项目对异常结果进行了统一处理，代码位于`devops-repository\src\AppMixin.js`文件的第 20-36 行。

- 本项目中独立部署模式和集成CI模式下打包后的前缀均为 /ui/，管理页面devops-op在生产模式下打包后的前缀为 /admin/。

- 本项目所支持的浏览器环境配置路径为 `.browserslistrc`。

- 在需求开发完成或缺陷修复完成后，请及时在 cwoa 环境中将工作项状态流转。

- 项目中存在一些可能会发生变动的文档，如有需要，可查看企微CPack群中群公告中的微文档。

## 本地启动集成CI模式

**此处以 onlycteam 环境为例**

- 修改 `devops-repository\index.html`文件的第 50-60 行代码

```js
// PAAS_SERVICE_URL  修改为需要访问的系统的 paas
// DEVOPS_SITE_URL   修改为需要访问的系统的 devops
// MODE_CONFIG       修改为 ci 
<script>
    var PAAS_SERVICE_URL = 'http://paas.onlycteam.net'
    var DEVOPS_SITE_URL = 'http://devops.onlycteam.net'
    var LOGIN_SERVICE_URL = /^https?/.test(PAAS_SERVICE_URL)
        ? PAAS_SERVICE_URL + '/login/'
        : '__BK_REPO_PAAS_LOGIN_URL__'
    // standalone: 独立部署模式
    // ci: ci模式接入devops平台
    // saas: saas模式接入蓝鲸底座
    var MODE_CONFIG = 'ci' || 'standalone'
</script>
```

- 然后执行下方的相关命令，重新打包和启动项目

## Build Setup

- 在 `\src\frontend` 目录下安装依赖并启动服务
``` bash
# install dependencies
yarn install
yarn start

```
- 将本地项目打包，若更新了图片或者一些静态引入的文件则需要重新编译打包
```bash
# build for production with minification
yarn public

# build for production with mode

yarn public -m standalone

# 集成CI模式下打包
yarn public -m ci

```
- 进入到对应的文件目录下启动项目
```bash
# serve with hot reload at localhost:xxxx
cd ./devops-repository

#  ci模式
#  cd ./devops-repository-ci

yarn dev
```
