package com.nailec.stagetents.client;

import com.nailec.stagetents.client.MeshKit.Builder;
import com.nailec.stagetents.furniture.MachineKind;
import com.nailec.stagetents.furniture.MachineKind.Scissor;

import static com.nailec.stagetents.furniture.MachineKind.FL_FORK_FACE;
import static com.nailec.stagetents.furniture.MachineKind.FL_FORK_TIP;
import static com.nailec.stagetents.furniture.MachineKind.FL_FRONT_AXLE;
import static com.nailec.stagetents.furniture.MachineKind.FL_GUARD_TOP;
import static com.nailec.stagetents.furniture.MachineKind.FL_MAST_H;
import static com.nailec.stagetents.furniture.MachineKind.FL_REAR;
import static com.nailec.stagetents.furniture.MachineKind.FL_REAR_AXLE;

/**
 * Meshes of the machines in model space (metres, the main block at the origin, the front towards -z).
 * Static pieces are built once per paint; the moving pieces (mast, carriage, scissor stack) are built per lift and
 * cached by the renderer.
 */
final class MachineMeshes {
    private MachineMeshes() {}

    static final int DARK = 0xFF2C2E31;
    static final int BLACK = 0xFF1B1C1E;
    static final int RUBBER = 0xFF1A1A1B;
    static final int TYRE_GREY = 0xFF85898D;
    static final int STEEL = 0xFF8C9197;
    static final int GALV = 0xFFC4C8CD;
    static final int CHROME = 0xFFE2E6EA;
    static final int HOSE = 0xFF141414;
    static final int SEAT = 0xFF232325;
    static final int AMBER = 0xFFFFA514;
    static final int RED_LENS = 0xFFD8231B;
    static final int YELLOW = 0xFFF2BE16;
    static final int WOOD = 0xFFC9A26E;
    static final int CASE = 0xFF222224;
    static final int ALU = 0xFFC9CDD2;
    static final int WHITE = 0xFFFFFFFF;

    // ================================================================== forklift

    /** Chassis, cab, overhead guard and wheels: everything that does not tilt with the mast. */
    static void forkliftBody(Builder b, int paint) {
        int paintDark = MeshKit.shade(paint, 0.78f);
        double x0 = -0.075, x1 = 1.075;

        // Wheels: big drive tyres under the cowl, small steer tyres under the counterweight.
        for (double x : new double[]{-0.06, 0.88}) wheel(b, x, x + 0.18, 0.345, FL_FRONT_AXLE, RUBBER, x < 0.5, Decals.WHEEL, 18);
        for (double x : new double[]{-0.03, 0.88}) wheel(b, x, x + 0.15, 0.29, FL_REAR_AXLE, RUBBER, x < 0.5, Decals.WHEEL, 16);
        // Drive axle housing and steer axle.
        b.rod(0.10, 0.345, FL_FRONT_AXLE, 0.90, 0.345, FL_FRONT_AXLE, 0.085, 10, DARK);
        b.box(0.36, 0.22, FL_FRONT_AXLE - 0.16, 0.64, 0.50, FL_FRONT_AXLE + 0.14, DARK);
        b.rod(0.10, 0.29, FL_REAR_AXLE, 0.90, 0.29, FL_REAR_AXLE, 0.05, 8, DARK);
        b.box(0.40, 0.26, FL_REAR_AXLE - 0.10, 0.60, 0.40, FL_REAR_AXLE + 0.10, DARK);

        // Frame rails between the wheels.
        b.box(0.13, 0.14, 1.30, 0.87, 0.60, 3.10, DARK);
        // Floor plate with tread and the side steps.
        b.box(0.02, 0.58, 1.46, 0.98, 0.62, 2.22, DARK);
        b.decalTop(0.10, 0.6205, 1.62, 0.80, 0.56, Decals.CHECKER);
        for (double x : new double[]{x0, 0.90}) {
            b.box(x, 0.36, 1.98, x + 0.175, 0.40, 2.22, DARK);
            b.decalTop(x + 0.01, 0.4005, 2.00, 0.155, 0.20, Decals.CHECKER);
        }
        // Front fenders over the drive wheels.
        for (double[] side : new double[][]{{x0, 0.13}, {0.87, x1}}) {
            fender(b, side[0], side[1], FL_FRONT_AXLE, 0.40, 0.70, paint);
        }
        // Cowl and dash.
        b.box(0.12, 0.62, 1.40, 0.88, 1.06, 1.60, paint);
        b.chamferBox(0.16, 1.06, 1.42, 0.84, 1.14, 1.62, 0.03, DARK);
        b.decal(0.31, 1.142, 1.47, 1, 0, 0, 0, 0.42, 0.91, 0.38, 0.13, Decals.DASH, WHITE, false);
        b.decalFront(0.30, 0.78, 1.399, 0.40, 0.20, Decals.LOAD_CHART);
        // Steering column, wheel and spinner knob.
        b.rod(0.50, 1.06, 1.58, 0.50, 1.32, 1.82, 0.035, 8, DARK);
        b.push().translate(0.50, 1.33, 1.83).rotX(-32);
        ring(b, 0, 0, 0, 0.165, 0.018, 16, BLACK);
        b.rod(0, 0, 0, -0.16, 0, 0, 0.012, 6, BLACK);
        b.rod(0, 0, 0, 0.16, 0, 0, 0.012, 6, BLACK);
        b.rod(0, 0, 0, 0, 0, 0.03, 0.04, 8, BLACK);
        b.rod(0.12, 0.10, 0.012, 0.12, 0.10, 0.07, 0.016, 6, BLACK);
        b.pop();
        // Hydraulic levers with their knobs: lift, tilt, side shift.
        for (int i = 0; i < 3; i++) {
            double x = 0.66 + i * 0.055;
            b.box(x - 0.008, 1.10, 1.56, x + 0.008, 1.32, 1.58, DARK);
            b.rod(x, 1.32, 1.57, x, 1.37, 1.585, 0.018, 6, i == 0 ? 0xFF2050C0 : BLACK);
        }
        // Pedals.
        for (double x : new double[]{0.32, 0.46, 0.60}) {
            b.box(x - 0.04, 0.63, 1.60, x + 0.04, 0.70, 1.66, BLACK);
        }
        // Inching and parking brake lever.
        b.rod(0.22, 0.95, 1.58, 0.22, 1.20, 1.70, 0.012, 6, DARK);
        b.rod(0.22, 1.20, 1.70, 0.22, 1.24, 1.72, 0.022, 6, 0xFFC01818);

        // Engine hood under the seat, side panels, battery box.
        b.box(x0, 0.30, 2.22, x1, 0.62, 3.06, paint);
        b.chamferBox(0.0, 0.62, 2.22, 1.0, 1.10, 3.06, 0.05, paint);
        // Hood seam, hinge and latch.
        b.box(0.0, 1.098, 2.20, 1.0, 1.104, 2.24, paintDark);
        b.box(0.45, 0.98, 3.055, 0.55, 1.04, 3.07, BLACK);
        // Side louvres.
        for (double x : new double[]{x0 - 0.001, x1 + 0.001}) {
            if (x < 0.5) b.decalLeft(x, 0.70, 2.50, 0.42, 0.22, Decals.GRILLE);
            else b.decalRight(x, 0.70, 2.50, 0.42, 0.22, Decals.GRILLE);
        }
        // Diesel filler cap on the left, hydraulic filler on the right.
        b.rod(x0, 0.52, 2.38, x0 - 0.03, 0.52, 2.38, 0.045, 10, BLACK);
        b.rod(x1, 0.52, 2.38, x1 + 0.03, 0.52, 2.38, 0.04, 10, 0xFFE0C020);
        // Seat on its suspension: base, cushion, backrest, headrest, hip restraints, belt.
        b.box(0.28, 1.10, 2.34, 0.72, 1.16, 2.80, BLACK);
        b.chamferBox(0.24, 1.16, 2.30, 0.76, 1.26, 2.78, 0.03, SEAT);
        b.push().translate(0.5, 1.26, 2.80).rotX(12);
        b.chamferBox(-0.25, 0, -0.10, 0.25, 0.50, 0.0, 0.03, SEAT);
        b.box(-0.27, 0.02, -0.08, -0.23, 0.30, 0.0, SEAT);
        b.box(0.23, 0.02, -0.08, 0.27, 0.30, 0.0, SEAT);
        b.box(-0.03, 0.30, -0.105, 0.03, 0.46, -0.10, 0xFFE06010);
        b.pop();
        b.box(0.20, 1.18, 2.40, 0.24, 1.32, 2.70, SEAT);
        b.box(0.76, 1.18, 2.40, 0.80, 1.32, 2.70, SEAT);
        b.box(0.20, 1.31, 2.38, 0.25, 1.35, 2.70, BLACK);
        b.box(0.75, 1.31, 2.38, 0.80, 1.35, 2.70, BLACK);
        // Orange seat belt over the cushion.
        b.box(0.24, 1.262, 2.52, 0.76, 1.268, 2.56, 0xFFE06010);

        // Counterweight: cast block with rounded rear corners, tow pin, lamps, hazard stripes.
        double cz0 = 3.04, cz1 = FL_REAR;
        b.box(x0, 0.24, cz0, x1, 1.12, cz1 - 0.12, paint);
        b.box(x0 + 0.08, 0.24, cz1 - 0.12, x1 - 0.08, 1.12, cz1, paint);
        for (double[] c : new double[][]{{x0 + 0.08, cz1 - 0.12, 180}, {x1 - 0.08, cz1 - 0.12, 270}}) {
            b.push().translate(c[0], 0.24, c[1]);
            corner(b, 0.12, 0.88, c[2], paint);
            b.pop();
        }
        // Top of the counterweight slopes down to the back.
        b.chamferBox(x0 + 0.02, 1.12, cz0, x1 - 0.02, 1.17, cz1 - 0.06, 0.03, paint);
        b.box(x0, 0.10, cz0 + 0.10, x1, 0.24, cz1 - 0.05, BLACK);
        b.decalBack(0.04, 0.30, cz1 + 0.002, 0.92, 0.10, Decals.HAZARD);
        b.decalBack(0.20, 0.62, cz1 + 0.002, 0.60, 0.15, Decals.BRAND);
        // Tow pin pocket and pin.
        b.box(0.42, 0.42, cz1, 0.58, 0.54, cz1 + 0.02, BLACK);
        b.rod(0.50, 0.48, cz1 + 0.01, 0.50, 0.66, cz1 + 0.01, 0.018, 8, CHROME);
        b.rod(0.50, 0.66, cz1 + 0.01, 0.55, 0.70, cz1 + 0.01, 0.01, 6, CHROME);
        // Rear lamps in the counterweight shoulders.
        for (double x : new double[]{0.04, 0.84}) {
            b.box(x, 0.92, cz1 - 0.02, x + 0.12, 1.04, cz1 + 0.005, BLACK);
            b.decalBack(x + 0.01, 0.93, cz1 + 0.007, 0.05, 0.10, Decals.TAILLIGHT);
            b.decal(x + 0.065, 0.93, cz1 + 0.007, 1, 0, 0, 0, 1, 0, 0.045, 0.10, Decals.BEACON, WHITE, false);
        }
        // Model badge on the counterweight sides.
        b.decalLeft(x0 - 0.002, 0.82, 3.10, 0.36, 0.09, Decals.MODEL_FD25);
        b.decalRight(x1 + 0.002, 0.82, 3.10, 0.36, 0.09, Decals.MODEL_FD25);
        b.decalLeft(x0 - 0.002, 0.62, 2.58, 0.40, 0.10, Decals.BRAND);
        b.decalRight(x1 + 0.002, 0.62, 2.58, 0.40, 0.10, Decals.BRAND);
        b.decalLeft(x0 - 0.002, 0.40, 2.30, 0.14, 0.14, Decals.WARN);
        b.decalRight(x1 + 0.002, 0.40, 2.30, 0.14, 0.14, Decals.WARN);

        // Overhead guard: four square posts, side rails, cross bars and slats.
        double gy = FL_GUARD_TOP;
        double[][] posts = {{0.02, 1.06, 1.46, 0.04, gy, 1.40}, {0.92, 1.06, 1.46, 0.94, gy, 1.40},
                {0.02, 1.15, 3.08, 0.04, gy, 3.20}, {0.92, 1.15, 3.08, 0.94, gy, 3.20}};
        for (double[] p : posts) {
            b.beam(p[0] + 0.03, p[1], p[2], p[3] + 0.03, p[4] - 0.03, p[5], 1, 0, 0, 0.06, 0.06, BLACK);
        }
        b.box(0.0, gy - 0.05, 1.36, 0.08, gy, 3.24, BLACK);
        b.box(0.92, gy - 0.05, 1.36, 1.0, gy, 3.24, BLACK);
        b.box(0.0, gy - 0.05, 1.36, 1.0, gy, 1.42, BLACK);
        b.box(0.0, gy - 0.05, 3.18, 1.0, gy, 3.24, BLACK);
        for (double z = 1.50; z < 3.14; z += 0.13) b.box(0.08, gy - 0.035, z, 0.92, gy - 0.005, z + 0.04, BLACK);
        // Work lamps on the front posts, mirror, amber beacon and blue spot on the guard.
        for (double x : new double[]{0.03, 0.97}) {
            b.box(x - 0.05, gy - 0.20, 1.34, x + 0.05, gy - 0.10, 1.40, BLACK);
            b.decalFront(x - 0.045, gy - 0.195, 1.339, 0.09, 0.09, Decals.HEADLIGHT);
        }
        b.rod(-0.02, gy - 0.30, 1.43, -0.12, gy - 0.22, 1.38, 0.008, 6, BLACK);
        b.box(-0.20, gy - 0.28, 1.36, -0.10, gy - 0.16, 1.38, BLACK);
        b.decal(-0.10, gy - 0.275, 1.359, -1, 0, 0, 0, 1, 0, 0.10, 0.11, Decals.MIRROR, WHITE, false);
        b.rod(0.82, gy, 3.12, 0.82, gy + 0.03, 3.12, 0.06, 10, BLACK);
        b.cyl(0.82, gy + 0.03, 3.12, 0.82, gy + 0.15, 3.12, 0.055, 0.045, 10, AMBER, true, MeshKit.PAINT);
        b.box(0.12, gy - 0.12, 3.24, 0.22, gy - 0.04, 3.28, BLACK);
        // Rear grab handle with the horn button, exhaust up the right rear post.
        b.rod(0.06, 1.40, 3.24, 0.06, 1.95, 3.24, 0.018, 8, BLACK);
        b.rod(0.06, 1.40, 3.24, 0.06, 1.40, 3.18, 0.018, 8, BLACK);
        b.rod(0.06, 1.95, 3.24, 0.06, 1.95, 3.18, 0.018, 8, BLACK);
        b.rod(0.06, 1.74, 3.255, 0.06, 1.74, 3.27, 0.018, 8, 0xFFC01818);
        b.rod(1.00, 1.10, 3.00, 1.00, 2.05, 3.00, 0.035, 10, DARK);
        b.cyl(1.00, 2.05, 3.00, 1.00, 2.12, 2.98, 0.035, 0.04, 10, BLACK, true, MeshKit.PAINT);
        // Mast tilt brackets on the cowl.
        for (double x : new double[]{0.10, 0.84}) b.box(x, 0.70, 1.62, x + 0.06, 0.86, 1.80, DARK);
    }

