package net.osslabz.evm.abi.decoder;

import lombok.Getter;
import net.osslabz.evm.abi.definition.AbiDefinition;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class AbiDecoder {

    protected final AbiDefinition abi;
    protected final Map<String, AbiDefinition.Entry> methodSignatures = new HashMap<>();

    public AbiDecoder(String abiFilePath) throws IOException {
        this.abi = AbiDefinition.fromJson(new String(Files.readAllBytes(Paths.get(abiFilePath)), StandardCharsets.UTF_8));
        init();
    }

    public AbiDecoder(InputStream inputStream) {
        this.abi = AbiDefinition.fromJson(inputStream);
        init();
    }

    private void init() {
        for (AbiDefinition.Entry entry : this.abi) {
            String hexEncodedMethodSignature = Hex.toHexString(entry.encodeSignature());
            this.methodSignatures.put(hexEncodedMethodSignature, entry);
        }
    }

    public DecodedFunctionCall decodeFunctionCall(String inputData) {
        if (inputData == null || (inputData.startsWith("0x") && inputData.length() < 10) || inputData.length() < 8) {
            throw new IllegalArgumentException("Can't decode invalid input '" + inputData + "'.");
        }
        String inputNoPrefix = cleanup(inputData);

        String methodBytes = inputNoPrefix.substring(0, 8);

        if (!this.methodSignatures.containsKey(methodBytes)) {
            //return null;
            throw new IllegalStateException("Couldn't find method with signature " + methodBytes);
        }
        AbiDefinition.Entry abiEntry = this.methodSignatures.get(methodBytes);

        if (!(abiEntry instanceof AbiDefinition.Function abiFunction)) {
            throw new IllegalArgumentException("Input data is not a function call, it's of type '" + abiEntry.type + "'.");
        }

        List<DecodedFunctionCall.Param> params = new ArrayList<>(abiFunction.inputs.size());
        List<?> decoded = abiFunction.decode(Hex.decode(inputNoPrefix));

        for (int i = 0; i < decoded.size(); i++) {
            AbiDefinition.Entry.Param paramDefinition = abiFunction.inputs.get(i);
            DecodedFunctionCall.Param param = new DecodedFunctionCall.Param(paramDefinition.getName(), paramDefinition.getType().getName(), decoded.get(i));
            params.add(param);
        }
        return new DecodedFunctionCall(abiFunction.name, params);
    }

    public List<DecodedFunctionCall> decodeFunctionsCalls(String inputData) {

        DecodedFunctionCall decodedFunctionCall = this.decodeFunctionCall(inputData);

        List<DecodedFunctionCall> resolvedCalls = Collections.singletonList(decodedFunctionCall);

        if (decodedFunctionCall.getName().equalsIgnoreCase("multicall")) {

            DecodedFunctionCall.Param multiCallPayloadData = decodedFunctionCall.getParam("data");

            if (multiCallPayloadData == null) {
                throw new IllegalStateException("multicall function call doesn't contain expected data input param.");
            }

            resolvedCalls = new ArrayList<>();
            Object paramValue = multiCallPayloadData.getValue();

            if (paramValue instanceof String) {
                resolvedCalls.add(this.decodeFunctionCall((String) paramValue));
            } else if (paramValue instanceof byte[]) {
                resolvedCalls.add(this.decodeFunctionCall(Hex.toHexString((byte[]) paramValue)));
            } else if (paramValue instanceof Object[]) {
                for (Object singleCallInputData : (Object[]) paramValue) {
                    if (singleCallInputData instanceof String) {
                        DecodedFunctionCall call = this.decodeFunctionCall((String) singleCallInputData);
                        if (call != null) {
                            resolvedCalls.add(call);
                        }
                    } else if (singleCallInputData instanceof byte[]) {
                        DecodedFunctionCall call = this.decodeFunctionCall(Hex.toHexString((byte[]) singleCallInputData));
                        if (call != null) {
                            resolvedCalls.add(call);
                        }
                    } else {
                        throw new IllegalStateException("Can't decode param name=" + multiCallPayloadData.getName() + ", type=" + multiCallPayloadData.getType() + ", value=" + multiCallPayloadData.getValue());
                    }
                }
            } else {
                throw new IllegalStateException("Can't decode param name=" + multiCallPayloadData.getName() + ", type=" + multiCallPayloadData.getType() + ", value=" + multiCallPayloadData.getValue());
            }
        }
        return resolvedCalls;
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
            if (abiEntry instanceof AbiDefinition.Event abiEvent) {
                List<?> decoded = abiEvent.decode(hexBytes(data), topics
                        .stream()
                        .map(AbiDecoder::hexBytes)
                        .toArray(byte[][]::new));
                List<DecodedFunctionCall.Param> params = new ArrayList<>(abiEvent.inputs.size());
                for (int i = 0; i < decoded.size(); i++) {
                    AbiDefinition.Entry.Param paramDefinition = abiEvent.inputs.get(i);
                    DecodedFunctionCall.Param param = new DecodedFunctionCall.Param(paramDefinition.getName(), paramDefinition.getType()
                            .getName(), decoded.get(i));
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
