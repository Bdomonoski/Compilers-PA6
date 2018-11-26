package edu.jmu.decaf;

import java.util.*;

/**
 * Decaf program. Basically just a list of {@link ASTVariable} and {@link
 * ASTFunction} declarations.
 */
public class ASTProgram extends ASTNode
{
    public List<ASTVariable> variables;
    public List<ASTFunction> functions;

    public ASTProgram()
    {
        super();
        this.variables = new ArrayList<ASTVariable>();
        this.functions = new ArrayList<ASTFunction>();
    }

    @Override
    public void traverse(ASTVisitor visitor)
    {
        visitor.preVisit(this);
        for (ASTVariable v : variables) {
            v.traverse(visitor);
        }
        for (ASTFunction f : functions) {
            f.traverse(visitor);
        }
        visitor.postVisit(this);
    }
}

