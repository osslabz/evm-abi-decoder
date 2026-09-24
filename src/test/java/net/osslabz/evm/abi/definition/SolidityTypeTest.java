package net.osslabz.evm.abi.definition;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SolidityTypeTest {

    @Test
    void bytesTypeEncodesStringsAsUtf8() {
        SolidityType.BytesType type = new SolidityType.BytesType();

        byte[] encoded = type.encode("grüße");

        assertArrayEquals("grüße".getBytes(StandardCharsets.UTF_8), (byte[]) type.decode(encoded));
    }

    @Test
    void boolTypeEncodesEveryTrueBooleanAsOne() throws ReflectiveOperationException {
        // The deprecated constructor is the only way to get a true that is not Boolean.TRUE.
        Boolean uncachedTrue = Boolean.class.getConstructor(boolean.class).newInstance(true);

        byte[] encoded = new SolidityType.BoolType().encode(uncachedTrue);

        assertEquals(BigInteger.ONE, SolidityType.IntType.decodeInt(encoded, 0));
    }
}
