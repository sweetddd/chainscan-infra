'use strict';
const ethers = require("ethers");

const _ = require('lodash');
const Service = require('egg').Service;
const TABLE = 'user_reward';
const { env } = require('process');

class UserRewardService extends Service {
  constructor(ctx) {
    super(ctx);
    this.database = this.app.mysql;
  }

  /**
   * 增加transaction详情
   * @return
   */
  async addUserReward(rewards) {
    console.log(rewards)
    const addTxSql = 'INSERT INTO user_reward (' +
        'address,era,reward,transaction_volume,withdraw,time' +
        ') VALUES(?,?,?,?,?,?)';
    const reward = ethers.utils.formatUnits(
        rewards.reward.toString(),
         18
    )
    const transaction_volume = ethers.utils.formatUnits(
        rewards.transaction_volume.toString(),
         6
    )
    const addTxSql_Params = [rewards.miner,rewards.era,reward,transaction_volume,rewards.withdraw,new Date()];
    await this.app.mysql.query(addTxSql, addTxSql_Params, function(err, result) {
      if (err) {
        console.log('[INSERT SUBSCABTX ERROR] - ', err.message);
        return;
      }
      console.log('-------INSERT SUBSCABTX----------');
      console.log('INSERT ID:', result);
      console.log('#######################');
    });
  }

  async getMaxEra() {
    const res = await this.app.mysql.query(`select max(era) from user_reward`);
    return res[0]['max(era)'];
  }


}

module.exports = UserRewardService;
