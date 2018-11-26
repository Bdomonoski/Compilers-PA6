package edu.jmu.decaf;

/**
 * Static analysis; perform type checking.
 */
public abstract class DecafAnalysis extends StaticAnalysis
{
    /**
     * Retrieves symbol information for a given symbol name. Searches for
     * symbol tables up the parent tree if there is no table at the given
     * node. Adds a static analysis error and returns null if the symbol
     * cannot be found.
     *
     * @param node {@link ASTNode} to search
     * @param name Decaf symbol name
     * @return Symbol information
     */
    public static Symbol lookupSymbol(ASTNode node, String name)
    {
        Symbol sym = null;
        ASTNode tableParent = node;
        while (tableParent != null && !tableParent.attributes.containsKey("symbolTable")) {
            tableParent = tableParent.getParent();
        }
        if (tableParent == null) {
            addError("Symbol not found: \"" + name + "\"" +
                        " at " + node.getSourceInfo().toString());
        } else {
            try {
                SymbolTable table = (SymbolTable)tableParent.attributes.get("symbolTable");
                sym = table.lookup(name);
            } catch (InvalidProgramException ex) {
                addError(new InvalidProgramException(ex.getMessage() +
                            " at " + node.getSourceInfo().toString()));
            }
        }
        return sym;
    }
}
