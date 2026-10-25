package com.emsi.billing;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByRequestKey(String requestKey);
    @Query("select b from Bill b where (:customerId is null or b.customerId = :customerId) and (:status is null or b.status = :status)")
    Page<Bill> search(@Param("customerId") Long customerId, @Param("status") BillStatus status, Pageable page);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> lockById(@Param("id") Long id);
}
