package edu.jmu.decaf;

import java.util.*;

/**
 * Decaf program in ILOC intermediate representation (IR). Stores a symbol
 * table for global symbols (basically the symbol table from an ASTProgram
 * node) as well as a list of ILOC function objects.
 *
 * This class also handles allocation of sequential register and label IDs so
 * that virtual registers and labels can continue to be generated after the
 * initial AST-to-ILOC conversion.
 */
public class ILOCProgram
{
    /**
     * Static symbol table (global variables and functions)
     */
    public SymbolTable staticSymbols;

    /**
     * Size (in bytes) to allocate in static region for global variables
     */
    public int staticSize;

    /**
     * List of ILOC functions
     */
    public List<ILOCFunction> functions;

    /**
     * Create a new ILOC program object with an empty symbol table
     */
    public ILOCProgram()
    {
        this.staticSymbols = new SymbolTable();
        this.staticSize = 0;
        this.functions = new ArrayList<ILOCFunction>();
    }

    /**
     * Search for a function based on name
     * @param name Name to search for
     * @return {@link ILOCFunction} if found, {@code null} otherwise
     */
    public ILOCFunction getFunction(String name)
    {
        for (ILOCFunction func : functions) {
            if (func.functionSymbol.name.equals(name)) {
                return func;
            }
        }
        return null;
    }

    /**
     * Builds a standard string-based representation of an ILOC program
     */
    public String toString()
    {
        StringBuffer str = new StringBuffer();
        //str.append(staticSymbols.toString());
        boolean spacing = false;
        for (ILOCFunction f : functions) {
            if (spacing) {
                str.append("\n\n");
            } else {
                spacing = true;
            }
            str.append(f.toString());
        }
        return str.toString();
    }
}
