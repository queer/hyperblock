package gg.amy.hyperblock.component;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import gg.amy.hyperblock.component.database.HyperPlayer;
import gg.amy.hyperblock.component.database.ImmutableHyperPlayer;
import gg.amy.mc.cardboard.component.Component;
import gg.amy.mc.cardboard.component.Single;
import org.bson.codecs.pojo.PojoCodecProvider;

import javax.annotation.Nonnull;
import java.util.UUID;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * @author amy
 * @since 3/25/21.
 */
@Single
@Component(name = "database", description = "hyper's mongodb wrapper")
public class Database {
    private final MongoClient client;
    private final MongoDatabase db;
    private final PlayerDb playerDb;

    public Database() {
        final var pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        client = MongoClients.create(MongoClientSettings.builder()
                // TODO: Env
                .applyConnectionString(new ConnectionString("mongodb://127.0.0.1/hyperblock"))
                .retryWrites(true)
                .codecRegistry(pojoCodecRegistry)
                .build());
        db = client.getDatabase("hyperblock");
        db.createCollection("players");
        db.getCollection("players").createIndex(Indexes.text("uuid"));

        playerDb = new PlayerDb();
    }

    public PlayerDb getPlayerDb() {
        return playerDb;
    }

    private interface DB<T, K> {
        T get(@Nonnull final K key);

        void set(@Nonnull final T obj);
    }

    public class PlayerDb implements DB<HyperPlayer, UUID> {
        private final MongoCollection<HyperPlayer> collection = db.getCollection("players", HyperPlayer.class);

        @Override
        public void set(@Nonnull final HyperPlayer player) {
            collection.replaceOne(Filters.eq("uuid", player.uuid().toString()), player, new ReplaceOptions().upsert(true));
        }

        @Override
        public HyperPlayer get(@Nonnull final UUID uuid) {
            var player = collection.find(Filters.eq("uuid", uuid.toString())).first();
            if(player == null) {
                player = ImmutableHyperPlayer.builder()
                        .uuid(uuid)
                        .compressedInventory()
                        .build();
            }
            return player;
        }
    }
}
