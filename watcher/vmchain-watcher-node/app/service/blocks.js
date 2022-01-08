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
}

module.exports = BlockService;
