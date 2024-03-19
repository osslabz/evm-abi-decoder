package net.osslabz.evm.abi.decoder;

import lombok.Data;
import net.osslabz.evm.abi.util.ByteUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Data
public class DecodedFunctionCall {

    private String name;
    private Map<String, Param> params;

    public DecodedFunctionCall(String name, List<Param> params) {
        this.name = name;
        this.params = new LinkedHashMap<>();
        for (Param param : params) {
            this.params.put(param.getName().toLowerCase(), param);
        }
    }

    public Param getParam(String paramName) {
        return this.params.get(paramName.toLowerCase());
    }

    public Map<String, Param> params() {
        return this.params;
    }

    public Collection<Param> getParams() {
        return this.params.values();
    }

    public List<Param> getParamList() {
        return new ArrayList<>(this.getParams());
    }

    public int getSize() {
        return this.params.size();
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
            } else if (value instanceof Object[] valueAsObjectArray) {
                this.value = new Object[valueAsObjectArray.length];
                for (int i = 0; i < valueAsObjectArray.length; i++) {
                    Object o = valueAsObjectArray[i];
                    ((Object[]) this.value)[i] = o instanceof byte[] ? "0x" + ByteUtil.toHexString((byte[]) o) : o;
                }
            } else {
                this.value = value;
            }
        }

        public String toString() {
            String valueString = this.value == null ? "null" : (this.value.getClass().isArray() ? Arrays.toString((Object[]) this.value) : this.value.toString());
            return this.getClass().getName() + "(name=" + this.name + ", type=" + this.getType() + ", value=" + valueString + ")";
        }
    }
}
