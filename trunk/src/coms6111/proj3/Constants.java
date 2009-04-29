package coms6111.proj3;

import java.util.HashMap;
import java.util.HashSet;

public class Constants {
	public static HashSet<Integer> singleBit = null; // All of the 32-bit integers with a single bit set 
	public static HashMap<Integer, Integer> lastBit = null; // Returns the value of the least significant bit for all 32-bit numbers
	public static HashMap<Integer, Integer> numBits = null; // The number of 1-bits in this 32-bit integer
	
	public static void init() {
		initSingleBit();
		initLastBit();
		initNumBits();
	}
	
	private static void initSingleBit() {
		singleBit = new HashSet<Integer>();
		for (long i = 1; i <= 0xFFFFFFFFL; i <<= 1) {
			singleBit.add((int) i);
		}
	}
	
	private static void initLastBit() {
		lastBit = new HashMap<Integer, Integer>();
		for (long i = 1; i <= 0xFFFFFFFFL; i <<= 1) {
			for (long j = i; j <= 0xFFFFFFFFL; j += 2*i) {
				if (!lastBit.containsKey((int)j)) {
					lastBit.put((int)j, (int)i);
				}
			}
		}
	}
	
	private static void initNumBits() {
		numBits = new HashMap<Integer, Integer>();
		numBits.put(0, 0);
		for (long i = 1; i <= 0xFFFFFFFFL; i++) {
			numBits.put((int)i, (int)(i & 1) + numBits.get(i >> 1));
		}
	}
}
