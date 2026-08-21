package com.izekip.izessentials.client.friends.firebase;

/**
 * Firebase Web API key ve Realtime Database URL'i - Firebase'in kendi belgelerine gore
 * bunlar "sir" degildir, guvenlik tamamen Realtime Database Security Rules ile saglanir.
 * ASLA buraya bir "service account" / Admin SDK anahtari konulmamali - o gercek bir sirdir
 * ve tum veritabanina sinirsiz erisim verir.
 */
public final class FirebaseConfig {
	public static final String WEB_API_KEY = "AIzaSyC_9o-Vl94qLvRy2Wb7OL9RCEjKkSw_z4k";
	public static final String DATABASE_URL = "https://izekip-af1df-default-rtdb.firebaseio.com";

	private FirebaseConfig() {
	}
}
