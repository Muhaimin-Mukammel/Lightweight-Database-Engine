package db_engine;

public class PageManager {

    private static final int SLOT_SIZE = 128;

    public int insert(Page page, byte[] record) {

        byte[] data = page.getData();

        for (int slot = 0; slot < data.length; slot += SLOT_SIZE) {

            boolean empty = true;

            for (int i = slot; i < slot + SLOT_SIZE; i++) {
                if (data[i] != 0) {
                    empty = false;
                    break;
                }
            }

            if (empty) {
                System.arraycopy(record, 0, data, slot, Math.min(record.length, SLOT_SIZE));
                return slot / SLOT_SIZE;
            }
        }

        throw new RuntimeException("Page full");
    }

    public byte[] read(Page page, int slotId) {

        int start = slotId * SLOT_SIZE;

        byte[] result = new byte[SLOT_SIZE];

        System.arraycopy(page.getData(), start, result, 0, SLOT_SIZE);

        return result;
    }

    public void delete(Page page, int slotId) {

        int start = slotId * SLOT_SIZE;

        for (int i = start; i < start + SLOT_SIZE; i++) {
            page.getData()[i] = 0;
        }
    }
}