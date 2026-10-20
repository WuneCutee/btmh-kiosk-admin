package vn.baotinmanhhai.kioskadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Hinh dang trang (pagination) cua admin-portal (PagedResponseData ben do) - KHAC voi
 * Page<T> chuan cua Spring Data (content/totalElements/...) ma cac API doc khac trong app nay
 * dang tra ve. Da xac nhan tung truong qua curl thuc te vao admin-portal dang chay
 * (vi du {"page":0,"pageSize":1,"totalPages":1,"total":1,"data":[...],"last":true}) - KHONG
 * doan, "last" khong co tien to "is" trong JSON du field Java ben kia ten "isLast".
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminPortalPage<T> {
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Long total;
    private boolean last;
    private List<T> data;
}
