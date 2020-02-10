import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Constructs a DFA from a file provided (arg[0]), and tests if strings in a
 * second file (args[1]) are accepted by the DFA.
 * 
 * Note: the alphabet is not validated because we're guaranteed to get input
 * strings of the correct alphabet, in the correct format.
 */
public class DFACheck {
    public static void main(String[] args) {
        ArrayList<String> states = new ArrayList<String>();
        ArrayList<String> finalStates = new ArrayList<String>();
        HashMap<String, HashMap<String, String>> deltas = new HashMap<>();
        ArrayList<String> dfaSpec = new ArrayList<String>();
        ArrayList<String> testStrings = new ArrayList<String>();
        String startState = null;
        String currentState = null;

        if (args.length == 0) {
            System.err.println("DFACheck: no input files specified");
        } else if (args.length == 1) {
            System.err.println("DFACheck: invalid usage - the program must be given two files as input");
        }

        // Get the DFA characteristics from the first file.
        try (Stream<String> stream = Files.lines(Paths.get(args[0]))) {
            dfaSpec = (ArrayList<String>) stream.collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("DFACheck: the file \'" + args[0] + "\' could not be opened.");
            System.exit(0);
        }

        // Parse test strings from second file.
        try (Stream<String> stream = Files.lines(Paths.get(args[1]))) {
            testStrings = (ArrayList<String>) stream.collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("DFACheck: the file \'" + args[0] + "\' could not be opened.");
            System.exit(0);
        }

        // Get the states of the DFA; Q.
        parseStates(dfaSpec, states);

        // Get the final states of the DFA; F.
        parseFinalStates(dfaSpec, finalStates);

        // Map the transition function.
        deltas = buildDelta(states, dfaSpec);

        // Get the first current state; Q0.
        startState = dfaSpec.get(dfaSpec.indexOf("% Q0") + 1);

        // Test strings from the second file.
        for (String line : testStrings) {
            if (finalStates.contains(testLine(line, startState, deltas))) {
                System.out.println(line + " accepted");
            } else {
                System.out.println(line + " rejected");
            }
        }

    }

    /*
     * Check if the string is accepted by the DFA.
     */
    static String testLine(String line, String startState, HashMap<String, HashMap<String, String>> deltas) {
        String[] lineArr = line.split("");
        String currentState = startState;
        for (int i = 0; i < lineArr.length; i++) {
            currentState = deltas.get(currentState).get(lineArr[i]);
        }
        return currentState;
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
     * Map the transition function
     */
    static HashMap<String, HashMap<String, String>> buildDelta(ArrayList<String> states, ArrayList<String> dfaSpec) {
        // Key is the state as String, value is HashMap of <input value, next state>.
        HashMap<String, HashMap<String, String>> deltas = new HashMap<>();

        for (int i = dfaSpec.indexOf("% Delta") + 1; i < dfaSpec.size(); i++) {
            // [0] is current state, [1] is input value, [2] is next state.
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
