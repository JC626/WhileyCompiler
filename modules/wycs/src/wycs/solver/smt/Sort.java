package wycs.solver.smt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import wycc.util.Pair;

/**
 * A sort is the term for a type in the SMT domain. This class provides a few default sorts as well
 * as the ability to create generic sorts, such as {@link wycs.solver.smt.Sort.Set}s or {@link
 * wycs.solver.smt.Sort.Tuple}s. In order to utilise these generic sorts, it is required that the
 * initialisers (from {@link #generateInitialisers()} be added to the surrounding {@link
 * wycs.solver.smt.Block} or {@link wycs.solver.smt.Smt2File}.
 * <p/>
 * This design pattern is required as it is not possible to easily write custom theorems to add to
 * SMT solvers.
 *
 * @author Henry J. Wylde
 */
public abstract class Sort {

    /**
     * The name for an array sort.
     */
    public static final String ARRAY = "Array";
    /**
     * The name for a boolean sort.
     */
    public static final String BOOL = "Bool";
    /**
     * The name for an integer sort.
     */
    public static final String INT = "Int";
    /**
     * The name for a rational sort.
     */
    public static final String REAL = "Real";

    /**
     * This class can only be instantiated locally.
     */
    Sort() {}

    /**
     * Generates the required statements to use this generic sort. Will also add in extra utility
     * functions for working with the sort.
     *
     * @return the generated initialisation statements.
     */
    public abstract List<Stmt> generateInitialisers();

    /**
     * TODO: Documentation.
     *
     * @author Henry J. Wylde
     */
    public static final class Set extends Sort {

        public static final String FUN_ADD_NAME = "add";
        public static final String FUN_CONTAINS_NAME = "contains";
        public static final String FUN_EMPTY_NAME = "empty";
        public static final String FUN_LENGTH_NAME = "length";
        public static final String FUN_REMOVE_NAME = "remove";
        public static final String FUN_SUBSET_NAME = "subset";
        public static final String FUN_SUBSETEQ_NAME = "subseteq";

        private final String type;