    /** Outer mast: two channels, ties, chain anchors and the lift cylinder barrels. Tilts as one with the carriage. */
    static void forkliftOuterMast(Builder b, int paint) {
        double z0 = 1.18, z1 = 1.32, y0 = 0.10, y1 = FL_MAST_H;
        for (double x : new double[]{0.10, 0.84}) {
            channel(b, x, x + 0.06, y0, y1, z0, z1, x < 0.5, DARK);
        }
        b.box(0.10, y1 - 0.10, z1 - 0.06, 0.90, y1, z1, DARK);
        b.box(0.10, 0.70, z1 - 0.05, 0.90, 0.80, z1, DARK);
        b.box(0.10, y0, z0, 0.90, y0 + 0.08, z1, DARK);
        // Tilt pivots at the foot.
        for (double x : new double[]{0.08, 0.86}) b.rod(x, 0.30, 1.30, x + 0.06, 0.30, 1.30, 0.05, 8, STEEL);
        // Lift cylinder barrels behind the channels.
        for (double x : new double[]{0.20, 0.80}) {
            b.rod(x, 0.14, 1.38, x, 1.80, 1.38, 0.045, 10, DARK);
            b.rod(x, 1.80, 1.38, x, 1.86, 1.38, 0.05, 10, BLACK);
            b.box(x - 0.05, 0.10, 1.32, x + 0.05, 0.16, 1.44, DARK);
        }
        // Chain anchors on the top tie.
        for (double x : new double[]{0.38, 0.62}) b.box(x - 0.02, y1 - 0.18, z1 - 0.06, x + 0.02, y1 - 0.10, z1 + 0.02, STEEL);
        b.decal(0.30, 1.20, z1 + 0.061, 1, 0, 0, 0, 1, 0, 0.40, 0.06, Decals.MAST_TAG, WHITE, false);
        // Hose reel on the top tie.
        b.rod(0.47, y1 - 0.05, z1 + 0.06, 0.53, y1 - 0.05, z1 + 0.06, 0.07, 12, DARK);
    }

    /** Inner mast at its rise above the outer one, with the sheaves and the cylinder rods. */
    static void forkliftInnerMast(Builder b, double rise) {
        double z0 = 1.14, z1 = 1.26, y0 = 0.12 + rise, y1 = FL_MAST_H - 0.04 + rise;
        for (double x : new double[]{0.16, 0.78}) channel(b, x, x + 0.06, y0, y1, z0, z1, x < 0.5, BLACK);
        b.box(0.16, y1 - 0.08, z1 - 0.05, 0.84, y1, z1, BLACK);
        b.box(0.16, y0, z1 - 0.05, 0.84, y0 + 0.06, z1, BLACK);
        // Rods from the barrels to the crosshead.
        for (double x : new double[]{0.20, 0.80}) b.rod(x, 1.84, 1.38, x, y1 - 0.02, 1.38, 0.028, 8, CHROME);
        b.box(0.14, y1 - 0.02, 1.26, 0.86, y1 + 0.04, 1.44, BLACK);
        // Chain sheaves.
        for (double x : new double[]{0.38, 0.62}) b.rod(x - 0.025, y1 + 0.02, 1.20, x + 0.025, y1 + 0.02, 1.20, 0.07, 12, STEEL);
    }

