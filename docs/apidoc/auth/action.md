## bkrepo 部门相关接口

### 查询部门

- API: GET /{project}/{repo}/optional
- API 名称: list_action
- 功能说明：
  - 中文：查询该仓库下可选的动作
  - English：list department

- input body:

``` json


```


- input 字段说明

| 字段    | 类型   | 是否必须 | 默认值 | 说明   | Description |
| ------- | ------ | -------- | ------ | ------ | ----------- |
| project | String | 是       | 无     | 项目名 | project     |
| repo    | string | 是       | 无     | 仓库名 | repo        |


- output:

```
{
    "code": 0,
    "message": null,
    "data": [
        {
            "action": "REPO_MANAGE",
            "nickName": "仓库管理"
        },
        {
            "action": "FOLDER_MANAGE",
            "nickName": "目录管理"
        }
    ],
    "traceId": ""
}
```

- output 字段说明

| 字段     | 类型           | 说明                                    | Description               |
| -------- | -------------- | --------------------------------------- | ------------------------- |
| code     | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message  | result message | 错误消息                                | the failure message       |
| data     | bool           | result data                             | the data for response     |
| traceId  | string         | 请求跟踪id                              | the trace id              |
| action   | Int            | action Id                               | action Id                 |
| nickName | Int            | action Id 中文名                        | nick name                 |





