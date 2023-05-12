package ai.everylink.chainscan.watcher.core.util;

import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorsUtil {

    public static final ExecutorService executor = TtlExecutors.getTtlExecutorService(Executors.newFixedThreadPool(30));

}
