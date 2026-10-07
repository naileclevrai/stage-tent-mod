# Stage Tents

Mod Forge pour Minecraft 1.20.1. Il sert à monter un événement : tentes, scène, gradins, file d’attente, électricité de chantier et habillage textile. Tout est dans l’onglet créatif **Stage Tents**. La langue du jeu bascule entre français et anglais.

La plupart des meubles se teintent avec une teinture Minecraft. Le clic droit main vide assied le joueur sur les sièges. La **clé de montage** ouvre les menus et copie les réglages des tentes.

## Clé de montage

| Geste | Effet |
| --- | --- |
| Clic sur une tente | Colle les réglages copiés, ou ouvre le menu s’il n’y a rien en mémoire |
| Accroupi sur une tente | Copie ses réglages. La clé brille tant qu’elle garde une copie |
| Clic dans le vide | Ouvre le menu de la tente dans laquelle on se trouve |
| Accroupi dans le vide | Efface la copie |
| Sur un gradin | Construit une tribune |
| Sur un tourniquet | Change le mode. Accroupi : ouvre le menu |
| Sur un praticable | Passe à la hauteur suivante (25 cm, 50 cm, 75 cm, 1 m) |
| Sur une tour régie | Largeur, profondeur, hauteur, toit et bâches |
| Sur une frise ou un pendrillon | Ouvre le menu de longueur |
| Sur un rideau | Ferme, attache ou écarte |
| Accroupi sur un canon à eau | Inclinaison, panoramique, portée |
| Sur un passage de câble | Ouvre ou ferme ce module. Accroupi : nombre de goulottes |
| Accroupi sur un mât d’éclairage | Hauteur, orientation, inclinaison |

Copier d’une tente vers une tente du même type reprend tout. Vers un autre type, seules les couleurs et le style passent.

## Tentes

Chaque tente se pose par une platine métallique. Le menu règle la structure, le style et l’aménagement. La toile, les murs et les mâts posent des blocs de collision invisibles. Casser la platine retire l’ensemble.

Les tentes qui ont un intérieur peuvent recevoir un plancher (aucun, bois clair, bois foncé, moquette rouge, moquette noire, piste de danse), une scène au fond, un rideau de fond et des barres d’accroche. Les guirlandes sont éteintes, blanc chaud ou multicolores, et elles posent de vraies sources de lumière. La platine peut être cachée.

| Tente | Rôle | Tailles |
| --- | --- | --- |
| Chapiteau | Cirque, rayures, haubans, drapeaux | Largeur 8–64, longueur en plus 0–64, mâts 4–16 |
| Pagode | Pointe centrale | Largeur 3–16 |
| Tente de réception | Cadre rectangulaire, fenêtres | Largeur 4–30, longueur 4–64 |
| Barnum pliant | Petit cadre en ciseaux | Largeur 2–8, longueur 2–12 |
| Arche gonflable | Portique, épaisseur du boudin réglable | Portée 4–24 |
| Toile DJ | Deux mâts, un arceau, toile PVC des deux côtés, dessous ouvert | Portée 4–12, profondeur 2–6, hauteur 4–8 |
| Tente stretch | Toile libre sur mâts. Le débord et les mâts de bord se règlent. Des mâts posés à part sont détectés depuis le menu | Débord 2–10, jusqu’à 16 mâts |
| Arène tensile | Membrane sur deux lignes de mâts treillis | Portée 24–90, longueur 24–160, mâts jusqu’à 40 |
| Scène mobile Opus 4200 | Scène de concert déjà déployée, taille fixe | 16 × 13, hauteur 12 |

Pagode, réception et barnum se joignent côte à côte : le menu propose de coller les côtés voisins et de poser une gouttière.

Murs : roulés, fermés, ou fermés avec fenêtres. Entrées : aucune, avant, avant et arrière, façade ouverte, comptoir, comptoir avec l’arrière ouvert. Le comptoir fait 1,10 m. Une enseigne peut porter un titre (24 caractères) et un texte (64). Des rideaux peuvent habiller les poteaux.

