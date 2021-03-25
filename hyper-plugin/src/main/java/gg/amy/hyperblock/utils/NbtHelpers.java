package gg.amy.hyperblock.utils;

import io.netty.buffer.ByteBufInputStream;
import net.minecraft.server.v1_16_R3.*;
import org.spigotmc.LimitStream;

import java.io.*;

/**
 * @author amy
 * @since 3/23/21.
 */
@SuppressWarnings({"SameParameterValue", "TypeMayBeWeakened"})
public final class NbtHelpers {
    private NbtHelpers() {
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
