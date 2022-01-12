'use strict';

const fs = require('fs');
const http = require('@polkadot/api');
const web3 = require('web3');
const ethers = require('ethers');
const zksync = require('../zksync/build');
const bridgerCall = require('./../bridge/bridge-utils');
const GlobalConstants = require('./../bridge/constants');

const WsProvider = http.WsProvider;
const ApiPromise = http.ApiPromise;

const wsProvider = new WsProvider(process.env.CHAIN_WS_ENDPOINT);
let context = fs.readFileSync('./config/types.json');
// const wsProvider = new WsProvider('ws://10.233.65.230:9900');
// const context = fs.readFileSync('E:\\project\\IdeaProject\\chainscan-infra\\watcher\\vmchain-watcher-node\\config\\types.json');

const typesData = JSON.parse(context);

const L2Address = {
  rinkeby: 'http://xapi.powx.io/l2-server-jsrpc/jsrpc',
  vm: 'http://xapi.powx.io/l2-server3-jsrpc/jsrpc',
};
const token = 'USDT';
const type = 'Withdraw';
// 手续费账号
const ethPrivateKey = '0xe71c0d817dc5b037f1fcd36b374ffb4e3894026a028e3c26e946943c4362036a';


async function main() {
  console.log(`CHAIN_WS_ENDPOINT: ${process.env.CHAIN_WS_ENDPOINT}`);
  console.log(`Fee account address: ${ethPrivateKey}`);

  // Create our API with a default connection to the local node
  const api = await ApiPromise.create({
    provider: wsProvider,
    types: typesData,
  });
    // Subscribe to system events via storage
  await api.query.system.events(async events => {
    // Loop through the Vec<EventRecord>
    await events.forEach(async record => {
      // Extract the event
      const event = record.event;
      if (event.section.toString() === 'cposContribution' && event.method.toString() === 'Dividend') {
        const block_height = event.data[0].toString();
        const reverse = parseInt(event.data[1].toString());
        const timestamp = event.data[2].toString();
        console.log(`Received dividend events, block height: ${block_height}, reverse: ${reverse}, timestamp: ${timestamp}`);
        // await rinkebyWithdraw("rinkeby", reverse)
        // await vmWithdraw("vm", reverse)
        Object.keys(L2Address).map(async name => {
          if (name.trim() === 'rinkeby') {
            await rinkebyWithdraw(name, reverse);
          } else {
            await vmWithdraw(name, reverse);
          }
        });
      }
    });
  });
}

const rinkebyWithdraw = async function(name, reverse) {
  try {
    // let ethPrivateKey = "0xa53578fe8f9a1678be99f58dbe3e189743f5cb2149ba77d004c6819d0dd25104";
    const web3Wallet = await new web3('http://rinkeby.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf');
    const provider = await new ethers.providers.Web3Provider(web3Wallet.eth.currentProvider);
    const syncProvider = await zksync.Provider.newHttpProvider(L2Address[name], 1000);
    const ethWallet = await new ethers.Wallet(ethPrivateKey, provider);
    const zkWallet = await zksync.Wallet.fromEthSigner(ethWallet, syncProvider);
    // 查看 zkSync 账户余额
    const state = await zkWallet.getAccountState();
    // console.log(state)
    const committedBalances = state.committed.balances;
    const balance = parseInt(committedBalances.USDT);
    let volume = 0;
    if (reverse < balance) {
      volume = reverse;
    } else {
      // 10个点的手续费
      volume = parseInt(balance * 0.9);
    }
    console.log(`L2 name: ${name}, reverse：${reverse}, USDT balance: ${balance}, withdraw to L1 volume: ${volume}`);
    const fee = await zkWallet.provider.getTransactionFee(type, zkWallet.address(), token);
    const withdrawTransaction = await zkWallet.withdrawFromSyncToEthereum({
      ethAddress: zkWallet.address(),
      token,
      amount: volume,
      fee: fee.totalFee,
    });
    await withdrawTransaction.awaitVerifyReceipt();
    await bridge(volume);
  } catch (err) {
    console.log(`Rinkeby withdraw exception: -> ${err.message}`);
  }
};

const vmWithdraw = async function(name, reverse) {
  try {
    // let ethPrivateKey = "0xa53578fe8f9a1678be99f58dbe3e189743f5cb2149ba77d004c6819d0dd25104";
    const web3Wallet = new web3('http://vmtest.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf');
    const provider = new ethers.providers.Web3Provider(web3Wallet.eth.currentProvider);
    const syncProvider = await zksync.Provider.newHttpProvider(L2Address[name], 1000);
    const ethWallet = new ethers.Wallet(ethPrivateKey, provider);
    const zkWallet = await zksync.Wallet.fromEthSigner(ethWallet, syncProvider);
    // 查看 zkSync 账户余额
    const state = await zkWallet.getAccountState();
    // console.log(state)
    const committedBalances = state.committed.balances;
    const balance = committedBalances.USDT;
    let volume = 0;
    if (reverse < balance) {
      volume = reverse;
    } else {
      // 10个点的手续费
      volume = parseInt(balance * 0.9);
    }
    console.log(`L2 name: ${name}, reverse：${reverse}, USDT balance: ${balance}, withdraw to L1 volume: ${volume}`);

    const fee = await zkWallet.provider.getTransactionFee(type, zkWallet.address(), token);
    const withdrawTransaction = await zkWallet.withdrawFromSyncToEthereum({
      ethAddress: zkWallet.address(),
      token,
      amount: volume,
      fee: fee.totalFee,
    });
    await withdrawTransaction.awaitVerifyReceipt();
  } catch (err) {
    console.log(`VM withdraw exception: -> ${err.message}`);
  }
};

const bridge = async function(amount) {
  // 一层rinkeby
  const decimals = 6;
  const wallet = new web3('http://rinkeby.infra.powx.io/v1/72f3a83ea86b41b191264bd16cbac2bf');
  const provider = new ethers.providers.Web3Provider(
    wallet.eth.currentProvider
  );

  const ethWallet = new ethers.Wallet(ethPrivateKey, provider);
  const tx = await bridgerCall.Call.erc20Aprrove(GlobalConstants.GlobalConstants.Contracts[token].from, GlobalConstants.GlobalConstants.ETH_ERC20_HANDLER_ADDRESS, '150000', '6', ethWallet);
  console.log(`${tx} 完成`);
  const txs = await bridgerCall.Call.erc20_despoit(
    GlobalConstants.GlobalConstants.ETH_BRIDGE_ADDRESS,
    token,
    amount,
    GlobalConstants.GlobalConstants.VMCHAIN_ID,
    GlobalConstants.GlobalConstants.Contracts[token].resourceId,
    bridgerCall.Call.getCallData(amount, decimals, ethWallet.address),
    ethWallet,
    decimals);
  console.log(`${txs} 完成`);
};

main().catch(error => {
  console.error(error);
  process.exit(-1);
});

