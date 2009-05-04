package coms6111.proj3;

import java.util.*;

/**
 * 1) L[1] = {large 1-itemsets};
 * 2) for ( k = 2; L[k-1] != empty_set; k++ ) do begin
 * 3)   C[k] = apriori-gen(L[k-1]); // New candidates
 * 4)   forall transactions t in D do begin
 * 5)     C[t] = subset(C[k], t); // Candidates contained in t
 * 6)     forall candidates c in C[t] do
 * 7)       c.count++;
 * 8)   end
 * 9)   Lk = {c in C[k] | c.count >= minsup}
 * 10) end
 * 11) Answer = Union[k] L[k];
 *
 */
public class Apriori {
	
	private double minsup, minconf;
	private int maxWordsPerItemset = 0;
	
	private HashMap<String, Integer> docIds;
	private HashMap<String, Integer> wordIds;
	private HashMap<Integer, String> idWords;
	private HashMap<Integer, Itemset> wordDocs;
	private HashMap<Integer, Itemset> docWords;
	
	// INSTRUMENTATION
	static long instrAprioriPrune = 0, instrAprioriPruneCount = 0;

	public Apriori(HashMap<String, Integer> newDocIds,
			       HashMap<String, Integer> newWordIds,
			       HashMap<Integer, String> newIdWords,
			       HashMap<Integer, Itemset> newWordDocs,
			       HashMap<Integer, Itemset> newDocWords,
			       double newMinsup,
			       double newMinconf) {
		docIds = newDocIds;
		wordIds = newWordIds;
		idWords = newIdWords;
		wordDocs = newWordDocs;
		docWords = newDocWords;
		minsup = newMinsup;
		minconf = newMinconf;
	}
	
	public Apriori(HashMap<String, Integer> newDocumentsPosition,
			       HashMap<String, Integer> newWordsPosition,
			       HashMap<Integer, String> newIdWords,
			       HashMap<Integer, Itemset> newWordDocs,
			       HashMap<Integer, Itemset> newDocWords,
			       double newMinsup,
			       double newMinconf,
			       int newMaxWordsPerItemset) {
		this(newDocumentsPosition, newWordsPosition, newIdWords, newWordDocs, newDocWords, newMinsup, newMinconf);
		maxWordsPerItemset = newMaxWordsPerItemset;
	}

	/**
	 * 
	 * @param large1Itemsets Collection of the large 1-itemsets
	 * @return Set of largest itemsets
	 */
	public ArrayList<SortedSet<Itemset>> doApriori(TreeMap<String, Integer> sortedwords) {
		SortedSet<Itemset> large1Itemsets = getLarge1Itemsets(sortedwords);
		
		ArrayList<SortedSet<Itemset>> L = new ArrayList<SortedSet<Itemset>>(); // Large itemsets
		SortedSet<Itemset> Lk;
				
		L.add(new TreeSet<Itemset>()); // The 0-itemsets; an empty set
		L.add(large1Itemsets); // 1-itemsets; gotten from external
		
		for (int k = 2; k<=3; k++) {
//			System.out.println("DEBUG: doApriori: k=" + k);
			Lk = aprioriGen(L.get(k-1), k); // Will update Ck
//			for (Iterator<String> it = docIds.keySet().iterator(); it.hasNext(); /* */) {
//				String transaction = it.next();
//
//				C[transaction] = subset(Ck, transaction); // Candidates contained in t
//				for (Candidate c : C[transaction]) {
//					c.count++;
//				}
//			}
//			HashSet<Itemset> Lk = new HashSet<Itemset>();
//			L.append(new HashSet<Itemset>()); // Set of k-itemsets
////			for (Itemset c : C[k]) {
//			for (ItemsetTrie c : leaves) {
//				Lk.add(c);
//			}
			if (Lk == null) {
				// No more large itemsets were found.
//				System.out.println("DEBUG: doApriori: break because no more large itemsets. k=" + k);
				break;
			} else {
				L.add(Lk);
			}
		}
		
		// DEBUG
//		System.out.println("aprioriGenPrune: " + instrAprioriPrune+"ms " + instrAprioriPruneCount);
		
		return L;
	}
	
	public SortedSet<Itemset> aprioriGen(SortedSet<Itemset> prevL, int k) {
		TreeSet<Itemset> newCandidates = new TreeSet<Itemset>(); // will replace L at the end
		TreeSet<Itemset> groupCandidates = new TreeSet<Itemset>(); // candidates sharing k-2 prefix
		Itemset groupPrefix = new Itemset(); // holds k-2 prefix (last bit chopped off from large itemsets from prev. round)
		
		// Loop through the k-1 itemsets (saved from the previous apriori iteration)
		// and try to combine itemsets with those that come after,
		// if they share the same prefix k-2 bits
		for (Itemset kmin1Itemset : prevL) {
//			System.out.println("DEBUG: aprioriGen: kmin1Itemset:");
//			kmin1Itemset.debugPrintWords(idWords);
			Itemset currPrefix = kmin1Itemset.chopLastBit();
//			System.out.println("currPrefix:");
//			currPrefix.debugPrintWords(idWords);
			if (kmin1Itemset.getNumWords() != currPrefix.getNumWords() + 1) {
				System.err.println("ERROR: aprioriGen: kmin1Itemset #words="
						+kmin1Itemset.getNumWords()
						+" currPrefix #words="+currPrefix.getNumWords());
			}
			
			if (!currPrefix.equals(groupPrefix)) {
				// Try to combine the current group (if it has >= 2 members)
				newCandidates.addAll(aprioriGenPrune(groupCandidates, prevL, k));
				// Initialize the next group's Set
				groupPrefix = currPrefix;
				groupCandidates = new TreeSet<Itemset>();
			}
			groupCandidates.add(kmin1Itemset);
		}
		
		newCandidates.addAll(aprioriGenPrune(groupCandidates, prevL, k));
		
		// Finally check if newCandidates is empty. If so then the previous set of
		// large itemsets is the final one.
		if (newCandidates.size() > 0) {
			return newCandidates;
		} else {
			// Ck == prevL
//			System.out.println("DEBUG: aprioriGen: newCandidates is empty at k="+k);
			return null;
		}
	}
	