        public Set(String type) {
            if (type == null) {
                throw new NullPointerException("type cannot be null");
            }

            this.type = type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Stmt> generateInitialisers() {
            List<Stmt> initialisers = new ArrayList<>();

            initialisers.addAll(generateSorts());
            initialisers.addAll(generateAddFunctions());
            initialisers.addAll(generateContainsFunctions());
            initialisers.addAll(generateRemoveFunctions());
            initialisers.addAll(generateEmptyConstants());
            initialisers.addAll(generateLengthFunctions());
            initialisers.addAll(generateEmptyLengthAssertions());
            initialisers.addAll(generateSubsetFunctions());

            return initialisers;
        }

        /**
         * Gets the name of this set sort. The name is simply "Set".
         * <p/>
         * For the sort name to use in functions etc., use {@link #toString()}.
         *
         * @return the name of this set sort.
         */
        public String getName() {
            return "Set";
        }

        /**
         * Gets the name of this particular set sort, with it's type. The whole name is surrounded
         * by parenthesis.
         */
        @Override
        public String toString() {
            return "(" + getName() + " " + type + ")";
        }

        private List<Stmt> generateAddFunctions() {
            List<Pair<String, String>> parameters = new ArrayList<>();
            parameters.add(new Pair<>("set", toString()));
            parameters.add(new Pair<>("t", type));
            String expr = "(store set t true)";

            return Arrays.<Stmt>asList(new Stmt.DefineFun(FUN_ADD_NAME, parameters, toString(),
                    expr));
        }

        private List<Stmt> generateContainsFunctions() {
            List<Pair<String, String>> parameters = new ArrayList<>();
            parameters.add(new Pair<>("set", toString()));
            parameters.add(new Pair<>("t", type));
            String expr = "(select set t)";

            return Arrays.<Stmt>asList(new Stmt.DefineFun(FUN_CONTAINS_NAME, parameters, BOOL,
                    expr));
        }

        private List<Stmt> generateEmptyConstants() {
            List<Stmt> stmts = new ArrayList<>();

            stmts.add(new Stmt.DeclareFun(FUN_EMPTY_NAME, Collections.EMPTY_LIST, toString()));
            stmts.add(new Stmt.Assert(
                    "(not (exists ((t " + type + ")) (contains " + FUN_EMPTY_NAME + " t)))"));

            return stmts;
        }

        private List<Stmt> generateEmptyLengthAssertions() {
            return Arrays.<Stmt>asList(new Stmt.Assert("(= (length " + FUN_EMPTY_NAME + ") 0)"));
        }

        private List<Stmt> generateLengthFunctions() {
            List<Stmt> stmts = new ArrayList<>();

            stmts.add(new Stmt.DeclareFun(FUN_LENGTH_NAME, Arrays.asList(toString()), INT));
            stmts.add(new Stmt.Assert("(forall ((set " + toString() + ")) (<= 0 (length set)))"));
            stmts.add(new Stmt.Assert(
                    "(forall ((set " + toString() + ")) (=> (not (= set " + FUN_EMPTY_NAME
                            + ")) (exists ((t " + type
                            + ")) (and (contains set t) (= (length set) (+ 1 (length (remove set t))))))))"));
            // TODO: This conjecture really should be iff or xor (going both ways), however using
            // xor causes it to time out, so for now we use implication
            //            lines.add(new Stmt.Assert(
            //                    "(forall ((set " + toString() + ")) (xor (= set " + FUN_EMPTY_NAME
            //                            + ") (exists ((t " + type
            //                            + ")) (and (contains set t) (= (length set) (+ 1 (length (remove set t))))))))"
            //            ));

            return stmts;
        }

        private List<Stmt> generateRemoveFunctions() {
            List<Pair<String, String>> parameters = new ArrayList<>();
            parameters.add(new Pair<>("set", toString()));
            parameters.add(new Pair<>("t", type));
            String expr = "(store set t false)";

            return Arrays.<Stmt>asList(new Stmt.DefineFun(FUN_REMOVE_NAME, parameters, toString(),
                    expr));
        }

        private List<Stmt> generateSorts() {
            List<String> parameters = Arrays.asList("T");
            String expr = "(" + ARRAY + " T " + BOOL + ")";

            return Arrays.<Stmt>asList(new Stmt.DefineSort(getName(), parameters, expr));
        }

        private List<Stmt> generateSubsetFunctions() {
            List<Pair<String, String>> parameters = new ArrayList<>();
            parameters.add(new Pair<>("first", toString()));
            parameters.add(new Pair<>("second", toString()));
            String subseteqExpr =
                    "(forall ((t " + type + ")) (=> (contains first t) (contains second t)))";
            String subsetExpr = "(and (subseteq first second) (exists ((t " + type
                    + ")) (and (not (contains first t)) (contains second t))))";
            // Alternative that uses length
            // String subsetExpr = "(and (subseteq first second) (distinct (length first) (length second)))";

            List<Stmt> functions = new ArrayList<>();
            functions.add(new Stmt.DefineFun(FUN_SUBSETEQ_NAME, parameters, BOOL, subseteqExpr));
            functions.add(new Stmt.DefineFun(FUN_SUBSET_NAME, parameters, BOOL, subsetExpr));

            return functions;
        }
    }

    /**
     * TODO: Documentation.
     *
     * @author Henry J. Wylde
     */
    public static final class Tuple extends Sort {

        public static final String FUN_GET_NAME = "get";

        private final List<String> types;

        public Tuple(String... types) {
            this(Arrays.asList(types));
        }

        public Tuple(List<String> types) {
            if (types.contains(null)) {
                throw new NullPointerException("types cannot contain null");
            }

            this.types = Collections.unmodifiableList(new ArrayList<>(types));
        }

        public static String generateGetFunctionName(int index) {
            return FUN_GET_NAME + index;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Stmt> generateInitialisers() {
            List<Stmt> initialisers = new ArrayList<>();

            initialisers.addAll(generateSorts());
            initialisers.addAll(generateGetFunctions());

            return initialisers;
        }

        /**
         * Gets the name of this tuple sort. The name is equivalent to "Tuple" with the elements
         * size appended.
         * <p/>
         * For the sort name to use in functions etc., use {@link #toString()}.
         *
         * @return the name of this tuple sort.
         */
        public String getName() {
            return "Tuple" + types.size();
        }

        /**
         * Gets the name of this particular tuple sort, with it's types. The whole name is
         * surrounded by parenthesis.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("(");
            sb.append(getName());
            for (String type : types) {
                sb.append(" ").append(type);
            }
            sb.append(")");

            return sb.toString();
        }

        private List<Stmt> generateGetFunctions() {
            List<Stmt> stmts = new ArrayList<>();

            for (int i = 0; i < types.size(); i++) {
                stmts.add(new Stmt.DeclareFun(generateGetFunctionName(i), Arrays.asList(toString()),
                        types.get(i)));
            }

            return stmts;
        }

        private List<Stmt> generateSorts() {
            return Arrays.<Stmt>asList(new Stmt.DeclareSort(getName(), types.size()));
        }
    }
}
