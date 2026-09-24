package net.osslabz.evm.abi.definition;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SolidityTypeTest {

    @ParameterizedTest
    @CsvSource({
        "bool, BoolType",
        "int8, IntType",
        "int, IntType",
        "uint256, UnsignedIntType",
        "uint, UnsignedIntType",
        "address, AddressType",
        "string, StringType",
        "bytes, BytesType",
        "bytes32, Bytes32Type",
        "function, FunctionType",
        "tuple, TupleType",
        "uint256[], DynamicArrayType",
        "address[2], StaticArrayType"
    })
    void resolvesTypeNamesToTheirSolidityType(String typeName, String expectedClass) {
        SolidityType type = SolidityType.getType(typeName);

        assertEquals(expectedClass, type.getClass().getSimpleName());
        assertEquals(typeName, type.getName());
    }

    @Test
    void createsAFreshTupleTypeOnEveryLookup() {
        assertNotSame(SolidityType.getType("tuple"), SolidityType.getType("tuple"));
    }

    @Test
    void rejectsUnknownTypeNames() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> SolidityType.getType("fixed128x18"));

        assertEquals("Unknown type: fixed128x18", e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"int, int256", "uint, uint256", "int8, int8", "uint[], uint256[]", "int[3], int256[3]"})
    void expandsShorthandTypesInCanonicalNames(String typeName, String canonicalName) {
        assertEquals(canonicalName, SolidityType.getType(typeName).getCanonicalName());
    }

    @Test
    void staticArrayTypeReadsItsSizeFromTheTypeName() {
        SolidityType.StaticArrayType type = (SolidityType.StaticArrayType) SolidityType.getType("uint8[3]");

        assertInstanceOf(SolidityType.UnsignedIntType.class, type.getElementType());
        assertEquals(3 * 32, type.getFixedSize());
    }

    @Test
    void staticArrayTypeRejectsANonNumericSize() {
        assertThrows(NumberFormatException.class, () -> new SolidityType.StaticArrayType("uint8[x]"));
    }

    @Test
    void staticArrayTypeRejectsAListOfTheWrongSize() {
        SolidityType type = SolidityType.getType("uint8[3]");

        RuntimeException e = assertThrows(RuntimeException.class, () -> type.encode(Arrays.asList(1, 2)));

        assertEquals("List size (2) != 3 for type uint8[3]", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"31", "0x1f", "0X1F", " 1f ", "1F"})
    void numericTypesParseDecimalAndHexStrings(String value) {
        byte[] encoded = new SolidityType.UnsignedIntType("uint256").encode(value);

        assertEquals(BigInteger.valueOf(31), SolidityType.UnsignedIntType.decodeInt(encoded, 0));
    }

    @Test
    void numericTypesEncodeNumbersAndBytes() {
        SolidityType type = new SolidityType.IntType("int256");

        assertEquals(BigInteger.valueOf(-5), SolidityType.IntType.decodeInt(type.encode(-5L), 0));
        assertEquals(BigInteger.TEN, SolidityType.IntType.decodeInt(type.encode(BigInteger.TEN), 0));
        assertEquals(BigInteger.valueOf(258), SolidityType.IntType.decodeInt(type.encode(new byte[] {1, 2}), 0));
    }

    @Test
    void numericTypesRejectOtherValues() {
        SolidityType type = new SolidityType.IntType("int256");

        RuntimeException e = assertThrows(RuntimeException.class, () -> type.encode(new Object()));

        assertTrue(e.getMessage().startsWith("Invalid value for type 'int256': java.lang.Object@"));
    }

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
