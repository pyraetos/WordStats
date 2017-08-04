package net.pyraetos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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
	
	static class SOA implements Comparable{

		float soa;
		String word;
		
		@Override
		public int compareTo(Object o){
			if(!(o instanceof SOA)) return 0;
			SOA other = (SOA)o;
			return soa < other.soa ? 1 : -1;
		}
		
	}
	
	//Utilized per document
	private static Map<String, Set<Integer>> locations;
	private static Map<String, Map<String, Float>> strengths = new HashMap<String, Map<String, Float>>();
	private static Map<String, PriorityQueue<SOA>> strengthHeap = new HashMap<String, PriorityQueue<SOA>>();
		//Map<Word, <All other words, current minimum distance>>
	
	private static int maxROA = 0;
	private static List<Integer> allRankings = new ArrayList<Integer>();
	
	private static void updateSOAs(){
		for(String wordA : locations.keySet()){
			if(!strengths.containsKey(wordA))
				strengths.put(wordA, new HashMap<String, Float>());
			for(String wordB : locations.keySet()){
				if(wordB.equals(wordA)) continue;
				Map<String, Float> wordAMap = strengths.get(wordA);
				if(!wordAMap.containsKey(wordB))
					wordAMap.put(wordB, 0f);
				
				//Use this space to compare all instance locations of the wordA with all instance locations of wordB
				int ranking = 0;
				for(int a_i : locations.get(wordA)){
					int ranking_i = Integer.MAX_VALUE;
					for(int b_j : locations.get(wordB)){
						int d_ij = Math.abs(a_i - b_j);
						if(d_ij < ranking_i) ranking_i = d_ij;
					}
					ranking += ranking_i; //Add mindist to cumulative mindist
				}
				ranking /= locations.get(wordA).size(); //Arithmetic mean of mindists to get ranking of association
				allRankings.add(ranking);
				
				//Emplace temporary roa
				if(ranking > maxROA) maxROA = ranking;
				wordAMap.put(wordB, (float)ranking);
			}
		}
		
		//Convert all RoAs to SoA
		float max = (float)maxROA;
		for(String a : strengths.keySet()){
			strengthHeap.put(a, new PriorityQueue<SOA>());
			for(String b : strengths.get(a).keySet()){
				float roa = strengths.get(a).get(b);
				float strength = (float)Math.pow(4f, -((roa/max) * 6f));
				strengths.get(a).put(b, strength);
				SOA soa = new SOA();
				soa.word = b;
				soa.soa = strength;
				strengthHeap.get(a).offer(soa);
			}
		}
		
		
		//Graph the RoAs
		/*int iii[] = new int[allRankings.size()];
		long bigoleranking=0;
		for(int i =0; i < allRankings.size(); i++) {
			iii[i] = allRankings.get(i);
			bigoleranking += (long)allRankings.get(i);
		}
		Sys.histogram(100, 30, iii);
		Sys.debug(bigoleranking / ((long)allRankings.size()));
		System.exit(1);*/
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
				updateSOAs();
				
				//for(String a : strengths.keySet()){
				String a = "bird";
				Sys.debug(strengths.get("bird").get("twig"));
				while(!strengthHeap.get(a).isEmpty()){
					SOA soa = strengthHeap.get(a).poll();
					Sys.debug(a +" -> " + soa.word + ": " + soa.soa);
					Sys.sleep(3000);
				}
				//}
				System.exit(0);
			}
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static void wordGame(String word){
		Set<String> used = new HashSet<String>();
		for(int i = 0; i < 50; i++){
			Sys.debug(word);
			PriorityQueue<SOA> heap = strengthHeap.get(word);
			used.add(word);
			while(used.contains(word))
				word = heap.poll().word;
			Sys.sleep(1000);
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
