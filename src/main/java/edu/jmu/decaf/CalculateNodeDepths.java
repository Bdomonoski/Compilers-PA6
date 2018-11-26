package edu.jmu.decaf;

/**
 * AST pre-order visitor; calculates "depth" at each node in the AST.
 * Must be run AFTER {@link BuildParentLinks}.
 *
 */
class CalculateNodeDepths extends DefaultASTVisitor
{
    // must be run AFTER BuildNodeParentLinks
    //

    // special handler for top level (base case)
    @Override
    public void preVisit(ASTProgram node)
    {
        node.setDepth(0);
    }

    // generic handler (recursive case)
    @Override
    public void defaultPreVisit(ASTNode node)
    {
        node.setDepth(node.getParent().getDepth() + 1);
    }
}

