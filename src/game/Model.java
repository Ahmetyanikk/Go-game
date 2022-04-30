package game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import multiplayer.Client;
import multiplayer.LAN_Conn;
import multiplayer.Server;
import multiplayer.UpdateMessages;


 // Oyunun modeli bu java dosyasında yazıyor.
 
public class Model extends Observable{
    
    private LAN_Conn lan;       //Server or client
    private int ter_B;                    //ele geçirilen siyah bölgeler
    private int ter_W;                    //ele geçirilen beyaz bölgeler
    private int pris_B = 0;                    //ele geçirilen(mahkum) siyah taşlar
    private int pris_W = 0;                    //ele geçirilen(mahkum) beyaz taşlar
    private int pris_B_b4 = pris_B;
    private int pris_W_b4 = pris_W;
    private Player player;
    
    /**
     * The number of the current draw, incremented by each player in each of their turns. Used to determine whose turn it is.
     *
     */
	private int gameCnt;
	private IS[][] board;
	/** The previous ("minus 1") state of the board. board is set to this when a move is undone. */
	private IS[][] board_m1;
	private Position playedLast = new Position(0, 0); //Position where the last stone was put
	private Position played2ndLast = new Position(-1, -1); //used e.g. when a locally processed move must be undone
	private Position killedLast = new Position(-1, -1);
	private int lastKillCount = 0;
	boolean blackGroup;
	boolean whiteGroup;
	int currentGroup = 0;							  //How many different regions exist
	int dim = Constants.BOARD_DIM;           //Board dimension (Beginner = 9, Professional = 19)
	private boolean isMyTurn;
	private boolean isGameOver = false;
	
	public Model(){
		gameCnt = 1;                    //Oyun sayaç 1 iken başlar
		//boş tahta yaratıyoruz
		board = new IS[dim][dim];
		board_m1 = new IS[dim][dim];
		
		initBoard(board);
		initBoard(board_m1);
	}//Model constructor
	
	private void initBoard(IS[][] board){
	    IS is;                          //kesişimleri belirlemek için kullanıyorum
        //merkezdeki kesişimleri oluşturuyorum
        for (int y=1; y < dim-1; y++){
            for (int x=1; x < dim-1; x++){
                is = new IS(IS.Type.C);
                board[y][x] = is;
            }
        }
        
        //köşe kesişimleri oluşturuyorum
        is = new IS(IS.Type.CRN_TL);
        board[0][0] = is;
        is = new IS(IS.Type.CRN_TR);
        board[0][dim-1] = is;
        is = new IS(IS.Type.CRN_BL);
        board[dim-1][0] = is;
        is = new IS(IS.Type.CRN_BR);
        board[dim-1][dim-1] = is;
        
        //kenar kesişimleri oluşturuyorum
        for (int x = 1; x < dim-1; x++){ //Top
            is = new IS(IS.Type.E_T);
            board[0][x] = is;
        }
        for (int y = 1; y < dim-1; y++){ //Left
            is = new IS(IS.Type.E_L);
            board[y][0] = is;
        }
        for (int y = 1; y < dim-1; y++){ //Right
            is = new IS(IS.Type.E_R);
            board[y][dim-1] = is;
        }
        for (int x = 1; x < dim-1; x++){ //Bottom
            is = new IS(IS.Type.E_B);
            board[dim-1][x] = is;
        }
	}
	
