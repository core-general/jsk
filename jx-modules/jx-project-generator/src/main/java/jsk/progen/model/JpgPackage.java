package jsk.progen.model;

import sk.utils.files.PathWithBase;

import java.util.Arrays;

public class JpgPackage extends PathWithBase {
    public JpgPackage(String base) {
        super(base.replace(".", "/"));
    }

    public JpgPackage(String base, String... path) {
        super(base.replace(".", "/"), Arrays.stream(path).map($ -> $.replace(".", "/")).toArray(String[]::new));
    }

    public String asPackage() {
        return getPathNoSlash().replace("/", ".");
    }
}
