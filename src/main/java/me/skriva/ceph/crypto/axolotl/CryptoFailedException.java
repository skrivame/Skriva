package me.skriva.ceph.crypto.axolotl;

class CryptoFailedException extends Exception {

	public CryptoFailedException(String msg) {
		super(msg);
	}

	public CryptoFailedException(String msg, Exception e) {
		super(msg, e);
	}

	public CryptoFailedException(Exception e){
		super(e);
	}
}
