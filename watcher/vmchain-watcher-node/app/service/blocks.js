'use strict';

const _ = require('lodash');
const Service = require('egg').Service;
const TABLE = 'block';
const { env } = require('process');

class BlockService extends Service {
  constructor(ctx) {
    super(ctx);
    this.database = this.app.mysql;
  }


  /**
   * 获取最大区块号
   * @returns
   */
  async getMaxBlockNumber() {
    const res = await this.app.mysql.query(`select max(block_number) from ${TABLE} where chain_type = 'CPoS'`);
    return res[0]['max(block_number)'];
  }
  /**
   * 获取未扫描区块hash
   * @returns {Promise<*>}
   */
  async getSubScanBlockHash() {
    const res = await this.database.query(`select  block_number,block_hash,block_timestamp from  ${TABLE} where sca_satate = 0`);
    return res;
  }

  /**
     * 更新区块subscan获取明细数据状态
     * @param block_number 区块号
     * @return
     */
  async updateSubScanBlockState(block_number) {
    console.log(block_number);
    //修改数据
    var updateSql = 'UPDATE block SET sca_satate = 1 WHERE block_number = ?';
    var updateqSlParams = [block_number];
    this.database.query(updateSql,updateqSlParams,function (err,result) {
      if(err){
        console.log('[UPDATE SUBSCABBLOCK ERROR] - ',err.message);
        return;
      }
    });
  }

  async getBlockByHash(hash) {
    const res = await this.database.query(`select  * from  ${TABLE} where chain_type = 'CPoS' and block_hash = ?`,[hash]);
    return res;
  }


  async addBlock(block) {

   let exitBlock =  await this.getBlockByHash(block.block_hash);
    if(exitBlock[0]) {
      await this.updateBlock(exitBlock[0],block);
    }else{
      const addTxSql = 'INSERT INTO block (' +
          'block_number, block_hash,  block_timestamp,  tx_size,  difficulty,  create_time, block_fee, chain_type' +
          ') VALUES(?,?,?,?,?,?,?,?)';
      const addTxSql_Params = [block.block_height,block.block_hash,block.start_time,block.transaction_count,block.difficulty,new Date(),block.blocked_fee,'CPoS'];
      await this.app.mysql.query(addTxSql, addTxSql_Params, function(err, result) {
        if (err) {
          console.log('[INSERT SUBSCABTX ERROR] - ', err.message);
          return;
        }
        console.log('INSERT ID:', result);
        console.log('#######################');

      });
    }

  }

  async updateBlock(oldBlock,newBlock) {
    //修改数据
    var updateSql = 'UPDATE block SET tx_size = ?,block_fee = ?  WHERE id = ? ';
    var updateqSlParams = [newBlock.transaction_count,newBlock.blocked_fee,oldBlock.id];
    this.database.query(updateSql,updateqSlParams,function (err,result) {
      if(err){
        console.log('[UPDATE SUBSCABBLOCK ERROR] - ',err.message);
        return;
      }
    });
  }


}

module.exports = BlockService;
