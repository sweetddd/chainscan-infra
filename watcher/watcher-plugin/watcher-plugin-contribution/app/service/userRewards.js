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
        decimals || 18
    )
    const addTxSql_Params = [rewards.miner,rewards.era,reward,rewards.transaction_volume,rewards.withdraw,new Date()];
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


}

module.exports = UserRewardService;