    /** Carriage, backrest, forks and the load, at fork height {@code fork}. */
    static void forkliftCarriage(Builder b, double fork, int load) {
        double y = fork + 0.025;
        double cz0 = FL_FORK_FACE, cz1 = 1.13;
        // Carriage plate between two fork bars, and the rollers running in the inner mast.
        b.box(0.08, y + 0.04, cz0 + 0.02, 0.92, y + 0.42, cz1, BLACK);
        b.box(0.06, y + 0.30, cz0, 0.94, y + 0.40, cz0 + 0.03, DARK);
        b.box(0.06, y + 0.04, cz0, 0.94, y + 0.12, cz0 + 0.03, DARK);
        for (double x = 0.14; x < 0.90; x += 0.0475) b.box(x, y + 0.395, cz0 - 0.002, x + 0.012, y + 0.40, cz0 + 0.01, BLACK);
        for (double x : new double[]{0.16, 0.78}) {
            b.box(x, y + 0.02, cz1, x + 0.06, y + 0.46, 1.20, BLACK);
            for (double ry : new double[]{y + 0.10, y + 0.38}) b.rod(x - 0.01, ry, 1.19, x + 0.07, ry, 1.19, 0.035, 8, STEEL);
        }
        // Load backrest.
        double by0 = y + 0.42, by1 = y + 1.30;
        b.box(0.08, by0, cz0 + 0.04, 0.12, by1, cz0 + 0.08, BLACK);
        b.box(0.88, by0, cz0 + 0.04, 0.92, by1, cz0 + 0.08, BLACK);
        b.box(0.08, by1 - 0.04, cz0 + 0.04, 0.92, by1, cz0 + 0.08, BLACK);
        b.box(0.08, by0 + 0.38, cz0 + 0.045, 0.92, by0 + 0.42, cz0 + 0.075, BLACK);
        for (double x = 0.20; x < 0.86; x += 0.11) b.box(x, by0, cz0 + 0.05, x + 0.025, by1 - 0.04, cz0 + 0.07, BLACK);
        // Chain ends on the carriage.
        for (double x : new double[]{0.38, 0.62}) b.box(x - 0.02, y + 0.40, 1.15, x + 0.02, y + 0.50, 1.19, STEEL);
        // Forks: hook, shank, heel and a tapered blade.
        for (double fx : new double[]{0.27, 0.73}) fork(b, fx, y);
        b.decalFront(0.32, y + 0.16, cz0 - 0.001, 0.36, 0.07, Decals.MODEL_FD25);
        switch (load) {
            case 1 -> pallet(b, y - 0.025);
            case 2 -> {
                pallet(b, y - 0.025);
                cartons(b, y - 0.025 + 0.144);
            }
            case 3 -> {
                pallet(b, y - 0.025);
                double top = y - 0.025 + 0.144;
                flightCase(b, 0.12, top, -0.14, 0.88, top + 0.64, 0.98, true);
                flightCase(b, 0.16, top + 0.64, -0.08, 0.84, top + 1.22, 0.92, false);
            }
            case 4 -> decks(b, y + 0.045);
            default -> {}
        }
    }

    /** Chains from the anchor on the outer tie, over the sheaves, down to the carriage. */
    static void forkliftChains(Builder b, double fork, double rise) {
        double sheave = FL_MAST_H - 0.04 + rise + 0.02;
        double carriage = fork + 0.025 + 0.48;
        for (double x : new double[]{0.38, 0.62}) {
            chain(b, x, carriage, 1.17, sheave, 1.13);
            chain(b, x, FL_MAST_H - 0.14, 1.27, sheave, 1.27);
            b.rod(x - 0.012, sheave + 0.05, 1.13, x + 0.012, sheave + 0.07, 1.27, 0.01, 4, DARK);
        }
    }

    /** Tilt cylinders, from the frame brackets to the mast at its tilt. */
    static void forkliftTiltRams(Builder b, double tiltDeg) {
        double pz = 1.30, py = 0.30;
        double a = Math.toRadians(tiltDeg);
        double my = 1.00 - py, mz = 1.34 - pz;
        double ry = py + my * Math.cos(a) - mz * Math.sin(a);
        double rz = pz + my * Math.sin(a) + mz * Math.cos(a);
        for (double x : new double[]{0.13, 0.87}) {
            double fy = 0.80, fz = 1.72;
            double dy = ry - fy, dz = rz - fz, len = Math.sqrt(dy * dy + dz * dz);
            double by = fy + dy / len * 0.28, bz = fz + dz / len * 0.28;
            b.rod(x, fy, fz, x, by, bz, 0.04, 10, DARK);
            b.rod(x, by, bz, x, ry, rz, 0.022, 8, CHROME);
            b.rod(x - 0.03, ry, rz, x + 0.03, ry, rz, 0.03, 8, STEEL);
        }
    }

    // ------------------------------------------------------------------ forklift pieces

    private static void channel(Builder b, double x0, double x1, double y0, double y1, double z0, double z1, boolean left, int col) {
        double t = 0.016;
        b.box(x0, y0, z0, x1, y1, z0 + t, col);
        b.box(x0, y0, z1 - t, x1, y1, z1, col);
        double wx0 = left ? x0 : x1 - t, wx1 = left ? x0 + t : x1;
        b.box(wx0, y0, z0, wx1, y1, z1, col);
    }

    private static void fork(Builder b, double fx, double y) {
        double w = 0.05;
        // Hooks over the two fork bars and the shank.
        b.box(fx - w, y + 0.28, FL_FORK_FACE - 0.06, fx + w, y + 0.44, FL_FORK_FACE - 0.005, DARK);
        b.box(fx - w, y + 0.06, FL_FORK_FACE - 0.06, fx + w, y + 0.14, FL_FORK_FACE - 0.005, DARK);
        b.box(fx - w, y, FL_FORK_FACE - 0.05, fx + w, y + 0.44, FL_FORK_FACE - 0.005, DARK);
        // Locking pin.
        b.rod(fx, y + 0.44, FL_FORK_FACE - 0.03, fx, y + 0.48, FL_FORK_FACE - 0.03, 0.012, 6, CHROME);
        // Blade, thinning to the tip, with a heel.
        double tipStart = FL_FORK_TIP + 0.18;
        b.box(fx - w, y, tipStart, fx + w, y + 0.045, FL_FORK_FACE - 0.05, DARK);
        b.quad(fx - w, y + 0.045, tipStart, fx + w, y + 0.045, tipStart, fx + w * 0.8, y + 0.012, FL_FORK_TIP, fx - w * 0.8, y + 0.012, FL_FORK_TIP, DARK, MeshKit.PAINT);
        b.quad(fx - w, y, FL_FORK_TIP, fx + w, y, FL_FORK_TIP, fx + w, y, tipStart, fx - w, y, tipStart, DARK, MeshKit.PAINT);
        b.quad(fx - w * 0.8, y, FL_FORK_TIP, fx - w * 0.8, y + 0.012, FL_FORK_TIP, fx + w * 0.8, y + 0.012, FL_FORK_TIP, fx + w * 0.8, y, FL_FORK_TIP, DARK, MeshKit.PAINT);
        b.quad(fx - w, y, tipStart, fx - w, y + 0.045, tipStart, fx - w * 0.8, y + 0.012, FL_FORK_TIP, fx - w * 0.8, y, FL_FORK_TIP, DARK, MeshKit.PAINT);
        b.quad(fx + w, y, tipStart, fx + w * 0.8, y, FL_FORK_TIP, fx + w * 0.8, y + 0.012, FL_FORK_TIP, fx + w, y + 0.045, tipStart, DARK, MeshKit.PAINT);
        b.quad(fx - w, y + 0.045, FL_FORK_FACE - 0.05, fx + w, y + 0.045, FL_FORK_FACE - 0.05, fx + w, y + 0.10, FL_FORK_FACE - 0.05, fx - w, y + 0.10, FL_FORK_FACE - 0.05, DARK, MeshKit.PAINT);
        // Heel fillet.
        b.beam(fx, y + 0.045, FL_FORK_FACE - 0.11, fx, y + 0.11, FL_FORK_FACE - 0.05, 1, 0, 0, 0.03, w * 2, DARK);
    }

    private static void chain(Builder b, double x, double ya, double za, double yb, double zb) {
        double dy = yb - ya, dz = zb - za, len = Math.sqrt(dy * dy + dz * dz);
        int links = Math.max(1, (int) (len / 0.035));
        for (int i = 0; i < links; i++) {
            double t0 = i / (double) links, t1 = (i + 0.8) / links;
            double w = i % 2 == 0 ? 0.036 : 0.026;
            b.beam(x, ya + dy * t0, za + dz * t0, x, ya + dy * t1, za + dz * t1, 0, 0, 1, w, 0.012, i % 2 == 0 ? 0xFF505357 : 0xFF3A3C40);
        }
    }

    private static void pallet(Builder b, double y) {
        double x0 = 0.10, x1 = 0.90, z0 = FL_FORK_FACE - 0.05 - 1.20, z1 = FL_FORK_FACE - 0.05;
        int wood = WOOD, dark = MeshKit.shade(WOOD, 0.82f);
        double[] xs = {x0, (x0 + x1) / 2 - 0.0725, x1 - 0.145};
        double[] zs = {z0, (z0 + z1) / 2 - 0.05, z1 - 0.10};
        for (double x : xs) {
            b.box(x, y, z0, x + 0.145, y + 0.022, z1, dark);
            for (double z : zs) {
                b.box(x, y + 0.022, z, x + 0.145, y + 0.10, z + 0.10, z == zs[1] ? dark : wood);
                if (z == zs[0]) b.decalFront(x + 0.02, y + 0.035, z - 0.001, 0.10, 0.05, Decals.PALLET);
            }
        }
        for (double z : zs) b.box(x0, y + 0.10, z, x1, y + 0.122, z + 0.10, dark);
        double[] tops = {x0, x0 + 0.1575, (x0 + x1) / 2 - 0.0725, x1 - 0.2575, x1 - 0.145};
        double[] widths = {0.145, 0.10, 0.145, 0.10, 0.145};
        for (int i = 0; i < tops.length; i++) {
            b.box(tops[i], y + 0.122, z0, tops[i] + widths[i], y + 0.144, z1, wood);
            b.decalTop(tops[i] + 0.002, y + 0.1445, z0 + 0.02, widths[i] - 0.004, 1.16, Decals.PALLET);
        }
    }

