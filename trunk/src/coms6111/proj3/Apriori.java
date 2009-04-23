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
	
	private double myMinsup, myMinconf;
	
	public Apriori(double minsup, double minconf) {
		myMinsup = minsup;
		myMinconf = minconf;
	}

	/**
	 * 
	 * @param large1Itemsets Collection of the large 1-itemsets
	 * @return Set of largest itemsets
	 */
	public HashSet<Itemset> doApriori(Collection<Itemset> large1Itemsets) {
		ArrayList<Set<Itemset>> L = new ArrayList<Set<Itemset>>(); // Large itemsets
		ArrayList<Set<Itemset>> C = new ArrayList<Set<Itemset>>(); // Candidate Large itemsets
				
		L.append(new HashSet<Itemset>()); // The 0-itemsets; an empty set
		L.append(large1Itemsets); // 1-itemsets; gotten from external
		
		C.append(new HashSet<Itemset>()); // Candidate 0-itemsets; empty set
		C.append(new HashSet<Itemset>()); // Candidate 1-itemsets; empty set
		
		for (int k = 2; L[k-1].length > 0; k++) {
			C[k] = aprioriGen(L[k-1]); // New candidates
			for (Transaction t : D) {
				C[t] = subset(C[k], t); // Candidates contained in t
				for (Candidate c : C[t]) {
					c.count++;
				}
			}
			L.append(new HashSet<Itemset>()); // Set of k-itemsets
			for (Itemset c : C[k]) {
				L.get(k).add(c);
			}
		}
		
	}
	
	public HashSet<Itemset> subset(Set<Itemset> ck, Transaction t) {
		
	}
	
}
