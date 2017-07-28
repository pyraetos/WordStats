package net.pyraetos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import org.atteo.evo.inflector.English;

import net.pyraetos.util.Sys;

public class Pyrauthor{

	public static final double KLEENE_CHANCE = 0.25;
	public static final double SP_CHANCE = 0.95;
	public static final int CHARS_TILL_NEWLINE = 70;
	public static final double SOFTMAX_BASE = Math.PI;//Higher values give more weight to LRU words. 1.0 is uniform.
	public static final boolean DEBUG_MODE = false;
	
	private static class Word{
		
		private String[] forms;
		private int used;
		
		Word(String...forms){
			this.forms = new String[4];
			for(int i = 0; i < forms.length; i++)
				this.forms[i] = forms[i];
			used = 0;
		}
		
		//For all others
		public String get(){
			return forms[0];
		}
		
		//For nouns and articles
		public String singular(){
			return forms[0];
		}
		
		public String plural(){
			return forms[1] == null ? English.plural(forms[0]) : forms[1];
		}
		
		//For verbs
		public String presentIndicativeSingular(){
			return forms[0];
		}
		
		public String presentIndicativePlural(){
			return forms[1];
		}
		
		public String presentSubjunctive(){
			return forms[2];
		}
		
		public String past(){
			return forms[3];
		}
	}
	
	public static Word[] NOUNS;/* = {
		new Word("cadet"),
		new Word("shit"),
		new Word("food"),
		new Word("computer"),
		new Word("cigarette"),
		new Word("SGT Jung", "SGT Jung")
	};*/
	
	public static final Word[] ARTICLES = {
			new Word("the", "the"),
			new Word("a", "some"),
		};
	
	public static final Word[] TRANS_VERBS = {
		new Word("eats", "eat", "eat", "ate"),
		new Word("wipes", "wipe", "wipe", "wiped"),
		new Word("cleans", "clean", "clean", "cleaned"),
		new Word("thinks about", "think about", "think about", "thought about"),
		new Word("jacks off", "jack off", "jack off", "jacked off")
	};
	
	public static final Word[] INTRANS_VERBS = {
		new Word("lives", "live", "live", "lived"),
		new Word("suffers", "suffer", "suffer", "suffered"),
		new Word("poops", "poop", "poop", "pooped"),
		new Word("pisses", "piss", "piss", "took a piss"),
		new Word("plays Quiplash", "play Quiplash", "play Quiplash", "played Quiplash"),
		new Word("masturbates", "masturbate", "masturbate", "masturbated")
	};
	
	public static final Word[] ADJECTIVES = {
			new Word("gay"),
			new Word("rotten"),
			new Word("massive"),
			new Word("dirty"),
			new Word("ugly")
	};
		
	public static final Word[] ADVERBS = {
			new Word("furiously"),
			new Word("unintentionally"),
			new Word("for a long time")
	};
	
	public static final Word[] CONNECTORS = {
			new Word("however"),
			new Word("unfortunatlely"),
			new Word("despite this fact")
	};
	
	public static final Word[] CONJUNCTIONS = {
			new Word("and"),
			new Word("and also")
	};
	
	public static final Word[] SUBJUNCTIVES = {
			new Word("in order to"),
	};
	
	//For ease of the non-conjugable wordbanks
	public static final Map<Type, Word[]> wordBanks = new HashMap<Type, Word[]>();
	
	private static void populateBanks(){
		//Load nouns
		try{
			File file = new File("nounlist.txt");
			if(!file.exists()) throw new Exception();
			BufferedReader reader = new BufferedReader(new FileReader(file));
			Set<Word> set = new HashSet<Word>();
			for(String noun = reader.readLine(); noun != null; noun = reader.readLine())
				set.add(new Word(noun));
			reader.close();
			NOUNS = set.toArray(new Word[set.size()]);
		}catch(Exception e){
			System.exit(1);
		}
		wordBanks.put(Type.ADJECTIVE, ADJECTIVES);
		wordBanks.put(Type.ADVERB, ADVERBS);
		wordBanks.put(Type.CONNECTOR, CONNECTORS);
		wordBanks.put(Type.CONJUNCTION, CONJUNCTIONS);
		wordBanks.put(Type.SUBJUNCTIVE, SUBJUNCTIVES);
	}
	
