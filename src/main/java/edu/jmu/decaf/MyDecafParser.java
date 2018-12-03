package edu.jmu.decaf;

import java.util.*;

/**
 * Concrete Decaf parser class.
 * Partners: Brandon Domonoski, Bryan Quach
 */
class MyDecafParser extends DecafParser
{
    private static final DecafParser PARSER = new MyDecafParser();
  
    /**
     * Top-level parsing routine. Begins parsing by calling the LL(1) routine
     * for the start symbol (i.e., the Program non-terminal).
     *
     * @param tokens Input token stream
     * @return Parsed abstract syntax tree
     * @throws InvalidSyntaxException Thrown if a syntax error is encountered
     */
    @Override
    public ASTProgram parse(Queue<Token> tokens) throws InvalidSyntaxException
    {
        return parseProgram(tokens);
    }

    /**
     * LL(1) parsing routine for the Program non-terminal.
     *
     * @param tokens Input token stream
     * @return Parsed abstract syntax tree
     * @throws InvalidSyntaxException Thrown if a syntax error is encountered
     */
    public ASTProgram parseProgram(Queue<Token> tokens)
            throws InvalidSyntaxException
    {
        SourceInfo src = getCurrentSourceInfo(tokens);
        ASTProgram program = new ASTProgram();
        program.setSourceInfo(src);

        // Empty Program
        if (tokens.isEmpty()) return program;
        
        while(!tokens.isEmpty())                       // *
        {
          try
          {
            ASTFunction func = parseFunc(tokens);      // Func
            program.functions.add(func);
            
          }
          catch(InvalidSyntaxException ise)
          {
            ASTVariable var = parseVar(tokens);        // Var
            program.variables.add(var);
          }
        }

        
        return program;
    }
    
    public static ASTVariable parseVar(Queue<Token> tokens)
        throws InvalidSyntaxException
    {
      ASTVariable result;
      String name;
      ASTNode.DataType type;
      Token lookahead;
      int arraySize = 0;
      SourceInfo sc = PARSER.getCurrentSourceInfo(tokens);
      
      if (tokens.isEmpty())
        throw new InvalidSyntaxException("Unexpected end of input");
      
      // Type
      type = parseType(tokens);                      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
      lookahead = tokens.peek();
      if(PARSER.isNextToken(tokens, Token.Type.ID))
      {
        name = tokens.poll().text;         // ID
        
        if (tokens.isEmpty()) throw new InvalidSyntaxException("Unexpected end of input");
        lookahead = tokens.peek();
        
        if (PARSER.isNextTokenSymbol(tokens, ";"))  // Not array
        {
          PARSER.matchSymbol(tokens, ";");
          result = new ASTVariable(name, type);
          result.setSourceInfo(sc);
          return result;
        }
        else if (PARSER.isNextTokenSymbol(tokens, "["))  // array
        {
          PARSER.matchSymbol(tokens, "[");
          if(PARSER.isNextToken(tokens, Token.Type.DEC))
          {
            arraySize = Integer.parseInt(tokens.poll().text);
            PARSER.matchSymbol(tokens, "]");
            PARSER.matchSymbol(tokens, ";");
            result = new ASTVariable(name, type, arraySize);
            result.setSourceInfo(sc);
            return result;
          }
        }
        else
        throw new InvalidSyntaxException("Unrecognized token: \"" 
            + lookahead.toString() + "\"");
      }
      else 
        throw new InvalidSyntaxException("Unrecognized token: \"" 
          + lookahead.toString() + "\"");
      
      return null; // will NEVER happen.
    }
    
