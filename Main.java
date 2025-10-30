public class Main {
    public static void main(String[] args) {
        int[] src = {5, 12, 8, 3, 14, 7};

        BitPacking bp = BitPacking.of(BitPacking.Mode.NO_OVERLAP);
        bp.compress(src);

        int[] out = new int[src.length];
        bp.decompressInto(out);

        System.out.println("n=" + bp.size() + ", k=" + bp.bitsPerValue());
        for (int i = 0; i < bp.size(); i++) System.out.println("get(" + i + ") = " + bp.get(i));
        System.out.print("ok ? ");
        boolean ok = true; // On compare ici le tableau src, qui contient les valeurs initiales, avec out, qui contient les valeurs décompressées
        for (int i = 0; i < src.length; i++) ok &= (src[i] == out[i]);
        System.out.println(ok);

        // Si les deux tableaux sont identiques, alors l'algorithme de compression/décompression fonctionne correctement
    }
}

