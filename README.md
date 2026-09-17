# MacroTrack — Suivi de macros avec IA

Application Android (Kotlin + Jetpack Compose) qui :
- Garde en mémoire tout ton historique (poids, objectifs, repas) via **Room**.
- Te demande ton poids chaque semaine (logique "lundi matin", en fait "nouvelle semaine ISO non renseignée") et recalcule ton **BMR** (Mifflin-St Jeor).
- Récupère ta dépense active du jour via **Health Connect** (données Mi Fitness synchronisées) pour estimer ton **TDEE** et tes objectifs de macros (recomposition corporelle : léger déficit, protéines hautes).
- Te permet de **photographier un plat + le décrire**, envoie ça à **l'API Gemini** et récupère calories/macros au format JSON strict.
- Affiche un dashboard fluide avec anneaux de progression qui passent au rouge en cas de dépassement, et des conseils dynamiques de rattrapage.

## 1. Ouvrir le projet

Deux options : **Android Studio** (section suivante) ou **compiler via GitHub sans rien installer** (section "Compiler avec GitHub Actions" plus bas — recommandé si tu n'as pas Android Studio).

### Avec Android Studio

1. Installe [Android Studio](https://developer.android.com/studio) (version récente, "Ladybug" ou plus).
2. `File > Open` et sélectionne le dossier `MacroTrack` (celui qui contient `settings.gradle.kts`).
3. Laisse Gradle synchroniser (ça télécharge les dépendances, ça peut prendre quelques minutes la première fois).

> ⚠️ Je n'ai pas pu compiler ce projet moi-même (pas de SDK Android/Gradle dans mon environnement de génération), donc il est possible qu'Android Studio pointe une ou deux erreurs mineures de version de dépendance à la première synchro. Le plus probable : la version de `androidx.health.connect:connect-client` (encore en alpha, l'API change parfois). Si ça bloque, ouvre `app/build.gradle.kts` et mets la dernière version stable indiquée sur [la doc Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect).

## 2. Ajouter ta clé API Gemini

**Si tu utilises Android Studio en local :**
1. Va sur [Google AI Studio](https://aistudio.google.com/apikey) et crée une clé API (gratuite).
2. Ouvre `gradle.properties` à la racine du projet.
3. Remplace `COLLE_TA_CLE_API_ICI` par ta vraie clé :
   ```
   GEMINI_API_KEY=AIzaSy...ta_cle...
   ```
4. **Ne commite jamais ce fichier avec ta clé sur un repo public** (ajoute `gradle.properties` à ton `.gitignore` si tu utilises Git, ou au moins cette ligne).

**Si tu compiles via GitHub Actions :** ne touche pas à `gradle.properties`, laisse-le avec le texte `COLLE_TA_CLE_API_ICI`. La vraie clé va dans un **secret GitHub** (voir section suivante) — elle n'est jamais commitée dans le code.

Le modèle utilisé par défaut est `gemini-flash-latest` (alias toujours à jour côté Google). Si jamais Google le déprécie, change la valeur dans `UserPreferences.kt` (`geminiModelFlow`) ou ajoute un écran de réglages pour le faire depuis l'app.

## 3. Compiler avec GitHub Actions (sans Android Studio)

Le projet contient déjà `.github/workflows/build.yml` : GitHub compile l'APK pour toi à chaque `push`, tu n'as qu'à le télécharger.

1. **Crée un repo GitHub** (public ou privé, peu importe) — sur github.com, bouton "New repository", donne-lui un nom (ex: `macrotrack`), ne coche aucune case d'initialisation (pas de README/gitignore, tu les as déjà).

2. **Ajoute ta clé Gemini comme secret** (avant de pousser le code, ou juste après, peu importe) :
   - Dans ton nouveau repo → onglet **Settings** → **Secrets and variables** → **Actions** → bouton **New repository secret**.
   - Nom : `GEMINI_API_KEY`
   - Valeur : ta clé récupérée sur [Google AI Studio](https://aistudio.google.com/apikey)
   - Sauvegarde.

3. **Pousse le code** depuis ton ordinateur (dans le dossier `MacroTrack` décompressé) :
   ```bash
   cd MacroTrack
   git init
   git add .
   git commit -m "Premier import de MacroTrack"
   git branch -M main
   git remote add origin https://github.com/TON_PSEUDO/macrotrack.git
   git push -u origin main
   ```
   (Remplace `TON_PSEUDO/macrotrack` par l'URL réelle de ton repo, affichée sur la page GitHub juste après sa création.)

4. **Regarde la compilation se lancer** : sur GitHub, onglet **Actions** de ton repo → tu verras un run "Build MacroTrack APK" démarrer automatiquement. Ça prend 3 à 6 minutes (installation du SDK Android + compilation).

5. **Télécharge l'APK** : une fois le run terminé (coche verte ✅), clique sur le run → tout en bas, section **Artifacts** → télécharge `macrotrack-debug-apk` (c'est un zip contenant le `.apk`).

6. **Installe l'APK sur ton téléphone** :
   - Transfère le fichier `.apk` sur ton téléphone (par câble, Google Drive, Telegram à toi-même, etc.).
   - Ouvre-le depuis ton téléphone. Android va te demander d'autoriser "l'installation d'apps depuis cette source" la première fois — accepte.
   - L'app s'installe comme n'importe quelle app.

Si tu modifies le code plus tard (par exemple en re-uploadant des fichiers modifiés puis `git add . && git commit -m "maj" && git push`), un nouveau build se relance automatiquement et un nouvel APK est disponible dans Actions.

> Cet APK "debug" n'est pas signé pour le Play Store, mais s'installe très bien directement sur ton téléphone (sideload), ce qui suffit pour un usage perso.

## 4. Lier Mi Fitness à l'application

Xiaomi ne fournit pas d'API publique pour Mi Fitness. Le seul chemin officiel :

1. Installe l'app **Health Connect** depuis le Play Store si elle n'est pas déjà présente (sur Android 14+, elle est intégrée au système).
2. Dans **Mi Fitness** : Profil → Compte et confidentialité (ou "Paramètres de synchronisation") → active la synchronisation avec **Google Health Connect**.
3. Au premier lancement de MacroTrack, l'app te demandera l'autorisation de lire tes pas et calories actives dans Health Connect — accepte.
4. Si Mi Fitness ne remonte que les calories totales (et pas les "calories actives"), l'app fait automatiquement un calcul de repli (`total - BMR`).

## 5. Utilisation

1. **Premier lancement** : renseigne poids / taille / âge / sexe (calcule ton BMR initial).
2. **Chaque semaine** (dès qu'une nouvelle semaine commence), une fenêtre te demande ton poids du jour — valide pour recalculer ton BMR et tes objectifs.
3. **Onglet Aujourd'hui** : calories et macros du jour, comparées à tes objectifs, avec conseils si tu es en retard sur les protéines/lipides/glucides.
4. **Onglet Caméra** : prends en photo ton plat, décris-le si besoin (ex: "avec une cuillère d'huile d'olive"), Gemini calcule calories/macros, tu valides pour l'enregistrer.
5. **Onglet Historique** : retrouve tous tes jours passés et repas enregistrés.

## 6. Structure du projet

```
app/src/main/java/com/example/macrotrack/
├── data/
│   ├── local/          -> Room (UserStats, DailyGoal, Meal) : la mémoire long terme
│   ├── datastore/      -> Profil utilisateur + suivi hebdo de la pesée
│   ├── health/         -> Lecture Health Connect (Mi Fitness)
│   ├── remote/          -> Appel REST direct à l'API Gemini
│   └── repository/     -> MacroRepository, point d'entrée unique pour l'UI
├── domain/             -> BmrCalculator (Mifflin-St Jeor), MacroCalculator (objectifs)
├── ui/
│   ├── onboarding/     -> Premier lancement
│   ├── dashboard/      -> Écran principal + BottomSheet de pesée
│   ├── camera/         -> CameraX + appel Gemini
│   ├── history/        -> Historique des jours/repas
│   ├── components/     -> MacroRing (anneau réactif)
│   └── navigation/     -> Bottom bar + NavHost
└── util/               -> Dates (semaine ISO), factory de ViewModels
```

## 7. Personnaliser les objectifs

Tout se règle dans `domain/MacroCalculator.kt` :
- `DEFICIT_KCAL = 250f` → change le déficit calorique (200 à 300 kcal recommandé pour une recomposition corporelle).
- `proteinG = weightKg * 2f` → grammes de protéines par kg de poids de corps.
- `fatG = weightKg * 0.9f` → grammes de lipides par kg.
- Le reste des calories est automatiquement alloué aux glucides.

## 8. Limites connues / pistes d'amélioration

- Le calcul du BMR ne prend en compte que taille/âge/sexe/poids (formule Mifflin-St Jeor) — pas de % de masse grasse. Tu peux passer à la formule de **Katch-McArdle** si tu mesures régulièrement ta masse grasse.
- Pas d'écran de réglages pour modifier son profil après l'onboarding — facile à ajouter (réutilise `OnboardingViewModel.completeOnboarding` sans le `recordWeighIn`).
- Pas de compression/redimensionnement de l'image avant envoi à Gemini — pour économiser des tokens/de la latence sur de grosses photos, tu peux réduire la résolution du `Bitmap` avant `bitmapToBase64` dans `GeminiApi.kt`.
- Pas de gestion des erreurs réseau avec retry automatique.
