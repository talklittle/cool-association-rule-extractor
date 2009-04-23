package coms6111.proj3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 1) L1 = {large l-itemsets};
 * 2) C1_bar = database D;
 * 3) for ( k = 2; L[k-l] != empty_set; k++ ) do begin
 * 4)   Ck = apriori-gen(L[k-1]); // New candidates
 * 5)   Ck_bar = empty_set;
 * 6)   forall entries t in Ck-1_bar do begin
 * 7)     // determine candidate itemsets in Ck contained
 *        // in the transaction with identifier t.TID
 *        Ct = {c in Ck | (c - c[k]) in t.set-of-itemsets INTERSECT
 *           (c - c[k - 1]) in t.set-of-itemsets};
 * 8)     forall candidates c in C[t] do
 * 9)       c.count++;
 * 10)    if (C[t] != 0) then Ck_bar += < t.TID, Ct >;
 * 11)  end
 * 12)  Lk = {c E ck 1 c.count 2 minsup}
 * 13) end
 * 14) Answer = Union[k] L[k];
 * 
 */
public class AprioriTid {
	
	
	public Answer doAprioriTid() {
		
		ArrayList<Set<Itemset>> L = new ArrayList<Set<Itemset>>(); // Large itemsets
		ArrayList<Set<Itemset>> C = new ArrayList<Set<Itemset>>(); // Candidate Large itemsets
				
		L.append(new HashSet<Itemset>()); // The 0-itemsets; an empty set
		L.append(large1Itemsets); // 1-itemsets; gotten from external
		
		C.append(new HashSet<Itemset>()); // Candidate 0-itemsets; empty set
		C.append(new HashSet<Itemset>()); // Candidate 1-itemsets; empty set
		
		for (int k = 2; L[k-1].length > 0; k++) {
			C[k] = aprioriGen(L[k-1]); // New candidates
			C_bar[k] = new TreeSet<Transaction>();
			for (Transaction t : C_bar[k-1]) {
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
		
		for (int k = 2; L[k-1].length() > 0; k++) {
			C[k] = aprioriGen(L[k-1]);
			for (Transaction t : C_bar[k-1] {
				// Determine candidate itemsets in C[k] contained
				// in the transaction with identifier t.TID
				C.put(t, new TreeSet<Transaction>());
				
				
			}
		}
	}
}
