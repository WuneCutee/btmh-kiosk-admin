package vn.baotinmanhhai.kioskadmin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kiosk_ad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskAd {

    public static final String STATUS_ON = "ON";
    public static final String STATUS_OFF = "OFF";

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;

    @Column(name = "category_id", length = 50)
    private String categoryId;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "product_id", length = 50)
    private String productId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;
}