	private static Word softmaxDebugMode(Word[] words){
		double softmax[] = new double[words.length];
		double softmaximmutable[] = new double[words.length];
		double sum = 0;
		for(int i = 0; i < words.length; i++){
			softmax[i] = Math.pow(SOFTMAX_BASE, -words[i].used);
			softmaximmutable[i] = Math.pow(SOFTMAX_BASE, -words[i].used);
			sum += softmax[i];
		}
		for(int i = 0; i < words.length; i++){
			softmax[i] = softmax[i] / sum;
			softmaximmutable[i] = softmaximmutable[i] / sum;
		}
		double x = random.nextDouble();
		for(int i = 0; i < softmax.length; i++){
			if(x < softmax[i]){
				Sys.debug("\"" + words[i].get() + "\" chosen from chance " + softmaximmutable[i]);
				Sys.sleep(1000);
				words[i].used++;
				return words[i];
			}
			softmax[i+1] += softmax[i];
		}
		return null;
	}
	
	private static Word softmax(Word[] words){
		if(DEBUG_MODE)
			return softmaxDebugMode(words);
		double softmax[] = new double[words.length];
		double sum = 0;
		for(int i = 0; i < words.length; i++){
			softmax[i] = Math.pow(SOFTMAX_BASE, -words[i].used);
			sum += softmax[i];
		}
		for(int i = 0; i < words.length; i++){
			softmax[i] = softmax[i] / sum;
		}
		double x = random.nextDouble();
		for(int i = 0; i < softmax.length; i++){
			if(x < softmax[i]){
				words[i].used++;
				return words[i];
			}
			softmax[i+1] += softmax[i];
		}
		return null;
	}
	
	private static String chooseWord(Type type, int option){
		switch(type){
		case NOUN:{
			Word w = softmax(NOUNS);
			return option == SINGULAR ? w.singular() : w.plural();
		}
		case TRANS_VERB:{
			Word w = softmax(TRANS_VERBS);
			return option == SINGULAR ? w.presentIndicativeSingular() : w.presentIndicativePlural();
		}
		case INTRANS_VERB:{
			Word w = softmax(INTRANS_VERBS);
			return option == SINGULAR ? w.presentIndicativeSingular() : w.presentIndicativePlural();
		}
		case ARTICLE:{
			Word w = softmax(ARTICLES);
			return option == SINGULAR ? w.presentIndicativeSingular() : w.presentIndicativePlural();
		}
		default:{
			return softmax(wordBanks.get(type)).get();
		}
		}
	}
	
	private enum Type{
		TERMINAL,
		PAPER,
		PARAGRAPH,
		SENTENCE,
		CLAUSE,
		CONNECTOR,
		CONJUNCTION,
		SUBJUNCTIVE,
		NOUN_PHRASE,
		TRANS_VERB_PHRASE,
		INTRANS_VERB_PHRASE,
		ARTICLE,
		NOUN,
		ADJECTIVE,
		TRANS_VERB,
		ADVERB,
		INTRANS_VERB,
	}
	
	/* Component Options - 4 bytes
	 * 
	 * 00 - Tense
	 * 00 - Mood
	 * 00 - Number
	 * 00 - Unused
	 */
	public static int PRESENT = 0x00000000;
	public static int PAST = 0x00000001;
	
	public static int INDICATIVE = 0x00000000;
	public static int SUBJUNCTIVE = 0x00000100;
	
	public static int SINGULAR = 0x00000000;
	public static int PLURAL = 0x00010000;
	
	private static class Component{
		
		private Type type;
		private String data;
		private Component[] children;
		private Component parent;
		private int option;
		
		Component(Type type, Component parent){
			this.type = type;
			children = null;
			data = null;
			this.option = 0;
			this.parent = parent;
		}
		
