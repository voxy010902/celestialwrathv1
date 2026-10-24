# Celestial Wrath (Fabric, Minecraft 26.2)

Item : **Désignateur Orbital** (onglet Outils, ou craft : 4 étoiles du Nether, 2 cristaux de l'End, 1 balise, 1 bloc de netherite).

- Clic droit : tir sur le bloc visé (portée 320)
- Accroupi + clic droit : change de mode
  1. Lance Orbitale — ciblage, charge, faisceau, cratère de 26 blocs, onde de choc, champignon, météores
  2. Tempête de Météores — 55 blocs de rayon, ~6 s d'alerte puis ~16 s de pluie + gros bolides
  3. Singularité — trou noir qui aspire blocs et entités, puis implosion

## Build
1. Génère un template sur https://fabricmc.net/develop/template/ (MC 26.2) pour obtenir gradlew + gradle/wrapper.
2. Remplace son `src/`, `build.gradle`, `gradle.properties`, `settings.gradle` par ceux-ci (ou copie juste les versions).
3. `./gradlew build` (JDK 25 requis) -> `build/libs/celestialwrath-1.0.0.jar`
4. Mets le jar + Fabric API dans `mods/` (client ET serveur).

## Réglages
- Rayons : constantes `RADIUS` (BeamStrike), `28` (SingularityStrike.collapse)
- Cooldown : `20 * 45` dans DesignatorItem
- Densité de météores : `0.45f` dans MeteorStormStrike
- Vitesse de creusement : argument de `crater.step(...)`

⚠ Détruit le terrain et tue les joueurs. Teste sur une copie de monde.
