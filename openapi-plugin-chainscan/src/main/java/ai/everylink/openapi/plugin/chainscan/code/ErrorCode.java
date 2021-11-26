package ai.everylink.openapi.plugin.chainscan.code;


/**
 * @author by watson
 * @Description the err code constant
 * @Date 2020/11/7 10:08
 */
public class ErrorCode {

    /**
     * 1	Parameter error
     * 2	Business logic error
     * 3	Application system error
     * 4	Database error
     * 5	Cache error
     * 9	Unknown error
     */


    public static final String COIN_NOT_EXIST                   = "1-001";
    public static final String TRANSACTION_TYPE_NOT_SPECIFIED   = "1-002";
    public static final String TRANSACTION_STATUS_NOT_SPECIFIED = "1-003";
    public static final String DATE_NOT_SPECIFIED               = "1-004";
    public static final String PAGE_NO_NOT_EXIST                = "1-005";
    public static final String PAGE_SIZE_NOT_EXIST              = "1-006";
    public static final String ID_CAN_NOT_BE_NULL               = "1-007";
    public static final String COIN_CANNOT_BE_NULL              = "1-008";
    public static final String ADDRESS_CANNOT_BE_EMPTY          = "1-009";
    public static final String ADDRESS_NAME_CANNOT_BE_EMPTY     = "1-010";
    public static final String NETWORK_ID_CANNOT_BE_NULL        = "1-011";
    public static final String WITHDRAW_AMOUNT_CANNOT_BE_EMPTY  = "1-012";
    public static final String EXCHANGE_AMOUNT_CANNOT_BE_NULL   = "1-013";
    public static final String MEMO_CANNOT_BE_EMPTY             = "1-014";

    public static final String COIN_NETWORK_NOT_EXIST                               = "2-001";
    public static final String DATA_ERROR                                           = "2-002";
    public static final String NO_DATA_FOR_CREATE_EXCEL                             = "2-003";
    public static final String INVALID_ADDRESS                                      = "2-004";
    public static final String ETHEREUM_WALLET_NOT_EXIST                            = "2-005";
    public static final String TRANSACTION_FEE_INSUFFICIENT                         = "2-006";
    public static final String MEMBER_WALLET_NOT_EXIST                              = "2-007";
    public static final String WITHDRAW_AMOUNT_CANNOT_LESS_THAN_MIN_WITHDRAW_AMOUNT = "2-008";
    public static final String AVAILABLE_AMOUNT_INSUFFICIENT                        = "2-009";
    public static final String WITHDRAW_FAILED                                      = "2-010";
    public static final String MEMBER_TRANSACTION_IS_NULL                           = "2-011";
    public static final String MEMBER_TRANSACTION_STATUS_ERROR                      = "2-012";
    public static final String ADDRESS_ALREADY_EXISTS                               = "2-013";
    public static final String CANNOT_WITHDRAWAL                                    = "2-014";
    public static final String THE_COIN_CANNOT_WITHDRAWAL                           = "2-015";

    public static final String AUTHENTICATION_FAILED = "2-023";

    public static final String UNKNOWN_ERROR = "9-001";
}
