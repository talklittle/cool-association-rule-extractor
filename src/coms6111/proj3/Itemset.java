package coms6111.proj3;

import java.util.*;

public class Itemset implements Comparable<Itemset> {
	public int[] ranges;
	public int[] words; // words[] and ranges[] are same length
	
	// CONSTRUCTORS
	
	public Itemset() {
		ranges = new int[0];
		words = new int[0];
	}
	
	public Itemset(SortedSet<Integer> wordPositions) {
		TreeMap<Integer, Integer> rangesWords = new TreeMap<Integer, Integer>();
		int rangeIndex, bitmask;
		for (Iterator<Integer> it = wordPositions.iterator(); it.hasNext(); /* */) {
			int i = it.next();
			rangeIndex = posToRange(i);
			bitmask = posToBitmask(i);
			if (rangesWords.containsKey(rangeIndex)) {
				rangesWords.put(rangeIndex, rangesWords.get(rangeIndex) | bitmask);
			} else {
				rangesWords.put(rangeIndex, bitmask);
			}
		}
		ranges = new int[rangesWords.size()];
		words = new int[rangesWords.size()];
		int i = 0;
		for (Iterator<Integer> it = rangesWords.keySet().iterator(); it.hasNext(); /* */) {
			int num = it.next();
			ranges[i] = num;
			words[i] = rangesWords.get(num);
			i++;
		}
	}
	
	public Itemset(List<Integer> wordPositions) {
		TreeMap<Integer, Integer> rangesWords = new TreeMap<Integer, Integer>();
		int rangeIndex, bitmask;
		for (Iterator<Integer> it = wordPositions.iterator(); it.hasNext(); /* */) {
			int i = it.next();
			rangeIndex = posToRange(i);
			bitmask = posToBitmask(i);
			if (rangesWords.containsKey(rangeIndex)) {
				rangesWords.put(rangeIndex, rangesWords.get(rangeIndex) | bitmask);
			} else {
				rangesWords.put(rangeIndex, bitmask);
			}
		}
		ranges = new int[rangesWords.size()];
		words = new int[rangesWords.size()];
		int i = 0;
		for (Iterator<Integer> it = rangesWords.keySet().iterator(); it.hasNext(); /* */) {
			int num = it.next();
			ranges[i] = num;
			words[i] = rangesWords.get(num);
			i++;
		}
	}
	
	public Itemset(int[] newRanges, int[] newWords) {
		ranges = newRanges;
		words = newWords;
	}
	
	public Itemset(Integer[] newRanges, Integer[] newWords) {
		ranges = new int[newRanges.length];
		words = new int[newWords.length];
		for (int i = 0; i < newRanges.length; i++) {
			ranges[i] = newRanges[i];
			words[i] = newWords[i];
		}
	}
	
	public Itemset(Itemset other, int newLength) {
		ranges = Arrays.copyOf(other.ranges, newLength);
		words = Arrays.copyOf(other.words, newLength);
	}
	
	
	// END CONSTRUCTORS
	
	
	public int getNumBits() {
		int returnMe = 0;
		for (int i = 0; i < ranges.length; i++) {
			returnMe += Bits.getNumBits(words[i]);
		}
		return returnMe;
	}
	
	/**
	 * Get an Itemset of Intersection of document Ids where the words in this Itemset can be found.
	 * i.e., the documents where you can find all words.
	 * Assuming that this is an Itemset of words.
	 * @param wordDocs
	 * @return
	 */
	public Itemset getDocIdsIntersection(HashMap<Integer, Itemset> wordDocs) {
		List<Integer> myIds = this.getIds();
		boolean firstPass = true;
		Itemset intersection = null;
		
		for (Integer wordId : myIds) {
			Itemset docsContainingWord = wordDocs.get(wordId);
			
//			System.out.println("DEBUG: getDocIdsIntersection: wordId="+wordId+" rangeId="+posToRange(wordId)
//					+" docsContainingWordLen=" + docsContainingWord.getNumWords());
			
			if (firstPass) {
				intersection = docsContainingWord;
				firstPass = false;
			} else {
				intersection = intersection.intersect(docsContainingWord);
			}
		}
		
//		System.out.println("DEBUG: getDocIdsIntersection: return intersectionDocsLen="+intersectionDocs.size());
		return intersection;
	}
	
//	/**
//	 * Get a TreeSet of Union of document Ids where the words in this Itemset can be found.
//	 * Assuming that this is an Itemset of words.
//	 * @param wordDocs
//	 * @return
//	 */
//	public TreeSet<Integer> getDocIdsUnion(HashMap<Integer, Itemset> wordDocs) {
//		TreeSet<Integer> returnMe = new TreeSet<Integer>();
//		List<Integer> myIds = this.getIds();
//		for (Integer wordId : myIds) {
//			Itemset docsContainingWord = wordDocs.get(wordId);
//			if (docsContainingWord == null) {
//				System.err.println("ERROR: getDocIds: docsContainingWord is null. wordId: "+wordId+" word: "+FileReader.idWords.get(wordId));
//				continue;
//			}
//			returnMe.addAll(docsContainingWord.getIds());
//		}
//		return returnMe;
//	}
	
