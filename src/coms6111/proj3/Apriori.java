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
	
	public HashMap<Double, List<Itemset>> supportItemsets = new HashMap<Double, List<Itemset>>(); 
	
	private double minsup, minconf;
	private int maxWordsPerItemset = 0;
	private int maxWordId;
	
	private HashMap<String, Integer> docIds;
	private HashMap<String, Integer> wordIds;
	private HashMap<Integer, String> idWords;
	private HashMap<Integer, Itemset> wordDocs;
	private HashMap<Integer, TreeSet<Integer>> multiwordDocs;
	private HashMap<Integer, Itemset> docWords;
	
	private HashSet<Integer> smallWordIds = new HashSet<Integer>(); // single words that are not large 1-itemsets
	
	private static HashMap<Integer, Long> factorialTable = new HashMap<Integer, Long>();
	
	// INSTRUMENTATION
	static long instrAprioriPrune = 0, instrAprioriPruneCount = 0;
	static long instrCombineIds = 0, instrCombineIdsCount = 0;

	public Apriori(HashMap<String, Integer> newDocIds,
			       HashMap<String, Integer> newWordIds,
			       HashMap<Integer, String> newIdWords,
			       HashMap<Integer, Itemset> newWordDocs,
			       HashMap<Integer, TreeSet<Integer>> newMultiwordDocs,
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
			       HashMap<Integer, TreeSet<Integer>> newMultiwordDocs,
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
		
//		// DEBUG INSTRUMENTATION
//		System.out.println("aprioriGenPrune: " + instrAprioriPrune+" ms " + instrAprioriPruneCount);
//		System.out.println("instrCombineIds: " + instrCombineIds+" ms " + instrCombineIdsCount);
		
		return L;
	}
	
	public ArrayList<Itemset> aprioriGen(List<Itemset> prevL, int k) {
		ArrayList<Itemset> newCandidates = new ArrayList<Itemset>(); // will replace L at the end
		ArrayList<Itemset> groupCandidates = new ArrayList<Itemset>(); // candidates sharing k-2 prefix
		Itemset groupPrefix = new Itemset(); // holds k-2 prefix (last bit chopped off from large itemsets from prev. round)
		
		if (k == 2 && multiwordDocs != null) {
			// Loop through the 1-itemsets, try to combine with all later 1-itemsets
			// We can use multiwordDocs table
			for (int i = 0; i < prevL.size() - 1; i++) {
				Itemset itsa = prevL.get(i);
				Integer idA = (itsa.ranges[0] * 32) + Bits.getPosFromLeft(itsa.words[0]);
				
				if (smallWordIds.contains(idA)) {
					// This should not happen if getLarge1Itemsets() is correct!
					System.err.println("ERROR: aprioriGen: smallWordIds contains idA="+idA);
					continue;
				}
				
				SortedSet<Integer> docIdsShared = multiwordDocs.get(idA);
				for (Iterator<Integer> itj = docIdsShared.iterator(); itj.hasNext(); /* */) {
					Integer idB = itj.next();
//					System.out.println("DEBUG: aprioriGen: idA="+idA+"idB="+idB);
					
					if (smallWordIds.contains(idB)) {
						continue;
					}

					Itemset combined = itsa.addAndCopy(Itemset.posToRange(idB), 
							Itemset.posToBitmask(idB));
					
					Itemset docIdsIntersection = combined.getDocIdsIntersection(wordDocs);
					
					double theSup = (double)docIdsIntersection.getNumBits() / (double)docIds.size();
					if (theSup < minsup) {
						// Not enough documents contain both. i.e., support too low
						continue;
					}
					
					// Done pruning
					newCandidates.add(combined);
//					System.out.println("DEBUG: aprioriGen: k=2 added combined(next line)");
//					combined.debugPrintWords(idWords);
//					if (supportItemsets.containsKey(theSup)) {
//						supportItemsets.get(theSup).add(addMe);
//					} else {
//						ArrayList<Itemset> newlist = new ArrayList<Itemset>();
//						newlist.add(addMe);
//						supportItemsets.put(theSup, newlist);
//					}
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
		boolean pleaseContinue = false;
		
		instrAprioriPrune = System.currentTimeMillis() - instrAprioriPrune;
		instrAprioriPruneCount++;
		
		if (groupCandidates.size() >= 2) {
			// Split off the previous group, combine and try add to newCandidates
			Itemset a = null;
			for (int i = 0; i < groupCandidates.size() - 1; i++) {
				a = groupCandidates.get(i);
				List<Integer> idsInA = a.getIds();
				
				for (int j = i+1; j < groupCandidates.size(); j++) {
					Itemset b = groupCandidates.get(j);
					Integer bLastId = b.ranges[b.ranges.length-1]*32
							+ Bits.getPosFromLeft(Bits.getLastBit(b.words[b.words.length-1]));
					
					pleaseContinue = false;
					for (Integer idInA : idsInA) {
						if (multiwordDocs != null && !multiwordDocs.get(idInA).contains(bLastId)) {
							// the Id you're trying to add does not appear in any docs
							// with one of the Ids from the first itemset
//							System.out.println("DEBUG: aprioriGenPrune: idInA="+idInA
//									+" does not share doc with bLastId="+bLastId);
							pleaseContinue = true;
							break;
						}
					}
					if (pleaseContinue)
						continue;
					
					// Combine a and b
					Itemset combined = a.addAndCopy(b.ranges[b.ranges.length-1],
							b.words[b.words.length-1]);
					
//					System.out.println("DEBUG: aprioriGenPrune combineId="+combineId
//							+" a, combined (next 2 lines)");
//					a.debugPrintWords(idWords);
//					combined.debugPrintWords(idWords);
					
					double theSup = FileReader.getItemsetSupport(combined);
					if (theSup < minsup) {
//						System.out.println("DEBUG: aprioriGen: support="+FileReader.getItemsetSupport(combined)
//								+ " < minsup="+minsup+" for following line:");
//						combined.debugPrintWords(idWords);
//						System.err.println("ERROR: Trying to add itemset k="+k+" with insufficient support="+FileReader.getItemsetSupport(combined));
						continue;
					}
					
					// Pruning based on whether all subsets are part of the k-1 large itemsets
					int numLargeSubsetsOfCandidate = countMin1Subsets(combined, prevL);
					if (numLargeSubsetsOfCandidate < combination(k, k-1)) {
//						System.out.println("DEBUG: aprioriGen: not all subsets are in k-1. k="+k+" num="
//								+numLargeSubsetsOfCandidate+" expected="+combination(k,k-1));
//						combined.debugPrintWords(idWords);
						continue;
					}
					
					// Survived pruning so add to newCandidates
					newCandidates.add(combined);
//					if (supportItemsets.containsKey(theSup)) {
//						supportItemsets.get(theSup).add(combined);
//					} else {
//						ArrayList<Itemset> newlist = new ArrayList<Itemset>();
//						newlist.add(combined);
//						supportItemsets.put(theSup, newlist);
//					}
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
	
	public static int countMin1Subsets(Itemset superset, List<Itemset> goodSubsets) {
		int count = 0;
		for (Integer id : superset.getIds()) {
			Itemset min1 = superset.removeAndCopy(Itemset.posToRange(id), Itemset.posToBitmask(id));
			int left, mid, right;
			left = 0;
			right = goodSubsets.size();
			// Binary search to find each len-1 subset of superset, in goodSubsets
			while (left < right) {
				mid = left + (right - left) / 2;
				int cmp = min1.compareTo(goodSubsets.get(mid));
				if (cmp > 0) {
					left = mid + 1;
				} else if (cmp < 0) {
					right = mid;
				} else {
					count++;
					break;
				}
			}
		}
		return count;
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
	
//	public static class ItemsetWithSupport implements Comparable<ItemsetWithSupport> {
//		public Itemset itemset;
//		public double support;
//		private int hashcode;
//		private static int codes = 0;
//		
//		public ItemsetWithSupport(Itemset it, double sup) {
//			itemset = it;
//			support = sup;
//			hashcode = codes++;
//		}
//		
//		/**
//		 * Higher support comes first. i.e., always sort in decreasing order
//		 */
//		public int compareTo(ItemsetWithSupport o) {
//			if (this.support > o.support)
//				return -1;
//			if (this.support < o.support)
//				return 1;
//			return 0;
//		}
//		
//		public boolean equals(ItemsetWithSupport o) {
//			return this.hashCode() == o.hashCode();
//		}
//		
//		public int hashCode() {
//			return hashcode;
//		}
//	}
}
