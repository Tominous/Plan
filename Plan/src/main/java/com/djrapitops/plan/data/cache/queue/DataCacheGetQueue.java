package main.java.com.djrapitops.plan.data.cache.queue;

import main.java.com.djrapitops.plan.Log;
import main.java.com.djrapitops.plan.Phrase;
import main.java.com.djrapitops.plan.Plan;
import main.java.com.djrapitops.plan.Settings;
import main.java.com.djrapitops.plan.data.cache.DBCallableProcessor;
import main.java.com.djrapitops.plan.database.Database;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * This Class is starts the Get Queue Thread, that fetches data from DataCache.
 *
 * @author Rsl1122
 * @since 3.0.0
 */
public class DataCacheGetQueue extends Queue<Map<UUID, List<DBCallableProcessor>>> {

    /**
     * Class constructor, starts the new Thread for fetching.
     *
     * @param plugin current instance of Plan
     */
    public DataCacheGetQueue(Plan plugin) {
        super(new ArrayBlockingQueue(Settings.PROCESS_GET_LIMIT.getNumber()));
        setup = new GetSetup(queue, plugin.getDB());
        setup.go();
    }

    /**
     * Schedules UserData objects to be get for the given processors.
     *
     * @param uuid       UUID of the player whose UserData object is fetched.
     * @param processors Processors which process-method will be called after
     *                   fetch is complete, with the UserData object.
     */
    public void scheduleForGet(UUID uuid, DBCallableProcessor... processors) {
        Log.debug(uuid + ": Scheduling for get");
        try {
            Map<UUID, List<DBCallableProcessor>> map = new HashMap<>();
            map.put(uuid, Arrays.asList(processors));
            queue.add(map);
        } catch (IllegalStateException e) {
            Log.error(Phrase.ERROR_TOO_SMALL_QUEUE.parse("Get Queue", String.valueOf(Settings.PROCESS_GET_LIMIT.getNumber())));
        }
    }

    public boolean containsUUIDtoBeCached(UUID uuid) {
        return uuid != null && new ArrayList<>(queue).stream().anyMatch((map) -> (map.get(uuid) != null && map.get(uuid).size() >= 2));
    }
}

class GetConsumer extends Consumer<Map<UUID, List<DBCallableProcessor>>> {

    private Database db;

    GetConsumer(BlockingQueue q, Database db) {
        super(q, "GetQueueConsumer");
        this.db = db;
    }

    @Override
    void consume(Map<UUID, List<DBCallableProcessor>> processors) {
        if (db == null) {
            return;
        }

        try {
            for (UUID uuid : processors.keySet()) {
                if (uuid == null) {
                    continue;
                }
                List<DBCallableProcessor> processorsList = processors.get(uuid);
                if (processorsList != null) {
                    Log.debug(uuid + ": Get, For:" + processorsList.size());
                    try {
                        db.giveUserDataToProcessors(uuid, processorsList);
                    } catch (SQLException e) {
                        Log.toLog(this.getClass().getName(), e);
                    }
                }
            }
        } catch (Exception ex) {
            Log.toLog(this.getClass().getName(), ex);
        }
    }

    @Override
    void clearVariables() {
        if (db != null) {
            db = null;
        }
    }
}

class GetSetup extends Setup<Map<UUID, List<DBCallableProcessor>>> {

    GetSetup(BlockingQueue<Map<UUID, List<DBCallableProcessor>>> q, Database db) {
        super(new GetConsumer(q, db), new GetConsumer(q, db));
    }
}
