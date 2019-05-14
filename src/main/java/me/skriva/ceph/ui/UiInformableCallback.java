package me.skriva.ceph.ui;

interface UiInformableCallback<T> extends UiCallback<T> {
    void inform(String text);
}
