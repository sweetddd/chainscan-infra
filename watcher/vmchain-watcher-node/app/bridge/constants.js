let Erc20 = require('./contracts/ERC20.json');
let Bridge = require('./contracts/Bridge.json');
let Erc20Handler = require('./contracts/ERC20Handler.json');
let GenericHandler = require('./contracts/GenericHandler.json');

//公用的
const ERC20_RESOURCE_ID="0x000000000000000000000000000000483ebe4a02bbc34786d860b355f5a5ce00"
const USDT_RESOURCE_ID="0x000000000000000000000000000003542ebe4a02bbc34786d860b355f5a5ce00"
//标准erc20 可以互转
const ERC20="0xaa469899FD46e6e146272534473DE3c978463027"
const VERC20="0x448BB21ED6f8a12d0a03594C3663AD2d4BEC4aE6"
//转移 usdt to vmchain
const USDT_ADDRESS="0x4f206AD31Db8812111DD2F657211aF0Ef9fEEEe1"
const VUSDT_ADDRESS="0xcD99ad44621fa67Ea313AD5E336574eFF1641f11"


const Contracts={
    ERC20:{
        from:ERC20,
        to: VERC20,
        resourceId: ERC20_RESOURCE_ID,
        decimals: 6,
    },
    USDT:{
        from:USDT_ADDRESS,
        to: VUSDT_ADDRESS,
        resourceId: USDT_RESOURCE_ID,
        decimals: 18,
    }
}
//eth chain constants
const ETH_BRIDGE_ADDRESS = "0xdD4e3ce584B83e36DEfc1945A3305104a89b644f";
const ETH_ERC20_HANDLER_ADDRESS = "0x701a14C59FfDa650F12C8f67D041816731A3De3E";
const ETH_GENERIC_HANDLER_ADDRESS = "0x2dBc54b20919C2e00d3B9300242Fc97A0dcD62be";
const ETH_CHAIN_ID=1;


//vmchain chain constants
const VMCHAIN_BRIDGE_ADDRESS = "0x593927DB30671d2d28c70eB74e22D707724dabE3";
const VMCHAIN_ERC20_HANDLER_ADDRESS = "0x4b2591A9Ad52b5C0407eceE999E2EEA9c3655843";
const VMCHAIN_GENERIC_HANDLER_ADDRESS = "0x3AD5C874705Bc1DD5545e9ae0E7462B93Cb36Cd9";
const VMCHAIN_ID=97;


//vmchain chain constants;



const ContractABIs = {
    Bridge,
    Erc20Handler,
    GenericHandler,
    Erc20
}

exports.GlobalConstants={
    Contracts,
    ContractABIs,
    ETH_CHAIN_ID,
    VMCHAIN_ID,
    ETH_GENERIC_HANDLER_ADDRESS,
    ETH_ERC20_HANDLER_ADDRESS,
    ETH_BRIDGE_ADDRESS,
    ERC20_RESOURCE_ID,
    USDT_RESOURCE_ID,
    VMCHAIN_BRIDGE_ADDRESS,
    VMCHAIN_ERC20_HANDLER_ADDRESS,
    VMCHAIN_GENERIC_HANDLER_ADDRESS,
}
//export default GlobalConstants;