	public void send(int b){
	    isMyTurn = false;
	    try {
	        System.out.println("Model: send: Going to send draw...");
            lan.send(b); 
            System.out.println("Model: send: Sent draw");
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}//model çizilmesi için gönderiliyor
	
	public void receive(){
	    try {
	        System.out.println("Model: receive: Call to receive");
            int recv = lan.receive();
            System.out.println("Model: receive: Return from receive. recv: " + recv);
            if (recv == Constants.SEND_PASS){
                System.out.println("Model: receive: Received pass");
                pass();
                setChanged();
                notifyObservers(UpdateMessages.RECVD_PASS);
            }else if (recv == Constants.SEND_DOUBLEPASS){
                System.out.println("Model: receive: Received double pass");
                pass();
                isGameOver = true;
                setChanged();
                notifyObservers(UpdateMessages.RECVD_DOUBLEPASS);
            }else if (recv == Constants.SEND_QUIT){
                System.out.println("Model: receive: Received quit");
                isGameOver = true;
                setChanged();
                notifyObservers(UpdateMessages.RECVD_QUIT);
            }else{
                int y = recv / dim;
                int x = recv - dim*y;
                processMove(y, x);
                setChanged();
                notifyObservers(UpdateMessages.RECVD_MOVE);
            }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } catch (InterruptedException e) {
			e.printStackTrace();
		}
	    isMyTurn = true;
	}//model alınıyor
	
	
	/**
	*Link server yada link client kurulur. Server adresine bağlı olarak değişir
	*server_adress lan ise boş olur ama uzaktaki servera bağlı ise adres girinir
     * eğer 0 dönerse bağlantıda problem yok
     *-1 dönerse hata aldık
     * -2 döner bağlantı daha önce yapıldı
	 */
	public int setLANRole(String server_address){
	    if (this.lan != null){
	        if (this.lan.isConnected())
	            return -2;
	        try {
                lan.terminate();
            } catch (IOException e) {
                e.printStackTrace();
            }
	    }
	    
        if (server_address.equals("")){
            this.lan = new Server(this);
            this.player = Player.WHITE;
            isMyTurn = false;
        }else{
            this.lan = new Client(this);
            this.player = Player.BLACK;
            isMyTurn = true;
        }
        try{
            this.lan.init(server_address); 
            this.lan.start();
            return 0;
        }catch (IOException e) {
            return -1;
        }
	}//setLAN_Role

    
    /**
	 * 2 oyuncunun da  bölgelerini saymak için aşağıdaki kodu kullandım
     */
	public void calcTerritory(){
		int[][] mark = new int[dim][dim];         //her oyuncunun bölgesini işaretler
												  //Tahtanın kopyasını oluşturup boş kesişim bölgelerini sayar
		ter_B = 0;
	    ter_W = 0;
	    currentGroup = 0;						  //o anki bölge sayısı (1-81)
	    for (int y=0; y < dim; y++){     
	        for (int x=0; x < dim; x++){           //bütün kesişimleri test etmeye çalışıyorum
	            
	            blackGroup = false;               //bölge işaretcisini resetliyorum
	            whiteGroup = false;               //bölge işaretcisini resetliyorum
	            currentGroup++;				  
	            
	            markGroup(y, x, mark);
	            
	            int cnt = 0;
	            for (int[] ia : mark){
	                for (int val : ia){
	                    if (val == currentGroup){
	                        cnt++;
	                    }
	                }
	            }
	            if (blackGroup && !whiteGroup){      	 //Siyah bölgesi olup olmadığını test ediyorum
	                ter_B += cnt;
	            }else if (whiteGroup && !blackGroup){      //Beyaz bölgesi olup olmadığını test ediyorum
	                ter_W += cnt;            	
	            }
	        }
	    }
	}//calcTerritory

	
	/**
	 *  Boş kesişim bölgelerini işaretleyip onların siyaha yada beyaza ait olduğuna bakıyorum.
	 *  0: bir gruba ait, -1: siyah taş, -2: beyaz.	
	 */
	public void markGroup(int y, int x, int[][] mark){        
		if (   y < 0 || y >= dim || x < 0 || x >= dim //oyun tahtasının sınırlarına geldim
		    || mark[y][x] > 0 ){                      //işaretlenmiş kesişim buldum
		    return;
		}else if ( mark[y][x] == -1 ){             
			blackGroup = true;
		}else if ( mark[y][x] == -2 ){             
			whiteGroup = true;
	    }else if ( board[y][x].getState().equals(IS.State.B) ){ //Boş bölge siyah oyuncuya ait    
	        blackGroup = true;
	        mark[y][x] = -1;
	    }else if ( board[y][x].getState().equals(IS.State.W) ){ //Boş bölge beyaz oyuncuya ait
            whiteGroup = true;
            mark[y][x] = -2;
	    }else{
	        mark[y][x] = currentGroup;        //boş kesişimleri bölgelerine göre işaretle
	        markGroup(y, x-1, mark);                //batı
	        markGroup(y-1, x, mark);                //kuzey
	        markGroup(y, x+1, mark);                //doğu
	        markGroup(y+1, x, mark);                //güney
	    }
	}
	
    public MoveReturn processMove(int y, int x) {
        
        MoveReturn mr = findKillOppGroups(y, x, getCurrentPlayer());
        if (mr.equals(MoveReturn.KO) || mr.equals(MoveReturn.SUICIDE))
    		return mr;
        
        board[playedLast.getY()][playedLast.getX()].wasNotPutLast(); /
        played2ndLast.set(playedLast);
        playedLast.set(y, x);
        
        gameCnt++;  //sonraki oyuncu için  sayaç çalışıyor
        return MoveReturn.OK;
    }// processMove
    public MoveReturn findKillOppGroups(int y, int x, IS.State playerColor){
    	Position killed = new Position(-1, -1);
    	int killCount = 0;
    	
    	board[y][x].setState(getCurrentPlayer());      //Oyuncunu taşlarını boş kesişimlere koyuyorum
    	Position[] adj = getAdjPositions(y, x);
        for (Position pos : adj) {
            if (board[pos.getY()][pos.getX()].getState().equals(getOpponent(playerColor))){
                int [][] mark = new int[dim][dim];                                          //bulduğumuzu işaretle
                mark[y][x] = 2; 
                if (!hasGroupLiberty(pos.getY(), pos.getX(), pos.getY(), pos.getX(), getOpponent(playerColor), mark)){  //eğer nefes alanı yoksa,
                    for (int l = 0; l < dim; l++) {                                             //kaldır
                        for (int k = 0; k < dim; k++) {
                            if (mark[l][k] == 1) {
                				board[l][k].setEmpty();
                				killed.set(l, k);
                				killCount++;
                            }
                        }
                    }
                }
            }
        }
        if (killCount == 0 && isSuicide(y, x)){
        	board[y][x].setEmpty();
            return MoveReturn.SUICIDE;
        }
        if (killCount == 1 && lastKillCount == 1
        		&& killedLast.equals(new Position(y, x))){
    		board[y][x].setEmpty();
    		board[killed.getY()][killed.getX()].setState(getOpponent(getCurrentPlayer()));
    		return MoveReturn.KO;
    	}
        if (killCount > 0){
        	if (getCurrentPlayer().equals(IS.State.W)) {        //beyazın hamlesi
        		pris_W += killCount;                                       //beyaz siyahın taşlarını alır
        	} else {                                            //siyahın hamlesi
        		pris_B += killCount;                                       //siyah beyazın taşlarını alır
        	}
        	if (killCount == 1){ 
        		killedLast.set(killed);
        	}else{ //eğer birden fazla taş  alındıysa sonraki hamlede ko olamaz
        		killedLast.set(-1, -1);
        	}
        }
        lastKillCount = killCount;
        return MoveReturn.OK;
    }
    // özgür alan var mı diye tahtayı arıyorum
    public boolean hasGroupLiberty(int yStart, int xStart, int yNow, int xNow, IS.State playerColor, int[][] mark){
        if (yNow < 0 || yNow >= dim || xNow < 0 || xNow >= dim        
                || mark[yNow][xNow] == 1 || mark[yNow][xNow] == 2){    
            return false;                                
        }else if (board[yNow][xNow].getState().equals(getOpponent(playerColor))){ 
            mark[yNow][xNow] = 2;                    
            return false;
        }else if (board[yNow][xNow].getState().equals(IS.State.E) && (yStart != yNow || xStart != xNow)){
            return true;                                 
        }else{                                           

            mark[yNow][xNow] = 1;                    
            if (    (hasGroupLiberty(yStart, xStart, yNow, xNow-1, playerColor, mark))     
                 || (hasGroupLiberty(yStart, xStart, yNow-1, xNow, playerColor, mark))
                 || (hasGroupLiberty(yStart, xStart, yNow, xNow+1, playerColor, mark)) 
                 || (hasGroupLiberty(yStart, xStart, yNow+1, xNow, playerColor, mark))) {

                return true;
            }
            return false;           
        }
    }
    
	
asGroupLiberty
	 */
    public boolean isSuicide(int y, int x) {
        int[][] mark = new int[dim][dim];                                      
        return !hasGroupLiberty(y, x, y, x, getCurrentPlayer(), mark);        
    }
    

    /** 
  boş geçme hamlesi 
     */
    public boolean pass() {
        backupBoardStates(); 
        int pris_tmp, pris_tmp_b4;
        if (getCurrentPlayer().equals(IS.State.B)) { 
            pris_tmp = pris_B;
            pris_tmp_b4 = pris_B_b4;
            this.pris_W_b4 = this.pris_W;
            this.pris_W++; 
        } else { 
            pris_tmp = pris_W;
            pris_tmp_b4 = pris_W_b4;
            this.pris_B_b4 = this.pris_B;
            this.pris_B++; 
        }
        this.gameCnt++;

        
        if (pris_tmp == pris_tmp_b4 + 1){
            isGameOver = true;
        }
        return isGameOver;
    }
    
    

	

	private void backupBoardStates(){
	    cpyBoard(board, board_m1);
	}
	
	private void restoreBoardStates(){
        cpyBoard(board_m1, board);                          
	}
	
	private void backupPrisoners(){
        if (getCurrentPlayer().equals(IS.State.B)) {
            this.pris_W_b4 = this.pris_W;
        } else {
            this.pris_B_b4 = this.pris_B;
        }
	}
	
	private void restorePrisoners(){
        if (getCurrentPlayer().equals(IS.State.B)){        
            this.pris_W = this.pris_W_b4;                           
        }else{
            this.pris_B = this.pris_B_b4;               
        }
	}
	

 
    public boolean areBoardsEqual(IS[][] one, IS[][] other){
        for (int y = 0; y < dim; y++){
            for (int x = 0; x < dim; x++){
                //TODO: implement and use equals instead of comparing states? Also in cpyBoard()
                if ( one[y][x].getState() != other[y][x].getState() ){
                    return false;
                }
            }
        }
        return true;
    }
    
   
	public boolean isEmptyIntersection(int y, int x){
	    return (board[y][x].getState() == IS.State.E);    
	}
	
	public Position[] getAdjPositions(int y, int x){
        List<Position> adj = new ArrayList<Position>();
		int []yAdj = new int[4];
        yAdj[0] = y;
        yAdj[1] = y-1;
        yAdj[2] = y;
        yAdj[3] = y+1;
        int []xAdj = new int[4];
        xAdj[0] = x-1;
        xAdj[1] = x;
        xAdj[2] = x+1;
        xAdj[3] = x;
        for (int i = 0; i < 4; i++){
            if (yAdj[i] < 0 || yAdj[i] >= dim || xAdj[i] < 0 || xAdj[i] >= dim)
                continue;
            adj.add(new Position(yAdj[i], xAdj[i]));
        }
        return adj.toArray(new Position[adj.size()]);
	}

	public void cpyBoard(IS[][] src, IS[][] dest){
        for (int y=0; y < dim; y++){
            for (int x=0; x < dim; x++){
                if (dest[y][x].getState() != src[y][x].getState())
                    dest[y][x].setState(src[y][x].getState());
            }            
        }
    }

    public void setChanged1() {
        setChanged();
    }//setChanged1
    
    public void notifyObservers2(Object updateMessage){
        notifyObservers(updateMessage);
    }
    public String boardToString(IS[][]board, boolean orientations){
        StringBuffer results = new StringBuffer();
        String separator = " ";

        for (int y = 0; y < dim; ++y){
            if (y > 0)
                results.append('\n');
            for (int x = 0; x < dim; ++x){
                results.append(board[y][x].toString(orientations)).append(separator);
            }
        }
        return results.toString();
    }

    @Override
    public String toString() {
        return "\nModel [lan=" + lan + ", ter_B=" + ter_B + ", ter_W=" + ter_W + ", pris_B="
                + pris_B + ", pris_W=" + pris_W + ", pris_B_b4=" + pris_B_b4 + ", pris_W_b4=" + pris_W_b4 + ", gamecnt="
                + gameCnt + ",\nboard=\n" + boardToString(board, false) + ",\nboard_b4=\n" + boardToString(board_m1, false)
                + ",\ntmp_4_ko=\n" /*+ boardToString(board_m2, false)*/
                + ",\nblackGroup=" + blackGroup + ", whiteGroup=" + whiteGroup + ", currentRegion=" + currentGroup
                + ", dim=" + dim + "]\n";
    }
    
    public IS[][] getBoard() {
        return board;
    }

    public IS[][] getBoard_m1() {
        return board_m1;
    }
    
    public int getTer_B() {
        return ter_B;
    }

    public int getTer_W() {
        return ter_W;
    }

    public int getPris_B() {
        return pris_B;
    }

    public int getPris_W() {
        return pris_W;
    }

    public int getScr_B(){
        return pris_B + ter_B;
    }
    
    public int getScr_W(){
        return pris_W + ter_W;
    }
    
    public boolean isMyTurn(){
        return isMyTurn;
    }
    
    public boolean isGameOver(){
        return isGameOver;
    }
    
	public int getGamecnt() {
	    return gameCnt;
	}
	
    public IS getIntersection(int y, int x) {
        return board[y][x];
    }
 
    public Player getMyPlayer(){
        return player;
    }
    
    
    public void setMyPlayer(Player player){
        this.player = this.player == null ? player : this.player;
    }
    

    public IS.State getCurrentPlayer(){     
        if (gameCnt % 2 == 0){
            return IS.State.W;
        }else{
            return IS.State.B;
        }
    }
    

    public IS.State getOpponent(IS.State state){     
        if (state == IS.State.W){
            return IS.State.B;
        }else if(state == IS.State.B){
            return IS.State.W;
        }else{
            System.out.println("This should not have happened.");
            return null;
        }
    }
    
    private void printBoards() {

        System.out.println("board:");
        System.out.println(boardToString(board, false) + "\n");
    }
    
    public enum MoveReturn {
    	OK, SUICIDE, KO
    }
}
