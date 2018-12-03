package edu.jmu.decaf;

import edu.jmu.decaf.ASTNode.DataType;

/**
 * Perform type checking using the Decaf type rules.
 */
public class MyDecafAnalysis extends DecafAnalysis
{

  /**
   * Infer the type of a literal.
   */
  @Override
  public void preVisit(ASTLiteral node)
  {
    node.setType(node.type);
  }

  /**
   * Infer the type of a unary expression.
   */
  @Override
  public void preVisit(ASTUnaryExpr node)
  {
    switch (node.operator)
    {
      case NEG:
        node.setType(DataType.INT);
        break;
      case NOT:
        node.setType(DataType.BOOL);
        break;
      default:
        addError("Invalid unary operator at " + node.getSourceInfo());
        node.setType(DataType.VOID);
        break;
    }
  }

  /**
   * Infer the type of a function call.
   */
  @Override
  public void preVisit(ASTFunctionCall node)
  {
    Symbol symbol = lookupSymbol(node.getParent(), node.name);
    if (symbol != null)
    {
      node.setType(symbol.type);
    }
  }

  /**
   * Infer the type for ASTLocations.
   */
  @Override
  public void preVisit(ASTLocation node)
  {
    Symbol symbol = lookupSymbol(node.getParent(), node.name);
    if (symbol != null)
    {
      node.setType(symbol.type);
    }
  }

  /**
   * Infer the type of a binary expression.
   */
  @Override
  public void preVisit(ASTBinaryExpr node)
  {
    switch (node.operator)
    {
      case ADD:
        node.setType(DataType.INT);
        break;
      case AND:
        node.setType(DataType.BOOL);
        break;
      case DIV:
        node.setType(DataType.INT);
        break;
      case EQ:
        node.setType(DataType.BOOL);
        break;
      case GE:
        node.setType(DataType.BOOL);
        break;
      case GT:
        node.setType(DataType.BOOL);
        break;
      case LE:
        node.setType(DataType.BOOL);
        break;
      case LT:
        node.setType(DataType.BOOL);
        break;
      case MOD:
        node.setType(DataType.INT);
        break;
      case MUL:
        node.setType(DataType.INT);
        break;
      case NE:
        node.setType(DataType.BOOL);
        break;
      case OR:
        node.setType(DataType.BOOL);
        break;
      case SUB:
        node.setType(DataType.INT);
        break;
      default:
        addError("Unrecognized binary expression at " + node.getSourceInfo());
        node.setType(DataType.VOID);
        break;
    }
  }

  /**
   * Infer the type of functions.
   */
  @Override
  public void preVisit(ASTFunction node)
  {
    node.setType(node.returnType);
  }

  /**
   * Make sure that arrays are declared with positive lengths and are global.
   */
  @Override
  public void postVisit(ASTVariable node)
  {
    // Non-negative size check
    if (node.isArray && node.arrayLength <= 0)
    {
      addError("Non-positive array size at " + node.getSourceInfo());
    }

    // Global check
    if (node.isArray && !(node.getParent() instanceof ASTProgram))
    {
      addError("Non-global array at " + node.getSourceInfo());
    }
  }

  /**
   * Make sure that break statements only exist inside while loops
   */
  @Override
  public void postVisit(ASTBreak node)
  {
    ASTNode n = node;
    while (n != null)
    {
      if (n instanceof ASTWhileLoop)
      {
        // If we found an ancestor that's a while loop, stop looking
        return;
      }
      n = n.getParent();
    }

    // We got to the top of the tree without finding a while loop
    addError("Found break outside of a while loop at " + node.getSourceInfo());
  }

  /**
   * Make sure that continue statements only exist inside while loops
   */
  @Override
  public void postVisit(ASTContinue node)
  {
    ASTNode n = node;
    while (n != null)
    {
      if (n instanceof ASTWhileLoop)
      {
        // If we find an ancestor that's a while loop, stop looking
        return;
      }
      n = n.getParent();
    }

    // We got to the root of the tree without finding a while loop
    addError("Found continue outside of a while loop at " + node.getSourceInfo());
  }

