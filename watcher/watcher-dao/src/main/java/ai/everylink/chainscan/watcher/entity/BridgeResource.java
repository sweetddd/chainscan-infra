package ai.everylink.chainscan.watcher.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "wallet_bridge")
@AllArgsConstructor
@NoArgsConstructor
public class BridgeResource implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "token")
    private String token;

    @Column(name = "token_type")
    private String tokenType;

    @Column(name = "chain_id")
    private Long chainId;

    @Column(name = "network")
    private String network;

    @Column(name = "contact_address")
    private String contactAddress;

    @Column(name = "handler_address")
    private String handlerAddress;

    @Column(name = "coin_contact_address")
    private String coinContactAddress;

    @Column(name = "refer_coin")
    private String referCoin;

    @Column(name = "refer_network")
    private String referNetwork;

    @Column(name = "refer_contact_address")
    private String referContactAddress;

    @Column(name = "refer_handler_address")
    private String referHandlerAddress;

    @Column(name = "refer_coin_contact_address")
    private String referCoinContactAddress;

    @Column(name = "destination_chain_id")
    private Long destinationChainId;

    @Column(name = "bridge_fee")
    private BigDecimal bridgeFee;

    @Column(name = "refer_bridge_fee")
    private BigDecimal referBridgeFee;

    @Column(name = "destination_network")
    private String destinationNetwork;

    private Integer sort;

    @Column(name = "native")
    private Integer tokenNative;

    @Column(name = "refer_native")
    private Integer referTokenNative;
}