Styles prêts : cirque classique, cirque bleu, concert noir, mariage, cabaret, guinguette, réception, garden party, bord de mer, et des variantes par famille (pagode, barnum, arche, stretch, arène, toile DJ).

Le mât de tente stretch et la barre d’accroche existent aussi comme blocs à poser seuls.

## Mobilier

Teinture sur le tissu, le coussin ou la coque. Les sièges se prennent au clic droit.

- Table ronde de banquet, blanche.
- Mange-debout, blanc, un peu plus haut.
- Table de pique-nique en bois, deux blocs, bancs intégrés.
- Table pliante en plastique blanc, trois blocs de long. Elle ne se plie pas en jeu et ne se teint pas.
- Chaise de banquet (bois, coussin blanc), chaise pliante (métal, noire), tabouret de bar (rouge).
- Comptoir de bar : se continue en ligne, les côtés ne s’habillent qu’aux extrémités.
- Gradin : siège, structure métallique. L’allée de gradins et la structure de gradins complètent une tribune.
- Stand de tir : décor de fête foraine sur six blocs (3 de large, 2 de haut). L’objet ne s’empile pas.

### Tribune

La clé sur un gradin ouvre le générateur. On choisit les rangées (1–16), la largeur (1–32), une allée tous les N sièges (0 pour aucune, jusqu’à 16) et la couleur des sièges. Il pose les sièges, les allées et la structure en dessous.

## File et accès

- **Barrière Vauban** : trois blocs de long, 1,5 bloc de haut.
- **Poteau à corde** : rouge par défaut. Une corde de velours part vers l’est et le sud jusqu’au poteau voisin, collé ou à un bloc d’écart s’il n’y a rien de solide entre les deux.
- **Rambarde de file** : inox, 1,5 bloc de haut. Une ligne reste droite. Là où deux rambardes se rencontrent à angle droit, le bloc devient un quart de tour.
- **Tourniquet tripode** : le boîtier est à gauche du passage. Le rotor tourne d’un tiers à chaque personne.
  - Libre : le rotor tourne.
  - Badge : fermé tant qu’un badge accepté n’a pas été présenté.
  - Verrouillé : personne ne passe.
  - Sens unique ou double. Ouverture tenue de quelques secondes, puis le portillon se referme ou reste ouvert. Son au passage, au choix.
  - Un signal redstone ouvre le tourniquet dans tous les modes.
  - Le comparateur envoie une impulsion au passage, ou un niveau selon le compteur. Le compteur se remet à zéro dans le menu.
- **Badge d’accès** : clic droit en l’air pour le programmer. Standard (même identifiant ; un badge sans identifiant n’ouvre que les tourniquets sans identifiant), passe-partout, ou usage unique. Le passe-partout brille.

## Scène

- **Praticable** : se continue en ligne. Hauteur 25 cm, 50 cm, 75 cm ou 1 m, par défaut 1 m. La clé passe à la hauteur suivante.
- **Tour régie** : échafaudage posé par une platine. Clic droit, ou la clé, ouvre le menu. Largeur 4 à 12 m, profondeur 4 à 8 m, hauteur 2 à 6 niveaux (chaque niveau fait 2 m). Toit et bâches de côté, teintés à la teinture. Le devant reste ouvert. Planchers en bois, garde-corps, contreventements, rosaces sur les montants, échelle dans l’angle arrière. On marche sur les planchers.
- **Escalier de scène** et **rampe de scène** : jaunes, en quatre et huit marches, et ils se continuent en ligne.
- **Cyclorama** : toile blanche sur trois blocs de haut, en ligne. La teinture colore la toile.
- **Frise** : tissu plissé à ourlet festonné, pendu à un tuyau. On aligne les blocs à hauteur du tuyau. Le tombé va de 25 cm à 16 m, par pas de 25 cm. La teinture recolore toute la ligne. Un seul tissu est dessiné pour toute la ligne.
- **Pendrillon** : jambe de tissu pleine hauteur, de 2 à 32 m. Le menu vérifie qu’il y a la place au-dessus. Même principe de ligne et de teinture.
- **Rideau** : rouge par défaut, teinture sur toute la ligne. Tombé de 1 à 12 m (défaut 4 m). Trois façons de l’ouvrir, avec une animation :
  - Fermé : deux pans qui se rejoignent, on ne passe pas.
  - Attaché : cordes dorées, deux glands, drapé festonné au-dessus.
  - Écarté : ouverture de 0 à 96 %, le tissu s’entasse aux montants.
  - Dès qu’il est attaché ou écarté, on traverse le vide. Le tuyau reste cliquable pour rouvrir le menu.