    private static void cartons(Builder b, double y) {
        double z1 = FL_FORK_FACE - 0.06, z0 = z1 - 1.18;
        int brown = 0xFFC8955E;
        for (int layer = 0; layer < 2; layer++) {
            double ly = y + 0.008 + layer * 0.45;
            for (int ix = 0; ix < 2; ix++) {
                for (int iz = 0; iz < 3; iz++) {
                    double bx0 = 0.11 + ix * 0.395, bz0 = z0 + iz * 0.393;
                    double bx1 = bx0 + 0.39, bz1 = bz0 + 0.388;
                    b.box(bx0, ly, bz0, bx1, ly + 0.43, bz1, brown);
                    b.decalTop(bx0 + 0.15, ly + 0.43, bz0, 0.09, 0.388, Decals.CARTON);
                    if (iz == 0) b.decalFront(bx0 + 0.02, ly + 0.05, bz0 - 0.001, 0.35, 0.18, Decals.CARTON);
                    if (ix == 0) b.decalLeft(bx0 - 0.001, ly + 0.05, bz0 + 0.02, 0.35, 0.18, Decals.CARTON);
                    if (ix == 1) b.decalRight(bx1 + 0.001, ly + 0.05, bz0 + 0.02, 0.35, 0.18, Decals.CARTON);
                }
            }
        }
        // Pallet strapping.
        for (double x : new double[]{0.30, 0.70}) {
            b.box(x - 0.012, y, z0 - 0.004, x + 0.012, y + 0.905, z0, 0xFF2A5CC8);
            b.box(x - 0.012, y + 0.900, z0, x + 0.012, y + 0.905, z1, 0xFF2A5CC8);
            b.box(x - 0.012, y, z1, x + 0.012, y + 0.905, z1 + 0.004, 0xFF2A5CC8);
        }
    }

    /** Road case: black panels, aluminium extrusions on every edge, ball corners, latches and handles. */
    static void flightCase(Builder b, double x0, double y0, double z0, double x1, double y1, double z1, boolean label) {
        b.box(x0, y0, z0, x1, y1, z1, CASE);
        double e = 0.018;
        double[] xs = {x0 - 0.002, x1 - e + 0.002}, ys = {y0, y1 - e}, zs = {z0 - 0.002, z1 - e + 0.002};
        for (double x : xs) for (double y : ys) b.box(x, y, z0, x + e, y + e, z1, ALU);
        for (double x : xs) for (double z : zs) b.box(x, y0, z, x + e, y1, z + e, ALU);
        for (double y : ys) for (double z : zs) b.box(x0, y, z, x1, y + e, z + e, ALU);
        double lid = y1 - (y1 - y0) * 0.25;
        b.box(x0 - 0.004, lid - 0.008, z0 - 0.004, x1 + 0.004, lid + 0.008, z1 + 0.004, ALU);
        for (double x : xs) for (double y : new double[]{y0, y1}) for (double z : zs) {
            b.box(x - 0.008, y - (y == y0 ? 0 : 0.022), z - 0.008, x + e + 0.008, y + (y == y0 ? 0.022 : 0), z + e + 0.008, CHROME);
        }
        for (double x : new double[]{x0 + (x1 - x0) * 0.25, x1 - (x1 - x0) * 0.25}) {
            b.box(x - 0.03, lid - 0.04, z0 - 0.012, x + 0.03, lid + 0.03, z0, CHROME);
            b.rod(x, lid - 0.005, z0 - 0.014, x, lid - 0.005, z0 - 0.02, 0.014, 8, CHROME);
        }
        double hy = (y0 + lid) * 0.5;
        for (double x : new double[]{x0, x1}) {
            double s = x == x0 ? -1 : 1;
            b.box(Math.min(x, x + s * 0.012), hy - 0.04, (z0 + z1) / 2 - 0.10, Math.max(x, x + s * 0.012), hy + 0.04, (z0 + z1) / 2 + 0.10, CHROME);
            b.box(Math.min(x, x + s * 0.02), hy - 0.01, (z0 + z1) / 2 - 0.07, Math.max(x, x + s * 0.02), hy + 0.01, (z0 + z1) / 2 + 0.07, BLACK);
        }
        if (label) b.decalFront(x0 + (x1 - x0) * 0.3, y0 + (y1 - y0) * 0.30, z0 - 0.003, (x1 - x0) * 0.4, (y1 - y0) * 0.25, Decals.CASE);
    }

    private static void decks(Builder b, double y) {
        double z1 = FL_FORK_FACE - 0.06, z0 = z1 - 1.0;
        // Two bearers across the forks, then four 2 x 1 m decks.
        for (double z : new double[]{z0 + 0.12, z1 - 0.22}) b.box(-0.05, y, z, 1.05, y + 0.074, z + 0.10, WOOD);
        double dy = y + 0.074;
        for (int i = 0; i < 4; i++) {
            double t = dy + i * 0.15;
            // Deck: aluminium frame with a recessed bolt rail, phenolic top. Layers leave a 5 mm gap.
            b.box(-0.50, t + 0.016, z0 - 0.01, 1.50, t + 0.14, z1 + 0.01, ALU);
            b.box(-0.49, t + 0.05, z0 - 0.022, 1.49, t + 0.10, z1 + 0.022, 0xFF9DA2A8);
            b.box(-0.495, t + 0.14, z0 - 0.005, 1.495, t + 0.15, z1 + 0.005, 0xFF2E2A26);
            b.decalTop(-0.48, t + 0.15, z0 + 0.01, 1.96, 0.98, Decals.DECK);
        }
        // Ratchet strap round the stack.
        double top = dy + 0.6;
        for (double x : new double[]{0.0, 1.0}) {
            b.box(x - 0.025, dy, z0 - 0.014, x + 0.025, top + 0.004, z0 - 0.01, 0xFFE0A010);
            b.box(x - 0.025, top, z0 - 0.014, x + 0.025, top + 0.004, z1 + 0.014, 0xFFE0A010);
            b.box(x - 0.025, dy, z1 + 0.01, x + 0.025, top + 0.004, z1 + 0.014, 0xFFE0A010);
        }
        b.box(-0.06, dy + 0.25, z0 - 0.04, 0.06, dy + 0.33, z0 - 0.014, CHROME);
    }

    // ================================================================== scissor lifts

    /** Chassis of a scissor lift with its wheels, trays, steps and decals. Outriggers are drawn apart. */
    static void scissorBase(Builder b, MachineKind kind, int paint, boolean potholes) {
        Scissor s = kind.scissor();
        boolean rt = kind == MachineKind.RT_SCISSOR;
        if (rt) rtBase(b, s, paint);
        else slabBase(b, s, paint, potholes);
        // Scissor tracks and the fixed pivot brackets on the chassis.
        Arms arms = Arms.of(kind);
        for (double x : new double[]{arms.outer0, arms.outer1}) {
            double side = x < 1 ? -1 : 1;
            double tx = x + side * (arms.w * 0.5 + 0.03);
            b.box(Math.min(x, tx) - 0.01, s.chassisTop(), s.fixedZ() - s.arm() - 0.08, Math.max(x, tx) + 0.01,
                    s.chassisTop() + 0.03, s.fixedZ() - 0.20, STEEL);
            b.box(x - arms.w * 0.5 - 0.03, s.chassisTop(), s.fixedZ() - 0.08, x + arms.w * 0.5 + 0.03, s.pivot() + 0.05,
                    s.fixedZ() + 0.08, DARK);
        }
    }

