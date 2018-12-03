package edu.jmu.decaf;

import java.util.*;

/**
 * Concrete ILOC generator class.
 * 
 * Group Members: Brandon Domonoski, Bryan Quach
 */
public class MyILOCGenerator extends ILOCGenerator
{
  public static final ILOCOperand TRUE = ILOCOperand.newIntConstant(1);
  public static final ILOCOperand FALSE = ILOCOperand.newIntConstant(1);

    public MyILOCGenerator()
    {
    }


    
    @Override
    public void postVisit(ASTFunction node)
    {
        // propagate code from body block to the function level
        copyCode(node, node.body);
        
        // void functions might not have a return statement at the end,
        // so we manually add a "return" instruction just in case
        if (node.returnType == ASTNode.DataType.VOID) {
            emit(node, ILOCInstruction.Form.RETURN);
        }
     }
    
    @Override
    public void postVisit(ASTBlock node)
    {
      // concatenate the generated code for all child statements
      for (ASTStatement s : node.statements)
      {
        copyCode(node,s);    
      }
      
    }
    
    @Override
    public void postVisit(ASTUnaryExpr node)
    {
      ILOCOperand reg = ILOCOperand.newVirtualReg();
      copyCode(node, node.child);
      
      switch (node.operator)
      {
        case NOT:
          emit(node, ILOCInstruction.Form.NOT, getTempReg(node.child), reg);
          break;
          
        case NEG:
          emit(node, ILOCInstruction.Form.NEG, getTempReg(node.child), reg);
          break;
        
        default:
          break;
      }
      
      setTempReg(node, reg);
    }
    