## Flight cases

Quatre tailles, coque métallique teintable : flight case (un bloc, rouge), malle (deux blocs, bleue), armoire (deux blocs de haut, jaune), grand flight case (quatre blocs, blanc).

## Électricité

- **Groupe électrogène** : trois blocs de long, deux de haut. Clic droit : marche ou arrêt. En marche, fumée à l’échappement et bruit de diesel en boucle. La teinture colore la capote (orange par défaut).
- **Mât d’éclairage** : remorque de chantier sur deux blocs, capote blanche teintable. Quatre projecteurs LED, crics, roues, attelage, prises, arrêt d’urgence. Clic droit : le même diesel, la fumée, et les projecteurs. Accroupi avec la clé : hauteur du mât de 2 à 8 m (défaut 6), orientation de −180° à 180° par pas de 15°, inclinaison vers le bas de 0° à 60° (défaut 20°). En marche, il pose jusqu’à deux blocs de lumière invisibles dans l’air, dans l’axe des projecteurs, et les retire à l’arrêt ou à la casse.
- **Coffret de distribution** : un bloc, caoutchouc noir, prises en façade. Il ne se teint pas.
- **Rack de distribution** : un flight case. Prises d’un côté, disjoncteurs de l’autre. Pas de poignées.
- **Passage de câble** : plastique noir et jaune, le mot CABLE moulé dans la matière. De 1 à 5 goulottes : plus il y en a, plus la rampe est large. Clic droit ouvre ou ferme ce module seulement. Accroupi avec la clé change le nombre de goulottes. Défaut : 2 goulottes, fermé.

## Chantier et effets

- **Canon à eau** : skid à damier, bouche dans le bloc qu’il regarde. Clic droit : marche ou arrêt. Le jet est visuel, sans bruit. Accroupi avec la clé : inclinaison de 20° à 70° (défaut 55°), panoramique de −180° à 180° par pas de 15° (le positif tourne la bouche vers la droite), portée de 2 à 16 m par pas de 2 m. La teinture colore le fût (bleu par défaut).
- **Sanitaire de chantier** : cabine sur deux blocs de haut. Clic droit ouvre la porte. La teinture colore la coque (bleue par défaut).
- **Clôture de site** : panneau de trois blocs de large et deux de haut, avec un brise-vue teintable (vert par défaut).
- **Oriflamme** : drapeau plume sur cinq blocs de haut. On ne cogne que le mât. Tissu rouge par défaut.

## Commande

`/stagetents clean [rayon]` demande le niveau opérateur 2. Sans rayon, elle agit sur 48 blocs (128 au maximum). Elle retire les cellules de toile invisibles qui ne appartiennent plus à aucune tente, puis reconstruit les tentes du voisinage.

## En jeu, ce qui se partage

- Une teinture sur un meuble ne change que la partie tissu, coussin, corde ou capot prévue pour ça. Le métal, les roues, les prises et les bandes de danger gardent leur couleur.
- Les meubles en ligne (bar, gradins, escalier, rampe, praticable, cyclorama, frise, pendrillon, rideau) reconnaissent leurs voisins du même groupe et du même sens.
- Les gros objets (générateur, clôture, barrière, flight cases, mât, table, sanitaire, oriflamme) occupent plusieurs blocs. Casser le bloc principal retire le reste. Les blocs annexes ne donnent pas de drop.
- Les tentes et le mât d’éclairage ne posent des blocs de lumière que dans l’air, et ne retirent que ceux qu’ils ont posés eux-mêmes.