	public Itemset chopLastBit() {
		Itemset returnMe;
		if (words.length == 0)
			return null;
		int lastWords = words[words.length - 1];
		if (Bits.singleBit.contains(lastWords)) {
			// The last bit is in a range by itself, so chop off the last range
			returnMe = new Itemset(this, ranges.length-1);
		} else {
			returnMe = new Itemset(this, ranges.length);
			// Chop off the final bit
			returnMe.words[words.length-1] &= ~Bits.getLastBit(lastWords);
		}
		return returnMe;
	}
	
	/**
	 * Add a new item to a copy of the Itemset and return the copy.
	 * @param rangeId Range id of new item
	 * @param bitmask Bitmask (single bit set) of the new item
	 * @return Copy of this Itemset with new item inserted
	 */
	public Itemset addAndCopy(int rangeId, int bitmask) {
		int[] newRanges, newWords;
		
		int newRangeLength;
		int rangeInsertionPos = getRangeInsertionPos(rangeId);
		if (rangeInsertionPos == ranges.length || ranges[rangeInsertionPos] != rangeId) {
			// Goes into a new range
			newRangeLength = ranges.length + 1;
		} else {
			// Goes into an existing range
			newRangeLength = ranges.length;
		}
//		System.out.println("ranges.length " + ranges.length + " newRangeLength " + newRangeLength);

		newRanges = Arrays.copyOf(ranges, newRangeLength);
		newWords = Arrays.copyOf(words, newRangeLength);
		if (newRangeLength > ranges.length) {
			// Shift everything to the right to make room
			for (int i = newRangeLength - 1; i > rangeInsertionPos; i--) {
				newRanges[i] = newRanges[i-1];
				newWords[i] = newWords[i-1];
			}
			newRanges[rangeInsertionPos] = rangeId;
			newWords[rangeInsertionPos] = 0;
		}
		// Insert the bitmask
		newWords[rangeInsertionPos] |= bitmask;
		
		return new Itemset(newRanges, newWords);
	}
	
	public Itemset intersect(Itemset o) {
		HashMap<Integer, Integer> rangeWords = new HashMap<Integer, Integer>();
		TreeSet<Integer> sharedRanges = new TreeSet<Integer>();
		Integer[] intersectRanges;
		ArrayList<Integer> intersectWords = new ArrayList<Integer>();
		Itemset intersection;
		int minRangeId, maxRangeId;
		int myMinRangePos, myMaxRangePos, oMinRangePos, oMaxRangePos;

		// Get min and max range that you need to look through
		if (this.ranges[0] < o.ranges[0])
			minRangeId = o.ranges[0];
		else
			minRangeId = this.ranges[0];
		if (this.ranges[this.ranges.length-1] < o.ranges[o.ranges.length-1])
			maxRangeId = this.ranges[this.ranges.length-1];
		else
			maxRangeId = o.ranges[o.ranges.length-1];
		
		myMinRangePos = this.getRangeInsertionPos(minRangeId);
		myMaxRangePos = this.getRangeInsertionPos(maxRangeId);
		oMinRangePos = o.getRangeInsertionPos(minRangeId);
		oMaxRangePos = o.getRangeInsertionPos(maxRangeId);
		
		// Add bitmasks from this
		for (int i = myMinRangePos; i <= myMaxRangePos && i < this.ranges.length; i++) {
			rangeWords.put(this.ranges[i], this.words[i]);
		}

		// Intersect with bitmasks from other
		for (int i = oMinRangePos; i <= oMaxRangePos && i < o.ranges.length; i++) {
			if (rangeWords.containsKey(o.ranges[i])) {
				int bitmask = rangeWords.get(o.ranges[i]) & o.words[i];
				if (bitmask != 0) {
					rangeWords.put(o.ranges[i], bitmask);
					sharedRanges.add(o.ranges[i]);
				} else {
					rangeWords.remove(o.ranges[i]);
					sharedRanges.remove(o.ranges[i]);
				}
			} else {
				// Do nothing because we are finding intersection
			}
		}
		
		intersectRanges = sharedRanges.toArray(new Integer[0]);
		for (Integer i : intersectRanges) {
			intersectWords.add(rangeWords.get(i));
		}
		intersection = new Itemset(intersectRanges, intersectWords.toArray(new Integer[0]));
		return intersection;
	}
	
