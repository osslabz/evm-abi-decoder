package net.osslabz.evm.abi.definition;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SolidityTypeTest {

    @Test
    void bytesTypeEncodesStringsAsUtf8() {
        SolidityType.BytesType type = new SolidityType.BytesType();

        byte[] encoded = type.encode("grüße");

        assertArrayEquals("grüße".getBytes(StandardCharsets.UTF_8), (byte[]) type.decode(encoded));
    }
}
