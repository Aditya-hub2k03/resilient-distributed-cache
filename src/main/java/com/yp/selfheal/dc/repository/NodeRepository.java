package com.yp.selfheal.dc.repository;

import com.yp.selfheal.dc.model.NodeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NodeRepository extends JpaRepository<NodeRecord, Long> {
    Optional<NodeRecord> findByNodeId(String nodeId);
}
