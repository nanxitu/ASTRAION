# API 鎺ュ彛璁捐

> ASTRAION 鎻愪緵涓ょ被 API锛?> 1. **绯荤粺 API** 鈥?鍒濆鍖栥€佽璇併€佹潈闄愩€佹祦绋嬬瓑鍐呯疆鍔熻兘
> 2. **鍔ㄦ€?API** 鈥?鏍规嵁涓氬姟妯″瀷鑷姩鐢熸垚鐨?CRUD API

---

## 1. 鍩虹绾﹀畾

```
鍩虹璺緞锛?api/v1
鍝嶅簲鏍煎紡锛?{
  "code": 0,        // 0=鎴愬姛锛岄潪0=閿欒鐮?  "message": "ok",
  "data": {}        // 瀹為檯鏁版嵁
}

璁よ瘉鏂瑰紡锛欱earer Token (JWT)
鍒嗛〉鍙傛暟锛?page=1&size=20
鎺掑簭鍙傛暟锛?sort=name:asc,createdAt:desc
绛涢€夊弬鏁帮細?filter=name:eq:寮犱笁
```

### 绛涢€夋搷浣滅

| 鎿嶄綔绗?| 璇存槑 | 绀轰緥 |
|--------|------|------|
| eq | 绛変簬 | name:eq:寮犱笁 |
| neq | 涓嶇瓑浜?| status:neq:disabled |
| gt | 澶т簬 | amount:gt:1000 |
| gte | 澶т簬绛変簬 | amount:gte:100 |
| lt | 灏忎簬 | amount:lt:500 |
| lte | 灏忎簬绛変簬 | amount:lte:500 |
| like | 妯＄硦鍖归厤 | name:like:寮? |
| in | 鍖呭惈 | status:in:active,disabled |
| between | 鑼冨洿 | createdAt:between:2024-01-01,2024-12-31 |

---

## 2. 璁よ瘉 API

#### POST /api/v1/auth/login

鐧诲綍鑾峰彇 Token銆?
```
Request:
{
  "username": "admin",
  "password": "***"
}

Response:
{
  "token": "eyJhbG鈥...",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "username": "ASTRAION",
    "displayName": "ASTRAION Root",
    "role": "root" | "admin" | "user"
  }
}
```

#### POST /api/v1/auth/logout

鐧诲嚭銆?
#### GET /api/v1/auth/me

鑾峰彇褰撳墠鐧诲綍鐢ㄦ埛淇℃伅锛堝惈 role锛屽墠绔嵁姝ゅ喅瀹氬彲鐢ㄧ殑浜や簰鑳藉姏锛夈€?
---

## 3. 绯荤粺鍒濆鍖?API锛堜粎 root锛?
#### POST /api/v1/system/init/database

閰嶇疆涓绘暟鎹簱銆?*鏉冮檺锛氫粎 root銆?*

```
Request:
{
  "dbType": "postgresql",
  "host": "localhost",
  "port": 5432,
  "databaseName": "astraion",
  "username": "postgres",
  "password": "***"
}

Response:
{
  "status": "ok",
  "message": "鏁版嵁搴撹繛鎺ユ垚鍔燂紝鍏冩暟鎹〃宸插垵濮嬪寲"
}
```

#### POST /api/v1/system/init/ai-model

閰嶇疆 AI 妯″瀷銆?*鏉冮檺锛氫粎 root銆?*

```
Request:
{
  "provider": "deepseek",
  "model": "deepseek-chat",
  "baseUrl": "https://api.deepseek.com",
  "apiKey": "sk-xxxxx",
  "temperature": 0.7,
  "maxTokens": 4096
}

Response:
{
  "status": "ok",
  "message": "AI 妯″瀷杩炴帴鎴愬姛"
}
```

#### POST /api/v1/system/init/admin

鍒涘缓绠＄悊鍛樿处鍙枫€?*鏉冮檺锛氫粎 root銆?*

```
Request:
{
  "username": "tunx",
  "password": "******",
  "displayName": "娑傚皯"
}

Response:
{
  "id": 2,
  "username": "tunx",
  "role": "admin",
  "message": "绠＄悊鍛樺垱寤烘垚鍔?
}
```

#### GET /api/v1/system/config

鏌ョ湅绯荤粺閰嶇疆锛圓PI Key 绛夋晱鎰熷瓧娈佃劚鏁忚繑鍥烇級銆?*鏉冮檺锛氫粎 root銆?*

