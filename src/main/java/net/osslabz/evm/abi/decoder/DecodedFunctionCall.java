package net.osslabz.evm.abi.decoder;

import lombok.Data;
import net.osslabz.evm.abi.util.ByteUtil;

import java.util.List;

@Data
public class DecodedFunctionCall {
    private String name;
    private List<Param> params;

    public DecodedFunctionCall(String name, List<Param> params) {
        this.name = name;
        this.params = params;
    }


    @Data
    public static class Param {
        private String name;
        private String type;
        private Object value;

        public Param(String name, String type, Object value) {
            this.name = name;
            this.type = type;
            if (value instanceof byte[]) {
                this.value = "0x" + ByteUtil.toHexString((byte[]) value);
            } else if (value instanceof Object[]) {
                Object[] valueAsObjectArray = (Object[]) value;
                this.value = new Object[valueAsObjectArray.length];
                for (int i = 0; i < valueAsObjectArray.length; i++) {
                    Object o = valueAsObjectArray[i];
                    ((Object[]) this.value)[i] = o instanceof byte[] ? "0x" + ByteUtil.toHexString((byte[]) o) : o;
                }
            } else {
                this.value = value;
            }
        }
    }
}
