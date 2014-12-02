package slp;

import control.Control;
import slp.Slp.Exp;
import slp.Slp.Exp.Eseq;
import slp.Slp.Exp.Id;
import slp.Slp.Exp.Num;
import slp.Slp.Exp.Op;
import slp.Slp.ExpList;
import slp.Slp.Stm;
import util.Bug;

import java.io.FileWriter;
import java.util.HashSet;

public class Main {
	// ///////////////////////////////////////////
	// maximum number of args
	boolean IsFirestTable = true;
	Table DateTable = new Table("", 0, null);

	private int maxArgsExpList(ExpList.T explist) {
		if (explist instanceof ExpList.Last) {
			return 1;
		} else if (explist instanceof ExpList.Pair) {
			return 1 + maxArgsExpList(((ExpList.Pair) explist).list);
		} else {
			new Bug();
			return 0;
		}
	}

	private int maxArgsExp(Exp.T exp) {
		if (exp instanceof Exp.Eseq) {
			return maxArgsStm(((Exp.Eseq) exp).stm);
		} else {
			return 0;
		}
	}

	private int maxArgsStm(Stm.T stm) {
		if (stm instanceof Stm.Compound) {

			Stm.Compound s = (Stm.Compound) stm;
			int n1 = maxArgsStm(s.s1);
			int n2 = maxArgsStm(s.s2);
			return n1 >= n2 ? n1 : n2;

		} else if (stm instanceof Stm.Assign) {

			Stm.Assign assignStm = (Stm.Assign) stm;
			if (assignStm.exp instanceof Eseq) {
				// return maxArgsStm( ((Exp.Eseq)(assignStm.exp) ).stm);
				return maxArgsExp(assignStm.exp);
			}
		} else if (stm instanceof Stm.Print) {
			return maxArgsExpList(((Stm.Print) stm).explist);
		} else
			new Bug();
		return 0;
	}

	// ////////////////////////////////////////
	// interpreter

	/**
	 * 在数据表中查找 id 对应的值
	 * 
	 * @param id
	 * @return 返回id 对应的值
	 */
	private int lookup(String id) {
		Table table = DateTable;
		while ((!table.id.equals(id)) && (!table.id.equals(""))) {
			table = table.tail;
		}
		if (table.id.equals("")) {
			System.out.println(table.id + " does't exsit!");
			new Bug();
		}
		return table.value;
	}

	/**
	 * 对表达式进行计算
	 * 
	 * @param BinOp
	 * @return 计算的结果
	 */
	private int interpOp(Exp.Op BinOp) {
		switch (BinOp.op) {
		case ADD:
			return interpExp(BinOp.left) + interpExp(BinOp.right);
		case SUB:
			return interpExp(BinOp.left) - interpExp(BinOp.right);
		case TIMES:
			return interpExp(BinOp.left) * interpExp(BinOp.right);
		case DIVIDE:
			if (0 == interpExp(BinOp.right)) {
				System.out.println("divide by zero");
				System.exit(0);
			} else {
				return interpExp(BinOp.left) / interpExp(BinOp.right);
			}
		default:
			System.out.println("Can not implement Operate!");
			new Bug();
			break;
		}
		new Bug();
		return -1;
	}

	private int interpExp(Exp.T exp) {
		if (exp instanceof Exp.Id) {
			Exp.Id tableId = (Exp.Id) exp;
			int tempValue = lookup(tableId.id);
			return tempValue;
		} else if (exp instanceof Exp.Num) {
			Exp.Num Num = (Exp.Num) exp;
			return Num.num;
		} else if (exp instanceof Exp.Op) {
			return interpOp((Exp.Op) exp);
		} else if (exp instanceof Exp.Eseq) {
			// 处理逗号运算符左边的状态语句
			interpStm(((Exp.Eseq) exp).stm);
			// 处理逗号运算符右边的表达式
			return interpExp(((Exp.Eseq) exp).exp);
		} else {
			new Bug();
			// 前面由出错处理,所以下面这条语句是不会返回的.
			return -1;
		}

	}

