package peergos.util;

import peergos.corenode.CoreNode;
import peergos.crypto.UserPublicKey;
import peergos.server.merklebtree.MerkleBTree;
import peergos.server.storage.ContentAddressedStorage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BtreeImpl implements Btree {
    private final CoreNode coreNode;
    private final ContentAddressedStorage dht;

    public BtreeImpl(CoreNode coreNode, ContentAddressedStorage dht) {
        this.coreNode = coreNode;
        this.dht = dht;
    }

    @Override
    public ByteArrayWrapper put(UserPublicKey sharingKey, byte[] mapKey, byte[] value) throws IOException {
        byte[] raw = coreNode.getMetadataBlob(sharingKey.serialize());
        byte[] rootHash = raw.length == 0 ? new byte[0] : raw;
        MerkleBTree btree = MerkleBTree.create(rootHash, dht);
        byte[] newRoot = btree.put(mapKey, value);
        return new ByteArrayWrapper(newRoot);
    }

    @Override
    public ByteArrayWrapper get(UserPublicKey sharingKey, byte[] mapKey) throws IOException {
        byte[] rootHash = coreNode.getMetadataBlob(sharingKey.serialize());
        MerkleBTree btree = MerkleBTree.create(rootHash, dht);
        return new ByteArrayWrapper(btree.get(mapKey));
    }

    @Override
    public ByteArrayWrapper remove(UserPublicKey sharingKey, byte[] mapKey) throws IOException {
        byte[] rootHash = coreNode.getMetadataBlob(sharingKey.serialize());
        MerkleBTree btree = MerkleBTree.create(rootHash, dht);
        byte[] newRoot = btree.delete(mapKey);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);
        Serialize.serialize(rootHash, dout);
        Serialize.serialize(newRoot, dout);
        return new ByteArrayWrapper(bout.toByteArray());
    }
}