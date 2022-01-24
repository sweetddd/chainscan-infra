'use strict';

const _ = require('lodash');
const Service = require('egg').Service;
const TABLE = 'block';
const dividend_block = 6;
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
  async sumFees(startHeight ,endHeight) {
    const res = await this.database.query(`select  sum(block_fee) from  ${TABLE} where chain_type = 'CPoS' and block_number > ? and block_number < ?`,[startHeight,endHeight]);
    return res;
  }
  async sumCount(startHeight ,endHeight) {
    const res = await this.database.query(`select  sum(tx_size) from  ${TABLE} where chain_type = 'CPoS' and block_number > ? and block_number < ?`,[startHeight,endHeight]);
    return res;
  }


  async addBlock(block,ctx) {

   let exitBlock =  await this.getBlockByHash(block.block_hash);
    if(exitBlock[0]) {
      await this.updateBlock(exitBlock[0],block);
    }else{
      if(block.block_height  >1 && block.block_height % dividend_block == 1){
        let dividend_height = parseInt(block.block_height / dividend_block)
        let start_height = dividend_height*dividend_block - dividend_block+1;
        let end_height = dividend_height*dividend_block;

        let mining_details = start_height + "-" + end_height;
        let earnings = await this.sumFees(start_height,end_height);
        let volume = await ctx.service.transactions.sumVolume(start_height,end_height);
        let transactions = await this.sumCount(start_height,end_height);
        let dividendRecord = {
          "mining_details":mining_details,
          "earnings":earnings[0]['sum(block_fee)'],
          "volume":volume[0]['sum(value)'],
          "transactions":transactions[0]['sum(tx_size)'],
          "mining_earnings":300,
          "time":block.start_time,
          "burnt":0,
        }
        await ctx.service.dividendRecord.addRecord(dividendRecord);


      }
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
