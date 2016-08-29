package peergos.shared.user.fs;

import peergos.shared.ipfs.api.*;
import peergos.shared.crypto.*;
import peergos.shared.crypto.symmetric.SymmetricKey;
import peergos.shared.ipfs.api.Base58;
import peergos.shared.util.*;

import java.io.*;
import java.util.*;

public class ReadableFilePointer {
    public final UserPublicKey owner, writer;
    public final byte[] mapKey;
    public final SymmetricKey baseKey;

    public ReadableFilePointer(UserPublicKey owner, UserPublicKey writer, byte[] mapKey, SymmetricKey baseKey) {
        this.owner = owner;
        this.writer = writer;
        this.mapKey = mapKey;
        this.baseKey = baseKey;
    }

    public Location getLocation() {
        return new Location(owner, writer, mapKey);
    }

    public ReadableFilePointer withBaseKey(SymmetricKey newBaseKey) {
        return new ReadableFilePointer(owner, writer, mapKey, newBaseKey);
    }

    public ReadableFilePointer withWritingKey(UserPublicKey writingKey) {
        return new ReadableFilePointer(owner, writingKey, mapKey, baseKey);
    }

    public byte[] serialize() {
        try {
            DataSink bout = new DataSink();
            bout.writeArray(owner.getPublicKeys());
            bout.writeByte(this.isWritable() ? 1 : 0);
            writer.serialize(bout);
            bout.writeArray(mapKey);
            bout.writeArray(baseKey.serialize());
            return bout.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ReadableFilePointer deserialize(byte[] arr) throws IOException {
        DataSource bin = new DataSource(arr);
        UserPublicKey owner = UserPublicKey.fromByteArray(bin.readArray());
        UserPublicKey writer = User.deserialize(bin);
        byte[] mapKey = bin.readArray();
        byte[] rootDirKeySecret = bin.readArray();
        return new ReadableFilePointer(owner, writer, mapKey, SymmetricKey.deserialize(rootDirKeySecret));
    }

    public ReadableFilePointer readOnly() {
        if (!isWritable())
            return this;
        UserPublicKey publicWriter = UserPublicKey.fromPublicKeys(this.writer.getPublicKeys());
        return new ReadableFilePointer(this.owner, publicWriter, this.mapKey, this.baseKey);
    }

    public boolean isWritable() {
        return this.writer instanceof User;
    }

    public String toLink() {
        return "#" + Base58.encode(writer.getPublicKeys()) + "/" + Base58.encode(mapKey) + "/" + Base58.encode(baseKey.serialize());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReadableFilePointer that = (ReadableFilePointer) o;

        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        if (writer != null ? !writer.equals(that.writer) : that.writer != null) return false;
        if (!Arrays.equals(mapKey, that.mapKey)) return false;
        return true;

    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (writer != null ? writer.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(mapKey);
        return result;
    }

    @Override
    public String toString() {
        return ArrayOps.bytesToHex(mapKey);
    }

    public boolean isNull() {
        UserPublicKey nullUser = UserPublicKey.createNull();
        return nullUser.equals(owner) && nullUser.equals(writer) && Arrays.equals(mapKey, new byte[32]) && baseKey.equals(SymmetricKey.createNull());
    }

    public static ReadableFilePointer fromLink(String keysString) {
        String[] split = keysString.split("/");
        UserPublicKey owner = UserPublicKey.fromPublicKeys(Base58.decode(split[0]));
        UserPublicKey writer = UserPublicKey.fromPublicKeys(Base58.decode(split[1]));
        byte[] mapKey = Base58.decode(split[2]);
        SymmetricKey baseKey = SymmetricKey.deserialize(Base58.decode(split[3]));
        return new ReadableFilePointer(owner, writer, mapKey, baseKey);
    }

    public static ReadableFilePointer createNull() {
        return new ReadableFilePointer(UserPublicKey.createNull(), UserPublicKey.createNull(), new byte[32], SymmetricKey.createNull());
    }
}