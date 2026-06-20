package db_engine;

import java.util.HashMap;
import java.util.Map;

public class HashIndex {

    public static class Location {
        private final int pageId;
        private final int slotId;

        public Location(int pageId, int slotId) {
            this.pageId = pageId;
            this.slotId = slotId;
        }

        public int getPageId() {
            return pageId;
        }

        public int getSlotId() {
            return slotId;
        }
    }

    private final Map<String, Location> map = new HashMap<>();

    public void put(String key, Location value) {
        map.put(key, value);
    }

    public Location get(String key) {
        return map.get(key);
    }

    public void remove(String key) {
        map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public Map<String, Location> raw() {
        return map;
    }
}