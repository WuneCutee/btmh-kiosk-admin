package vn.baotinmanhhai.kioskadmin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.dto.CategoryRequest;
import vn.baotinmanhhai.kioskadmin.dto.CategoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.CategoryUpdateRequest;
import vn.baotinmanhhai.kioskadmin.dto.PageResponse;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;
import vn.baotinmanhhai.kioskadmin.filter.JwtAuthFilter;
import vn.baotinmanhhai.kioskadmin.service.AuditLogService;
import vn.baotinmanhhai.kioskadmin.service.CategoryService;

import java.util.List;
import java.util.Map;

/** Nhom san pham - proxy sang admin-portal (xem CategoryService). */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final AuditLogService auditLogService;

    @GetMapping
    public ApiResponse<PageResponse<CategoryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.ok(categoryService.list(page, size));
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request,
                                                @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                                HttpServletRequest httpRequest) {
        CategoryResponse category = categoryService.create(request);
        auditLogService.record(actor, "CATEGORY_CREATE", "Category", category.getId(), category.getName(), httpRequest);
        return ApiResponse.ok(category);
    }

    @PutMapping("/{id}")
    public ApiResponse<JsonNode> update(@PathVariable String id, @Valid @RequestBody CategoryUpdateRequest request,
                                        @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                        HttpServletRequest httpRequest) {
        JsonNode category = categoryService.update(id, request);
        auditLogService.record(actor, "CATEGORY_UPDATE", "Category", id, request.getName(), httpRequest);
        return ApiResponse.ok(category);
    }

    /** Body {visible: true|false}. */
    @PatchMapping("/{id}/visibility")
    public ApiResponse<JsonNode> setVisibility(@PathVariable String id, @RequestBody Map<String, Object> body,
                                               @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                               HttpServletRequest httpRequest) {
        if (!(body.get("visible") instanceof Boolean visible)) {
            throw new BusinessException("Thieu truong visible (true/false)", "400", 400);
        }
        JsonNode result = categoryService.setVisible(id, visible);
        auditLogService.record(actor, "CATEGORY_VISIBILITY", "Category", id, "visible=" + visible, httpRequest);
        return ApiResponse.ok(result);
    }

    /** Body [{categoryId, priority}]. */
    @PutMapping("/priority")
    public ApiResponse<JsonNode> updatePriorities(@RequestBody List<Map<String, Object>> priorities,
                                                  @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                                  HttpServletRequest httpRequest) {
        JsonNode result = categoryService.updatePriorities(priorities);
        auditLogService.record(actor, "CATEGORY_PRIORITY", "Category", null, priorities.size() + " nhom", httpRequest);
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @RequestAttribute(JwtAuthFilter.REQUEST_ATTR_USERNAME) String actor,
                                    HttpServletRequest httpRequest) {
        categoryService.deleteFromKiosk(id);
        auditLogService.record(actor, "CATEGORY_DELETE", "Category", id, null, httpRequest);
        return ApiResponse.ok(null);
    }
}
