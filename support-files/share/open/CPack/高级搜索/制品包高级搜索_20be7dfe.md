# 制品包高级搜索
功能描述：制品包高级搜索，仅限于在单个项目中搜索。



## 请求

#### 接口方法

`POST`

#### 接口地址

`/api/open/CPack/repo-repository/api/package/search`

#### 请求头

| 字段                  | 类型   | 必填 | 描述               |
| --------------------- | ------ | ---- | ------------------ |
| X-DEVOPS-ACCESS-TOKEN | String | 是   | Devops用户访问令牌 |

#### 路径参数

无

#### 查询参数

无

#### 请求体

| 字段   | 类型         | 必填 | 描述         | 默认行为             |
| ------ | ------------ | ---- | ------------ | -------------------- |
| page   | Object       | 否   | 分页参数     | 根据page默认参数分页 |
| sort   | Object       | 否   | 排序参数     | 不排序               |
| select | List<String> | 否   | 返回字段列表 | 查询所有字段         |
| rule   | Object       | 是   | 查询规则     | /                    |



------



**page对象字段**

| 字段       | 类型 | 必填 | 描述     | 默认值 |
| ---------- | ---- | ---- | -------- | ------ |
| pageNumber | Int  | 否   | 当前页码 | 1      |
| pageSize   | Int  | 否   | 每页大小 | 20     |



**sort对象字段**

| 字段       | 类型         | 必填 | 描述                                         | 默认值           |
| ---------- | ------------ | ---- | -------------------------------------------- | ---------------- |
| properties | List<String> | 是   | 按照哪些字段排序（靠前的字段排序优先级更高） | 空数组（不排序） |
| direction  | String       | 否   | 排序方向。可选值：DESC（倒序），ASC（正序）  | ASC              |



**rule对象字段**

| 字段     | 类型         | 必填 | 描述                                                         | 默认行为             |
| -------- | ------------ | ---- | ------------------------------------------------------------ | -------------------- |
| rules    | List<Object> | 是   | 查询规则列表（必须传入projectId EQ规则）                     | 根据page默认参数分页 |
| relation | String       | 是   | rules数组中多个规则的组合关系，只能填"AND" （多个规则的关系为"且"） | /                    |



------



**rules列表中单个规则对象的字段**

| 字段      | 类型   | 必填 | 描述                   |
| --------- | ------ | ---- | ---------------------- |
| field     | String | 是   | 用哪个字段作为查询条件 |
| value     | Any    | 是   | 查询字段值             |
| operation | String | 是   | 查询操作类型           |



**field可用值**

| field            | 描述                          | 适用操作类型                                                 |
| ---------------- | ----------------------------- | ------------------------------------------------------------ |
| projectId        | 项目ID，必须传入该查询规则    | EQ                                                           |
| repoType         | 制品类型（MAVEN、PYPI......） | EQ、IN                                                       |
| repoName         | 仓库名称                      | EQ、IN、NIN                                                  |
| name             | 制品包名称                    | EQ、NE、IN、NIN、PREFIX、SUFFIX、MATCH、MATCH_I、REGEX、REGEX_I |
| key              | 制品包Key                     | EQ、NE、IN、NIN、PREFIX、SUFFIX、MATCH、MATCH_I、REGEX、REGEX_I |
| downloads        | 下载量总计                    | EQ、NE、IN、NIN、LTE、LT、GTE、GT                            |
| versions         | 版本数量                      | EQ、NE、IN、NIN、LTE、LT、GTE、GT                            |
| latest           | 最新版本号                    | EQ、NE、IN、NIN、PREFIX、SUFFIX、MATCH、MATCH_I、REGEX、REGEX_I |
| createdBy        | 创建人                        | EQ、NE、IN、NIN、PREFIX、SUFFIX、MATCH、MATCH_I、REGEX、REGEX_I |
| lastModifiedBy   | 最后修改人                    | EQ、NE、IN、NIN、PREFIX、SUFFIX、MATCH、MATCH_I、REGEX、REGEX_I |
| createdDate      | 创建时间                      | EQ、NE、IN、NIN、BEFORE、AFTER                               |
| lastModifiedDate | 最后修改时间                  | EQ、NE、IN、NIN、BEFORE、AFTER                               |



**operation可用值**

| operation值 | 描述                      | 适用数据类型  |
| ----------- | ------------------------- | ------------- |
| EQ          | 相等                      | Any           |
| NE          | 不相等                    | Any           |
| LTE         | 小于或等于                | Number        |
| LT          | 小于                      | Number        |
| GTE         | 大于或等于                | Number        |
| GT          | 大于                      | Number        |
| BEFORE      | 在指定时间前              | LocalDateTime |
| AFTER       | 在指定时间后              | LocalDateTime |
| IN          | 与列表中的某个值相等      | List          |
| NIN         | 与列表中的所有值都不相等  | List          |
| PREFIX      | 前缀匹配                  | String        |
| SUFFIX      | 后缀匹配                  | String        |
| MATCH       | 可使用*通配符进行模糊匹配 | String        |
| MATCH_I     | MATCH的大小写不敏感形式   | String        |
| REGEX       | 正则匹配                  | String        |
| REGEX_I     | 大小写不敏感的正则匹配    | String        |



