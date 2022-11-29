package bar.barinade.barry.brain.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bar.barinade.barry.brain.data.Association;

@Service
public class Cerebellum {
	
	private static final Logger m_logger = LoggerFactory.getLogger(Cerebellum.class);
	
	// automatically deny any sentence that matches this
	private static final Pattern PATTERN_EMAILS = Pattern.compile("[\\w\\d]+@[\\w\\d]+");
	private static final Pattern PATTERN_PROTOCOLS = Pattern.compile("([/])\\1{1,}");
	private static final Pattern PATTERN_REPEATS = Pattern.compile("(.)\\1{2,}");
	
	// if the input is one character, it must fit this
	// (only a letter or a number)
	private static final Pattern PATTERN_ONE_CHAR_PASS = Pattern.compile("[\\w\\d]");
	
	// this matches all text which should be deleted if it isnt the last letter
	// (don't -> dont)
	private static final Pattern PATTERN_DELETE_IF_MATCH = Pattern.compile("['\"]");
	
	// this matches all text which should be replaced with space if it isnt the last letter
	// (this!thing -> this thing)
	private static final Pattern PATTERN_SPACE_IF_MATCH = Pattern.compile("([^\\d\\w\\s])(?!$)");
	
	// matches a string which ends with a set punctuation
	private static final Pattern PATTERN_ENDS_WITH_PUNCTUATION = Pattern.compile(".+[!.?]$");
	
	// matches a string which is entirely capital letters or camelCase (allow acronyms and emote names)
	private static final Pattern PATTERN_ALL_CAPS_OR_CAMELCASE = Pattern.compile("^[A-Z]*$|(^[a-z]+[A-Z]+\\w+)+");
	
	// for each new word, the sentence could suddenly end by this out of 100
	private static final int PERCENT_TO_END_PER_WORD = 5;
	
	// up to this many words or letters may be present in an output sentence
	private static final int MAX_OUTPUT_WORDS = 45;
	private static final int MAX_OUTPUT_CHARS = 1995;
	
	@Autowired
	private AssociationService neurons;
	
	public void commitAssociation(Long brainId, String left, String right, String value) {
		neurons.save(brainId, left, right, value);
	}
	
	public void flushNeurons() {
		neurons.flush();
	}
	
	public void parseAndCommitSentence(Long brainId, String sentence, boolean deferFlush) {
		if (sentence == null || sentence.isBlank()) {
			return;
		}
		
		// deny if any of these matchers succeeds
		Matcher matchEmail = PATTERN_EMAILS.matcher(sentence);
		Matcher matchProtocol = PATTERN_PROTOCOLS.matcher(sentence);
		Matcher matchRepeats = PATTERN_REPEATS.matcher(sentence);
		if (matchEmail.find() || matchProtocol.find() || matchRepeats.find()) {
			m_logger.trace("Brain ID {} | Forbidden pattern found in phrase - not committing", brainId);
			return;
		}
		
		// remove all garbage
		sentence = PATTERN_DELETE_IF_MATCH.matcher(sentence).replaceAll("");
		sentence = PATTERN_SPACE_IF_MATCH.matcher(sentence).replaceAll(" ");
		
		String[] words = sentence.strip().split("\\s+");
		if (words == null || words.length < 3) {
			m_logger.trace("Brain ID {} | Post Processing phrase too short to commit", brainId);
			return;
		}
		
		// validate the remaining string
		for (int i = 0; i < words.length; i++) {
			// this shouldnt happen but what if it does?
			if (words[i] == null) {
				m_logger.trace("Brain ID {} | Word in post processed phrase found to be null", brainId);
				return;
			}
			
			// this shouldnt happen but what if it does?
			words[i] = words[i].strip();
			if (words[i].isEmpty()) {
				m_logger.trace("Brain ID {} | Word in post processed phrase found to be empty", brainId);
				return;
			}
			
			if (words[i].length() == 1) {
				Matcher matchOneChar = PATTERN_ONE_CHAR_PASS.matcher(words[i]);
				if (!matchOneChar.find()) {
					m_logger.trace("Brain ID {} | Single character did not pass single character check: '{}'", words[i]);
					return;
				}
			}
			
			// result of this basically just turns "This" into lower case but does nothing else
			Matcher matchAllCaps = PATTERN_ALL_CAPS_OR_CAMELCASE.matcher(words[i]);
			if (!matchAllCaps.find()) {
				words[i] = words[i].toLowerCase();
			}
		}
		
		// by this point the sentence is valid (but words may have been removed due to being entirely symbols or something ...)
		// commit it
		for (int i = 0; i < words.length - 2; i++) {
			final String left = words[i];
			final String right = words[i+1];
			final String value = words[i+2];
			commitAssociation(brainId, left, right, value);
		}

		if (!deferFlush) {
			neurons.flush();
		}
		m_logger.debug("Brain ID {} | Parsed and committed: {}", brainId, String.join(" ", words));
	}
	
