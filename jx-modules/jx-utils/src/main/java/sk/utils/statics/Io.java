package sk.utils.statics;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import sk.utils.files.FileList;
import sk.utils.functional.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;
import static java.util.stream.Collectors.toMap;
import static sk.utils.functional.O.*;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class Io {


    public static void endlessReadFromKeyboard(String endSequence, C1<String> consumer) {
        endlessReadFromKeyboard(endSequence, s -> {
            consumer.accept(s);
            return true;
        });
    }

    public static void endlessReadFromKeyboard(String endSequence, F1<String, Boolean> consumer) {
        endSequence = endSequence.trim();
        try (Scanner in = new Scanner(System.in)) {
            while (true) {
                String s = in.nextLine().trim();
                if (Fu.equal(s, endSequence)) {
                    break;
                } else {
                    final Boolean continuE = consumer.apply(s);
                    if (!continuE) {
                        break;
                    }
                }
            }
        }
    }

    public static byte[] streamToBytes(InputStream is) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4 * 1024];
            for (int n = is.read(buf); n != -1; n = is.read(buf)) {
                os.write(buf, 0, n);
            }
            return os.toByteArray();
        } catch (IOException e) {
            return Ex.thRow(e);
        }
    }

    public static InputStream bytesToStream(byte[] in) {
        return new ByteArrayInputStream(in);
    }

    //region Read
    public static FileList fileToStructure(String path) {
        FileList toRet = new FileList();
        String root = new File(path).getAbsolutePath();
        visitEachFile(path,
                f -> bRead(f.getAbsolutePath()).oBytes().ifPresent($ -> toRet.put(f.getAbsolutePath().replace(root, ""), $)));
        return toRet;
    }

    public static LineReader sRead(String path) {
        return sRead(path, StandardCharsets.UTF_8);
    }

    public static LineReader sRead(String path, Charset charset) {
        return new LineReader(path, charset);
    }

    public static ByteReader bRead(String path) {
        return new ByteReader(path);
    }

    public static boolean exists(String path) {
        return path != null && new File(path).exists();
    }

    public static boolean isParent(String maybeParent, String maybeChild) {
        return maybeChild.contains(St.endWith(maybeParent, "/"));
    }


    public static boolean isResourceExists(String fullFileName) {
        return getResourceStream(fullFileName).map($ -> {
            Ex.run($::close);
            return true;
        }).orElse(false);
    }

    public static O<String> getResource(String resource) {
        return getResourceStream(resource).flatMap($ -> {
            try {
                return ofNullable(St.streamToS($, "UTF-8"));
            } catch (Exception e) {
                return empty();
            }
        });
    }

    public static O<byte[]> getResourceBytes(String resource) {
        return getResourceStream(resource).flatMap($ -> {
            try {
                return ofNullable(streamToBytes($));
            } catch (Exception e) {
                return empty();
            }
        });
    }

    public static O<InputStream> getResourceStream(String resource) {
        if (resource.charAt(0) == '/') {
            resource = resource.substring(1);
        }
        return O.ofNullable(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource));
    }

    public static void visitEachFile(String fileOrFolder, C1<File> onFile) {
        visitEachFileWithFinish(fileOrFolder, file -> {
            onFile.accept(file);
            return true;
        });
    }

    public static void visitEachFileWithFinish(String fileOrFolder, P1<File> onFileWithContinueCheck) {
        Path path = Paths.get(fileOrFolder);
        if (!isDirectory(path)) {
            onFileWithContinueCheck.test(path.toFile());
        } else {
            try {
                walkFileTree(path, new SimpleFileVisitor<Path>() {
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        final boolean continueOrFinish = onFileWithContinueCheck.test(new File(file.toString()));
                        return continueOrFinish ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
                    }
                });
            } catch (IOException e) {
                Ex.thRow(e);
            }
        }
    }

    public static O<File> getLastFolderOf(String path) {
        return O.ofNullable(path)
                .map(File::new)
                .map($ -> $.isDirectory() ? $ : $.getParentFile());
    }

    @AllArgsConstructor
    public static class LineReader {
        String path;
        Charset charset;

        public <A> A lineStreamMap(F1<OneOf<Stream<String>, Exception>, A> f) {
            try {
                try (Stream<String> s = Files.lines(Paths.get(path), charset)) {
                    return f.apply(OneOf.left(s));
                }
            } catch (IOException e) {
                return f.apply(OneOf.right(e));
            }
        }

        public void lineStream(C1<OneOf<Stream<String>, Exception>> consumer) {
            lineStreamMap(either -> {
                consumer.accept(either);
                return null;
            });
        }

        public O<List<String>> oLines() {
            return lineStreamMap(either -> either.map($ -> $.collect(Cc.toL()), $ -> $)).oLeft();
        }

        public List<String> lines() {
            return oLines().get();
        }

        public O<String> oString() {
            return oLines().map($ -> Cc.join(System.lineSeparator(), $));
        }

        public String string() {
            return oString().get();
        }
    }

    @AllArgsConstructor
    public static class ByteReader {
        String path;

        public O<byte[]> oBytes() {
            try {
                return of(readAllBytes(new File(path).toPath()));
            } catch (IOException e) {
                return empty();
            }
        }

        public byte[] bytes() {
            return oBytes().get();
        }
    }
    //endregion

    //region Write
    public static void addWrite(String path, C1<PlainWriter> writer) {
        addWrite(path, "UTF-8", writer);
    }

    public static void addWrite(String path, String charSet, C1<PlainWriter> writer) {
        withCreatedPath(path, $ -> {
            try (BufferedWriter bwr = newBufferedWriter($, Charset.forName(charSet), WRITE, CREATE, APPEND)) {
                writer.accept(new PlainWriter(bwr));
            } catch (Exception ignored) {
            }
        });
    }

    public static void reWrite(String path, C1<PlainWriter> writer) {
        reWrite(path, "UTF-8", writer);
    }

    public static void reWrite(String path, String charSet, C1<PlainWriter> writer) {
        withCreatedPath(path, $ -> {
            try (BufferedWriter bwr = newBufferedWriter($, Charset.forName(charSet), WRITE, CREATE,
                    TRUNCATE_EXISTING)) {
                writer.accept(new PlainWriter(bwr));
            } catch (Exception ignored) {
            }
        });

    }

    public static void addWriteBin(String path, C1<BinaryWriter> writer) {
        withCreatedPath(path, $ -> {
            try (OutputStream output = newOutputStream(Paths.get(path), WRITE, CREATE, APPEND)) {
                writer.accept(new BinaryWriter(output));
            } catch (Exception ignored) {
            }
        });
    }

    public static void reWriteBin(String path, C1<BinaryWriter> writer) {
        withCreatedPath(path, $ -> {
            try (OutputStream output = newOutputStream($, WRITE, CREATE, TRUNCATE_EXISTING)) {
                writer.accept(new BinaryWriter(output));
            } catch (Exception ignored) {
            }
        });
    }

    public static boolean deleteIfExists(String location) {
        Path path = Paths.get(location);
        File f = new File(location);
        if (!f.exists()) {
            return false;
        }
        try {
            if (!isDirectory(path)) {
                Files.delete(path);
            } else {
                walkFileTree(path, new SimpleFileVisitor<Path>() {
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            return true;
        } catch (IOException e) {
            return Ex.thRow(e);
        }
    }

    public static boolean copy(String from, String to) {
        return copy(from, to, false);
    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public static boolean copy(String from, String to, boolean copyAttributes) {
        try {
            new File(to).getParentFile().mkdirs();
            return copyAttributes
                   ? Files.copy(new File(from).toPath(), new File(to).toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES) != null
                   : Files.copy(new File(from).toPath(), new File(to).toPath(),
                           StandardCopyOption.REPLACE_EXISTING) != null
                    ;
        } catch (IOException e) {
            return false;
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean move(String from, String to) {
        try {
            return Files.move(new File(from).toPath(), new File(to).toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE) != null;
        } catch (IOException e) {
            return false;
        }
    }


    @AllArgsConstructor
    public static class PlainWriter {
        BufferedWriter bw;

        public PlainWriter append(String text) {
            try {
                bw.append(text);
            } catch (IOException e) {
                return Ex.thRow(e);
            }
            return this;
        }

        public PlainWriter appendLine(String text) {
            try {
                bw.append(text).append(System.lineSeparator());
            } catch (IOException e) {
                return Ex.thRow(e);
            }
            return this;
        }
    }

    @AllArgsConstructor
    public static class BinaryWriter {
        OutputStream bw;

        public void append(byte[] bytes) {
            try {
                bw.write(bytes);
            } catch (IOException e) {
                Ex.thRow(e);
            }
        }
    }
    //endregion

    //region Serialize/Deserialize
    public static void serialize(String resultingFile, Object data) {
        try {
            try (FileOutputStream fos = new FileOutputStream(resultingFile, false);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(data);
            }
        } catch (IOException e) {
            Ex.thRow(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deSerialize(String readFrom, Class<T> cls) {
        try (FileInputStream fos = new FileInputStream(readFrom);
             ObjectInputStream oos = new ObjectInputStream(fos)) {
            return (T) oos.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return Ex.thRow(e);
        }
    }

    public static Map<String, Object> deSerializeParallel(String... files) {
        return deSerializeParallel(Object.class, files);
    }

    public static <T> Map<String, T> deSerializeParallel(Class<T> cls, String... files) {
        return Stream.of(files).parallel().collect(toMap(file -> file, file -> Io.deSerialize(file, cls)));
    }

    public static Object deSerialize(String readFrom) {
        return deSerialize(readFrom, Object.class);
    }
    //endregion

    public static ExecuteInfo execute(String command) {
        ProcessBuilder ps = new ProcessBuilder(command.split("\\s+"));
        ps.redirectErrorStream(true);

        try {
            Process pr = ps.start();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
                int endState = pr.waitFor();
                return new ExecuteInfo(Cc.l(command), endState, sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "");
            }
        } catch (IOException | InterruptedException e) {
            return Ex.thRow(e);
        }
    }

    @Data
    @AllArgsConstructor
    public static class ExecuteInfo {
        List<String> commands;
        int code;
        String output;
    }

    private static void withCreatedPath(String path, C1<Path> r) {
        File file = new File(path);
        //noinspection ResultOfMethodCallIgnored
        file.getAbsoluteFile().getParentFile().mkdirs();
        r.accept(file.toPath());
    }

    private Io() {
    }
}
