package com.emsi.bank;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationRepository extends JpaRepository<AccountOperation, Long> {
    Optional<AccountOperation> findByAccountIdAndRequestKey(UUID accountId, String requestKey);
    Page<AccountOperation> findByAccountId(UUID accountId, Pageable pageable);
}
