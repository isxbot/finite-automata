import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NFAConvert {

	/**
	 * Take an NFA and output the equivalent DFA.
	 */
	public static void main(String[] args) {
		ArrayList<String> states = new ArrayList<String>();
		ArrayList<String> nfaFinalStates = new ArrayList<String>();
		HashMap<String, HashMap<String, String>> nfaDeltas = new HashMap<>();
		HashMap<String, HashMap<String, String>> dfaDeltas = new HashMap<>();
		ArrayList<String> nfaSpec = new ArrayList<String>();
		ArrayList<String> alphabet = new ArrayList<String>();
		String startState = null;

		// Get the DFA characteristics.
		try (Stream<String> stream = Files.lines(Paths.get(args[0]))) {
			nfaSpec = (ArrayList<String>) stream.collect(Collectors.toList());
		} catch (IOException e) {
			System.err.println("NFAConvert: the file \'" + args[0] + "\' could not be opened.");
			System.exit(0);
		}

		// Get the start state; Q0.
		startState = nfaSpec.get(nfaSpec.indexOf("% Q0") + 1);

		// Get the states of the DFA; Q.
		parseStates(nfaSpec, states);

		// Parse the alphabet
		parseAlphabet(nfaSpec, alphabet);

		// Get the final states of the DFA; F.
		parseFinalStates(nfaSpec, nfaFinalStates);

		// Map NFA delta (with epsilon transitions).
		nfaDeltas = mapDelta(states, nfaSpec);

		// Get the power sets
		dfaDeltas = getPowerSet(nfaDeltas, startState);

		// Map the power sets
		mapPowerSet(dfaDeltas, nfaDeltas, alphabet);
		
		// Print states
		System.out.println("% Q");
		for(String state : dfaDeltas.keySet()) {
			System.out.println("{" + state.toString() + "}");
		}
		// Print alphabet
		System.out.println("% Sigma");
		for(String input : alphabet) {
			System.out.println(input);
		}
		
		// Print final states
		System.out.println("% F");
		for(String state : nfaFinalStates) {
			for(String key : dfaDeltas.keySet()) {
				if(key.contains(state)) {
					System.out.println("{" + key + "}");
				}
			}
		}
		
		// Print start state
		System.out.println("% Q0");
		System.out.println("{" + startState + "}");
		
		System.out.println("% Delta");
		for (String name : dfaDeltas.keySet()) {
			String key = name.toString();
			HashMap<String, String> values = dfaDeltas.get(name);
			for(String input : values.keySet()) {
			    System.out.println("{" + key + "}" +  " " + input + " " + "{" + values.get(input) + "}");
			}
		}
	}

	/*
	 * Parse alphabet
	 */
	static void parseAlphabet(ArrayList<String> nfaSpec, ArrayList<String> alphabet) {
		// Add characters between % Sigma and next % F.
		for (int i = nfaSpec.indexOf("% Sigma") + 1; i < nfaSpec.indexOf("% F"); i++) {
			alphabet.add(nfaSpec.get(i));
		}
	}

	/*
	 * Parse states
	 */
	static void parseStates(ArrayList<String> nfaSpec, ArrayList<String> states) {
		// Add states between %Q and next % Sigma.
		for (int i = nfaSpec.indexOf("% Q") + 1; i < nfaSpec.indexOf("% Sigma"); i++) {
			states.add(nfaSpec.get(i));
		}
	}

	/*
	 * Parse finalStates
	 */
	static void parseFinalStates(ArrayList<String> nfaSpec, ArrayList<String> finalStates) {
		for (int i = nfaSpec.indexOf("% F") + 1; i < nfaSpec.indexOf("% Q0"); i++) {
			finalStates.add(nfaSpec.get(i));
		}
	}

	/*
	 * Map the transition function
	 */
	static HashMap<String, HashMap<String, String>> mapDelta(ArrayList<String> states, ArrayList<String> nfaSpec) {
		// Key is the state as String, value is HashMap of <input value, next state>.
		HashMap<String, HashMap<String, String>> deltas = new HashMap<>();
		StringBuilder sb = new StringBuilder();

		// Allow each state to reach itself on E*
		for (int i = nfaSpec.indexOf("% Q") + 1; i < nfaSpec.indexOf("% Sigma"); i++) {

		}

		for (int i = nfaSpec.indexOf("% Delta") + 1; i < nfaSpec.size(); i++) {
			// [0] is current state, [1] is input value, [2] is next state.
			String[] line = nfaSpec.get(i).split("\\s");
			HashMap<String, String> transition = new HashMap<>();
			String state = line[0];
			String input = line[1];
			String next = line[2];

			// First put each state into its own E column.
			transition.put("E", state);

			// If epsilon input, check if an E entry exists, put if not.
			if (input.contentEquals("E") && !deltas.get(state).containsKey("E")) {
				// Add current state to epsilon transition.
				sb.append(state + "," + next);
				transition.put(input, sb.toString());
			} else if (input.contentEquals("E") && deltas.get(state).containsKey("E")) {
				sb.append(deltas.get(state).get("E") + "," + next);
				transition.put(input, sb.toString());
			} else {
				transition.put(input, next);
			}

			if (deltas.get(state) == null) {
				deltas.put(state, transition);
			} else if (input.contentEquals("E") && deltas.get(state).containsKey("E")) {
				deltas.get(state).replace("E", transition.get("E"));
			} else {
				deltas.get(state).put(input, transition.get(input));
			}
			// Reset StringBuilder
			sb.setLength(0);
		}
		return deltas;
	}

	/*
	 * Get the power sets
	 */
	static HashMap<String, HashMap<String, String>> getPowerSet(HashMap<String, HashMap<String, String>> nfaDelta,
			String startState) {
		HashMap<String, HashMap<String, String>> dfaDelta = new HashMap();
		ArrayList<String> newStates = new ArrayList<String>();
		newStates.add(startState);

		// Determine power sets
		for (String state : nfaDelta.keySet()) {
			// Get transitions for this state.
			HashMap<String, String> transitions = nfaDelta.get(state);

			// Map input character + E*
			for (String input : transitions.keySet()) {
				if (input.contentEquals("E")) {
					continue;
				}

				String next = transitions.get(input);
				String newState = nfaDelta.get(next).containsKey("E") ? nfaDelta.get(next).get("E") : next;
				if (!newStates.contains(newState)) {
					newStates.add(newState);
				}
			}
		}

		for (String pState : newStates) {
			dfaDelta.put(pState, null);
		}
		return dfaDelta;
	}

	/*
	 * Map the power set values
	 */
	static void mapPowerSet(HashMap<String, HashMap<String, String>> dfaDeltas,
			HashMap<String, HashMap<String, String>> nfaDeltas, ArrayList<String> alphabet) {
		

		// Iterate over each power set
		for (String set : dfaDeltas.keySet()) {
			HashMap<String, String> newTRA = new HashMap();
			StringBuilder values = new StringBuilder();

			// Check each input value for states.
			for (String input : alphabet) {
				String[] states = set.split(",");

				for (int i = 0; i < states.length; i++) {
					if (nfaDeltas.get(states[i]).containsKey(input)) {
						String next = nfaDeltas.get(states[i]).get(input);
						values.append(next + ",");
						values.append(nfaDeltas.get(next).get("E"));
					}
				}

				// Remove duplicates
				List<String> listWithoutDuplicates = Arrays.asList(values.toString().split(","))
						.stream()
						.distinct()
						.collect(Collectors.toList());
				
				values.setLength(0);
				
				for(String state : listWithoutDuplicates) {
					if(listWithoutDuplicates.indexOf(state) == listWithoutDuplicates.size() - 1) {
						values.append(state);
					} else {
						values.append(state + ",");
					}
				}

				// Don't insert if there are no values.
				if(values.length() == 0) {
					continue;
				}
				
				newTRA.put(input, values.toString());
				dfaDeltas.put(set, newTRA);
				values.setLength(0);
			}
		}
	}
}
