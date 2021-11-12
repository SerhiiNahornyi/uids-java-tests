import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import com.nixxcode.jvmbrotli.enc.Encoder;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

public class TestClass {
    public static void main(String[] args) {
        // start up functions
        BrotliLoader.isBrotliAvailable();
        makeJVMBurn();
        final Prebid.uidCollection uids = createUids();

        // Brotli tests
        measureCompression(() -> toBase64(toBrotliBytes(uids.toByteArray())), "brotli");

        // Gzip tests
        measureCompression(() -> toBase64(toGzipBytes(uids.toByteArray())), "gzip");
    }

    public static void makeJVMBurn() {
        String random = null;
        for (int i = 0; i < 10000; i++) {
            random = UUID.randomUUID().toString();
            toBrotliBytes(random.getBytes());
            toGzipBytes(random.getBytes());
            toBase64(createUids().toByteArray());
        }
        System.out.println(random);
    }

    public static void measureCompression(Supplier<byte[]> compressorFunction, String compressionType) {
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        final Timer timer = registry.timer("prebid.compression." + compressionType);
        final ByteWrapper compressedArray = new ByteWrapper();

        timer.record(() -> {
            compressedArray.byteArray = compressorFunction.get();
        });

        System.out.println(timer.getId().getName());
        System.out.println("size (bytes): " + compressedArray.byteArray.length);
        System.out.println("time (nanos): " + timer.totalTime(TimeUnit.NANOSECONDS));
    }

    @SneakyThrows
    private static byte[] toBrotliBytes(byte[] bytes) {
        Encoder.Parameters params = new Encoder.Parameters().setQuality(0);
        final ByteArrayOutputStream dst = new ByteArrayOutputStream();
        final BrotliOutputStream encoder = new BrotliOutputStream(dst, params);

        encoder.write(bytes);
        encoder.close();

        final byte[] brotliBytes = dst.toByteArray();
        dst.close();
        return brotliBytes;
    }

    private static byte[] toGzipBytes(byte[] bytes) {
        try (ByteArrayOutputStream obj = new ByteArrayOutputStream(); GZIPOutputStream gzip = new GZIPOutputStream(
                obj)) {
            gzip.write(bytes);
            gzip.finish();

            return obj.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Failed to compress request : %s", e.getMessage()));
        }
    }

    private static byte[] toBase64(byte[] bytes) {

        return Base64.getEncoder().encode(bytes);
    }

    private static Prebid.uidCollection createUids() {
        final List<Prebid.uidCollection.uidObject> uidObjects = new ArrayList<>();

        IntStream.range(1, 34).forEach(bidderId -> uidObjects.add(createUidObject(bidderId)));

        return Prebid.uidCollection.newBuilder()
                .addAllUids(uidObjects)
                .setExpirationBase(123)
                .build();
    }

    private static Prebid.uidCollection.uidObject createUidObject(int id) {
        final byte[] array = new byte[70];
        new Random().nextBytes(array);
        final String generatedString = new String(array, StandardCharsets.UTF_8);

        return Prebid.uidCollection.uidObject.newBuilder()
                .setBidder("bidder" + id)
                .setUid(generatedString)
                .setExpirationOffset(22)
                .build();
    }
}
