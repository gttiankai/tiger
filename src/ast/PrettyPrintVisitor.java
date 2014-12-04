package ast;

import ast.Ast.Class.ClassSingle;
import ast.Ast.*;
import ast.Ast.Exp.*;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Stm.*;
import ast.Ast.Type.Boolean;
import ast.Ast.Type.ClassType;
import ast.Ast.Type.Int;
import ast.Ast.Type.IntArray;

public class PrettyPrintVisitor implements Visitor
{
  private static int indentLevel;

  public PrettyPrintVisitor()
  {
    this.indentLevel = 0;
  }


  private void indent()
  {
    this.indentLevel += 4;
  }

  private void unIndent()
  {
    this.indentLevel -= 4;
  }

  public int getIndentLevel(){

    return this.indentLevel;
  }
  private void setIndentLevel(int temIndentLevel){
    indentLevel = temIndentLevel;
  }

  public void autoIndent(){
    for (int i = 0; i < this.indentLevel; i++){
      say(" ");
    }
  }


  private void printSpaces()
  {
      this.say(" ");
  }

  private void sayln(String s)
  {
    System.out.println(s);
  }

  private void say(String s)
  {
    System.out.print(s);
  }

  // /////////////////////////////////////////////////////
  // expressions
  @Override
  public void visit(Add e)
  {
    // Lab2, exercise4: filling in missing code.
    // Similar for other methods with empty bodies.
    // Your code here:
    e.left.accept(this);
    printSpaces();
    say("+");
    printSpaces();
    e.right.accept(this);

  }

  @Override
  public void visit(And e)
  {
    e.left.accept(this);
    printSpaces();
    say("&&");
    printSpaces();
    e.left.accept(this);
  }

  @Override
  public void visit(ArraySelect e)
  {
    e.array.accept(this);
    say("[");
    e.index.accept(this);
    say("]");
  }

  @Override
  public void visit(Call e)
  {
    e.exp.accept(this);
    this.say("." + e.id + "(");
    for (Exp.T x : e.args) {  
      x.accept(this); 
      this.say(", ");
    }
	if (!e.args.isEmpty())
		this.say("\b\b");
    this.say(")");
    return;
  }

  @Override
  public void visit(False e)
  {
    say("false");
  }

  @Override
  public void visit(Id e)
  {

    this.say(e.id);
  }

  @Override
  public void visit(Length e)
  {
    say(".length");

  }

  @Override
  public void visit(Lt e)
  {
    e.left.accept(this);
    this.say(" < ");
    e.right.accept(this);
    return;
  }

  @Override
  public void visit(NewIntArray e)
  {
    say("int");
    printSpaces();
    say("[]");
    printSpaces();
    e.exp.accept(this);
  }

  @Override
  public void visit(NewObject e)
  {
    this.say("new " + e.id + "()");
    return;
  }

  @Override
  public void visit(Not e)
  {
    say("!");
    e.exp.accept(this);
  }

  @Override
  public void visit(Num e)
  {
    System.out.print(e.num);
    return;
  }

  @Override
  public void visit(Sub e)
  {
    e.left.accept(this);
    this.say(" - ");
    e.right.accept(this);
    return;
  }

  @Override
  public void visit(This e)
  {
    this.say("this");
  }

  @Override
  public void visit(Times e)
  {
    e.left.accept(this);
    this.say(" * ");
    e.right.accept(this);
    return;
  }

  @Override
  public void visit(True e)
  {
    say("true");
  }

  // statements
  @Override
  public void visit(Assign s)
  {
    this.say(s.id + " = ");
    s.exp.accept(this);
    this.sayln(";");
    return;
  }

  @Override
  public void visit(AssignArray s)
  {
    say(s.id);
    say("[");
    s.exp.accept(this);     // Exp
    say("]");
    printSpaces();
    say("=");
    printSpaces();

    s.exp.accept(this);     // Exp

    sayln(";");
  }

  @Override
  public void visit(Block s)
  {
    int size = s.stms.size();
    for(int i = 0; i < size; i++){
      // for the first element
      if (0 == i){
        s.stms.pop().accept(this);// stms
      }else {
        this.autoIndent();
        s.stms.pop().accept(this);// stms
      }
    }
  }

