package vn.baotinmanhhai.kioskadmin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;
import vn.baotinmanhhai.kioskadmin.entity.PriceUpdateLog;
import vn.baotinmanhhai.kioskadmin.entity.TransactionLog;
import vn.baotinmanhhai.kioskadmin.service.LogQueryService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class LogController {

    private final LogQueryService logQueryService;

    @GetMapping("/transaction-logs")
    public ApiResponse<Page<TransactionLog>> transactionLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(logQueryService.listTransactionLogs(page, size));
    }

    @GetMapping("/price-update-logs")
    public ApiResponse<Page<PriceUpdateLog>> priceUpdateLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(logQueryService.listPriceUpdateLogs(page, size));
    }
}
