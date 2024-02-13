package net.osslabz.evm.abi.definition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.Data;
import net.osslabz.evm.abi.util.ByteUtil;
import net.osslabz.evm.abi.util.HashUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;
import static java.lang.String.format;
import static net.osslabz.evm.abi.definition.SolidityType.IntType.decodeInt;
import static org.apache.commons.collections4.ListUtils.select;
import static org.apache.commons.lang3.ArrayUtils.subarray;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.stripEnd;

public class AbiDefinition extends ArrayList<AbiDefinition.Entry> {
    private final static ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

    public static class ParamSanitizer extends StdConverter<Entry.Param, Entry.Param> {
        public ParamSanitizer() {
        }

        @Override
        public Entry.Param convert(Entry.Param param) {
            if (param.type instanceof SolidityType.TupleType) {
                for (Entry.Component c : param.components) {
                    ((SolidityType.TupleType) param.type).types.add(c.getType());
                }
            }
            return param;
        }
    }

    public static AbiDefinition fromJson(String json) {
        try {
            return DEFAULT_MAPPER.readValue(json, AbiDefinition.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static AbiDefinition fromJson(Reader reader) {
        try {
            return DEFAULT_MAPPER.readValue(reader, AbiDefinition.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static AbiDefinition fromJson(InputStream inputStream) {
        try {
            return DEFAULT_MAPPER.readValue(inputStream, AbiDefinition.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends Entry> T find(Class<T> resultClass, final Entry.Type type, final Predicate<T> searchPredicate) {
        return (T) CollectionUtils.find(this, entry -> entry.type == type && searchPredicate.evaluate((T) entry));
    }

    public Function findFunction(Predicate<Function> searchPredicate) {
        return find(Function.class, Entry.Type.function, searchPredicate);
    }

    public Event findEvent(Predicate<Event> searchPredicate) {
        return find(Event.class, Entry.Type.event, searchPredicate);
    }

    public Error findError(Predicate<Error> searchError) {
        return find(Error.class, Entry.Type.error, searchError);
    }

    public Constructor findConstructor() {
        return find(Constructor.class, Entry.Type.constructor, object -> true);
    }

    @Override
    public String toString() {
        return toJson();
    }


    @JsonInclude(Include.NON_NULL)
    public static abstract class Entry {

        public final Boolean anonymous;
        public final Boolean constant;
        public final String name;
        public final List<Param> inputs;
        public final List<Param> outputs;
        public final Type type;
        public final Boolean payable;

        public Entry(Boolean anonymous, Boolean constant, String name, List<Param> inputs, List<Param> outputs, Type type, Boolean payable) {
            this.anonymous = anonymous;
            this.constant = constant;
            this.name = name;
            this.inputs = inputs;
            this.outputs = outputs;
            this.type = type;
            this.payable = payable;
        }

        @JsonCreator
        public static Entry create(@JsonProperty("anonymous") boolean anonymous,
                                   @JsonProperty("constant") boolean constant,
                                   @JsonProperty("name") String name,
                                   @JsonProperty("inputs") List<Param> inputs,
                                   @JsonProperty("outputs") List<Param> outputs,
                                   @JsonProperty("type") Type type,
                                   @JsonProperty(value = "payable", required = false, defaultValue = "false") Boolean payable) {
            Entry result = null;
            switch (type) {
                case constructor:
                    result = new Constructor(inputs, outputs);
                    break;
                case function:
                case fallback:
                    result = new Function(constant, name, inputs, outputs, payable);
                    break;
                case receive:
                    result = new Function(constant, name, inputs, outputs, payable);
                    break;
                case event:
                    result = new Event(anonymous, name, inputs, outputs);
                    break;
                case error:
                    result = new Error(name, inputs);
                    break;
            }

            return result;
        }

        public String formatSignature() {
            StringBuilder paramsTypes = new StringBuilder();
            if (inputs != null) {
                for (Param param : inputs) {
                    String type = param.type.getCanonicalName();
                    if (param.type instanceof SolidityType.TupleType) {
                        type = "(" + StringUtils.join(param.getComponents().stream().map(Component::getType).collect(Collectors.toList()), ",") + ")";
                    }
                    paramsTypes.append(type).append(",");
                }
            }

            return format("%s(%s)", name, stripEnd(paramsTypes.toString(), ","));
        }

        public byte[] fingerprintSignature() {
            return HashUtil.hashAsKeccak(formatSignature().getBytes());
        }

        public byte[] encodeSignature() {
            return fingerprintSignature();
        }

        public enum Type {
            constructor,
            function,
            event,
            fallback,
            receive,
            error
        }

        @Data
        public static class Component {
            private String name;
            private SolidityType type;
        }

        @Data
        @JsonInclude(Include.NON_NULL)
        @JsonDeserialize(converter = ParamSanitizer.class)  // invoked after class is fully deserialized
        public static class Param {
            private Boolean indexed;
            private String name;
            private SolidityType type;

            private List<Component> components;

            public static List<?> decodeList(List<Param> params, byte[] encoded) {
                List<Object> result = new ArrayList<>(params.size());

                int offset = 0;
                for (Param param : params) {
                    Object decoded = param.type.isDynamicType()
                            ? param.type.decode(encoded, decodeInt(encoded, offset).intValue())
                            : param.type.decode(encoded, offset);
                    result.add(decoded);

                    offset += param.type.getFixedSize();
                }

                return result;
            }

            @Override
            public String toString() {
                return format("%s%s%s", type.getCanonicalName(), (indexed != null && indexed) ? " indexed " : " ", name);
            }
        }
    }

    public static class Constructor extends Entry {

        public Constructor(List<Param> inputs, List<Param> outputs) {
            super(null, null, "", inputs, outputs, Type.constructor, false);
        }

        public List<?> decode(byte[] encoded) {
            return Param.decodeList(inputs, encoded);
        }

        public String formatSignature(String contractName) {
            return format("function %s(%s)", contractName, join(inputs, ", "));
        }
    }

    public static class Function extends Entry {

        private static final int ENCODED_SIGN_LENGTH = 4;

        public Function(boolean constant, String name, List<Param> inputs, List<Param> outputs, Boolean payable) {
            super(null, constant, name, inputs, outputs, Type.function, payable);
        }

        public static byte[] extractSignature(byte[] data) {
            return subarray(data, 0, ENCODED_SIGN_LENGTH);
        }

        public byte[] encode(Object... args) {
            return ByteUtil.merge(encodeSignature(), encodeArguments(args));
        }

        private byte[] encodeArguments(Object... args) {
            if (args.length > inputs.size())
                throw new RuntimeException("Too many arguments: " + args.length + " > " + inputs.size());

            int staticSize = 0;
            int dynamicCnt = 0;
            // calculating static size and number of dynamic params
            for (int i = 0; i < args.length; i++) {
                SolidityType type = inputs.get(i).type;
                if (type.isDynamicType()) {
                    dynamicCnt++;
                }
                staticSize += type.getFixedSize();
            }

            byte[][] bb = new byte[args.length + dynamicCnt][];
            for (int curDynamicPtr = staticSize, curDynamicCnt = 0, i = 0; i < args.length; i++) {
                SolidityType type = inputs.get(i).type;
                if (type.isDynamicType()) {
                    byte[] dynBB = type.encode(args[i]);
                    bb[i] = SolidityType.IntType.encodeInt(curDynamicPtr);
                    bb[args.length + curDynamicCnt] = dynBB;
                    curDynamicCnt++;
                    curDynamicPtr += dynBB.length;
                } else {
                    bb[i] = type.encode(args[i]);
                }
            }

            return ByteUtil.merge(bb);
        }

        public List<?> decode(byte[] encoded) {
            return Param.decodeList(inputs, subarray(encoded, ENCODED_SIGN_LENGTH, encoded.length));
        }

        public List<?> decodeResult(byte[] encoded) {
            return Param.decodeList(outputs, encoded);
        }

        @Override
        public byte[] encodeSignature() {
            return extractSignature(super.encodeSignature());
        }

        @Override
        public String toString() {
            String returnTail = "";
            if (constant) {
                returnTail += " constant";
            }
            if (!outputs.isEmpty()) {
                List<String> types = new ArrayList<>();
                for (Param output : outputs) {
                    types.add(output.type.getCanonicalName());
                }
                returnTail += format(" returns(%s)", join(types, ", "));
            }

            return format("function %s(%s)%s;", name, join(inputs, ", "), returnTail);
        }
    }

    public static class Event extends Entry {

        public Event(boolean anonymous, String name, List<Param> inputs, List<Param> outputs) {
            super(anonymous, null, name, inputs, outputs, Type.event, false);
        }

        public List<?> decode(byte[] data, byte[][] topics) {
            List<Object> result = new ArrayList<>(inputs.size());

            byte[][] argTopics = anonymous ? topics : subarray(topics, 1, topics.length);
            List<Param> indexedParams = filteredInputs(true);
            List<Object> indexed = new ArrayList<>();
            for (int i = 0; i < indexedParams.size(); i++) {
                Object decodedTopic;
                if (indexedParams.get(i).type.isDynamicType()) {
                    // If arrays (including string and bytes) are used as indexed arguments,
                    // the Keccak-256 hash of it is stored as topic instead.
                    decodedTopic = SolidityType.Bytes32Type.decodeBytes32(argTopics[i], 0);
                } else {
                    decodedTopic = indexedParams.get(i).type.decode(argTopics[i]);
                }
                indexed.add(decodedTopic);
            }
            List<?> notIndexed = Param.decodeList(filteredInputs(false), data);

            for (Param input : inputs) {
                result.add(input.indexed ? indexed.remove(0) : notIndexed.remove(0));
            }

            return result;
        }

        private List<Param> filteredInputs(final boolean indexed) {
            return select(inputs, param -> param.indexed == indexed);
        }

        @Override
        public String toString() {
            return format("event %s(%s);", name, join(inputs, ", "));
        }
    }

    public static class Error extends Entry {
        public Error(String name, List<Param> inputs) {
            super(null, null, name, inputs, null, Type.error, false);
        }

        public List<?> decode(byte[] encoded) {
            return Param.decodeList(inputs, encoded);
        }

        @Override
        public String toString() {
            return format("error %s(%s);", name, join(inputs, ", "));
        }
    }
}
