public final class BitPacking {

    public enum Mode { OVERLAP, NO_OVERLAP } // nous implémenterons le NO_OVERLAP plus tard

    // Factory
    public static BitPacking of(Mode mode) {
        return new BitPacking(mode);
    }

    private final Mode mode;
    private int n;
    private int k;
    private int[] words;  // stockage compressé

    private BitPacking(Mode mode) { this.mode = mode; }


    public void compress(int[] src) {
        if (src == null) throw new IllegalArgumentException("src null"); // cas null
        this.n = src.length;
        this.k = computeK(src);
        if (n == 0) { this.words = new int[0]; return; }

        long totalBits = (long) n * k;
        int wordCount = (int) ((totalBits + 31) >>> 5);
        if (wordCount == 0) wordCount = 1; // au moins un mot
        this.words = new int[wordCount];

        if (mode == Mode.OVERLAP) {
            // écriture avec chevauchement
            packOverlap(src);
        } else {
            // écriture sans chevauchement
            packNoOverlap(src);
        }
    }

    public int get(int i) {
        if (i < 0 || i >= n) throw new IndexOutOfBoundsException("i=" + i);
        if (mode == Mode.OVERLAP) {
            int startBit  = i * k;
            int wordIndex = startBit >>> 5;   // /32
            int bitOffset = startBit & 31;    // %32

            if (bitOffset + k <= 32) {
                return (words[wordIndex] >>> bitOffset) & mask(k);
            } else {
                int bitsInFirst  = 32 - bitOffset;
                int bitsInSecond = k - bitsInFirst;

                int low  = (words[wordIndex]   >>> bitOffset) & mask(bitsInFirst);
                int high =  words[wordIndex+1] & mask(bitsInSecond);
                return (high << bitsInFirst) | low;
            }
        } else {
            int vpw = 32 / k;
            if (vpw <= 0) throw new IllegalStateException("k > 32 non supporté");
            int wordIndex = i / vpw;
            int bitOffset = (i % vpw) * k;
            return (words[wordIndex] >>> bitOffset) & mask(k);
        }
    }

    public void decompressInto(int[] dst) {
        if (dst == null || dst.length != n) throw new IllegalArgumentException("dst size");
        for (int i = 0; i < n; i++) dst[i] = get(i);
    }

    public int size() { return n; }
    public int bitsPerValue() { return k; }


    // gestion des masques
    private static int mask(int bits) {
        return (bits == 32) ? -1 : ((1 << bits) - 1);
    }

    private static int computeK(int[] src) {
        int max = 0;
        for (int v : src) {
            if (v < 0) throw new IllegalArgumentException("Négatifs = bonus (Jour 4)");
            if (v > max) max = v;
        }
        int bits = 0;
        while (max > 0) { bits++; max >>>= 1; }
        return (bits == 0) ? 1 : bits;
    }

    // écriture avec chevauchement
    private void packOverlap(int[] src) {
        int bitPtr = 0; // index de bit dans le flux compressé
        for (int v : src) {
            int val = v & mask(k);
            int wi = bitPtr >>> 5; // mot courant
            int bo = bitPtr & 31;  // offset dans le mot

            if (bo + k <= 32) {
                words[wi] |= (val << bo);
            } else {
                int b1 = 32 - bo;      // bits dans le premier mot
                int b2 = k - b1;       // bits dans le deuxieme mot
                words[wi]     |= (val & mask(b1)) << bo;
                words[wi + 1] |= (val >>> b1) & mask(b2);
            }
            bitPtr += k;
        }
    }
    // écriture sans chevauchement
    private void packNoOverlap(int[] src) {
        int vpw = 32 / k;                       // values per word
        if (vpw <= 0) throw new IllegalStateException("k > 32 non supporté");

        int wordCount = (n + vpw - 1) / vpw;    // ceil(n / vpw)
        this.words = new int[wordCount];

        for (int i = 0; i < n; i++) {
            int wordIndex = i / vpw;
            int bitOffset = (i % vpw) * k;
            int val = src[i] & mask(k);
            words[wordIndex] |= (val << bitOffset); // tient toujours dans 1 mot
        }
    }
}


