package net.osslabz.evm.abi.util;

import org.bouncycastle.jcajce.provider.digest.Keccak;

// Public API: a private constructor would remove the public one callers may use.
@SuppressWarnings("PMD.InstantiableUtilityClass")
public class HashUtil {

    public static byte[] hashAsKeccak(byte[] input) {
        return new Keccak.Digest256().digest(input);
    }
}
