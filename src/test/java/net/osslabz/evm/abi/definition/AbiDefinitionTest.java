package net.osslabz.evm.abi.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

class AbiDefinitionTest {

    private static final String ABI = "["
            + "{\"type\":\"constructor\",\"inputs\":[{\"name\":\"owner\",\"type\":\"address\"}]},"
            + "{\"type\":\"function\",\"name\":\"transfer\",\"inputs\":[{\"name\":\"to\",\"type\":\"address\"},"
            + "{\"name\":\"amount\",\"type\":\"uint256\"}],\"outputs\":[{\"name\":\"\",\"type\":\"bool\"}]},"
            + "{\"type\":\"event\",\"name\":\"Transfer\",\"anonymous\":false,\"inputs\":["
            + "{\"name\":\"from\",\"type\":\"address\",\"indexed\":true},"
            + "{\"name\":\"value\",\"type\":\"uint256\",\"indexed\":false}]},"
            + "{\"type\":\"error\",\"name\":\"InsufficientBalance\",\"inputs\":[{\"name\":\"needed\",\"type\":\"uint256\"}]}"
            + "]";

    private final AbiDefinition abi = AbiDefinition.fromJson(ABI);

    @Test
    void findsEntriesOfEachKind() {
        assertEquals(
                "function transfer(address to, uint256 amount) returns(bool);",
                abi.findFunction(f -> "transfer".equals(f.name)).toString());
        assertEquals(
                "event Transfer(address indexed from, uint256 value);",
                abi.findEvent(e -> "Transfer".equals(e.name)).toString());
        assertEquals(
                "error InsufficientBalance(uint256 needed);",
                abi.findError(e -> "InsufficientBalance".equals(e.name)).toString());
        assertEquals("function Token(address owner)", abi.findConstructor().formatSignature("Token"));
    }

    @Test
    void findsNothingWhenOnlyAnotherKindMatches() {
        assertNull(abi.findEvent(e -> "transfer".equals(e.name)));
        assertNotNull(abi.findFunction(f -> "transfer".equals(f.name)));
    }

    @Test
    void readsTheSameDefinitionFromAReader() {
        assertEquals(abi.toJson(), AbiDefinition.fromJson(new StringReader(ABI)).toJson());
    }

    @Test
    void wrapsUnreadableJsonInARuntimeException() {
        assertThrows(RuntimeException.class, () -> AbiDefinition.fromJson("not json"));
    }
}