  /**
   * Make sure locations are correct. They can't be functions and if they're arrays then they must
   * have an integer index.
   */
  @Override
  public void postVisit(ASTLocation node)
  {
    Symbol symbol = lookupSymbol(node.getParent(), node.name);

    // Only bother checking if the symbol exists - if it doesn't, lookup already added an error
    if (symbol != null)
    {
      // Functions can't be used as locations
      if (symbol.location == Symbol.MemLoc.STATIC_FUNC)
      {
        addError(
            "Symbol " + node.name + " is a function, not a variable at " + node.getSourceInfo());
      }

      // Arrays need integer index
      else if (symbol.isArray)
      {
        if (node.index == null)
        {
          addError("Arrays must have an index at " + node.getSourceInfo());
        }
        else if (node.index.getType() != DataType.INT)
        {
          addError("Arrays must have an integer index at " + node.getSourceInfo());
        }
      }

      // If it's a normal variable ...
      else
      {
        // ... then it sohuldn't have an index
        if (node.index != null)
        {
          addError("Non-arrays cannot have an index at " + node.getSourceInfo());
        }
      }
    }
  }

  /**
   * Make sure the program has a main function with no parameters that returns an int
   */
  @Override
  public void postVisit(ASTProgram node)
  {
    Symbol symbol = lookupSymbol(node, "main");
    if (symbol != null && (symbol.location != Symbol.MemLoc.STATIC_FUNC
        || symbol.paramTypes.size() > 0 || symbol.type != DataType.INT))
    {
      addError("No main function found");
    }
  }

  /**
   * Make sure void function calls are to functions that exist and have the correct number of
   * arguments with correct types.
   */
  @Override
  public void postVisit(ASTVoidFunctionCall node)
  {
    Symbol symbol = lookupSymbol(node, node.name);

    // If the symbol wasn't found, lookupSymbol already added an error
    if (symbol != null)
    {
      // Function calls need to be to actual functions
      if (symbol.location != Symbol.MemLoc.STATIC_FUNC)
      {
        addError(node.name + " is not a valid function at " + node.getSourceInfo());
      }
      else
      {
        // Make sure there are the correct number of arguments
        if (node.arguments.size() != symbol.paramTypes.size())
        {
          addError(
              "Incorrect number of arguments for " + node.name + " at " + node.getSourceInfo());
        }
        else
        {
          // Make sure the argument types match
          for (int i = 0; i < node.arguments.size(); i++)
          {
            if (node.arguments.get(i).getType() != symbol.paramTypes.get(i))
            {
              addError("Parameter " + (i + 1) + " for " + node.name + " should be "
                  + symbol.paramTypes.get(i) + " but found " + node.arguments.get(i).getType()
                  + " at " + node.getSourceInfo());
            }
          }
        }
      }
    }
  }

  /**
   * Make sure function calls are to functions that exist and have the correct number of arguments
   * with the correct types.
   */
  @Override
  public void postVisit(ASTFunctionCall node)
  {
    Symbol symbol = lookupSymbol(node, node.name);

    // If the symbol wasn't found, an error was already added
    if (symbol != null)
    {
      // Function calls need to be to actual functions
      if (symbol.location != Symbol.MemLoc.STATIC_FUNC)
      {
        addError(node.name + " is not a valid function at " + node.getSourceInfo());
      }
      else
      {
        // Number of arguments must match
        if (node.arguments.size() != symbol.paramTypes.size())
        {
          addError(
              "Incorrect number of arguments for " + node.name + " at " + node.getSourceInfo());
        }
        else
        {
          // Types of arguments must match
          for (int i = 0; i < node.arguments.size(); i++)
          {
            if (node.arguments.get(i).getType() != symbol.paramTypes.get(i))
            {
              addError("Parameter " + (i + 1) + " for " + node.name + " should be "
                  + symbol.paramTypes.get(i) + " but found " + node.arguments.get(i).getType()
                  + " at " + node.getSourceInfo());
            }
          }
        }
      }
    }
  }

  /**
   * Make sure if statements have a boolean conditional.
   */
  @Override
  public void postVisit(ASTConditional node)
  {
    if (node.condition.getType() != DataType.BOOL)
    {
      addError("Conditional must be of type bool at " + node.condition.getSourceInfo());
    }
  }

  /**
   * Make sure while loops have a boolean conditional.
   */
  @Override
  public void postVisit(ASTWhileLoop node)
  {
    if (node.guard.getType() != DataType.BOOL)
    {
      addError("While loop guard must be of type bool at " + node.guard.getSourceInfo());
    }
  }

  /**
   * Make sure both sides of assignments have the correct type.
   */
  @Override
  public void postVisit(ASTAssignment node)
  {
    if (node.location.getType() != node.value.getType())
    {
      addError("Assignment type mismatch at " + node.getSourceInfo());
    }
  }

