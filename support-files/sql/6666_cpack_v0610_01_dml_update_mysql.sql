-- 使用devops_ci_auth数据库
USE devops_ci_auth;

DROP PROCEDURE IF EXISTS ci_bkrepo_auth_data_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_bkrepo_auth_data_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
SELECT DATABASE() INTO db;

-- 判断T_RESOURCE_ACTION中是否存在符合条件的数据
IF EXISTS (
    SELECT 1
    FROM T_RESOURCE_ACTION
    WHERE ACTION_CODE = 'access'
      AND RESOURCE_CODE = 'bkrepo') THEN
   -- 存在则删除
DELETE FROM T_RESOURCE_ACTION
WHERE ACTION_CODE = 'access' AND RESOURCE_CODE = 'bkrepo';
END IF;

-- 判断T_RESOURCE_TYPE中是否存在符合条件的数据
IF EXISTS (
    SELECT 1
    FROM T_RESOURCE_TYPE
    WHERE RESOURCE_CODE = 'bkrepo'
      AND PERMISSION_MODE = 'NOT_INSTANCE') THEN
   -- 存在则更新
UPDATE T_RESOURCE_TYPE SET
    PERMISSION_MODE = 'INSTANCE_ONLY',
    INSTANCE_SOURCE_URL = '/ms/cpack/auth/api/extAuth/instanceld',
    HAS_INSTANCE = 1
WHERE RESOURCE_CODE = 'bkrepo' AND PERMISSION_MODE = 'NOT_INSTANCE';
END IF;

COMMIT;
END <CI_UBF>
DELIMITER ;

CALL ci_bkrepo_auth_data_update();