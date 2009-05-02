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
	private TreeSet<Itemset> Ck;
	private int k; // The current iteration of the algorithm
	private int maxWordsPerItemset = 0;
	
	private HashMap<String, Integer> docIds;
	private HashMap<String, Integer> wordIds;
	private HashMap<Integer, Itemset> wordDocs;
	private HashMap<Integer, Itemset> docWords;
	
	public Apriori(HashMap<String, Integer> newDocIds,
			       HashMap<String, Integer> newWordIds,
			       HashMap<Integer, Itemset> newWordDocs,
			       HashMap<Integer, Itemset> newDocWords,
			       double newMinsup,
			       double newMinconf) {
		docIds = newDocIds;
		wordIds = newWordIds;
		wordDocs = newWordDocs;
		minsup = newMinsup;
		minconf = newMinconf;
		Ck = new TreeSet<Itemset>();
	}
	
	public Apriori(HashMap<String, Integer> newDocumentsPosition,
			       HashMap<String, Integer> newWordsPosition,
			       HashMap<Integer, Itemset> newWordDocs,
			       HashMap<Integer, Itemset> newDocWords,
			       double newMinsup,
			       double newMinconf,
			       int newMaxWordsPerItemset) {
		this(newDocumentsPosition, newWordsPosition, newWordDocs, newDocWords, newMinsup, newMinconf);
		maxWordsPerItemset = newMaxWordsPerItemset;
	}

	/**
	 * 
	 * @param large1Itemsets Collection of the large 1-itemsets
	 * @return Set of largest itemsets
	 */
	public ArrayList<Set<Itemset>> doApriori(TreeMap<String, Integer> sortedwords, HashMap<String, Integer> wordIds) {
		Set<Itemset> large1Itemsets = getLarge1Itemsets(sortedwords, wordIds);
		
		ArrayList<Set<Itemset>> L = new ArrayList<Set<Itemset>>(); // Large itemsets
		//ArrayList<Itemset> C = new ArrayList<Itemset>(); // Candidate Large itemsets
		Set<Itemset> Lk;
				
		L.add(new HashSet<Itemset>()); // The 0-itemsets; an empty set
		L.add(large1Itemsets); // 1-itemsets; gotten from external
		
		// XXX these 2 needed?
		//C.add(new HashSet<Itemset>()); // Candidate 0-itemsets; empty set
		//C.add(new HashSet<Itemset>()); // Candidate 1-itemsets; empty set
		
		for (k = 2; k<4; k++) {
			//C[k] = aprioriGen(L[k-1]); // New candidates
			Lk = aprioriGen(Ck, k); // Will update Ck
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
			L.add(Lk);
		}
		return L;
	}
	
	public SortedSet<Itemset> aprioriGen(SortedSet<Itemset> prevL, int k) {
		TreeSet<Itemset> newCandidates = new TreeSet<Itemset>(); // will replace L at the end
		TreeSet<Itemset> groupCandidates = new TreeSet<Itemset>(); // candidates sharing k-2 prefix
		Itemset groupPrefix = null; // holds k-2 prefix (last bit chopped off from large itemsets from prev. round)
		
		// Loop through the k-1 itemsets (saved from the previous apriori iteration)
		// and try to combine itemsets with those that come after,
		// if they share the same prefix k-2 bits
		for (Itemset kmin1Itemset : prevL) {
			Itemset currPrefix = kmin1Itemset.chopLastBit();
			if (!currPrefix.equals(groupPrefix)) {
				if (groupCandidates.size() >= 2) {
					// Split off the previous group, combine and try add to newCandidates
					for (Iterator<Itemset> it = groupCandidates.iterator(); it.hasNext(); /* */) {
						Itemset a = it.next();
						SortedSet<Itemset> bigger = groupCandidates.tailSet(a);
						for (Iterator<Itemset> itb = bigger.iterator(); itb.hasNext(); /* */) {
							Itemset b = itb.next();
							int bLastRange = b.ranges[b.ranges.length-1];
							int bLastBit = Bits.getLastBit(b.words[b.words.length-1]);
							// Combine a and b
							Itemset combined = a.addAndCopy(bLastRange, bLastBit);
							
							// Pruning based on minsup
							double support = 0;
							for (Iterator<Itemset> wiadIt = docWords.values().iterator(); wiadIt.hasNext(); /* */) {
								Itemset wordsInADoc = wiadIt.next();
								if (wordsInADoc.contains(combined)) {
									support++;
								}
							}
							support /= docWords.size(); // Ratio of containing transactions
							if (support < minsup) {
								break;
							}
							
							// Pruning based on whether all subsets are part of the k-1 large itemsets
							int numLargeSubsetsOfCandidate = 0;
							for (Itemset kmin1Itemset2 : prevL) {
								if (combined.contains(kmin1Itemset2))
									numLargeSubsetsOfCandidate++;
							}
							if (numLargeSubsetsOfCandidate < combination(k, k-1)) {
								break;
							}
							
							// Survived pruning so add to newCandidates
							newCandidates.add(combined);
						}
					}
				}
				// Initialize the next group's Set
				groupPrefix = currPrefix;
				groupCandidates = new TreeSet<Itemset>();
			}
			groupCandidates.add(kmin1Itemset);
		}
		
		// Finally check if newCandidates is empty. If so then the previous set of
		// large itemsets is the final one.
		if (newCandidates.size() > 0) {
			return newCandidates;
		} else {
			// Ck == prevL
			return null;
		}
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
	
	public static Set<Itemset> getLarge1Itemsets (TreeMap<String, Integer> sortedwords, HashMap<String, Integer> wordIds){
		Set<Itemset> result = new TreeSet<Itemset>();
		for(String s: sortedwords.keySet()){
			Itemset largeItem = null;
			int position = wordIds.get(s);
			int[] rangeId= { Itemset.posToRange(position) };
			int[] wordId= { Itemset.posToBitmask(position) };
			largeItem = new Itemset(rangeId, wordId);
			result.add(largeItem);
			}
		return result;
		}
}