    /**
     * Attempts to parse a Type Nonterminal.
     * @param tokens token queue.
     * @return ASTNode.DataType referring to the parsed data type.
     * @throws InvalidSyntaxException token doesn't refer to valid Type.
     */
    public static ASTNode.DataType parseType(Queue<Token> tokens)
        throws InvalidSyntaxException
    { 
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
      Token lookahead = tokens.element();
      if (PARSER.isNextTokenKeyword(tokens, "int")) // Type -> int
      {
        PARSER.consumeNextToken(tokens);
        return ASTNode.DataType.INT;
      }
      else if (PARSER.isNextTokenKeyword(tokens, "bool")) // Type -> bool
      {
        PARSER.consumeNextToken(tokens);
        return ASTNode.DataType.BOOL;
      }
      else if (PARSER.isNextTokenKeyword(tokens, "void")) // Type -> void
      {
        PARSER.consumeNextToken(tokens);
        return ASTNode.DataType.VOID;
      }
      else
      {
        throw new InvalidSyntaxException("Unrecognized token: \"" 
                      + lookahead.toString() + "\"");
      }
    }
    
    
    public static ASTFunction parseFunc(Queue<Token> tokens) 
        throws InvalidSyntaxException
    {
      ASTFunction result;
      ASTNode.DataType returnType;
      String fName;
      List<ASTFunction.Parameter> params;
      ASTBlock block;
      SourceInfo sc = PARSER.getCurrentSourceInfo(tokens);
      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
      // def Type ID '(' Params? ')' Block
      PARSER.matchKeyword(tokens, "def");                  // def
      returnType = parseType(tokens);                      // Type
      
      if(PARSER.isNextToken(tokens, Token.Type.ID)) 
      {
        fName = tokens.poll().text;                        // ID
        
        PARSER.matchSymbol(tokens, "(");                   // (
        if(PARSER.isNextTokenSymbol(tokens, ")"))          // ) no params
        {
          PARSER.matchSymbol(tokens, ")");
          block = parseBlock(tokens);
          result = new ASTFunction(fName, returnType, block);
        }
        else                                               // params
        {
          params = parseParam(tokens);
          PARSER.matchSymbol(tokens, ")");                 // )
          block = parseBlock(tokens);
          result = new ASTFunction(fName, returnType, block);
          result.parameters = params;
        }
            
            result.setSourceInfo(sc);
            return result;
      }
      throw new InvalidSyntaxException("Unrecognized token: \"" 
          + tokens.peek().text.toString() + "\"");
    }
    
    
    public static List<ASTFunction.Parameter> parseParam(Queue<Token> tokens)
      throws InvalidSyntaxException
    {
      List<ASTFunction.Parameter> result = new ArrayList<ASTFunction.Parameter>();
      
        if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
        // Parse parameter type
        ASTNode.DataType pType = parseType(tokens);                 // Type
        if (PARSER.isNextToken(tokens, Token.Type.ID))
        {
          // Get Parameter name and return parameter.
          String pName = tokens.poll().text;                        // ID
          result.add(new ASTFunction.Parameter(pName, pType));
        }
        else 
          throw new InvalidSyntaxException("Unrecognized token: \"" 
              + tokens.peek().text.toString() + "\"");
        
        while(PARSER.isNextTokenSymbol(tokens, ","))
        {
          PARSER.matchSymbol(tokens, ",");
          // Parse parameter type
          pType = parseType(tokens);                                  // Type
          if (PARSER.isNextToken(tokens, Token.Type.ID))
          {
            // Get Parameter name and return parameter.
            String pName = tokens.poll().text;                        // ID
            result.add(new ASTFunction.Parameter(pName, pType));
          }
          else 
            throw new InvalidSyntaxException("Unrecognized token: \"" 
                + tokens.peek().text.toString() + "\"");
        }
        
        return result;
     
    }
    
    
    
