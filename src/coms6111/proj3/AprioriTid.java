package coms6111.proj3;

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
		
		
		
		for (int k = 2; L[k-1].length() > 0; k++) {
			C[k] = aprioriGen(L[k-1]);
			C_bar[k] = new TreeSet<Transaction>();
			for (Transaction t : C_bar[k-1] {
				// Determine candidate itemsets in C[k] contained
				// in the transaction with identifier t.TID
				C.put(t, new TreeSet<Transaction>());
				
				
			}
		}
	}
}