    private static void slabBase(Builder b, Scissor s, int paint, boolean potholes) {
        double x0 = s.x0(), x1 = s.x1(), top = s.chassisTop(), len = MachineKind.SL08_LENGTH;
        int paintDark = MeshKit.shade(paint, 0.8f);
        // Wheels: grey non-marking tyres, front steer, rear drive with the motor hub.
        double r = 0.19;
        for (double z : new double[]{0.34, len - 0.34}) {
            wheel(b, x0 + 0.01, x0 + 0.13, r, z, TYRE_GREY, true, Decals.WHEEL, 18);
            wheel(b, x1 - 0.13, x1 - 0.01, r, z, TYRE_GREY, false, Decals.WHEEL, 18);
        }
        // Body: the steel tub, chamfered ends, and the swing-out trays on both sides.
        double end0 = 0.60, end1 = len - 0.60;
        b.box(x0 + 0.14, 0.10, 0.04, x1 - 0.14, top - 0.01, len - 0.04, DARK);
        b.chamferBox(x0, 0.20, end0, x1, top, end1, 0.02, paint);
        b.box(x0 + 0.02, 0.12, end0, x1 - 0.02, 0.19, end1, paintDark);
        for (double[] end : new double[][]{{0.04, end0}, {end1, len - 0.04}}) {
            b.box(x0 + 0.14, 0.27, end[0], x1 - 0.14, top - 0.025, end[1], paint);
            b.box(x0 + 0.145, top - 0.025, end[0] + 0.005, x1 - 0.145, top, end[1] - 0.005, paintDark);
        }
        // Rubber bumpers.
        b.box(x0 + 0.08, 0.20, 0.0, x1 - 0.08, 0.36, 0.03, RUBBER);
        b.box(x0 + 0.08, 0.20, len - 0.03, x1 - 0.08, 0.36, len, RUBBER);
        // Tray doors: battery on the left, hydraulics and ground controls on the right.
        double doorZ0 = end0 + 0.06, doorZ1 = end1 - 0.06;
        b.decalLeft(x0, 0.24, doorZ0 + 0.04, 0.42, 0.20, Decals.GRILLE);
        b.decalLeft(x0, 0.26, doorZ0 + 0.56, 0.34, 0.085, Decals.BATTERY);
        b.decalLeft(x0, 0.37, doorZ0 + 0.56, 0.34, 0.085, Decals.MODEL_SL08);
        b.decalRight(x1, 0.24, doorZ0 + 0.04, 0.30, 0.15, Decals.GROUND_PANEL);
        b.decalRight(x1, 0.25, doorZ0 + 0.44, 0.38, 0.095, Decals.LOWERING);
        b.decalRight(x1, 0.36, doorZ0 + 0.44, 0.38, 0.095, Decals.BRAND);
        for (double x : new double[]{x0 - 0.008, x1 + 0.004}) {
            b.box(x, 0.20, doorZ0, x + 0.004, 0.21, doorZ1, BLACK);
            b.box(x, 0.30, doorZ1 - 0.05, x + 0.004, 0.36, doorZ1 - 0.03, CHROME);
        }
        // Emergency lowering pull knob and charger socket.
        b.rod(x1, 0.30, doorZ0 + 0.92, x1 + 0.035, 0.30, doorZ0 + 0.92, 0.02, 8, 0xFFC01818);
        b.box(x1 - 0.30, 0.38, 0.028, x1 - 0.18, 0.45, 0.04, BLACK);
        b.decalFront(x1 - 0.295, 0.385, 0.028, 0.11, 0.06, Decals.PLUG);
        // Fork pockets and tie-down rings.
        for (double z : new double[]{end0 + 0.10, end1 - 0.28}) {
            for (double x : new double[]{x0 - 0.008, x1 + 0.002}) b.box(x, 0.125, z, x + 0.006, 0.185, z + 0.18, BLACK);
        }
        for (double z : new double[]{-0.03, len + 0.03}) {
            for (double x : new double[]{x0 + 0.20, x1 - 0.20}) ring(b, x, 0.30, z, 0.03, 0.006, 8, YELLOW);
        }
        b.decalFront(x0 + 0.26, 0.24, 0.0, 0.08, 0.08, Decals.TIE_DOWN);
        b.decalFront(x1 - 0.34, 0.24, 0.0, 0.08, 0.08, Decals.LIFT_POINT);
        b.decalFront(0.30, 0.38, 0.04, 0.40, 0.07, Decals.HAZARD);
        // Pothole guards under the sides: they drop when the platform leaves the ground.
        double gy = potholes ? 0.025 : 0.10;
        for (double x : new double[]{x0 + 0.145, x1 - 0.195}) {
            b.box(x, gy, end0 - 0.02, x + 0.05, gy + 0.06, end1 + 0.02, STEEL);
            b.box(x + 0.01, gy + 0.06, end0 + 0.04, x + 0.04, 0.13, end0 + 0.08, STEEL);
            b.box(x + 0.01, gy + 0.06, end1 - 0.08, x + 0.04, 0.13, end1 - 0.04, STEEL);
        }
        // Ladder at the back up to the gate.
        for (double x : new double[]{0.24, 0.76}) {
            b.box(x - 0.015, 0.10, len + 0.015, x + 0.015, s.stowDeck() - 0.04, len + 0.05, paint);
        }
        for (double y : new double[]{0.30, 0.62}) {
            b.box(0.255, y, len + 0.015, 0.745, y + 0.03, len + 0.10, DARK);
            b.decalTop(0.26, y + 0.03, len + 0.02, 0.48, 0.07, Decals.CHECKER);
        }
        // Beacon on the rear corner, horn under the tub.
        b.rod(x1 - 0.12, top, len - 0.12, x1 - 0.12, top + 0.03, len - 0.12, 0.04, 8, BLACK);
        b.cyl(x1 - 0.12, top + 0.03, len - 0.12, x1 - 0.12, top + 0.10, len - 0.12, 0.035, 0.028, 10, AMBER, true, MeshKit.PAINT);
        b.rod(0.5, 0.10, 0.30, 0.5, 0.06, 0.30, 0.03, 8, BLACK);
    }

    private static void rtBase(Builder b, Scissor s, int paint) {
        double top = s.chassisTop();
        int paintDark = MeshKit.shade(paint, 0.78f);
        double r = 0.34;
        // Knobby tyres on yellow rims, drive motors, the oscillating front axle.
        for (double z : new double[]{0.75, 3.15}) {
            wheel(b, -0.15, 0.15, r, z, RUBBER, true, Decals.WHEEL_RT, 22);
            wheel(b, 1.85, 2.15, r, z, RUBBER, false, Decals.WHEEL_RT, 22);
            b.rod(0.15, r, z, 1.85, r, z, 0.06, 10, DARK);
            for (double x : new double[]{0.15, 1.72}) b.rod(x, r, z, x + 0.13, r, z, 0.11, 10, DARK);
        }
        b.box(0.70, 0.22, 0.62, 1.30, 0.46, 0.88, DARK);
        // Main frame and the body boxes on each side.
        b.box(0.30, 0.32, 0.30, 1.70, top, 3.60, DARK);
        b.chamferBox(0.18, 0.40, 1.15, 0.58, top, 2.75, 0.03, paint);
        b.chamferBox(1.42, 0.40, 1.15, 1.82, top, 2.75, 0.03, paint);
        b.box(0.20, 0.36, 1.15, 0.56, 0.40, 2.75, paintDark);
        b.box(1.44, 0.36, 1.15, 1.80, 0.40, 2.75, paintDark);
        // Door shut lines, hinges, latches, grilles and the decals.
        for (double z : new double[]{1.15, 1.95, 2.75}) {
            b.box(0.176, 0.44, z - 0.004, 0.18, top - 0.04, z + 0.004, BLACK);
            b.box(1.82, 0.44, z - 0.004, 1.824, top - 0.04, z + 0.004, BLACK);
        }
        for (double z : new double[]{1.85, 2.65}) {
            b.box(0.172, 0.70, z - 0.03, 0.18, 0.78, z, CHROME);
            b.box(1.82, 0.70, z - 0.03, 1.828, 0.78, z, CHROME);
        }
        b.decalLeft(0.179, 0.52, 1.22, 0.62, 0.14, Decals.GRILLE);
        b.decalLeft(0.179, 0.78, 1.26, 0.40, 0.17, Decals.TEXT_4X2);
        b.decalLeft(0.179, 0.72, 2.00, 0.66, 0.17, Decals.BRAND);
        b.decalLeft(0.179, 0.48, 2.06, 0.34, 0.11, Decals.MODEL_SL1418);
        b.decalRight(1.821, 0.50, 1.24, 0.30, 0.15, Decals.GROUND_PANEL);
        b.decalRight(1.821, 0.80, 1.24, 0.40, 0.17, Decals.TEXT_4X2);
        b.decalRight(1.821, 0.72, 2.00, 0.66, 0.17, Decals.BRAND);
        b.decalRight(1.821, 0.48, 2.06, 0.62, 0.14, Decals.GRILLE);
        b.decalRight(1.821, 0.66, 1.62, 0.26, 0.07, Decals.LOWERING);
        // Fuel and hydraulic tanks with their caps.
        b.box(0.24, top, 2.30, 0.52, top + 0.04, 2.70, paintDark);
        b.rod(0.32, top + 0.04, 2.50, 0.32, top + 0.08, 2.50, 0.04, 10, BLACK);
        b.rod(1.62, top, 2.50, 1.62, top + 0.06, 2.50, 0.04, 10, 0xFFE0C020);
        // Exhaust stack and the air filter on the engine side.
        b.rod(1.74, 0.80, 1.40, 1.74, top + 0.30, 1.40, 0.03, 8, DARK);
        b.cyl(1.74, top + 0.30, 1.40, 1.74, top + 0.36, 1.37, 0.03, 0.035, 8, BLACK, true, MeshKit.PAINT);
        b.rod(1.50, top, 1.60, 1.50, top + 0.18, 1.60, 0.07, 10, BLACK);
        // Bumpers, front and rear, with the tie-downs.
        for (double[] end : new double[][]{{0.27, 0.36}, {3.54, 3.63}}) {
            b.box(0.28, 0.34, end[0], 1.72, 0.62, end[1], BLACK);
            for (double x : new double[]{0.50, 1.50}) {
                ring(b, x, 0.48, end[0] < 1 ? end[0] - 0.03 : end[1] + 0.03, 0.04, 0.008, 8, YELLOW);
            }
        }
        b.decalFront(0.70, 0.40, 0.27, 0.60, 0.10, Decals.HAZARD);
        b.decalBack(0.70, 0.40, 3.63, 0.60, 0.10, Decals.HAZARD);
        // Steps at the back up to the gate, with checker treads.
        for (double x : new double[]{0.80, 1.20}) b.box(x - 0.02, 0.20, 3.60, x + 0.02, s.stowDeck() - 0.05, 3.66, paint);
        for (double y : new double[]{0.40, 0.80, 1.20}) {
            b.box(0.80, y, 3.60, 1.20, y + 0.035, 3.74, DARK);
            b.decalTop(0.805, y + 0.0355, 3.605, 0.39, 0.13, Decals.CHECKER);
        }
        // Beacons on the front corners.
        for (double x : new double[]{0.30, 1.70}) {
            b.rod(x, top, 0.42, x, top + 0.04, 0.42, 0.05, 8, BLACK);
            b.cyl(x, top + 0.04, 0.42, x, top + 0.13, 0.42, 0.045, 0.036, 10, AMBER, true, MeshKit.PAINT);
        }
        // Outrigger housings: square boxes at the four corners.
        for (double[] c : outriggerCorners()) {
            b.chamferBox(c[0], 0.30, c[1], c[0] + 0.40, 1.05, c[1] + 0.28, 0.02, paint);
            b.box(c[0] + 0.12, 1.05, c[1] + 0.06, c[0] + 0.28, 1.12, c[1] + 0.22, DARK);
            // Upright lettering on the outer face, read bottom to top, and the crush warning.
            boolean front = c[1] < 1;
            if (front) {
                b.decal(c[0] + 0.12, 0.44, c[1] - 0.001, 0, 1, 0, 1, 0, 0, 0.56, 0.14, Decals.OUTRIGGER, WHITE, false);
                b.decalFront(c[0] + 0.28, 0.34, c[1] - 0.001, 0.09, 0.09, Decals.CRUSH);
            } else {
                b.decal(c[0] + 0.28, 0.44, c[1] + 0.281, 0, 1, 0, -1, 0, 0, 0.56, 0.14, Decals.OUTRIGGER, WHITE, false);
                b.decalBack(c[0] + 0.03, 0.34, c[1] + 0.281, 0.09, 0.09, Decals.CRUSH);
            }
        }
    }