**rules列表中的必填规则**

projectId EQ规则，用于限定从单个项目中查询：

```json
{
    "field": "projectId",
    "value": "<your project id>",
    "operation": "EQ"
}
```



**rules列表中的常用自定义规则**

限制仅查询Maven仓库类型：

```json
{
    "field": "repoType",
    "value": "MAVEN",
    "operation": "EQ"
}
```



限制仅从固定的仓库列表中查询：

```json
{
    "field": "repoName",
    "value": ["repo1", "repo2"],
    "operation": "IN"
}
```



模糊查询包名：

```json
{
    "field": "name",
    "value": "*apple*",
    "operation": "MATCH_I"
}
```



##### 请求体示例

```json
{
    "page": {
        "pageNumber": 1,
        "pageSize": 20
    },
    "rule": {
        "rules": [
            {
                "field": "projectId",
                "value": "e8156e",
                "operation": "EQ"
            },
            {
                "field": "repoType",
                "value": "PYPI",
                "operation": "EQ"
            },
            {
                "field": "name",
                "value": "*fast*",
                "operation": "MATCH_I"
            }
        ],
        "relation": "AND"
    }
}
```



#### 请求示例

```bash
curl -X 'POST' \
    -H 'Content-Type: application/json' \
    -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' \
    -d '<请求体...>' \
    'https://devops.example.com/api/open/CPack/repo-repository/api/package/search'
```



## 响应

#### 响应示例

```json
{
    "code": 0,
    "message": null,
    "data": {
        "pageNumber": 1,
        "pageSize": 20,
        "totalRecords": 3,
        "totalPages": 1,
        "records": [
            {
                "_id": {
                    "timestamp": 1701324921,
                    "date": 1701324921000
                },
                "createdBy": "admin",
                "repoName": "dev_pypi",
                "extension": null,
                "lastModifiedBy": "admin",
                "key": "pypi://fastgm-whl",
                "type": "PYPI",
                "lastModifiedDate": 1701324922478,
                "createdDate": 1701324921917,
                "projectId": "e8156e",
                "downloads": 0,
                "historyVersion": [
                    "0.4.1"
                ],
                "name": "fastgm-whl",
                "versionTag": {},
                "versions": 1,
                "description": null,
                "latest": "0.4.1"
            },
            {
                "_id": {
                    "timestamp": 1701164786,
                    "date": 1701164786000
                },
                "createdBy": "admin",
                "repoName": "pypi_remote",
                "extension": null,
                "lastModifiedBy": "admin",
                "key": "pypi://fasteners",
                "type": "PYPI",
                "lastModifiedDate": 1701164786902,
                "createdDate": 1701164786898,
                "projectId": "e8156e",
                "downloads": 1,
                "historyVersion": [
                    "0.19"
                ],
                "name": "fasteners",
                "versionTag": {},
                "versions": 1,
                "description": null,
                "latest": "0.19"
            },
            {
                "_id": {
                    "timestamp": 1696929294,
                    "date": 1696929294000
                },
                "key": "pypi://fastgm-whl",
                "name": "fastgm-whl",
                "historyVersion": [
                    "0.4.1"
                ],
                "downloads": 0,
                "versions": 1,
                "repoName": "test_pypi",
                "createdBy": "admin",
                "lastModifiedDate": 1696929295073,
                "projectId": "e8156e",
                "type": "PYPI",
                "lastModifiedBy": "admin",
                "versionTag": {},
                "createdDate": 1696929294632,
                "extension": null,
                "description": null,
                "latest": "0.4.1"
            }
        ],
        "count": 3,
        "page": 1
    },
    "traceId": "c500ffbefc8d04a690e900543bfab392"
}
```

#### 响应体

| 字段    | 说明       |
| ------- | ---------- |
| code    | 返回码     |
| message | 错误信息   |
| data    | 数据       |
| traceId | 链路追踪id |

##### data 字段说明

分页信息

| 字段         | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| pageNumber   | 当前页码                                                 |
| pageSize     | 每页大小                                                 |
| totalRecords | 总记录条数                                               |
| totalPages   | 总页数                                                   |
| records      | **数据列表**                                             |
| count        | 总记录条数（该字段可能在未来移除，请使用`totalRecords`） |
| page         | 总页数（该字段可能在未来移除，请使用`totalPages`）       |

##### records 部分字段说明

制品包信息

| 字段             | 说明         |
| ---------------- | ------------ |
| createdBy        | 创建人       |
| createdDate      | 创建时间     |
| lastModifiedBy   | 最后修改人   |
| lastModifiedDate | 最后修改时间 |
| projectId        | 项目ID       |
| repoName         | 仓库名称     |
| type             | 制品包类型   |
| key              | 制品包key    |
| name             | 制品包名称   |
| versions         | 版本数量     |
| latest           | 最新版本号   |
| downloads        | 总下载量     |
| description      | 制品包描述   |