	public Itemset intersectAtLeast(int minRangeId, Itemset o) {
		HashMap<Integer, Integer> rangeWords = new HashMap<Integer, Integer>();
		TreeSet<Integer> sharedRanges = new TreeSet<Integer>();
		Integer[] intersectRanges;
		ArrayList<Integer> intersectWords = new ArrayList<Integer>();
		Itemset intersection;
		
		// Add bitmasks from this
		int myStart = this.getRangeInsertionPos(minRangeId);
		for (int i = myStart; i < this.ranges.length; i++) {
			rangeWords.put(this.ranges[i], this.words[i]);
		}
		// Intersect with bitmasks from other
		int oStart = o.getRangeInsertionPos(minRangeId);
		for (int i = oStart; i < o.ranges.length; i++) {
			if (rangeWords.containsKey(o.ranges[i])) {
				int bitmask = rangeWords.get(o.ranges[i]) & o.words[i];
				if (bitmask != 0) {
					rangeWords.put(o.ranges[i], bitmask);
					sharedRanges.add(o.ranges[i]);
				} else {
					rangeWords.remove(o.ranges[i]);
					sharedRanges.remove(o.ranges[i]);
				}
			} else {
				// Do nothing because we are finding intersection
			}
		}
		
		intersectRanges = sharedRanges.toArray(new Integer[0]);
		for (Integer i : intersectRanges) {
			intersectWords.add(rangeWords.get(i));
		}
		intersection = new Itemset(intersectRanges, intersectWords.toArray(new Integer[0]));
		return intersection;
	}
	
	public boolean contains(Itemset o) {
		for (int i = 0; i < o.ranges.length; i++) {
			if (!this.containsWords(o.ranges[i], o.words[i]))
				return false;
		}
		return true;
	}
	
	public boolean containsRange(int rangeId) {
		return (getRangePos(rangeId) != -1);
	}
	
	public boolean containsWords(int rangeId, int bitmask) {
		int rangePos = getRangePos(rangeId);
		if (rangePos == -1)
			return false;
		return ((words[rangePos] & bitmask) == bitmask);
	}
	
	public boolean containsWordIds(List<Integer> wordIds) {
		for (int wordId : wordIds) {
			if (!containsWords(Itemset.posToRange(wordId), Itemset.posToBitmask(wordId)))
				return false;
		}
		return true;
	}
	
	/**
	 * Remove the word in place (mutable operation)
	 * @param rangeId
	 * @param bitmask
	 * @return boolean whether or not the word was in the itemset
	 */
	public boolean remove(int rangeId, int bitmask) {
		int rangePos = getRangePos(rangeId);
		int[] newRanges, newWords;
		if (rangePos == -1) {
			// Doesn't contain that range
			return false;
		}
		// Unset the bits
		this.words[rangePos] &= ~bitmask;
		if (words[rangePos] == 0) {
			// Remove this range
			newRanges = Arrays.copyOf(ranges, ranges.length - 1);
			newWords = Arrays.copyOf(words, words.length - 1);
			for (int i = rangePos; i < ranges.length - 1; i++) {
				newRanges[i] = ranges[i+1];
				newWords[i] = words[i+1];
			}
			this.ranges = newRanges;
			this.words = newWords;
		}
		return true;
	}
	
	/**
	 * Get a copy of the itemset with the word removed
	 * @param rangeId
	 * @param bitmask
	 * @return boolean whether or not the word was in the itemset
	 */
	public Itemset removeAndCopy(int rangeId, int bitmask) {
		Itemset itset = new Itemset(this, this.ranges.length);
		itset.remove(rangeId, bitmask);
		return itset;
	}
	
	public List<Integer> getIds() {
		ArrayList<Integer> allWordIds = new ArrayList<Integer>();
		for (int i = 0; i < ranges.length; i++) {
			allWordIds.addAll(rangeToWordIds(ranges[i]));
		}
		return allWordIds;
	}
	