	private void interpExpList(ExpList.T expList) {
		if (expList instanceof ExpList.Last) {
			System.out.println(interpExp(((ExpList.Last) expList).exp));
		} else if (expList instanceof ExpList.Pair) {
			/**
			 * 在最后加上两个空格，区分同一行输出
			 */
			System.out.print(interpExp(((ExpList.Pair) expList).exp) + "  ");
			interpExpList(((ExpList.Pair) expList).list);
		} else
			new Bug();
	}

	private void interpStm(Stm.T prog) {

		if (prog instanceof Stm.Compound) {
			Stm.Compound stm = (Stm.Compound) prog;
			interpStm(stm.s1);
			interpStm(stm.s2);
		} else if (prog instanceof Stm.Assign) {
			Table tempTable = Table.Update_Table(DateTable,
					((Stm.Assign) prog).id, interpExp(((Stm.Assign) prog).exp));
			DateTable = tempTable;

		} else if (prog instanceof Stm.Print) {
			interpExpList(((Stm.Print) prog).explist);
		} else
			new Bug();
	}

	// ////////////////////////////////////////
	// compile
	HashSet<String> ids;
	StringBuffer buf;

	private void emit(String s) {
		buf.append(s);
	}

	/***
	 * assert the divide by zero
	 * 
	 * 
	 * @param exp
	 */
	private void assertDivideByZero(Exp.T exp) {
		if (exp instanceof Id) {
			Exp.Id e = (Exp.Id) exp;
			String id = e.id;
			if (0 == lookup(e.id)) {
				System.out.println("divide by zero");
				System.exit(0);
			}
		} else if (exp instanceof Num) {
			Exp.Num e = (Exp.Num) exp;
			int num = e.num;
			if (0 == num) {
				System.out.println("divide by zero");
				System.exit(0);
			}

		} else if (exp instanceof Op) {
			Exp.Op e = (Exp.Op) exp;
			if (0 == interpOp(e)) {
				System.out.println("divide by zero");
				System.exit(0);
			}
		}

	}

	private void compileExp(Exp.T exp) {
		if (exp instanceof Id) {
			Exp.Id e = (Exp.Id) exp;
			String id = e.id;

			emit("\tmovl\t" + id + ", %eax\n");
		} else if (exp instanceof Num) {
			Exp.Num e = (Exp.Num) exp;
			int num = e.num;

			emit("\tmovl\t$" + num + ", %eax\n");
		} else if (exp instanceof Op) {
			Exp.Op e = (Exp.Op) exp;
			Exp.T left = e.left;
			Exp.T right = e.right;
			Exp.OP_T op = e.op;

			switch (op) {
			case ADD:
				compileExp(left);
				emit("\tpushl\t%eax\n");
				compileExp(right);
				emit("\tpopl\t%edx\n");
				emit("\taddl\t%edx, %eax\n");
				break;
			case SUB:
				compileExp(left);
				emit("\tpushl\t%eax\n");
				compileExp(right);
				emit("\tpopl\t%edx\n");
				emit("\tsubl\t%eax, %edx\n");
				emit("\tmovl\t%edx, %eax\n");
				break;
			case TIMES:
				compileExp(left);
				emit("\tpushl\t%eax\n");
				assertDivideByZero(right);
				compileExp(right);
				emit("\tpopl\t%edx\n");
				emit("\timul\t%edx\n");
				break;
			case DIVIDE:
				compileExp(left);
				emit("\tpushl\t%eax\n");
				assertDivideByZero(right);
				compileExp(right);
				emit("\tpopl\t%edx\n");
				emit("\tmovl\t%eax, %ecx\n");
				emit("\tmovl\t%edx, %eax\n");
				emit("\tcltd\n");
				emit("\tdiv\t%ecx\n");
				break;
			default:
				new Bug();
			}
		} else if (exp instanceof Eseq) {
			Eseq e = (Eseq) exp;
			Stm.T stm = e.stm;
			Exp.T ee = e.exp;

			compileStm(stm);
			compileExp(ee);
		} else
			new Bug();
	}

