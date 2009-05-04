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
	
	private HashSet<Integer> smallWordIds = new HashSet<Integer>(); // single words that are not large 1-itemsets
	
	private static HashMap<Integer[], Long> combinationTable = new HashMap<Integer[], Long>();
	private static HashMap<Integer, Long> factorialTable = new HashMap<Integer, Long>();
	
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
		
		// DEBUG
//		System.out.println("aprioriGenPrune: " + instrAprioriPrune+"ms " + instrAprioriPruneCount);
		
		return L;
	}
	
	public ArrayList<Itemset> aprioriGen(List<Itemset> prevL, int k) {
		ArrayList<Itemset> newCandidates = new ArrayList<Itemset>(); // will replace L at the end
		ArrayList<Itemset> groupCandidates = new ArrayList<Itemset>(); // candidates sharing k-2 prefix
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
				Set<Integer> aDocs = a.getDocIdsIntersection(wordDocs);
				SortedSet<Integer> toCombine = getCombineIds(a.getIds(), aDocs, aLastId);
				
				for (Iterator<Integer> it = toCombine.iterator(); it.hasNext(); /* */) {
					Integer combineId = it.next();
					
					// Combine a and b
					Itemset combined = a.addAndCopy(Itemset.posToRange(combineId),
							Itemset.posToBitmask(combineId));

//					// DEBUG
//					List<Integer> azzz = a.getIds();
//					List<Integer> bzzz = b.getIds();
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
	
	public SortedSet<Integer> getCombineIds(Collection<Integer> alreadyHaveIds, Collection<Integer> docs,
			int minimumId) {
		HashSet<Integer> ignoreIds = new HashSet<Integer>();
		ignoreIds.addAll(smallWordIds);
		ignoreIds.addAll(alreadyHaveIds);
		HashMap<Integer, Integer> counts = new HashMap<Integer, Integer>(); // word counts
		SortedSet<Integer> combineIds = new TreeSet<Integer>();
		
		double threshold = minsup * (double)docIds.size();
//		Integer largestLtThreshold = 0; // Largest word count less than threshold
		
		for (Integer docId : docs) {
			Collection<Integer> words = docWords.get(docId).getIds();
			for (Integer wordId : words) {
				if (ignoreIds.contains(wordId) || wordId < minimumId)
					continue;
				if (counts.containsKey(wordId)) {
					counts.put(wordId, counts.get(wordId) + 1);
				} else {
					counts.put(wordId, 1);
				}
			}
		}
		for (Integer wordId : counts.keySet()) {
			if (counts.get(wordId) >= threshold) {
//				System.out.println("DEBUG: getCombineIds: adding; wordId="+wordId+" count="+counts.get(wordId)
//						+" threshold="+threshold);
				combineIds.add(wordId);
			}
		}
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
		Integer[] c = { top, bottom };
		if (combinationTable.containsKey(c))
			return combinationTable.get(c);
		combinationTable.put(c, factorial(top) / (factorial(bottom) * factorial(top-bottom)));
		return combinationTable.get(c);
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
