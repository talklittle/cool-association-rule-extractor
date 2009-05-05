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
	private int maxWordId;
	
	private HashMap<String, Integer> docIds;
	private HashMap<String, Integer> wordIds;
	private HashMap<Integer, String> idWords;
	private HashMap<Integer, Itemset> wordDocs;
	private HashMap<Integer, HashMap<Integer, Itemset>> multiwordDocs;
	private HashMap<Integer, Itemset> docWords;
	
	private HashSet<Integer> smallWordIds = new HashSet<Integer>(); // single words that are not large 1-itemsets
	
	private static HashMap<Integer[], Long> combinationTable = new HashMap<Integer[], Long>();
	private static HashMap<Integer, Long> factorialTable = new HashMap<Integer, Long>();
	
	// INSTRUMENTATION
	static long instrAprioriPrune = 0, instrAprioriPruneCount = 0;
	static long instrCombineIds = 0, instrCombineIdsCount = 0;

	public Apriori(HashMap<String, Integer> newDocIds,
			       HashMap<String, Integer> newWordIds,
			       HashMap<Integer, String> newIdWords,
			       HashMap<Integer, Itemset> newWordDocs,
			       HashMap<Integer, HashMap<Integer, Itemset>> newMultiwordDocs,
			       HashMap<Integer, Itemset> newDocWords,
			       int newMaxWordId,
			       double newMinsup,
			       double newMinconf) {
		docIds = newDocIds;
		wordIds = newWordIds;
		idWords = newIdWords;
		wordDocs = newWordDocs;
		multiwordDocs = newMultiwordDocs;
		docWords = newDocWords;
		maxWordId = newMaxWordId;
		minsup = newMinsup;
		minconf = newMinconf;
	}
	
	public Apriori(HashMap<String, Integer> newDocumentsPosition,
			       HashMap<String, Integer> newWordsPosition,
			       HashMap<Integer, String> newIdWords,
			       HashMap<Integer, Itemset> newWordDocs,
			       HashMap<Integer, HashMap<Integer, Itemset>> newMultiwordDocs,
			       HashMap<Integer, Itemset> newDocWords,
			       int newMaxWordId,
			       double newMinsup,
			       double newMinconf,
			       int newMaxWordsPerItemset) {
		this(newDocumentsPosition, newWordsPosition, newIdWords, newWordDocs, newMultiwordDocs, 
				newDocWords, newMaxWordId, newMinsup, newMinconf);
		maxWordsPerItemset = newMaxWordsPerItemset;
	}

	/**
	 * 
	 * @param large1Itemsets Collection of the large 1-itemsets
	 * @return Set of largest itemsets
	 */
	public ArrayList<List<Itemset>> doApriori(TreeMap<String, Integer> sortedwords) {
		List<Itemset> large1Itemsets = getLarge1Itemsets(sortedwords);
		
		ArrayList<List<Itemset>> L = new ArrayList<List<Itemset>>(); // Large itemsets
		ArrayList<Itemset> Lk;
				
		L.add(new ArrayList<Itemset>()); // The 0-itemsets; an empty set
		L.add(large1Itemsets); // 1-itemsets; gotten from external
		
		for (int k = 2; k<=3; k++) {
//			System.out.println("DEBUG: doApriori: k=" + k);
			Lk = aprioriGen(L.get(k-1), k);

			if (Lk == null) {
				// No more large itemsets were found.
//				System.out.println("DEBUG: doApriori: break because no more large itemsets. k=" + k);
				break;
			} else {
				L.add(Lk);
			}
		}
		
		// DEBUG INSTRUMENTATION
		System.out.println("aprioriGenPrune: " + instrAprioriPrune+" ms " + instrAprioriPruneCount);
		System.out.println("instrCombineIds: " + instrCombineIds+" ms " + instrCombineIdsCount);
		
		return L;
	}
	
	public ArrayList<Itemset> aprioriGen(List<Itemset> prevL, int k) {
		ArrayList<Itemset> newCandidates = new ArrayList<Itemset>(); // will replace L at the end
		ArrayList<Itemset> groupCandidates = new ArrayList<Itemset>(); // candidates sharing k-2 prefix
		Itemset groupPrefix = new Itemset(); // holds k-2 prefix (last bit chopped off from large itemsets from prev. round)
		
		if (k == 2) {
			// Loop through the 1-itemsets, try to combine with all later 1-itemsets
			// We can use multiwordDocs table
			for (int i = 0; i < prevL.size() - 1; i++) {
				Itemset itsa = prevL.get(i);
				Integer idA = (itsa.ranges[0] * 32) + Bits.getPosFromLeft(itsa.words[0]);
				
				if (smallWordIds.contains(idA)) {
					// This should not happen if getLarge1Itemsets() is correct!
					continue;
				}
				
				for (int j = i+1; j < prevL.size(); j++) {
					Itemset itsb = prevL.get(j);
					Integer idB = (itsb.ranges[0] * 32) + Bits.getPosFromLeft(itsb.words[0]);
					Integer[] ab = { idA, idB };
					Arrays.sort(ab); // just in case

					// Prune
					HashMap<Integer, Itemset> tmp = multiwordDocs.get(ab[0]);
					if (tmp == null) {
						// idA does not appear in any docs...? Shouldn't happen
						System.err.println("ERROR: aprioriGen: idA="+ab[0]+" has no entry in multiwordDocs");
						continue;
					}
					Itemset sharedDocs = tmp.get(ab[1]);
					if (sharedDocs == null) {
						// The 2 words don't appear in the same documents
//						System.out.println("DEBUG: aprioriGen: ab={"+ab[0]+","+ab[1]+"} not in multiwordDocs");
						continue;
					}
					if ((double)sharedDocs.getNumBits() / (double)docIds.size() < minsup) {
						// Not enough documents contain both. i.e., support too low
						continue;
					}
					if (smallWordIds.contains(ab[1])) {
						// idB is not a large itemset so give up.
						// If getLarge1Itemsets() is correct, this should not happen.
						System.err.println("ERROR: aprioriGen: idB="+ab[1]+" was a Small itemset");
						continue;
					}
					
					// Done pruning
					newCandidates.add(new Itemset(Arrays.asList(ab)));
				}
			}
		} else {
			// Loop through the k-1 itemsets (saved from the previous apriori iteration)
			// and try to combine itemsets with those that come after,
			// if they share the same prefix k-2 bits
			for (Itemset kmin1Itemset : prevL) {
	//			System.out.println("DEBUG: aprioriGen: kmin1Itemset:");
	//			kmin1Itemset.debugPrintWords(idWords);
				Itemset currPrefix = kmin1Itemset.chopLastBit();
	//			System.out.println("currPrefix:");
	//			currPrefix.debugPrintWords(idWords);
	//			if (kmin1Itemset.getNumWords() != currPrefix.getNumWords() + 1) {
	//				System.err.println("ERROR: aprioriGen: kmin1Itemset #words="
	//						+kmin1Itemset.getNumWords()
	//						+" currPrefix #words="+currPrefix.getNumWords());
	//			}
				
				if (!currPrefix.equals(groupPrefix)) {
					// Try to combine the current group (if it has >= 2 members)
					newCandidates.addAll(aprioriGenPrune(groupCandidates, prevL, k));
					// Initialize the next group's Set
					groupPrefix = currPrefix;
					groupCandidates = new ArrayList<Itemset>();
				}
				groupCandidates.add(kmin1Itemset);
			}
			
			newCandidates.addAll(aprioriGenPrune(groupCandidates, prevL, k));
		}
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
	
	private ArrayList<Itemset> aprioriGenPrune(List<Itemset> groupCandidates, List<Itemset> prevL, int k) {
		ArrayList<Itemset> newCandidates = new ArrayList<Itemset>();
		
		instrAprioriPrune = System.currentTimeMillis() - instrAprioriPrune;
		instrAprioriPruneCount++;
		
		if (groupCandidates.size() >= 2) {
			// Split off the previous group, combine and try add to newCandidates
			Itemset a = null;
			for (int i = 0; i < groupCandidates.size() - 1; i++) {
				a = groupCandidates.get(i);
				int aLastId = (a.ranges[a.ranges.length-1] * 32) 
					+ Bits.getPosFromLeft(Bits.getLastBit(a.words[a.words.length-1]));
				SortedSet<Integer> toCombine = getCombineIds(a, aLastId+1);
				
				for (Iterator<Integer> it = toCombine.iterator(); it.hasNext(); /* */) {
					Integer combineId = it.next();
					
					// Combine a and b
					Itemset combined = a.addAndCopy(Itemset.posToRange(combineId),
							Itemset.posToBitmask(combineId));
					
//					System.out.println("DEBUG: aprioriGenPrune combineId="+combineId
//							+" a, combined (next 2 lines)");
//					a.debugPrintWords(idWords);
//					combined.debugPrintWords(idWords);
					
					if (FileReader.getItemsetSupport(combined) < minsup) {
//						System.out.println("DEBUG: aprioriGen: support="+FileReader.getItemsetSupport(combined)
//								+ " < minsup="+minsup+" for following line:");
//						combined.debugPrintWords(idWords);
						System.err.println("ERROR: Trying to add itemset k="+k+" with insufficient support="+FileReader.getItemsetSupport(combined));
						continue;
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
						continue;
					}
					
					// Survived pruning so add to newCandidates
					newCandidates.add(combined);
//					System.out.println("DEBUG: aprioriGen: added new candidate:");
//					combined.debugPrintWords(idWords);
				}
			}
		}
		instrAprioriPrune = System.currentTimeMillis() - instrAprioriPrune;
		
//		System.out.println("DEBUG: aprioriGenPrune: k="+k+" called "+instrAprioriPruneCount+" times"
//				+" groupCandidates has "+groupCandidates.size()+" Itemsets"
//				+" returning "+newCandidates.size()+" new large itemsets");
		
		return newCandidates;
	}
	
	public SortedSet<Integer> getCombineIds(Itemset initial, int minimumId) {
		
		instrCombineIds = System.currentTimeMillis() - instrCombineIds;
		instrCombineIdsCount++;
		
		SortedSet<Integer> combineIds = new TreeSet<Integer>();
		Itemset docsWithInitial;
		
		double threshold = minsup * (double)docIds.size();
//		Integer largestLtThreshold = 0; // Largest word count less than threshold
		
		if (initial.getNumBits() == 2) {
			// We can use multiwordDocs
			Integer a = initial.ranges[0]*32 + Bits.getPosFromLeft(Bits.getFirstBit(initial.words[0]));
			Integer b = initial.ranges[initial.ranges.length-1]*32
					+ Bits.getPosFromLeft(Bits.getLastBit(initial.words[initial.words.length-1]));
			Integer[] ab = { a, b };
			Arrays.sort(ab); // Just in case
			HashMap<Integer, Itemset> tmp = multiwordDocs.get(ab[0]);
			if (tmp == null) {
				// A is not in any documents with other words. This should not happen.
				System.err.println("ERROR: getCombineIds: no multiwordDocs for a="+ab[0]);
				instrCombineIds = System.currentTimeMillis() - instrCombineIds;
				return combineIds;
			}
			docsWithInitial = tmp.get(ab[1]);
			if (docsWithInitial == null) {
				// A and B do not share any docs. This should not happen since A and B are large 2-itemset
				System.err.println("ERROR: getCombineIds: no multiwordDocs for ab={"+ab[0]+","+ab[1]+"}");
			}
		} else {
			// Presumably slower than the above block; use with k >= 4 (not in this prog!)
			System.err.println("WARN: getCombineIds: unexpected conditional branch");
			docsWithInitial = initial.getDocIdsIntersection(wordDocs);
		}
		for (Integer wordId = minimumId; wordId <= maxWordId; wordId++) {
			if (smallWordIds.contains(wordId) || !wordDocs.containsKey(wordId)) {
				// Ignore word Ids below a minimum Id (preserve ordering)
				// and ignore COMMON words
				continue;
			}
			Itemset intersectionOfDocs = docsWithInitial.intersect(wordDocs.get(wordId));
			if (intersectionOfDocs.getNumBits() >= threshold) {
				combineIds.add(wordId);
			}
		}
		instrCombineIds = System.currentTimeMillis() - instrCombineIds;
		return combineIds;
	}
	
	/**
	 * Return mathematical combination: top C bottom (usually expressed: n C k)
	 * = n! / (k!(n-k)!)
	 * @param top
	 * @param bottom
	 * @return
	 */
	public static long combination(int top, int bottom) {
		if (top - bottom == 1)
			return top;
		return factorial(top) / factorial(bottom) / factorial(top-bottom);
	}
	
	public static long factorial(int n) {
		if (factorialTable.containsKey(n))
			return factorialTable.get(n);
		if (n == 1 || n == 0)
			return 1;
		factorialTable.put(n, n * factorial(n-1));
		return factorialTable.get(n);
	}
	
	public List<Itemset> getLarge1Itemsets (TreeMap<String, Integer> sortedwords){
		ArrayList<Itemset> result = new ArrayList<Itemset>();
		Itemset[] tmp;
		for(String s: sortedwords.keySet()){
			Itemset largeItem;
			int position = wordIds.get(s);
			int[] rangeId= { Itemset.posToRange(position) };
			int[] wordId = { Itemset.posToBitmask(position) };
//			System.out.println("DEBUG: getLarge1Itemsets: s "+s+" wordId "+position+" range "+rangeId[0]
//					+" wordId is "+wordId[0]+" and has " + Bits.getNumBits(wordId[0]) + " bits");
			largeItem = new Itemset(rangeId, wordId);
			
			if (FileReader.getItemsetSupport(largeItem) >= minsup) {
				if (result.add(largeItem)) {
//					System.out.println("DEBUG: getLarge1Itemsets: added word " + idWords.get(position));
				} else {
					System.err.println("WARN: getLarge1Itemsets: Trying to add existing Itemset(next line)");
					largeItem.debugPrintWords(idWords);
				}
			} else {
				smallWordIds.addAll(largeItem.getIds());
			}
		}
		
//		System.out.println("DEBUG: getLarge1Itemsets: result.size()="+result.size());
		tmp = result.toArray(new Itemset[0]);
		Arrays.sort(tmp);
		return Arrays.asList(tmp);
	}
}
