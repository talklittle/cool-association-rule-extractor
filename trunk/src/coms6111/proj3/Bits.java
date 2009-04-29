package coms6111.proj3;

import java.util.HashMap;
import java.util.HashSet;

public class Bits {
	public static HashSet<Integer> singleBit = null; // All of the 32-bit integers with a single bit set 
	public static HashMap<Integer, Integer> lastBit8 = null; // The value of the least significant bit for all 8-bit ints
	public static HashMap<Integer, Integer> numBits8 = null; // The number of 1-bits in an 8-bit integer
	
	public static void init() {
		initSingleBit();
		initLastBit8();
		initNumBits8();
	}
	
	private static void initSingleBit() {
		singleBit = new HashSet<Integer>();
		for (long i = 1; i <= 0xFFFFFFFFL; i <<= 1) {
			singleBit.add((int) i);
		}
	}
	
	private static void initLastBit8() {
		lastBit8 = new HashMap<Integer, Integer>();
		lastBit8.put(0, 0);
		for (int i = 1; i <= 0xFF; i <<= 1) {
			for (int j = i; j <= 0xFF; j += 2*i) {
				if (!lastBit8.containsKey(j)) {
					lastBit8.put(j, i);
				}
			}
		}
	}
	
	private static void initNumBits8() {
		numBits8 = new HashMap<Integer, Integer>();
		numBits8.put(0, 0);
		for (int i = 1; i <= 0xFF; i++) {
			numBits8.put(i, (i & 1) + numBits8.get(i >> 1));
		}
	}
	
	public static int getLastBit(int num) {
		// num is 32-bit so need to check 4 groups of 8 bits
		for (int i = 0; i < 4; i++) {
			int tmp = lastBit8.get((num >> (8*i)) & 0xFF);
			if (tmp != 0)
				return tmp << (8*i);
		}
		return 0;
	}
	
	public static int getNumBits(int num) {
		int returnMe = 0;
		// num is 32-bit so need to check 4 groups of 8 bits
		for (int i = 0; i < 4; i++) {
			returnMe += numBits8.get((num >> (8*i)) & 0xFF);
		}
		return returnMe;
	}
}