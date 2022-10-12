EVM ABI Decoder
============
![GitHub](https://img.shields.io/github/license/rvullriede/evm-abi-decoder)
![GitHub Workflow Status](https://img.shields.io/github/workflow/status/rvullriede/evm-abi-decoder/Java%20CI%20with%20Maven)
[![Maven Central](https://img.shields.io/maven-central/v/net.osslabz.evm/evm-abi-decoder?label=Maven%20Central)](https://search.maven.org/artifact/net.osslabz.evm/evm-abi-decoder)

EVM ABI Decoder allows to decode raw input data from EVM tx (on Ethereum or a compatible chain like Avalanche, BSC stc)
into processable format obtained from the contract's ABi definition (JSON).

**Acknowledgement**:
This project is based on [Bryce Neals's](https://github.com/prettymuchbryce) project [abidecoder](https://github.com/prettymuchbryce/abidecoder) (Kotlin), which itself is a port
of [ConsenSys](https://github.com/ConsenSys) project [abi-decoder](https://github.com/ConsenSys/abi-decoder) (JavaScript).

The original project is written in Kotlin, only published on [JitPack](https://jitpack.io/) (but not on [Maven Central](https://search.maven.org/) and depends on the now
deprecated [ethereumj](https://github.com/ethereum/ethereumj). These were enough reasons for me to rewrite it in Java ;-)

QuickStart
---------

Maven
------

```xml

<dependency>
    <groupId>net.osslabz.evm</groupId>
    <artifactId>evm-abi-decoder</artifactId>
    <version>0.0.6</version>
</dependency>
```

Usage
------

Loads the UniswapV2Router02 contract and parses a swap transaction:

```java
// Abi can be found here: https://etherscan.io/address/0x7a250d5630b4cf539739df2c5dacb4c659f2488d#code
AbiDecoder uniswapv2Abi=new AbiDecoder(this.getClass().getResource("/abiFiles/UniswapV2Router02.json").getFile());

// tx: https://etherscan.io/tx/0xde2b61c91842494ac208e25a2a64d99997c382f6aaf0719d6a719b5cff1f8a07
String inputData="0x18cbafe5000000000000000000000000000000000000000000000000000000000098968000000000000000000000000000000000000000000000000000165284993ac4ac00000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000d4cf8e47beac55b42ae58991785fa326d9384bd10000000000000000000000000000000000000000000000000000000062e8d8510000000000000000000000000000000000000000000000000000000000000002000000000000000000000000a0b86991c6218b36c1d19d4a2e9eb0ce3606eb48000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";

/**
 * #  Name          Type       Data
 * ----------------------------------------------------------------------
 * 0  amountIn      uint256    10000000
 * 1  amountOutMin  uint256    6283178947560620
 * 2  path          address[]  0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48
 *                             0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2
 * 3  to            address    0xD4CF8e47BeAC55b42Ae58991785Fa326d9384Bd1
 * 4  deadline      uint256    1659426897
 */

DecodedFunctionCall decodedFunctionCall=uniswapv2Abi.decodeFunctionCall(inputData);

System.out.println(decodedFunctionCall.getName()); // prints swapExactTokensForETH

```

Logging
------
This project uses slf4j-api but doesn't package an implementation. This is up to the using application. For the
tests logback is backing slf4j as implementation, with a default configuration logging to STOUT.
