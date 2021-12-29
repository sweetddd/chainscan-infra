package ai.everylink.chainscan.watcher.plugin.util;

import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.crypto.digests.Blake2bDigest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

public class UtilsCrypto {

    /**
     * Creates a blake2b u8a from the input.
     * From a `Uint8Array` input, create the blake2b and return the result as a u8a with the specified `bitLength`.
     * **example**
     * <p>
     * ```java
     * blake2AsU8a("abc"); // => [0xba, 0x80, 0xa53, 0xf98, 0x1c, 0x4d, 0x0d]
     * ```
     */

    public static byte[] blake2AsU8a(byte[] data, int bitLength, byte[] key) {
        int byteLength = (int) Math.ceil(bitLength / 8F);
        Blake2bDigest blake2bkeyed = new Blake2bDigest(key, byteLength, null, null);
        blake2bkeyed.reset();
        blake2bkeyed.update(data, 0, data.length);
        byte[] keyedHash = new byte[64];
        int digestLength = blake2bkeyed.doFinal(keyedHash, 0);
        return ArrayUtils.subarray(keyedHash, 0, digestLength);
    }

    public static byte[] blake2AsU8a(byte[] data) {
        return blake2AsU8a(data, 256, null);
    }

    public static byte[] blake2AsU8a(byte[] data, int bitLength) {
        return blake2AsU8a(data, bitLength, null);
    }


    /**
     * Creates a xxhash64 u8a from the input.
     * From either a `string`, `Uint8Array` or a `Buffer` input, create the xxhash64 and return the result as a `Uint8Array` with the specified `bitLength`.
     * **example**
     * <p>
     * ```java
     * xxhashAsU8a("abc"); // => 0x44bc2cf5ad770999
     * ```
     */
    public static byte[] xxhashAsU8a(byte[] data) {
        return xxhashAsU8a(data, 64);
    }

    public static byte[] xxhashAsU8a(byte[] data, int bitLength) {
        int iterations = (int) Math.ceil(bitLength / 64F);
        byte[] u8a = new byte[(int) Math.ceil(bitLength / 8F)];
        for (int seed = 0; seed < iterations; seed++) {
            BigInteger bigInteger = xxhash64AsBn(data, seed);
            byte[] bytes = bnToU8a(bigInteger, true, 64);
            System.arraycopy(bytes, 0, u8a, seed * 8, 8);
        }
        return u8a;
    }

    public static byte[] bnToU8a(BigInteger value, boolean isLe, int bitLength) {
        return bnToU8a(value, isLe, false, bitLength);
    }

    public static BigInteger bnToBn(Object value) {
        if (value == null) {
            return BigInteger.ZERO;
        }
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        } else if (value instanceof Number) {
            return new BigInteger(value.toString());
        } else if (value instanceof String) {
            return new BigInteger((String) value, 16);
        }
        throw new RuntimeException(" bnToBn " + value);
    }

    public static byte[] bnToU8a(BigInteger value, boolean isLe, boolean isNegative, int bitLength) {
        BigInteger valueBn = bnToBn(value);
        int byteLength;
        if (bitLength == -1) {
            byteLength = (int) Math.ceil(valueBn.bitLength() / 8f);
        } else {
            byteLength = (int) Math.ceil(bitLength / 8f);
        }

        if (value == null) {
            if (bitLength == -1) {
                return new byte[0];
            } else {
                return new byte[byteLength];
            }
        }

        byte[] output = new byte[byteLength];

        if (isNegative) {
            //TODO  valueBn.negate()
        }

        if (isLe) {
            byte[] bytes = toByteArrayLittleEndianUnsigned(valueBn);
            System.arraycopy(bytes, 0, output, 0, bytes.length);
        } else {
            byte[] bytes = valueBn.toByteArray();
            System.arraycopy(bytes, 0, output, output.length - bytes.length, bytes.length);
        }
        return output;

    }

    public static byte[] toByteArrayLittleEndianUnsigned(BigInteger bi) {
        byte[] extractedBytes = toByteArrayUnsigned(bi);
        ArrayUtils.reverse(extractedBytes);
        return extractedBytes;
    }

    public static byte[] toByteArrayUnsigned(BigInteger bi) {
        byte[] extractedBytes = bi.toByteArray();
        int skipped = 0;
        boolean skip = true;
        for (byte b : extractedBytes) {
            boolean signByte = b == (byte) 0x00;
            if (skip && signByte) {
                skipped++;
                continue;
            } else if (skip) {
                skip = false;
            }
        }
        extractedBytes = Arrays.copyOfRange(extractedBytes, skipped,
                extractedBytes.length);
        return extractedBytes;
    }



    /**
     * Creates a xxhash BN from the input.
     * From either a `string`, `Uint8Array` or a `Buffer` input, create the xxhash and return the result as a BN.
     * **example**
     * <p>
     * ```java
     * xxhash64AsBn("abcd", 0xabcd)); // => new BN(0xe29f70f8b8c96df7)
     * ```
     */
    public static BigInteger xxhash64AsBn(byte[] data, long seed) {
        String hash = xxhash64AsRaw(data, seed);
        return new BigInteger(hash, 16);
    }

    /**
     * Creates a xxhash non-prefixed hex from the input.
     * From either a `string`, `Uint8Array` or a `Buffer` input, create the xxhash and return the result as a non-prefixed hex string.
     * **example**
     * <p>
     * ```java
     * xxhash64AsRaw("abcd", 0xabcd)); // => e29f70f8b8c96df7
     * ```
     */
    public static String xxhash64AsRaw(byte[] data, long seed) {
        return Long.toHexString(xxhash64AsValue(data, seed));
    }


    /**
     * Creates a hex number from the input.
     * From either a `string`, `Uint8Array` or a `Buffer` input, create the xxhash and return the result as a hex number
     * **example**
     * <p>
     * ```java
     * xxhash64AsValue("abcd", 0xabcd)); // => e29f70f8b8c96df7
     * ```
     */
    public static long xxhash64AsValue(byte[] data, long seed) {

        XXHashFactory factory = XXHashFactory.fastestInstance();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        // value you want, but always the same
        StreamingXXHash64 xxHash64 = factory.newStreamingHash64(seed);

        byte[] buf = new byte[16]; // for real-world usage, use a larger buffer, like 8192 bytes
        for (; ; ) {
            int read = 0;
            try {
                read = in.read(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (read == -1) {
                break;
            }
            xxHash64.update(buf, 0, read);
        }
        long hash = xxHash64.getValue();
        return hash;
    }
}

