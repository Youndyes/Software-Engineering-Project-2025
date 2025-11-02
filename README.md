# Software Engineering Project 2025

Projet réalisé dans le cadre de l'UE de Software Engineering à l'Université Côte d'Azur.

Objectif : Implémenter plusieurs méthodes de compression d'entiers tout en conservant l'accès direct aux éléments.

### Environnement de test
- **Système** : Ubuntu 22.04.1 LTS (64-bit) (Sur un pc windows)
- **Java version** : OpenJDK 11.0.28 (build 11.0.28+6-post-Ubuntu-1ubuntu122.04.1)

### Prérequis
- **Java 11 ou supérieur** (testé avec OpenJDK 11.0.28 sur Ubuntu 22.04)
- **Bash** (pour les scripts `run.sh` et `run_all.sh`)

### Structure du projet
- `BitPacking.java` : Contient l'implémentation des différentes méthodes de compression.
- `Main.java` : Point d'entrée du programme, gère les entrées/sorties et les tests.
- `run.sh` : Script pour exécuter un test unique.
- `run_all.sh` : Script pour exécuter une série de tests prédéfinis.
- `Tests/` : Dossier contenant les fichiers de test avec des entiers à compresser.

### Compilation et exécution
#### Manuelle
```bash
javac BitPacking.java Main.java
java Main _chemin_du_fichier_à_tester_ _mode_de_compression_
```

#### Avec script
```bash
./run.sh #Pour un seul test
./run_all.sh #Pour exécuter tous les tests
```

run.sh va demander le fichier à tester et le mode de compression.

run_all.sh va quant à lui exécuter tous les tests prédéfinis dans le script et mettre les resultats dans deux fichiers : results.txt (résultats détaillés) et summary.txt (résumé des résultats, sous forme de tab).

### Exemple de sortie 
```bash
=== Test du mode OVERFLOW ===          # Mode de compression utilisé
Taille brute : 3200000 bits            # Taille originale (non compressée)
Taille compressée : 540000 bits        # Taille après compression
Temps moyen compression : 0.31 ms      # Temps pour compresser
Temps moyen décompression : 0.09 ms    # Temps pour décompresser
Accès get(i) séquentiel : 8 ns/appel   # Temps moyen d’accès à un élément
Débit seuil B* : 560.3 Mbit/s          # Débit à partir duquel c’est rentable
Exemple : t=20 ms, B=100 Mbit/s -> Traw=0.052s, Tcomp=0.029s  # Exemple de gain pour une latence et une bande passante donnée
Compression rentable ? true            # Compression avantageuse
Décompression correcte : true          # Résultat fidèle à l’original
```

### Résumé des performances (extrait, retrouvable dans summary.txt)

| Fichier                   | Mode         | Taille comp. (bits) | tComp (ms) | tGet (ns) | B* (Mbit/s) |
|---------------------------|--------------|-----------------|-----------|----------|-------------|
| small_values.txt          | OVERLAP      | 300k            | 0.409     | 5        | 3682.5      |
|                           | NO_OVERLAP   | 320k            | 0.614     | 8        | 1923.7      |
|                           | OVERFLOW     | 400k            | 2.020     | 5        | 1139.4      |
| 95_5_distribution.txt     | OVERLAP      | 1.40M           | 0.482     | 4        | 2071.2      |
|                           | NO_OVERLAP   | 1.60M           | 0.797     | 8        | 962.9       |
|                           | OVERFLOW     | 520k            | 3.653     | 6        | 633.3       |
| random.txt                | OVERLAP      | 2.00M           | 0.582     | 5        | 1240.5      |
|                           | NO_OVERLAP   | 3.20M           | 1.284     | 8        | —           |
|                           | OVERFLOW     | 2.10M           | 3.073     | 5        | 307.3       |

Auteur : **Bensallah Younes**