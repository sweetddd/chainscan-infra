package ai.everylink.chainscan.watcher.plugin;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

/**
 * @Author apple
 * @Description
 * @Date 2022/4/25 11:45 下午
 **/


@Component
@EnableScheduling   // 1.开启定时任务
@EnableAsync
public class TokenScheduleTask {


//    @Async
//    @Scheduled(fixedRate = 5000)
//    public void tokenWatcher() {
//        TokenWatcher tokenWatcher = new TokenWatcher();
//        tokenWatcher.scanBlock();
//    }
//
//    @Async
//    @Scheduled(fixedRate = 5000)
//    public void tokenWatcher2() {
//        TokenWatcher tokenWatcher = new TokenWatcher();
//        tokenWatcher.scanBlock();
//    }
//
//    @Async
//    @Scheduled(fixedRate = 5000)
//    public void tokenWatcher3() {
//        TokenWatcher tokenWatcher = new TokenWatcher();
//        tokenWatcher.scanBlock();
//    }
//
//    @Async
//    @Scheduled(fixedRate = 5000)
//    public void tokenWatcher4() {
//        TokenWatcher tokenWatcher = new TokenWatcher();
//        tokenWatcher.scanBlock();
//    }
//
//    @Async
//    @Scheduled(fixedRate = 5000)
//    public void tokenWatcher5() {
//        TokenWatcher tokenWatcher = new TokenWatcher();
//        tokenWatcher.scanBlock();
//    }


}
