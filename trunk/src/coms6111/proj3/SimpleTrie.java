package coms6111.proj3;

import java.util.*;

/**
 * A trie where the path in the trie is equivalent to the data stored in the trie.
 * I.e., no extra data is stored at each node.
 * 
 * @param <T>
 */
public class SimpleTrie<T> {
	public List<T> path; // The path taken to get here.
	
	public T value;       // The value of the step taken to get here.
		                  // e.g., if children can have keys "a", "b", "c" from a given node,
		                  // then the child at "b" will have value "b".
		                  // Root of trie should have null value.
	public SortedMap<List<T>, SimpleTrie<T>> children;
	
	
	public SimpleTrie() {
		path = new ArrayList<T>();
		value = null;
		children = new TreeMap<List<T>, SimpleTrie<T>>();
	}
	
	public SimpleTrie(List<T> val) {
		path = val;
		value = val.get(val.size()-1);
		children = new TreeMap<List<T>, SimpleTrie<T>>();
	}
	
	public SimpleTrie(List<T> prefix, T leaf) {
		path = new ArrayList<T>();
		path.addAll(prefix);
		path.add(leaf);
		value = leaf;
		children = new TreeMap<List<T>, SimpleTrie<T>>();
	}
	
	/**
	 * Add a new list of items (a path) to the trie.
	 * Will throw an exception if path exists.
	 * @param items
	 * @throws Exception
	 */
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
	
	/**
	 * Same as add() but will only add to a leaf node.
	 * The leaf node then becomes an internal node.
	 * @param items
	 * @throws Exception
	 */
	public void addToLeaf(List<T> items) throws Exception {
		SimpleTrie<T> currTrie = this;
		int itemsLen = items.size();
		int depth = 0;
		for (T item : items) {
			if (!currTrie.children.containsKey(item)) {
				if (depth == itemsLen - 1) {
					List<T> ap = appended(items, item);
					currTrie.children.put(ap, new SimpleTrie<T>(ap));
				} else {
					throw new Exception("Trying to add more than one level");
				}
			} else if (depth == itemsLen - 1){
				throw new Exception("Key already exists: " + item);
			}
			currTrie = currTrie.children.get(item);
		}
	}
//	@SuppressWarnings("unchecked")
//	public void addToLeaf(List<T> items) throws Exception {
//		addToLeaf((T[]) items.toArray());
//	}
	
	/**
	 * Get the subtrie rooted at the given path.
	 * @param items
	 * @return
	 */
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
//	@SuppressWarnings("unchecked")
//	public void get(List<T> items) throws Exception {
//		get((T[]) items.toArray());
//	}
	
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
//	@SuppressWarnings("unchecked")
//	public void remove(List<T> items) throws Exception {
//		get((T[]) items.toArray());
//	}
	
	/**
	 * Return a deep copy of the SimpleTrie.
	 * @return
	 */
	public SimpleTrie<T> deepCopy() {
		SimpleTrie<T> copy = new SimpleTrie<T>(this.path);
		for (SimpleTrie<T> child : this.children.values()) {
			List<T> copyList = new ArrayList<T>();
			copyList.addAll(child.path);
			copy.children.put(copyList, child.deepCopy());
		}
		return copy;
	}
	
	private List<T> appended(List<T> prefix, T leaf) {
		List<T> returnMe = new ArrayList<T>();
		returnMe.addAll(prefix);
		returnMe.add(leaf);
		return returnMe;
	}
}
