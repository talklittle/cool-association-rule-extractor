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
	private SimpleTrie<Item> Ck;
	private HashSet<List<Item>> kMin2Prefixes; // Keep track of these for quick lookups during aprioriGen
	private int k; // The current iteration of the algorithm
	
	public Apriori(double minsup, double minconf) {
		myMinsup = minsup;
		myMinconf = minconf;
		Ck = new SimpleTrie<Item>(null);
		kMin2Prefixes = new HashSet<List<Item>>();
	}

	/**
	 * 
	 * @param large1Itemsets Collection of the large 1-itemsets
	 * @return Set of largest itemsets
	 */
	public HashSet<SortedSet<Item>> doApriori() {
		Set<SortedSet<Item>> large1Itemsets = getLarge1Itemsets();
		
		ArrayList<Set<SortedSet<Item>>> L = new ArrayList<Set<SortedSet<Item>>>(); // Large itemsets
		ArrayList<Set<SortedSet<Item>>> C = new ArrayList<Set<SortedSet<Item>>>(); // Candidate Large itemsets
				
		L.add(new HashSet<SortedSet<Item>>()); // The 0-itemsets; an empty set
		L.add(large1Itemsets); // 1-itemsets; gotten from external
		
		C.add(new HashSet<SortedSet<Item>>()); // Candidate 0-itemsets; empty set
		C.add(new HashSet<SortedSet<Item>>()); // Candidate 1-itemsets; empty set
		
		for (k = 2; L.get(k-1).size() > 0; k++) {
			//C[k] = aprioriGen(L[k-1]); // New candidates
			aprioriGen(L.get(k-1)); // Will update Ck
			for (Transaction t : D) {
				C[t] = subset(C[k], t); // Candidates contained in t
				for (Candidate c : C[t]) {
					c.count++;
				}
			}
			L.append(new HashSet<SortedSet<Item>>()); // Set of k-itemsets
			for (SortedSet<Item> c : C[k]) {
				L.get(k).add(c);
			}
		}
		
	}
	
	public HashSet<SortedSet<Item>> subset(Set<SortedSet<Item>> ck, Transaction t) {
		
	}
	
	public void aprioriGen(Collection<SortedSet<Item>> prevCandidates) {
		
		aprioriGenJoin(prevCandidates);
		aprioriGenPrune();
	}
	

	public void aprioriGenJoin(Collection<SortedSet<Item>> prevCandidates) {
		ArrayList<Item> tmpItemList;
		ArrayList<SortedSet<Item>> newCandidates = new ArrayList<SortedSet<Item>>(); 
		
		for (Iterator<List<Item>> it = kMin2Prefixes.iterator(); it.hasNext(); /* */ ) {
			List<Item> prevPrefix = it.next();
			SimpleTrie<Item> sharedNode = Ck.get(prevPrefix);
			if (sharedNode.children.size() > 1) {
				// This rocks! The itemsets will be combined.
				// Combine each child with the children that come after it
				Collection<SimpleTrie<Item>> leafNodes = sharedNode.children.values();
				for (Iterator<SimpleTrie<Item>> itj = leafNodes.iterator(); itj.hasNext(); /* */ ) {
					SimpleTrie<Item> leafj = itj.next();
					if (leafj == sharedNode.children.get(sharedNode.children.lastKey()))
						break;
					Collection<SimpleTrie<Item>> biggerChildren = sharedNode.children.tailMap(leafj.path).values();
					for (Iterator<SimpleTrie<Item>> itk = biggerChildren.iterator(); itk.hasNext(); /* */ ) {
						SimpleTrie<Item> leafk = itk.next();
						// XXX check here if the minsup???
						
						List<Item> newleaf = new ArrayList<Item>();
						newleaf.addAll(sharedNode.path);
						newleaf.add(leafj.value);
						newleaf.add(leafk.value);
						
						try {
							Ck.addToLeaf(newleaf);
						} catch (Exception e) {
							System.err.println("Skipped leaf");
						}
					}
				}
			}
		}
	}
	
	public void aprioriGenPrune() {
		
	}
	
}
