package ai.everylink.chainscan.watcher.plugin.dao;


import ai.everylink.chainscan.watcher.plugin.entity.DividendRecord;
//import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 挖矿详情数据记录Mapper接口
 *
 * @author brett
 * @date 2021-10-15
 */
public interface DividendRecordDao extends JpaRepository<DividendRecord, Long> {

}
