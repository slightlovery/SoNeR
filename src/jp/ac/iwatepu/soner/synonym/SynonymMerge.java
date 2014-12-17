package jp.ac.iwatepu.soner.synonym;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jp.ac.iwatepu.soner.DBConnector;
import jp.ac.iwatepu.soner.Util;
import jp.ac.iwatepu.soner.classifier.SVM;
import jp.ac.iwatepu.soner.ranking.Person;

public class SynonymMerge {
	int[] mapping;
	Set<Integer> synonymSets [];
	boolean withAttributeMatching;
	boolean onlyWithAttributeMatching;
	
	public SynonymMerge(boolean withAttributeMatching, boolean onlyWithAttributeMatching) {
		this.withAttributeMatching = withAttributeMatching;
		this.onlyWithAttributeMatching = onlyWithAttributeMatching;
	}
		
	public Set<Integer> getAllSynonyms(int id) {
		return synonymSets[id];
	}

	public static void main(String[] args) throws Exception {
		SynonymMerge synMerge = new SynonymMerge(true, false);
		synMerge.run();
	}
	
	public int[] run() throws Exception {
		Util.getInstance().logIfDebug("Staritng SynonymMerge Main...");
		Util.getInstance().logIfDebug("Loading from DB...");
		int peopleSize = DBConnector.getInstance().getPeopleSize();
		int synonymPeople [] = DBConnector.getInstance().getAllSynonyms();			
		synonymSets = new Set [peopleSize];
		for (int i = 0; i < synonymSets.length; i++) {
			synonymSets[i] = new HashSet<Integer>();
			synonymSets[i].add(i);
		}
		Person[] allPeople = DBConnector.getInstance().getAllPeople();
		
		if (!onlyWithAttributeMatching) {
			Util.getInstance().logIfDebug("Starting the seeAlso merge...");
			int totalMergedBasedOnSeeAlso = 0;
			
			//merge people based on seeAlso
			Util.getInstance().logIfDebug("Total synonyms: " + synonymPeople.length);
			int part100 = synonymPeople.length / 100;
			for (int i = 0; i < synonymPeople.length; i += 2) {			
				if (part100 != 0 && i % part100 == 0) {
				//	System.out.println("Part " + i * 100.0 / synonymPeople.length + "%");
				}
				int firstPersonId = synonymPeople[i];
				int secondPersonId = synonymPeople[i + 1];
				Person p1 = allPeople[firstPersonId - 1];
				Person p2 = allPeople[secondPersonId - 1];
				if (p1.isValidURL() && p2.isValidURL()) {
				//	continue;
				}
				//merge
				synonymSets[secondPersonId].addAll(synonymSets[firstPersonId]);
				//change references
				totalMergedBasedOnSeeAlso++;
				for (int firstPersonSubsetId : synonymSets[firstPersonId]) {
					synonymSets[firstPersonSubsetId] = synonymSets[secondPersonId];
				}
			}
			
			Util.getInstance().logIfDebug("SeeAlso based merges: " + totalMergedBasedOnSeeAlso);
		}
		
		if (withAttributeMatching) {
			int totalMergedBasedOnAttributes = 0;
			SVM svm = new SVM();
			int attributeBasedMerges[] = svm.run();
			for (int i = 0; i < attributeBasedMerges.length; i += 2) {
				int firstPersonId = attributeBasedMerges[i];
				int secondPersonId = attributeBasedMerges[i + 1];
				synonymSets[secondPersonId].addAll(synonymSets[firstPersonId]);
				//change references
				totalMergedBasedOnAttributes++;
				for (int firstPersonSubsetId : synonymSets[firstPersonId]) {
					synonymSets[firstPersonSubsetId] = synonymSets[secondPersonId];
				}
			}
			Util.getInstance().logIfDebug("Attribute based merges: " + totalMergedBasedOnAttributes);
		}
		
		long totalSize = 0;
		for (int i = 0; i < synonymSets.length; i++) {
			totalSize += synonymSets[i].size();
		}
		
		Util.getInstance().logIfDebug(totalSize  + " " + synonymSets.length);
		Util.getInstance().logIfDebug("Average synonym set size is : " + (totalSize * 1.0 / synonymSets.length));
		
		mapping = new int[peopleSize];
		for (int i = 0; i < synonymSets.length; i++) {
			mapping[i] = Collections.min(synonymSets[i]);
		}
		return mapping;
	}

	/**
	 * modifies in place; creates mapping
	 * @param knownPeople
	 */
	public void applySynonymsToKnownRelationships(int [] knownPeople) throws Exception {
		int[] mapping = run();
		for (int i = 0; i < knownPeople.length; i += 2) {
			if (knownPeople[i] != mapping[knownPeople[i]] ||
				knownPeople[i + 1] != mapping[knownPeople[i + 1]]) {		    
				knownPeople[i] = mapping[knownPeople[i]];
				knownPeople[i + 1] = mapping[knownPeople[i + 1]];
			}
		}
	}
}
