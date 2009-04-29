package coms6111.proj3;

import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

public class ItemsetTrie implements Comparable<ItemsetTrie> {
	
	public Itemset value; // The value of the step taken to get here.
		                  // e.g., if children can have keys "a", "b", "c" from a given node,
		                  // then the child at "b" will have value "b".
		                  // Root of trie should have null value.
	public SortedSet<ItemsetTrie> children;
	
	private ItemsetTrie parent;
	
	
	
	public void add(List<T> items) throws Exception {
		SimpleTrie<T> currTrie = this;
		int itemsLen = items.size();
		int depth = 0;
		for (T item : items) {
			if (!currTrie.children.containsKey(item)) {
				List<T> ap = appended(items, item);
				currTrie.children.put(ap, new SimpleTrie<T>(ap));
			} else if (depth == itemsLen - 1){
				throw new Exception("Key already exists: " + item);
			}
			currTrie = currTrie.children.get(item);
		}
	}
	
	public SimpleTrie<T> get(List<T> items) {
		SimpleTrie<T> currTrie = this;
		
		if (items.size() < this.path.size())
			return null;
		// FIXME Should really check if initial path.size() elements actually match...
		for (int i = this.path.size() + 1; i < items.size(); i++) {
			if (currTrie.children.containsKey(items.get(i))) {
				currTrie = currTrie.children.get(items.get(i));
			} else {
				return null;
			}
		}
		
		return currTrie;
	}
	
	public void remove(List<T> items) throws Exception {
		SimpleTrie<T> currTrie = this;
		int itemsLen = items.size();
		int depth = 0;
		
		if (items.size() < this.path.size())
			throw new Exception("items.size " + items.size() + " less than this.path.size " + this.path.size());
		// FIXME Should really check if initial path.size() elements actually match...
		for (int i = this.path.size() + 1; i < items.size(); i++) {
			if (currTrie.children.containsKey(items.get(i))) {
				if (depth == itemsLen - 1) {
					currTrie.children.remove(items.get(i));
					return;
				}
				currTrie = currTrie.children.get(items.get(i));
			} else {
				throw new Exception("Path does not exist");
			}
		}
	}
	
	public int compareTo(ItemsetTrie o) {
		ItemsetTrie myTrie, otherTrie;
		for (myTrie = this; myTrie.value == null; myTrie = myTrie.children.first())
			;
		for (otherTrie = this; otherTrie.value == null; otherTrie = otherTrie.children.first())
			;
		if (value == null && o.value == null)
			return children.first().compareTo(o.children.first());
		return value.compareTo(o.value);
	}
}
