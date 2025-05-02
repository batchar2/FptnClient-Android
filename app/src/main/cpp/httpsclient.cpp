///*=============================================================================
//Copyright (c) 2024-2025 Stas Skokov
//
//Distributed under the MIT License (https://opensource.org/licenses/MIT)
//=============================================================================*/
//
//public class HttpsClientJni {
//public native long createClient(String host, int port, String sni);
//public native void destroyClient(long clientPtr);
//public native String get(long clientPtr, String handle, int timeout);
//public native String post(long clientPtr, String handle, String request,
//                          String contentType, int timeout);
//
//    // Вспомогательные методы
//public native String getSHA1Hash(int number);
//public native String generateFptnKey(int timestamp);
//
//    static {
//        System.loadLibrary("httpsclient");
//    }
//}