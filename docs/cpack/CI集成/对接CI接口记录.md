# 对接CI/蓝鲸接口记录

[toc]

## 4.3.11.x

* CI:

  ```http
  # 权限中心
  GET /ms/permission/api/service/resource_instance/view/project/%s
  GET /ms/permission/api/service/tenant/%s/group
  GET /ms/permission/api/service/blueking/user
  GET /ms/permission/api/service/resource_instance/view/group/project/
  GET /ms/permission/api/service/administrator/{userId}/{type}?instanceCode=
  GET /ms/permission/api/service/resource_instance/view/group/project/{projectId}
  POST /ms/permission/api/service/resource_instance/query
  # 用户管理中心
  GET /ms/usermanaget/service/user/allUser
  GET /ms/usermanager/api/service/organization/departments/{userId}
  # 项目中心
  GET /ms/project/api/user/project/cw/selectByType?typeId=2
  
  ```

* 蓝鲸：

  ```http
  # 
  GET /api/c/compapi/v2/bk_login/get_user/?bk_app_code=%s&bk_app_secret=%s&%s=%s
  GET /api/c/compapi/v2/usermanage/list_users/bk_app_code=%s&username=admin&bk_app_secret=%s&fields=username,display_name&page=%d&page_size=%d
  GET /api/c/compapi/v2/usermanage/list_departments/
  GET /api/c/compapi/v2/usermanage/department_batch/
  GET /api/c/compapi/v2/usermanage/list_department_profiles/?bk_app_code=%s&bk_app_secret=%s&%s=%s&id=%d&recursive=true&no_page=true
  ```

  