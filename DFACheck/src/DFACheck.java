import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DFACheck {
	public static void main(String[] args) {
		ArrayList<String> states = new ArrayList<String>();
		ArrayList<String> finalStates = new ArrayList<String>();
		ArrayList<String> alphabet = new ArrayList<String>();
		HashMap<String, HashMap<String, String>> deltas = new HashMap<>();
		ArrayList<String> dfaSpec = new ArrayList<String>();
		ArrayList<String> testStrings = new ArrayList<String>();
		String currentState = null;

		// Parse first file for DFA rules.
		try (Stream<String> stream = Files.lines(Paths.get(args[0]))) {
			dfaSpec = (ArrayList<String>) stream.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Parse input from second file.
		try (Stream<String> stream = Files.lines(Paths.get(args[1]))) {
			testStrings = (ArrayList<String>) stream.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}

		currentState = dfaSpec.get(dfaSpec.indexOf("% Q0") + 1);
		parseStates(dfaSpec, states);
		parseFinalStates(dfaSpec, finalStates);
		parseAlphabet(dfaSpec, alphabet);

		// Map the deltas
		deltas = buildDelta(states, dfaSpec);

	}

	/*
	 * Parse states
	 */
	static void parseStates(ArrayList<String> dfaSpec, ArrayList<String> states) {
		// Add states between %Q and next % Sigma.
		for (int i = dfaSpec.indexOf("% Q") + 1; i < dfaSpec.indexOf("% Sigma"); i++) {
			states.add(dfaSpec.get(i));
		}
	}

	/*
	 * Parse finalStates
	 */
	static void parseFinalStates(ArrayList<String> dfaSpec, ArrayList<String> finalStates) {
		for (int i = dfaSpec.indexOf("% F") + 1; i < dfaSpec.indexOf("% Q0"); i++) {
			finalStates.add(dfaSpec.get(i));
		}
	}

	/*
	 * Parse alphabet
	 */
	static void parseAlphabet(ArrayList<String> dfaSpec, ArrayList<String> alphabet) {
		for (int i = dfaSpec.indexOf("% Sigma") + 1; i < dfaSpec.indexOf("% F"); i++) {
			alphabet.add(dfaSpec.get(i));
		}
	}

	/*
	 * Build deltas
	 */
	static HashMap<String, HashMap<String, String>> buildDelta(ArrayList<String> states, ArrayList<String> dfaSpec) {
		HashMap<String, HashMap<String, String>> deltas = new HashMap<>();
		for (int i = dfaSpec.indexOf("% Delta") + 1; i < dfaSpec.size(); i++) {
			String[] line = dfaSpec.get(i).split("\\s");
			HashMap<String, String> transition = new HashMap<>();
			transition.put(line[1], line[2]);

			if (deltas.get(line[0]) == null) {
				deltas.put(line[0], transition);
			} else {
				deltas.get(line[0]).put(line[1], line[2]);
			}
		}
		return deltas;
	}
}
