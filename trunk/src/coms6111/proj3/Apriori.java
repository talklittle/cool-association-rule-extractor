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
	
	private HashMap<String, Integer> documentsPosition;
	
	public Apriori(double newMinsup, double newMinconf) {
		minsup = newMinsup;
		minconf = newMinconf;
		Ck = new TreeSet<Itemset>();
	}

	/**
	 * 
	 * @param large1Itemsets Collection of the large 1-itemsets
	 * @return Set of largest itemsets
	 */
	public HashSet<Itemset> doApriori() {
		Set<Itemset> large1Itemsets = getLarge1Itemsets();
		
		ArrayList<Set<Itemset>> L = new ArrayList<Set<Itemset>>(); // Large itemsets
		ArrayList<Itemset> C = new ArrayList<Itemset>(); // Candidate Large itemsets
				
		L.add(new HashSet<Itemset>()); // The 0-itemsets; an empty set
		L.add(large1Itemsets); // 1-itemsets; gotten from external
		
		C.add(new HashSet<Itemset>()); // Candidate 0-itemsets; empty set
		C.add(new HashSet<Itemset>()); // Candidate 1-itemsets; empty set
		
		for (k = 2; L.get(k-1).size() > 0; k++) {
			//C[k] = aprioriGen(L[k-1]); // New candidates
			aprioriGen(Ck); // Will update Ck
			for (Iterator<String> it = documentsPosition.keySet().iterator(); it.hasNext(); /* */) {
				String transaction = it.next();

				C[transaction] = subset(Ck, transaction); // Candidates contained in t
				for (Candidate c : C[transaction]) {
					c.count++;
				}
			}
			L.append(new HashSet<Itemset>()); // Set of k-itemsets
//			for (Itemset c : C[k]) {
			for (ItemsetTrie c : leaves) {
				L.get(k).add(c);
			}
		}
		
	}
	
	public HashSet<Itemset> subset(Set<Itemset> ck, Transaction t) {
		
	}
	
	public boolean aprioriGen(SortedSet<Itemset> prevL) {
		TreeSet<Itemset> newCandidates = new TreeSet<Itemset>(); // will replace L at the end
		TreeSet<Itemset> groupCandidates = new TreeSet<Itemset>(); // candidates sharing k-2 prefix
		Itemset groupPrefix = null; // holds k-2 prefix (last bit chopped off from large itemsets from prev. round)
		
		// Loop through the k-1 itemsets (saved from the previous apriori iteration)
		// and try to combine itemsets with those that come after,
		// if they share the same prefix k-2 bits
		for (Itemset kmin1Itemset : prevL) {
			Itemset currPrefix = kmin1Itemset.chopLastBit();
			if (currPrefix.equals(groupPrefix)) {
				// Prefix matches, so add to the group to be combined
				groupCandidates.add(kmin1Itemset);
			} else {
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
							
							// XXX and check minsup and minconf??? (Probably yes)
							
						}
					}
				}
				groupPrefix = currPrefix;
				groupCandidates = new TreeSet<Itemset>();
			}
		}
		
		// Finally check if newCandidates is empty. If so then the previous set of
		// large itemsets is the final one.
		if (newCandidates.size() > 0) {
			Ck = newCandidates;
			return true;
		} else {
			// Ck == prevL
			return false;
		}
	}
	
	public void aprioriGenPrune(ItemsetTrie trie, Collection<ItemsetTrie> leaves, int depth) {
		for (ItemsetTrie leaf : leaves) {
			
		}
	}
	
}
