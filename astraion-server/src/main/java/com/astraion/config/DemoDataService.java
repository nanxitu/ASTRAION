package com.astraion.config;

import com.astraion.core.engine.DynamicTableManager;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.FieldDef;
import com.astraion.model.ModelMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 演示数据服务 — 一键创建典型 OA 系统模型和示例数据
 * 仅供演示，admin 可随时清除
 */
@Component
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private final MetadataEngine metadataEngine;
    private final DynamicTableManager tableManager;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataService(MetadataEngine metadataEngine, DynamicTableManager tableManager,
                           JdbcTemplate jdbcTemplate) {
        this.metadataEngine = metadataEngine;
        this.tableManager = tableManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 检查演示数据是否已加载 */
    public boolean isLoaded() {
        try {
            metadataEngine.getModel("department");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 一键创建演示数据 */
    public Map<String, Object> loadDemoData() {
        int modelCount = 0;
        int dataCount = 0;

        try {
            // ── 1. 部门 ──
            createDepartmentModel(); modelCount++;
            Long dept1 = insertDept("技术部", "负责产品研发和技术架构", "张伟");
            Long dept2 = insertDept("市场部", "负责市场推广和品牌运营", "李娜");
            Long dept3 = insertDept("人事部", "负责人力资源和行政管理", "王芳");
            Long dept4 = insertDept("财务部", "负责财务管理和成本控制", "陈强");
            dataCount += 4;

            // ── 2. 员工 ──
            createEmployeeModel(); modelCount++;
            insertEmp("张伟", dept1, "部门经理", "zhangwei@company.com", "13800000001");
            insertEmp("刘洋", dept1, "高级工程师", "liuyang@company.com", "13800000002");
            insertEmp("陈敏", dept1, "前端工程师", "chenmin@company.com", "13800000003");
            insertEmp("李娜", dept2, "部门经理", "lina@company.com", "13800000004");
            insertEmp("周杰", dept2, "客户专员", "zhoujie@company.com", "13800000005");
            insertEmp("王芳", dept3, "部门经理", "wangfang@company.com", "13800000006");
            insertEmp("赵磊", dept3, "HR主管", "zhaolei@company.com", "13800000007");
            insertEmp("陈强", dept4, "部门经理", "chenqiang@company.com", "13800000008");
            insertEmp("孙悦", dept4, "会计", "sunyue@company.com", "13800000009");
            insertEmp("马超", dept1, "后端工程师", "machao@company.com", "13800000010");
            dataCount += 10;

            // ── 3. 请假申请 ──
            createLeaveRequestModel(); modelCount++;
            insertLeave(1L, "年假", "2026-06-20", "2026-06-21", "approved", "同意，好好休息");
            insertLeave(5L, "事假", "2026-06-18", "2026-06-18", "approved", "批准");
            insertLeave(3L, "病假", "2026-06-22", "2026-06-23", "pending", null);
            insertLeave(8L, "年假", "2026-07-01", "2026-07-05", "pending", null);
            insertLeave(7L, "婚假", "2026-07-10", "2026-07-17", "pending", null);
            dataCount += 5;

            // ── 4. 报销申请 ──
            createExpenseModel(); modelCount++;
            insertExpense(2L, "差旅费", "北京出差机票+酒店", 3580.00, "approved", "合理支出");
            insertExpense(5L, "招待费", "客户餐饮接待", 865.00, "approved", "已核实");
            insertExpense(1L, "办公用品", "机械键盘+显示器支架", 1560.00, "approved", null);
            insertExpense(10L, "培训费", "技术大会门票", 2800.00, "pending", null);
            insertExpense(6L, "交通费", "6月打车发票", 430.00, "pending", null);
            dataCount += 5;

            // ── 5. 公告通知 ──
            createAnnouncementModel(); modelCount++;
            insertAnnounce("端午节放假通知", "根据国家规定，6月19日-6月21日放假3天...", "important", "王芳");
            insertAnnounce("Q2技术评审安排", "本周五下午2点在3楼会议室进行Q2技术评审...", "normal", "张伟");
            insertAnnounce("新员工入职培训", "下周一上午9点，请各部门新员工到培训室参加入职培训...", "normal", "赵磊");
            dataCount += 3;

            // ── 6. 会议室 ──
            createMeetingRoomModel(); modelCount++;
            insertRoom("A-301", "小型会议室", 8, "3楼东侧", "投影仪、白板");
            insertRoom("B-101", "大型会议厅", 50, "1楼", "投影仪、音响、视频会议");
            insertRoom("A-205", "洽谈室", 4, "2楼西侧", "白板");
            insertRoom("B-202", "培训室", 30, "2楼", "投影仪、音响、直播设备");
            dataCount += 4;

            // ── 7. 会议预约 ──
            createMeetingBookingModel(); modelCount++;
            insertBooking(1L, 1L, "Q2技术评审", "2026-06-18", "14:00", "16:00", "张伟");
            insertBooking(3L, 5L, "客户需求沟通", "2026-06-19", "10:00", "11:30", "李娜");
            insertBooking(2L, 7L, "新人培训", "2026-06-23", "09:00", "12:00", "赵磊");
            insertBooking(4L, 10L, "架构升级讨论", "2026-06-20", "15:00", "16:30", "马超");
            dataCount += 4;

            log.info("[Demo] Created {} models with {} demo records", modelCount, dataCount);
            return Map.of("success", true, "message",
                "OA演示数据已就绪：创建了 " + modelCount + " 个模型，" + dataCount + " 条示例数据",
                "modelCount", modelCount, "dataCount", dataCount);

        } catch (Exception e) {
            log.error("[Demo] Failed to create demo data", e);
            return Map.of("success", false, "message", "创建演示数据失败: " + e.getMessage());
        }
    }

    /** 清除所有演示数据（删除业务模型及数据） */
    public Map<String, Object> clearDemoData() {
        String[] models = {"meeting_booking", "meeting_room", "announcement",
            "expense_report", "leave_request", "employee", "department"};
        int removed = 0;
        for (String name : models) {
            try {
                metadataEngine.deleteModel(name);
                tableManager.dropTable(name);
                removed++;
                log.info("[Demo] Removed model: {}", name);
            } catch (Exception e) {
                log.warn("[Demo] Could not remove {}: {}", name, e.getMessage());
            }
        }
        return Map.of("success", true, "message",
            "已清除 " + removed + " 个演示模型及所有数据。系统已恢复干净状态。",
            "removedCount", removed);
    }

    // ═══════════════ Model Definitions ═══════════════

    private void createDepartmentModel() {
        createModel("department", "部门", "公司组织架构",
            List.of(fd("name", "string", "部门名称", true),
                    fd("description", "text", "部门描述"),
                    fd("manager_name", "string", "负责人"),
                    fd("employee_count", "integer", "员工数")));
    }

    private void createEmployeeModel() {
        createModel("employee", "员工", "员工档案信息",
            List.of(fd("name", "string", "姓名", true),
                    fd("dept_id", "relation", "所属部门", false, "department"),
                    fd("position", "string", "职位"),
                    fd("email", "email", "邮箱"),
                    fd("phone", "phone", "手机号"),
                    fd("hire_date", "date", "入职日期"),
                    fd("status", "enum", "状态", false, null,
                        List.of("在职", "离职", "休假"))));
    }

    private void createLeaveRequestModel() {
        createModel("leave_request", "请假申请", "员工请假审批",
            List.of(fd("employee_id", "relation", "申请人", true, "employee"),
                    fd("leave_type", "enum", "请假类型", true, null,
                        List.of("年假", "事假", "病假", "婚假", "产假", "调休")),
                    fd("start_date", "date", "开始日期", true),
                    fd("end_date", "date", "结束日期", true),
                    fd("reason", "text", "请假理由"),
                    fd("status", "enum", "审批状态", false, null,
                        List.of("pending", "approved", "rejected")),
                    fd("approval_comment", "text", "审批意见")));
    }

    private void createExpenseModel() {
        createModel("expense_report", "报销申请", "费用报销审批",
            List.of(fd("employee_id", "relation", "申请人", true, "employee"),
                    fd("expense_type", "enum", "报销类型", true, null,
                        List.of("差旅费", "招待费", "办公用品", "培训费", "交通费", "其他")),
                    fd("description", "text", "费用说明", true),
                    fd("amount", "decimal", "金额", true),
                    fd("status", "enum", "审批状态", false, null,
                        List.of("pending", "approved", "rejected")),
                    fd("approval_comment", "text", "审批意见")));
    }

    private void createAnnouncementModel() {
        createModel("announcement", "公告通知", "公司公告发布",
            List.of(fd("title", "string", "标题", true),
                    fd("content", "text", "内容", true),
                    fd("level", "enum", "重要程度", false, null,
                        List.of("normal", "important", "urgent")),
                    fd("publisher", "string", "发布人"),
                    fd("publish_date", "datetime", "发布时间")));
    }

    private void createMeetingRoomModel() {
        createModel("meeting_room", "会议室", "会议室资源管理",
            List.of(fd("code", "string", "编号", true),
                    fd("type", "string", "类型"),
                    fd("capacity", "integer", "容量"),
                    fd("location", "string", "位置"),
                    fd("facilities", "text", "设备")));
    }

    private void createMeetingBookingModel() {
        createModel("meeting_booking", "会议预约", "会议室预约管理",
            List.of(fd("room_id", "relation", "会议室", true, "meeting_room"),
                    fd("employee_id", "relation", "预约人", true, "employee"),
                    fd("title", "string", "会议主题", true),
                    fd("meeting_date", "date", "日期", true),
                    fd("start_time", "string", "开始时间"),
                    fd("end_time", "string", "结束时间"),
                    fd("organizer", "string", "组织者")));
    }

    // ═══════════════ Data Insertion ═══════════════

    private Long insertDept(String name, String desc, String mgr) {
        var rows = jdbcTemplate.queryForList(
            "SELECT * FROM astraion_data_department WHERE name=?", name);
        if (!rows.isEmpty()) return ((Number) rows.get(0).get("id")).longValue();
        jdbcTemplate.update(
            "INSERT INTO astraion_data_department (name, description, manager_name, employee_count) VALUES (?,?,?,?)",
            name, desc, mgr, 0);
        return jdbcTemplate.queryForObject("SELECT LASTVAL()", Long.class);
    }

    private void insertEmp(String name, Long deptId, String pos, String email, String phone) {
        jdbcTemplate.update(
            "INSERT INTO astraion_data_employee (name, dept_id, position, email, phone, hire_date, status) " +
            "VALUES (?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
            name, deptId, pos, email, phone, "2025-03-01", "在职");
    }

    private void insertLeave(Long empId, String type, String start, String end, String status, String comment) {
        jdbcTemplate.update(
            "INSERT INTO astraion_data_leave_request (employee_id, leave_type, start_date, end_date, reason, status, approval_comment) " +
            "VALUES (?,?,?,?,?,?,?)",
            empId, type, start, end, type + "申请", status, comment);
    }

    private void insertExpense(Long empId, String type, String desc, double amount, String status, String comment) {
        jdbcTemplate.update(
            "INSERT INTO astraion_data_expense_report (employee_id, expense_type, description, amount, status, approval_comment) " +
            "VALUES (?,?,?,?,?,?)",
            empId, type, desc, amount, status, comment);
    }

    private void insertAnnounce(String title, String content, String level, String publisher) {
        jdbcTemplate.update(
            "INSERT INTO astraion_data_announcement (title, content, level, publisher, publish_date) VALUES (?,?,?,?,?)",
            title, content, level, publisher, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    private void insertRoom(String code, String type, int cap, String loc, String fac) {
        jdbcTemplate.update(
            "INSERT INTO astraion_data_meeting_room (code, type, capacity, location, facilities) VALUES (?,?,?,?,?)",
            code, type, cap, loc, fac);
    }

    private void insertBooking(Long roomId, Long empId, String title, String date, String start, String end, String org) {
        jdbcTemplate.update(
            "INSERT INTO astraion_data_meeting_booking (room_id, employee_id, title, meeting_date, start_time, end_time, organizer) " +
            "VALUES (?,?,?,?,?,?,?)",
            roomId, empId, title, date, start, end, org);
    }

    // ═══════════════ Helpers ═══════════════

    private void createModel(String name, String display, String desc, List<FieldDef> fields) {
        try {
            metadataEngine.getModel(name); // already exists
            return;
        } catch (Exception ignored) {}
        ModelMeta meta = new ModelMeta();
        meta.setModelName(name);
        meta.setDisplayName(display);
        meta.setDescription(desc);
        meta.setBuiltin(false);
        meta.setVersion(1);
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setOrder(i + 1);
        }
        meta.setFields(fields);
        metadataEngine.createModel(meta);
        tableManager.createTable(meta);
        log.info("[Demo] Model created: {}", name);
    }

    private FieldDef fd(String name, String type, String label) {
        return fd(name, type, label, false);
    }

    private FieldDef fd(String name, String type, String label, boolean required) {
        return fd(name, type, label, required, null, null);
    }

    private FieldDef fd(String name, String type, String label, boolean required, String targetModel) {
        return fd(name, type, label, required, targetModel, null);
    }

    private FieldDef fd(String name, String type, String label, boolean required,
                         String targetModel, List<String> options) {
        FieldDef f = new FieldDef();
        f.setName(name);
        f.setType(type);
        f.setLabel(label);
        f.setRequired(required);
        if (targetModel != null) f.setTargetModel(targetModel);
        // Note: options not supported by FieldDef yet; stored as part of model config
        return f;
    }
}
