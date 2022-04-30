package multiplayer;

/**
 *  2 oyuncu arası gitmesi gereken değişkenler;"oyuncu bağlandı,gelen hareket,boş geçilen tur ,çift boş geçilen tur ve çıkış bildirimi"
 */
public enum UpdateMessages {
    CLIENT_CONNECTED, RECVD_MOVE, RECVD_PASS, RECVD_DOUBLEPASS, RECVD_QUIT
}
