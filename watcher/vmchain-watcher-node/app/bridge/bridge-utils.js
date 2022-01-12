let Web3 = require('web3');
let ethers = require('ethers');
var Tx = require('ethereumjs-tx');
let contractCache = {}
let GlobalConstants = require('./../bridge/constants');
const {add} = require("lodash/math");
let walletObj = undefined


const toUsdt = (bn) => {
    return Web3.utils.fromWei(bn, "Mwei")
}
const toEther = (bn) => {
    return Web3.utils.fromWei(bn, "ether")
}
const expandDecimals = (amount, decimals = 18) => {
    return ethers.utils.parseUnits(String(amount), decimals);
}

const createWeb3 = async function (url) {
    if (window.ethereum) {
        walletObj = walletObj || await new Web3(window.ethereum);
        try {
            // Request account access if needed
            await window.ethereum.enable();

            return walletObj
            // Acccounts now exposed

        } catch (error) {
            // User denied account access...
        }
    }
}
const getCallData = function (amount, decimals, recipient) {
    // ethers.BigNumber.from()
    return '0x' +
        ethers.utils.hexZeroPad(ethers.BigNumber.from(expandDecimals(amount, decimals)).toHexString(), 32).substr(2) +    // Deposit Amount        (32 bytes)
        ethers.utils.hexZeroPad(ethers.utils.hexlify((recipient.length - 2) / 2), 32).substr(2) +    // len(recipientAddress) (32 bytes)
        recipient.substr(2);
}
const createContractInstance = async function (contractAddress, contractABIs, wallet) {

    contractCache[contractAddress] = contractCache[contractAddress] || await new wallet.eth.Contract(contractABIs.abi, contractAddress);
    return contractCache[contractAddress];
}

const erc20Aprrove = async function (contractAddress,address, amount, decimals = 18,wallet, gasPrice = 2000000000, gasLimit = 2100000) {
    let dec = expandDecimals(amount, decimals);
    let contract = new ethers.Contract(contractAddress, GlobalConstants.GlobalConstants.ContractABIs.Erc20.abi, wallet);
    let tx = await contract.approve(address, dec, {

    });
    console.log(tx)
    return tx.hash;

}

const erc20_despoit = async function (contractAddress,token, amount,   dest, resourceId, data,wallet,decimals) {
    console.log("despoit", token, amount)

    let dec = expandDecimals(amount, decimals);
    let contract = new ethers.Contract(contractAddress, GlobalConstants.GlobalConstants.ContractABIs.Bridge.abi, wallet);

   let res =  await  contract.deposit(dest,resourceId,data, {gasPrice:"10000000015",gasLimit :"600000"});

    // let tx = await bridgeInstance.methods.deposit(
    //     dest, // destination chain id
    //     resourceId,
    //     data);
    // let res = await tx.send({ from: address, gasPrice, gasLimit })
    console.log(res)
    return res.hash
}

exports.Call = {
    createWeb3,
    getCallData,
    erc20Aprrove,
    erc20_despoit,
    createContractInstance,
    toEther,
    toUsdt
};
//
//export default Call