	public void loadFormattedBrainFromPath(String path, Long brainId) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String line;
			while ((line = br.readLine()) != null) {
				String[] halves = line.split("=");
				if (halves.length == 2) {
					String key = halves[0].strip();
					String valList = halves[1];
					
					String[] keyHalves = key.split(" ");
					if (keyHalves.length == 2) {
						String left = keyHalves[0];
						String right = keyHalves[1];
						
						String[] vals = valList.split(" ");
						for (String v : vals) {
							commitAssociation(brainId, left.strip(), right.strip(), v.strip());
						}
					} else {
						m_logger.warn("Keyhalves was length {} | LINE: {}", keyHalves.length, line);
					}
				} else {
					m_logger.warn("Halves was length {} | LINE: {}", halves.length, line);
				}
			}
			br.close();
			neurons.flush();
			m_logger.info("Finished brain loading.");
		} catch (Exception e) {
			m_logger.error("Brain load failed.", e);
		}
	}
	
	public String formSentence(Long brainId, String weight) {
		m_logger.info("Forming sentence for brain {} weight '{}'", brainId, weight);
		
		String left = "";
		String right = "";
		
		if (weight == null || weight.isEmpty()) {
			Association random = neurons.random(brainId);
			if (random != null) {
				left = random.getId().getLeft();
				right = random.getId().getRight();
			}
		} else {
			weight = weight.strip();
			String[] pieces = weight.split(" ");
			
			if (pieces.length == 0) {
				Association random = neurons.random(brainId);
				if (random != null) {
					left = random.getId().getLeft();
					right = random.getId().getRight();
				}
			} else if (pieces.length == 1) {
				List<Association> candidates = neurons.getCandidates(brainId, pieces[0]);
				if (candidates == null || candidates.isEmpty()) {
					Association random = neurons.random(brainId);
					if (random != null) {
						left = random.getId().getLeft();
						right = random.getId().getRight();
					}
				} else {
					int i = ThreadLocalRandom.current().nextInt(candidates.size());
					left = candidates.get(i).getId().getLeft();
					right = candidates.get(i).getId().getRight();
				}
			} else if (pieces.length >= 2) {
				List<Association> candidates = neurons.getCandidates(brainId, pieces[0], pieces[1]);
				if (candidates == null || candidates.isEmpty()) {
					for (int i = 1; i < pieces.length && (candidates == null || candidates.isEmpty()); i++) {
						candidates = neurons.getCandidates(brainId, pieces[i]);
					}
					
					if (candidates == null || candidates.isEmpty()) {
						Association random = neurons.random(brainId);
						if (random != null) {
							left = random.getId().getLeft();
							right = random.getId().getRight();
						}
					} else {
						int i = ThreadLocalRandom.current().nextInt(candidates.size());
						left = candidates.get(i).getId().getLeft();
						right = candidates.get(i).getId().getRight();
					}
				} else {
					int i = ThreadLocalRandom.current().nextInt(candidates.size());
					left = candidates.get(i).getId().getLeft();
					right = candidates.get(i).getId().getRight();
				}
			}
		}
		
		left = left.strip();
		right = right.strip();
		if (left.isEmpty() || right.isEmpty()) {
			return "...";
		} else {
			List<String> words = new LinkedList<>();
			words.add(left);
			words.add(right);
			
			boolean keepgoing = true;
			while (keepgoing) {
				keepgoing = keepgoing && words.size() < MAX_OUTPUT_WORDS;
				
				String nextWord = getWord(brainId, left, right);
				if (nextWord != null && !nextWord.isEmpty()) {
					words.add(nextWord.strip());
					left = right;
					right = nextWord.strip();
					
					keepgoing = shouldContinue(nextWord.strip());
				} else {
					keepgoing = false;
				}
			}
			StringBuilder sb = new StringBuilder();
			for (String w : words) {
				sb.append(w);
				sb.append(" ");
			}
			if (sb.length() > MAX_OUTPUT_CHARS) {
				return sb.substring(0, MAX_OUTPUT_CHARS);
			}
			return sb.toString();
		}
	}
	
	private static boolean shouldContinue(String lastWord) {
		// ends with punctuation means stop
		Matcher m = PATTERN_ENDS_WITH_PUNCTUATION.matcher(lastWord);
		return ThreadLocalRandom.current().nextInt(100) > PERCENT_TO_END_PER_WORD && 
				!m.find();
	}
	
	private String getWord(Long brainId, String left, String right) {
		List<Association> candidates;
		if (right == null || right.isEmpty()) {
			candidates = neurons.getCandidates(brainId, left);
		} else {
			candidates = neurons.getCandidates(brainId, left, right);
		}
		
		if (candidates == null || candidates.size() == 0) {
			return null;
		}
		
		// for memory efficiency just in case
		// dont actually keep n*x instances here
		int count = 0;
		for (Association candidate : candidates) {
			count += candidate.getCount();
		}
		int index = ThreadLocalRandom.current().nextInt(0, count);
		Iterator<Association> it = candidates.iterator();
		Association cur = null;
		while (index >= 0 && it.hasNext()) {
			cur = it.next();
			index -= cur.getCount();
		}
		if (cur == null)
			return null;
		return cur.getId().getValue();
	}
}
