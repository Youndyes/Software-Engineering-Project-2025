public final class BitPacking {

    public enum Mode { OVERLAP, NO_OVERLAP, OVERFLOW } // nous implémenterons le NO_OVERLAP plus tard

    // Factory
    public static BitPacking of(Mode mode) {
        return new BitPacking(mode);
    }

    private final Mode mode;
    private int n;
    private int k;
    private int[] words;  // stockage compressé
    private int[] prefixOv;
    private int kPrime, kOverflow, nbOverflow, bitsIdx, overflowStartBit;

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
        } else if (mode == Mode.NO_OVERLAP) {
            // écriture sans chevauchement
            packNoOverlap(src);
        }
        else {
            // écriture avec overflow
            packOverflow(src);
        }
    }

    public int get(int i) {
        if (i < 0 || i >= n) throw new IndexOutOfBoundsException("i=" + i);

        if (mode == Mode.OVERLAP) {
            int startBit  = i * k;
            int wordIndex = startBit >>> 5;
            int bitOffset = startBit & 31;
            if (bitOffset + k <= 32) {
                return (words[wordIndex] >>> bitOffset) & mask(k);
            } else {
                int bitsInFirst  = 32 - bitOffset;
                int bitsInSecond = k - bitsInFirst;
                int low  = (words[wordIndex]   >>> bitOffset) & mask(bitsInFirst);
                int high =  words[wordIndex+1] & mask(bitsInSecond);
                return (high << bitsInFirst) | low;
            }
        } else if (mode == Mode.NO_OVERLAP) {
            int vpw = 32 / k;
            if (vpw <= 0) throw new IllegalStateException("k > 32 non supporté");
            int wordIndex = i / vpw;
            int bitOffset = (i % vpw) * k;
            return (words[wordIndex] >>> bitOffset) & mask(k);
        } else { // OVERFLOW
            // nb d'overflow avant i
            int ovBefore    = prefixOv[i];
            int smallBefore = i - ovBefore;
            int bitsBefore  = i  + smallBefore * kPrime + ovBefore * bitsIdx;

            // lire 1 bit de flag à offset bitsBefore
            int wi = bitsBefore >>> 5, bo = bitsBefore & 31;
            int flag;
            if (bo + 1 <= 32) flag = (words[wi] >>> bo) & 1;
            else flag = ((words[wi] >>> bo) & 1) | ((words[wi+1] & 1) << (32 - bo)); // (théorique)

            if (flag == 0) {
                // lire kPrime bits à bitsBefore+1
                int off = bitsBefore + 1;
                wi = off >>> 5; bo = off & 31;
                if (bo + kPrime <= 32) {
                    return (words[wi] >>> bo) & ((kPrime == 32) ? -1 : ((1 << kPrime) - 1));
                } else {
                    int b1 = 32 - bo, b2 = kPrime - b1;
                    int low  = (words[wi]   >>> bo) & ((1 << b1) - 1);
                    int high =  words[wi+1]         & ((1 << b2) - 1);
                    return (high << b1) | low;
                }
            } else {
                // lire index sur bitsIdx, puis aller lire la valeur dans la zone overflow
                int idx = 0;
                int off = bitsBefore + 1;
                if (bitsIdx > 0) {
                    wi = off >>> 5; bo = off & 31;
                    if (bo + bitsIdx <= 32) {
                        idx = (words[wi] >>> bo) & ((1 << bitsIdx) - 1);
                    } else {
                        int b1 = 32 - bo, b2 = bitsIdx - b1;
                        int low  = (words[wi]   >>> bo) & ((1 << b1) - 1);
                        int high =  words[wi+1]         & ((1 << b2) - 1);
                        idx = (high << b1) | low;
                    }
                }
                int offOv = overflowStartBit + idx * kOverflow;
                wi = offOv >>> 5; bo = offOv & 31;
                if (bo + kOverflow <= 32) {
                    return (words[wi] >>> bo) & ((kOverflow == 32) ? -1 : ((1 << kOverflow) - 1));
                } else {
                    int b1 = 32 - bo, b2 = kOverflow - b1;
                    int low  = (words[wi]   >>> bo) & ((1 << b1) - 1);
                    int high =  words[wi+1]         & ((1 << b2) - 1);
                    return (high << b1) | low;
                }
            }
        }
    }


    public void decompress(int[] dst) {
        if (dst == null || dst.length != n) throw new IllegalArgumentException("dst size");
        for (int i = 0; i < n; i++) dst[i] = get(i);
    }

    public int size() { return n; }


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

    // écriture avec débordement
    private void packOverflow(int[] src) {

        // calcul bit par valeur
        int[] bl = new int[n];
        int kMax = 1;
        for (int i = 0; i < n; i++) {
            int v = src[i];
            if (v < 0) throw new IllegalArgumentException("Négatifs = bonus");
            int bits = (v == 0) ? 1 : (32 - Integer.numberOfLeadingZeros(v));
            bl[i] = bits;
            if (bits > kMax) kMax = bits;
        }

        // recherches des meilleurs paramètres
        long bestTotal = Long.MAX_VALUE;
        int bestKp = 1, bestNbOv = 0, bestKov = 0, bestBitsIdx = 0;
        for (int kp = 1; kp <= kMax; kp++) {
            int nbOv = 0, kov = 0;
            for (int i = 0; i < n; i++) {
                if (bl[i] > kp) {
                    nbOv++;
                    if (bl[i] > kov) kov = bl[i];
                }
            }
            int bIdx = (nbOv <= 1) ? 0 : (32 - Integer.numberOfLeadingZeros(nbOv - 1));
            long mainBits = (long) n /*flags*/ + (long)(n - nbOv) * kp + (long) nbOv * bIdx;
            long ovBits   = (long) nbOv * kov;
            long total    = mainBits + ovBits;
            if (total < bestTotal) {
                bestTotal = total;
                bestKp = kp; bestNbOv = nbOv; bestKov = kov; bestBitsIdx = bIdx;
            }
        }
        this.kPrime = bestKp;
        this.nbOverflow = bestNbOv;
        this.kOverflow = bestKov;
        this.bitsIdx = bestBitsIdx;

        // marquer les overflows
        boolean[] isOv = new boolean[n];
        this.prefixOv = new int[n + 1];
        for (int i = 0; i < n; i++) {
            isOv[i] = (bl[i] > kPrime);
            prefixOv[i + 1] = prefixOv[i] + (isOv[i] ? 1 : 0);
        }

        // allocution du tab de stockage
        long mainBits = (long) n + (long)(n - nbOverflow) * kPrime + (long) nbOverflow * bitsIdx;
        long ovBits   = (long) nbOverflow * kOverflow;
        long totalBits = mainBits + ovBits;
        int wordCount = (int) ((totalBits + 31) >>> 5);
        if (wordCount == 0) wordCount = 1;
        this.words = new int[wordCount];

        // ecriture
        int bitPtr = 0;
        for (int i = 0; i < n; i++) {
            // écrire flag (1 bit)
            int wi = bitPtr >>> 5, bo = bitPtr & 31;
            if (bo + 1 <= 32) words[wi] |= (1 & 1) << bo;
            bitPtr += 1;

            if (!isOv[i]) {
                words[wi] &= ~(1 << bo);

                int val = src[i] & ((kPrime == 32) ? -1 : ((1 << kPrime) - 1));
                wi = bitPtr >>> 5; bo = bitPtr & 31;
                if (bo + kPrime <= 32) {
                    words[wi] |= val << bo;
                } else {
                    int b1 = 32 - bo, b2 = kPrime - b1;
                    words[wi]     |= (val & ((1 << b1) - 1)) << bo;
                    words[wi + 1] |= (val >>> b1) & ((1 << b2) - 1);
                }
                bitPtr += kPrime;
            } else {
                int idx = prefixOv[i];
                if (bitsIdx > 0) {
                    wi = bitPtr >>> 5; bo = bitPtr & 31;
                    if (bo + bitsIdx <= 32) {
                        words[wi] |= (idx & ((1 << bitsIdx) - 1)) << bo;
                    } else {
                        int b1 = 32 - bo, b2 = bitsIdx - b1;
                        words[wi]     |= (idx & ((1 << b1) - 1)) << bo;
                        words[wi + 1] |= (idx >>> b1) & ((1 << b2) - 1);
                    }
                }
                bitPtr += bitsIdx;
            }
        }

        // debut zone overflow
        this.overflowStartBit = bitPtr;

        // ecriture zone overflow
        if (nbOverflow > 0) {
            for (int i = 0; i < n; i++) {
                if (!isOv[i]) continue;
                int val = src[i] & ((kOverflow == 32) ? -1 : ((1 << kOverflow) - 1));
                int wi = bitPtr >>> 5, bo = bitPtr & 31;
                if (bo + kOverflow <= 32) {
                    words[wi] |= val << bo;
                } else {
                    int b1 = 32 - bo, b2 = kOverflow - b1;
                    words[wi]     |= (val & ((1 << b1) - 1)) << bo;
                    words[wi + 1] |= (val >>> b1) & ((1 << b2) - 1);
                }
                bitPtr += kOverflow;
            }
        }
    }

    // Benchmark
    private static long now() { return System.nanoTime(); }

    // Retourne la moyenne par run (ns)
    public static long timeCompressAvgNs(Mode mode, int[] src, int warmup, int runs) {
        // On fait un warmup pour éviter les effets de cache/optimisation

        for (int i = 0; i < warmup; i++) { BitPacking.of(mode).compress(src); }
        long total = 0;
        for (int i = 0; i < runs; i++) {
            BitPacking bp = BitPacking.of(mode);
            long t0 = now();
            bp.compress(src);
            long t1 = now();
            total += (t1 - t0);
        }
        return total / runs;
    }

    public long timeDecompressAvgNs(int warmup, int runs) {
        int[] dst = new int[n];
        for (int i = 0; i < warmup; i++) decompress(dst);
        long total = 0;
        for (int i = 0; i < runs; i++) {
            long t0 = now();
            decompress(dst);
            long t1 = now();
            total += (t1 - t0);
        }
        return total / runs;
    }


    public long timeGetSequentialAvgNs(int warmupLoops, int loops) {
        int acc = 0;
        for (int w = 0; w < warmupLoops; w++) for (int i = 0; i < n; i++) acc ^= get(i);
        long total = 0;
        for (int L = 0; L < loops; L++) {
            long t0 = now();
            for (int i = 0; i < n; i++) acc ^= get(i);
            long t1 = now();
            total += (t1 - t0);
        }

        if (acc == 42_4242) System.out.print(""); // Fait avec chatGPT, sert à éviter l'optimisation
        return total / (loops * (long) n);
    }

    // Taille brute sans compression (32 bits par entier)
    public static long rawBits(int n) { return 32L * n; }


    // Formule / seuil expliqués dans le rapport
    public static double breakEvenBandwidthBitsPerSec(long sRaw, long sComp, long tCompNs, long tDecompNs) {
        long savedBits = sRaw - sComp;
        long cpuNs = tCompNs + tDecompNs;
        if (savedBits <= 0 || cpuNs <= 0) return Double.POSITIVE_INFINITY;
        return (double) savedBits / (cpuNs * 1e-9); // bits / seconde
    }



    public long transmittedBits() { return (words == null) ? 0L : (long) words.length * 32L; }



}


