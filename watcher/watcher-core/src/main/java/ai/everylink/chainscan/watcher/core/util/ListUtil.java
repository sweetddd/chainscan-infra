package ai.everylink.chainscan.watcher.core.util;

import com.google.common.collect.Lists;

import java.util.List;

public class ListUtil {

    @SafeVarargs
    public static List<String> merge(List<String> ...lists){
        List<String> mergeList = Lists.newArrayList();
        for (List<String> list : lists) {
            mergeList.addAll(list);
        }
        return mergeList;
    }

}
