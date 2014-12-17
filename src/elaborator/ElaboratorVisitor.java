package elaborator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import ast.Ast;
import ast.Ast.Class;
import ast.Ast.Class.ClassSingle;
import ast.Ast.Dec;
import ast.Ast.Exp;
import ast.Ast.Exp.Add;
import ast.Ast.Exp.And;
import ast.Ast.Exp.ArraySelect;
import ast.Ast.Exp.Call;
import ast.Ast.Exp.False;
import ast.Ast.Exp.Id;
import ast.Ast.Exp.Length;
import ast.Ast.Exp.Lt;
import ast.Ast.Exp.NewIntArray;
import ast.Ast.Exp.NewObject;
import ast.Ast.Exp.Not;
import ast.Ast.Exp.Num;
import ast.Ast.Exp.Sub;
import ast.Ast.Exp.This;
import ast.Ast.Exp.Times;
import ast.Ast.Exp.True;
import ast.Ast.MainClass;
import ast.Ast.Method;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Stm;
import ast.Ast.Stm.Assign;
import ast.Ast.Stm.AssignArray;
import ast.Ast.Stm.Block;
import ast.Ast.Stm.If;
import ast.Ast.Stm.Print;
import ast.Ast.Stm.While;
import ast.Ast.Type;
import ast.Ast.Type.ClassType;
import control.Control.ConAst;
import slp.Main;

public class ElaboratorVisitor implements ast.Visitor
{
  public ClassTable classTable; // symbol table for class
  public MethodTable methodTable; // symbol table for each method
  public String currentClass; // the class name being elaborated
  public Type.T type; // type of the expression being elaborated
  public Warning warning;

  private LinkedList<String> errorLinkedlist = new LinkedList<String>();



  public ElaboratorVisitor()
  {
    this.classTable = new ClassTable();
    this.methodTable = new MethodTable();
    // every class has one warning,in fact has one used id array
    this.warning = new Warning();
    this.currentClass = null;
    this.type = null;
  }

  private void error()
  {
    System.out.println("type mismatch");
    System.exit(1);
  }


  private void error(String errorMessage, int lineNumber){

    errorLinkedlist.add("[++ERROR++] at "
            + lineNumber
            + " line type mismatch\n  "
            + errorMessage);
  }
  private void printError(){

    for (String s : errorLinkedlist){
      System.out.println(s);
    }
  }



  // /////////////////////////////////////////////////////
  // expressions
  @Override
  public void visit(Add e)
  {
    Type.T leftType ;
    e.left.accept(this);
    leftType = this.type;
    if ( !(leftType instanceof Type.Int) ){
      error("Add Operate: the left hand is not int!", e.lineNumber);
    }
    e.right.accept(this);
    if ( ! (this.type.toString().equals( leftType.toString()))){
      error("Add Operate: the right hand is not int!", e.lineNumber);
    }
    this.type = new Type.Int();
    return;

  }

  @Override
  public void visit(And e)
  {

    e.left.accept(this);
    Type.T leftType = this.type;
    e.right.accept(this);
    if (  leftType instanceof Type.Boolean){
      if (this.type.toString().equals( leftType.toString())){
        this.type = new Type.Boolean();
      }else {
        error("And Operate: the right hand is not boolean!", e.lineNumber);
      }
    }else {
      error("And Operate: the left hand is not boolean!", e.lineNumber);
    }

  }

  @Override
  public void visit(ArraySelect e)
  {
    e.index.accept(this);
    Type.T indexType = this.type;
    e.array.accept(this);
    if ( !(indexType instanceof Type.Int) ){
      error("ArraySelect Operate: the index is not int", e.lineNumber);
    }
    if ( !(this.type instanceof Type.IntArray)){
      error("ArraySelect Operate: the array is not int[]", e.lineNumber);
    }
    this.type = new Type.Int();
    return;

  }

