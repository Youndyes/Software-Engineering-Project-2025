#!/usr/bin/env bash
set -euo pipefail

# ---- Config ----
TEST_DIR="Tests"
MODES=("OVERLAP" "NO_OVERLAP" "OVERFLOW")

# ---- Compile ----
echo "[*] Compilation..."
javac BitPacking.java Main.java

# ---- Prepare outputs ----
: > results.txt
: > summary.txt
printf "%-25s %-10s %-12s %-12s %-10s %-10s %-10s %-10s %-10s %-10s %-8s\n" \
"File" "Mode" "sRaw(bits)" "sComp(bits)" "tComp(ms)" "tDecomp(ms)" "tGet(ns)" "B*(Mbit/s)" "Traw(s)" "Tcomp(s)" "OK" >> summary.txt
printf "%0.s-" {1..120} >> summary.txt
echo "" >> summary.txt

# ---- Run all tests ----
for f in "$TEST_DIR"/*.txt; do
  [ -e "$f" ] || { echo "Aucun fichier trouvé dans $TEST_DIR"; break; }

  for mode in "${MODES[@]}"; do
    echo "[*] Running: $f  $mode"
    echo "====== $f | $mode ======" >> results.txt

    # Exécution
    if ! out="$(java Main "$f" "$mode" 2>&1)"; then
      echo "[ERROR] Échec exécution sur $f $mode (voir results.txt)"
      echo "$out" >> results.txt
      printf "%-25s %-10s %-12s %-12s %-10s %-10s %-10s %-10s %-10s %-10s %-8s\n" \
        "$f" "$mode" "-" "-" "-" "-" "-" "-" "-" "-" "false" >> summary.txt
      continue
    fi

    echo "$out" >> results.txt
    echo "" >> results.txt

    # Parsing des valeurs affichées par Main (lignes FR)
    sRaw=$(echo "$out"    | sed -n 's/^Taille brute : \([0-9]\+\) bits/\1/p')
    sComp=$(echo "$out"   | sed -n 's/^Taille compressée : \([0-9]\+\) bits/\1/p')
    tComp=$(echo "$out"   | sed -n 's/^Temps moyen compression[^:]*: \([0-9.]\+\) ms/\1/p')
    tDecomp=$(echo "$out" | sed -n 's/^Temps moyen décompression[^:]*: \([0-9.]\+\) ms/\1/p')
    tGet=$(echo "$out"    | sed -n 's/^Accès get(i) séquentiel : \([0-9]\+\) ns.*/\1/p')
    Bstar=$(echo "$out"   | sed -n 's/^Débit seuil B\* : \([0-9.]\+\) Mbit.*/\1/p')
    Traw=$(echo "$out"    | sed -n 's/^Exemple : .* -> Traw=\([0-9.]\+\) s .*$/\1/p')
    Tcomp=$(echo "$out"   | sed -n 's/^Exemple : .* Tcomp=\([0-9.]\+\) s$/\1/p')
    ok=$(echo "$out"      | sed -n 's/^Décompression correcte : \(true\|false\)$/\1/p')

    # Ligne formatée (alignée)
    printf "%-25s %-10s %-12s %-12s %-10s %-10s %-10s %-10s %-10s %-10s %-8s\n" \
      "$f" "$mode" "$sRaw" "$sComp" "$tComp" "$tDecomp" "$tGet" "$Bstar" "$Traw" "$Tcomp" "$ok" >> summary.txt
  done
done

echo ""
echo "[OK] Exécution terminée."
echo "Résumé des tests dans : summary.txt"
echo "Logs complets dans : results.txt"