    public static ASTBlock parseBlock(Queue<Token> tokens)
      throws InvalidSyntaxException
    {
      ASTBlock result = new ASTBlock();
      SourceInfo sc = PARSER.getCurrentSourceInfo(tokens);
      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
      // '{' Var* Stmt* '}'
      PARSER.matchSymbol(tokens, "{");
      
      ASTVariable var;
      ASTStatement stmt;
      
      while(!PARSER.isNextTokenSymbol(tokens, "}"))
      {
        try
        {
          var = parseVar(tokens);         // Var
          result.variables.add(var);
        }
        catch(InvalidSyntaxException ise)
        {
          stmt = parseStmt(tokens);       // Stmt
          result.statements.add(stmt);  
        }
      }
      PARSER.matchSymbol(tokens, "}");

      result.setSourceInfo(sc);
      return result;
    }
    
    
    public static ASTStatement parseStmt(Queue<Token> tokens)
      throws InvalidSyntaxException
    { 
      String name;
      ASTStatement result;
      SourceInfo sc = PARSER.getCurrentSourceInfo(tokens);
      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
      // 1. Loc '=' Expr ';'
      if (PARSER.isNextToken(tokens, Token.Type.ID))
      {
        name = tokens.poll().text;
        
        if (PARSER.isNextTokenSymbol(tokens, "("))     // FuncCall
        {
          ASTFunctionCall curr = parseFuncCall(tokens, name); 
          PARSER.matchSymbol(tokens, ";");
          result = new ASTVoidFunctionCall(name);
          result.attributes = curr.attributes;
        }
        else
        {
          ASTLocation loc = parseLoc(tokens, name);  // loc
          PARSER.matchSymbol(tokens, "=");           // '='
          ASTExpression expr = parseExpr(tokens);    // expr
          PARSER.matchSymbol(tokens, ";");           // ';'
          result = new ASTAssignment(loc, expr);          
        }
      }
      else if (PARSER.isNextTokenKeyword(tokens, "if")) // 3. if '(' Expr ')' Block (else Block)?
      {
        PARSER.consumeNextToken(tokens);
        PARSER.matchSymbol(tokens, "(");
        ASTExpression cond = parseExpr(tokens);
        PARSER.matchSymbol(tokens, ")");
        ASTBlock ifBlock = parseBlock(tokens);
        ASTBlock elseBlock = null;
        
        if (PARSER.isNextTokenKeyword(tokens, "else"))
        {
          PARSER.consumeNextToken(tokens);
          elseBlock = parseBlock(tokens);
        }
        
        result = new ASTConditional(cond, ifBlock, elseBlock);
      }
      else if (PARSER.isNextTokenKeyword(tokens, "while"))   // 4. while '(' Expr ')' Block
      {
        PARSER.consumeNextToken(tokens);
        PARSER.matchSymbol(tokens, "(");
        ASTExpression guard = parseExpr(tokens);
        PARSER.matchSymbol(tokens, "");
        
        result = new ASTWhileLoop(guard, parseBlock(tokens));
      }
      else if (PARSER.isNextTokenKeyword(tokens, "return"))  // 5. return Expr? ';'
      {
        PARSER.consumeNextToken(tokens);
        
        if (PARSER.isNextTokenSymbol(tokens, ";"))
        {
          PARSER.consumeNextToken(tokens);
          result = new ASTReturn(null);
        }
        else
        {
          result = new ASTReturn(parseExpr(tokens));
          PARSER.matchSymbol(tokens, ";");
        }
      }
      else if (PARSER.isNextTokenKeyword(tokens, "break"))    // 6. break ';'
      {
        PARSER.consumeNextToken(tokens);
        PARSER.matchSymbol(tokens, "");
        result = new ASTBreak();
      }
      else if (PARSER.isNextTokenKeyword(tokens, "continue"))  // 7. continue ';'
      {
        PARSER.consumeNextToken(tokens);
        PARSER.matchSymbol(tokens, ";");
        result = new ASTContinue();
      }
      else
        throw new InvalidSyntaxException("Unrecognized token: \"" 
            + tokens.peek().text + "\"");
      
      result.setSourceInfo(sc);
      return result;
    }
    
    
    public static ASTExpression parseExpr(Queue<Token> tokens) 
        throws InvalidSyntaxException
    {
      ASTExpression result = null;
      ASTExpression single = null;
      SourceInfo sc = PARSER.getCurrentSourceInfo(tokens);
      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
       
       if (PARSER.isNextTokenSymbol(tokens, "!"))
       {
         PARSER.consumeNextToken(tokens);
         single = new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens));
       }
       else if (PARSER.isNextTokenSymbol(tokens, "-"))
       {
         PARSER.consumeNextToken(tokens);
         single = new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens));
       }
       else single = parseBaseExpr(tokens);
       
       while(true)
       {
         if (PARSER.isNextTokenSymbol(tokens, "||"))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.OR,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.OR,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.OR, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "&&"))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.AND,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.AND,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.AND, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "=="))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.EQ,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.EQ,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.EQ, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "!="))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.NE,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.NE,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.NE, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "<"))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.LT,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.LT,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.LT, single, parseBaseExpr(tokens)); 
         }
         else if (PARSER.isNextTokenSymbol(tokens, ">"))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.GT,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.GT,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.GT, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "<="))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.LE,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.LE,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.LE, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, ">="))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.GE,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.GE,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.GE, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "+"))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.ADD,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.ADD,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.ADD, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "-"))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.SUB,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.SUB,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.SUB, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "*"))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.MUL,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.MUL,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.MUL, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "/"))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.DIV,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.DIV,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.DIV, single, parseBaseExpr(tokens));
         }
         else if (PARSER.isNextTokenSymbol(tokens, "%"))
         {
           PARSER.consumeNextToken(tokens);
           if (PARSER.isNextTokenSymbol(tokens, "!"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.MOD,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NOT, parseBaseExpr(tokens)));
           }
           else if(PARSER.isNextTokenSymbol(tokens, "-"))
           {
             PARSER.consumeNextToken(tokens);
             result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.MOD,
                 single, new ASTUnaryExpr(ASTUnaryExpr.UnaryOp.NEG, parseBaseExpr(tokens)));
           }
           else result = new ASTBinaryExpr(ASTBinaryExpr.BinOp.MOD, single, parseBaseExpr(tokens));
         }
         else break;
       }
         
       if (result == null)
       {
         single.setSourceInfo(PARSER.getCurrentSourceInfo(tokens));
         return single;
       }
       else
       {
         result.setSourceInfo(sc);
         return result;
       }
    }
    
    public static ASTExpression parseBaseExpr(Queue<Token> tokens)
      throws InvalidSyntaxException
    {
      ASTExpression result;
      String name;
      SourceInfo sc = PARSER.getCurrentSourceInfo(tokens);
      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
      if (PARSER.isNextTokenSymbol(tokens, "("))       // '(' Expr ')'
      {
        PARSER.matchSymbol(tokens, "(");
        result = parseExpr(tokens);
        PARSER.matchSymbol(tokens, ")");
        
      }
      else if (PARSER.isNextToken(tokens, Token.Type.ID))
      {
        name = tokens.poll().text;
        
        if (PARSER.isNextTokenSymbol(tokens, "("))     // FuncCall
        {
          result = parseFuncCall(tokens, name);
        }
        else result = parseLoc(tokens, name);          // Loc
      }
      else result = parseLit(tokens);                  // Lit
      
      result.setSourceInfo(sc);
      return result;
    }
    
    public static ASTLocation parseLoc(Queue<Token> tokens, String name)
        throws InvalidSyntaxException
    {
      ASTLocation result;
      ASTExpression expr;
      SourceInfo sc = PARSER.getCurrentSourceInfo(tokens);
      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
      if (PARSER.isNextTokenSymbol(tokens, "["))      // Array.
      {
        PARSER.matchSymbol(tokens, "["); 
        expr = parseExpr(tokens);
        PARSER.matchSymbol(tokens, "]");
      }
      else expr = null;                                // Not array.
        
      result = new ASTLocation(name, expr);
      result.setSourceInfo(sc);
      return result;
    }
    
    
    public static ASTFunctionCall parseFuncCall(Queue<Token> tokens, String name)
        throws InvalidSyntaxException
    {
      ASTFunctionCall result;
      SourceInfo sc = PARSER.getCurrentSourceInfo(tokens);
      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
      result = new ASTFunctionCall(name);
      PARSER.matchSymbol(tokens, "(");
      result.arguments = parseArgs(tokens);
      PARSER.matchSymbol(tokens, ")");
      result.setSourceInfo(sc);
      return result;
    }
    
    public static List<ASTExpression> parseArgs(Queue<Token> tokens)
      throws InvalidSyntaxException
    {
      List<ASTExpression> result = new ArrayList<ASTExpression>();
      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      
        ASTExpression arg = parseExpr(tokens);
        result.add(arg);
        while (PARSER.isNextTokenSymbol(tokens, ","))
        {
          PARSER.consumeNextToken(tokens);
          arg = parseExpr(tokens);
          result.add(arg);
        }
        
      
      return result;
    }
    
    
    public static ASTLiteral parseLit(Queue<Token> tokens)
      throws InvalidSyntaxException
    {
      ASTLiteral result;
      SourceInfo sc = PARSER.getCurrentSourceInfo(tokens);
      
      if (tokens.isEmpty()) 
        throw new InvalidSyntaxException("Unexpected end of input");
      try
      {
        if (PARSER.isNextToken(tokens, Token.Type.DEC)) // DEC
        {
          result 
            = new ASTLiteral(ASTNode.DataType.INT, Integer.parseInt(tokens.poll().text));
        }
        else if (PARSER.isNextToken(tokens, Token.Type.HEX)) // HEX
        {
          String text = tokens.poll().text;
          if(text.startsWith("0x"))
          {
            text = text.substring(2);
          }
          result 
            = new ASTLiteral(ASTNode.DataType.INT, Integer.parseInt(text, 16));
        }
        else if (PARSER.isNextToken(tokens, Token.Type.STR)) // STR
        {
          result 
            = new ASTLiteral(ASTNode.DataType.STR, tokens.poll().text);
        }
        else if (PARSER.isNextTokenKeyword(tokens, "true")) // true
        {
          result 
            = new ASTLiteral(ASTNode.DataType.BOOL, tokens.poll().text);
        }
        else if (PARSER.isNextTokenKeyword(tokens, "false")) // false
        { 
          result 
            = new ASTLiteral(ASTNode.DataType.BOOL, tokens.poll().text);
        }
        else throw new InvalidSyntaxException("Unrecognized token: \"" 
            + tokens.peek().text.toString() + "\"");
      }
      catch (NumberFormatException nfe)
      {
          throw new InvalidSyntaxException("Unrecognized token: \"" 
              + tokens.peek().text + "\"");
      }
      
      result.setSourceInfo(sc);
      return result;
    }
    
}

