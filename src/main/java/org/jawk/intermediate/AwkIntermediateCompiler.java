package org.jawk.intermediate;

import org.jawk.ext.JawkExtension;
import org.jawk.frontend.AwkParser;
import org.jawk.frontend.AwkSyntaxTree;
import org.jawk.util.ScriptSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Compiles source code into intermediate representations.
 *
 * This compiler is stateless; i.e, it will create a new parser every time it is invoked.
 * The only reusable resources are the given extensions.
 */
public class AwkIntermediateCompiler {

    /* --- Data Members --- */

    private final Map<String, JawkExtension> extensions;
    private final boolean additionalFunctions;
    private final boolean additionalTypeFunctions;
    private final boolean useStdIn;

    /* --- Constructors --- */

    public AwkIntermediateCompiler(Map<String, JawkExtension> extensions,
                                   boolean additionalFunctions,
                                   boolean additionalTypeFunctions,
                                   boolean useStdIn) {

        this.extensions = extensions;
        this.additionalFunctions = additionalFunctions;
        this.additionalTypeFunctions = additionalTypeFunctions;
        this.useStdIn = useStdIn;
    }

    public AwkIntermediateCompiler() {

        this(Collections.<String, JawkExtension>emptyMap());
    }

    public AwkIntermediateCompiler(Map<String, JawkExtension> extensions) {

        this(extensions, false, false, false);
    }

    /* --- Methods --- */

    /* --- Public Methods --- */

    /**
     * Compiles the given source into intermediate tuples and returns these tuples.
     *
     * @param source The given source.
     * @return The intermediate tuples.
     * @throws IOException if an error occurs during compilation.
     */
    public AwkTuples compile(String source) throws IOException {

        List<ScriptSource> sources = Arrays.asList(
                new ScriptSource("<inline-script>", new StringReader(source), false)
        );
        return compile(sources);
    }

    /**
     * Compiles the given sources into intermediate tuples and returns these tuples.
     *
     * @param sources The given sources.
     * @return The intermediate tuples.
     * @throws IllegalArgumentException if any of the sources is already in intermediate form.
     * @throws IOException              if an error occurs during compilation.
     */
    public AwkTuples compile(List<ScriptSource> sources) throws IllegalArgumentException, IOException {

        for (ScriptSource source : sources) {
            if (source.isIntermediate()) {
                throw new IllegalArgumentException("Cannot compile intermediate code: " + source.getDescription());
            }
        }

        AwkParser parser = new AwkParser(additionalFunctions, additionalTypeFunctions, useStdIn, extensions);
        AwkSyntaxTree ast = parser.parse(sources);

        // 1st pass to tie actual parameters to back-referenced formal parameters
        ast.semanticAnalysis();
        // 2nd pass to tie actual parameters to forward-referenced formal parameters
        ast.semanticAnalysis();
        // build tuples
        AwkTuples tuples = new AwkTuples();
        int result = ast.populateTuples(tuples);
        // ASSERTION: NOTHING should be left on the operand stack ...
        assert result == 0;
        // Assign queue.next to the next element in the queue.
        // Calls touch(...) per Tuple so that addresses can be normalized/assigned/allocated
        tuples.postProcess();
        // record global_var -> offset mapping into the tuples
        // so that the interpreter/compiler can assign variables
        // on the "file list input" command line
        parser.populateGlobalVariableNameToOffsetMappings(tuples);

        return tuples;
    }

    /**
     * Compiles the given sources into intermediate syntax-tree and returns it.
     *
     * @param sources The given sources.
     * @return The intermediate syntax-tree.
     * @throws IOException if an error occurs during compilation.
     */
    public AwkSyntaxTree parseAst(List<ScriptSource> sources) throws IOException {

        AwkParser parser = new AwkParser(additionalFunctions, additionalTypeFunctions, useStdIn, extensions);
        return parser.parse(sources);
    }
}
