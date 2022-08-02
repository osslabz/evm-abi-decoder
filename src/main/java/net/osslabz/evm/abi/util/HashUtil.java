package net.osslabz.evm.abi.util;

import org.bouncycastle.jcajce.provider.digest.Keccak;

public class HashUtil {

    public static byte[] hashAsKeccak(byte[] input) {
        Keccak.Digest256 digest256 = new Keccak.Digest256();
        byte[] output = digest256.digest(input);
        return output;
    }
}
