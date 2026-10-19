package com.nailec.stagetents.client;

import com.nailec.stagetents.furniture.MachineKind;
import com.nailec.stagetents.furniture.MachineKind.Scissor;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Dev tool: dumps a whole machine at a pose for offline previews. One line per vertex:
 * material x y z argb u v. Args: dir kind rise tilt load out deck.
 */
public final class MachineDump {
    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]);
        Files.createDirectories(dir);
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        if (args[1].equals("CUBE")) {
            // Orientation check: a cube with the brand on each face, and an arrow on the +x side.
            MeshKit.Builder c = new MeshKit.Builder();
            c.box(0, 0, 0, 1, 1, 1, 0xFF808080);
            c.decalFront(0.1, 0.4, -0.001, 0.8, 0.2, Decals.BRAND);
            c.decalBack(0.1, 0.4, 1.001, 0.8, 0.2, Decals.BRAND);
            c.decalLeft(-0.001, 0.4, 0.1, 0.8, 0.2, Decals.BRAND);
            c.decalRight(1.001, 0.4, 0.1, 0.8, 0.2, Decals.BRAND);
            c.decalTop(0.1, 1.001, 0.1, 0.8, 0.2, Decals.BRAND);
            c.box(1.0, 0.0, 0.0, 1.3, 0.1, 0.1, 0xFFFF0000);
            c.box(0.0, 0.0, -0.3, 0.1, 0.1, 0.0, 0xFF0000FF);
            write(c.finish(), dir.resolve("cube.txt"));
            return;
        }
        MachineKind kind = MachineKind.valueOf(args[1]);
        double rise = Double.parseDouble(args[2]);
        int tilt = Integer.parseInt(args[3]);
        int load = Integer.parseInt(args[4]);
        float out = Float.parseFloat(args[5]);
        float deck = Float.parseFloat(args[6]);
        int paint = MachineMeshes.paint(kind.color);
        MeshKit.Builder b = new MeshKit.Builder();
        if (kind == MachineKind.FORKLIFT) {
            MachineMeshes.forkliftBody(b, paint);
            MachineMeshes.forkliftTiltRams(b, tilt);
            b.push().translate(0, 0.30, 1.30).rotX(tilt).translate(0, -0.30, -1.30);
            MachineMeshes.forkliftOuterMast(b, paint);
            MachineMeshes.forkliftInnerMast(b, MachineKind.innerRise(rise));
            MachineMeshes.forkliftCarriage(b, rise, load);
            MachineMeshes.forkliftChains(b, rise, MachineKind.innerRise(rise));
            b.pop();
        } else {
            Scissor s = kind.scissor();
            MachineMeshes.scissorBase(b, kind, paint, rise > 0.02);
            if (kind.hasOutriggers()) MachineMeshes.outriggers(b, out);
            MachineMeshes.scissorStack(b, kind, rise);
            b.push().translate(0, s.deck(rise), 0);
            MachineMeshes.scissorPlatform(b, kind, paint);
            b.translate(0, 0, -s.extension() * deck);
            MachineMeshes.scissorDeck(b, kind, paint);
            b.pop();
        }
        MeshKit.Mesh m = b.finish();
        Path file = dir.resolve(kind.name().toLowerCase(Locale.ROOT) + ".txt");
        write(m, file);
        System.out.println(kind + " quads " + m.quads + " -> " + file);
    }

    private static void write(MeshKit.Mesh m, Path file) throws Exception {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file))) {
            for (int q = 0; q < m.quads; q++) {
                for (int v = 0; v < 4; v++) {
                    int p = q * 12 + v * 3;
                    w.printf(Locale.ROOT, "%d %.4f %.4f %.4f %08X %.5f %.5f%n", m.mat[q], m.xyz[p], m.xyz[p + 1], m.xyz[p + 2],
                            m.color[q], m.uv[q * 8 + v * 2], m.uv[q * 8 + v * 2 + 1]);
                }
            }
        }
    }
}
