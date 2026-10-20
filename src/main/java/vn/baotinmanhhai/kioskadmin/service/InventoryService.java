package vn.baotinmanhhai.kioskadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.baotinmanhhai.kioskadmin.dto.InventoryHistoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.InventoryResponse;
import vn.baotinmanhhai.kioskadmin.dto.PageResponse;

/**
 * Ton kho - proxy sang admin-portal (GET/PUT /api/v1/inventories, GET .../history). So luong
 * con lai THUC nam trong Redis (key INVENTORY_QTY:{partnerCode}:{productCode}) ma admin-portal
 * tu dong bo khi PUT - kiosk-admin KHONG doc/ghi Redis truc tiep, chi hien lai gia tri da duoc
 * admin-portal tong hop trong InventoryResponse.remainingQuantity/availableQuantity.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final int DEFAULT_SIZE = 100;

    private final AdminPortalClient adminPortalClient;

    public PageResponse<InventoryResponse> list(String productName, int page, int size) {
        return adminPortalClient.listInventory(productName, page, size <= 0 ? DEFAULT_SIZE : size);
    }

    public void update(String inventoryId, int quantity) {
        adminPortalClient.updateInventory(inventoryId, quantity);
    }

    public PageResponse<InventoryHistoryResponse> history(String inventoryId, int page, int size) {
        return adminPortalClient.inventoryHistory(inventoryId, page, size <= 0 ? DEFAULT_SIZE : size);
    }
}
