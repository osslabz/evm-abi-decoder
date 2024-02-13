package net.osslabz.evm.abi;

import lombok.extern.slf4j.Slf4j;
import net.osslabz.evm.abi.decoder.AbiDecoder;
import net.osslabz.evm.abi.decoder.DecodedFunctionCall;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class AbiDecoderTest {

    @Test
    public void testDecodeFunctionCallUniswapV2Router02() throws IOException {

        // Abi can be found here: https://etherscan.io/address/0x7a250d5630b4cf539739df2c5dacb4c659f2488d#code
        File abiJson = new File(this.getClass().getResource("/abiFiles/UniswapV2Router02.json").getPath());
        AbiDecoder uniswapv2Abi = new AbiDecoder(abiJson.getAbsolutePath());

        // tx: https://etherscan.io/tx/0xde2b61c91842494ac208e25a2a64d99997c382f6aaf0719d6a719b5cff1f8a07
        String inputData = "0x18cbafe5000000000000000000000000000000000000000000000000000000000098968000000000000000000000000000000000000000000000000000165284993ac4ac00000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000d4cf8e47beac55b42ae58991785fa326d9384bd10000000000000000000000000000000000000000000000000000000062e8d8510000000000000000000000000000000000000000000000000000000000000002000000000000000000000000a0b86991c6218b36c1d19d4a2e9eb0ce3606eb48000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";

        /**
         * #	Name	      Type	     Data
         * 0	amountIn	  uint256	 10000000
         * 1	amountOutMin  uint256	 6283178947560620
         * 2	path	      address[]	 0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48
         *                               0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2
         * 3	to	          address	 0xD4CF8e47BeAC55b42Ae58991785Fa326d9384Bd1
         * 4	deadline	uint256	1659426897
         */

        DecodedFunctionCall decodedFunctionCall = uniswapv2Abi.decodeFunctionCall(inputData);

        Assertions.assertEquals("swapExactTokensForETH", decodedFunctionCall.getName());

        List<DecodedFunctionCall.Param> paramList = decodedFunctionCall.getParamList();

        DecodedFunctionCall.Param param0 = paramList.get(0);
        Assertions.assertEquals("amountIn", param0.getName());
        Assertions.assertEquals("uint256", param0.getType());
        Assertions.assertEquals(BigInteger.valueOf(10000000), param0.getValue());

        DecodedFunctionCall.Param param1 = paramList.get(1);
        Assertions.assertEquals("amountOutMin", param1.getName());
        Assertions.assertEquals("uint256", param1.getType());
        Assertions.assertEquals(new BigInteger("6283178947560620"), param1.getValue());

        DecodedFunctionCall.Param param2 = paramList.get(2);
        Assertions.assertEquals("path", param2.getName());
        Assertions.assertEquals("address[]", param2.getType());
        Assertions.assertEquals(Arrays.toString(new String[]{"0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"}), Arrays.toString((Object[]) param2.getValue()));

        DecodedFunctionCall.Param param3 = paramList.get(3);
        Assertions.assertEquals("to", param3.getName());
        Assertions.assertEquals("address", param3.getType());
        Assertions.assertEquals("0xd4cf8e47beac55b42ae58991785fa326d9384bd1", param3.getValue());

        DecodedFunctionCall.Param param4 = paramList.get(4);
        Assertions.assertEquals("deadline", param4.getName());
        Assertions.assertEquals("uint256", param4.getType());
        Assertions.assertEquals(BigInteger.valueOf(1659426897), param4.getValue());
    }

    @Test
    public void testDecodeFunctionCallWithTuple() throws IOException {
        File abiJson = new File(this.getClass().getResource("/abiFiles/UniswapV3SwapRouter02.json").getPath());
        AbiDecoder uniswapv3Abi = new AbiDecoder(abiJson.getAbsolutePath());

        String inputData = "0x04e45aaf000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc20000000000000000000000002260fac5e5542a773aa44fbcfedf7c193bc2c59900000000000000000000000000000000000000000000000000000000000001f4000000000000000000000000bebc44782c7db0a1a60cb6fe97d0b483032ff1c70000000000000000000000000000000000000000000000000000000000067932000000000000000000000000000000000000000000000000000000000000002a0000000000000000000000000000000000000000000000000000000000000000";

        DecodedFunctionCall decodedFunctionCall = uniswapv3Abi.decodeFunctionCall(inputData);

        Assertions.assertEquals("exactInputSingle", decodedFunctionCall.getName());


        DecodedFunctionCall.Param param0 = decodedFunctionCall.getParams().stream().findFirst().get();
        Assertions.assertEquals("params", param0.getName());
        Assertions.assertEquals("tuple", param0.getType());
        Object[] v = (Object[]) param0.getValue();
        Assertions.assertEquals("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2", v[0].toString());
        Assertions.assertEquals("0x2260fac5e5542a773aa44fbcfedf7c193bc2c599", v[1].toString());
        Assertions.assertEquals("500", v[2].toString());
        Assertions.assertEquals("424242", v[4].toString());
        Assertions.assertEquals("42", v[5].toString());
    }


    @Test
    public void testDecodeFunctionCallUniswapV3SwapRouter02() throws IOException {

        File abiJson = new File(this.getClass().getResource("/abiFiles/UniswapV3SwapRouter02.json").getPath());
        AbiDecoder uniswapv3SwapRouter02Abi = new AbiDecoder(abiJson.getAbsolutePath());

        // https://etherscan.io/tx/0x731847de5b19b26039f283826ae5218ac7e070ed1b7fff689c2253a3035d8bd6
        String inputData = "0x5ae401dc0000000000000000000000000000000000000000000000000000000062ed6b0d000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000000e4472b43f3000000000000000000000000000000000000000000000000000008c75ee6fb3900000000000000000000000000000000000000000000000001cb1a1493ed3d4b0000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000020000000000000000000000009bbe10ba8ad02c2a54963b3e2a64f1754c90f411000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004449404b7c00000000000000000000000000000000000000000000000001cb1a1493ed3d4b000000000000000000000000c0da58d88e967d883ef0540db458381e9f5e9c8000000000000000000000000000000000000000000000000000000000";
        List<DecodedFunctionCall> decodedFunctionCalls = uniswapv3SwapRouter02Abi.decodeFunctionsCalls(inputData);


        int i = 0;
        for (DecodedFunctionCall func : decodedFunctionCalls) {
            log.debug("{}: function: {}", i, func.getName());
            int p = 0;
            for (DecodedFunctionCall.Param param : func.getParams()) {
                log.debug("param {}: name={}, type={}, value={}", p, param.getName(), param.getType(), param.getValue());
                p++;
            }
            i++;
            log.debug("-------------------------");
        }
    }

    @Test
    public void testDecodeFunctionCallUniswapV3SwapRouter02Swap() throws IOException {

        File abiJson = new File(this.getClass().getResource("/abiFiles/UniswapV3SwapRouter02.json").getPath());
        AbiDecoder uniswapv3SwapRouter02Abi = new AbiDecoder(abiJson.getAbsolutePath());

        // https://etherscan.io/tx/0xb8ea1e889a4b7bfd1a51359801645518e2f648522c6662c4dd2939a5d9fe6ff4
        String inputData = "0x5ae401dc0000000000000000000000000000000000000000000000000000000062fba2e2000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000000e4472b43f3000000000000000000000000000000000000000003a5efc474214c296d933ea100000000000000000000000000000000000000000000000000d5b866fcc68674000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002000000000000000000000000d041e4427412ede81ccac87f08fa3490af61033d000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004449404b7c00000000000000000000000000000000000000000000000000d5b866fcc68674000000000000000000000000caed7876d89f4f67808e0301a1dfb218d73f569000000000000000000000000000000000000000000000000000000000";
        List<DecodedFunctionCall> decodedFunctionCalls = uniswapv3SwapRouter02Abi.decodeFunctionsCalls(inputData);


        int i = 0;
        for (DecodedFunctionCall func : decodedFunctionCalls) {
            log.debug("{}: function: {}", i, func.getName());
            int p = 0;
            for (DecodedFunctionCall.Param param : func.getParams()) {
                log.debug("param {}: name={}, type={}, value={}", p, param.getName(), param.getType(), param.getValue());
                p++;
            }
            i++;
            log.debug("-------------------------");
        }
    }

    @Test
    public void testDecodeFunctionCallTupleContainingDynamicTypes() throws IOException {

        // https://api-testnet.bscscan.com/api?module=contract&action=getabi&address=0xb7564227245bb161ebf4d350e1056c26801f1366&format=raw
        File abiJson = new File(this.getClass().getResource("/abiFiles/SereshForwarder.json").getPath());
        AbiDecoder sereshForwarderAbi = new AbiDecoder(abiJson.getAbsolutePath());

        // https://testnet.bscscan.com/tx/0x73b80d49777f0c32d45a0a5a7c3487eb9e8da2c93922540c260cdafc3e81a165
        String inputData = "0x005575f20000000000000000000000000000000000000000000000000000000000000080967c9812e5f939318262ccbd023be072015c3ad2f470d47ab5e6b13e1ca810a540274bf9ce7b9da08b0003fc05e67d74e993f3381bf00bcba0fef022bf3b8d6a000000000000000000000000000000000000000000000000000000000000001b000000000000000000000000ddcfc6f09a26413c2b0d6224b29738e74102de04000000000000000000000000cbb869911c0acd242c15a03c42ce3ddcdd82ea1b000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000038d7ea4c68000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000001e4216f62d8000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000e0000000000000000000000000000000000000000000000000000000000000014000000000000000000000000000000000000000000000000000000000000001a0000000000000000000000000000000000000000000000000000000000000003b6261666b7265696263796c746f36667974667336746f796f6f716f6b6272366e333566673767683236646e6f3464766a716c6d743464687a34716d0000000000000000000000000000000000000000000000000000000000000000000000003b6261666b726569647533356c64727965703433797574337275616a747936743278346b3773787077737365347572616b7676366d723763657a696d0000000000000000000000000000000000000000000000000000000000000000000000003b6261666b7265696567616e657563727a6e646b6676726a64346a63346235356174646b707475326d6d3779327372783235617068626b623666373400000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000ddcfc6f09a26413c2b0d6224b29738e74102de0400000000000000000000000000000000000000000000000000000000";
        List<DecodedFunctionCall> decodedFunctionCalls = sereshForwarderAbi.decodeFunctionsCalls(inputData);

        int i = 0;
        for (DecodedFunctionCall func : decodedFunctionCalls) {
            log.debug("{}: function: {}", i, func.getName());
            int p = 0;
            for (DecodedFunctionCall.Param param : func.getParams()) {
                log.debug("param {}: name={}, type={}, value={}", p, param.getName(), param.getType(), param.getValue());
                p++;
            }
            i++;
            log.debug("-------------------------");
        }
    }
}
