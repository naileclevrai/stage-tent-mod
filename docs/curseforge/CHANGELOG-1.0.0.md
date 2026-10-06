# Stage Tents 1.0.0

Texte prêt à coller dans CurseForge, onglet **Changelog** de la version **1.0.0**.
Le français est en premier, l'anglais ensuite : n'en colle qu'un.

Minecraft **1.20.1**, Forge **47.2** ou plus récent.

---

## Français

Première version publique. Des tentes en toile tendue, une scène, et tout le chantier autour.

### Tentes

- **Chapiteau** : mâts roi, quarts de mât, lambrequin festonné, haubans et fanions. Rond ou long, de 1 à 8 mâts.
- **Pagode** : pointe creusée sur pieds aluminium. Carrée ou étirée, accolable avec chéneau.
- **Tente de réception** : deux pentes en travées, fermes, poteaux de pignon et fenêtres cintrées. Jusqu'à 30 × 64.
- **Barnum pliant** : structure en ciseaux, pour un bar ou un stand.
- **Tente stretch** : une membrane sur les mâts que tu poses toi-même, résolue comme une toile tendue, avec mâts de rive et sangles à cliquet.
- **Arène tensile** : grande portée sur mâts et ancrages.
- **Arche gonflable** : boudins en deux couleurs, lests et haubans. Lignes de départ et entrées.

La toile se règle dans un menu en trois onglets, avec aperçu en direct :

- deux couleurs, rayures, n'importe quel RGB, doublure séparée, lambrequin, ralingue, rideaux noués ;
- murs fermés, relevés, ou à fenêtres. Entrées : face, les deux bouts, face ouverte, ou comptoir ;
- planchers (bois clair ou foncé, moquette rouge ou noire, piste de danse) et scène avec rideau de fond ;
- guirlandes chaudes ou multicolores, qui éclairent vraiment ;
- barres d'accroche. Elles deviennent des tubes **Theatrical** si Theatrical est installé ;
- pagodes, réceptions et barnums se rejoignent : le côté commun perd ses murs et gagne un chéneau ;
- la toile, le lambrequin, les rideaux et les fanions bougent avec le temps, davantage sous la pluie et beaucoup en orage ;
- présets : cirque, concert noir, mariage, cabaret, guinguette, réception, garden-party, régie, et d'autres.

La clé à rigging copie et colle les réglages. `/stagetents clean` répare les cellules de collision orphelines.

### Scène

- **Praticables** à quatre hauteurs (25 cm, 50 cm, 75 cm, 1 m), jupe en velours teignable, vérins et contreventements. La clé change la hauteur.
- **Escalier** et **rampe**, avec garde-corps en bout de volée.
- **Cyclorama** : toile tendue sur une perche, teignable.
- **Flight cases** : le coffre, la malle (2 de large), l'armoire (2 de haut) et le grand (2 × 2). Les trois grands se fabriquent à partir du coffre.

### Chantier

- **Groupe électrogène**, 3 de long sur 2 de haut. Clic droit pour le démarrer : la fumée sort de l'échappement.
- **Sanitaire de chantier**, avec la porte qui s'ouvre.
- **Clôture de site**, 3 blocs de large, bâche, œillets et colliers.
- **Oriflamme**, environ 4 blocs et demi de haut.
- **Rambarde de file** en inox. Une file droite reste droite. Poser une rambarde perpendiculaire au bout de la file transforme ce bout en **virage arrondi**.
- **Barrière Vauban**, **poteaux à corde** (la corde rejoint le poteau suivant, même à un bloc d'écart) et **tourniquet tripode** réglable à la clé, avec badge d'accès.
- **Stand de tir**.

### Salle

- Tables de banquet, mange-debout, chaises de banquet, chaises pliantes, comptoir de bar et tabourets. Teignables. On s'assoit sur les sièges.
- **Gradins**, allées et structure. La clé ouvre le générateur de tribune (rangées, largeur, allées, couleur).

### Technique

- Les tentes sont des maillages calculés une fois, envoyés en un seul appel. De loin, l'intérieur et les détails fins sont ignorés, et le vent s'arrête au-delà de 40 blocs.
- Les collisions suivent la toile : les murs arrêtent à quelques centimètres du tissu, les haubans ne bloquent pas.
- Icônes pixel art dans l'inventaire, dans le même style que les tentes.

---

## English

First public release. Tensioned canvas tents, a stage, and the site around them.

### Tents

- **Big top**: king poles, quarter poles, scalloped valance, guy ropes and pennants. Round or long, with 1 to 8 masts.
- **Pagoda**: a deeply curved peak on aluminium legs. Square or stretched, and joinable with a gutter.
- **Reception tent**: a two-pitch frame in bays, with rafters, gable posts and arched windows. Up to 30 × 64.
- **Folding gazebo**: a scissor-frame canopy for a bar or a stall.
- **Stretch tent**: one membrane over the poles you place yourself, solved as tensioned fabric, with edge poles and ratchet straps.
- **Tensile arena**: a long span on masts and anchors.
- **Inflatable arch**: puffed chambers in two colours, ballast feet and guy ropes. Start lines and entrances.

The canvas is set from a three-tab screen, with a live preview:

- two colours, stripes, any RGB colour, a separate lining, valance, bolt rope, tied-back curtains;
- walls closed, rolled up, or with windows. Entrances: front, both ends, open front, or counter;
- floors (light or dark wood, red or black carpet, dance floor) and a stage with a back drape;
- warm or multicolour festoons that place real light;
- rigging bars. They become **Theatrical** pipes when Theatrical is installed;
- pagodas, reception tents and gazebos join side by side: the shared side loses its walls and gains a gutter;
- canvas, valance, curtains and flags move with the weather, more in the rain and a lot in a storm;
- presets: classic circus, concert black, wedding, cabaret, guinguette, reception, garden party, FOH control, and more.

The rigging wrench copies and pastes settings. `/stagetents clean` repairs orphaned collision cells.

### Stage

- **Decks** at four heights (25 cm, 50 cm, 75 cm, 1 m), with a dyeable velour skirt, jacks and braces. The wrench changes the height.
- **Stairs** and a **ramp**, with rails at the end of a run.
- **Cyclorama**: cloth on a pipe, dyeable.
- **Flight cases**: the trunk, the wide case (2 long), the wardrobe (2 tall) and the large one (2 × 2). The three bigger cases craft from the trunk.

### Site

- **Generator**, 3 long and 2 tall. Right-click to start it: smoke leaves the exhaust.
- **Site toilet**, with a door that opens.
- **Site fence**, 3 blocks wide, with a scrim, grommets and cable ties.
- **Feather flag**, about four and a half blocks tall.
- **Queue rails** in stainless steel. A straight run stays straight. Placing a rail perpendicular to the end of a run turns that end into a **curved corner**.
- **Crowd barriers**, **rope stanchions** (the rope reaches the next post, even one block away) and a **tripod turnstile** configured with the wrench, plus an access badge.
- **Shooting gallery**.

### Floor

- Banquet tables, standing tables, banquet chairs, folding chairs, a bar counter and stools. Dyeable. Seats can be sat on.
- **Bleachers**, aisles and supports. The wrench opens the grandstand builder (rows, width, aisles, colour).

### Under the hood

- Tents are baked into meshes once and streamed in a single bulk call. From far away the inside and the thin details are skipped, and the wind stops past 40 blocks.
- Collision follows the canvas: walls stop you within a few centimetres of the fabric, and guy ropes never block you.
- Pixel-art inventory icons, in the same style as the tents.
