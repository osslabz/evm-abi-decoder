package net.osslabz.evm.abi.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.List;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

class AbiDecoderInlineAbiTest {

    private static final String ABI = "["
            + function("transfer", "{\"name\":\"to\",\"type\":\"address\"},{\"name\":\"amount\",\"type\":\"uint256\"}")
            + "," + function("multicall", "{\"name\":\"data\",\"type\":\"bytes[]\"}")
            + "," + function("multicall", "{\"name\":\"data\",\"type\":\"bytes\"}")
            + "," + function("multicall", "{\"name\":\"data\",\"type\":\"bool\"}")
            + "," + function("multicall", "{\"name\":\"data\",\"type\":\"uint256[]\"}")
            + "," + function("multicall", "{\"name\":\"deadline\",\"type\":\"uint256\"}")
            + "]";

    private static final String RECIPIENT = "0x00000000000000000000000000000000000000aa";

    private final AbiDecoder decoder = new AbiDecoder(new ByteArrayInputStream(ABI.getBytes(StandardCharsets.UTF_8)));

    @Test
    void decodesAPlainCallAsASingleCall() {
        List<DecodedFunctionCall> calls = decoder.decodeFunctionsCalls(hex(transfer(7)));

        assertEquals(1, calls.size());
        assertEquals("transfer", calls.get(0).getName());
        assertEquals(RECIPIENT, calls.get(0).getParam("to").getValue());
        assertEquals(BigInteger.valueOf(7), calls.get(0).getParam("amount").getValue());
    }

    @Test
    void decodesEveryCallInsideAMulticall() {
        byte[] multicall = encode("multicall(bytes[])", (Object) new Object[] {transfer(1), transfer(2)});

        List<DecodedFunctionCall> calls = decoder.decodeFunctionsCalls(hex(multicall));

        assertEquals(2, calls.size());
        assertEquals(BigInteger.ONE, calls.get(0).getParam("amount").getValue());
        assertEquals(BigInteger.valueOf(2), calls.get(1).getParam("amount").getValue());
    }

    @Test
    void decodesTheSingleCallInsideAMulticallOverBytes() {
        byte[] multicall = encode("multicall(bytes)", (Object) transfer(3));

        List<DecodedFunctionCall> calls = decoder.decodeFunctionsCalls(hex(multicall));

        assertEquals(1, calls.size());
        assertEquals(BigInteger.valueOf(3), calls.get(0).getParam("amount").getValue());
    }

    @Test
    void rejectsAMulticallWithoutADataParam() {
        String input = hex(encode("multicall(uint256)", BigInteger.ONE));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> decoder.decodeFunctionsCalls(input));

        assertEquals("multicall function call doesn't contain expected data input param.", e.getMessage());
    }

    @Test
    void rejectsMulticallDataThatIsNotACall() {
        String input = hex(encode("multicall(bool)", true));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> decoder.decodeFunctionsCalls(input));

        assertEquals("Can't decode param name=data, type=bool, value=true", e.getMessage());
    }

    @Test
    void rejectsMulticallDataElementsThatAreNotCalls() {
        String input = hex(encode("multicall(uint256[])", (Object) new Object[] {BigInteger.ONE}));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> decoder.decodeFunctionsCalls(input));

        assertTrue(
                e.getMessage().startsWith("Can't decode param name=data, type=uint256[], value=[Ljava.lang.Object;@"));
    }

    @Test
    void rejectsInputTooShortForASelector() {
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> decoder.decodeFunctionCall("0x1234"));

        assertEquals("Can't decode invalid input '0x1234'.", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> decoder.decodeFunctionCall(null));
    }

    @Test
    void rejectsAnUnknownSelector() {
        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> decoder.decodeFunctionCall("0xdeadbeef"));

        assertEquals("Couldn't find method with signature deadbeef", e.getMessage());
    }

    @Test
    void rejectsALogWithoutTopics() {
        List<String> topics = Collections.emptyList();

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> decoder.decodeLogEvent(topics, "0x"));

        assertEquals("Log.topics is empty", e.getMessage());
    }

    @Test
    void rejectsALogWhoseTopicIsAFunction() {
        List<String> topics = Collections.singletonList(hex(
                decoder.getAbi().findFunction(f -> "transfer".equals(f.name)).encodeSignature()));

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> decoder.decodeLogEvent(topics, "0x"));

        assertEquals("Input data is not a event, it's of type 'function'.", e.getMessage());
    }

    @Test
    void failsToReadAMissingAbiFile() {
        assertThrows(NoSuchFileException.class, () -> new AbiDecoder("does/not/exist.json"));
    }

    private static String function(String name, String inputs) {
        return "{\"type\":\"function\",\"name\":\"" + name + "\",\"inputs\":[" + inputs + "],\"outputs\":[]}";
    }

    private byte[] transfer(long amount) {
        return encode("transfer(address,uint256)", RECIPIENT, BigInteger.valueOf(amount));
    }

    private byte[] encode(String signature, Object... args) {
        return decoder.getAbi()
                .findFunction(f -> signature.equals(f.formatSignature()))
                .encode(args);
    }

    private static String hex(byte[] bytes) {
        return "0x" + Hex.toHexString(bytes);
    }
}
