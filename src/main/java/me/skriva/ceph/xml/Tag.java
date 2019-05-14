package me.skriva.ceph.xml;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;

import me.skriva.ceph.utils.XmlHelper;

public class Tag {
	private static final int NO = -1;
	private static final int START = 0;
	private static final int END = 1;
	private static final int EMPTY = 2;

	private final int type;
	private final String name;
	private Hashtable<String, String> attributes = new Hashtable<>();

	private Tag(int type, String name) {
		this.type = type;
		this.name = name;
	}

	public static Tag no(String text) {
		return new Tag(NO, text);
	}

	public static Tag start(String name) {
		return new Tag(START, name);
	}

	public static Tag end(String name) {
		return new Tag(END, name);
	}

	public static Tag empty(String name) {
		return new Tag(EMPTY, name);
	}

	public String getName() {
		return name;
	}

	public String getAttribute(String attrName) {
		return this.attributes.get(attrName);
	}

	public void setAttribute(String attrName, String attrValue) {
		this.attributes.put(attrName, attrValue);
	}

	public void setAtttributes(Hashtable<String, String> attributes) {
		this.attributes = attributes;
	}

	public boolean isStart(String needle) {
		if (needle == null)
			return false;
		return (this.type == START) && (needle.equals(this.name));
	}

	public boolean isEnd(String needle) {
		if (needle == null)
			return false;
		return (this.type == END) && (needle.equals(this.name));
	}

	public boolean isNo() {
		return (this.type == NO);
	}

	public String toString() {
		StringBuilder tagOutput = new StringBuilder();
		tagOutput.append('<');
		if (type == END) {
			tagOutput.append('/');
		}
		tagOutput.append(name);
		if (type != END) {
			Set<Entry<String, String>> attributeSet = attributes.entrySet();
			for (Entry<String, String> entry : attributeSet) {
				tagOutput.append(' ');
				tagOutput.append(entry.getKey());
				tagOutput.append("=\"");
				tagOutput.append(XmlHelper.encodeEntities(entry.getValue()));
				tagOutput.append('"');
			}
		}
		if (type == EMPTY) {
			tagOutput.append('/');
		}
		tagOutput.append('>');
		return tagOutput.toString();
	}

	public Hashtable<String, String> getAttributes() {
		return this.attributes;
	}
}