	private void compileExpList(ExpList.T explist) {
		if (explist instanceof ExpList.Pair) {
			ExpList.Pair pair = (ExpList.Pair) explist;
			Exp.T exp = pair.exp;
			ExpList.T list = pair.list;

			compileExp(exp);
			emit("\tpushl\t%eax\n");
			emit("\tpushl\t$slp_format\n");
			emit("\tcall\tprintf\n");
			emit("\taddl\t$4, %esp\n");
			compileExpList(list);
		} else if (explist instanceof ExpList.Last) {
			ExpList.Last last = (ExpList.Last) explist;
			Exp.T exp = last.exp;

			compileExp(exp);
			emit("\tpushl\t%eax\n");
			emit("\tpushl\t$slp_format\n");
			emit("\tcall\tprintf\n");
			emit("\taddl\t$4, %esp\n");
		} else
			new Bug();
	}

	private void compileStm(Stm.T prog) {
		if (prog instanceof Stm.Compound) {
			Stm.Compound s = (Stm.Compound) prog;
			Stm.T s1 = s.s1;
			Stm.T s2 = s.s2;

			compileStm(s1);
			compileStm(s2);
		} else if (prog instanceof Stm.Assign) {
			Stm.Assign s = (Stm.Assign) prog;
			String id = s.id;
			Exp.T exp = s.exp;

			ids.add(id);
			compileExp(exp);
			emit("\tmovl\t%eax, " + id + "\n");
		} else if (prog instanceof Stm.Print) {
			Stm.Print s = (Stm.Print) prog;
			ExpList.T explist = s.explist;

			compileExpList(explist);
			emit("\tpushl\t$newline\n");
			emit("\tcall\tprintf\n");
			emit("\taddl\t$4, %esp\n");
		} else
			new Bug();
	}

	// ////////////////////////////////////////
	public void doit(Stm.T prog) {
		// return the maximum number of arguments
		if (Control.ConSlp.action == Control.ConSlp.T.ARGS) {
			int numArgs = maxArgsStm(prog);
			System.out.println(numArgs);
		}

		// interpret a given program
		if (Control.ConSlp.action == Control.ConSlp.T.INTERP) {
			interpStm(prog);
		}

		// compile a given SLP program to x86
		if (Control.ConSlp.action == Control.ConSlp.T.COMPILE) {
			ids = new HashSet<String>();
			buf = new StringBuffer();

			compileStm(prog);
			try {
				// FileOutputStream out = new FileOutputStream();
				FileWriter writer = new FileWriter("slp_gen.s");
				writer.write("// Automatically generated by the Tiger compiler, do NOT edit.\n\n");
				writer.write("\t.data\n");
				writer.write("slp_format:\n");
				writer.write("\t.string \"%d \"\n");
				writer.write("newline:\n");
				writer.write("\t.string \"\\n\"\n");
				for (String s : this.ids) {
					writer.write(s + ":\n");
					writer.write("\t.int 0\n");
				}
				writer.write("\n\n\t.text\n");
				writer.write("\t.globl main\n");
				writer.write("main:\n");
				writer.write("\tpushl\t%ebp\n");
				writer.write("\tmovl\t%esp, %ebp\n");
				writer.write(buf.toString());
				writer.write("\tleave\n\tret\n\n");
				writer.close();
				/**
				 * if your OS is 32-bites ,you should use Process child =
				 * Runtime.getRuntime().exec("gcc slp_gen.s"); if your OS is
				 * 64-bites ,you should use Process child =
				 * Runtime.getRuntime().exec("gcc -m32 slp_gen.s");
				 */
				Process child = Runtime.getRuntime().exec("gcc -m32 slp_gen.s");
				child.waitFor();
				if (!Control.ConSlp.keepasm)
					Runtime.getRuntime().exec("rm -rf slp_gen.s");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			// System.out.println(buf.toString());
		}
	}
}
