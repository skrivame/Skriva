package me.skriva.ceph.xmpp.jingle;

import me.skriva.ceph.entities.DownloadableFile;

interface OnFileTransmissionStatusChanged {
	void onFileTransmitted(DownloadableFile file);

	void onFileTransferAborted();
}