```
Response:
{
  "initialized": true,
  "database": {
    "type": "postgresql",
    "host": "localhost",
    "port": 5432,
    "databaseName": "astraion"
  },
  "aiModel": {
    "provider": "deepseek",
    "model": "deepseek-chat",
    "apiKey": "sk-****...xxxx"    // 鑴辨晱
  }
}
```

#### PUT /api/v1/system/config/ai-model

鏇存崲 AI 妯″瀷銆?*鏉冮檺锛氫粎 root銆?*

#### PUT /api/v1/system/config/database

淇敼鏁版嵁搴撻厤缃€?*鏉冮檺锛氫粎 root銆?*

#### PUT /api/v1/system/admin/{id}/reset-password

閲嶇疆绠＄悊鍛樺瘑鐮併€?*鏉冮檺锛氫粎 root銆?*

---

## 4. 鏁版嵁婧愮鐞?API

#### POST /api/v1/datasources

杩藉姞澶栭儴鏁版嵁婧愩€?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

```
Request:
{
  "name": "鏃RM鏁版嵁搴?,
  "dbType": "mysql",
  "host": "192.168.1.100",
  "port": 3306,
  "databaseName": "legacy_crm",
  "username": "readonly_user",
  "password": "***",
  "mode": "readonly"
}
```

#### GET /api/v1/datasources

鏌ョ湅鎵€鏈夋暟鎹簮銆?
#### DELETE /api/v1/datasources/{id}

绉婚櫎澶栭儴鏁版嵁婧愩€?
#### POST /api/v1/datasources/{id}/import-tables

閫嗗悜宸ョ▼锛氳嚜鍔ㄨ鍙栬〃缁撴瀯骞跺垱寤烘ā鍨嬨€?
```
Request:
{
  "tables": ["t_customer", "t_order"],
  "prefix": "legacy_"       // 妯″瀷鍚嶅墠缂€
}

Response:
{
  "imported": [
    {"table": "t_customer", "model": "legacy_customer", "fields": 8},
    {"table": "t_order", "model": "legacy_order", "fields": 12}
  ]
}
```

---

## 5. 妯″瀷绠＄悊 API

#### POST /api/v1/models

鍒涘缓鏂版ā鍨嬨€?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

```
Request:
{
  "modelName": "customer",
  "displayName": "瀹㈡埛",
  "description": "瀹㈡埛淇℃伅绠＄悊",
  "fields": [
    {
      "name": "name",
      "type": "string",
      "label": "瀹㈡埛鍚嶇О",
      "required": true,
      "maxLength": 200
    },
    ...
  ]
}

Response:
{
  "modelName": "customer",
  "version": 1,
  "status": "active",
  "message": "妯″瀷鍒涘缓鎴愬姛锛屾暟鎹〃宸茶嚜鍔ㄥ缓濂?
}
```

#### GET /api/v1/models

鑾峰彇鎵€鏈夋ā鍨嬪垪琛ㄣ€?
#### GET /api/v1/models/{modelName}

鑾峰彇鍗曚釜妯″瀷鐨勫畬鏁村畾涔夈€?
#### PUT /api/v1/models/{modelName}

鏇存柊妯″瀷瀹氫箟锛堝鍔犲瓧娈碉級銆?
#### DELETE /api/v1/models/{modelName}

鍒犻櫎妯″瀷锛堜細鍒犻櫎瀵瑰簲鐨勫姩鎬佽〃鍜屾墍鏈夋暟鎹級銆?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

---

## 6. 鏉冮檺绠＄悊 API

#### GET /api/v1/roles

鑾峰彇鎵€鏈夎鑹层€?
#### POST /api/v1/roles

鍒涘缓瑙掕壊銆?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

#### PUT /api/v1/models/{modelName}/permissions

璁剧疆妯″瀷鐨勬潈闄愯鍒欍€?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

---

## 7. 娴佺▼绠＄悊 API

#### POST /api/v1/workflows

鍒涘缓瀹℃壒娴佺▼銆?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

#### POST /api/v1/workflows/{id}/start

鎵嬪姩鍚姩娴佺▼瀹炰緥銆?
#### POST /api/v1/tasks/{id}/complete

瀹屾垚涓€涓緟鍔炰换鍔★紙瀹℃壒锛夈€?
#### GET /api/v1/tasks?assignee=me&status=pending