		Component(String data, Component parent){
			this.type = Type.TERMINAL;
			children = null;
			this.data = data;
			this.option = 0;
			this.parent = parent;
		}
		
		public int getTense(){
			return 0x00000011 & option;
		}
		
		public int getMood(){
			return 0x00001100 & option;
		}
		
		public int getNumber(){
			return 0x00110000 & option;
		}
	}
	
	private static Random random = new Random();
	
	public static void main(String[] args){
		populateBanks();
		Component paper = new Component(Type.PAPER, null);
		parse(paper);
		print(paper);
		save(paper);
	}
	
	public static void print(Component c){
		charCounter = CHARS_TILL_NEWLINE;
		print(c, System.out);
	}
	
	private static int charCounter;
	private static boolean newSentence = true;
	
	private static String capitalize(String s){
		return s.substring(0,1).toUpperCase() + s.substring(1,s.length());
	}
	
	private static void print(Component c, PrintStream p){
		if(c.type != Type.TERMINAL)
			for(Component ch : c.children)
				print(ch, p);
		else{
			if(c.parent.type == Type.SENTENCE || c.parent.type == Type.PAPER){
				p.print(c.data);
				charCounter += c.data.length();
				newSentence = true;
				return;
			}
			
			
			String space = " ";
			if(charCounter > CHARS_TILL_NEWLINE){
				p.println();
				space = "";
				charCounter = 0;
			}
			
		//	Sys.debug("\'" + c.data + "\' " + charCounter + " \'" + space + "\'"); Sys.sleep(1000);
			if(c.parent.type == Type.CONNECTOR){
				if(newSentence){
					p.printf(space + capitalize(c.data) + ",");
					newSentence = false;
				}else
					p.printf(space + c.data + ",");
				charCounter += (2 + c.data.length());
			}
			else{
				if(newSentence){
					p.print(space + capitalize(c.data));
					newSentence = false;
				}else
					p.print(space + c.data);
				
				charCounter += (1 + c.data.length());
			}
		}
	}
	
