package vn.baotinmanhhai.kioskadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.baotinmanhhai.kioskadmin.dto.AdRequest;
import vn.baotinmanhhai.kioskadmin.entity.KioskAd;
import vn.baotinmanhhai.kioskadmin.exception.BusinessException;
import vn.baotinmanhhai.kioskadmin.repository.KioskAdRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KioskAdService {

    private final KioskAdRepository kioskAdRepository;

    public List<KioskAd> list() {
        return kioskAdRepository.findAll().stream()
                .sorted(Comparator.comparing(KioskAd::getDisplayOrder))
                .toList();
    }

    @Transactional
    public KioskAd create(AdRequest request) {
        KioskAd ad = KioskAd.builder()
                .id(UUID.randomUUID().toString())
                .actionType(request.getActionType())
                .categoryId(request.getCategoryId())
                .mediaType(request.getMediaType())
                .name(request.getName())
                .displayOrder(request.getDisplayOrder())
                .productId(request.getProductId())
                .status(request.getStatus() == null ? KioskAd.STATUS_ON : request.getStatus())
                .url(request.getUrl())
                .websiteUrl(request.getWebsiteUrl())
                .build();
        return kioskAdRepository.save(ad);
    }

    @Transactional
    public KioskAd update(String id, AdRequest request) {
        KioskAd ad = findOrThrow(id);
        ad.setActionType(request.getActionType());
        ad.setCategoryId(request.getCategoryId());
        ad.setMediaType(request.getMediaType());
        ad.setName(request.getName());
        ad.setDisplayOrder(request.getDisplayOrder());
        ad.setProductId(request.getProductId());
        if (request.getStatus() != null) {
            ad.setStatus(request.getStatus());
        }
        ad.setUrl(request.getUrl());
        ad.setWebsiteUrl(request.getWebsiteUrl());
        return kioskAdRepository.save(ad);
    }

    @Transactional
    public void delete(String id) {
        if (!kioskAdRepository.existsById(id)) {
            throw new BusinessException("Khong tim thay quang cao: " + id, "404", 404);
        }
        kioskAdRepository.deleteById(id);
    }

    @Transactional
    public KioskAd toggle(String id) {
        KioskAd ad = findOrThrow(id);
        ad.setStatus(KioskAd.STATUS_ON.equals(ad.getStatus()) ? KioskAd.STATUS_OFF : KioskAd.STATUS_ON);
        return kioskAdRepository.save(ad);
    }

    private KioskAd findOrThrow(String id) {
        return kioskAdRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Khong tim thay quang cao: " + id, "404", 404));
    }
}
