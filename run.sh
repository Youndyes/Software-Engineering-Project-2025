#!/usr/bin/env bash
set -euo pipefail
LC_ALL=C

# ---- Couleurs ----
GREEN="\033[1;32m"
CYAN="\033[1;36m"
YELLOW="\033[1;33m"
RED="\033[1;31m"
RESET="\033[0m"

# ---- Dossier de tests ----
TEST_DIR="Tests"

# Compilation
echo -e "${CYAN}=============================="
echo -e "   🔧 Compilation du projet"
echo -e "==============================${RESET}"
javac BitPacking.java Main.java
echo -e "${GREEN} Compilation terminée avec succès${RESET}"
echo ""

# Liste fichiers de tests
echo -e "${CYAN}=============================="
echo -e "Fichiers de test disponibles"
echo -e "==============================${RESET}"

mapfile -t FILES < <(find "$TEST_DIR" -maxdepth 1 -type f -name "*.txt" | sort)
if [ ${#FILES[@]} -eq 0 ]; then
  echo -e "${RED}Aucun fichier trouvé dans le dossier ${TEST_DIR}/.${RESET}"
  exit 1
fi

for i in "${!FILES[@]}"; do
  fname=$(basename "${FILES[$i]}")
  printf "  %2d) %s\n" $((i+1)) "$fname"
done
echo ""

# ---- Étape 3 : Choix du fichier ----
read -rp "$(echo -e "${YELLOW}Entrez le numéro du test à exécuter : ${RESET}")" choice
if ! [[ "$choice" =~ ^[0-9]+$ ]] || [ "$choice" -lt 1 ] || [ "$choice" -gt "${#FILES[@]}" ]; then
  echo -e "${RED}Choix invalide.${RESET}"
  exit 1
fi
TEST_FILE="${FILES[$((choice-1))]}"
echo -e "→ Fichier sélectionné : ${GREEN}$(basename "$TEST_FILE")${RESET}"
echo ""

# Choix mode
echo -e "${CYAN}=============================="
echo -e "Modes disponibles"
echo -e "==============================${RESET}"
echo "  1) OVERLAP"
echo "  2) NO_OVERLAP"
echo "  3) OVERFLOW"
echo ""
read -rp "$(echo -e "${YELLOW}Entrez le numéro du mode : ${RESET}")" mode_choice

case "$mode_choice" in
  1) MODE="OVERLAP" ;;
  2) MODE="NO_OVERLAP" ;;
  3) MODE="OVERFLOW" ;;
  *) echo -e "${RED}Choix invalide.${RESET}"; exit 1 ;;
esac
echo -e "→ Mode sélectionné : ${GREEN}${MODE}${RESET}"
echo ""

# Exec
echo -e "${CYAN}=============================="
echo -e "Exécution du test"
echo -e "==============================${RESET}"
echo ""
java Main "$TEST_FILE" "$MODE"
echo ""
echo -e "${GREEN}✔ Test terminé avec succès${RESET}"
