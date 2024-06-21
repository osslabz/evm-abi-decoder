package net.osslabz.evm.abi.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

@UtilityClass
public class FileUtil {

    public String readFileIntoString(String path) throws URISyntaxException, IOException {
        URL resource = Objects.requireNonNull(FileUtil.class.getClassLoader().getResource(path));
        return new String(Files.readAllBytes(Paths.get(resource.toURI())), StandardCharsets.UTF_8);
    }
}