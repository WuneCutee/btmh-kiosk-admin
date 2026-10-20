package vn.baotinmanhhai.kioskadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.baotinmanhhai.kioskadmin.entity.PriceUpdateLog;
import vn.baotinmanhhai.kioskadmin.entity.TransactionLog;
import vn.baotinmanhhai.kioskadmin.repository.PriceUpdateLogRepository;
import vn.baotinmanhhai.kioskadmin.repository.TransactionLogRepository;

/**
 * Chi doc 2 bang log da co san ben webview (transaction_log, price_update_logs) - phuc vu dieu
 * tra su co tu man quan tri kiosk-admin, khong co CUD.
 */
@Service
@RequiredArgsConstructor
public class LogQueryService {

    private static final int MAX_PAGE_SIZE = 200;

    private final TransactionLogRepository transactionLogRepository;
    private final PriceUpdateLogRepository priceUpdateLogRepository;

    public Page<TransactionLog> listTransactionLogs(int page, int size) {
        return transactionLogRepository.findAllByOrderByTimeDesc(pageRequest(page, size));
    }

    public Page<PriceUpdateLog> listPriceUpdateLogs(int page, int size) {
        return priceUpdateLogRepository.findAllByOrderByCreatedAtDesc(pageRequest(page, size));
    }

    private PageRequest pageRequest(int page, int size) {
        int s = size <= 0 ? 50 : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), s);
    }
}
