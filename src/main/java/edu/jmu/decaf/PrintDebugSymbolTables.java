package edu.jmu.decaf;

import java.io.*;

/**
 * AST pre-order visitor; prints each node with a symbol table to standard
 * output.
 * 
 * For best output, should be run AFTER {@link BuildParentLinks}, {@link
 * CalculateNodeDepths}, and {@link BuildSymbolTables}.
 *
 */
class PrintDebugSymbolTables extends DefaultASTVisitor
{
    private PrintStream output;

    public PrintDebugSymbolTables()
    {
        this(System.out);
    }

    public PrintDebugSymbolTables(PrintStream output)
    {
        this.output = output;
    }

    public void indent(ASTNode node)
    {
        if (node.attributes.containsKey("depth")) {
            int level = ((Integer)node.attributes.get("depth")).intValue();
            while (level > 0) {
                output.print("  ");
                level--;
            }
        }
    }

    @Override
    public void defaultPreVisit(ASTNode node)
    {
        if (node.attributes.containsKey("symbolTable")) {
            SymbolTable table = (SymbolTable)node.attributes.get("symbolTable");
            indent(node);
            output.println(node.getASTTypeStr());
            if (node.attributes.containsKey("depth")) {
                int level = ((Integer)node.attributes.get("depth")).intValue();
                output.println(table.toString(level));
            } else {
                output.println(table.toString());
            }
            output.println();
        }
    }
}