    @Override
    public void postVisit(ASTBinaryExpr node)
    {     
        copyCode(node, node.leftChild);
        copyCode(node, node.rightChild);
        ILOCOperand register = ILOCOperand.newVirtualReg();
        switch (node.operator)
        {   
          case OR:
            emit(node, ILOCInstruction.Form.OR, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;

          case AND:
            emit(node, ILOCInstruction.Form.AND, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case EQ:
            emit(node, ILOCInstruction.Form.CMP_EQ, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case NE:
            emit(node, ILOCInstruction.Form.CMP_NE, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case LT:
            emit(node, ILOCInstruction.Form.CMP_LT, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case GT:
            emit(node, ILOCInstruction.Form.CMP_GT, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case LE:
            emit(node, ILOCInstruction.Form.CMP_LE, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case GE:
            emit(node, ILOCInstruction.Form.CMP_GE, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case ADD:
            emit(node, ILOCInstruction.Form.ADD, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case SUB:
            emit(node, ILOCInstruction.Form.SUB, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case MUL:
            emit(node, ILOCInstruction.Form.MULT, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case DIV:
            emit(node, ILOCInstruction.Form.DIV, getTempReg(node.leftChild), getTempReg(node.rightChild), register);
            break;
            
          case MOD:
          {
            // TODO: handle mod case. there is NO MOD ILOC Instruction!!!!
            break;
          }
          default:
        }
        
          if (node.getParent() instanceof ASTReturn)
            emit(node, ILOCInstruction.Form.I2I, register, ILOCOperand.REG_RET);
          setTempReg(node, register);
    }

    @Override
    public void postVisit(ASTReturn node)
    {   
        copyCode(node, node.value);
        
        emit(node, ILOCInstruction.Form.RETURN);
    }
    
    
    @Override
    public void postVisit(ASTLiteral node)
    {
      ILOCOperand intConst, strConst, boolConst, register;
      
      if (node.type == ASTNode.DataType.INT)
      {
        intConst = ILOCOperand.newIntConstant((int)node.value);
        register = ILOCOperand.newVirtualReg();
        emit(node, ILOCInstruction.Form.LOAD_I, intConst, register);
        setTempReg(node, register);
      }
      else if (node.type == ASTNode.DataType.STR)
      {
        strConst = ILOCOperand.newStrConstant((String)node.value);
        register = ILOCOperand.newVirtualReg();
        emit(node, ILOCInstruction.Form.LOAD_I, strConst, register);
        setTempReg(node, register);
      }
      else if (node.type == ASTNode.DataType.BOOL)
      {
        if ((boolean)node.value == true)
        {
          boolConst = ILOCOperand.newIntConstant(1);
          register = ILOCOperand.newVirtualReg();
          emit(node, ILOCInstruction.Form.LOAD_I, boolConst, register);
          setTempReg(node, register);
        }
        
        if ((boolean)node.value == false)
        {
          boolConst = ILOCOperand.ZERO;
          register = ILOCOperand.newVirtualReg();
          emit(node, ILOCInstruction.Form.LOAD_I, boolConst, register);
          setTempReg(node, register);
        }
      }
      
      if (node.getParent()instanceof ASTReturn)
        emit(node, ILOCInstruction.Form.I2I, getTempReg(((ASTReturn)node.getParent()).value), ILOCOperand.REG_RET);
    }
    
    @Override
    public void postVisit(ASTWhileLoop node)
    {
      ILOCOperand whileLoopJump, trueJump, falseJump;
      
      falseJump = ILOCOperand.newAnonymousLabel();
      trueJump = ILOCOperand.newAnonymousLabel();
      whileLoopJump = ILOCOperand.newAnonymousLabel();
      
      emit(node, ILOCInstruction.Form.LABEL, whileLoopJump);
      copyCode(node, node.guard);
      
      /* Conditional Branch */
      emit(node, ILOCInstruction.Form.CBR, getTempReg(node.guard), trueJump, falseJump);
      
      /* Conitional True branch */
      emit(node, ILOCInstruction.Form.LABEL, trueJump);
      copyCode(node, node.body);
      
      /* jump to guard */
      emit(node, ILOCInstruction.Form.JUMP, whileLoopJump);
      
      /* While loop jump exit */
      emit(node, ILOCInstruction.Form.LABEL, falseJump);
    }
    
    @Override
    public void postVisit(ASTConditional node)
    { 
      ILOCOperand ifTrueJump, ifTrueSkipFalseJump, ifFalseJump;
      
      /* Load if condition code */
      copyCode(node, node.condition);
      
      /* Make comparison */
      ILOCOperand register = ILOCOperand.newVirtualReg();
      if (node.condition instanceof ASTLiteral)
        emit(node, ILOCInstruction.Form.CMP_EQ, getTempReg(node.condition), TRUE, register);
        

      /* Initialize conditional jump locations */
      ifTrueJump = ILOCOperand.newAnonymousLabel();
      ifTrueSkipFalseJump = ILOCOperand.newAnonymousLabel();
      if (node.hasElseBlock()) // If - else conditional
      {
       ifFalseJump = ILOCOperand.newAnonymousLabel();
       emit(node, ILOCInstruction.Form.CBR, getTempReg(node.condition), ifTrueJump, ifFalseJump);
      /* Condition Branch */

       
       /* True */
       emit(node, ILOCInstruction.Form.LABEL, ifTrueJump);
       copyCode(node, node.ifBlock);
       
       emit(node, ILOCInstruction.Form.JUMP, ifTrueSkipFalseJump);
         
       
       /* False */
     
       emit(node, ILOCInstruction.Form.LABEL, ifFalseJump);
       copyCode(node, node.elseBlock);
       
       emit(node, ILOCInstruction.Form.LABEL, ifTrueSkipFalseJump);
      }
      else // if conditional
      {
        emit(node, ILOCInstruction.Form.CBR, getTempReg(node.condition), ifTrueJump, ifTrueSkipFalseJump);
        
        /* True */
        emit(node, ILOCInstruction.Form.LABEL, ifTrueJump);
        copyCode(node, node.ifBlock);
        
        /* False */
        emit(node, ILOCInstruction.Form.JUMP, ifTrueSkipFalseJump);
      }
     
      setTempReg(node, ifTrueSkipFalseJump);
    }
    
    @Override
    public void postVisit(ASTAssignment node)
    {
        copyCode(node, node.value);
        ILOCOperand srcReg = getTempReg(node.value);
        emitStore(node, srcReg);
    }
    
    @Override
    public void postVisit(ASTFunctionCall node)
    {
      ILOCOperand reg;
      /* Copy code of all arguments */
      for (ASTExpression e : node.arguments)
      {
        copyCode(node, e);
      }
      
      for (ASTExpression e : node.arguments)
      {
        if (e instanceof ASTLocation || e instanceof ASTLiteral)
        emit(node, ILOCInstruction.Form.PARAM, getTempReg(e));
      }
      Symbol func = DecafAnalysis.lookupSymbol(node, node.name);
      emit(node, ILOCInstruction.Form.CALL, ILOCOperand.newCallLabel(func.name));
      reg = ILOCOperand.newVirtualReg();
      emit(node, ILOCInstruction.Form.I2I, ILOCOperand.REG_RET, reg);
    }
    
    @Override
    public void postVisit(ASTLocation node)
    { 
      setTempReg(node, emitLoad(node)); 
      
      if (node.getParent() instanceof ASTReturn)
      {
        emit(node, ILOCInstruction.Form.I2I, getTempReg(((ASTReturn)node.getParent()).value), ILOCOperand.REG_RET);
      }
      
    }
    
}
