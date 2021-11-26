package ai.everylink.openapi.plugin.chainscan.constant;

import ai.everylink.openapi.plugin.chainscan.code.ErrorCode;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author by watson
 * @Description the transaciont type
 * @Date 2020/11/6 22:06
 */
public enum TransactionTypeEnum {
    DEPOSIT("deposit", "Deposit"),
    WITHDRAW("withdraw", "Withdraw"),
    TRANSFER("transfer", "Transfer"),
    FUNDRAISING("fundraising", "fundraising"),
    // PAYMENT("payment", "Payment"),


    All("all", "All");
    private String type;
    private String tab;


    TransactionTypeEnum(String type, String tab) {
        this.type = type;
        this.tab = tab;
    }

    public String getType() {
        return type;
    }

    public String getTab() {
        return tab;
    }

    public Integer getQueryValue() {
        if (!this.equals(All)) {
            return this.ordinal();
        }
        return null;
    }

    public static List<TransactionTypeEnum> getQueryEnumList() {
        List<TransactionTypeEnum> valueList = new ArrayList<>();
        valueList.add(DEPOSIT);
        valueList.add(WITHDRAW);
        valueList.add(TRANSFER);
        return valueList;
    }

    public List<Integer> getQueryValueList(boolean isVip) {
        List<Integer> valueList = new ArrayList<>();
        if (this.equals(All)) {
            if (isVip) {
                valueList.add(DEPOSIT.ordinal());
                valueList.add(WITHDRAW.ordinal());
                valueList.add(TRANSFER.ordinal());
            } else {
                valueList.add(DEPOSIT.ordinal());
                valueList.add(WITHDRAW.ordinal());
            }
        } else {
            valueList.add(this.ordinal());
        }
        return valueList;
    }

    public static List<Integer> getAllQuery() {
        List<Integer>         valueList = new ArrayList<>();
        TransactionTypeEnum[] values    = TransactionTypeEnum.values();
        for (int i = 0; i < values.length; i++) {
            TransactionTypeEnum item = values[i];
            if (!item.equals(All)) {
                valueList.add(item.ordinal());
            }
        }
        return valueList;
    }


    public static List<String> getTabList(boolean isVip) {
        List<String> tabList = new ArrayList<>();
        if (isVip) {
            tabList.add(TransactionTypeEnum.All.getTab());
            tabList.add(TransactionTypeEnum.DEPOSIT.getTab());
            tabList.add(TransactionTypeEnum.WITHDRAW.getTab());
            tabList.add(TransactionTypeEnum.TRANSFER.getTab());
        } else {
            tabList.add(TransactionTypeEnum.All.getTab());
            tabList.add(TransactionTypeEnum.DEPOSIT.getTab());
            tabList.add(TransactionTypeEnum.WITHDRAW.getTab());
        }
        return tabList;
    }

    public static TransactionTypeEnum valueOfType(String type) {
        String value = Optional.ofNullable(type)
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.TRANSACTION_TYPE_NOT_SPECIFIED));
        return Arrays.stream(TransactionTypeEnum.values())
                .filter((t) -> t.getTab().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.TRANSACTION_TYPE_NOT_SPECIFIED));
    }

    public static String valueOfStatus(Integer value) {
        return Arrays.stream(TransactionTypeEnum.values())
                .filter((t) -> t.ordinal() == (value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(ErrorCode.DATA_ERROR))
                .getType();
    }

    @JsonValue
    public int getOrdinal() {
        return this.ordinal();
    }
}
