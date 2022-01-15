'use strict';

const { Controller } = require('egg');
const { useSuccess } = require('../../utils/index.js');

const fs = require('fs');
const context = fs.readFileSync('./config/types.json');
const typesData = JSON.parse(context);

// substrate
const http = require('@polkadot/api');
const Keyring = http.Keyring;

// const wsProvider = new WsProvider(process.env.CHAIN_WS_ENDPOINT);

class subscanJob extends Controller {
  async index() {

  }



}

module.exports = subscanJob;
