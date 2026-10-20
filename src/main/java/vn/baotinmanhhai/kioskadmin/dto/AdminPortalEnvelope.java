package vn.baotinmanhhai.kioskadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Vo boc phan hoi CHUNG cua admin-portal ({status, message, data, fieldErrors}) - KHAC voi
 * ApiResponse<T> cua chinh app nay ({success, code, message, data}). Chi dung NOI BO trong
 * AdminPortalAuthClient/AdminPortalClient de giai ma HTTP response cua admin-portal - phan con
 * lai cua app KHONG duoc biet toi hinh dang nay, luon di qua lop adapter o day truoc.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminPortalEnvelope<T> {
    public static final String STATUS_SUCCESS = "SYS-200";

    private String status;
    private String message;
    private T data;
    private List<Map<String, Object>> fieldErrors;
}