	private List<Integer> rangeToWordIds(int rangeId) {
		ArrayList<Integer> wordIds = new ArrayList<Integer>();
		int rangePos = getRangePos(rangeId);
		if (rangePos == -1) {
			System.err.println("ERROR: rangeToWordIds: Bad rangeId "+rangeId);
			return wordIds;
		}
		if (rangeId != ranges[rangePos]) {
			System.err.println("ERROR: rangeToWordIds: rangeId != ranges[rangePos]"
					+" ("+rangeId+" != "+ranges[rangePos]+")");
		}
		
		int base = rangeId * 32;
		int bitmask = words[rangePos];
		int firstbit;
		
		while (bitmask != 0) {
			firstbit = Bits.getFirstBit(bitmask);
			wordIds.add(base + Bits.getPosFromLeft(firstbit));
			bitmask &= ~firstbit;
		}
		return wordIds;
	}
	
	/**
	 * Given a rangeId, find the position in the current ranges[] array
	 * where that Id is located.
	 * @param rangeId
	 * @return the position, or -1 if rangeId is not in ranges[]
	 */
	private int getRangePos(int rangeId) {
		int left, mid, right;
		left = 0;
		right = ranges.length;
		while (left < right) {
			mid = left + (right - left) / 2;
			if (rangeId > ranges[mid]) {
				left = mid+1;
			} else if (rangeId < ranges[mid]) {
				right = mid;
			} else {
				return mid;
			}
		}
		return -1;
	}
	
	/**
	 * Given a rangeId, find the position in the current ranges[] array
	 * where the Id should be inserted. If ranges[retval] != rangeId,
	 * that means you need to shift everything from position retval 
	 * to the right before inserting.
	 */
	private int getRangeInsertionPos(int rangeId) {
		int left, mid, right;
		left = 0;
		right = ranges.length;
		while (left < right) {
			mid = left + (right - left) / 2;
			if (rangeId > ranges[mid]) {
				left = mid+1;
			} else if (rangeId < ranges[mid]) {
				right = mid;
			} else {
				return mid;
			}
		}
		return left;
	}
	
	/**
	 * Find the range id of the given bit index.
	 * This is very simple. Each range contains 32 bits, so just
	 * count bits starting from the left.
	 * @param i
	 * @return
	 */
	public static int posToRange(int i) {
		return i/32;
	}
	
	/**
	 * Find the bit within the range of the given bit index.
	 * This is very simple. Just get rid of all preceding ranges (mod 32)
	 * and then return the binary number you are left with.
	 * @param i
	 * @return
	 */
	public static int posToBitmask(int i) {
		return 0x80000000 >>> (i % 32);
	}
	
	public boolean equals(Itemset o) {
		if (o == null) {
			if (ranges.length == 0)
				return true;
			else
				return false;
		}
		if (ranges.length != o.ranges.length || words.length != o.words.length)
			return false;
		// Compares 32 bits at a time
		for (int i = 0; i < words.length; i++) {
			if (words[i] != o.words[i] || ranges[i] != o.ranges[i])
				return false;
		}
		return true;
	}

	/**
	 * Compare two Itemsets such that the one with leftmost 1-bit comes first.
	 * So we treat comparisons as reversed numerical comparisons, since
	 * larger numbers have bits farther to the left.
	 */
	public int compareTo(Itemset o) {
		int length;
		int myNumBits, oNumBits;
		myNumBits = this.getNumBits();
		oNumBits = o.getNumBits();
		// Something with fewer bits should come first
		if (myNumBits < oNumBits)
			return -1;
		if (myNumBits > oNumBits)
			return 1;
		
		// Use the shortest # ranges, since you will find a difference
		if (this.ranges.length <= o.ranges.length)
			length = this.ranges.length;
		else
			length = o.ranges.length;
		for (int i = 0; i < length; i++) {
			// ranges[] holds regular ints, not bitmasks, so lower comes first
			if (ranges[i] < o.ranges[i])
				return -1;
			if (ranges[i] > o.ranges[i])
				return 1;
			// Java treats MSB as a sign bit, so need to convert ints to longs
			long myI, otherI;
			myI = words[i] & 0x7FFFFFFF;
			if ((words[i] & 0x80000000) == 0x80000000)
				myI += 0x80000000L;
			otherI = words[i] & 0x7FFFFFFF;
			if ((o.words[i] & 0x80000000) == 0x80000000)
				otherI += 0x80000000L;
			// words[] holds bitmasks, so a numerically larger value comes first
			if (myI > otherI)
				return -1;
			else if (myI < otherI)
				return 1;
		}
		return 0;
	}
	
	public void debugPrintWords(Map<Integer, String> idWords) {
		List<Integer> wordIds = this.getIds();
		System.out.print("DEBUG: Itemset (len "+wordIds.size()+"): ");
		for (Integer wordId : wordIds) {
			System.out.print(idWords.get(wordId) + " ("+wordId+"), ");
		}
		System.out.println();
	}
}
