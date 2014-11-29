package slp;

public class Table {
	private static boolean IsFirstTable =  true;
	String id;
	int value;
	Table tail; // 尾巴
	
	Table(){
		// 空的构造函数
	}
	Table(String i, int v, Table t){
		id = i;
		value = v;
		tail = t;
	}
	
	public  static Table Update_Table(Table Table_older, String id, int value){
		 Table TableTemp = new Table(id, value,Table_older);
		 return TableTemp;
	}
	
}
