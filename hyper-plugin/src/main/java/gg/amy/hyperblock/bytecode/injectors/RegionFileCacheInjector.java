package gg.amy.hyperblock.bytecode.injectors;

import gg.amy.hyperblock.bytecode.Injector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author amy
 * @since 3/22/21.
 */
public class RegionFileCacheInjector extends Injector {
    private static final String TARGET = $("net.minecraft.server.v1_16_R3.RegionFileCache");

    public RegionFileCacheInjector() {
        super(TARGET);
    }

    @Override
    protected void inject(final ClassReader cr, final ClassNode cn) {
        injectLoader(cr, cn);
        injectSaver(cr, cn);
    }

    private void injectLoader(@SuppressWarnings("unused") final ClassReader cr, final ClassNode cn) {
        final var readMethod = cn.methods.stream()
                .filter(mn -> mn.name.equals("read"))
                .findFirst();
        if(readMethod.isEmpty()) {
            throw new IllegalStateException("Couldn't find RegionFileCache#read(ChunkCoordIntPair)!?");
        }
        final var mn = readMethod.get();

        final InsnList insns = new InsnList();
        // Directory storing the region files. We use this as a lookup key
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new FieldInsnNode(GETFIELD, $(TARGET), "b", $$(File.class)));
        insns.add(new VarInsnNode(ASTORE, 2));

        // File
        insns.add(new VarInsnNode(ALOAD, 2));
        // Chunk coords
        insns.add(new VarInsnNode(ALOAD, 1));

        insns.add(new MethodInsnNode(INVOKESTATIC, $("gg.amy.hyperblock.bukkit.WorldStorageBackend"), "read",
                String.format(
                        "(%s%s)%s",
                        $$(File.class),
                        $$("net.minecraft.server.v1_16_R3.ChunkCoordIntPair"),
                        $$("net.minecraft.server.v1_16_R3.NBTTagCompound")
                ),
                false));
        insns.add(new InsnNode(ARETURN));
        mn.instructions.clear();
        mn.instructions.add(insns);
        // These methods have try/catch normally, so we make it go away
        mn.tryCatchBlocks.clear();
        // Dedup local variables
        mn.localVariables = List.copyOf(mn.localVariables.stream().<Map<String, LocalVariableNode>>collect(HashMap::new, (m, e) -> m.put(e.name, e), Map::putAll).values());
        System.out.println(">> hyper-agent: inject: loader");
    }

    private void injectSaver(@SuppressWarnings("unused") final ClassReader cr, final ClassNode cn) {
        final var writeMethod = cn.methods.stream()
                .filter(mn -> mn.name.equals("write"))
                .findFirst();
        if(writeMethod.isEmpty()) {
            throw new IllegalStateException("Couldn't find RegionFileCache#write(ChunkCoordIntPair, NBTTagCompound)!?");
        }

        final var mn = writeMethod.get();
        final InsnList insns = new InsnList();
        // Directory storing the region files. We use this as a lookup key
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new FieldInsnNode(GETFIELD, $(TARGET), "b", $$(File.class)));
        insns.add(new VarInsnNode(ASTORE, 3));

        // File
        insns.add(new VarInsnNode(ALOAD, 3));
        // Chunk coords
        insns.add(new VarInsnNode(ALOAD, 1));
        // NBT
        insns.add(new VarInsnNode(ALOAD, 2));

        insns.add(new MethodInsnNode(INVOKESTATIC, $("gg.amy.hyperblock.bukkit.WorldStorageBackend"), "write",
                String.format(
                        "(%s%s%s)%s",
                        $$(File.class),
                        $$("net.minecraft.server.v1_16_R3.ChunkCoordIntPair"),
                        $$("net.minecraft.server.v1_16_R3.NBTTagCompound"),
                        "V"
                ),
                false));
        insns.add(new InsnNode(RETURN));
        mn.instructions.clear();
        mn.instructions.add(insns);
        // These methods have try/catch normally, so we make it go away
        mn.tryCatchBlocks.clear();
        // Dedup local variables
        mn.localVariables = List.copyOf(mn.localVariables.stream().<Map<String, LocalVariableNode>>collect(HashMap::new, (m, e) -> m.put(e.name, e), Map::putAll).values());
        System.out.println(">> hyper-agent: inject: saver");
    }
}
