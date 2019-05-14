package me.skriva.ceph.xmpp.jingle;

interface OnPrimaryCandidateFound {
	void onPrimaryCandidateFound(boolean success, JingleCandidate canditate);
}
