package eu.clarin.sru.fcs.validator;

import java.util.List;

public interface Recorder<T> {

    void addRecord(T record);

    List<T> getRecords();

    boolean hasRecords();

    List<T> removeRecords();

}