	public static void save(Component c){
		File file = new File("component.txt");
		if(file.exists()) file.delete();
		try{
			file.createNewFile();
			PrintStream p = new PrintStream(new FileOutputStream(file));
			charCounter = CHARS_TILL_NEWLINE;
			print(c, p);
			p.flush();
			p.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public static void parse(Component c){
		//Sys.debug(c.type.name());
		//new Scanner(System.in).nextLine();
		switch(c.type){
		case TERMINAL: return;
		case PAPER: parsePaper(c); break;
		case PARAGRAPH: parseParagraph(c); break;
		case SENTENCE: parseSentence(c); break;
		case CLAUSE: parseClause(c); break;
		case NOUN_PHRASE: parseNounPhrase(c); break;
		case TRANS_VERB_PHRASE: parseTransVerbPhrase(c); break;
		case INTRANS_VERB_PHRASE: parseIntransVerbPhrase(c); break;
		case CONNECTOR: parseConnector(c); break;
		case CONJUNCTION: parseConjunction(c); break;
		case SUBJUNCTIVE: parseSubjunctive(c); break;
		case ARTICLE: parseArticle(c); break;
		case NOUN: parseNoun(c); break;
		case ADJECTIVE: parseAdjective(c); break;
		case TRANS_VERB: parseTransVerb(c); break;
		case INTRANS_VERB: parseIntransVerb(c); break;
		case ADVERB: parseAdverb(c); break;
		}
		for(Component ch : c.children) parse(ch);
	}
	
	public static void parsePaper(Component c){
		c.children = new Component[3];
		c.children[0] = new Component("Once upon a time,\n", c);
		c.children[1] = new Component(Type.PARAGRAPH, c);
		c.children[2] = new Component("\n\nThe end.\n", c);
	}
	
	public static void parseParagraph(Component c){
		//SP chance SentenceParagraph
		if(Sys.chance(SP_CHANCE)){
			c.children = new Component[2];
			c.children[0] = new Component(Type.SENTENCE, c);
			c.children[1] = new Component(Type.PARAGRAPH, c);
		}
		//chance Sentence
		else{
			c.children = new Component[1];
			c.children[0] = new Component(Type.SENTENCE, c);
		}
	}

	public static void parseSentence(Component c){
		double rand = random.nextDouble();
		//25% chance ConnectorClause
		if(rand < .25){
			c.children = new Component[3];
			c.children[0] = new Component(Type.CONNECTOR, c);
			c.children[1] = new Component(Type.CLAUSE, c);
			c.children[2] = new Component(".", c);
			return;
		}
		//25% chance ClauseConjClause
		if(rand < .25){
			c.children = new Component[4];
			c.children[0] = new Component(Type.CLAUSE, c);
			c.children[1] = new Component(Type.CONJUNCTION, c);
			c.children[2] = new Component(Type.CLAUSE, c);
			c.children[3] = new Component(".", c);
			return;
		}
		//50% chance Clause
		else{
			c.children = new Component[2];
			c.children[0] = new Component(Type.CLAUSE, c);
			c.children[1] = new Component(".", c);
		}
	}
	
	public static void parseClause(Component c){
		double rand = random.nextDouble();
		//50% chance NounphraseTransverbphraseNounphrase
		if(rand < 0.5){
			c.children = new Component[3];
			c.children[0] = new Component(Type.NOUN_PHRASE, c);
			c.children[1] = new Component(Type.TRANS_VERB_PHRASE, c);
			c.children[2] = new Component(Type.NOUN_PHRASE, c);
			return;
		}
		//50% chance NounphraseIntransverbphrase
		else{
			c.children = new Component[2];
			c.children[0] = new Component(Type.NOUN_PHRASE, c);
			c.children[1] = new Component(Type.INTRANS_VERB_PHRASE, c);
		}
	}
	
	public static void parseNounPhrase(Component c){
		double rand = random.nextDouble();
		if(rand < 0.25){
			c.children = new Component[3];
			c.children[0] = new Component(Type.NOUN_PHRASE, c);
			c.children[1] = new Component(Type.CONJUNCTION, c);
			c.children[2] = new Component(Type.NOUN_PHRASE, c);
			c.option |= PLURAL;
			return;
		}
		if(rand < .50){
			c.children = new Component[2];
			c.children[0] = new Component(Type.ARTICLE, c);
			c.children[1] = new Component(Type.NOUN, c);
			c.option |= Sys.chance(0.3333) ? PLURAL : SINGULAR;
			return;
		}
		else{
			int kleene = 0;
			for(; Sys.chance(KLEENE_CHANCE); kleene++);
			c.children = new Component[3 + 2 * kleene];
			c.children[0] = new Component(Type.ARTICLE, c);
			c.children[1] = new Component(Type.ADJECTIVE, c);
			for(int i = 0; i < kleene; i++){
				c.children[2 + i * 2] = new Component(Type.CONJUNCTION, c);
				c.children[3 + i * 2] = new Component(Type.ADJECTIVE, c);
			}
			c.children[2 + 2 * kleene] = new Component(Type.NOUN, c);
			c.option |= Sys.chance(0.3333) ? PLURAL : SINGULAR;
			return;
		}
	}
	
	public static void parseTransVerbPhrase(Component c){
		double rand = random.nextDouble();
		if(rand < 0.25){
			c.children = new Component[3];
			c.children[0] = new Component(Type.TRANS_VERB_PHRASE, c);
			c.children[1] = new Component(Type.CONJUNCTION, c);
			c.children[2] = new Component(Type.TRANS_VERB_PHRASE, c);
			return;
		}
		if(rand < .50){
			c.children = new Component[1];
			c.children[0] = new Component(Type.TRANS_VERB, c);
			return;
		}
		else{
			int kleene = 0;
			for(; Sys.chance(KLEENE_CHANCE); kleene++);
			c.children = new Component[2 + 2 * kleene];
			c.children[0] = new Component(Type.ADVERB, c);
			for(int i = 0; i < kleene; i++){
				c.children[1 + i * 2] = new Component(Type.CONJUNCTION, c);
				c.children[2 + i * 2] = new Component(Type.ADVERB, c);
			}
			c.children[1 + 2 * kleene] = new Component(Type.TRANS_VERB, c);
			return;
		}
	}
	
	public static void parseIntransVerbPhrase(Component c){
		double rand = random.nextDouble();
		if(rand < 0.25){
			c.children = new Component[3];
			c.children[0] = new Component(Type.INTRANS_VERB_PHRASE, c);
			c.children[1] = new Component(Type.CONJUNCTION, c);
			c.children[2] = new Component(Type.INTRANS_VERB_PHRASE, c);
			return;
		}
		if(rand < .50){
			c.children = new Component[1];
			c.children[0] = new Component(Type.INTRANS_VERB, c);
			return;
		}
		else{
			int kleene = 0;
			for(; Sys.chance(KLEENE_CHANCE); kleene++);
			c.children = new Component[2 + 2 * kleene];
			c.children[0] = new Component(Type.ADVERB, c);
			for(int i = 0; i < kleene; i++){
				c.children[1 + i * 2] = new Component(Type.CONJUNCTION, c);
				c.children[2 + i * 2] = new Component(Type.ADVERB, c);
			}
			c.children[1 + 2 * kleene] = new Component(Type.INTRANS_VERB, c);
			return;
		}
	}

	public static void parseConnector(Component c){
		c.children = new Component[1];
		c.children[0] = new Component(chooseWord(Type.CONNECTOR, 0), c);
	}
	
	public static void parseConjunction(Component c){
		c.children = new Component[1];
		c.children[0] = new Component(chooseWord(Type.CONJUNCTION, 0), c);
	}
	
	public static void parseSubjunctive(Component c){
		c.children = new Component[1];
		c.children[0] = new Component(chooseWord(Type.SUBJUNCTIVE, 0), c);
	}
	
	public static void parseAdjective(Component c){
		c.children = new Component[1];
		c.children[0] = new Component(chooseWord(Type.ADJECTIVE, 0), c);
	}
	
	public static void parseAdverb(Component c){
		c.children = new Component[1];
		c.children[0] = new Component(chooseWord(Type.ADVERB, 0), c);
	}
	
	public static void parseArticle(Component c){
		Component cp = c.parent;
		for(; cp.type != Type.NOUN_PHRASE; cp = cp.parent);
		c.children = new Component[1];
		c.children[0] = new Component(chooseWord(Type.ARTICLE, cp.option), c);
	}
	
	public static void parseNoun(Component c){
		Component cp = c.parent;
		for(; cp.type != Type.NOUN_PHRASE; cp = cp.parent);
		c.children = new Component[1];
		c.children[0] = new Component(chooseWord(Type.NOUN, cp.option), c);
	}
	
	public static void parseTransVerb(Component c){
		Component cp = c.parent;
		for(; cp.children[0].type != Type.NOUN_PHRASE; cp = cp.parent);
		c.children = new Component[1];
		c.children[0] = new Component(chooseWord(Type.TRANS_VERB, cp.children[0].option), c);
	}
	
	public static void parseIntransVerb(Component c){
		Component cp = c.parent;
		for(; cp.children[0].type != Type.NOUN_PHRASE; cp = cp.parent);
		c.children = new Component[1];
		c.children[0] = new Component(chooseWord(Type.INTRANS_VERB, cp.children[0].option), c);
	}
	
	public static void testSoftmax(){
		double softmax[] = {0,1,2,3,4,5,6,7,8,9};
		double base = Math.E;
		double sum = 0;
		for(int i = 0; i < softmax.length; i++){
			softmax[i] = Math.pow(base, -softmax[i]);
			sum += softmax[i];
		}
		double shouldbe1 = 0;
		for(int i = 0; i < softmax.length; i++){
			softmax[i] = softmax[i] / sum;
			System.out.print(Sys.round(softmax[i]));
			for(double d = 0; d < softmax[i]; d += 0.03)
				System.out.print("-");
			System.out.println();
			shouldbe1 += softmax[i];
		}
		System.out.println("\n" + shouldbe1);
		System.exit(0);
	}
}
