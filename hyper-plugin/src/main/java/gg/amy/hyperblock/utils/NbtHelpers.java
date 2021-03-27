package gg.amy.hyperblock.utils;

import io.netty.buffer.ByteBufInputStream;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.LimitStream;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.ArrayList;

/**
 * @author amy
 * @since 3/23/21.
 */
@SuppressWarnings({"SameParameterValue", "TypeMayBeWeakened"})
public final class NbtHelpers {
    private NbtHelpers() {
    }

    public static NBTTagCompound serializePlayerInventory(@Nonnull final Player p) {
        final var inv = new NBTTagList();
        final var armour = new NBTTagList();
        final var extra = new NBTTagList();
        for(final var i : p.getInventory().getContents()) {
            final var compound = new NBTTagCompound();
            CraftItemStack.asNMSCopy(i).save(compound);
            inv.add(compound);
        }
        for(final var i : p.getInventory().getArmorContents()) {
            final var compound = new NBTTagCompound();
            CraftItemStack.asNMSCopy(i).save(compound);
            armour.add(compound);
        }
        for(final var i : p.getInventory().getExtraContents()) {
            final var compound = new NBTTagCompound();
            CraftItemStack.asNMSCopy(i).save(compound);
            extra.add(compound);
        }
        final var compound = new NBTTagCompound();
        compound.set("inv", inv);
        compound.set("armour", armour);
        compound.set("extra", extra);
        return compound;
    }

    public static void deserializePlayerInventory(@Nonnull final Player p, @Nonnull final NBTTagCompound nbt) {
        final var inv = new ArrayList<ItemStack>();
        final var armour = new ArrayList<ItemStack>();
        final var extra = new ArrayList<ItemStack>();
        for(final var i : nbt.getList("inv", 9)) {
            final var bis = CraftItemStack.asBukkitCopy(net.minecraft.server.v1_16_R3.ItemStack.a((NBTTagCompound) i));
            inv.add(bis);
        }
        for(final var i : nbt.getList("armour", 9)) {
            final var bis = CraftItemStack.asBukkitCopy(net.minecraft.server.v1_16_R3.ItemStack.a((NBTTagCompound) i));
            armour.add(bis);
        }
        for(final var i : nbt.getList("extra", 9)) {
            final var bis = CraftItemStack.asBukkitCopy(net.minecraft.server.v1_16_R3.ItemStack.a((NBTTagCompound) i));
            extra.add(bis);
        }
        p.getInventory().setContents(inv.toArray(new ItemStack[0]));
        p.getInventory().setArmorContents(armour.toArray(new ItemStack[0]));
        p.getInventory().setExtraContents(extra.toArray(new ItemStack[0]));
    }

    public static NBTTagCompound read(final InputStream inputstream) {
        try(final DataInputStream dis = new DataInputStream(inputstream)) {
            return readNbtCompoundFromInput(dis, NBTReadLimiter.a);
        } catch(final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static void write(final NBTTagCompound tag, final OutputStream outputstream) {
        try(final DataOutputStream dis = new DataOutputStream(outputstream)) {
            writeTagToStream(tag, dis);
        } catch(final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeTagToStream(final NBTTagCompound tag, final DataOutput out) throws Exception {
        writeTagToDataOutput(tag, out);
    }

    private static void writeTagToDataOutput(final NBTBase nbt, final DataOutput out) throws Exception {
        out.writeByte(nbt.getTypeId());
        if(nbt.getTypeId() != 0) {
            out.writeUTF("");
            nbt.write(out);
        }
    }

    private static NBTTagCompound readNbtCompoundFromInput(DataInput di, final NBTReadLimiter readLimiter) throws Exception {
        if(di instanceof ByteBufInputStream) {
            di = new DataInputStream(new LimitStream((InputStream) di, readLimiter));
        }
        final NBTBase nbtbase = readNextTag(di, 0, readLimiter);
        if(nbtbase instanceof NBTTagCompound) {
            return (NBTTagCompound) nbtbase;
        } else {
            throw new IOException("Root tag must be a named compound tag");
        }
    }

    private static NBTBase readNextTag(final DataInput di, final int i, final NBTReadLimiter limiter) throws Exception {
        final byte maybeEnd = di.readByte();
        if(maybeEnd == 0) {
            return NBTTagEnd.b;
        } else {
            di.readUTF();
            return NBTTagTypes.a(maybeEnd).b(di, i, limiter);
        }
    }
}
