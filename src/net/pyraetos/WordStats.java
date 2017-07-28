package net.pyraetos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.atteo.evo.inflector.English;

import net.pyraetos.util.Sys;

public class WordStats{
	
	private static String totalText;
	
	private static Map<String, Integer> histogram = new HashMap<String, Integer>();
	private static List<HistoElement> sortedHistogram;
	
	private static BidiMap<String, String> nouns;
	
	//Utilized per document
	private static Map<String, Set<Integer>> locations;
	private static Map<String, Map<String, Integer>> minDistances = new HashMap<String, Map<String, Integer>>();
		//Map<Word, <All other words, current minimum distance>>
	
	
	private static void updateMinDistances(){
		for(String wordA : locations.keySet()){
			if(!minDistances.containsKey(wordA))
				minDistances.put(wordA, new HashMap<String, Integer>());
			
			for(String wordB : locations.keySet()){
				if(wordB.equals(wordA)) continue;
				Map<String, Integer> wordAMap = minDistances.get(wordA);
				if(!wordAMap.containsKey(wordB))
					wordAMap.put(wordB, Integer.MAX_VALUE);
				
				//Use this space to compare all instance locations of the wordA with all instance locations of wordB
				//SPEND SOME TIME THINKING ABOUT WAYS TO OPTIMIZE
				//ADD MODE DISTANCE WEIGHTED BY PAPER LENGTH?? # of occurrences should factor into strengthOfAssociation
				for(int absLocationA : locations.get(wordA)){
					for(int absLocationB : locations.get(wordB)){
						int d = Math.abs(absLocationA - absLocationB);
						if(d < wordAMap.get(wordB))
							wordAMap.put(wordB, d);
					}
				}
			}
		}
	}
	
	public static void main(String[] args){
		load_words();
		sortHistogram();
		print_histo();
	}
	
	//returns the singular version of the noun if its a noun
	//or null if it is not a noun
	private static String getNoun(String word){
		if(nouns.containsKey(word)) return word;
		if(nouns.containsValue(word)) return nouns.getKey(word);
		return null;
	}

	public static void load_words(){
		try{
			//First let's load all the nouns
			nouns = new DualHashBidiMap<String, String>();
			File nounfile = new File("nounlist.txt");
			if(!nounfile.exists()) throw new Exception();
			BufferedReader reader = new BufferedReader(new FileReader(nounfile));
			for(String noun = reader.readLine(); noun != null; noun = reader.readLine())
				nouns.put(noun, English.plural(noun));
			reader.close();
			
			//Second let's load words from our PDF corpus
			File in_dir = new File("input");
			File[] pdfs = in_dir.listFiles((f, s) -> {return s.toLowerCase().endsWith(".pdf");});
			PDFTextStripper pdfts = new PDFTextStripper();
			for(File pdf_file : pdfs){
				totalText = "";
				PDDocument pdf = PDDocument.load(pdf_file);
				totalText += pdfts.getText(pdf);
				pdf.close();
				updateHistoMap();
				updateMinDistances();
				
				for(String a : minDistances.keySet()){
					for(String b : minDistances.get(a).keySet()){
						Sys.debug("MinDist(" + a + ", " + b + ") = " + minDistances.get(a).get(b));
					}
				}
				System.exit(0);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void updateHistoMap(){
		int location = 0;
		locations = new HashMap<String, Set<Integer>>();
		for(int b = 0, i = 0; i < totalText.length(); i++){
			char ic = totalText.charAt(i);
			if(ic == ' ' || ic == '\t' || ic == '\n'){
				String word = totalText.substring(b, i).toLowerCase();
				String noun = getNoun(word);
				if(noun != null){
					word = noun;
					//Place location into locations map
					if(!locations.containsKey(word)) locations.put(word, new HashSet<Integer>());
					locations.get(word).add(location);
				}
				
				//Place into histo map
				if(!histogram.containsKey(word)) histogram.put(word, 0);
				histogram.put(word, histogram.get(word)+1);
				
				i++; b = i;
				location++;
			}else
			if(ic == '.' || ic == ','){
				if(totalText.charAt(i+1) == ' '){
					String word = totalText.substring(b, i).toLowerCase();
					String noun = getNoun(word);
					if(noun != null){
						word = noun;
						//Place location into locations map
						if(!locations.containsKey(word)) locations.put(word, new HashSet<Integer>());
						locations.get(word).add(location);
					}
					
					//Place into histo map
					if(!histogram.containsKey(word)) histogram.put(word, 0);
					histogram.put(word, histogram.get(word)+1);
					
					
					i++;
				}
				i++; b = i;
				location++;
			}
		}
		
		//Update word-location associations for this file
	}
	
	public static void sortHistogram(){
		sortedHistogram = new ArrayList<HistoElement>();
		for(String word : histogram.keySet()){
			if(word.length() < 2) continue;
			sortedHistogram.add(new HistoElement(word, histogram.get(word)));
		}
		sortedHistogram.sort((e1, e2) -> {
			if(e1.i > e2.i) return 1;
			if(e1.i == e2.i) return 0;
			return -1;
		});
	}
	
	static class HistoElement{
		private String word;
		private int i;
		public HistoElement(String word, int i){
			this.word = word;
			this.i = i;
		}
	}
	
	public static void print_histo(){
		for(HistoElement he : sortedHistogram){
			Sys.debug(he.word + ": " + he.i);
		}
	}
}
