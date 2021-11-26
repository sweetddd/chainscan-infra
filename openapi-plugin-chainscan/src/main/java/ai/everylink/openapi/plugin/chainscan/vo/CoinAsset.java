package ai.everylink.openapi.plugin.chainscan.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoinAsset implements Serializable {

    private static final long serialVersionUID = -3783316657677071171L;

    private String name;
    private String symbol;
    @JsonProperty("total_amount")
    private String totalAmount;

    @JsonProperty("available_amount")
    private String availableAmount;


    @JsonProperty("locked_amount")
    private String lockedAmount ;

    @JsonProperty("currency_amount")
    private String currencyAmount ;

    @JsonProperty("icon_url")
    private String iconUrl;

    @JsonProperty("is_dividend_coin")
    private Boolean isDividendCoin;

    @JsonProperty("asset_type")
    private String assetType = "";
}
