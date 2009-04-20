package coms6111.proj3;

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

	
	public static Answer doApriori() {
		L[1] = getLarge1Itemsets();
		
		for (int k = 2; L[k-1].length > 0; k++) {
			C[k] = aprioriGen(L[k-1]); // New candidates
			for (Transaction t : D) {
				C[t] = subset(C[k], t); // Candidates contained in t
				for (Candidate c : C[t]) {
					c.count++;
				}
			}
			
		}
	}
}
