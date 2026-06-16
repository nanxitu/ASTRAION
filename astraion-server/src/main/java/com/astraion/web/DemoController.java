package com.astraion.web;

import com.astraion.config.DemoDataService;
import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 演示数据控制器 — admin 一键加载/清除 OA 演示数据
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final DemoDataService demoService;

    public DemoController(DemoDataService demoService) {
        this.demoService = demoService;
    }

    /** 检查演示数据状态 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(Map.of("loaded", demoService.isLoaded()));
    }

    /** 加载 OA 演示数据 */
    @PostMapping("/load")
    public ApiResponse<Map<String, Object>> load(HttpServletRequest request) {
        UserContext ctx = getCtx(request);
        if (!ctx.isAdmin()) return ApiResponse.fail(403, "仅管理员可操作");
        Map<String, Object> result = demoService.loadDemoData();
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        return ok ? ApiResponse.ok((String) result.get("message"), result)
                  : ApiResponse.fail(500, (String) result.get("message"));
    }

    /** 清除所有演示数据 */
    @PostMapping("/clear")
    public ApiResponse<Map<String, Object>> clear(HttpServletRequest request) {
        UserContext ctx = getCtx(request);
        if (!ctx.isAdmin()) return ApiResponse.fail(403, "仅管理员可操作");
        Map<String, Object> result = demoService.clearDemoData();
        return ApiResponse.ok((String) result.get("message"), result);
    }

    private UserContext getCtx(HttpServletRequest r) {
        return (UserContext) r.getAttribute("userContext");
    }
}
