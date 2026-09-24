package net.osslabz.evm.abi.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.osslabz.evm.abi.definition.AbiDefinition;
import org.bouncycastle.util.encoders.Hex;

@Getter
public class AbiDecoder {

    protected final AbiDefinition abi;
    protected final Map<String, AbiDefinition.Entry> methodSignatures = new HashMap<>();

    public AbiDecoder(String abiFilePath) throws IOException {
        this(readAbi(abiFilePath));
    }

    public AbiDecoder(InputStream inputStream) {
        this(AbiDefinition.fromJson(inputStream));
    }

    private AbiDecoder(AbiDefinition abi) {
        this.abi = abi;
        for (AbiDefinition.Entry entry : this.abi) {
            String hexEncodedMethodSignature = Hex.toHexString(entry.encodeSignature());
            this.methodSignatures.put(hexEncodedMethodSignature, entry);
        }
    }

    private static AbiDefinition readAbi(String abiFilePath) throws IOException {
        return AbiDefinition.fromJson(new String(Files.readAllBytes(Paths.get(abiFilePath)), StandardCharsets.UTF_8));
    }

    public DecodedFunctionCall decodeFunctionCall(String inputData) {
        if (inputData == null || (inputData.startsWith("0x") && inputData.length() < 10) || inputData.length() < 8) {
            throw new IllegalArgumentException("Can't decode invalid input '" + inputData + "'.");
        }
        String inputNoPrefix = cleanup(inputData);

        String methodBytes = inputNoPrefix.substring(0, 8);

        if (!this.methodSignatures.containsKey(methodBytes)) {
            // return null;
            throw new IllegalStateException("Couldn't find method with signature " + methodBytes);
        }
        AbiDefinition.Entry abiEntry = this.methodSignatures.get(methodBytes);

        if (!(abiEntry instanceof AbiDefinition.Function)) {
            throw new IllegalArgumentException(
                    "Input data is not a function call, it's of type '" + abiEntry.type + "'.");
        }

        AbiDefinition.Function abiFunction = (AbiDefinition.Function) abiEntry;

        List<DecodedFunctionCall.Param> params = new ArrayList<>(abiFunction.inputs.size());
        List<?> decoded = abiFunction.decode(Hex.decode(inputNoPrefix));

        for (int i = 0; i < decoded.size(); i++) {
            AbiDefinition.Entry.Param paramDefinition = abiFunction.inputs.get(i);
            DecodedFunctionCall.Param param = new DecodedFunctionCall.Param(
                    paramDefinition.getName(), paramDefinition.getType().getName(), decoded.get(i));
            params.add(param);
        }
        return new DecodedFunctionCall(abiFunction.name, params);
    }

    public List<DecodedFunctionCall> decodeFunctionsCalls(String inputData) {

        DecodedFunctionCall decodedFunctionCall = this.decodeFunctionCall(inputData);

        if (!"multicall".equalsIgnoreCase(decodedFunctionCall.getName())) {
            return Collections.singletonList(decodedFunctionCall);
        }

        DecodedFunctionCall.Param multiCallPayloadData = decodedFunctionCall.getParam("data");

        if (multiCallPayloadData == null) {
            throw new IllegalStateException("multicall function call doesn't contain expected data input param.");
        }

        List<DecodedFunctionCall> resolvedCalls = new ArrayList<>();
        Object paramValue = multiCallPayloadData.getValue();

        if (paramValue instanceof Object[]) {
            for (Object singleCallInputData : (Object[]) paramValue) {
                resolvedCalls.add(this.decodeMultiCallEntry(singleCallInputData, multiCallPayloadData));
            }
        } else {
            resolvedCalls.add(this.decodeMultiCallEntry(paramValue, multiCallPayloadData));
        }
        return resolvedCalls;
    }

    private DecodedFunctionCall decodeMultiCallEntry(
            Object callInputData, DecodedFunctionCall.Param multiCallPayloadData) {
        if (callInputData instanceof String) {
            return this.decodeFunctionCall((String) callInputData);
        }
        if (callInputData instanceof byte[]) {
            return this.decodeFunctionCall(Hex.toHexString((byte[]) callInputData));
        }
        throw new IllegalStateException("Can't decode param name=" + multiCallPayloadData.getName() + ", type="
                + multiCallPayloadData.getType() + ", value=" + multiCallPayloadData.getValue());
    }

    public DecodedFunctionCall decodeLogEvent(List<String> topics, String data) {
        if (topics.isEmpty()) {
            throw new IllegalArgumentException("Log.topics is empty");
        }
        String funcSignature = cleanup(topics.get(0));
        AbiDefinition.Entry abiEntry = methodSignatures.get(funcSignature);
        if (abiEntry == null) {
            throw new IllegalStateException("Couldn't find method with signature " + funcSignature);
        } else {
            if (abiEntry instanceof AbiDefinition.Event) {
                AbiDefinition.Event abiEvent = (AbiDefinition.Event) abiEntry;
                List<?> decoded = abiEvent.decode(
                        hexBytes(data),
                        topics.stream().map(AbiDecoder::hexBytes).toArray(byte[][]::new));
                List<DecodedFunctionCall.Param> params = new ArrayList<>(abiEvent.inputs.size());
                for (int i = 0; i < decoded.size(); i++) {
                    AbiDefinition.Entry.Param paramDefinition = abiEvent.inputs.get(i);
                    DecodedFunctionCall.Param param = new DecodedFunctionCall.Param(
                            paramDefinition.getName(), paramDefinition.getType().getName(), decoded.get(i));
                    params.add(param);
                }
                return new DecodedFunctionCall(abiEvent.name, params);
            } else {
                throw new IllegalArgumentException("Input data is not a event, it's of type '" + abiEntry.type + "'.");
            }
        }
    }

    private static String cleanup(String hex) {
        return hex.startsWith("0x") ? hex.substring(2) : hex;
    }

    private static byte[] hexBytes(String hex) {
        return Hex.decode(cleanup(hex));
    }
}