    /** {x0, z0} of each outrigger housing. */
    static double[][] outriggerCorners() {
        return new double[][]{{-0.15, 0.02}, {1.75, 0.02}, {-0.15, 3.60}, {1.75, 3.60}};
    }

    /** Outrigger legs at deploy {@code out} (0 stowed, 1 on the ground). */
    static void outriggers(Builder b, float out) {
        for (double[] c : outriggerCorners()) {
            double cx = c[0] + 0.20, cz = c[1] + 0.14;
            double foot = 0.30 - out * 0.28;
            b.box(cx - 0.07, foot + 0.04, cz - 0.07, cx + 0.07, 0.34, cz + 0.07, CHROME);
            b.box(cx - 0.075, foot + 0.04, cz - 0.075, cx + 0.075, foot + 0.07, cz + 0.075, STEEL);
            b.cyl(cx, foot, cz, cx, foot + 0.04, cz, 0.14, 0.12, 12, BLACK, true, MeshKit.PAINT);
            b.rod(cx, foot + 0.04, cz, cx, foot + 0.08, cz, 0.04, 8, STEEL);
        }
    }

    /** Arm planes across the width: inner zig-zag and outer zig-zag on each side. */
    record Arms(double inner0, double inner1, double outer0, double outer1, double w, double depth, int col, boolean hazard) {
        static Arms of(MachineKind kind) {
            return kind == MachineKind.RT_SCISSOR
                    ? new Arms(0.40, 1.60, 0.30, 1.70, 0.08, 0.13, 0xFF222326, false)
                    : new Arms(0.12, 0.88, 0.04, 0.96, 0.06, 0.085, 0xFF8E9298, true);
        }
    }

    /** The scissor stack at a platform rise: arms, pins, cross tubes and the lift cylinders. */
    static void scissorStack(Builder b, MachineKind kind, double rise) {
        Scissor s = kind.scissor();
        Arms a = Arms.of(kind);
        int n = s.stages();
        double h = s.stage(rise);
        double reach = s.reach(rise);
        double zf = s.fixedZ(), zs = zf - reach;
        double ext = 0.05;
        for (int k = 0; k < n; k++) {
            double y0 = s.pivot() + k * h, y1 = y0 + h;
            // A runs from the fixed end at the bottom to the sliding end at the top, B the other way.
            boolean aInner = k % 2 == 0;
            for (int side = 0; side < 2; side++) {
                double xi = side == 0 ? a.inner0 : a.inner1, xo = side == 0 ? a.outer0 : a.outer1;
                double xa = aInner ? xi : xo, xb = aInner ? xo : xi;
                arm(b, a, xa, zf, y0, zs, y1, ext, side == 0, xa == xo);
                arm(b, a, xb, zs, y0, zf, y1, ext, side == 0, xb == xo);
                // Centre pivot bolt through both arms, with hubs on the outside.
                double zc = (zf + zs) * 0.5, yc = (y0 + y1) * 0.5;
                double out = side == 0 ? -1 : 1;
                b.rod(xi, yc, zc, xo + out * (a.w * 0.5 + 0.015), yc, zc, 0.022, 8, STEEL);
                b.rod(xo + out * a.w * 0.5, yc, zc, xo + out * (a.w * 0.5 + 0.02), yc, zc, 0.045, 10, DARK);
                // End pins: the outer arms join end to end, the inner ones too.
                for (double[] p : new double[][]{{zf, y0}, {zs, y0}, {zf, y1}, {zs, y1}}) {
                    b.rod(xo + out * (a.w * 0.5 + 0.01), p[1], p[0], xo - out * (a.w * 0.5), p[1], p[0], 0.032, 8, STEEL);
                    b.rod(xi + out * (a.w * 0.5), p[1], p[0], xi - out * (a.w * 0.5 + 0.01), p[1], p[0], 0.032, 8, STEEL);
                }
            }
            // Cross tubes tying the left and right inner arms at both ends.
            for (double[] p : new double[][]{{zf, y0}, {zs, y1}}) {
                double py = k % 2 == 0 ? p[1] : (p[1] == y0 ? y1 : y0);
                b.rod(a.inner0 + a.w * 0.5, py, p[0], a.inner1 - a.w * 0.5, py, p[0], 0.028, 8, a.col);
            }
            double cy = k % 2 == 0 ? y1 : y0;
            b.rod(a.inner0 + a.w * 0.5, cy, zs, a.inner1 - a.w * 0.5, cy, zs, 0.026, 8, a.col);
        }
        // Rollers in the chassis track and under the platform at the sliding end.
        double top = s.pivot() + n * h;
        for (double x : new double[]{a.outer0, a.outer1}) {
            double out = x < 1 ? -1 : 1;
            b.rod(x + out * a.w * 0.5, s.pivot(), zs, x + out * (a.w * 0.5 + 0.03), s.pivot(), zs, 0.05, 10, BLACK);
            b.rod(x + out * a.w * 0.5, top, zs, x + out * (a.w * 0.5 + 0.03), top, zs, 0.05, 10, BLACK);
        }
        // Lift cylinders: the barrel pins to a cross tube on the first inner arm near the fixed end, the rod to a
        // cross tube on the inner arm of the stage above. Both tubes sit on the arms, so the cylinders ride with them.
        boolean rt = kind == MachineKind.RT_SCISSOR;
        double t0 = 0.10, t1 = 0.30;
        double[] base = liftMount(s, rise, t0, false), head = liftMount(s, rise, t1, true);
        double[] base0 = liftMount(s, 0, t0, false), head0 = liftMount(s, 0, t1, true);
        double stowed = Math.hypot(head0[0] - base0[0], head0[1] - base0[1]);
        double barrel = stowed * 0.92;
        double r = rt ? 0.065 : 0.05;
        double inX0 = a.inner0 + a.w * 0.5, inX1 = a.inner1 - a.w * 0.5;
        for (double[] m : new double[][]{base, head}) {
            b.rod(inX0, m[0], m[1], inX1, m[0], m[1], rt ? 0.045 : 0.035, 10, a.col);
        }
        double[] xs = rt ? new double[]{0.78, 1.22} : new double[]{0.5};
        double py = base[0], pz = base[1], qy = head[0], qz = head[1];
        double dy = qy - py, dz = qz - pz, len = Math.max(1.0e-4, Math.hypot(dy, dz));
        double ey = dy / len, ez = dz / len;
        double by = py + ey * barrel, bz = pz + ez * barrel;
        for (double x : xs) {
            // Clevis lugs on the cross tubes.
            for (double side : new double[]{-1, 1}) {
                double lx = x + side * (r + 0.012);
                b.beam(lx, py, pz, lx, py + ey * 0.12, pz + ez * 0.12, 1, 0, 0, r * 1.6, 0.014, a.col);
                b.beam(lx, qy, qz, lx, qy - ey * 0.10, qz - ez * 0.10, 1, 0, 0, r * 1.3, 0.014, a.col);
            }
            b.rod(x - r - 0.03, py, pz, x + r + 0.03, py, pz, r * 0.45, 8, STEEL);
            b.rod(x - r - 0.03, qy, qz, x + r + 0.03, qy, qz, r * 0.4, 8, STEEL);
            // Barrel with its gland, rod and rod eye.
            double sy = py + ey * 0.06, sz = pz + ez * 0.06;
            b.rod(x, sy, sz, x, by, bz, r, 12, rt ? 0xFF2E3034 : 0xFF6E7378);
            b.rod(x, by - ey * 0.05, bz - ez * 0.05, x, by + ey * 0.02, bz + ez * 0.02, r * 1.12, 12, BLACK);
            b.rod(x, by, bz, x, qy - ey * 0.05, qz - ez * 0.05, r * 0.5, 10, CHROME);
            b.rod(x, qy - ey * 0.07, qz - ez * 0.07, x, qy, qz, r * 0.7, 8, STEEL);
            // Hydraulic hose from the chassis to the barrel port.
            b.rod(x + r, sy + ey * 0.10, sz + ez * 0.10, x + r + 0.02, s.chassisTop() + 0.02, pz + 0.25, 0.012, 6, HOSE);
        }
    }

