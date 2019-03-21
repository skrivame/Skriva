package me.skriva.ceph.ui;

public interface UiInformableCallback<T> extends UiCallback<T> {
    void inform(String text);
}