  @Override
  public void visit(Call e)
  {
    Type.T leftty;
    Type.ClassType ty = null;

    e.exp.accept(this);
    leftty = this.type;
    if (leftty instanceof ClassType) {
      ty = (ClassType) leftty;
      e.type = ty.id;
    } else
      error("Call Operate: " + e.id + " is not class!", e.lineNumber);
    MethodType mty = this.classTable.getMethodType(ty.id, e.id);

    java.util.LinkedList<Type.T> declaredArgTypes
    = new java.util.LinkedList<Type.T>();
    for (Dec.T dec: mty.argsType){
      declaredArgTypes.add(((Dec.DecSingle)dec).type);
    }
    java.util.LinkedList<Type.T> argsty = new LinkedList<Type.T>();
    for (Exp.T a : e.args) {
      a.accept(this);
      argsty.addLast(this.type);
    }
    if (declaredArgTypes.size() != argsty.size())
      error(" Call Operate: the args number is mismatch!", e.lineNumber);
    // For now, the following code only checks that
    // the types for actual and formal arguments should
    // be the same. However, in MiniJava, the actual type
    // of the parameter can also be a subtype (sub-class) of the 
    // formal type. That is, one can pass an object of type "A"
    // to a method expecting a type "B", whenever type "A" is
    // a sub-class of type "B".
    // Modify the following code accordingly:
    for (int i = 0; i < argsty.size(); i++) {
      Dec.DecSingle dec = (Dec.DecSingle) mty.argsType.get(i);
      if (dec.type.toString().equals(argsty.get(i).toString()))
        ;
      else {
        String ancestor = argsty.get(i).toString();
        for (;;) {
          // find if dec.type.toString() is ancestor of argsty.get(i).toString()
          ClassBinding cb = this.classTable.get(ancestor);
          if (cb == null){
            error( "Call: dec type is " + dec.type.toString() +
                    ",but the parsed argument is " + argsty.get(i).toString(), e.lineNumber);
          }else {
            if (cb.extendss.equals(dec.type.toString()))
              ;//detected extends
            else {
              ancestor = cb.extendss;
              continue;//
            }
          }
          break;
        }
      }
    }

    this.type = mty.retType;
    // the following two types should be the declared types.
    e.at = declaredArgTypes;
    e.rt = this.type;

    return;
  }

  @Override
  public void visit(False e)
  {
    this.type = new Type.Boolean();
    return;

  }

  @Override
  public void visit(Id e)
  {
    // first look up the id in method table
    Type.T type = this.methodTable.get(e.id);
    // if search failed, then s.id must be a class field.
    if (type == null) {
      type = this.classTable.get(this.currentClass, e.id);
      // mark this id as a field id, this fact will be
      // useful in later phase.
      e.isField = true;
    }
    if (type == null)
      error(e.id + " is not defined!", e.lineNumber);

    // add this id to warning's usedid list
    this.warning.put(e.id);

    this.type = type;
    // record this type on this node for future use.
    e.type = type;
    return;
  }

  @Override
  public void visit(Length e)
  {
    e.array.accept(this);
    Type.T arrayType= this.type;
    if ( !(arrayType instanceof Type.IntArray)){
      error("Length Operate: the Array type is not int[]", e.lineNumber);
    }
    this.type = new Type.Int();
  }

  @Override
  public void visit(Lt e)
  {
    // todo reference
    e.left.accept(this);
    Type.T ty = this.type;
    e.right.accept(this);
    if (!this.type.toString().equals(ty.toString()))
      error("Lt Operate: the left hand is not match right hand!", e.lineNumber);
    this.type = new Type.Boolean();
    return;
  }

  @Override
  public void visit(NewIntArray e)
  {
    this.type = new Type.IntArray();
    return;

  }

  @Override
  public void visit(NewObject e)
  {
    this.type = new Type.ClassType(e.id);
    return;
  }

  @Override
  public void visit(Not e)
  {
    e.exp.accept(this);
    Type.T expType = this.type;
    if ( !(expType instanceof Type.Boolean)){
      error("Not Operate: The exp is not boolean", e.lineNumber);
    }
    this.type = new Type.Boolean();
  }

  @Override
  public void visit(Num e)
  {
    this.type = new Type.Int();
    return;
  }

  @Override
  public void visit(Sub e)
  {
    e.left.accept(this);
    Type.T leftType = this.type;
    e.right.accept(this);
    if (!this.type.toString().equals(leftType.toString()))
      error("Sub Operate: the left type is " + leftType.toString()
              + " and the right type is " + this.type.toString() + "It is mismatch!",
              e.lineNumber);
    this.type = new Type.Int();
    return;
  }

  @Override
  public void visit(This e)
  {
    this.type = new Type.ClassType(this.currentClass);
    return;
  }

  @Override
  public void visit(Times e)
  {
    e.left.accept(this);
    Type.T leftty = this.type;
    e.right.accept(this);
    if (!this.type.toString().equals(leftty.toString()))
      error("Sub Operate: the left type is " + leftty.toString()
                      + "and the right type is " + this.type.toString() + "It is mismatch!",
              e.lineNumber);
    this.type = new Type.Int();
    return;
  }

  @Override
  public void visit(True e)
  {
    this.type = new Type.Boolean();
  }

  // statements
  @Override
  public void visit(Assign s)
  {
    // first look up the id in method table
    Type.T idType = this.methodTable.get(s.id);
    // if search failed, then s.id must
    if (idType == null)
      idType = this.classTable.get(this.currentClass, s.id);
    if (idType == null)
      error(s.id + "is not define!",s.lineNumber);
    this.warning.put(s.id);

    s.type = idType;// the type of id

    // get the type of exp
    s.exp.accept(this);
    Type.T expType = this.type;
    // compare
    if( !(expType.toString().equals(s.type.toString()) )){
      error("Assign Operate: can not make " +expType.toString() +" to " + s.type.toString(),
              s.lineNumber);
    }
    return;
  }

