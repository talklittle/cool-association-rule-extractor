package coms6111.proj3;

import java.util.*;

public class Itemset implements Comparable<Itemset> {
	public int[] ranges;
	public int[] words; // words[] and ranges[] are same length
	
	public Itemset() {
		ranges = new int[0];
		words = new int[0];
	}
	
	public Itemset(SortedSet<Integer> wordPositions) {
		TreeMap<Integer, Integer> rangesWords = new TreeMap<Integer, Integer>();
		int rangeIndex, bitmask;
		for (int i : wordPositions) {
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
	
	public Itemset(Itemset other, int newLength) {
		ranges = Arrays.copyOf(other.ranges, newLength);
		words = Arrays.copyOf(other.words, newLength);
	}
	
	public int getNumWords() {
		int returnMe = 0;
		for (int i = 0; i < ranges.length; i++) {
			returnMe += Bits.getNumBits(words[i]);
		}
		return returnMe;
	}
	
	public String[] getDocs(HashMap<Integer, String> docsRev) {
		// FIXME
		return null;
	}
	
	public Itemset chopLastBit() {
		Itemset returnMe;
		int lastRange = ranges.length - 1;
		if (lastRange < 0)
			return null;
		// The last bit is in a range by itself, so chop off the last range
		if (Bits.singleBit.contains(ranges[lastRange])) {
			returnMe = new Itemset(this, ranges.length-1);
		} else {
			returnMe = new Itemset(this, ranges.length);
			// Chop off the final bit using XOR
			returnMe.ranges[ranges.length-1] ^= Bits.getLastBit(ranges[ranges.length-1]);
		}
		return returnMe;
	}
	
	/**
	 * Add a new item to a copy of the Itemset and return the copy.
	 * @param range Range id of new item
	 * @param bitmask Bitmask (single bit set) of the new item
	 * @return Copy of this Itemset with new item inserted
	 */
	public Itemset addAndCopy(int range, int bitmask) {
		int[] newRanges, newWords;
		
		int newRangeLength;
		int rangePos;
		rangePos = getRangePos(range);
		if (rangePos != -1) {
			// Goes into existing range
			newRangeLength = ranges.length;
		} else {
			// Goes into a new range
			newRangeLength = ranges.length + 1;
		}
//		System.out.println("ranges.length " + ranges.length + " newRangeLength " + newRangeLength);

		// Insert the new item where it should go
		newRanges = new int[newRangeLength];
		newWords = new int[newRangeLength];
		boolean inserted = false;
		int oldI = 0;
		for (int i = 0; i < newRangeLength; i++) {
			// Use this set of conditionals to find position to insert range id
			if (!inserted && i == newRangeLength - 1 && newRangeLength > ranges.length) {
				// The new range goes at the end
				newRanges[i] = range;
				newWords[i] = bitmask;
				inserted = true;
			} else if (!inserted && ranges[oldI] >= range) {
				newRanges[i] = range;
				if (ranges[oldI] == range) {
					// New bit goes into existing range
					newWords[i] = words[oldI++] | bitmask;
				} else {
					// New bit goes into a new range
					newWords[i] = bitmask;
				}
				inserted = true;
			} else {
				newRanges[i] = ranges[oldI];
				newWords[i] = words[oldI];
				oldI++;
			}
		}
		return new Itemset(newRanges, newWords);
	}
	
	public boolean contains(Itemset o) {
		for (int i = 0; i < o.ranges.length; i++) {
			if (!this.containsWords(o.ranges[i], o.words[i]))
				return false;
		}
		return true;
	}
	
	public boolean containsRange(int rangeId) {
		return getRangePos(rangeId) != -1;
	}
	
	public boolean containsWords(int rangeId, int bitmask) {
		int rangePos = getRangePos(rangeId);
		if (rangePos == -1)
			return false;
		return (words[rangePos] & bitmask) == bitmask;
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
		words[rangePos] &= ~bitmask;
		if (words[rangePos] == 0) {
			// Remove this range
			newRanges = Arrays.copyOf(ranges, ranges.length - 1);
			newWords = Arrays.copyOf(words, words.length - 1);
			for (int i = rangePos; i < ranges.length - 1; i++) {
				newRanges[i] = ranges[i+1];
				newWords[i] = words[i+1];
			}
			ranges = newRanges;
			words = newWords;
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
		int rangePos = getRangePos(rangeId);
		int newBitmap;
		int[] newRanges, newWords;
		if (rangePos == -1) {
			// Doesn't contain that range
			return new Itemset(this, ranges.length);
		}
		// Unset the bits
		newBitmap = words[rangePos] & ~bitmask;
		if (newBitmap == 0) {
			// Remove this range
			newRanges = Arrays.copyOf(ranges, ranges.length - 1);
			newWords = Arrays.copyOf(words, words.length - 1);
			for (int i = rangePos; i < ranges.length - 1; i++) {
				newRanges[i] = ranges[i+1];
				newWords[i] = words[i+1];
			}
			return new Itemset(newRanges, newWords);
		} else {
			// Return a copy with bits from bitmask unset
			Itemset returnMe = new Itemset(this, ranges.length);
			returnMe.words[rangePos] = newBitmap;
			return returnMe;
		}
	}
	
	public List<Integer> getWordIds() {
		ArrayList<Integer> allWordIds = new ArrayList<Integer>();
		for (int i = 0; i < ranges.length; i++) {
			allWordIds.addAll(rangeToWordIds(ranges[i]));
		}
		return allWordIds;
	}
	
	private List<Integer> rangeToWordIds(int rangeId) {
		ArrayList<Integer> wordIds = new ArrayList<Integer>();
		int base = rangeId * 32;
		int bitmask = words[getRangePos(rangeId)];
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
				right = mid-1;
			} else {
				return mid;
			}
		}
		return -1;
	}
	
	/**
	 * Find the range id of the given bit index.
	 * This is very simple. Each range contains 32 bits, so just
	 * count bits starting from the left.
	 * @param i
	 * @return
	 */
	public static int posToRange(int i) {
		if (i % 32 != 0)
			return (i/32) + 1;
		else
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
		// Compares 32 bits at a time :)
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
		if (words.length <= o.words.length)
			length = words.length;
		else
			length = o.words.length;
		for (int i = 0; i < length; i++) {
			// ranges[] holds regular ints, not bitmasks, so lower comes first
			if (ranges[i] < o.ranges[i])
				return -1;
			if (ranges[i] > o.ranges[i])
				return 1;
			// Java treats MSB as a sign bit, so need to convert ints to longs
			long myI, otherI;
			myI = words[i] & 0x7FFFFFFF;
			if (words[i] < 0)
				myI += 0x80000000;
			otherI = words[i] & 0x7FFFFFFF;
			if (o.words[i] < 0)
				otherI += 0x80000000;
			// words[] holds bitmasks, so a numerically larger value comes first
			if (myI < otherI)
				return 1;
			else if (myI > otherI)
				return -1;
		}
		return 0;
	}
}
