package ai.everylink.chainscan.watcher.core.util.httpUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Brett
 * @Description
 * @Date 2021/9/28 23:02
 **/
public class HttpHeader {
    private Map<String, String> params = new HashMap<String, String>();

    public HttpHeader addParam(String name, String value) {
        this.params.put(name, value);
        return this;
    }

    public Map<String, String> getParams() {
        return this.params;
    }
}
