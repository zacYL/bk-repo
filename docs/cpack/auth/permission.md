# Permission接口

[toc]

## 是否项目管理员

- API: GET /auth/api/permission/admin?projectId={test}
- API 名称: is_project_admin
- 功能说明：
  - 中文：是否项目管理员
  - English：is project admin
- 请求体

  ```json
  
  ```
  
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名|proejct name|
  
- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": true,
    "traceId": null
  }
  ```

- data 字段说明

  | 字段 | 类型    | 说明                                      | Description      |
  | ---- | ------- | ----------------------------------------- | ---------------- |
  | data | boolean | true: 是项目管理员；false: 不是项目管理员 | the project name |

