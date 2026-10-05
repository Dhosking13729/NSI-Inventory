package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.Material;
import com.nauticalstructures.inventory.domain.ReorderRequest;
import com.nauticalstructures.inventory.domain.RequestStatus;
import com.nauticalstructures.inventory.domain.StaffUser;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import com.nauticalstructures.inventory.repository.ReorderRequestRepository;
import com.nauticalstructures.inventory.repository.StaffUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

/** View Reorder Requests and Update Request Status use cases (Purchasing). */
@Service
@Transactional
public class ReorderService {

    private final ReorderRequestRepository reorders;
    private final MaterialRepository materials;
    private final StaffUserRepository staff;

    public ReorderService(ReorderRequestRepository reorders, MaterialRepository materials, StaffUserRepository staff) {
        this.reorders = reorders;
        this.materials = materials;
        this.staff = staff;
    }

    @Transactional(readOnly = true)
    public List<ReorderRequest> open() {
        return reorders.findByRequestStatusInOrderByCreatedDateDescRequestIdDesc(EnumSet.of(RequestStatus.PENDING, RequestStatus.ORDERED));
    }

    @Transactional(readOnly = true)
    public List<ReorderRequest> recentlyClosed() {
        return reorders.findTop20ByRequestStatusOrderByCreatedDateDescRequestIdDesc(RequestStatus.CLOSED);
    }

    /** Manual request (no AlertID), e.g. for a planned job. */
    public ReorderRequest createManual(Integer materialId, BigDecimal quantity) {
        Material m = materials.findById(materialId == null ? -1 : materialId)
                .orElseThrow(() -> new NotFoundException("Choose a material"));
        return reorders.save(new ReorderRequest(m, null, Quantities.positive(quantity, "RequestedQuantity")));
    }

    public ReorderRequest updateStatus(Integer requestId, RequestStatus next, Integer staffId) {
        ReorderRequest r = reorders.findById(requestId).orElseThrow(() -> new NotFoundException("No reorder request #" + requestId));
        if (next == null || !r.getRequestStatus().next().contains(next)) {
            throw new BusinessRuleException("Reorder request #" + requestId + " is " + r.getRequestStatus().label()
                    + " and can't be changed to " + (next == null ? "that status" : next.label()));
        }
        StaffUser by = staff.findById(staffId).orElseThrow(() -> new NotFoundException("No staff user #" + staffId));
        r.review(next, by);
        return r;
    }
}
