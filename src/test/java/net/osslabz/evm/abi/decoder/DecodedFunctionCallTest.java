package net.osslabz.evm.abi.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DecodedFunctionCallTest {

    @Test
    void findsParamsByNameInAnyDefaultLocale() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            DecodedFunctionCall.Param tokenId = new DecodedFunctionCall.Param("tokenId", "uint256", BigInteger.ONE);

            DecodedFunctionCall call = new DecodedFunctionCall("burn", Collections.singletonList(tokenId));

            assertSame(tokenId, call.getParam("tokenid"));
            assertEquals(Collections.singleton("tokenid"), call.params().keySet());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