  /**
   * Make sure binary expressions have the correct operand types.
   */
  @Override
  public void postVisit(ASTBinaryExpr node)
  {
    switch (node.operator)
    {
      case ADD:
        if (node.rightChild.getType() != DataType.INT || node.leftChild.getType() != DataType.INT)
        {
          addError("Addition operator must have two integer operands at " + node.getSourceInfo());
        }
        break;
      case AND:
        if (node.rightChild.getType() != DataType.BOOL || node.leftChild.getType() != DataType.BOOL)
        {
          addError("And operator must have two bool operands at " + node.getSourceInfo());
        }
        break;
      case DIV:
        if (node.rightChild.getType() != DataType.INT || node.leftChild.getType() != DataType.INT)
        {
          addError("Division operator must have two integer operands at " + node.getSourceInfo());
        }
        break;
      case EQ:
        if (node.rightChild.getType() != node.leftChild.getType())
        {
          addError("Equal to operator must have two operands of the same type at "
              + node.getSourceInfo());
        }
        break;
      case GE:
        if (node.rightChild.getType() != DataType.INT || node.leftChild.getType() != DataType.INT)
        {
          addError("Greater than or equal to operator must have two integer operands at "
              + node.getSourceInfo());
        }
        break;
      case GT:
        if (node.rightChild.getType() != DataType.INT || node.leftChild.getType() != DataType.INT)
        {
          addError(
              "Greater than operator must have two integer operands at " + node.getSourceInfo());
        }
        break;
      case LE:
        if (node.rightChild.getType() != DataType.INT || node.leftChild.getType() != DataType.INT)
        {
          addError("Less than or equal to operator must have two integer operands at "
              + node.getSourceInfo());
        }
        break;
      case LT:
        if (node.rightChild.getType() != DataType.INT || node.leftChild.getType() != DataType.INT)
        {
          addError("Less than operator must have two integer operands at " + node.getSourceInfo());
        }
        break;
      case MOD:
        if (node.rightChild.getType() != DataType.INT || node.leftChild.getType() != DataType.INT)
        {
          addError("Modulo operator must have two integer operands at " + node.getSourceInfo());
        }
        break;
      case MUL:
        if (node.rightChild.getType() != DataType.INT || node.leftChild.getType() != DataType.INT)
        {
          addError(
              "Multiplication operator must have two integer operands at " + node.getSourceInfo());
        }
        break;
      case NE:
        if (node.rightChild.getType() != node.leftChild.getType())
        {
          addError("Not equal to operator must have two operands of the same type at "
              + node.getSourceInfo());
        }
        break;
      case OR:
        if (node.rightChild.getType() != DataType.BOOL || node.leftChild.getType() != DataType.BOOL)
        {
          addError("Or operator must have two bool operands at " + node.getSourceInfo());
        }
        break;
      case SUB:
        if (node.rightChild.getType() != DataType.INT || node.leftChild.getType() != DataType.INT)
        {
          addError(
              "Subtraction operator must have two integer operands at " + node.getSourceInfo());
        }
        break;
      default:
        addError("Invalid operator at " + node.getSourceInfo());
        break;
    }
  }

  /**
   * Make sure unary expressions have an operand of the correct type.
   */
  @Override
  public void postVisit(ASTUnaryExpr node)
  {
    switch (node.operator)
    {
      case NEG:
        if (node.child.getType() != DataType.INT)
        {
          addError("Negation operator must have an integer operand at " + node.getSourceInfo());
        }
        break;
      case NOT:
        if (node.child.getType() != DataType.BOOL)
        {
          addError("Not operator must have a bool operand at " + node.getSourceInfo());
        }
        break;
      default:
        addError("Invalid operator at " + node.getSourceInfo());
        break;
    }
  }

  /**
   * Make sure functions return the correct data type.
   */
  @Override
  public void postVisit(ASTReturn node)
  {
    // Find the function we're in (guaranteed to be in a function by parser)
    ASTNode function = node.getParent();
    while (!(function instanceof ASTFunction))
    {
      function = function.getParent();
    }
    
    // Get the data type returned by the function
    DataType returnedType = DataType.VOID;
    if (node.value != null)
    {
      returnedType = node.value.getType();
    }
    
    // If the return type doesn't match, add an error
    if (returnedType != function.getType())
    {
      addError("Function " + ((ASTFunction) function).name + " should return " + function.getType()
          + " but returns " + returnedType + " at " + node.getSourceInfo());
    }
  }
}
