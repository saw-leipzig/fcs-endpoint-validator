package eu.clarin.sru.fcs.tester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericRecorder<T> implements Recorder<T> {

    protected final Map<Long, List<T>> records = new HashMap<>();

    // ----------------------------------------------------------------------

    protected void addRecord(Long id, T record) {
        synchronized (records) {
            List<T> localRecords = records.get(id);
            if (localRecords == null) {
                localRecords = new ArrayList<>();
                records.put(id, localRecords);
            }
            localRecords.add(record);
        }
    }

    @Override
    public void addRecord(T record) {
        Long id = Thread.currentThread().getId();
        addRecord(id, record);
    }

    protected T getLastRecord(Long id) {
        synchronized (records) {
            List<T> localRecords = records.get(id);
            if (localRecords != null && !localRecords.isEmpty()) {
                return localRecords.get(localRecords.size() - 1);
            }
            return null;
        }
    }

    public T getLastRecord() {
        Long id = Thread.currentThread().getId();
        return getLastRecord(id);
    }

    // ----------------------------------------------------------------------

    protected List<T> getRecords(Long id) {
        synchronized (records) {
            return records.get(id);
        }
    }

    @Override
    public List<T> getRecords() {
        Long id = Thread.currentThread().getId();
        return getRecords(id);
    }

    protected boolean hasRecords(Long id) {
        synchronized (records) {
            return records.containsKey(id);
        }
    }

    @Override
    public boolean hasRecords() {
        Long id = Thread.currentThread().getId();
        return hasRecords(id);
    }

    protected List<T> removeRecords(Long id) {
        synchronized (records) {
            return records.remove(id);
        }
    }

    @Override
    public List<T> removeRecords() {
        Long id = Thread.currentThread().getId();
        return removeRecords(id);
    }

}
