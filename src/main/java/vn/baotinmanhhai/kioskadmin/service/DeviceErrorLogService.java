package vn.baotinmanhhai.kioskadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.baotinmanhhai.kioskadmin.entity.KioskDeviceErrorLog;
import vn.baotinmanhhai.kioskadmin.repository.KioskDeviceErrorLogRepository;

/**
 * Chi doc - bang kiosk_device_error_log duoc webview ghi vao, kiosk-admin chi hien thi.
 */
@Service
@RequiredArgsConstructor
public class DeviceErrorLogService {

    private static final int MAX_PAGE_SIZE = 200;

    private final KioskDeviceErrorLogRepository kioskDeviceErrorLogRepository;

    public Page<KioskDeviceErrorLog> list(String deviceCode, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), cappedSize(size));
        if (deviceCode != null && !deviceCode.isBlank()) {
            return kioskDeviceErrorLogRepository.findByDeviceCodeOrderByReceivedAtDesc(deviceCode, pageRequest);
        }
        return kioskDeviceErrorLogRepository.findAllByOrderByReceivedAtDesc(pageRequest);
    }

    private int cappedSize(int size) {
        int s = size <= 0 ? 50 : size;
        return Math.min(s, MAX_PAGE_SIZE);
    }
}
