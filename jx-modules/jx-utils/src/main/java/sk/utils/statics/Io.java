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
import lombok.SneakyThrows;
import sk.utils.files.FileList;
import sk.utils.functional.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;
import static sk.utils.functional.O.*;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class Io/*Input/Output*/ {
    public static void endlessReadFromKeyboard(String endSequence, C1<String> consumer) {
        final String finalSequence = endSequence.trim();
        endlessReadFromKeyboard(s -> {
            if (Fu.equal(s, finalSequence)) {
                return false;
            } else {
                consumer.accept(s);
            }
            return true;
        });
    }

    public static void endlessReadFromKeyboard(F1<String, Boolean> consumer) {
        try (Scanner in = new Scanner(System.in)) {
            while (true) {
                String s = in.nextLine().trim();
                final boolean continuE = consumer.apply(s);
                if (!continuE) {
                    break;
                }
            }
        }
    }

    public static String getNextLineFromKeyboard() {
        try (Scanner in = new Scanner(System.in)) {
            return in.nextLine().trim();
        }
    }

    public static boolean isWWWAvailable() {
        return isWWWAvailable(5000);
    }

    public static boolean isWWWAvailable(int millis) {
        try {
            URL url = new URL("https://google.com");
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(millis);
            urlConnection.connect();
            urlConnection.getInputStream().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String changePortForUrl(String url, int newPort) {
        String urlWithPort = St.subRF(St.subLF(url, "://"), "/");
        String template = url.replace(urlWithPort, "%s");
        return urlWithPort.contains(":")
               ? template.formatted(St.subRF(urlWithPort, ":") + ":" + newPort)
               : template.formatted(urlWithPort + ":" + newPort);
    }

    //region URI
    public static String getFileFromUri(URI uri) throws RuntimeException {
        String locator = uri.toString();
        if (!locator.contains("file:") && !locator.startsWith("/")) {
            throw new RuntimeException("Can't work with non file URI's: " + locator);
        }

        return St.sub(locator, "file:", "!").get();
    }

    public static boolean isJarUri(URI uri) throws RuntimeException {
        String path = uri.toString();
        return path.startsWith("jar:file:") || path.contains(".jar");
    }

    public static String getJarContextPathFromUri(URI uri) {
        String path = uri.toString();
        return !path.contains("!") ? "" : St.notStartWith(St.subLL(path, "!"), "/");
    }
    //endregion


    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @SneakyThrows
    public static AtomicInteger getFreePort(AtomicInteger portStorage) {
        Objects.requireNonNull(portStorage);
        int port = portStorage.get();
        if (port == 0) {
            synchronized (portStorage) {
                port = portStorage.get();
                if (port == 0) {
                    try (ServerSocket tempSocket = new ServerSocket(0)) {
                        portStorage.set(tempSocket.getLocalPort());
                    }
                }
            }
        }
        return portStorage;
    }

    //region Input/Output streams. StreamPump
    public static byte[] streamToBytes(InputStream is) {
        return streamPump(is);
    }

    public static byte[] streamPump(InputStream is) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            streamPump(is, os, 8 * 1024, NONE);
            return os.toByteArray();
        } catch (IOException e) {
            return Ex.thRow(e);
        }
    }

    @SneakyThrows
    public static void streamPump(InputStream in, OutputStream out, int bufferSize, StreamPumpInterceptor also) {
        byte[] read_buf = new byte[bufferSize];
        int read_len;
        try (out; in) {
            while ((read_len = in.read(read_buf)) > 0) {
                out.write(read_buf, 0, read_len);
                also.intercept(read_buf, read_len);
            }
        }
    }

    @SneakyThrows
    public static long streamPumpLength(InputStream in, OutputStream out, int bufferSize, StreamPumpInterceptor also) {
        long[] fullLength = new long[1];
        streamPump(in, out, bufferSize, also.andThen((buffer, size) -> fullLength[0] += size));
        return fullLength[0];
    }

    public static InputStream bytesToStream(byte[] in) {
        return new ByteArrayInputStream(in);
    }

    public static StreamPumpInterceptor NONE = (buffer, size) -> {};

    public static OutputStream NONE() {
        return OutputStream.nullOutputStream();
    }

    @FunctionalInterface
    public interface StreamPumpInterceptor {
        public void intercept(byte[] buffer, int size);

        public default StreamPumpInterceptor andThen(StreamPumpInterceptor after) {
            return (buffer, size) -> {
                intercept(buffer, size);
                after.intercept(buffer, size);
            };
        }
    }
    //endregion

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

    public static LineReader sRead(File file) {
        return sRead(file.getAbsolutePath());
    }

    public static LineReader sRead(File file, Charset charset) {
        return new LineReader(file.getAbsolutePath(), charset);
    }

    public static ByteReader bRead(File file) {
        return new ByteReader(file.getAbsolutePath());
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
                return ofNullable(streamPump($));
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

    public static O<URI> getResourceUri(String resource) {
        try {
            return O.ofNull(Thread.currentThread().getContextClassLoader().getResource(resource).toURI());
        } catch (Exception e) {
            return empty();
        }
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

        public <A> A lineStreamNoExc(F1<Stream<String>, A> consumer) {
            return lineStreamMap(either -> consumer.apply(either.left()));
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

        public O<InputStream> oIs() {
            try {
                return of(new BufferedInputStream(newInputStream(new File(path).toPath())));
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    public static void addWriteBin(String path, C1<BinaryWriter> writer) {
        withCreatedPath(path, $ -> {
            try (OutputStream output = newOutputStream(Paths.get(path), WRITE, CREATE, APPEND)) {
                writer.accept(new BinaryWriter(output));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void reWriteBin(String path, C1<BinaryWriter> writer) {
        withCreatedPath(path, $ -> {
            try (OutputStream output = newOutputStream($, WRITE, CREATE, TRUNCATE_EXISTING)) {
                writer.accept(new BinaryWriter(output));
            } catch (Exception e) {
                throw new RuntimeException(e);
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
        File source = new File(from);
        if (source.isDirectory()) {
            return copyDirectory(from, to, copyAttributes);
        } else {
            return copyFile(from, to, copyAttributes);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean move(String from, String to) {
        return move(from, to, false);
    }

    public static boolean move(String from, String to, boolean moveAttributes) {
        Path sourcePath = Paths.get(from);
        Path targetPath = Paths.get(to).toAbsolutePath();

        try {
            if (Files.isDirectory(sourcePath)) {
                walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        moveFile(file, targetPath.resolve(sourcePath.relativize(file)), moveAttributes);
                        return FileVisitResult.CONTINUE;
                    }

                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                moveFile(sourcePath, targetPath, moveAttributes);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void moveFile(Path source, Path target, boolean moveAttributes) {
        try {
            // Ensure the parent directory of the target file exists
            Files.createDirectories(target.getParent());

            CopyOption[] co = moveAttributes
                              ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE,
                                                 StandardCopyOption.COPY_ATTRIBUTES}
                              : new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE};
            Files.move(source, target, co);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean copyFile(String from, String to, boolean copyAttributes) {
        try {
            new File(to).getParentFile().mkdirs(); // Ensure the parent directory exists
            CopyOption[] co = copyAttributes
                              ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES}
                              : new CopyOption[]{StandardCopyOption.REPLACE_EXISTING};
            Files.copy(new File(from).toPath(), new File(to).toPath(), co);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean copyDirectory(String from, String to, boolean copyAttributes) {
        File sourceDir = new File(from);
        File targetDir = new File(to);

        // Create target directory if it doesn't exist
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        // Copy each file/directory recursively
        String[] files = sourceDir.list();
        if (files != null) {
            for (String file : files) {
                File srcFile = new File(sourceDir, file);
                File destFile = new File(targetDir, file);

                if (srcFile.isDirectory()) {
                    // Recursively copy the directory
                    copyDirectory(srcFile.getAbsolutePath(), destFile.getAbsolutePath(), copyAttributes);
                } else {
                    // Copy the file
                    copyFile(srcFile.getAbsolutePath(), destFile.getAbsolutePath(), copyAttributes);
                }
            }
        }
        return true; // Return true if everything was copied successfully
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

        public synchronized PlainWriter appendThreadSafe(String text) {
            return append(text);
        }

        public synchronized PlainWriter appendLineThreadSafe(String text) {
            return appendLine(text);
        }
    }

    @AllArgsConstructor
    public static class BinaryWriter {
        OutputStream bw;

        public BinaryWriter append(byte[] bytes) {
            try {
                bw.write(bytes);
            } catch (IOException e) {
                Ex.thRow(e);
            }
            return this;
        }

        public synchronized BinaryWriter appendThreadSafe(byte[] bytes) {
            return append(bytes);
        }
    }
    //endregion

    //region Serialize/Deserialize
    public static void serialize(OutputStream os, Object data) {
        try {
            try (os;
                 ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeObject(data);
            }
        } catch (IOException e) {
            Ex.thRow(e);
        }
    }

    public static void serialize(File f, Object data) {
        try {
            serialize(new FileOutputStream(f), data);
        } catch (FileNotFoundException e) {
            Ex.thRow(e);
        }
    }

    public static byte[] serialize(Object data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serialize(baos, data);
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static <T> T deSerialize(InputStream readFrom, Class<T> cls) {
        try (readFrom;
             ObjectInputStream oos = new ObjectInputStream(readFrom)) {
            return (T) oos.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return Ex.thRow(e);
        }
    }

    public static <T> T deSerialize(File file, Class<T> cls) {
        try {
            return deSerialize(new FileInputStream(file), cls);
        } catch (FileNotFoundException e) {
            return Ex.thRow(e);
        }
    }

    public static <T> T deSerialize(byte[] bytes, Class<T> cls) {
        return deSerialize(new ByteArrayInputStream(bytes), cls);
    }
    //endregion

    //region Execute script + services
    public static ExecuteInfo executeAndFail(String command) {
        return execute(command).failIfNotOk();
    }

    public static ExecuteInfo execute(String command) {
        ProcessBuilder ps = command.contains("|")
                            ? new ProcessBuilder("/bin/sh", "-c", command)
                            : new ProcessBuilder(command.split("\\s+"));
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


    public static boolean serviceStop(String service) {
        execute("sudo service %s stop".formatted(service));
        return Io.serviceStatus(service) == ServiceStatus.INACTIVE;
    }

    public static boolean serviceStart(String service) {
        execute("sudo service %s start".formatted(service));
        return Io.serviceStatus(service) == ServiceStatus.ACTIVE;
    }

    public static boolean serviceRestart(String service) {
        execute("sudo service %s restart".formatted(service));
        return Io.serviceStatus(service) == ServiceStatus.ACTIVE;
    }

    private static final Pattern SERVICE_ACTIVITY_PATTERN = Pattern.compile("Active: (.*?) ");//Active: active (...) ....

    public static ServiceStatus serviceStatus(String service) {
        service = service.trim();
        if (service.contains(" ")) {
            throw new RuntimeException("Can't invoke service: '%s'" + service);
        }
        final ExecuteInfo result = execute("sudo service %s status".formatted(service));
        final List<String> rr = St.matchAllFirst(result.getOutput(), SERVICE_ACTIVITY_PATTERN);
        if (rr.size() == 0) {
            return ServiceStatus.NO_STATUS;
        } else {
            final String s = Cc.first(rr).get().trim();
            return Re.findInEnum(ServiceStatus.class, s.toUpperCase()).orElse(ServiceStatus.OTHER);
        }
    }

    @Data
    @AllArgsConstructor
    public static class ExecuteInfo {
        List<String> commands;
        int code;
        String output;

        public ExecuteInfo failIfNotOk() {
            if (code != 0) {
                throw new RuntimeException(
                        "ERROR executing:'%s' OUTPUT:'%s'".formatted(Cc.join(commands), output)
                );
            }
            return this;
        }
    }

    public enum ServiceStatus {ACTIVE, INACTIVE, OTHER, NO_STATUS}
    //endregion

    private static void withCreatedPath(String path, C1<Path> r) {
        File file = new File(path);
        //noinspection ResultOfMethodCallIgnored
        file.getAbsoluteFile().getParentFile().mkdirs();
        r.accept(file.toPath());
    }


    private Io() {}
}
