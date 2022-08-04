package ai.everylink.chainscan.watcher.dao;

import ai.everylink.chainscan.watcher.entity.PluginProcessing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginProcessingDao extends JpaRepository<PluginProcessing, Integer> {

}
