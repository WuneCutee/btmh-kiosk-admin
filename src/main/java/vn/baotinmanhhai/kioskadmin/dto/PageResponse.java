package vn.baotinmanhhai.kioskadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Hinh dang trang RIENG cua app nay (content/totalPages/totalElements/page/size) - khop voi
 * cach UI (app.js) da doc cac tab log co san (pageData.content, pageData.totalPages). Dung de
 * "dich" tu AdminPortalPage (hinh dang cua admin-portal) sang hinh dang quen thuoc nay, de phan
 * con lai cua app khong phai biet toi 2 kieu phan trang khac nhau.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int page;
    private int size;

    public static <T> PageResponse<T> from(AdminPortalPage<T> p) {
        return PageResponse.<T>builder()
                .content(p.getData())
                .totalPages(p.getTotalPages() != null ? p.getTotalPages() : 0)
                .totalElements(p.getTotal() != null ? p.getTotal() : 0L)
                .page(p.getPage() != null ? p.getPage() : 0)
                .size(p.getPageSize() != null ? p.getPageSize() : 0)
                .build();
    }
}
