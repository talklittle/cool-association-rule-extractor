package coms6111.proj3;

import java.util.HashMap;
import java.util.HashSet;

public class Bits {
	public static HashSet<Integer> singleBit = null; // All of the (32) 32-bit integers with a single bit set 
	public static HashMap<Integer, Integer> firstBit256 = null; // The value of the most significant bit for all 8-bit ints
	public static HashMap<Integer, Integer> lastBit256 = null; // The value of the least significant bit for all 8-bit ints
	public static HashMap<Integer, Integer> numBits256 = null; // The number of 1-bits in an 8-bit integer
	public static HashMap<Integer, Integer> posFromLeft = null; // The bit position # counting from left, given a bitmask. index from 0
	
	public static void init() {
		initSingleBit();
		initFirstBit256();
		initLastBit256();
		initNumBits256();
		initPosFromLeft();
	}
	
	private static void initSingleBit() {
		singleBit = new HashSet<Integer>();
		for (long i = 1; i <= 0xFFFFFFFFL; i <<= 1) {
			singleBit.add((int) i);
		}
	}
	
	private static void initFirstBit256() {
		firstBit256 = new HashMap<Integer, Integer>();
		firstBit256.put(0, 0);
		for (int i = 0x80; i >= 1; i >>= 1) {
			for (int j = i; j < 2*i; j++) {
				firstBit256.put(j, i);
			}
		}
	}
	
	private static void initLastBit256() {
		lastBit256 = new HashMap<Integer, Integer>();
		lastBit256.put(0, 0);
		for (int i = 1; i <= 0xFF; i <<= 1) {
			for (int j = i; j <= 0xFF; j += 2*i) {
				if (!lastBit256.containsKey(j)) {
					lastBit256.put(j, i);
				}
			}
		}
	}
	
	private static void initNumBits256() {
		numBits256 = new HashMap<Integer, Integer>();
		numBits256.put(0, 0);
		for (int i = 1; i <= 0xFF; i++) {
			numBits256.put(i, (i & 1) + numBits256.get(i >> 1));
		}
	}
	
	private static void initPosFromLeft() {
		posFromLeft = new HashMap<Integer, Integer>();
		for (int i = 0; i < 31; i++) {
			posFromLeft.put((0x80000000 >> i), i);
		}
	}
	
	public static int getFirstBit(int num) {
		// num is 32-bit so need to check 4 groups of 8 bits
		for (int i = 3; i >= 0; i--) {
			int tmp = firstBit256.get((num >> (8*i)) & 0xFF);
			if (tmp != 0)
				return tmp << (8*i);
		}
		return 0;
	}
	
	public static int getLastBit(int num) {
		// num is 32-bit so need to check 4 groups of 8 bits
		for (int i = 0; i < 4; i++) {
			int tmp = lastBit256.get((num >> (8*i)) & 0xFF);
			if (tmp != 0)
				return tmp << (8*i);
		}
		return 0;
	}
	
	public static int getNumBits(int num) {
		int returnMe = 0;
		// num is 32-bit so need to check 4 groups of 8 bits
		for (int i = 0; i < 4; i++) {
			returnMe += numBits256.get((num >> (8*i)) & 0xFF);
		}
		return returnMe;
	}
	
	public static int getPosFromLeft(int bitmask) {
		if (!posFromLeft.containsKey(bitmask)) {
			// bitmask is supposed to be a single bit set; provided arg is invalid
			return -1;
		}
		return posFromLeft.get(bitmask);
	}
}