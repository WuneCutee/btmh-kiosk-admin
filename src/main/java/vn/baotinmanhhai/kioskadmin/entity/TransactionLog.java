package vn.baotinmanhhai.kioskadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Shadow mapping CHI DOC cua bang "transaction_log" (da co san trong DB webview) - chi de xem
 * lai lich su goi API phuc vu dieu tra su co, khong co CUD.
 */
@Entity
@Table(name = "transaction_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "transaction_id", length = 50)
    private String transactionId;

    @Column(name = "time")
    private LocalDateTime time;

    @Column(name = "payload")
    private String payload;

    @Column(name = "output")
    private String output;

    @Column(name = "path", length = 255)
    private String path;

    @Column(name = "method", length = 255)
    private String method;

    @Column(name = "duration")
    private Integer duration;
}
