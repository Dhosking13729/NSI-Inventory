package com.nauticalstructures.inventory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Data dictionary table: ReorderRequest - created from an alert, or manually by purchasing staff. */
@Entity
@Table(name = "reorder_request")
public class ReorderRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    /** Optional: empty for manual requests. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private LowStockAlert alert;

    @Column(name = "requested_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedQuantity;

    @Column(name = "request_status", nullable = false, length = 20)
    private RequestStatus requestStatus = RequestStatus.PENDING;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    /** Optional until reviewed: the purchasing staff member who last updated the status. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_staff_id")
    private StaffUser reviewedBy;

    protected ReorderRequest() { }

    public ReorderRequest(Material material, LowStockAlert alert, BigDecimal requestedQuantity) {
        this.material = material;
        this.alert = alert;
        this.requestedQuantity = requestedQuantity;
        this.createdDate = Instant.now();
    }

    public Integer getRequestId() { return requestId; }
    public Material getMaterial() { return material; }
    public LowStockAlert getAlert() { return alert; }
    public BigDecimal getRequestedQuantity() { return requestedQuantity; }
    public RequestStatus getRequestStatus() { return requestStatus; }
    public Instant getCreatedDate() { return createdDate; }
    public StaffUser getReviewedBy() { return reviewedBy; }

    public void review(RequestStatus next, StaffUser by) {
        this.requestStatus = next;
        this.reviewedBy = by;
    }
}
