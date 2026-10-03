# Changelog

## 0.4.0

- Rendering engine reworked for performance:
  - Quads are sorted into layers, so the outside of the canvas is skipped from inside a tent, and the inside and the small details are skipped from far away.
  - Each vertex goes out in a single bulk call.
  - Wind animation stops past 40 blocks, and light is refreshed a slice per frame.
  - Up to 27% fewer quads on big tents, and up to 6x less CPU time per frame seen from afar.
- Stretch membrane solver stops as soon as it converges.
- Window panes no longer hide each other (dedicated translucent render type without depth writes).
- New entrances: **open front** and **counter** (front wall at table height with a shelf).
- New **FOH control** preset for pagodas, gazebos and reception tents.
- Quarter poles no longer poke through the big top canvas.

## 0.3.x

- New tents: **stretch tent** (free poles, solved membrane), **folding gazebo** and **inflatable arch**.
- **Floors** (wood, carpet, dance floor) and **stages** with a pleated back drape.
- **Joined sides and gutters** for pagodas, reception tents and gazebos, with automatic detection.
- **Rigging bars**, which become Theatrical pipes when Theatrical is installed.
- **Event furniture**: round tables, standing tables, banquet chairs, bar counters and bleachers. Dyeable, and you can sit on the seats.
- Canvas moves with the weather.
- Tabbed settings screen.
- Option to hide the centre plate.
- Blocks can be placed on stages.

## 0.2.x

- **Pagoda** and **reception tent**.
- Windows, rolled-up walls and entrances.
- Free RGB colours, lining and festoon lights.
- Presets.
- Rigging wrench (copy and paste settings).
- Tied-back door curtains, bolt rope, quarter poles, sagging guy ropes.
- Thin wall collision that follows the canvas.
- `/stagetents clean` command.

## 0.1.0

- First big top: tensioned canvas mesh, scalloped valance, guy ropes, flags and a live preview settings screen.