  @Override
  /**
   * array assign
   * example : int[] a = int [2];
   *           int[] b = int [2];
   *           b[1] = 1;
   *           b[2] = 2;
   *           a = b;
   */
  public void visit(AssignArray s)
  {
    Type.T idType = this.methodTable.get(s.id);
    if (idType == null){
      idType = this.classTable.get(this.currentClass, s.id);
    }
    if (idType == null){
      error(s.id + "is not define!", s.lineNumber);
    }
    if ( !(idType instanceof Type.IntArray) ){
      error("AssignArray Operate: the id is not int", s.lineNumber);
    }

    s.index.accept(this);
    Type.T indexType = this.type;

    if ( !(indexType instanceof Type.Int) ){
      error("AssignArray Operate: the index is not int", s.lineNumber);
    }
    s.exp.accept(this);
    Type.T expType = this.type;

    if( !( expType instanceof Type.Int) ){
      error("AssignArray Operate: can not make "+ this.type.toString() +" to "+ type.toString(),
              s.lineNumber);
    }
    return;
  }

  @Override
  public void visit(Block s)
  {
    for(Ast.Stm.T stm : s.stms){
      stm.accept(this);
    }
  }

  @Override
  public void visit(If s)
  {
    s.condition.accept(this);
    if (!this.type.toString().equals("@boolean"))
      error("If: the condition is npt boolean", s.lineNumber);
    s.thenn.accept(this);
    s.elsee.accept(this);
    return;
  }

  @Override
  public void visit(Print s)
  {

    s.exp.accept(this);
    if (!this.type.toString().equals("@int"))
      error("Print Operate: the exp is not int!", s.lineNumber);
    return;
  }

  @Override
  public void visit(While s)
  {
    s.condition.accept(this);
    if (!this.type.toString().equals("@boolean")){
      error("While: the condition is not boolean", s.lineNumber);
    }
    s.body.accept(this);
    return;
  }

  // type
  @Override
  public void visit(Type.Boolean t)
  {

  }

  @Override
  public void visit(Type.ClassType t)
  {
  }

  @Override
  public void visit(Type.Int t)
  {
  }

  @Override
  public void visit(Type.IntArray t)
  {
  }

  // dec
  @Override
  public void visit(Dec.DecSingle d)
  {
  }

  // method
  @Override
  public void visit(Method.MethodSingle m)
  {
    // construct the new method table and warning  every new method
    this.methodTable = new MethodTable();


    this.methodTable.put(m.formals, m.locals);

    if (ConAst.elabMethodTable)
      this.methodTable.dump(m.id);

    for (Stm.T s : m.stms)
      s.accept(this);
    m.retExp.accept(this);
    // print the waring
    this.warning.printMethodWarning(m);
    return;
  }

  // class
  @Override
  public void visit(Class.ClassSingle c)
  {
    this.currentClass = c.id;

    for (Method.T m : c.methods) {
      m.accept(this);
    }
    return;
  }

  // main class
  @Override
  public void visit(MainClass.MainClassSingle c)
  {
    this.currentClass = c.id;
    // "main" has an argument "arg" of type "String[]", but
    // one has no chance to use it. So it's safe to skip it...

    c.stm.accept(this);
    return;
  }

  // ////////////////////////////////////////////////////////
  // step 1: build class table
  // class table for Main class
  private void buildMainClass(MainClass.MainClassSingle main)
  {
    this.classTable.put(main.id, new ClassBinding(null));
  }

  // class table for normal classes
  private void buildClass(ClassSingle c)
  {
    this.classTable.put(c.id, new ClassBinding(c.extendss));
    for (Dec.T dec : c.decs) {
      Dec.DecSingle d = (Dec.DecSingle) dec;
      this.classTable.put(c.id, d.id, d.type);
    }
    for (Method.T method : c.methods) {
      MethodSingle m = (MethodSingle) method;
      this.classTable.put(c.id, m.id, new MethodType(m.retType, m.formals));
    }
  }

  // step 1: end
  // ///////////////////////////////////////////////////

  // program
  @Override
  public void visit(ProgramSingle p)
  {
    // ////////////////////////////////////////////////
    // step 1: build a symbol table for class (the class table)
    // a class table is a mapping from class names to class bindings
    // classTable: className -> ClassBinding{extends, fields, methods}
    buildMainClass((MainClass.MainClassSingle) p.mainClass);

    for (Class.T c : p.classes) {
      buildClass((ClassSingle) c);
    }

    // we can double check that the class table is OK!
    if (control.Control.ConAst.elabClassTable) {
      this.classTable.dump();
    }

    // ////////////////////////////////////////////////
    // step 2: elaborate(semantic analysis) each class in turn, under the class table
    // built above.
    p.mainClass.accept(this);

    for (Class.T c : p.classes) {
      c.accept(this);
    }
    printError();
  }
}
