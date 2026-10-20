package vn.baotinmanhhai.kioskadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.baotinmanhhai.kioskadmin.dto.PageResponse;
import vn.baotinmanhhai.kioskadmin.dto.ProductRequest;
import vn.baotinmanhhai.kioskadmin.dto.ProductResponse;

/**
 * San pham - KHONG con la shadow mapping truc tiep vao bang "product" nhu truoc (entity Product
 * cu da go bo, xem README). Ly do: tao thang vao bang "product" KHONG lam san pham xuat hien
 * tren kiosk app thuc, vi kiosk app con can them partner_product_config (partner_code=KIOSK)
 * va 1 dong inventory (so con lai thuc nam o Redis) - ca 2 thu nay admin-portal da lam DUNG va
 * AN TOAN (khoa Redis, kiem tra gio giao dich, dong bo delta...), nen kiosk-admin chi goi API
 * cua admin-portal thay vi tu viet lai.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int DEFAULT_SIZE = 100;

    private final AdminPortalClient adminPortalClient;

    public PageResponse<ProductResponse> list(String categoryCode, int page, int size) {
        return adminPortalClient.listProducts(categoryCode, page, size <= 0 ? DEFAULT_SIZE : size);
    }

    public ProductResponse get(String id) {
        return adminPortalClient.getProduct(id);
    }

    public ProductResponse create(ProductRequest request) {
        // San pham moi tao luon invisible=true theo mac dinh ben admin-portal - nguoi dung bam
        // "Hien/An" rieng sau khi xac nhan thong tin dung (xem toggleVisibility), tranh san pham
        // loi xuat hien ngay tren kiosk thuc truoc khi kiem tra lai.
        return adminPortalClient.createProduct(request);
    }

    public ProductResponse update(String id, ProductRequest request) {
        return adminPortalClient.updateProduct(id, request);
    }

    public void delete(String id) {
        adminPortalClient.deleteProduct(id);
    }

    public ProductResponse toggleVisibility(String id) {
        // QUAN TRONG: PATCH .../active cua admin-portal CHI doi partnerConfigs[].isActive cho
        // KIOSK, KHONG doi truong "invisible" top-level cua san pham (da xac nhan qua test thuc
        // te truc tiep vao admin-portal dang chay) - neu dung current.getInvisible() de xac dinh
        // trang thai hien tai thi lan toggle thu 2 se KHONG quay lai trang thai cu (luon gui
        // invisible=true). Phai doc tu partnerConfigs (xem ProductResponse#isKioskInvisible).
        ProductResponse current = adminPortalClient.getProduct(id);
        boolean currentlyInvisibleForKiosk = current.isKioskInvisible();
        adminPortalClient.setProductInvisible(id, !currentlyInvisibleForKiosk);
        return adminPortalClient.getProduct(id);
    }
}
