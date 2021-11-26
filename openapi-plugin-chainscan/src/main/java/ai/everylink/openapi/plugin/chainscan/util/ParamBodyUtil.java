package ai.everylink.openapi.plugin.chainscan.util;


import ai.everylink.openapi.plugin.chainscan.common.Constants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author Brett
 * @Description
 * @Date 2021/9/28 20:52
 **/
public class ParamBodyUtil {


    public static String formatStr(String str) {
        if (str != null && str.length() > 0) {
            Pattern p = Pattern.compile(Constants.CHAR_PATTERN);
            Matcher m = p.matcher(str);
            return m.replaceAll("");
        }
        return str;
    }
}
