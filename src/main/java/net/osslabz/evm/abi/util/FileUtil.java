package net.osslabz.evm.abi.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@UtilityClass
public class FileUtil {

    public String read(String path) throws URISyntaxException, IOException {
        return Files.readString(Path.of(Objects.requireNonNull(FileUtil.class
                .getClassLoader()
                .getResource(path)).toURI()));
    }
}
