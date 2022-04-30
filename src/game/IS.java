package game;

//bu dosyada tahtayı kontrol etmek için kullanıyorum
public class IS {
    public enum Type {
        /** Center */
        C,
        /** Bottom left corner */
        CRN_BL, 
        /** Bottom right corner */
        CRN_BR,
        /** Top left corner */
        CRN_TL,
        /** Top right corner */
        CRN_TR,
        /** Bottom edge*/
        E_B,
        /** Left edge*/
        E_L,
        /** Right edge*/
        E_R,
        /** Top edge*/
        E_T
    }

    /** Kesişimlerin olduğu yerlerdeki taş durumu  (empty, black, or white) */
    public enum State {
        /** Empty */
        E,
        /** Black */
        B,
        /** White */
        W
    }
    
    private Type type;
    private State state;
    private boolean wasPutLast;
    
    IS(Type type){
        this.type= type;
        state = State.E;
        wasPutLast = false;
    }//IS  kütüphanesi constructor

    public Type getType() {
        return type;
    }

    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
        if (state.equals(State.B) || state.equals(State.W))
            wasPutLast = true;
    }
    
    /** kesişimi boşaltma */ 
    public void setEmpty(){
    	state = State.E;
    }
    
    /** Oynanan son taş bu kesişimde mi ? */
    public boolean wasPutLast(){
        return wasPutLast;
    }
    
    public void wasNotPutLast(){
        wasPutLast = false;
    }

    public String toString(boolean orientation) {
        String result = (state == State.E) ? " " : state.toString();
        if (orientation){
            result = type + ", " + result; 
        }
        char bracketOpen;
        char bracketClose;
        if (wasPutLast){
            bracketOpen = '(';
            bracketClose = ')';
        }else {
            bracketOpen = '[';
            bracketClose = ']';
        }
        result = bracketOpen + result + bracketClose; 
        return result; 
    }
}
