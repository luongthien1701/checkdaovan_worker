package util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MinHash implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final long PRIME = 2147483647L;
    private final int numHashFunctions;
    private final List<HashFn> hashFunctions = new ArrayList<>();

    public MinHash(int numHashFunctions) {
        this.numHashFunctions = numHashFunctions;
        Random random = new Random(2024);
        for (int i = 0; i < numHashFunctions; i++) {
            long a = Math.abs(random.nextLong()) % PRIME;
            long b = Math.abs(random.nextLong()) % PRIME;
            hashFunctions.add(new HashFn(a, b));
        }
    }

    public int[] signature(Set<String> shingles) {
        int[] sig = new int[numHashFunctions];
        Arrays.fill(sig, Integer.MAX_VALUE);
        for (String shingle : shingles) {
            int hash = shingle.hashCode();
            for (int i = 0; i < numHashFunctions; i++) {
                int value = hashFunctions.get(i).hash(hash);
                if (value < sig[i]) {
                    sig[i] = value;
                }
            }
        }
        return sig;
    }

    public double similarity(int[] a, int[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Signature lengths mismatch");
        }
        int matches = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == b[i]) {
                matches++;
            }
        }
        return (double) matches / a.length;
    }

    private static class HashFn implements Serializable {
        private static final long serialVersionUID = 1L;
        private final long a;
        private final long b;

        HashFn(long a, long b) {
            this.a = a;
            this.b = b;
        }

        int hash(int value) {
            long result = (a * value + b) % PRIME;
            if (result < 0) {
                result += PRIME;
            }
            return (int) result;
        }
    }
}

