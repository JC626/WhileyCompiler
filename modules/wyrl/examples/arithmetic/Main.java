import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import wyautl.core.Automaton;
import wyautl.io.PrettyAutomataWriter;
import wyautl.rw.*;

public final class Main {
    public enum RewriteMode { SIMPLE, STATIC_DISPATCH };
    
    private Main() {} // avoid instantiation of this class

    public static void main(String[] args) {
	final BufferedReader input =
	    new BufferedReader(new InputStreamReader(System.in));

	try {
	    RewriteMode rwMode = RewriteMode.STATIC_DISPATCH;
	    System.out.println("Welcome!\n");			
	    while(true) {				
		System.out.print("> ");
		String text = input.readLine();

		// commands goes here
		if(text.equals("help")) {
		    printHelp();
		} else if(text.equals("exit")) {
		    System.exit(0);
		} else if(text.equals("rw-simple")) {
		    rwMode = RewriteMode.SIMPLE;
		    System.out.println("Rewrite mode: simple");
		} else if(text.equals("rw-static-dispatch")) {
		    System.out.println("Rewrite mode: static-dispatch");
		    rwMode = RewriteMode.STATIC_DISPATCH;
		} else {
		    reduce(text,rwMode);
		}
	    }
	} catch(IOException e) {
	    System.err.println("I/O Error - " + e.getMessage());
	}
    }

    private static void reduce(String text, RewriteMode rwMode) {
	try {				
	    Parser parser = new Parser(text);
	    Automaton automaton = new Automaton();
	    int root = parser.parse(automaton);
	    automaton.setRoot(0, root);
			
	    PrettyAutomataWriter writer = new PrettyAutomataWriter(System.out,
								   Arithmetic.SCHEMA, "Or", "And");
	    System.out.println("------------------------------------");
	    writer.write(automaton);
	    writer.flush();
			
	    Rewriter rw;
	    switch(rwMode) {
	    case SIMPLE:
		rw = new SimpleRewriter(Arithmetic.inferences,Arithmetic.reductions,Arithmetic.SCHEMA);
		break;
	    case STATIC_DISPATCH:
		rw = new StaticDispatchRewriter(Arithmetic.inferences,Arithmetic.reductions,Arithmetic.SCHEMA);
		break;
	    default:
		rw = null;
	    }
	    rw.apply(automaton);
	    System.out.println("\n\n=> (" + rw.getStats() + ")\n");
	    writer.write(automaton);
	    writer.flush();
	    System.out.println("\n");			
	} catch(RuntimeException e) {
	    // Catching runtime exceptions is actually rather bad style;
	    // see lecture about Exceptions later in the course!
	    System.err.println("error: " + e.getMessage());
	    e.printStackTrace(System.err);
	    System.err.println("Type \"help\" for help");
	} catch(IOException e) {
	    System.err.println("I/O Exception?");
	}
    }

    private static void printHelp() {
	System.out.println("Calculator commands:");
	System.out.println("\thelp --- access this help page");
	System.out.println("\texit --- quit");
    }
}