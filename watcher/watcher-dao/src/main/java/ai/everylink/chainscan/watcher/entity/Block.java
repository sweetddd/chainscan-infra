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

package ai.everylink.chainscan.watcher.entity;


import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * 区块
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
@Data
@Entity
@Table(name="block")
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "chain_id")
    private Integer chainId;

    @Column(name = "block_timestamp")
    private Date blockTimestamp;

    @Column(name = "parent_hash")
    private String parentHash;

    @Column(name = "nonce")
    private String nonce;

    @Column(name = "validator")
    private String validator;

    @Column(name = "burnt")
    private String burnt;

    @Column(name = "tx_size")
    private Integer txSize;

    @Column(name = "reward")
    private String reward;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "total_difficulty")
    private String totalDifficulty;

    @Column(name = "block_size")
    private Integer blockSize;

    @Column(name = "gas_used")
    private BigInteger gasUsed;

    @Column(name = "gas_limit")
    private BigInteger gasLimit;

    @Column(name = "extra_data")
    private String extraData;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "block_fee")
    private BigDecimal blockFee;

    @Column(name = "chain_type")
    private String chainType;

    @Column(name = "params")
    private String params;

    @Column(name = "finalized")
    private int finalized;

    @Column(name = "status")
    private Integer status;
}
