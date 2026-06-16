package com.astraion.web;

import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 文档/文件管理控制器
 */
@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final JdbcTemplate jdbcTemplate;
    private final Path uploadDir = Paths.get("uploads");

    public DocumentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        try { Files.createDirectories(uploadDir); } catch (IOException ignored) {}
    }

    /** 上传文件 */
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) Long recordId,
            @RequestParam(required = false) String fieldName,
            HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        try {
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".") ?
                originalName.substring(originalName.lastIndexOf('.')) : "";
            String storedName = UUID.randomUUID().toString() + ext;
            Path target = uploadDir.resolve(storedName);
            file.transferTo(target);

            jdbcTemplate.update(
                "INSERT INTO astraion_document (file_name, file_url, file_size, mime_type, model_name, record_id, field_name, uploaded_by) VALUES (?,?,?,?,?,?,?,?)",
                originalName, storedName, file.getSize(), file.getContentType(), modelName, recordId, fieldName, ctx.getUserId());

            Long docId = jdbcTemplate.queryForObject("SELECT LASTVAL()", Long.class);
            return ApiResponse.ok("上传成功", Map.of("id", docId, "fileName", originalName, "fileUrl", storedName));
        } catch (Exception e) {
            return ApiResponse.fail(500, "上传失败: " + e.getMessage());
        }
    }

    /** 下载文件 */
    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        var rows = jdbcTemplate.queryForList(
            "SELECT file_name, file_url, mime_type FROM astraion_document WHERE id=?", id);
        if (rows.isEmpty()) return ResponseEntity.notFound().build();

        String fileName = (String) rows.get(0).get("file_name");
        String fileUrl = (String) rows.get(0).get("file_url");
        String mimeType = (String) rows.get(0).get("mime_type");

        File file = uploadDir.resolve(fileUrl).toFile();
        if (!file.exists()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
            .contentType(mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .body(new FileSystemResource(file));
    }

    /** 删除文件 */
    @DeleteMapping("/files/{id}")
    public ApiResponse<Void> deleteFile(@PathVariable Long id) {
        var rows = jdbcTemplate.queryForList("SELECT file_url FROM astraion_document WHERE id=?", id);
        if (!rows.isEmpty()) {
            try { Files.deleteIfExists(uploadDir.resolve((String) rows.get(0).get("file_url"))); } catch (IOException ignored) {}
            jdbcTemplate.update("DELETE FROM astraion_document WHERE id=?", id);
        }
        return ApiResponse.ok(null);
    }

    private UserContext getUserContext(HttpServletRequest request) {
        return (UserContext) request.getAttribute("userContext");
    }
}
