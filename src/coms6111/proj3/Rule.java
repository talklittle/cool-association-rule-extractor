package coms6111.proj3;

public class Rule implements Comparable<Rule> {

	public double confidence;
	public String rule;
	
	public Rule(double nConfidence, String nRule) {
		confidence = nConfidence;
		rule = nRule;
	}
	
	public int compareTo(Rule o) {
		return new Double(confidence).compareTo(new Double(o.confidence));
	}
	
	public String toString() {
		return rule;
	}
}