  @Override
  public void visit(If s)
  {
    this.autoIndent();
    this.indent();           // +
    this.say("if (");
    s.condition.accept(this);// Exp :: condition
    this.sayln(")");
    this.sayln("{");
    this.autoIndent();
    s.thenn.accept(this);   // Exp
    this.sayln("");
    this.unIndent();        // -
    this.autoIndent();
    this.say("}");
    this.sayln("else");
    this.indent();          // +
    this.say("{");
    this.autoIndent();
    s.elsee.accept(this);   // Exp
    this.sayln("");
    this.unIndent();        // -
    this.autoIndent();
    this.say("}");
    return;
  }

  @Override
  public void visit(Print s)
  {
    this.autoIndent();
    this.say("System.out.println (");
    s.exp.accept(this);      // Exp
    this.sayln(");");
    return;
  }

  @Override
  public void visit(While s)
  {
    say("while ( ");
    this.indent();
    s.condition.accept(this); // Exp
    say(") {");
    sayln("");
    this.autoIndent();
    s.body.accept(this);      // Stm
    this.autoIndent();
    sayln("}");

  }

  // type
  @Override
  public void visit(Boolean t)
  {
    say("boolean");
  }

  @Override
  public void visit(ClassType t)
  {
    say("class");
    printSpaces();
    say(t.id);
    printSpaces();
    say("{");
    sayln("");
    indent();
  }

  @Override
  public void visit(Int t)
  {
    this.say("int");
  }

  @Override
  public void visit(IntArray t)
  {
    say(t.toString());
  }

  // dec
  @Override
  public void visit(Dec.DecSingle d)
  {
    d.type.accept(this);      // Type
    printSpaces();
    say(d.id);
    sayln(";");
  }

  // method
  @Override
  public void visit(MethodSingle m)
  {
    this.say("public");
    this.printSpaces();

    m.retType.accept(this);
    printSpaces();
    this.say(m.id);
    say("(");
    for (Dec.T d : m.formals) {

      Dec.DecSingle dec = (Dec.DecSingle) d;
      dec.type.accept(this);
      printSpaces();
      this.say(dec.id);
      say(", ");
    }
    if (!m.formals.isEmpty())
		this.say("\b\b");


    this.say(")");
    this.sayln("{");
    this.indent();
    for (Dec.T d : m.locals) {
      this.autoIndent();
      Dec.DecSingle dec = (Dec.DecSingle) d;
      dec.accept(this);
    }
    this.sayln("");
    for (Stm.T s : m.stms) {
      this.autoIndent();
      s.accept(this); // Stm
    }
    this.unIndent();
    this.autoIndent();
    this.say("return ");

    m.retExp.accept(this);
    this.sayln(";");
    this.unIndent();
    this.autoIndent();
    this.sayln("}");

    return;
  }

  // class
  @Override
  public void visit(ClassSingle c)
  {
    this.say("class " + c.id);
    if (c.extendss != null) {
      this.sayln(" extends " + c.extendss);
      printSpaces();
      say("{");
    }
    else{
      printSpaces();
      sayln("{");
    }

    this.indent();

    for (Dec.T d : c.decs) {
      Dec.DecSingle dec = (Dec.DecSingle) d;
      dec.type.accept(this);            // Type
      this.sayln(dec.id + ";");
    }
    this.autoIndent();
    for (Method.T mthd : c.methods)
      mthd.accept(this);
    this.unIndent();

    this.sayln("}");
    return;
  }

  // main class
  @Override
  public void visit(MainClass.MainClassSingle c)
  {
    this.sayln("class " + c.id);
    this.sayln("{");

    this.indent();

    this.autoIndent();
    this.say("public static void main (String [] " + c.arg + ")");
    printSpaces();
    this.sayln("{");
    this.autoIndent();
    c.stm.accept(this);// accept(statement)
    autoIndent();
    this.sayln("}");

    this.unIndent();
    this.autoIndent();
    this.sayln("}");
    return;
  }

  // program
  @Override
  public void visit(Program.ProgramSingle p)
  {
    p.mainClass.accept(this);
    this.sayln("");// oup put "\n" to the outstream
    for (ast.Ast.Class.T classs : p.classes) {
      classs.accept(this);
    }
    System.out.println("\n\n");
  }
}
