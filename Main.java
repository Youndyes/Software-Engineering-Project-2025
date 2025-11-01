import java.util.*;
import java.nio.file.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage : java Main <fichier_test> <mode>");
            System.err.println("Modes possibles : OVERLAP, NO_OVERLAP, OVERFLOW");
            return;
        }

        // Chargement du tableau depuis le fichier
        int[] src;
        try {
            src = loadIntsFromFile(Paths.get(args[0]));
            System.out.println("Fichier chargé : " + args[0] + " (" + src.length + " entiers)");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du fichier : " + e.getMessage());
            return;
        }

        // Sélection du mode
        BitPacking.Mode mode;
        try {
            mode = BitPacking.Mode.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Mode invalide : " + args[1]);
            System.err.println("Utilisez : OVERLAP, NO_OVERLAP ou OVERFLOW");
            return;
        }

        System.out.println("\n=== Test du mode " + mode + " ===");

        // Mesures de temps
        long tComp = BitPacking.timeCompressAvgNs(mode, src, 5, 20);
        BitPacking bp = BitPacking.of(mode);
        bp.compress(src);
        long tDecomp = bp.timeDecompressAvgNs(5, 20);
        long tGetSeq = bp.timeGetSequentialAvgNs(2, 10);

        // Tailles
        long sRaw  = BitPacking.rawBits(src.length);
        long sComp = bp.transmittedBits();

        // Calcul du seuil
        double Bstar = BitPacking.breakEvenBandwidthBitsPerSec(sRaw, sComp, tComp, tDecomp);

        // Exemple
        double tLatency = 0.020; // 20 ms
        double B = 100e6;        // 100 Mbit/s
        double Traw  = tLatency + sRaw  / B;
        double Tcomp = (tComp + tDecomp) / 1e9 + tLatency + sComp / B;

        // Résultats
        System.out.println("Taille brute : " + sRaw + " bits");
        System.out.println("Taille compressée : " + sComp + " bits");
        System.out.printf("Temps moyen compression : %.3f ms%n", tComp / 1e6);
        System.out.printf("Temps moyen décompression : %.3f ms%n", tDecomp / 1e6);
        System.out.printf("Accès get(i) séquentiel : %d ns/appel%n", tGetSeq);
        System.out.printf("Débit seuil B* : %.1f Mbit/s%n", Bstar / 1e6);
        System.out.printf("Exemple : t=%.0f ms, B=%.0f Mbit/s -> Traw=%.3fs, Tcomp=%.3fs%n",
                tLatency*1e3, B/1e6, Traw, Tcomp);
        System.out.println("Compression rentable ? " + (Tcomp < Traw));

        // Vérification
        int[] dst = new int[src.length];
        bp.decompress(dst);
        System.out.println("Décompression correcte : " + Arrays.equals(src, dst));
    }

    // Lecture du fichier
    private static int[] loadIntsFromFile(Path path) throws IOException {
        String content = Files.readString(path).replace(',', ' ').trim();
        if (content.isEmpty()) return new int[0];
        String[] tokens = content.split("\\s+");
        int[] out = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) out[i] = Integer.parseInt(tokens[i]);
        return out;
    }
}
