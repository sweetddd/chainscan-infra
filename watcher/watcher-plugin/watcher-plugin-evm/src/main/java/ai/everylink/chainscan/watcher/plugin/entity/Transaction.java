/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.everylink.chainscan.watcher.plugin.entity;


import com.fasterxml.jackson.databind.util.JSONPObject;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * 交易
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
@Data
@Entity
@Table(name="transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_hash")
    private String transactionHash;

    @Column(name = "transaction_index")
    private Integer transactionIndex;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "chain_id")
    private Integer chainId;

    @Column(name = "status")
    private String status;

    @Column(name = "fail_msg")
    private String failMsg;

    @Column(name = "tx_timestamp")
    private Date txTimestamp;

    @Column(name = "from_addr")
    private String fromAddr;

    @Column(name = "to_addr")
    private String toAddr;

    @Column(name = "value")
    private String value;

    @Column(name = "tx_fee")
    private Integer txFee;

    @Column(name = "gas_used")
    private Integer gasUsed;

    @Column(name = "gas_limit")
    private Integer gasLimit;

    @Column(name = "gas_price")
    private String gasPrice;

    @Column(name = "nonce")
    private String nonce;

    @Column(name = "input")
    private String input;

    @Column(name = "input_method")
    private String inputMethod;

    @Column(name = "input_params")
    private String inputParams;

    @Column(name = "tx_type")
    private Integer txType;

    @Column(name = "create_time")
    private Date createTime;

}