package elaborator;

import ast.Ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import elaborator.ClassTable;

/**
 * Created by tiankai on 14-12-17.
 */
public class Warning {

    private ArrayList<String> usedId;


    public Warning(){
        this.usedId = new ArrayList<String>();
    }
    private void printWarning(String id, int lineNumber){

        System.out.println("[++Warning++]: variable "
                + id
                +" declared at line "
                + lineNumber
                +" never used");
    }

    public void put(String id){
        this.usedId.add(id);
    }

    public void printMethodWarning(Ast.Method.MethodSingle m){


        for (Ast.Dec.T dec : m.formals) {
            Ast.Dec.DecSingle decc = (Ast.Dec.DecSingle) dec;

            if (!this.usedId.contains(decc.id)){
                printWarning(decc.id, decc.lineNumber);
            }
        }
        for (Ast.Dec.T dec : m.locals) {
            Ast.Dec.DecSingle decc = (Ast.Dec.DecSingle) dec;

            if( ! this.usedId.contains(decc.id)){
                printWarning(decc.id, decc.lineNumber);
            }

        }
    }
}