    /**
     * Mount point (y, z) on an inner arm. {@code upper} false: on the first stage's inner arm, a fraction t from its
     * fixed foot. True: on the second stage's inner arm, a fraction t from its sliding foot.
     */
    private static double[] liftMount(Scissor s, double rise, double t, boolean upper) {
        double h = s.stage(rise), reach = s.reach(rise), zf = s.fixedZ(), zs = zf - reach;
        if (!upper) return new double[]{s.pivot() + h * t, zf - reach * t};
        double y1 = s.pivot() + h;
        return new double[]{y1 + h * t, zs + reach * t};
    }

    private static void arm(Builder b, Arms a, double x, double za, double ya, double zb, double yb, double ext,
                            boolean left, boolean outer) {
        double dz = zb - za, dy = yb - ya, len = Math.sqrt(dz * dz + dy * dy);
        double ux = dz / len, uy = dy / len;
        double pz = za - ux * ext, py = ya - uy * ext, qz = zb + ux * ext, qy = yb + uy * ext;
        b.beam(x, py, pz, x, qy, qz, 1, 0, 0, a.depth, a.w, a.col);
        if (!outer) return;
        // Stickers on the outer face: hazard bands on the slab lift, small tags on the rough-terrain one.
        double face = x + (left ? -1 : 1) * (a.w * 0.5 + 0.0015);
        // Along the arm, read left to right from outside: +z on the left side, -z on the right side.
        double dzu = ux, dyu = uy;
        if ((left && dzu < 0) || (!left && dzu > 0)) {
            dzu = -dzu;
            dyu = -dyu;
        }
        double vy = left ? dzu : -dzu, vz = left ? -dyu : dyu;
        double hh = a.depth * 0.76;
        double w = a.hazard ? len * 0.28 : 0.30;
        for (double t : a.hazard ? new double[]{0.20, 0.64} : new double[]{0.32}) {
            double cz = za + dz * t, cy = ya + dy * t;
            double oz = cz - dzu * w * 0.5 - vz * hh * 0.5, oy = cy - dyu * w * 0.5 - vy * hh * 0.5;
            b.decal(face, oy, oz, 0, dyu, dzu, 0, vy, vz, w, hh, a.hazard ? Decals.HAZARD : Decals.ARM_TAG, WHITE, false);
        }
    }

    /** Platform: frame, deck, toe boards, rails, gate, control box. Its origin is the deck top. */
    static void scissorPlatform(Builder b, MachineKind kind, int paint) {
        Scissor s = kind.scissor();
        boolean rt = kind == MachineKind.RT_SCISSOR;
        double x0 = s.deckX0(), x1 = s.deckX1(), z0 = s.deckZ0(), z1 = s.deckZ1();
        double fr = s.frame();
        int rail = rt ? paint : paint;
        int paintDark = MeshKit.shade(paint, 0.80f);
        // Frame and the skirt panels with the brand and model on them.
        b.box(x0, -fr, z0, x1, -0.04, z1, paint);
        b.box(x0 + 0.04, -fr - 0.04, z0 + 0.06, x1 - 0.04, -fr, z1 - 0.06, paintDark);
        b.box(x0, -0.04, z0, x1, 0, z1, DARK);
        b.decalTop(x0 + 0.02, 0.0005, z0 + 0.02, x1 - x0 - 0.04, z1 - z0 - 0.04, Decals.CHECKER);
        if (rt) {
            b.decalLeft(x0 - 0.002, -fr + 0.03, z0 + 0.50, 1.20, 0.13, Decals.BRAND);
            b.decalRight(x1 + 0.002, -fr + 0.03, z1 - 1.70, 1.20, 0.13, Decals.BRAND);
            b.decalLeft(x0 - 0.002, -fr + 0.03, z0 + 2.10, 0.60, 0.14, Decals.MODEL_SL1418);
            b.decalRight(x1 + 0.002, -fr + 0.03, z0 + 1.00, 0.60, 0.14, Decals.MODEL_SL1418);
            b.decalLeft(x0 - 0.002, -fr + 0.02, z1 - 0.70, 0.60, 0.07, Decals.HAZARD);
            b.decalRight(x1 + 0.002, -fr + 0.02, z0 + 0.10, 0.60, 0.07, Decals.HAZARD);
        } else {
            b.decalBack(x0 + 0.06, -fr + 0.03, z1 + 0.002, 0.32, 0.08, Decals.MODEL_SL08);
            b.decalBack(x0 + 0.40, -fr + 0.03, z1 + 0.002, 0.30, 0.075, Decals.BRAND);
            b.decalLeft(x0 - 0.002, -fr + 0.03, z0 + 0.20, 0.60, 0.15, Decals.BRAND);
            b.decalRight(x1 + 0.002, -fr + 0.03, z0 + 0.20, 0.60, 0.15, Decals.BRAND);
            b.decalLeft(x0 - 0.002, -fr + 0.03, z0 + 0.90, 0.36, 0.09, Decals.MODEL_SL08);
            b.decalRight(x1 + 0.002, -fr + 0.03, z0 + 1.00, 0.36, 0.09, Decals.MODEL_SL08);
        }
        // Platform brackets for the top pins and the sliding track underneath.
        Arms a = Arms.of(kind);
        for (double x : new double[]{a.outer0, a.outer1}) {
            b.box(x - a.w * 0.5 - 0.03, -fr - 0.06, s.fixedZ() - 0.08, x + a.w * 0.5 + 0.03, -fr, s.fixedZ() + 0.08, DARK);
            double side = x < 1 ? -1 : 1;
            b.box(x + side * (a.w * 0.5) - 0.01, -fr - 0.03, z0 + 0.10, x + side * (a.w * 0.5 + 0.04), -fr, s.fixedZ() - 0.20, STEEL);
        }
        // Toe boards on the sides and back.
        double t = 0.15;
        b.box(x0, 0, z0 + 0.0, x0 + 0.015, t, z1, paint);
        b.box(x1 - 0.015, 0, z0, x1, t, z1, paint);
        double mid = (x0 + x1) * 0.5, gate = kind.gate();
        b.box(x0, 0, z1 - 0.015, mid - gate, t, z1, paint);
        b.box(mid + gate, 0, z1 - 0.015, x1, t, z1, paint);
        // Rails: posts, mid and top rails, in round tube.
        double rr = rt ? 0.024 : 0.021, top = s.rail();
        double[] zPosts = posts(z0 + 0.03, z1 - 0.03, rt ? 4 : 2);
        for (double x : new double[]{x0 + 0.03, x1 - 0.03}) {
            for (double z : zPosts) b.rod(x, 0, z, x, top, z, rr, 8, rail);
            b.rod(x, top, z0 + 0.03, x, top, z1 - 0.03, rr, 8, rail);
            b.rod(x, top * 0.52, z0 + 0.03, x, top * 0.52, z1 - 0.03, rr * 0.9, 8, rail);
        }
        // Back rail either side of the gate opening.
        for (double[] seg : new double[][]{{x0 + 0.03, mid - gate}, {mid + gate, x1 - 0.03}}) {
            b.rod(seg[0], top, z1 - 0.03, seg[1], top, z1 - 0.03, rr, 8, rail);
            b.rod(seg[0], top * 0.52, z1 - 0.03, seg[1], top * 0.52, z1 - 0.03, rr * 0.9, 8, rail);
            b.rod(seg[1], 0, z1 - 0.03, seg[1], top, z1 - 0.03, rr, 8, rail);
        }
        // Self-closing gate, swung in against the left of the opening, with its spring hinge.
        double gx = mid - gate, gz = z1 - 0.03;
        b.rod(gx + 0.02, top - 0.06, gz, gx + 0.02, top * 0.45, gz - gate * 1.6, rr * 0.85, 8, rail);
        b.rod(gx + 0.02, top - 0.06, gz, gx + 0.02, top - 0.06, gz - gate * 1.6, rr * 0.85, 8, rail);
        b.rod(gx + 0.02, top * 0.52, gz, gx + 0.02, top * 0.52, gz - gate * 1.6, rr * 0.85, 8, rail);
        b.rod(gx + 0.02, top * 0.45, gz - gate * 1.6, gx + 0.02, top - 0.06, gz - gate * 1.6, rr * 0.85, 8, rail);
        b.rod(gx + 0.02, top * 0.62, gz, gx + 0.02, top * 0.72, gz, rr * 1.6, 8, BLACK);
        // Lanyard anchors, the capacity plate and a socket on the platform.
        for (double z : new double[]{z0 + 0.40, z1 - 0.40}) {
            ring(b, x0 + 0.05, top * 0.52 + 0.06, z, 0.03, 0.006, 8, YELLOW);
            ring(b, x1 - 0.05, top * 0.52 + 0.06, z, 0.03, 0.006, 8, YELLOW);
        }
        b.box(x1 - 0.06, top * 0.52 - 0.10, z1 - 0.70, x1 - 0.035, top * 0.52 - 0.02, z1 - 0.50, 0xFFF2F2EE);
        b.decal(x1 - 0.061, top * 0.52 - 0.095, z1 - 0.505, 0, 0, -1, 0, 1, 0, 0.19, 0.07, Decals.CAPACITY, WHITE, false);
        b.box(x0 + 0.035, 0.20, z1 - 0.30, x0 + 0.06, 0.30, z1 - 0.18, BLACK);
        b.decalLeft(x0 + 0.0345, 0.21, z1 - 0.29, 0.10, 0.06, Decals.PLUG);
        // Manual canister on the rail.
        b.rod(x0 + 0.06, top * 0.52 + 0.02, z1 - 0.70, x0 + 0.06, top * 0.52 + 0.30, z1 - 0.70, 0.045, 10, BLACK);
        b.decal(x0 + 0.106, top * 0.52 + 0.06, z1 - 0.735, 0, 0, 1, 0, 1, 0, 0.07, 0.20, Decals.MANUAL, WHITE, false);
        // Platform control box hung on the front of the extension: it rides with the deck, see scissorDeck.
    }

