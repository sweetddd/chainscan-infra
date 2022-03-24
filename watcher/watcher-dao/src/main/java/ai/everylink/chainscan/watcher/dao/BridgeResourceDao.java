package ai.everylink.chainscan.watcher.dao;

import ai.everylink.chainscan.watcher.entity.BridgeResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BridgeResourceDao extends JpaRepository<BridgeResource, String> {


    @Query(value = "select * from wallet_bridge where resource_id = :resourceID order by sort", nativeQuery = true)
    List<BridgeResource> findByResourceID(String resourceID);
}
