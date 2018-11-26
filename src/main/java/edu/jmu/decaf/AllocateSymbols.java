package edu.jmu.decaf;

public class AllocateSymbols extends DefaultASTVisitor
{
    private int currentStaticSize;
    private int currentLocalSize;

    public AllocateSymbols()
    {
    }

    @Override
    public void preVisit(ASTProgram node)
    {
        currentStaticSize = 0;
    }

    @Override
    public void preVisit(ASTFunction node)
    {
        currentLocalSize = 0;
        if (node.attributes.containsKey("symbolTable")) {
            SymbolTable table = (SymbolTable)node.attributes.get("symbolTable");
            int paramOffset = 0;
            for (ASTFunction.Parameter v : node.parameters) {
                try {
                    Symbol sym = table.lookup(v.name);
                    sym.location = Symbol.MemLoc.STACK_PARAM;
                    sym.offset = paramOffset;
                    paramOffset += sym.totalSize;
                } catch (InvalidProgramException ex) { }
            }
        }
    }

    @Override
    public void preVisit(ASTVariable node)
    {
        ASTNode parent = node.getParent();
        while (parent != null &&
                !parent.attributes.containsKey("symbolTable")) {
            parent = parent.getParent();
        }
        if (parent == null) {
            return;
        }
        SymbolTable table = (SymbolTable)parent.attributes.get("symbolTable");
        try {
            Symbol sym = table.lookup(node.name);
            if (parent instanceof ASTProgram) {
                sym.location = Symbol.MemLoc.STATIC_VAR;
                sym.offset = currentStaticSize;
                currentStaticSize += sym.totalSize;
            } else {
                sym.location = Symbol.MemLoc.STACK_LOCAL;
                sym.offset = currentLocalSize;
                currentLocalSize += sym.totalSize;
            }
        } catch (InvalidProgramException ex) { }
    }

    @Override
    public void postVisit(ASTFunction node)
    {
        node.attributes.put("localSize", Integer.valueOf(currentLocalSize));
    }

    @Override
    public void postVisit(ASTProgram node)
    {
        node.attributes.put("staticSize", Integer.valueOf(currentStaticSize));
    }
}
