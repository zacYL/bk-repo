# Project项目接口

[toc]

## 创建项目

- API: POST /repository/api/project/create
- API 名称: create_project
- 功能说明：
  - 中文：创建项目
  - English：create project
- 请求体

  ```json
  {
    "name": "test",
    "displayName": "test",
    "description": "project description"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|项目名称，要求以字母或者下划线开头，长度不超过32位|proejct name|
  |displayName|string|是|无|显示名称，要求以字母或者下划线开头，长度不超过32位。此字段保留作用，和name设置为相同值即可|project display name|
  |description|string|是|无|项目描述|project description|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 查询项目列表

- API: GET /repository/api/project/list
- API 名称: get_project_list
- 功能说明：
  - 中文：查询项目列表
  - English：get project list
- 请求体
  此接口请求体为空
- 响应体

  ``` json
  {
      "code":0,
      "message":"",
      "data":[
          {
              "name":"project1",
              "displayName":"project1",
              "description":"project1"
          }
      ],
      "traceId": null
  }
  ```

- data 字段说明

  | 字段|类型|说明|Description|
  |---|---|---|---|
  |name|string|项目名|the project name |
  |displayName|string|项目展示名称|the display name of project|
  |description|string|项目描述|the description of project|

## 编辑项目

- API: PUT /repository/api/project/{projectId}

- API 名称: create_project

- 功能说明：

  - 中文：创建项目
  - English：create project

- 请求体

  ```json
  {
    "displayName":"abcdfg",
    "description":"fsffdff"
  }
  ```

- 请求字段说明

  | 字段        | 类型   | 是否必须 | 默认值 | 说明                                                         | Description          |
  | ----------- | ------ | -------- | ------ | ------------------------------------------------------------ | -------------------- |
  | projectId   | string | 是       | 无     |                                                              | proejct name         |
  | displayName | string | 否       | 无     | 显示名称，要求以字母或者下划线开头，长度不超过32位。此字段保留作用，和name设置为相同值即可 | project display name |
  | description | string | 否       | 无     | 项目描述                                                     | project description  |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": true,
      "traceId": ""
  }
  ```

- data 字段说明

  | 字段 | 类型    | 说明      | Description      |
  | ---- | ------- | --------- | ---------------- |
  | data | boolean | true:更新 | the project name |



## 校验项目信息是否存在

- API: GET /repository/api/project/exist?name=abc&displayName=abc

- API 名称: check_project_message_exist

- 功能说明：

  - 中文：校验项目信息是否存在
  - English：check project message exist

- 请求体
  此接口无请求体

- 请求字段说明

  | 字段        | 类型   | 是否必须 | 默认值 | 说明         | Description         |
  | ----------- | ------ | -------- | ------ | ------------ | ------------------- |
  | name        | string | 否       | 无     | 项目名称     | project name        |
  | displayName | string | 否       | 无     | 项目显示名称 | project displayName |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": true,
    "traceId": null
  }
  ```

- data字段说明

  | 字段 | 类型    | 说明                               | Description       |
  | ---- | ------- | ---------------------------------- | ----------------- |
  | data | boolean | true:资源已存在, false: 资源不存在 | repo exist or not |

