package coms6111.proj3;

public class Item implements Comparable<Item> {
	public String value;
	
	public boolean equals(Item o) {
		return value.equals(o.value);
	}
	public int compareTo(Item o) {
		return value.compareTo(o.value);
	}
}
