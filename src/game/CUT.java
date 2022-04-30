package game;

//test kodu
public class CUT {

	Model model;
	Controller controller;
	
	
	public CUT(Player player){
		this.model = new Model();
		this.controller = new Controller(model);
	}
}
