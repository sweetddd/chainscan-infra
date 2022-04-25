package ai.everylink.chainscan.watcher.plugin;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * @Author apple
 * @Description
 * @Date 2022/4/25 11:45 下午
 **/

@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling
public class TokenScheduleTask {


    @Scheduled(cron = "0/5 * * * * ?")
    //或直接指定时间间隔，例如：5秒
    //@Scheduled(fixedRate=5000)
    private void configureTasks() {
        TokenWatcher tokenWatcher = new TokenWatcher();
        tokenWatcher.scanBlock();

        System.err.println("执行静态定时任务时间: " + LocalDateTime.now());
    }


}
