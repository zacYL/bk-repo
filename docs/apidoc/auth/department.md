## bkrepo 部门相关接口

### 查询部门

- API: GET /auth/api/department/list/?username={username}&departmentId={departmentId}
- API 名称: list_department
- 功能说明：
  - 中文：查询该部门下一级部门列表
  - English：list department

- input body:

``` json


```


- input 字段说明

| 字段         | 类型   | 是否必须 | 默认值 | 说明     | Description  |
| ------------ | ------ | -------- | ------ | -------- | ------------ |
| departmentId | Int    | 否       | 无     | 父部门id | departmentId |
| username     | string | 否       | 无     | 用户名   | username     |


- output:

```
{
    "code": 0,
    "message": null,
    "data": [
        {
            "id": 2,
            "order": 1,
            "name": "测试部门",
            "parent": 1,
            "has_children": true
        }
    ],
    "traceId": ""
}
```

- output 字段说明

| 字段         | 类型           | 说明                                    | Description               |
| ------------ | -------------- | --------------------------------------- | ------------------------- |
| code         | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message      | result message | 错误消息                                | the failure message       |
| data         | bool           | result data                             | the data for response     |
| traceId      | string         | 请求跟踪id                              | the trace id              |
| id           | Int            | 部门id                                  | department Id             |
| parent       | Int            | 父部门id                                | parentId                  |
| name         | String         | 部门名                                  | department name           |
| order        | Int            |                                         |                           |
| has_children | Boolean        | 是否有子部门                            | has children              |





### 通过部门id查询部门详情

- API: POST /auth/api/department/listByIds/?username={username}
- API 名称: list_department_by_ids
- 功能说明：
  - 中文：批量查询部门详情
  - English：list department by ids

- input body:

``` json
[{departmentId1},{departmentId2}]
```


- input 字段说明

| 字段         | 类型   | 是否必须 | 默认值 | 说明             | Description  |
| ------------ | ------ | -------- | ------ | ---------------- | ------------ |
| departmentId | Int    | 否       | 无     | 需要查询的部门id | departmentId |
| username     | string | 否       | 无     | 用户名           | username     |


- output:

```
{
    "code": 0,
    "message": null,
    "data": [
        {
            "id": 1,
            "name": "总公司",
            "order": null,
            "parent": null,
            "has_children": null
        },
        {
            "id": 2,
            "name": "测试部门",
            "order": null,
            "parent": null,
            "has_children": null
        }
    ],
    "traceId": ""
}
```

- output 字段说明

| 字段         | 类型           | 说明                                    | Description               |
| ------------ | -------------- | --------------------------------------- | ------------------------- |
| code         | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message      | result message | 错误消息                                | the failure message       |
| data         | bool           | result data                             | the data for response     |
| traceId      | string         | 请求跟踪id                              | the trace id              |
| id           | Int            | 部门id                                  | department Id             |
| parent       | Int            | 父部门id                                | parentId                  |
| name         | String         | 部门名                                  | department name           |
| order        | Int            |                                         |                           |
| has_children | Boolean        | 是否有子部门                            | has children              |