鑾峰彇褰撳墠鐢ㄦ埛鐨勫緟鍔炰换鍔°€?
#### GET /api/v1/workflow-instances?modelName=leave_request&recordId=123

鏌ヨ鏌愪釜涓氬姟璁板綍鐨勫鎵硅繘搴︺€?
---

## 8. 鏂囨。绠＄悊 API

#### POST /api/v1/upload

涓婁紶鏂囦欢銆?
#### GET /api/v1/files/{id}

涓嬭浇鏂囦欢銆?
#### DELETE /api/v1/files/{id}

鍒犻櫎鏂囦欢銆?
---

## 9. 鐢ㄦ埛绠＄悊 API

#### POST /api/v1/users

鍒涘缓鐢ㄦ埛銆?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

```
Request:
{
  "username": "zhangsan",
  "password": "***",
  "displayName": "寮犱笁",
  "email": "zhangsan@company.com",
  "phone": "13800138000",
  "role": "user"
}
```

#### GET /api/v1/users

鑾峰彇鐢ㄦ埛鍒楄〃锛堟敮鎸佺瓫閫夈€佸垎椤碉級銆?
#### PUT /api/v1/users/{id}

鏇存柊鐢ㄦ埛淇℃伅銆?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

#### PUT /api/v1/users/{id}/password

淇敼瀵嗙爜锛堜粎鍙慨鏀硅嚜宸辩殑瀵嗙爜锛屾垨 admin 淇敼浠栦汉锛夈€?
#### DELETE /api/v1/users/{id}

鍒犻櫎鐢ㄦ埛銆?*鏉冮檺锛歛dmin 鍙婁互涓娿€?* root 鐢ㄦ埛涓嶅彲鍒犻櫎銆?
---

## 10. 閫氱煡绠＄悊 API

#### GET /api/v1/notifications?unread=true

鑾峰彇閫氱煡鍒楄〃銆?
#### PUT /api/v1/notifications/{id}/read

鏍囪宸茶銆?
#### PUT /api/v1/notifications/read-all

鍏ㄩ儴鏍囪宸茶銆?
---

## 11. 瀹¤鏃ュ織 API

#### GET /api/v1/audit-logs

鏌ヨ瀹¤鏃ュ織銆?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

---

## 12. 鎻掍欢绠＄悊 API

#### POST /api/v1/plugins/upload

涓婁紶鎻掍欢 jar 鍖呫€?*鏉冮檺锛歛dmin 鍙婁互涓娿€?*

#### DELETE /api/v1/plugins/{name}

鍗歌浇鎻掍欢銆?
#### PUT /api/v1/plugins/{name}/reload

閲嶆柊鍔犺浇鎻掍欢锛堢儹鍔犺浇锛屼笉閲嶅惎锛夈€?
#### GET /api/v1/plugins

鑾峰彇宸叉敞鍐岀殑鎻掍欢鍒楄〃銆?
---

## 13. 鏅鸿兘浣撶鐞?API锛坴2 棰勭暀锛?
#### POST /api/v1/agents

鍒涘缓鏅鸿兘浣撱€?*鏉冮檺锛歛dmin 鍙婁互涓娿€?* v2 鍚敤銆?
#### GET /api/v1/agents

鑾峰彇鏅鸿兘浣撳垪琛ㄣ€倂2 鍚敤銆?
#### PUT /api/v1/agents/{id}

鏇存柊鏅鸿兘浣撻厤缃€倂2 鍚敤銆?
#### PUT /api/v1/agents/{id}/activate

婵€娲绘櫤鑳戒綋銆倂2 鍚敤銆?
#### PUT /api/v1/agents/{id}/pause

鏆傚仠鏅鸿兘浣撱€倂2 鍚敤銆?
#### DELETE /api/v1/agents/{id}

鍒犻櫎鏅鸿兘浣撱€倂2 鍚敤銆?
---

## 14. 鍔ㄦ€?API锛堣嚜鍔ㄧ敓鎴愶級

