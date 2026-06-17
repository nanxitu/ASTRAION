# ASTRAION 动态字段类型规范

> 最后修订：2026-06-16 | ASTRAION v1.0

本规范定义动态模型字段到 PostgreSQL 列类型的映射规则，以及数据插入的类型安全约束。

---

## 一、字段类型 → SQL 列类型映射

| 字段类型 (field.type) | PostgreSQL 类型 | 说明 |
|---|---|---|
| `string` | `VARCHAR(255)` | 定长文本，默认 255 |
| `text` | `TEXT` | 不定长文本 |
| `integer` | `INTEGER` | 整数 |
| `decimal` | `DECIMAL(18,6)` | 高精度小数 |
| `boolean` | `BOOLEAN` | 布尔值 |
| `date` | `DATE` | 日期，**插入必须用 LocalDate 或 `?::date`** |
| `datetime` | `TIMESTAMP` | 时间戳，**插入必须用 LocalDateTime** |
| `email` | `VARCHAR(255)` | 邮箱地址 |
| `phone` | `VARCHAR(255)` | 电话号码 |
| `url` | `VARCHAR(255)` | URL 地址 |
| `autoNumber` | `VARCHAR(255)` | 自动编号 |
| `encrypted` | `VARCHAR(512)` | 加密字段 |
| `password` | `VARCHAR(512)` | 密码字段 |
| `file` | `VARCHAR(1024)` | 文件路径/URL |
| `image` | `VARCHAR(1024)` | 图片路径/URL |
| `enum` | `VARCHAR(255)` | **简单枚举值已改为 VARCHAR，兼容纯字符串插入** |
| `enumMulti` | `JSONB` | 多选枚举，需要结构化存储 |
| `relation` | `BIGINT` | 外键关联，存储目标模型记录 ID |
| `relationMulti` | `JSONB` | 多对多关联，JSON 数组 |
| `json` | `JSONB` | 明确的结构化数据 |

---

## 二、数据插入类型安全规则

### 🚫 禁止行为

| 列类型 | ❌ 错误做法 | ✅ 正确做法 |
|---|---|---|
| `DATE` | `jdbcTemplate.update("... VALUES (?,?,?)", ..., "2025-03-01")` | 传 `java.time.LocalDate` 对象，或 SQL 中用 `?::date` |
| `TIMESTAMP` | `jdbcTemplate.update("... VALUES (?,?,?)", ..., "2026-06-16 10:00:00")` | 传 `java.time.LocalDateTime` 对象 |
| `JSONB` | `jdbcTemplate.update("... VALUES (?)", ..., "在职")` | 传对象/Map，或 `to_jsonb(?)`，或用 PGobject |
| `DECIMAL(18,6)` | 传字符串 `"3580.00"` | 传 `double` 或 `BigDecimal` |

### 规则

1. **DATE 列**：Java 端传 `LocalDate`，SQL 端可用 `?::date` 强制转换（仅限 demo 等一次性数据）
2. **TIMESTAMP 列**：Java 端传 `LocalDateTime`，不解需要 `?::timestamp` 转换
3. **JSONB 列**：Java 端传 `Map`/`List` 对象或使用 PGobject，不允许裸字符串
4. **简单枚举**：使用 `VARCHAR(255)` 类型（已修改），可插入纯字符串
5. **生产环境的 CRUD 引擎**：应自动检测字段类型并做类型转换，而不是依赖调用方手动处理

---

## 三、CRUD 引擎类型自动转换（规划）

生产环境中，`CreateDataTool` / `UpdateDataTool` 在插入/更新数据时，应自动完成类型转换：

```java
// 伪代码
Object convertValue(FieldDef field, Object rawValue) {
    return switch (field.getType()) {
        case "date"      -> rawValue instanceof LocalDate ? rawValue : LocalDate.parse(rawValue.toString());
        case "datetime"  -> rawValue instanceof LocalDateTime ? rawValue : LocalDateTime.parse(rawValue.toString());
        case "integer"   -> rawValue instanceof Integer ? rawValue : Integer.parseInt(rawValue.toString());
        case "decimal"   -> rawValue instanceof Double ? rawValue : Double.parseDouble(rawValue.toString());
        case "boolean"   -> rawValue instanceof Boolean ? rawValue : Boolean.parseBoolean(rawValue.toString());
        case "json", "enumMulti", "relationMulti" -> rawValue instanceof String ? rawValue : objectMapper.writeValueAsString(rawValue);
        default          -> rawValue.toString();
    };
}
```

> ⚠️ 当前为 **Demo 模式**，手动处理类型转换。生产模式应实现此配置。

---

## 四、PostgreSQL 兼容性注意事项

| 场景 | 说明 |
|---|---|
| `CREATE TABLE IF NOT EXISTS` | 幂等建表，保证重复创建安全 |
| `LASTVAL()` | Demo 专用，获取自增 ID。生产应改用 `RETURNING id` |
| `ON CONFLICT DO NOTHING` | **需要唯一约束**，否则语法报错 |
| `?::type` 显式转换 | 可用于强制类型转换，但 JDBC 驱动可能不兼容，优先用 Java 端传正确类型 |

---

## 五、修改记录

| 日期 | 变更 | 影响 |
|---|---|---|
| 2026-06-16 | `enum` SQL 映射从 JSONB 改为 VARCHAR(255) | Demo 数据和所有新建的 enum 模型字段不再需要 JSON 转换 |
| 2026-06-16 | Demo 模型 enum 字段暂时改为 string | 已通过引擎映射修复，后续可恢复 enum 类型 |
