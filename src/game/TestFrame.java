package game;

import javax.swing.SwingUtilities;

public class TestFrame {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				CUT cut = new CUT(Player.WHITE);
			}
		});
	}

}//test sınıfı,oyunun çalıştığını görmek için 2 defa çalıştırın.