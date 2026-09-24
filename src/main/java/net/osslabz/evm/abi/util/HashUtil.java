package net.osslabz.evm.abi.util;

import org.bouncycastle.jcajce.provider.digest.Keccak;

public class HashUtil {

    public static byte[] hashAsKeccak(byte[] input) {
        return new Keccak.Digest256().digest(input);
    }
}
