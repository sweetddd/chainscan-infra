package ai.everylink.chainscan.watcher.plugin.constant;


import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Getter

public enum LendingTopicEnum {

    Supply("0x4c209b5fc8ad50758f13e2e1088ba56a560dff690a1c6fef26394f4c03821c4f"),
    Withdraw("0xe5b754fb1abb7f01b499791d0b820ae3b6af3424ac1c59768edb53f4ec31a929"),
    Borrow("0x13ed6866d4e1ee6da46f845c46d7e54120883d75c5ea9a2dacc1c4ca8984ab80"),
    Mint("0x13ed6866d4e1ee6da46f845c46d7e54120883d75c5ea9a2dacc1c4ca8984ab80"),
    Burnt("0x1a2a22cb034d26d1854bdc6666a5b91fe25efbbb5dcad3b0355478d6f5c362a1"),
    Repay("0x1a2a22cb034d26d1854bdc6666a5b91fe25efbbb5dcad3b0355478d6f5c362a1");

    private final String value;

    LendingTopicEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static List<Map<String, String>> getTypeList() {
        return Arrays.stream(LendingTopicEnum.values())
                .map(e -> {
                    HashMap<String, String> map = Maps.newHashMap();
                    map.put("label", e.name());
                    map.put("value", e.getValue());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public static LendingTopicEnum fromName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        Optional<LendingTopicEnum> statusEnum = Arrays.stream(LendingTopicEnum.values())
                .filter(e -> e.name().equalsIgnoreCase(name)).findFirst();
        return statusEnum.orElse(null);
    }
}