    private static double[] posts(double z0, double z1, int inner) {
        double[] out = new double[inner + 2];
        for (int i = 0; i <= inner + 1; i++) out[i] = z0 + (z1 - z0) * i / (inner + 1);
        return out;
    }

    /** Extension deck in its stowed place; the renderer slides it out. Its origin is the main deck top. */
    static void scissorDeck(Builder b, MachineKind kind, int paint) {
        Scissor s = kind.scissor();
        boolean rt = kind == MachineKind.RT_SCISSOR;
        double x0 = s.deckX0() + 0.05, x1 = s.deckX1() - 0.05, z0 = s.deckZ0(), len = s.extension() + 0.12;
        double z1 = z0 + len;
        double rr = rt ? 0.024 : 0.021, top = s.rail() + 0.02;
        b.box(x0, 0.002, z0, x1, 0.035, z1, DARK);
        b.decalTop(x0 + 0.01, 0.0355, z0 + 0.01, x1 - x0 - 0.02, len - 0.02, Decals.CHECKER);
        b.box(x0, 0.002, z0, x1, 0.16, z0 + 0.015, paint);
        b.decalFront(x0 + 0.04, 0.03, z0 - 0.001, Math.min(0.5, x1 - x0 - 0.08), 0.09, Decals.HAZARD);
        // Front rail and the side rails that nest inside the platform ones.
        double xl = x0 + 0.02, xr = x1 - 0.02;
        b.rod(xl, top, z0 + 0.02, xr, top, z0 + 0.02, rr, 8, paint);
        b.rod(xl, top * 0.52, z0 + 0.02, xr, top * 0.52, z0 + 0.02, rr * 0.9, 8, paint);
        for (double x : new double[]{xl, xr}) {
            b.rod(x, 0.03, z0 + 0.02, x, top, z0 + 0.02, rr, 8, paint);
            b.rod(x, top, z0 + 0.02, x, top, z1, rr, 8, paint);
            b.rod(x, top * 0.52, z0 + 0.02, x, top * 0.52, z1, rr * 0.9, 8, paint);
            b.rod(x, 0.03, z1, x, top, z1, rr, 8, paint);
        }
        if (rt) b.rod(xl, top * 0.52, z0 + 0.02 + len * 0.5, xl, top, z0 + 0.02 + len * 0.5, rr, 8, paint);
        // Locking pedal and handle.
        b.box((x0 + x1) / 2 - 0.05, 0.035, z0 + 0.10, (x0 + x1) / 2 + 0.05, 0.06, z0 + 0.18, YELLOW);
        // Platform control box hung on the front rail: e-stop, joystick, mode buttons.
        double cx = x1 - 0.22, cy = top - 0.20, cz = z0 + 0.02;
        b.box(cx - 0.12, cy - 0.10, cz + 0.01, cx + 0.12, cy + 0.12, cz + 0.17, 0xFF34373B);
        b.box(cx - 0.125, cy + 0.12, cz + 0.01, cx + 0.125, cy + 0.14, cz + 0.17, BLACK);
        b.decal(cx + 0.115, cy + 0.1405, cz + 0.02, -1, 0, 0, 0, 0, 1, 0.23, 0.14, Decals.PLATFORM_PANEL, WHITE, false);
        b.rod(cx - 0.06, cy + 0.14, cz + 0.12, cx - 0.06, cy + 0.26, cz + 0.12, 0.012, 6, BLACK);
        b.rod(cx - 0.06, cy + 0.26, cz + 0.12, cx - 0.06, cy + 0.31, cz + 0.12, 0.022, 8, BLACK);
        b.rod(cx - 0.06, cy + 0.25, cz + 0.105, cx - 0.06, cy + 0.27, cz + 0.095, 0.01, 6, YELLOW);
        b.rod(cx + 0.06, cy + 0.14, cz + 0.06, cx + 0.06, cy + 0.17, cz + 0.06, 0.03, 10, 0xFFD02018);
        b.box(cx - 0.12, cy + 0.04, cz + 0.005, cx - 0.06, cy + 0.08, cz + 0.012, BLACK);
        // Coiled cable from the box down to the deck.
        for (int i = 0; i < 6; i++) {
            double yy = cy - 0.12 - i * 0.05;
            ring(b, cx + 0.06, yy, cz + 0.10, 0.025, 0.006, 8, BLACK);
        }
    }

    // ================================================================== shared

    /** Wheel across x from x0 to x1, centred at (y = r, z). The outer face carries the printed tyre and rim. */
    static void wheel(Builder b, double x0, double x1, double r, double z, int tyre, boolean outerLeft, MeshKit.Rect face, int n) {
        double inset = 0.012;
        b.cyl(x0 + inset, r, z, x1 - inset, r, z, r, r, n, tyre, false, MeshKit.PAINT);
        // Rounded shoulders.
        b.cyl(x0, r, z, x0 + inset, r, z, r * 0.94, r, n, tyre, false, MeshKit.PAINT);
        b.cyl(x1 - inset, r, z, x1, r, z, r, r * 0.94, n, tyre, false, MeshKit.PAINT);
        // Tread blocks across the crown.
        for (int i = 0; i < n; i++) {
            double a = (i + 0.5) * Math.PI * 2 / n;
            double cy = r + Math.sin(a) * r, cz = z + Math.cos(a) * r;
            if (i % 2 == 0) b.box(x0 + inset * 2, cy - 0.004, cz - 0.004, x1 - inset * 2, cy + 0.004, cz + 0.004, MeshKit.shade(tyre, 0.8f));
        }
        double outX = outerLeft ? x0 - 0.004 : x1 + 0.004;
        double inX = outerLeft ? x1 + 0.004 : x0 - 0.004;
        b.disc(outX, r, z, outerLeft ? -1 : 1, 0, 0, r * 0.94, n, WHITE, MeshKit.DECAL, face);
        b.disc(inX, r, z, outerLeft ? 1 : -1, 0, 0, r * 0.94, n, MeshKit.shade(tyre, 0.9f), MeshKit.PAINT, null);
    }

    /** Front wheel arch: a curved fender over a wheel, across x from x0 to x1. */
    private static void fender(Builder b, double x0, double x1, double z, double r, double topY, int paint) {
        int n = 8;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * i / n, a1 = Math.PI * (i + 1) / n;
            double y0 = 0.345 + Math.sin(a0) * r, z0 = z + Math.cos(a0) * r;
            double y1 = 0.345 + Math.sin(a1) * r, z1 = z + Math.cos(a1) * r;
            // One quad only: the render type draws both sides, a second one in the same place would flicker.
            b.quad(x0, y0, z0, x1, y0, z0, x1, y1, z1, x0, y1, z1, paint, MeshKit.PAINT);
            // Side cheeks up to the cowl.
            b.quad(x0, y0, z0, x0, y1, z1, x0, topY, z1, x0, topY, z0, paint, MeshKit.PAINT);
            b.quad(x1, y0, z0, x1, topY, z0, x1, topY, z1, x1, y1, z1, paint, MeshKit.PAINT);
        }
        b.box(x0, topY - 0.01, z - r, x1, topY, z + r, paint);
    }

    /** Rounded vertical corner of radius r and height h, quarter starting at {@code fromDeg}. */
    private static void corner(Builder b, double r, double h, double fromDeg, int col) {
        int n = 4;
        for (int i = 0; i < n; i++) {
            double a0 = Math.toRadians(fromDeg + 90.0 * i / n), a1 = Math.toRadians(fromDeg + 90.0 * (i + 1) / n);
            double x0 = Math.cos(a0) * r, z0 = -Math.sin(a0) * r, x1 = Math.cos(a1) * r, z1 = -Math.sin(a1) * r;
            b.quad(x0, 0, z0, x1, 0, z1, x1, h, z1, x0, h, z0, col, MeshKit.PAINT);
            b.quad(0, h, 0, x0, h, z0, x1, h, z1, 0, h, 0, col, MeshKit.PAINT);
            b.quad(0, 0, 0, x1, 0, z1, x0, 0, z0, 0, 0, 0, col, MeshKit.PAINT);
        }
    }

    /** Ring in the plane facing z, as a polygon of small tubes. */
    static void ring(Builder b, double cx, double cy, double cz, double r, double t, int n, int col) {
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            b.tube(cx + Math.cos(a0) * r, cy + Math.sin(a0) * r, cz, cx + Math.cos(a1) * r, cy + Math.sin(a1) * r, cz, t, 4, col);
        }
    }

    static int paint(net.minecraft.world.item.DyeColor c) {
        return switch (c) {
            case WHITE -> 0xFFE6E6E2;
            case ORANGE -> 0xFFE8701A;
            case MAGENTA -> 0xFFB0349E;
            case LIGHT_BLUE -> 0xFF4A9AD8;
            case YELLOW -> 0xFFF0B418;
            case LIME -> 0xFF7CC02A;
            case PINK -> 0xFFE07AA0;
            case GRAY -> 0xFF55595E;
            case LIGHT_GRAY -> 0xFF9A9EA2;
            case CYAN -> 0xFF169C9C;
            case PURPLE -> 0xFF6B2FA0;
            case BLUE -> 0xFF1F4FA8;
            case BROWN -> 0xFF6E4628;
            case GREEN -> 0xFF2E8B3A;
            case RED -> 0xFFC8231E;
            case BLACK -> 0xFF2A2A2C;
        };
    }
}
