package coms6111.proj3;

import java.util.*;

public class Itemset implements Comparable<Itemset> {
	public int[] ranges;
	public int[] words; // words[] and ranges[] are same length
	
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
	
	public String[] getWords(HashMap<Integer, String> wordsRev) {
		// FIXME
		return null;
	}
	
	public String[] getDocs(HashMap<Integer, String> docsRev) {
		// FIXME
		return null;
	}
	
	public Itemset chopLastBit() {
		Itemset returnMe;
		int lastRange = ranges.length - 1;
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
		return 0x80000000 >> (i % 32);
	}
	
	public boolean equals(Itemset o) {
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
