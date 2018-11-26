package edu.jmu.decaf;

import java.io.*;
import java.util.*;

/**
 * Build a digraph DOT representation of a Decaf AST, which can converted to an
 * image (e.g., PNG or JPEG) using the "dot" utility included with GraphViz.
 */
class ExportTreeDOT extends DefaultASTVisitor
{
    private PrintStream output;
    private boolean showTypes;
    private Map<String, Integer> counts;

    public ExportTreeDOT()
    {
        this(System.out, false);
    }

    public ExportTreeDOT(PrintStream output)
    {
        this(output, false);
    }

    public ExportTreeDOT(PrintStream output, boolean showTypes)
    {
        this.output = output;
        this.showTypes = showTypes;
        this.counts = new HashMap<String, Integer>();
    }

    public int getNextID(String typeLabel)
    {
        if (counts.containsKey(typeLabel)) {
            int nextID = counts.get(typeLabel).intValue() + 1;
            counts.put(typeLabel, Integer.valueOf(nextID));
            return nextID;
        } else {
            counts.put(typeLabel, Integer.valueOf(1));
            return 1;
        }
    }

    @Override
    public void defaultPreVisit(ASTNode node)
    {
        String typeLabel = node.getASTTypeStr();
        node.attributes.put("dotid", typeLabel + getNextID(typeLabel));
    }

    @Override
    public void preVisit(ASTProgram node)
    {
        output.println("digraph AST {");
        node.attributes.put("dotid", "Program");
    }

    @Override
    public void defaultPostVisit(ASTNode node)
    {
        printNodeLabel(node, null, node.getASTTypeStr().toLowerCase());
    }

    @Override
    public void postVisit(ASTProgram node)
    {
        printNodeLabel(node, "Program");
        for (ASTVariable v : node.variables) {
            printLink(node, v);
        }
        for (ASTFunction f : node.functions) {
            printLink(node, f);
        }
        output.println("}");
    }

    @Override
    public void postVisit(ASTVariable node)
    {
        printNodeLabel(node, "\\\"" + node.name + "\\\"" +
                " : " + ASTNode.typeToString(node.type) +
                (node.arrayLength > 1 ? "[" + node.arrayLength + "]" : ""));
    }

    @Override
    public void postVisit(ASTFunction node)
    {
        printNodeLabel(node, "\\\"" + node.name + "\\\"" +
                " : " + ASTNode.typeToString(node.returnType) +
                " " + node.getParameterStr());
        printLink(node, node.body);
    }

    @Override
    public void postVisit(ASTBlock node)
    {
        printNodeLabel(node, null, "{ }");
        for (ASTVariable v : node.variables) {
            printLink(node, v);
        }
        for (ASTStatement s : node.statements) {
            printLink(node, s);
        }
    }

    @Override
    public void postVisit(ASTAssignment node)
    {
        printNodeLabel(node, null, "=");
        printLink(node, node.location);
        printLink(node, node.value);
    }

    @Override
    public void postVisit(ASTVoidFunctionCall node)
    {
        printNodeLabel(node, node.name + "(...)");
        for (ASTExpression e : node.arguments) {
            printLink(node, e);
        }
    }

    @Override
    public void postVisit(ASTConditional node)
    {
        printNodeLabel(node, null, "if");
        printLink(node, node.condition);
        printLink(node, node.ifBlock);
        if (node.hasElseBlock()) {
            printLink(node, node.elseBlock);
        }
    }

    @Override
    public void postVisit(ASTWhileLoop node)
    {
        printNodeLabel(node, null, "while");
        printLink(node, node.guard);
        printLink(node, node.body);
    }

    @Override
    public void postVisit(ASTReturn node)
    {
        printNodeLabel(node, null, "return");
        if (node.hasValue()) {
            printLink(node, node.value);
        }
    }

    @Override
    public void postVisit(ASTBinaryExpr node)
    {
        printNodeLabel(node, ASTBinaryExpr.opToString(node.operator));
        printLink(node, node.leftChild);
        printLink(node, node.rightChild);
    }

    @Override
    public void postVisit(ASTUnaryExpr node)
    {
        printNodeLabel(node, ASTUnaryExpr.opToString(node.operator));
        printLink(node, node.child);
    }

    @Override
    public void postVisit(ASTFunctionCall node)
    {
        printNodeLabel(node, node.name + "(...)");
        for (ASTExpression e : node.arguments) {
            printLink(node, e);
        }
    }

    @Override
    public void postVisit(ASTLocation node)
    {
        printNodeLabel(node, node.name + (node.hasIndex() ? "[]" : ""));
        if (node.hasIndex()) {
            printLink(node, node.index);
        }
    }

    @Override
    public void postVisit(ASTLiteral node)
    {
        printNodeLabel(node, node.toString());
    }

    public void printNodeLabel(ASTNode node, String label)
    {
        printNodeLabel(node, label, label);
    }

    public void printNodeLabel(ASTNode node, String regularLabel,
            String shortLabel)
    {
        String id = (String)node.attributes.get("dotid");
        String typeLabel = node.getASTTypeStr();
        StringBuffer label = new StringBuffer();
        label.append(id + " [label=\"");
        if (showTypes) {
            label.append(typeLabel);
            if (regularLabel != null) {
                label.append(regularLabel);
            }
        } else if (shortLabel != null) {
            label.append(shortLabel);
        }

        /* STATIC ANALYSIS */
        if (node.attributes.containsKey("symbolTable")) {
            label.append("\n" +
                ((SymbolTable)node.attributes.get("symbolTable")).toString());
        }
        if (node.attributes.containsKey("symbol")) {
            label.append("\n" +
                    ((Symbol)node.attributes.get("symbol")).toString());
        } else if (node.attributes.containsKey("type")) {
            label.append("\ntype=" + ASTNode.typeToString(
                    (ASTNode.DataType)node.attributes.get("type")));
        }

        /* CODE GENERATION */
        if (node.attributes.containsKey("localSize")) {
            label.append("\nlocalSize=" + Integer.toString(
                    ((Integer)node.attributes.get("localSize")).intValue()));
        }
        if (node.attributes.containsKey("staticSize")) {
            label.append("\nstaticSize=" + Integer.toString(
                    ((Integer)node.attributes.get("staticSize")).intValue()));
        }
        if (node.attributes.containsKey("code")) {
            label.append("\nCODE:");
            ArrayList<ILOCInstruction> code = (ArrayList<ILOCInstruction>)(node.attributes.get("code"));
            for (ILOCInstruction insn : code) {
                label.append("\n" + insn.toString(false).replaceAll("\"", "\\\\\""));
            }
        }

        label.append("\"];");
        output.println(label.toString());
    }

    public void printLink(ASTNode src, ASTNode dst)
    {
        String srcID = (String)src.attributes.get("dotid");
        String dstID = (String)dst.attributes.get("dotid");
        output.println(srcID + " -> " + dstID + ";");
    }
}