姣忎釜涓氬姟妯″瀷鍒涘缓鍚庯紝鑷姩鐢熸垚浠ヤ笅 API锛?
| 鏂规硶 | 璺緞 | 璇存槑 |
|------|------|------|
| POST | /api/v1/data/{modelName} | 鍒涘缓璁板綍 |
| GET | /api/v1/data/{modelName} | 鏌ヨ鍒楄〃锛堝垎椤?绛涢€?鎺掑簭锛?|
| GET | /api/v1/data/{modelName}/{id} | 鏌ヨ鍗曟潯 |
| PUT | /api/v1/data/{modelName}/{id} | 鏇存柊璁板綍 |
| DELETE | /api/v1/data/{modelName}/{id} | 鍒犻櫎璁板綍 |
| POST | /api/v1/data/{modelName}/batch | 鎵归噺鍒涘缓 |
| PUT | /api/v1/data/{modelName}/batch | 鎵归噺鏇存柊 |
| DELETE | /api/v1/data/{modelName}/batch | 鎵归噺鍒犻櫎 |
| GET | /api/v1/data/{modelName}/{id}/{relation} | 鍏宠仈鏌ヨ |
| GET | /api/v1/data/{modelName}/aggregate | 鑱氬悎鏌ヨ |

---

## 15. AI 浜や簰 API

### 15.1 WebSocket 瀵硅瘽

```
WS /api/v1/ai/chat

瀹㈡埛绔?鈫?鏈嶅姟绔細
{
  "type": "message",
  "content": "甯垜鏌ヤ竴涓媀IP瀹㈡埛"
}

鏈嶅姟绔?鈫?瀹㈡埛绔紙娴佸紡锛夛細
{
  "type": "thinking",
  "content": "姝ｅ湪鏌ヨ..."
}
{
  "type": "render",
  "component": "table",
  "props": { "columns": [...], "data": [...] }
}
{
  "type": "message",
  "content": "鍏辨湁3浣峍IP瀹㈡埛銆?
}
```

### 15.2 AI Tool 闆嗭紙渚?LLM 璋冪敤锛?
```
createModel      鈥?鍒涘缓涓氬姟妯″瀷
updateModel      鈥?鏇存柊妯″瀷瀹氫箟
deleteModel      鈥?鍒犻櫎妯″瀷
queryData        鈥?鏌ヨ鏁版嵁
createData       鈥?鍒涘缓鏁版嵁
updateData       鈥?鏇存柊鏁版嵁
deleteData       鈥?鍒犻櫎鏁版嵁
submitWorkflow   鈥?鎻愪氦瀹℃壒
approveTask      鈥?瀹℃壒浠诲姟
queryTasks       鈥?鏌ヨ寰呭姙
createUser       鈥?鍒涘缓鐢ㄦ埛
setPermission    鈥?璁剧疆鏉冮檺
addDatasource    鈥?杩藉姞鏁版嵁婧?importTable      鈥?瀵煎叆澶栭儴琛?uploadFile       鈥?涓婁紶鏂囦欢
```

---

## 16. API 鏉冮檺鐭╅樀

| API 鍒嗙粍 | ROOT | ADMIN | USER |
|----------|------|-------|------|
| **绯荤粺鍒濆鍖?*锛堟暟鎹簱/AI妯″瀷锛?| 鉁?| 鉂?| 鉂?|
| **鏌ョ湅绯荤粺閰嶇疆**锛圓PI Key 鑴辨晱锛?| 鉁?| 鉂?| 鉂?|
| **鏇存崲 AI 妯″瀷** | 鉁?| 鉂?| 鉂?|
| **閲嶇疆绠＄悊鍛樺瘑鐮?* | 鉁?| 鉂?| 鉂?|
| **鏁版嵁婧愮鐞?* | 鉁?| 鉁?| 鉂?|
| **妯″瀷绠＄悊** | 鉂?| 鉁?| 鉂?|
| **鏉冮檺閰嶇疆** | 鉂?| 鉁?| 鉂?|
| **娴佺▼閰嶇疆** | 鉂?| 鉁?| 鉂?|
| **鐢ㄦ埛绠＄悊** | 鉂?| 鉁?| 鉂?|
| **鎻掍欢绠＄悊** | 鉂?| 鉁?| 鉂?|
| **鏅鸿兘浣撶鐞嗭紙v2锛?* | 鉂?| 鉁?| 鉂?|
| **瀹¤鏃ュ織** | 鉂?| 鉁?| 鉂?|
| **涓氬姟鏁版嵁 CRUD** | 鉂?| 鉁?| 鉁咃紙鏉冮檺鍐咃級 |
| **瀹℃壒/寰呭姙** | 鉂?| 鉁?| 鉁咃紙鏉冮檺鍐咃級 |
| **閫氱煡** | 鉂?| 鉁?| 鉁咃紙鑷繁鐨勶級 |
| **淇敼鑷繁瀵嗙爜** | 鉁?| 鉁?| 鉁?|
