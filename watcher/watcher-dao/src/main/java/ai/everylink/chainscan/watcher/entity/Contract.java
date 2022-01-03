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
import java.util.Date;

/**
 * 合约信息
 *
 * @author david.zhang@everylink.ai
 * @since 2021-11-30
 */
@Data
@Entity
@Table(name="contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_addr")
    private String contract_addr;

    @Column(name = "contract_name")
    private Integer contractName;

    @Column(name = "src_code")
    private String srcCode;

    @Column(name = "abi")
    private String abi;

    @Column(name = "verified")
    private Integer verified;

    @Column(name = "contract_version")
    private String contractVersion;

    @Column(name = "contract_compiler")
    private String contractCompiler;

    @Column(name = "license")
    private String license;

    @Column(name = "extra_data")
    private String extraData;

    @Column(name = "create_time")
    private Date createTime;

}
