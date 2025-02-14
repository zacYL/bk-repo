# CPACK 配置指引
[toc]

<font color=red>第一次部署时指定部署模式后，不允许切换部署模式</font>

## 
1. 独立部署模式
common.yaml
```yaml
auth:
  realm: cpack
```

2. 集成CI部署
   common.yaml
```yaml
auth:
  realm: canway

devops:
  bkHost: http://paas.newcc.com   # 蓝鲸地址
  appCode: bk_ci                  # 制品库在注册蓝鲸的appCode
  appSecret: d055e2c0-3be3-4d9b-ad04-eb56b6957af7   # 制品库在注册蓝鲸的appSecret
  devopsHost: http://devops.newcc.com   #CI地址
  bkrepoHost: http://bkrepo.newcc.com   #制品库地址
```