	private SortedSet<Itemset> aprioriGenPrune(SortedSet<Itemset> groupCandidates, SortedSet<Itemset> prevL, int k) {
		TreeSet<Itemset> newCandidates = new TreeSet<Itemset>();
		
		instrAprioriPrune = System.currentTimeMillis() - instrAprioriPrune;
		instrAprioriPruneCount++;
		
		if (groupCandidates.size() >= 2) {
			// Split off the previous group, combine and try add to newCandidates
			Itemset a = null, afterA = null;
			for (Iterator<Itemset> it = groupCandidates.iterator(); it.hasNext(); /* */) {
				if (a == null) {
					a = it.next();
					continue;
				} else if (afterA == null) {
					afterA = it.next();
				} else {
					a = afterA;
					afterA = it.next();
				}

				SortedSet<Itemset> bigger = groupCandidates.tailSet(afterA);
				for (Iterator<Itemset> itb = bigger.iterator(); itb.hasNext(); /* */) {
					Itemset b = itb.next();
					int bLastRange = b.ranges[b.ranges.length-1];
					int bLastBit = Bits.getLastBit(b.words[b.words.length-1]);
					if (bLastBit == 0) {
						System.err.println("ERROR: aprioriGenPrune: bLastBit is 0");
					}
					// Combine a and b
					Itemset combined = a.addAndCopy(bLastRange, bLastBit);

//					// DEBUG
//					List<Integer> azzz = a.getWordIds();
//					List<Integer> bzzz = b.getWordIds();
//					System.out.println("DEBUG: a numwords "+a.getNumWords()+" lastRange "+a.ranges[a.ranges.length-1]
//					                 + " contains b:" + a.containsWordIds(bzzz));
//					a.debugPrintWords(idWords);
//					System.out.println("DEBUG: b numwords "+b.getNumWords()+" lastRange "+b.ranges[b.ranges.length-1]
//					                 + " contains a:" + b.containsWordIds(azzz));
//					b.debugPrintWords(idWords);
//					System.out.println("DEBUG: c numwords "+combined.getNumWords()
//							         + " contains a:" + combined.containsWordIds(azzz)
//							         + " contains b:" + combined.containsWordIds(bzzz));
//					combined.debugPrintWords(idWords);
//					System.out.println("DEBUG: c " + combined.ranges[0] + " " + combined.ranges[1]);
//					System.out.println("DEBUG: c " + combined.words[0] + " " + combined.words[1]);
					
					
					if (FileReader.getItemsetSupport(combined) < minsup) {
//						System.out.println("DEBUG: aprioriGen: support < minsup for following line:");
//						combined.debugPrintWords(idWords);
						break;
					}
					
					// Pruning based on whether all subsets are part of the k-1 large itemsets
					int numLargeSubsetsOfCandidate = 0;
					for (Itemset kmin1Itemset2 : prevL) {
						if (combined.contains(kmin1Itemset2)) {
							numLargeSubsetsOfCandidate++;
						}
					}
					if (numLargeSubsetsOfCandidate < combination(k, k-1)) {
//						System.out.println("DEBUG: aprioriGen: not all subsets are in k-1. k="+k+" num="
//								+numLargeSubsetsOfCandidate+" expected="+combination(k,k-1));
//						combined.debugPrintWords(idWords);
						break;
					}
					
					// Survived pruning so add to newCandidates
					newCandidates.add(combined);
//					System.out.println("DEBUG: aprioriGen: added new candidate:");
//					combined.debugPrintWords(idWords);
				}
			}
		}
		instrAprioriPrune = System.currentTimeMillis() - instrAprioriPrune;
		return newCandidates;
	}
	
	/**
	 * Return mathematical combination: top C bottom (usually expressed: n C k)
	 * = n! / (k!(n-k)!)
	 * @param top
	 * @param bottom
	 * @return
	 */
	public static long combination(int top, int bottom) {
		return factorial(top) / (factorial(bottom) * factorial(top-bottom));
	}
	
	public static long factorial(int n) {
		if (n == 1 || n == 0)
			return 1;
		return n * factorial(n-1);
	}
	
	public SortedSet<Itemset> getLarge1Itemsets (TreeMap<String, Integer> sortedwords){
		SortedSet<Itemset> result = new TreeSet<Itemset>();
		for(String s: sortedwords.keySet()){
			Itemset largeItem;
			int position = wordIds.get(s);
			int[] rangeId= { Itemset.posToRange(position) };
			int[] wordId = { Itemset.posToBitmask(position) };
//			System.out.println("DEBUG: getLarge1Itemsets: s "+s+" wordId "+position+" range "+rangeId[0]);
//			System.out.println("DEBUG: getLarge1Itemsets: wordId is "+wordId[0]+" and has " + Bits.getNumBits(wordId[0]) + " bits");
			largeItem = new Itemset(rangeId, wordId);
			
			if (FileReader.getItemsetSupport(largeItem) >= minsup) {
				result.add(largeItem);
//				System.out.println("DEBUG: getLarge1Itemsets: added word " + idWords.get(position));
			}
		}
		
		return result;
	}
}
