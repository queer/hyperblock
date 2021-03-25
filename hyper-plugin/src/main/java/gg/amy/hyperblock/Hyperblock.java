package gg.amy.hyperblock;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import gg.amy.hyperblock.bukkit.SkyblockChunkGenerator;
import gg.amy.mc.cardboard.Cardboard;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.management.ManagementFactory;

/**
 * @author amy
 * @since 3/23/21.
 */
public class Hyperblock extends Cardboard {
    public Hyperblock() {
        try {
            final VirtualMachine m = VirtualMachine.attach(ManagementFactory.getRuntimeMXBean().getPid() + "");
            m.loadAgent(System.getProperty("user.dir") + "/plugins/hyperblock.jar");
        } catch(final AttachNotSupportedException | AgentInitializationException | AgentLoadException | IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull final String worldName, @Nullable final String id) {
        return new SkyblockChunkGenerator();
    }
}
