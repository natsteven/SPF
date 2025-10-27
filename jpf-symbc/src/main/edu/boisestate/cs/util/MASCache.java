package edu.boisestate.cs.util;

import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.graph.SolutionSet;
import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.string.StringPathCondition;

import java.util.HashMap;

/**
 * Cache of queries to see if we can do simple automaton operations for results
 */

public class MASCache {
	HashMap<StringPathCondition, SolutionSet<Model_Acyclic_Inverse>> cache = new HashMap<>();
	final int MAX_CACHE_SIZE = 1024;

	// starting simple, check whether the pc is equals except for one negated comparator
	public StringPathCondition findNeg(StringPathCondition pc) {
		if (cache.isEmpty()){
			return null;
		}
		for (StringPathCondition old : cache.keySet()) {
			int contradictions = 0;
			StringConstraint SC = pc.header;
			StringConstraint oldSC = old.header;
			if (oldSC.contradicts(SC)) {
				contradictions++;
			}
			// compare each SC in old and new SPC
			while (oldSC.and() != null) {
				oldSC = oldSC.and();
				SC = pc.header;
				while (SC.and() != null) {
					SC = SC.and();
					if (oldSC.contradicts(SC)) {
						contradictions++;
					}
				}
			}
			if (contradictions == 1) {
				return old;
			}
		}
		return null;
	}

	public SolutionSet<Model_Acyclic_Inverse> get(){
		return cache.get(cache.keySet().iterator().next());
	}

	public void put(StringPathCondition pc, SolutionSet<Model_Acyclic_Inverse> solSet){
		if (cache.size() >= MAX_CACHE_SIZE){
			// TODO: better eviction policy
			cache.remove(cache.keySet().iterator().next());
		}
		cache.put(pc, solSet);
	}

	public boolean isEmpty(){
		return cache.isEmpty();
	}
}
